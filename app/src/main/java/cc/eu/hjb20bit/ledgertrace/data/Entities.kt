package cc.eu.hjb20bit.ledgertrace.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts", indices = [Index(value = ["name"], unique = true)])
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "balance_snapshots", indices = [Index(value = ["accountId", "date"], unique = true)])
data class BalanceSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val date: String,
    val amountCents: Long,
    val note: String = ""
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val type: String,
    val title: String,
    val category: String,
    val amountCents: Long,
    val accountId: Long? = null,
    val note: String = ""
)

@Entity(tableName = "recurring_entries")
data class RecurringEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String,
    val amountCents: Long,
    val dayOfMonth: Int,
    val startMonth: String,
    val endMonth: String? = null,
    val active: Boolean = true,
    val note: String = ""
)
