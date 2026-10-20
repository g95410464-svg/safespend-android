package com.safespend.app.data

import androidx.room.withTransaction

class FinanceRepository(private val db: AppDatabase) {
    private val dao = db.dao()

    val accounts = dao.observeAccounts()
    val transactions = dao.observeTransactions()
    val subscriptions = dao.observeSubscriptions()
    val savedCards = dao.observeSavedCards()

    suspend fun ensureAccounts() {
        AccountType.entries.forEach { dao.insertAccount(AccountEntity(it)) }
    }

    suspend fun addExpense(
        cents: Long, account: AccountType, category: String, note: String, fromReceipt: Boolean = false
    ) = db.withTransaction {
        dao.insertTransaction(
            TransactionEntity(
                kind = TxKind.EXPENSE, amountCents = cents, account = account,
                category = category, note = note, fromReceipt = fromReceipt
            )
        )
        dao.adjustBalance(account, -cents)
    }

    suspend fun addIncome(cents: Long, account: AccountType, category: String, note: String) =
        db.withTransaction {
            dao.insertTransaction(
                TransactionEntity(
                    kind = TxKind.INCOME, amountCents = cents, account = account,
                    category = category, note = note
                )
            )
            dao.adjustBalance(account, cents)
        }

    /** Internal movement (e.g. ATM withdrawal). NOT an expense: net worth stays the same. */
    suspend fun transfer(
        cents: Long, from: AccountType, to: AccountType, note: String, fromReceipt: Boolean = false
    ) = db.withTransaction {
        require(from != to) { "Source and destination must differ" }
        dao.insertTransaction(
            TransactionEntity(
                kind = TxKind.TRANSFER, amountCents = cents, account = from, toAccount = to,
                category = "Transferencia", note = note, fromReceipt = fromReceipt
            )
        )
        dao.adjustBalance(from, -cents)
        dao.adjustBalance(to, cents)
    }

    suspend fun paySubscription(sub: SubscriptionEntity, monthKey: String) = db.withTransaction {
        dao.insertTransaction(
            TransactionEntity(
                kind = TxKind.EXPENSE, amountCents = sub.amountCents, account = AccountType.BANK,
                category = SUBSCRIPTION_CATEGORY, note = sub.name
            )
        )
        dao.adjustBalance(AccountType.BANK, -sub.amountCents)
        dao.markPaid(sub.id, monthKey)
    }

    suspend fun bankBalance(): Long = dao.balanceOf(AccountType.BANK) ?: 0L

    /** Money still locked for subscriptions not yet paid in [monthKey]. */
    suspend fun pendingVault(monthKey: String): Long =
        dao.activeSubscriptions().filter { it.lastPaidMonth != monthKey }.sumOf { it.amountCents }

    suspend fun saveSubscription(sub: SubscriptionEntity) = dao.upsertSubscription(sub)
    suspend fun deleteSubscription(sub: SubscriptionEntity) = dao.deleteSubscription(sub)

    suspend fun saveCard(card: SavedCardEntity) = dao.insertSavedCard(card)
    suspend fun deleteCard(card: SavedCardEntity) = dao.deleteSavedCard(card)

    companion object { const val SUBSCRIPTION_CATEGORY = "Suscripción" }
}
