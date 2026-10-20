package com.safespend.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountType { BANK, CASH }
enum class TxKind { EXPENSE, INCOME, TRANSFER }

/** One row per wallet. Money is always stored in cents (Long) to avoid floating point errors. */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val type: AccountType,
    val balanceCents: Long = 0L
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: TxKind,
    val amountCents: Long,
    /** EXPENSE/INCOME: affected wallet. TRANSFER: source wallet. */
    val account: AccountType,
    /** Only for TRANSFER: destination wallet. */
    val toAccount: AccountType? = null,
    val category: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val fromReceipt: Boolean = false
)

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountCents: Long,
    val dayOfMonth: Int,
    val active: Boolean = true,
    /** "2026-10" when already paid this cycle; otherwise the amount stays locked in the Vault. */
    val lastPaidMonth: String? = null
)

/** Only the label and masked card metadata are stored in clear text. Secrets stay encrypted. */
@Entity(tableName = "saved_cards")
data class SavedCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val brand: String,
    val lastFour: String,
    val iv: ByteArray,
    val encryptedDetails: ByteArray
)
