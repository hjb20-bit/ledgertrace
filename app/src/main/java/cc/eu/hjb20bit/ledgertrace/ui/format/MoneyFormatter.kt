package cc.eu.hjb20bit.ledgertrace.ui.format

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

object MoneyFormatter {
    fun parseCents(value: String): Long? = try { BigDecimal(value.trim().replace(",", "")).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact().takeIf { it >= 0 } } catch (_: Exception) { null }
    fun format(cents: Long): String = "¥" + BigDecimal.valueOf(cents, 2).setScale(2, RoundingMode.UNNECESSARY).toPlainString()
}
