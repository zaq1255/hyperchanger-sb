// SPDX-License-Identifier: AGPL-3.0-or-later
package btm.m.os4.systemuihook

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import kotlin.math.roundToInt

/** Minimal status-bar settings screen (classic Views). Writes to local + framework remote prefs. */
class SettingsActivity : Activity() {
    private val G = REMOTE_PREFERENCE_GROUP
    private var status: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            setBackgroundColor(Color.parseColor("#101114"))
        }
        val title = TextView(this).apply {
            text = "SBOnly · 状态栏"
            textSize = 20f
            setTextColor(Color.WHITE)
        }
        root.addView(title)
        status = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#9AA0A6"))
            setPadding(0, dp(6), 0, dp(14))
            text = connStatus()
        }
        root.addView(status)
        val sync = Button(this).apply {
            text = "重新同步到框架"
            setOnClickListener { SbApp.holder?.let { SbApp.syncAllToRemote(it) }; refreshStatus() }
        }
        root.addView(sync)
        root.addView(label("双排信号 / 网络类型"))
        sw("双排信号", KEY_STACKED_MOBILE_SIGNAL_ENABLED, false)
        sf("双排信号缩放", KEY_STACKED_MOBILE_SIGNAL_SCALE, -0.9f, 2.0f, 0.1f, 0f)
        sf("双排垂直偏移", KEY_STACKED_MOBILE_SIGNAL_VERTICAL_OFFSET, -30f, 30f, 1f, 0f)
        sf("双排左边距", KEY_STACKED_MOBILE_SIGNAL_LEFT_MARGIN, -20f, 60f, 1f, 0f)
        sf("双排右边距", KEY_STACKED_MOBILE_SIGNAL_RIGHT_MARGIN, -20f, 60f, 1f, 0f)
        sw("双排圆角", KEY_STACKED_MOBILE_CORNER_ENABLED, false)
        sf("圆角半径(dp)", KEY_STACKED_MOBILE_CORNER_RADIUS, 0f, 4f, 0.1f, 0.5f)
        si("网络类型模式(0隐/1显)", KEY_MOBILE_NETWORK_TYPE_MODE, 0, 2, 1)
        sw("网络类型加粗", KEY_MOBILE_NETWORK_TYPE_BOLD, false)
        sf("网络类型缩放", KEY_MOBILE_NETWORK_TYPE_SCALE, -0.9f, 2.0f, 0.1f, 0f)
        sf("网络类型垂直偏移", KEY_MOBILE_NETWORK_TYPE_VERTICAL_OFFSET, -30f, 30f, 1f, 0f)
        sf("网络类型左边距", KEY_MOBILE_NETWORK_TYPE_LEFT_MARGIN, -20f, 60f, 1f, 0f)
        sf("网络类型右边距", KEY_MOBILE_NETWORK_TYPE_RIGHT_MARGIN, -20f, 60f, 1f, 0f)
        si("信号隐藏模式(0/1/2)", KEY_MOBILE_SIGNAL_HIDE_MODE, 0, 2, 0)
        root.addView(label("图标"))
        sf("图标缩放 %", KEY_STATUS_BAR_ICON_SIZE_PERCENT, -100f, 100f, 1f, 0f)
        si("图标间距 %", KEY_STATUS_BAR_ICON_SPACING_PERCENT, -100, 100, 0)
        val hint = TextView(this).apply {
            text = "改完回到系统界面/重启系统界面生效。"
            textSize = 12f
            setTextColor(Color.parseColor("#9AA0A6"))
            setPadding(0, dp(20), 0, 0)
        }
        root.addView(hint)
        val sv = ScrollView(this).apply { addView(root) }
        setContentView(sv)
    }

    override fun onResume() { super.onResume(); refreshStatus() }

    private fun connStatus() = "框架：" + (if (SbApp.holder != null) "已连接 ✓" else "未连接（先在本机 LSPosed/Vector 启用模块）")
    private fun refreshStatus() { status?.text = connStatus() }

    private fun label(t: String): TextView = TextView(this).apply {
        text = t; textSize = 15f; setTextColor(Color.parseColor("#8AB4F8")); setPadding(0, dp(18), 0, dp(6))
    }

    private fun sw(text: String, key: String, def: Boolean) {
        val sp = prefs()
        val v = sp.getBoolean(key, def)
        val s = Switch(this).apply {
            this.text = text; setTextColor(Color.WHITE); isChecked = v
            setOnCheckedChangeListener { _, checked -> editBoth { it.putBoolean(key, checked) } }
        }
        (findRoot()).addView(s)
    }

    private fun sf(text: String, key: String, min: Float, max: Float, step: Float, def: Float) {
        val sp = prefs()
        val cur = sp.getFloat(key, def).coerceIn(min, max)
        val steps = ((max - min) / step).roundToInt()
        val prog = ((cur - min) / step).roundToInt().coerceIn(0, steps)
        val tv = TextView(this).apply { text = "$text：${fmt(cur)}"; setTextColor(Color.WHITE); textSize = 14f; setPadding(0, dp(10), 0, 0) }
        (findRoot()).addView(tv)
        val sb = SeekBar(this).apply {
            this.max = steps; progress = prog
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    val valF = min + p * step
                    tv.text = "$text：${fmt(valF)}"
                    if (fromUser) editBoth { it.putFloat(key, valF) }
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        (findRoot()).addView(sb)
    }

    private fun si(text: String, key: String, min: Int, max: Int, def: Int) {
        val sp = prefs()
        val cur = sp.getInt(key, def).coerceIn(min, max)
        val tv = TextView(this).apply { text = "$text：$cur"; setTextColor(Color.WHITE); textSize = 14f; setPadding(0, dp(10), 0, 0) }
        (findRoot()).addView(tv)
        val sb = SeekBar(this).apply {
            this.max = (max - min); progress = cur - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    val v = min + p
                    tv.text = "$text：$v"
                    if (fromUser) editBoth { it.putInt(key, v) }
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        (findRoot()).addView(sb)
    }

    private fun fmt(f: Float) = if (f == f.roundToInt().toFloat()) f.roundToInt().toString() else String.format("%.1f", f)

    private fun prefs(): SharedPreferences = getSharedPreferences(G, Context.MODE_PRIVATE)

    private fun editBoth(block: (SharedPreferences.Editor) -> Unit) {
        val le = prefs().edit(); block(le); le.apply()
        SbApp.holder?.let { svc ->
            runCatching { val re = svc.getRemotePreferences(G).edit(); block(re); re.apply() }
        }
    }

    private fun findRoot(): LinearLayout {
        val sv = findViewById<ViewGroup>(android.R.id.content).getChildAt(0) as ScrollView
        return sv.getChildAt(0) as LinearLayout
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).roundToInt()
}
