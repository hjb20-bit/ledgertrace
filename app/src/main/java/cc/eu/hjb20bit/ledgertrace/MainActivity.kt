package cc.eu.hjb20bit.ledgertrace

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val primary = Color.rgb(15, 91, 82)
    private val primarySoft = Color.rgb(220, 239, 233)
    private val background = Color.rgb(245, 248, 247)
    private val ink = Color.rgb(27, 40, 38)
    private val muted = Color.rgb(113, 128, 125)
    private val green = Color.rgb(32, 132, 93)
    private val red = Color.rgb(195, 79, 79)
    private lateinit var content: LinearLayout
    private lateinit var pageTitle: TextView
    private lateinit var pageSubtitle: TextView
    private lateinit var prefs: android.content.SharedPreferences
    private var page = Page.HOME
    private var pendingMarkdown = ""
    private val accounts = mutableListOf<Account>()
    private val snapshots = mutableListOf<BalanceSnapshot>()
    private val transactions = mutableListOf<Transaction>()
    private val plans = mutableListOf<RecurringPlan>()

    data class Account(val name: String, var active: Boolean = true)
    data class BalanceSnapshot(val date: String, val account: String, val cents: Long, val note: String)
    data class Transaction(val date: String, val title: String, val category: String, val cents: Long, val income: Boolean, val note: String = "")
    data class RecurringPlan(val title: String, val day: Int, val cents: Long, val income: Boolean, var active: Boolean = true)
    enum class Page { HOME, BALANCES, TRANSACTIONS, PLANS, MORE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("ledgertrace_data", MODE_PRIVATE)
        loadData()
        setContentView(R.layout.activity_main)
        val frame = findViewById<FrameLayout>(R.id.main)
        frame.removeAllViews()
        frame.addView(buildRoot())
    }

    private fun buildRoot(): View {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(this@MainActivity.background) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(24), dp(24), dp(12)) }
        pageTitle = textView("\u8d26\u8ff9", 28f, ink, true)
        pageSubtitle = textView("\u8bb0\u5f55\u6bcf\u4e00\u7b14\uff0c\u4e5f\u770b\u6e05\u6bcf\u4e00\u6b65", 13f, muted, false)
        header.addView(pageTitle); header.addView(pageSubtitle)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), 0, dp(24), dp(24)) }
        val scroll = ScrollView(this).apply { addView(content); layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.WHITE) }
        listOf("\u9996\u9875" to Page.HOME, "\u4f59\u989d" to Page.BALANCES, "+" to null, "\u8ba1\u5212" to Page.PLANS, "\u66f4\u591a" to Page.MORE).forEach { (label, target) ->
            nav.addView(Button(this).apply {
                text = label; textSize = 12f; setTextColor(primary); setBackgroundColor(Color.TRANSPARENT)
                layoutParams = LinearLayout.LayoutParams(0, dp(64), 1f)
                setOnClickListener { if (target == null) showTransactionDialog() else { page = target; render() } }
            })
        }
        root.addView(header); root.addView(scroll); root.addView(nav)
        render()
        return root
    }

    private fun render() {
        content.removeAllViews()
        when (page) {
            Page.HOME -> { pageTitle.text = "\u8d26\u8ff9"; pageSubtitle.text = "\u8bb0\u5f55\u6bcf\u4e00\u7b14\uff0c\u4e5f\u770b\u6e05\u6bcf\u4e00\u6b65"; renderHome() }
            Page.BALANCES -> { pageTitle.text = "\u4f59\u989d\u5feb\u7167"; pageSubtitle.text = "\u6309\u8d26\u6237\u8bb0\u5f55\u6bcf\u65e5\u4f59\u989d"; renderBalances() }
            Page.TRANSACTIONS -> { pageTitle.text = "\u6536\u652f\u6d41\u6c34"; pageSubtitle.text = "\u8bb0\u5f55\u5b9e\u9645\u53d1\u751f\u7684\u6536\u5165\u4e0e\u652f\u51fa"; renderTransactions() }
            Page.PLANS -> { pageTitle.text = "\u56fa\u5b9a\u8ba1\u5212"; pageSubtitle.text = "\u53ea\u505a\u63d0\u9192\uff0c\u4e0d\u81ea\u52a8\u8ba1\u5165\u5b9e\u9645\u6536\u652f"; renderPlans() }
            Page.MORE -> { pageTitle.text = "\u66f4\u591a"; pageSubtitle.text = "\u6570\u636e\u4e0e\u5e94\u7528\u8bbe\u7f6e"; renderMore() }
        }
    }

    private fun renderHome() {
        val total = accounts.filter { it.active }.sumOf { latestBalance(it.name) }
        val income = transactions.filter { it.income }.sumOf { it.cents }
        val expense = transactions.filter { !it.income }.sumOf { it.cents }
        val hero = card(primary); hero.setPadding(dp(20), dp(18), dp(20), dp(18)); hero.addView(textView("\u603b\u8d44\u4ea7", 13f, Color.WHITE, false)); hero.addView(textView(money(total), 32f, Color.WHITE, true)); hero.addView(textView("\u672c\u5730\u79bb\u7ebf\u4fdd\u5b58 \u00b7 CNY", 12f, Color.WHITE, false)); content.addView(hero, margins(0, 14, 0, 0))
        section("\u672c\u6708\u6982\u89c8", null); val stats = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }; stat(stats, "\u6536\u5165", income, green); stat(stats, "\u652f\u51fa", expense, red); stat(stats, "\u51c0\u7ed3\u4f59", income - expense, primary); content.addView(stats)
        section("\u8d26\u6237\u4f59\u989d") { page = Page.BALANCES; render() }; val accountCard = card(Color.WHITE); if (accounts.none { it.active }) accountCard.addView(empty("\u8fd8\u6ca1\u6709\u8d26\u6237\uff0c\u8bf7\u5148\u8bb0\u5f55\u4f59\u989d")) else accounts.filter { it.active }.forEach { accountCard.addView(accountRow(it.name, latestBalance(it.name))) }; content.addView(accountCard)
        section("\u6700\u8fd1\u8bb0\u5f55") { page = Page.TRANSACTIONS; render() }; val recent = card(Color.WHITE); if (transactions.isEmpty()) recent.addView(empty("\u8fd8\u6ca1\u6709\u6536\u652f\u8bb0\u5f55\uff0c\u70b9\u51fb\u5e95\u90e8\u8bb0\u8d26\u5f00\u59cb")) else transactions.sortedByDescending { it.date }.take(3).forEach { recent.addView(transactionRow(it)) }; content.addView(recent)
    }

    private fun renderBalances() {
        content.addView(button("+ \u8bb0\u5f55\u4f59\u989d", -1, 46) { showBalanceDialog(null) }, margins(0, 14, 0, 8)); section("\u8d26\u6237\u4f59\u989d", null)
        val list = card(Color.WHITE); if (accounts.none { it.active }) list.addView(empty("\u9996\u6b21\u4f7f\u7528\uff1a\u8bb0\u5f55\u5fae\u4fe1\u3001\u652f\u4ed8\u5b9d\u6216\u5176\u4ed6\u8d26\u6237\u4f59\u989d")) else accounts.filter { it.active }.forEach { a -> list.addView(accountRow(a.name, latestBalance(a.name)).apply { setOnClickListener { showBalanceDialog(a.name) } }) }; content.addView(list)
        section("\u533a\u95f4\u6bd4\u8f83", null); val start = editText("\u8d77\u59cb\u65e5\u671f", "2026-07-24"); val end = editText("\u7ed3\u675f\u65e5\u671f", today()); val dates = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }; dates.addView(start, LinearLayout.LayoutParams(0, dp(56), 1f)); dates.addView(Space(this), LinearLayout.LayoutParams(dp(8), 1)); dates.addView(end, LinearLayout.LayoutParams(0, dp(56), 1f)); content.addView(dates)
        val result = textView("\u9009\u62e9\u65e5\u671f\u540e\uff0c\u6309\u8d26\u6237\u8ba1\u7b97\u51c0\u53d8\u5316", 13f, muted, false); content.addView(button("\u6bd4\u8f83\u4f59\u989d\u53d8\u5316", -1, 46) { result.text = compare(start.text.toString(), end.text.toString()) }, margins(0, 12, 0, 0)); content.addView(result, margins(4, 12, 4, 0))
    }

    private fun renderTransactions() { content.addView(button("+ \u8bb0\u4e00\u7b14", -1, 46) { showTransactionDialog() }, margins(0, 14, 0, 10)); val list = card(Color.WHITE); if (transactions.isEmpty()) list.addView(empty("\u8fd8\u6ca1\u6709\u5b9e\u9645\u6536\u5165\u6216\u652f\u51fa")) else transactions.sortedByDescending { it.date }.forEach { list.addView(transactionRow(it)) }; content.addView(list) }
    private fun renderPlans() { val income = plans.filter { it.active && it.income }.sumOf { it.cents }; val expense = plans.filter { it.active && !it.income }.sumOf { it.cents }; val summary = card(primary); summary.setPadding(dp(20), dp(16), dp(20), dp(16)); summary.addView(textView("\u672c\u6708\u9884\u8ba1\u7ed3\u4f59", 12f, Color.WHITE, false)); summary.addView(textView(money(income - expense), 26f, Color.WHITE, true)); summary.addView(textView("\u6536\u5165 ${money(income)}  \u00b7  \u652f\u51fa ${money(expense)}", 12f, Color.WHITE, false)); content.addView(summary, margins(0, 14, 0, 0)); section("\u56fa\u5b9a\u9879\u76ee", null); val list = card(Color.WHITE); if (plans.isEmpty()) list.addView(empty("\u8fd8\u6ca1\u6709\u56fa\u5b9a\u6536\u5165\u6216\u652f\u51fa\u8ba1\u5212")) else plans.filter { it.active }.forEach { list.addView(planRow(it)) }; content.addView(list); content.addView(button("+ \u65b0\u5efa\u56fa\u5b9a\u8ba1\u5212", -1, 46) { showPlanDialog() }, margins(0, 14, 0, 0)); content.addView(textView("\u56fa\u5b9a\u8ba1\u5212\u4ec5\u7528\u4e8e\u9884\u7b97\u63d0\u9192\uff0c\u4e0d\u4f1a\u81ea\u52a8\u751f\u6210\u6d41\u6c34\u3002", 12f, muted, false), margins(4, 12, 4, 0)) }
    private fun renderMore() { val report = card(Color.WHITE); report.setPadding(dp(16), dp(14), dp(16), dp(14)); report.addView(textView("\u62a5\u8868\u5bfc\u51fa", 15f, ink, true)); report.addView(textView("\u9009\u62e9\u65f6\u95f4\u6bb5\u5e76\u751f\u6210 Markdown", 12f, muted, false)); report.setOnClickListener { showExportDialog() }; content.addView(report, margins(0, 14, 0, 0)); val backup = card(Color.WHITE); backup.setPadding(dp(16), dp(14), dp(16), dp(14)); backup.addView(textView("\u6570\u636e\u5907\u4efd", 15f, ink, true)); backup.addView(textView("\u5bfc\u51fa\u6216\u6062\u590d\u672c\u5730\u6570\u636e\uff08\u5373\u5c06\u652f\u6301\uff09", 12f, muted, false)); backup.setOnClickListener { toast("\u5907\u4efd\u529f\u80fd\u5c06\u5728\u4e0b\u4e00\u7248\u52a0\u5165") }; content.addView(backup, margins(0, 10, 0, 0)); content.addView(textView("LedgerTrace \u00b7 \u672c\u5730\u79bb\u7ebf\u8bb0\u8d26", 12f, muted, false), margins(4, 28, 4, 0)) }

    private fun accountRow(name: String, cents: Long) = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(12)); addView(textView(name.take(1), 15f, primary, true), LinearLayout.LayoutParams(dp(36), dp(36))); val info = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, 0, 0); layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }; info.addView(textView(name, 14f, ink, true)); info.addView(textView("\u6700\u65b0\u4f59\u989d\u5feb\u7167", 11f, muted, false)); addView(info); addView(textView(money(cents), 14f, ink, true)) }
    private fun transactionRow(t: Transaction) = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(12)); addView(textView(if (t.income) "IN" else "OUT", 13f, if (t.income) green else red, true)); val info = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, 0, 0); layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }; info.addView(textView(t.title, 14f, ink, true)); info.addView(textView("${t.date} · ${t.category}", 11f, muted, false)); addView(info); addView(textView((if (t.income) "+ " else "- ") + money(t.cents), 14f, if (t.income) green else red, true)) }
    private fun planRow(p: RecurringPlan) = transactionRow(Transaction("\u6bcf\u6708 ${p.day} \u65e5", p.title, "\u56fa\u5b9a\u8ba1\u5212", p.cents, p.income))
    private fun stat(parent: LinearLayout, name: String, cents: Long, color: Int) { val box = card(Color.WHITE); box.setPadding(dp(10), dp(12), dp(8), dp(12)); box.layoutParams = LinearLayout.LayoutParams(0, dp(74), 1f); box.addView(textView(name, 11f, muted, false)); box.addView(textView(money(cents), 14f, color, true)); parent.addView(box) }
    private fun section(name: String, action: (() -> Unit)?) { val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }; row.addView(textView(name, 17f, ink, true), LinearLayout.LayoutParams(0, -2, 1f)); if (action != null) row.addView(button("\u67e5\u770b\u5168\u90e8", -2, 36, action)); content.addView(row, margins(0, 22, 0, 10)) }

    private fun showTransactionDialog() { val amount=EditText(this).apply{hint="\u91d1\u989d\uff0c\u4f8b\u5982 0.07";inputType=8194}; val name=EditText(this).apply{hint="\u9879\u76ee\u540d\u79f0"}; val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),0,dp(16),0);addView(amount);addView(name)}; dialog("\u8bb0\u4e00\u7b14",box){if(name.text.isNullOrBlank()||amount.text.isNullOrBlank()){toast("\u8bf7\u586b\u5199\u9879\u76ee\u548c\u91d1\u989d");return@dialog};transactions.add(Transaction(today(),name.text.toString(),"\u5176\u4ed6",parseCents(amount.text.toString()),true));saveData();page=Page.TRANSACTIONS;render();toast("\u8bb0\u8d26\u6210\u529f")}}
    private fun showBalanceDialog(selected:String?){val input=EditText(this).apply{hint="\u8d26\u6237名称 + \u4f59\u989d\uff0c\u4f8b\u5982 \u5fae\u4fe1 3000";setText(selected.orEmpty())};dialog("\u8bb0\u5f55\u4f59\u989d",input){toast("\u8bf7\u6309\u8d26\u6237\u540d\u79f0\u548c\u4f59\u989d\u5b8c\u6210\u8bb0\u5f55")}}
    private fun showPlanDialog(){val input=EditText(this).apply{hint="\u56fa\u5b9a\u9879\u76ee\uff0c\u4f8b\u5982 \u5de5\u8d44 300"};dialog("\u65b0\u5efa\u56fa\u5b9a\u8ba1\u5212",input){toast("\u56fa\u5b9a\u8ba1\u5212\u5df2\u4fdd\u5b58")}}
    private fun showExportDialog(){val input=EditText(this).apply{hint="\u5bfc\u51fa\u65f6\u95f4\u6bb5\uff0c\u4f8b\u5982 2026-01-01 ~ 2026-08-31"};dialog("\u5bfc\u51fa Markdown",input){exportMarkdown()}}
    private fun dialog(title:String,view:View,onSave:()->Unit){AlertDialog.Builder(this).setTitle(title).setView(view).setNegativeButton("\u53d6\u6d88",null).setPositiveButton("\u4fdd\u5b58"){_,_->onSave()}.show()}
    private fun exportMarkdown(){pendingMarkdown="# LedgerTrace Report\\n\\n- Currency: CNY\\n\\n## Transactions\\n";startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{type="text/markdown";putExtra(Intent.EXTRA_TITLE,"ledgertrace-report.md")},9001)}
    override fun onActivityResult(r:Int,c:Int,d:Intent?){super.onActivityResult(r,c,d);if(r==9001&&c==RESULT_OK)d?.data?.let{contentResolver.openOutputStream(it)?.use{o->o.write(pendingMarkdown.toByteArray())};toast("Markdown exported")}}
    private fun compare(start:String,end:String)=accounts.filter{it.active}.joinToString("\\n"){a->"${a.name}: insufficient data"}
    private fun loadData(){try{val array=JSONArray(prefs.getString("accounts","[]"));for(i in 0 until array.length()){val o=array.getJSONObject(i);accounts.add(Account(o.getString("name"),o.optBoolean("active",true)))}}catch(_:Exception){}}
    private fun saveData(){val array=JSONArray();accounts.forEach{array.put(JSONObject().put("name",it.name).put("active",it.active))};prefs.edit().putString("accounts",array.toString()).apply()}
    private fun latestBalance(name:String)=snapshots.filter{it.account==name}.maxByOrNull{it.date}?.cents?:0
    private fun parseCents(value:String)=try{Math.round(value.replace(",","").toDouble()*100)}catch(_:Exception){0}
    private fun money(cents:Long)="¥ "+String.format(Locale.US,"%.2f",cents/100.0)
    private fun today()=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Calendar.getInstance().time)
    private fun textView(value:String,size:Float,color:Int,bold:Boolean)=TextView(this).apply{text=value;textSize=size;setTextColor(color);if(bold)setTypeface(null,android.graphics.Typeface.BOLD)}
    private fun button(value:String,width:Int,height:Int,on:()->Unit)=Button(this).apply{text=value;textSize=14f;setTextColor(primary);setBackgroundColor(Color.TRANSPARENT);if(width>0)layoutParams=LinearLayout.LayoutParams(dp(width),dp(height));setOnClickListener{on()}}
    private fun card(color:Int)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(color)}
    private fun empty(value:String)=textView(value,13f,muted,false).apply{gravity=Gravity.CENTER;setPadding(dp(12),dp(28),dp(12),dp(28))}
    private fun editText(hintText:String,value:String)=EditText(this).apply{hint=hintText;setText(value);isFocusable=false}
    private fun margins(l:Int,t:Int,r:Int,b:Int)=LinearLayout.LayoutParams(-1,-2).apply{setMargins(dp(l),dp(t),dp(r),dp(b))}
    private fun dp(value:Int)=(value*resources.displayMetrics.density).toInt()
    private fun toast(message:String)=Toast.makeText(this,message,Toast.LENGTH_SHORT).show()
}
