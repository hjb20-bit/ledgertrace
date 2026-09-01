package cc.eu.hjb20bit.ledgertrace.ui.format

import cc.eu.hjb20bit.ledgertrace.data.*

object MarkdownExporter {
    fun create(start: String, end: String, accounts: List<AccountEntity>, balances: List<BalanceSnapshotEntity>, transactions: List<TransactionEntity>, recurring: List<RecurringEntryEntity>, includeBalances: Boolean = true, includeIncome: Boolean = true, includeExpense: Boolean = true, includeRecurring: Boolean = true): String {
        val sb = StringBuilder("# 账迹 LedgerTrace 财务报告\n\n- 统计时间：$start 至 $end\n- 货币：CNY\n")
        if (includeBalances) { sb.append("\n## 账户余额变化\n\n| 账户 | 起始余额 | 结束余额 | 净变化 |\n|---|---:|---:|---:|\n"); accounts.filter { it.active }.forEach { a ->
            val list = balances.filter { it.accountId == a.id }.sortedBy { it.date }
            val begin = list.firstOrNull { it.date == start }; val finish = list.firstOrNull { it.date == end }
            sb.append("| ${a.name} | ${begin?.let { MoneyFormatter.format(it.amountCents) } ?: "数据不足"} | ${finish?.let { MoneyFormatter.format(it.amountCents) } ?: "数据不足"} | ${if (begin != null && finish != null) MoneyFormatter.format(finish.amountCents - begin.amountCents) else "数据不足"} |\n")
        } }
        fun section(title: String, rows: List<TransactionEntity>) { sb.append("\n## $title\n\n| 日期 | 项目 | 金额 | 备注 |\n|---|---|---:|---|\n"); rows.forEach { sb.append("| ${it.date} | ${it.title} | ${MoneyFormatter.format(it.amountCents)} | ${it.note.replace("|", "\\|")} |\n") } }
        if (includeIncome) section("实际收入", transactions.filter { it.type == "INCOME" && it.date in start..end }); if (includeExpense) section("实际支出", transactions.filter { it.type == "EXPENSE" && it.date in start..end })
        if (includeRecurring) { sb.append("\n## 固定收支\n\n| 项目 | 类型 | 金额 | 每月日期 |\n|---|---|---:|---:|\n"); recurring.filter { it.active }.forEach { sb.append("| ${it.title} | ${it.type} | ${MoneyFormatter.format(it.amountCents)} | ${it.dayOfMonth} |\n") } }
        sb.append("\n## 数据说明\n\n余额变化基于已记录的余额快照，不能反映未记录期间的中间变化。固定收支仅为计划，不计入实际收支。\n")
        return sb.toString()
    }
}
