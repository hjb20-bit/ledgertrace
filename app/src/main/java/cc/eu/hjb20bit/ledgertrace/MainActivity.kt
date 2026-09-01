package cc.eu.hjb20bit.ledgertrace

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val primary = Color.rgb(15, 91, 82)
    private val bg = Color.rgb(245, 248, 247)
    private val ink = Color.rgb(27, 40, 38)
    private val muted = Color.rgb(113, 128, 125)
    private val green = Color.rgb(32, 132, 93)
    private val red = Color.rgb(195, 79, 79)
    private lateinit var content: LinearLayout
    private lateinit var title: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val root = findViewById<FrameLayout>(R.id.main)
        root.removeAllViews()
        root.addView(buildUi())
    }

    private fun buildUi(): LinearLayout {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(24), dp(24), dp(16)) }
        title = text("账迹", 28f, ink, true)
        header.addView(title)
        header.addView(text("记录每一笔，也看清每一步", 13f, muted, false))
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), 0, dp(24), dp(90)) }
        val scroll = ScrollView(this).apply { addView(content); layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.WHITE) }
        listOf("首页" to "首页", "余额" to "余额", "记账" to "记账", "计划" to "计划", "更多" to "更多").forEach { pair ->
            nav.addView(Button(this).apply {
                text = pair.first; textSize = 12f; setTextColor(primary); setBackgroundColor(Color.TRANSPARENT)
                layoutParams = LinearLayout.LayoutParams(0, dp(64), 1f)
                setOnClickListener { if (pair.second == "记账") addEntry() else toast("${pair.first} 页面原型") }
            })
        }
        root.addView(header); root.addView(scroll); root.addView(nav)
        renderHome()
        return root
    }

    private fun renderHome() {
        content.removeAllViews()
        val hero = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(18), dp(20), dp(18)); setBackgroundColor(primary) }
        hero.addView(text("总资产", 13f, Color.WHITE, false))
        hero.addView(text("¥ 0.00", 32f, Color.WHITE, true))
        hero.addView(text("本地离线保存 · CNY", 12f, Color.WHITE, false))
        content.addView(hero, margin(0, 14, 0, 0))
        content.addView(text("本月概览", 17f, ink, true), margin(0, 22, 0, 10))
        val stats = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        stat(stats, "收入", "¥ 0.00", green); stat(stats, "支出", "¥ 0.00", red); stat(stats, "净结余", "¥ 0.00", primary)
        content.addView(stats)
        content.addView(text("账户余额", 17f, ink, true), margin(0, 22, 0, 10))
        content.addView(text("还没有账户，请先在余额页面记录余额", 13f, muted, false).apply { gravity = Gravity.CENTER; setPadding(0, dp(28), 0, dp(28)) })
    }

    private fun addEntry() {
        val input = EditText(this).apply { hint = "金额，例如 0.07"; inputType = 8194 }
        AlertDialog.Builder(this).setTitle("记一笔").setView(input).setNegativeButton("取消", null).setPositiveButton("保存") { _, _ -> toast("已保存（演示）") }.show()
    }

    private fun stat(parent: LinearLayout, label: String, value: String, color: Int) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), dp(12), dp(8), dp(12)); setBackgroundColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0, dp(74), 1f) }
        box.addView(text(label, 11f, muted, false)); box.addView(text(value, 14f, color, true)); parent.addView(box)
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply { text = value; textSize = size; setTextColor(color); if (bold) setTypeface(null, android.graphics.Typeface.BOLD) }
    private fun margin(l: Int, t: Int, r: Int, b: Int) = LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(l), dp(t), dp(r), dp(b)) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
