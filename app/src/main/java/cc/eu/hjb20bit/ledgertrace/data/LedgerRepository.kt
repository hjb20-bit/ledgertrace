package cc.eu.hjb20bit.ledgertrace.data

class LedgerRepository(private val db: AppDatabase) {
    val accounts = db.accountDao()
    val balances = db.balanceDao()
    val transactions = db.transactionDao()
    val recurring = db.recurringDao()

    suspend fun accountFor(name: String): AccountEntity {
        accounts.byName(name)?.let { return it }
        val id = accounts.insert(AccountEntity(name = name))
        return accounts.byName(name) ?: AccountEntity(id = id, name = name)
    }

    suspend fun saveSnapshot(snapshot: BalanceSnapshotEntity, accountName: String): BalanceSnapshotEntity {
        val account = accountFor(accountName)
        val existing = balances.byAccount(account.id).firstOrNull { it.date == snapshot.date }
        val original = snapshot.id.takeIf { it != 0L }?.let { balances.byId(it) }
        if (original != null && (original.accountId != account.id || original.date != snapshot.date)) balances.delete(original)
        val saved = snapshot.copy(id = existing?.id ?: snapshot.id, accountId = account.id)
        balances.upsert(saved)
        return saved
    }

    suspend fun saveTransaction(item: TransactionEntity, accountName: String) {
        val accountId = accountName.trim().takeIf { it.isNotEmpty() }?.let { accountFor(it).id }
        val saved = item.copy(accountId = accountId)
        if (saved.id == 0L) transactions.insert(saved) else transactions.update(saved)
    }
}
