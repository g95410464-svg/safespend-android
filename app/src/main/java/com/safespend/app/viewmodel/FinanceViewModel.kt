package com.safespend.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.safespend.app.AppContainer
import com.safespend.app.data.AccountType
import com.safespend.app.data.FinanceRepository
import com.safespend.app.data.SettingsStore
import com.safespend.app.data.SavedCardEntity
import com.safespend.app.data.SubscriptionEntity
import com.safespend.app.data.TransactionEntity
import com.safespend.app.data.TxKind
import com.safespend.app.domain.ParsedReceipt
import com.safespend.app.domain.ReceiptParser
import com.safespend.app.service.NotificationHelper
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardState(
    val bankCents: Long = 0,
    val cashCents: Long = 0,
    /** Bank money locked for unpaid subscriptions. */
    val vaultCents: Long = 0,
    /** Bank minus Vault: what you can touch from the card without breaking a subscription. */
    val safeBankCents: Long = 0,
    /** Safe bank + cash. */
    val safeToSpendCents: Long = 0,
    val netWorthCents: Long = 0,
    /** Average daily discretionary spend this month (subscriptions excluded). */
    val dailyBurnCents: Long = 0,
    /** Daily burn as % of safe-to-spend: how much of your free money you burn per day. */
    val burnRatePct: Double = 0.0,
    val runwayDays: Int? = null,
    val thresholdCents: Long = 700,
    val belowThreshold: Boolean = false,
    val recent: List<TransactionEntity> = emptyList()
)

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data object Processing : ScanUiState
    data class Review(val parsed: ParsedReceipt) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

class FinanceViewModel(
    private val repo: FinanceRepository,
    private val settings: SettingsStore,
    private val notifier: NotificationHelper
) : ViewModel() {

    private fun monthKey() = YearMonth.now().toString()

    val subscriptions: StateFlow<List<SubscriptionEntity>> =
        repo.subscriptions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val savedCards: StateFlow<List<SavedCardEntity>> =
        repo.savedCards.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val state: StateFlow<DashboardState> = combine(
        repo.accounts, repo.transactions, repo.subscriptions, settings.thresholdCents
    ) { accounts, txs, subs, threshold ->
        val bank = accounts.firstOrNull { it.type == AccountType.BANK }?.balanceCents ?: 0L
        val cash = accounts.firstOrNull { it.type == AccountType.CASH }?.balanceCents ?: 0L
        val month = monthKey()
        val vault = subs.filter { it.active && it.lastPaidMonth != month }.sumOf { it.amountCents }

        val safeBank = bank - vault
        val safeTotal = safeBank + cash

        val monthStart = YearMonth.now().atDay(1).atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val discretionary = txs.filter {
            it.kind == TxKind.EXPENSE &&
                it.timestamp >= monthStart &&
                it.category != FinanceRepository.SUBSCRIPTION_CATEGORY
        }.sumOf { it.amountCents }
        val dailyBurn = discretionary / LocalDate.now().dayOfMonth.coerceAtLeast(1)

        val burnPct = when {
            dailyBurn == 0L -> 0.0
            safeTotal > 0 -> dailyBurn * 100.0 / safeTotal
            else -> 100.0
        }
        val runway = if (dailyBurn > 0 && safeTotal > 0) (safeTotal / dailyBurn).toInt() else null

        DashboardState(
            bankCents = bank, cashCents = cash, vaultCents = vault,
            safeBankCents = safeBank, safeToSpendCents = safeTotal,
            netWorthCents = bank + cash,
            dailyBurnCents = dailyBurn, burnRatePct = burnPct, runwayDays = runway,
            thresholdCents = threshold, belowThreshold = bank < threshold,
            recent = txs.take(15)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    // ---------- Manual transactions ----------
    fun addExpense(cents: Long, account: AccountType, category: String, note: String) =
        viewModelScope.launch {
            repo.addExpense(cents, account, category.ifBlank { "Otros" }, note)
            if (account == AccountType.BANK) checkSafetyLimit()
        }

    fun addIncome(cents: Long, account: AccountType, category: String, note: String) =
        viewModelScope.launch { repo.addIncome(cents, account, category.ifBlank { "Ingreso" }, note) }

    fun transfer(cents: Long, from: AccountType, to: AccountType, note: String) =
        viewModelScope.launch {
            repo.transfer(cents, from, to, note)
            if (from == AccountType.BANK) checkSafetyLimit()
        }

    // ---------- Subscriptions ----------
    fun saveSubscription(name: String, cents: Long, day: Int) = viewModelScope.launch {
        repo.saveSubscription(
            SubscriptionEntity(name = name.trim(), amountCents = cents, dayOfMonth = day.coerceIn(1, 31))
        )
    }

    fun deleteSubscription(sub: SubscriptionEntity) = viewModelScope.launch { repo.deleteSubscription(sub) }

    fun saveCard(card: SavedCardEntity) = viewModelScope.launch { repo.saveCard(card) }
    fun deleteCard(card: SavedCardEntity) = viewModelScope.launch { repo.deleteCard(card) }

    fun paySubscription(sub: SubscriptionEntity) = viewModelScope.launch {
        repo.paySubscription(sub, monthKey())
        checkSafetyLimit()
    }

    // ---------- Settings ----------
    fun setThreshold(cents: Long) = settings.setThreshold(cents)

    // ---------- OCR flow ----------
    private val _scan = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scan: StateFlow<ScanUiState> = _scan.asStateFlow()

    fun onScanStarted() { _scan.value = ScanUiState.Processing }
    fun onTextRecognized(text: String) { _scan.value = ScanUiState.Review(ReceiptParser.parse(text)) }
    fun onScanFailed(message: String) { _scan.value = ScanUiState.Error(message) }
    fun resetScan() { _scan.value = ScanUiState.Idle }

    /** One-tap confirmation. Withdrawals become Bank -> Cash transfers, never expenses. */
    fun confirmScan(
        cents: Long, isWithdrawal: Boolean, account: AccountType, category: String, note: String
    ) = viewModelScope.launch {
        if (isWithdrawal) {
            repo.transfer(cents, AccountType.BANK, AccountType.CASH, note.ifBlank { "Retiro en cajero" }, true)
            checkSafetyLimit()
        } else {
            repo.addExpense(cents, account, category.ifBlank { "Otros" }, note, true)
            if (account == AccountType.BANK) checkSafetyLimit()
        }
        _scan.value = ScanUiState.Idle
    }

    // ---------- Safety limit ----------
    private suspend fun checkSafetyLimit() {
        val bank = repo.bankBalance()
        val threshold = settings.thresholdCents.value
        if (bank < threshold) {
            notifier.showLowBalance(bank, repo.pendingVault(monthKey()), threshold)
        }
    }
}

@Suppress("UNCHECKED_CAST")
class FinanceViewModelFactory(private val c: AppContainer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        FinanceViewModel(c.repository, c.settings, c.notifications) as T
}
