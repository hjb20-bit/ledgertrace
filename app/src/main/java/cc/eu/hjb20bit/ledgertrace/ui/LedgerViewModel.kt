package cc.eu.hjb20bit.ledgertrace.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.eu.hjb20bit.ledgertrace.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LedgerState(
    val accounts: List<AccountEntity> = emptyList(),
    val balances: List<BalanceSnapshotEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val recurring: List<RecurringEntryEntity> = emptyList()
)

class LedgerViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = LedgerRepository(AppDatabase.get(app))
    private val _state = MutableStateFlow(LedgerState())
    val state: StateFlow<LedgerState> = _state.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch { _state.value = LedgerState(repo.accounts.all(), repo.balances.all(), repo.transactions.all(), repo.recurring.all()) }
    fun saveSnapshot(id: Long, account: String, date: String, cents: Long, note: String) = viewModelScope.launch { repo.saveSnapshot(BalanceSnapshotEntity(id = id, accountId = 0, date = date, amountCents = cents, note = note), account); refresh() }
    fun deleteSnapshot(item: BalanceSnapshotEntity) = viewModelScope.launch { repo.balances.delete(item); refresh() }
    fun saveTransaction(item: TransactionEntity, accountName: String) = viewModelScope.launch { repo.saveTransaction(item, accountName); refresh() }
    fun deleteTransaction(item: TransactionEntity) = viewModelScope.launch { repo.transactions.delete(item); refresh() }
    fun saveRecurring(item: RecurringEntryEntity) = viewModelScope.launch { if (item.id == 0L) repo.recurring.insert(item) else repo.recurring.update(item); refresh() }
    fun toggleRecurring(item: RecurringEntryEntity) = viewModelScope.launch { repo.recurring.setActive(item.id, !item.active); refresh() }
    fun deleteRecurring(item: RecurringEntryEntity) = viewModelScope.launch { repo.recurring.delete(item); refresh() }
}
