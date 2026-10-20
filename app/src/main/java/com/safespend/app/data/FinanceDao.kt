package com.safespend.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    // Accounts
    @Query("SELECT * FROM accounts")
    fun observeAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT balanceCents FROM accounts WHERE type = :type")
    suspend fun balanceOf(type: AccountType): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccount(account: AccountEntity)

    @Query("UPDATE accounts SET balanceCents = balanceCents + :delta WHERE type = :type")
    suspend fun adjustBalance(type: AccountType, delta: Long)

    // Transactions
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Insert
    suspend fun insertTransaction(tx: TransactionEntity): Long

    // Subscriptions
    @Query("SELECT * FROM subscriptions ORDER BY dayOfMonth, name")
    fun observeSubscriptions(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE active = 1")
    suspend fun activeSubscriptions(): List<SubscriptionEntity>

    @Upsert
    suspend fun upsertSubscription(sub: SubscriptionEntity)

    @Delete
    suspend fun deleteSubscription(sub: SubscriptionEntity)

    @Query("UPDATE subscriptions SET lastPaidMonth = :month WHERE id = :id")
    suspend fun markPaid(id: Long, month: String)

    // Locally encrypted payment-card references.
    @Query("SELECT * FROM saved_cards ORDER BY id DESC")
    fun observeSavedCards(): Flow<List<SavedCardEntity>>

    @Insert
    suspend fun insertSavedCard(card: SavedCardEntity): Long

    @Delete
    suspend fun deleteSavedCard(card: SavedCardEntity)
}
