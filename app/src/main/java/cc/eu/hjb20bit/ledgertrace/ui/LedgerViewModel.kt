package cc.eu.hjb20bit.ledgertrace.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import cc.eu.hjb20bit.ledgertrace.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(LedgerState())
    val state: StateFlow<LedgerState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() = scope.launch { refreshState() }

    fun saveSnapshot(id: Long, account: String, date: String, cents: Long, note: String) = scope.launch {
        repo.saveSnapshot(BalanceSnapshotEntity(id = id, accountId = 0, date = date, amountCents = cents, note = note), account)
        refreshState()
    }

    fun deleteSnapshot(item: BalanceSnapshotEntity) = scope.launch {
        repo.balances.delete(item)
        refreshState()
    }

    fun saveTransaction(item: TransactionEntity, accountName: String) = scope.launch {
        repo.saveTransaction(item, accountName)
        refreshState()
    }

    fun deleteTransaction(item: TransactionEntity) = scope.launch {
        repo.transactions.delete(item)
        refreshState()
    }

    fun saveRecurring(item: RecurringEntryEntity) = scope.launch {
        if (item.id == 0L) repo.recurring.insert(item) else repo.recurring.update(item)
        refreshState()
    }

    fun toggleRecurring(item: RecurringEntryEntity) = scope.launch {
        repo.recurring.setActive(item.id, !item.active)
        refreshState()
    }

    fun deleteRecurring(item: RecurringEntryEntity) = scope.launch {
        repo.recurring.delete(item)
        refreshState()
    }

    private suspend fun refreshState() {
        _state.value = LedgerState(
            accounts = repo.accounts.all(),
            balances = repo.balances.all(),
            transactions = repo.transactions.all(),
            recurring = repo.recurring.all()
        )
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }
}
