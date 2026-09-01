package cc.eu.hjb20bit.ledgertrace

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val primary=Color.rgb(15,91,82); private val bg=Color.rgb(245,248,247); private val ink=Color.rgb(27,40,38); private val muted=Color.rgb(113,128,125); private val green=Color.rgb(32,132,93); private val red=Color.rgb(195,79,79)
    private lateinit var content:LinearLayout
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContentView(R.layout.activity_main);val container=findViewById<FrameLayout>(R.id.main);container.removeAllViews();container.addView(buildUi())}
    private fun buildUi():LinearLayout{val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg)};val header=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(24),dp(24),dp(24),dp(16))};header.addView(label("\u8d26\u8ff9",28f,ink,true));header.addView(label("\u8bb0\u5f55\u6bcf\u4e00\u7b14\uff0c\u4e5f\u770b\u6e05\u6bcf\u4e00\u6b65",13f,muted,false));content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(24),0,dp(24),dp(24))};val scroll=ScrollView(this).apply{addView(content);layoutParams=LinearLayout.LayoutParams(-1,0,1f)};val nav=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setBackgroundColor(Color.WHITE)};listOf("\u9996\u9875" to false,"\u4f59\u989d" to false,"\u8bb0\u8d26" to true,"\u8ba1\u5212" to false,"\u66f4\u591a" to false).forEach{item->nav.addView(Button(this).apply{text=item.first;textSize=12f;setTextColor(primary);setBackgroundColor(Color.TRANSPARENT);layoutParams=LinearLayout.LayoutParams(0,dp(64),1f);setOnClickListener{if(item.second)showEntryDialog()else toast(item.first)}})};root.addView(header);root.addView(scroll);root.addView(nav);renderHome();return root}
    private fun renderHome(){content.removeAllViews();val hero=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(18),dp(20),dp(18));setBackgroundColor(primary)};hero.addView(label("\u603b\u8d44\u4ea7",13f,Color.WHITE,false));hero.addView(label("\u00a5 0.00",32f,Color.WHITE,true));hero.addView(label("\u672c\u5730\u79bb\u7ebf\u4fdd\u5b58 \u00b7 CNY",12f,Color.WHITE,false));content.addView(hero,margins(0,14,0,0));content.addView(label("\u672c\u6708\u6982\u89c8",17f,ink,true),margins(0,22,0,10));val stats=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};addStat(stats,"\u6536\u5165","\u00a5 0.00",green);addStat(stats,"\u652f\u51fa","\u00a5 0.00",red);addStat(stats,"\u51c0\u7ed3\u4f59","\u00a5 0.00",primary);content.addView(stats);content.addView(label("\u8d26\u6237\u4f59\u989d",17f,ink,true),margins(0,22,0,10));content.addView(label("\u8fd8\u6ca1\u6709\u8d26\u6237\uff0c\u8bf7\u5148\u5728\u4f59\u989d\u9875\u9762\u8bb0\u5f55\u4f59\u989d",13f,muted,false).apply{gravity=Gravity.CENTER;setPadding(0,dp(28),0,dp(28))})}
    private fun showEntryDialog(){val input=EditText(this).apply{hint="\u91d1\u989d\uff0c\u4f8b\u5982 0.07";inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL};AlertDialog.Builder(this).setTitle("\u8bb0\u4e00\u7b14").setView(input).setNegativeButton("\u53d6\u6d88",null).setPositiveButton("\u4fdd\u5b58"){_,_->toast("\u5df2\u4fdd\u5b58")}.show()}
    private fun addStat(parent:LinearLayout,name:String,value:String,color:Int){val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(10),dp(12),dp(8),dp(12));setBackgroundColor(Color.WHITE);layoutParams=LinearLayout.LayoutParams(0,dp(74),1f)};box.addView(label(name,11f,muted,false));box.addView(label(value,14f,color,true));parent.addView(box)}
    private fun label(value:String,size:Float,color:Int,bold:Boolean)=TextView(this).apply{text=value;textSize=size;setTextColor(color);if(bold)setTypeface(null,android.graphics.Typeface.BOLD)}
    private fun margins(left:Int,top:Int,right:Int,bottom:Int)=LinearLayout.LayoutParams(-1,-2).apply{setMargins(dp(left),dp(top),dp(right),dp(bottom))};private fun dp(value:Int)=(value*resources.displayMetrics.density).toInt();private fun toast(message:String)=Toast.makeText(this,message,Toast.LENGTH_SHORT).show()
}
