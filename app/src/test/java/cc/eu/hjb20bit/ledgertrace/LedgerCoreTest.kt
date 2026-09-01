package cc.eu.hjb20bit.ledgertrace

import cc.eu.hjb20bit.ledgertrace.data.*
import cc.eu.hjb20bit.ledgertrace.ui.format.MarkdownExporter
import cc.eu.hjb20bit.ledgertrace.ui.format.MoneyFormatter
import org.junit.Assert.*
import org.junit.Test

class LedgerCoreTest {
    @Test fun parsesCentsExactly() {
        assertEquals(7L, MoneyFormatter.parseCents("0.07"))
        assertEquals(300000L, MoneyFormatter.parseCents("3,000.00"))
        assertNull(MoneyFormatter.parseCents("-1"))
    }

    @Test fun markdownUsesSnapshotChangesAndTransactionSections() {
        val accounts = listOf(AccountEntity(id = 1, name = "微信"))
        val balances = listOf(
            BalanceSnapshotEntity(id = 1, accountId = 1, date = "2026-07-24", amountCents = 400000),
            BalanceSnapshotEntity(id = 2, accountId = 1, date = "2026-08-31", amountCents = 300000)
        )
        val text = MarkdownExporter.create("2026-07-24", "2026-08-31", accounts, balances, emptyList(), emptyList())
        assertTrue(text.contains("微信"))
        assertTrue(text.contains("¥-1000.00"))
        assertTrue(text.contains("## 实际收入"))
        assertTrue(text.contains("固定收支仅为计划"))
    }
}
