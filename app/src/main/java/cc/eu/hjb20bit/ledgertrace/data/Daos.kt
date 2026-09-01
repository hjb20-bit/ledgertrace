package cc.eu.hjb20bit.ledgertrace.data

import androidx.room.*

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY active DESC, name") suspend fun all(): List<AccountEntity>
    @Query("SELECT * FROM accounts WHERE active = 1 ORDER BY name") suspend fun active(): List<AccountEntity>
    @Query("SELECT * FROM accounts WHERE name = :name LIMIT 1") suspend fun byName(name: String): AccountEntity?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(account: AccountEntity): Long
    @Update suspend fun update(account: AccountEntity)
    @Query("UPDATE accounts SET active = 0 WHERE id = :id") suspend fun deactivate(id: Long)
}

@Dao
interface BalanceDao {
    @Query("SELECT * FROM balance_snapshots ORDER BY date DESC") suspend fun all(): List<BalanceSnapshotEntity>
    @Query("SELECT * FROM balance_snapshots WHERE accountId = :accountId ORDER BY date DESC") suspend fun byAccount(accountId: Long): List<BalanceSnapshotEntity>
    @Query("SELECT * FROM balance_snapshots WHERE accountId = :accountId AND date <= :date ORDER BY date DESC LIMIT 1") suspend fun latestAt(accountId: Long, date: String): BalanceSnapshotEntity?
    @Query("SELECT * FROM balance_snapshots WHERE accountId = :accountId ORDER BY date DESC LIMIT 1") suspend fun latest(accountId: Long): BalanceSnapshotEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(snapshot: BalanceSnapshotEntity): Long
    @Update suspend fun update(snapshot: BalanceSnapshotEntity)
    @Delete suspend fun delete(snapshot: BalanceSnapshotEntity)
    @Query("SELECT * FROM balance_snapshots WHERE id = :id LIMIT 1") suspend fun byId(id: Long): BalanceSnapshotEntity?
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC") suspend fun all(): List<TransactionEntity>
    @Insert suspend fun insert(item: TransactionEntity): Long
    @Update suspend fun update(item: TransactionEntity)
    @Delete suspend fun delete(item: TransactionEntity)
    @Query("SELECT * FROM transactions WHERE date BETWEEN :start AND :end ORDER BY date, id") suspend fun between(start: String, end: String): List<TransactionEntity>
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1") suspend fun byId(id: Long): TransactionEntity?
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_entries ORDER BY active DESC, type, dayOfMonth") suspend fun all(): List<RecurringEntryEntity>
    @Insert suspend fun insert(item: RecurringEntryEntity): Long
    @Update suspend fun update(item: RecurringEntryEntity)
    @Delete suspend fun delete(item: RecurringEntryEntity)
    @Query("UPDATE recurring_entries SET active = :active WHERE id = :id") suspend fun setActive(id: Long, active: Boolean)
}
