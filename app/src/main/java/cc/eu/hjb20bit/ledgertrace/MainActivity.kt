package cc.eu.hjb20bit.ledgertrace

import android.app.*
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import cc.eu.hjb20bit.ledgertrace.data.*
import cc.eu.hjb20bit.ledgertrace.ui.LedgerViewModel
import cc.eu.hjb20bit.ledgertrace.ui.format.MarkdownExporter
import cc.eu.hjb20bit.ledgertrace.ui.format.MoneyFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private val vm: LedgerViewModel by lazy {
        ViewModelProvider(this)[LedgerViewModel::class.java]
    }
    private lateinit var content: LinearLayout
    private lateinit var pageTitle: TextView
    private lateinit var pageSubtitle: TextView
    private val navViews = mutableListOf<Triple<ImageView, TextView, Page?>>()
    private var page = Page.HOME
    private var pendingMarkdown = ""
    private val createMarkdownDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        uri ?: return@registerForActivityResult
        contentResolver.openOutputStream(uri)?.use { output ->
            output.write(pendingMarkdown.toByteArray(Charsets.UTF_8))
        }
        toast("Markdown 已导出")
    }
    private val primary = Color.rgb(15, 91, 82)
    private val bg = Color.rgb(245, 248, 247)
    private val ink = Color.rgb(27, 40, 38)
    private val muted = Color.rgb(113, 128, 125)
    private val green = Color.rgb(32, 132, 93)
    private val red = Color.rgb(195, 79, 79)
    enum class Page { HOME, BALANCES, TRANSACTIONS, RECURRING, MORE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        buildRoot()
        lifecycleScope.launch { vm.state.collect { render() } }
    }

    private fun buildRoot() {
        val root = findViewById<FrameLayout>(R.id.main)
        root.removeAllViews()
        val shell = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(22), dp(24), dp(10)) }
        pageTitle = tv("账迹", 28f, ink, true); pageSubtitle = tv("记录每一笔，也看清每一步", 13f, muted, false)
        header.addView(pageTitle); header.addView(pageSubtitle)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), 0, dp(24), dp(24)) }
        shell.addView(header)
        shell.addView(ScrollView(this).apply { addView(content); layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) })
        shell.addView(nav())
        root.addView(shell)
    }

    private fun nav(): View {
        navViews.clear()
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.WHITE); elevation = dp(4).toFloat() }
        val items = listOf("首页" to Page.HOME, "余额" to Page.BALANCES, "记账" to null, "固定收支" to Page.RECURRING, "更多" to Page.MORE)
        items.forEach { (label, target) ->
            val b = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(0, dp(5), 0, dp(4)); layoutParams = LinearLayout.LayoutParams(0, dp(68), 1f) }
            val icon = ImageView(this).apply { setImageResource(iconFor(label)); layoutParams = LinearLayout.LayoutParams(dp(if (label == "记账") 34 else 22), dp(if (label == "记账") 34 else 22)) }
            val text = tv(label, 11f, if (target == page) primary else muted, target == page)
            b.addView(icon); b.addView(text)
            b.setOnClickListener { if (target == null) showTransactionDialog(null) else { page = target; render() } }
            navViews.add(Triple(icon, text, target))
            bar.addView(b)
        }
        return bar
    }

    private fun iconFor(label: String): Int = when (label) {
        "首页" -> R.drawable.ic_home; "余额" -> R.drawable.ic_wallet; "记账" -> R.drawable.ic_add; "固定收支" -> R.drawable.ic_repeat; else -> R.drawable.ic_more
    }

    private fun render() {
        if (!::content.isInitialized) return
        navViews.forEach { (icon, label, target) -> val color = if (target == null || target == page) primary else muted; icon.setColorFilter(color); label.setTextColor(color); label.setTypeface(null, if (target == page) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL) }
        content.removeAllViews()
        when (page) {
            Page.HOME -> { pageTitle.text = "账迹"; pageSubtitle.text = "记录每一笔，也看清每一步"; renderHome() }
            Page.BALANCES -> { pageTitle.text = "余额快照"; pageSubtitle.text = "按账户记录每次余额"; renderBalances() }
            Page.TRANSACTIONS -> { pageTitle.text = "记一笔"; pageSubtitle.text = "记录实际发生的收入与支出"; renderTransactions() }
            Page.RECURRING -> { pageTitle.text = "固定收支"; pageSubtitle.text = "记录每月固定收入和支出"; renderRecurring() }
            Page.MORE -> { pageTitle.text = "更多"; pageSubtitle.text = "数据与应用设置"; renderMore() }
        }
    }

    private fun renderHome() {
        val s = vm.state.value
        val total = s.accounts.filter { it.active }.sumOf { a -> s.balances.filter { it.accountId == a.id }.maxByOrNull { it.date }?.amountCents ?: 0 }
        val hero = card(primary).apply { setPadding(dp(20), dp(18), dp(20), dp(18)); addView(tv("总资产", 13f, Color.WHITE, false)); addView(tv(MoneyFormatter.format(total), 32f, Color.WHITE, true)); addView(tv("本地离线保存 · CNY", 12f, Color.WHITE, false)) }
        content.addView(hero, margins(0, 14, 0, 0))
        section("本月概况", null)
        val start = monthStart(); val end = today(); val stats = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        s.accounts.filter { it.active }.forEach { a ->
            val begin = s.balances.filter { it.accountId == a.id && it.date in start..end }.minByOrNull { it.date }
            val finish = s.balances.filter { it.accountId == a.id && it.date <= end }.maxByOrNull { it.date }
            val delta = if (begin != null && finish != null) finish.amountCents - begin.amountCents else null
            stats.addView(tv("${a.name}    ${delta?.let { (if (it >= 0) "+" else "") + MoneyFormatter.format(it) } ?: "数据不足"}", 14f, if ((delta ?: 0) >= 0) green else red, true), margins(4, 5, 4, 5))
        }
        if (s.accounts.none { it.active }) stats.addView(empty("暂无账户余额快照"))
        content.addView(stats)
        section("账户余额") { page = Page.BALANCES; render() }
        val ac = card(Color.WHITE)
        s.accounts.filter { it.active }.forEach { a -> ac.addView(accountRow(a.name, s.balances.filter { it.accountId == a.id }.maxByOrNull { it.date }?.amountCents ?: 0, s.balances.filter { it.accountId == a.id }.maxByOrNull { it.date }?.date ?: "")) }
        if (s.accounts.none { it.active }) ac.addView(empty("还没有账户，请先记录余额"))
        content.addView(ac)
        section("最近流水") { page = Page.TRANSACTIONS; render() }
        val tx = card(Color.WHITE); s.transactions.take(3).forEach { tx.addView(transactionRow(it)) }; if (s.transactions.isEmpty()) tx.addView(empty("暂无流水，点击底部记账开始")); content.addView(tx)
    }

    private fun renderBalances() {
        val s = vm.state.value
        content.addView(button("＋ 记录余额", -1) { showBalanceDialog(null) }, margins(0, 14, 0, 10)); section("余额记录", null)
        val list = card(Color.WHITE)
        s.balances.sortedByDescending { it.date }.forEach { snap -> val name = s.accounts.firstOrNull { it.id == snap.accountId }?.name ?: "未知账户"; list.addView(accountRow(name, snap.amountCents, snap.date).apply { setOnClickListener { balanceActions(snap) } }) }
        if (s.balances.isEmpty()) list.addView(empty("首次使用：记录微信、支付宝或其他账户余额")); content.addView(list)
        section("区间比较", null)
        val start = dateEdit("起始日期", monthStart()); val end = dateEdit("结束日期", today()); val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }; row.addView(start, LinearLayout.LayoutParams(0, dp(54), 1f)); row.addView(Space(this), LinearLayout.LayoutParams(dp(8), 1)); row.addView(end, LinearLayout.LayoutParams(0, dp(54), 1f)); content.addView(row)
        val result = tv("选择日期后，按账户计算净变化", 13f, muted, false); content.addView(button("比较余额变化", -1) { result.text = compare(start.text.toString(), end.text.toString()) }, margins(0, 10, 0, 0)); content.addView(result, margins(4, 12, 4, 0))
    }

    private fun renderTransactions() { val s = vm.state.value; content.addView(button("＋ 记一笔", -1) { showTransactionDialog(null) }, margins(0, 14, 0, 10)); val list = card(Color.WHITE); s.transactions.forEach { transaction -> list.addView(transactionRow(transaction).apply { setOnClickListener { transactionActions(transaction) } }) }; if (s.transactions.isEmpty()) list.addView(empty("还没有实际收入或支出")); content.addView(list) }
    private fun renderRecurring() { val s = vm.state.value; content.addView(button("＋ 新增固定收支", -1) { showRecurringDialog(null) }, margins(0, 14, 0, 10)); val list = card(Color.WHITE); s.recurring.forEach { recurring -> list.addView(recurringRow(recurring).apply { setOnClickListener { recurringActions(recurring) } }) }; if (s.recurring.isEmpty()) list.addView(empty("还没有固定收入或支出")); content.addView(list); content.addView(tv("固定收支仅作为计划，不会自动生成实际流水。", 12f, muted, false), margins(4, 12, 4, 0)) }
    private fun renderMore() { val c = card(Color.WHITE).apply { setPadding(dp(16), dp(15), dp(16), dp(15)); addView(tv("Markdown 导出", 16f, ink, true)); addView(tv("选择时间段并生成财务报告", 12f, muted, false)); setOnClickListener { showExportDialog() } }; content.addView(c, margins(0, 14, 0, 0)); content.addView(tv("LedgerTrace · 本地离线记账", 12f, muted, false), margins(4, 28, 4, 0)) }

    private fun exportOptions(start: EditText, end: EditText, box: LinearLayout) {
        val balance = CheckBox(this).apply { text = "包含余额变化"; isChecked = true }
        val income = CheckBox(this).apply { text = "包含实际收入"; isChecked = true }
        val expense = CheckBox(this).apply { text = "包含实际支出"; isChecked = true }
        val recurring = CheckBox(this).apply { text = "包含固定收支"; isChecked = true }
        listOf(balance, income, expense, recurring).forEach { box.addView(it) }
        box.tag = arrayOf(balance, income, expense, recurring, start, end)
    }

    private fun accountRow(name: String, cents: Long, date: String) = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(14), dp(11), dp(14), dp(11)); addView(tv(name.take(1), 15f, primary, true), LinearLayout.LayoutParams(dp(36), dp(36))); val info = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, 0, 0); layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }; info.addView(tv(name, 14f, ink, true)); info.addView(tv(if (date.isBlank()) "暂无快照" else "最近快照：$date", 11f, muted, false)); addView(info); addView(tv(MoneyFormatter.format(cents), 14f, ink, true)) }
    private fun transactionRow(t: TransactionEntity) = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(14), dp(11), dp(14), dp(11)); addView(tv(if (t.type == "INCOME") "入" else "出", 13f, if (t.type == "INCOME") green else red, true), LinearLayout.LayoutParams(dp(28), -2)); val info = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), 0, 0, 0); layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }; info.addView(tv(t.title, 14f, ink, true)); info.addView(tv("${t.date} · ${t.category}", 11f, muted, false)); addView(info); addView(tv((if (t.type == "INCOME") "+ " else "− ") + MoneyFormatter.format(t.amountCents), 14f, if (t.type == "INCOME") green else red, true)) }
    private fun recurringRow(p: RecurringEntryEntity) = transactionRow(TransactionEntity(date = "每月 ${p.dayOfMonth} 日", type = p.type, title = p.title, category = "固定收支", amountCents = p.amountCents)).apply { alpha = if (p.active) 1f else .45f }
    private fun section(name: String, action: (() -> Unit)?) { val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }; row.addView(tv(name, 17f, ink, true), LinearLayout.LayoutParams(0, -2, 1f)); action?.let { row.addView(button("查看全部", -2, it)) }; content.addView(row, margins(0, 22, 0, 10)) }

    private fun showBalanceDialog(existing: BalanceSnapshotEntity?) { val s = vm.state.value; val box = form(); val account = edit("账户名称", existing?.let { s.accounts.firstOrNull { a -> a.id == it.accountId }?.name ?: "" } ?: ""); val amount = edit("余额，例如 3000.00", existing?.let { "%.2f".format(Locale.US, it.amountCents / 100.0) } ?: ""); amount.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL; val date = dateEdit("日期", existing?.date ?: today()); val note = edit("备注（可选）", existing?.note ?: ""); listOf(account, amount, date, note).forEach { box.addView(it) }; dialog(if (existing == null) "记录余额" else "编辑余额", box) { val cents = MoneyFormatter.parseCents(amount.text.toString()); if (account.text.isNullOrBlank() || cents == null) { toast("请填写账户名称和合法金额"); return@dialog }; vm.saveSnapshot(existing?.id ?: 0, account.text.toString().trim(), date.text.toString().trim(), cents, note.text.toString()); page = Page.BALANCES } }
    private fun balanceActions(item: BalanceSnapshotEntity) = AlertDialog.Builder(this).setItems(arrayOf("编辑", "删除", "取消")) { _, which -> when (which) { 0 -> showBalanceDialog(item); 1 -> confirm("删除这条余额快照？") { vm.deleteSnapshot(item) } } }.show()
    private fun showTransactionDialog(existing: TransactionEntity?) { val s = vm.state.value; val box = form(); val type = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, arrayOf("收入", "支出")); setSelection(if (existing?.type == "EXPENSE") 1 else 0) }; val amount = edit("金额，例如 0.07", existing?.let { "%.2f".format(Locale.US, it.amountCents / 100.0) } ?: ""); amount.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL; val name = edit("项目名称", existing?.title ?: ""); val category = edit("分类", existing?.category ?: "其他"); val account = edit("账户（可选）", existing?.accountId?.let { id -> s.accounts.firstOrNull { it.id == id }?.name } ?: ""); val date = dateEdit("日期", existing?.date ?: today()); val note = edit("备注（可选）", existing?.note ?: ""); listOf(type, amount, name, category, account, date, note).forEach { box.addView(it) }; dialog(if (existing == null) "记一笔" else "编辑流水", box) { val cents = MoneyFormatter.parseCents(amount.text.toString()); if (name.text.isNullOrBlank() || cents == null) { toast("请填写项目名称和合法金额"); return@dialog }; vm.saveTransaction(TransactionEntity(existing?.id ?: 0, date.text.toString().trim(), if (type.selectedItemPosition == 0) "INCOME" else "EXPENSE", name.text.toString().trim(), category.text.toString().trim().ifBlank { "其他" }, cents, existing?.accountId, note.text.toString()), account.text.toString()); page = Page.TRANSACTIONS } }
    private fun transactionActions(item: TransactionEntity) = AlertDialog.Builder(this).setItems(arrayOf("编辑", "删除", "取消")) { _, which -> when (which) { 0 -> showTransactionDialog(item); 1 -> confirm("删除这条流水？") { vm.deleteTransaction(item) } } }.show()
    private fun showRecurringDialog(existing: RecurringEntryEntity?) { val box = form(); val type = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, arrayOf("收入", "支出")); setSelection(if (existing?.type == "EXPENSE") 1 else 0) }; val name = edit("项目名称", existing?.title ?: ""); val amount = edit("金额", existing?.let { "%.2f".format(Locale.US, it.amountCents / 100.0) } ?: ""); amount.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL; val day = edit("每月发生日（1-31）", existing?.dayOfMonth?.toString() ?: "1"); day.inputType = InputType.TYPE_CLASS_NUMBER; val start = edit("生效月份 yyyy-MM", existing?.startMonth ?: monthStart().substring(0, 7)); val note = edit("备注（可选）", existing?.note ?: ""); listOf(type, name, amount, day, start, note).forEach { box.addView(it) }; dialog(if (existing == null) "新增固定收支" else "编辑固定收支", box) { val cents = MoneyFormatter.parseCents(amount.text.toString()); val d = day.text.toString().toIntOrNull(); if (name.text.isNullOrBlank() || cents == null || d == null || d !in 1..31) { toast("请填写合法的项目、金额和日期"); return@dialog }; vm.saveRecurring(RecurringEntryEntity(existing?.id ?: 0, name.text.toString().trim(), if (type.selectedItemPosition == 0) "INCOME" else "EXPENSE", cents, d, start.text.toString().trim(), note = note.text.toString())); page = Page.RECURRING } }
    private fun recurringActions(item: RecurringEntryEntity) = AlertDialog.Builder(this).setItems(arrayOf("编辑", if (item.active) "停用" else "启用", "删除", "取消")) { _, which -> when (which) { 0 -> showRecurringDialog(item); 1 -> vm.toggleRecurring(item); 2 -> confirm("删除这条固定收支？") { vm.deleteRecurring(item) } } }.show()
    private fun showExportDialog() { val box = form(); val start = dateEdit("起始日期", monthStart()); val end = dateEdit("结束日期", today()); box.addView(start); box.addView(end); exportOptions(start, end, box); dialog("导出 Markdown", box) { val checks = box.tag as Array<*>; pendingMarkdown = MarkdownExporter.create(start.text.toString(), end.text.toString(), vm.state.value.accounts, vm.state.value.balances, vm.state.value.transactions, vm.state.value.recurring, (checks[0] as CheckBox).isChecked, (checks[1] as CheckBox).isChecked, (checks[2] as CheckBox).isChecked, (checks[3] as CheckBox).isChecked); createMarkdownDocument.launch("ledgertrace-report.md") } }
    private fun compare(start: String, end: String): String { val s = vm.state.value; return s.accounts.filter { it.active }.joinToString("\n") { a -> val b = s.balances.filter { it.accountId == a.id && it.date <= start }.maxByOrNull { it.date }; val e = s.balances.filter { it.accountId == a.id && it.date <= end }.maxByOrNull { it.date }; if (b == null || e == null) "${a.name}：数据不足" else "${a.name}：${if (e.amountCents - b.amountCents >= 0) "+" else ""}${MoneyFormatter.format(e.amountCents - b.amountCents)}" } }
    private fun form() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), 0, dp(20), 0) }
    private fun dialog(t: String, v: View, save: () -> Unit) { AlertDialog.Builder(this).setTitle(t).setView(v).setNegativeButton("取消", null).setPositiveButton("保存") { _, _ -> save() }.show() }
    private fun confirm(message: String, yes: () -> Unit) { AlertDialog.Builder(this).setMessage(message).setNegativeButton("取消", null).setPositiveButton("确认") { _, _ -> yes() }.show() }
    private fun edit(hint: String, value: String) = EditText(this).apply { this.hint = hint; setText(value); setPadding(0, dp(8), 0, dp(8)) }
    private fun dateEdit(hint: String, value: String) = edit(hint, value).apply { isFocusable = false; setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_calendar, 0); setOnClickListener { showDatePicker(this) } }
    private fun showDatePicker(target: EditText) { val cal = Calendar.getInstance(); runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(target.text.toString()) }.getOrNull()?.let { cal.time = it }; DatePickerDialog(this, { _, year, month, day -> target.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)) }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show() }
    private fun tv(value: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply { text = value; textSize = size; setTextColor(color); if (bold) setTypeface(null, android.graphics.Typeface.BOLD) }
    private fun button(label: String, width: Int, click: () -> Unit) = Button(this).apply { text = label; textSize = 14f; setTextColor(primary); setOnClickListener { click() }; if (width > 0) layoutParams = LinearLayout.LayoutParams(dp(width), dp(46)) }
    private fun card(color: Int) = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(color) }
    private fun empty(value: String) = tv(value, 13f, muted, false).apply { gravity = Gravity.CENTER; setPadding(dp(12), dp(26), dp(12), dp(26)) }
    private fun margins(l: Int, t: Int, r: Int, b: Int) = LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(l), dp(t), dp(r), dp(b)) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    private fun monthStart() = today().substring(0, 8) + "01"
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
