// SPDX-License-Identifier: AGPL-3.0-or-later
package btm.m.os4.systemuihook

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import kotlin.math.roundToInt

/** Minimal status-bar settings screen (classic Views). Writes local + framework remote prefs. */
class SettingsActivity : Activity() {
    private val G = REMOTE_PREFERENCE_GROUP
    private var root: LinearLayout? = null
    private var statusView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val r = LinearLayout(this)
        r.orientation = LinearLayout.VERTICAL
        r.setPadding(dp(20), dp(24), dp(20), dp(24))
        r.setBackgroundColor(Color.parseColor("#101114"))
        root = r

        addTitle(r, "SBOnly · 状态栏")
        statusView = addStatus(r)
        val sync = Button(this)
        sync.text = "重新同步到框架"
        sync.setOnClickListener {
            SbApp.holder?.let { SbApp.syncAllToRemote(it) }
            refreshStatus()
        }
        r.addView(sync)

        addSection(r, "双排信号 / 网络类型")
        addSwitch(r, "双排信号", KEY_STACKED_MOBILE_SIGNAL_ENABLED, false)
        addFloat(r, "双排信号缩放", KEY_STACKED_MOBILE_SIGNAL_SCALE, -0.9f, 2.0f, 0.1f, 0f)
        addFloat(r, "双排垂直偏移", KEY_STACKED_MOBILE_SIGNAL_VERTICAL_OFFSET, -30f, 30f, 1f, 0f)
        addFloat(r, "双排左边距", KEY_STACKED_MOBILE_SIGNAL_LEFT_MARGIN, -20f, 60f, 1f, 0f)
        addFloat(r, "双排右边距", KEY_STACKED_MOBILE_SIGNAL_RIGHT_MARGIN, -20f, 60f, 1f, 0f)
        addSwitch(r, "双排圆角", KEY_STACKED_MOBILE_CORNER_ENABLED, false)
        addFloat(r, "圆角半径(dp)", KEY_STACKED_MOBILE_CORNER_RADIUS, 0f, 4f, 0.1f, 0.5f)
        addInt(r, "网络类型模式(0隐/1显)", KEY_MOBILE_NETWORK_TYPE_MODE, 0, 2, 1)
        addSwitch(r, "网络类型加粗", KEY_MOBILE_NETWORK_TYPE_BOLD, false)
        addFloat(r, "网络类型缩放", KEY_MOBILE_NETWORK_TYPE_SCALE, -0.9f, 2.0f, 0.1f, 0f)
        addFloat(r, "网络类型垂直偏移", KEY_MOBILE_NETWORK_TYPE_VERTICAL_OFFSET, -30f, 30f, 1f, 0f)
        addFloat(r, "网络类型左边距", KEY_MOBILE_NETWORK_TYPE_LEFT_MARGIN, -20f, 60f, 1f, 0f)
        addFloat(r, "网络类型右边距", KEY_MOBILE_NETWORK_TYPE_RIGHT_MARGIN, -20f, 60f, 1f, 0f)
        addInt(r, "信号隐藏模式(0/1/2)", KEY_MOBILE_SIGNAL_HIDE_MODE, 0, 2, 0)

        addSection(r, "图标")
        addFloat(r, "图标缩放 %", KEY_STATUS_BAR_ICON_SIZE_PERCENT, -100f, 100f, 1f, 0f)
        addInt(r, "图标间距 %", KEY_STATUS_BAR_ICON_SPACING_PERCENT, -100, 100, 0)

        val hint = TextView(this)
        hint.setText("改完回到系统界面 / 重启系统界面生效。")
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        hint.setTextColor(Color.parseColor("#9AA0A6"))
        hint.setPadding(0, dp(20), 0, 0)
        r.addView(hint)

        val sc = ScrollView(this)
        sc.addView(r)
        setContentView(sc)
    }

    override fun onResume() { super.onResume(); refreshStatus() }

    private fun connStatus(): String =
        "框架：" + (if (SbApp.holder != null) "已连接 ✓" else "未连接（先在本机 LSPosed/Vector 启用模块）")

    private fun refreshStatus() { statusView?.setText(connStatus()) }

    private fun addTitle(r: LinearLayout, s: String) {
        val tv = TextView(this)
        tv.setText(s); tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f); tv.setTextColor(Color.WHITE)
        r.addView(tv)
    }

    private fun addStatus(r: LinearLayout): TextView {
        val tv = TextView(this)
        tv.setText(connStatus()); tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        tv.setTextColor(Color.parseColor("#9AA0A6")); tv.setPadding(0, dp(6), 0, dp(14))
        r.addView(tv); return tv
    }

    private fun addSection(r: LinearLayout, s: String) {
        val tv = TextView(this)
        tv.setText(s); tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        tv.setTextColor(Color.parseColor("#8AB4F8")); tv.setPadding(0, dp(18), 0, dp(6))
        r.addView(tv)
    }

    private fun addSwitch(r: LinearLayout, title: String, key: String, def: Boolean) {
        val v = prefs().getBoolean(key, def)
        val sw = Switch(this)
        sw.text = title
        sw.setTextColor(Color.WHITE)
        sw.isChecked = v
        sw.setOnCheckedChangeListener { _, checked -> editBoth { it.putBoolean(key, checked) } }
        r.addView(sw)
    }

    private fun addFloat(r: LinearLayout, title: String, key: String, min: Float, max: Float, step: Float, def: Float) {
        val cur = prefs().getFloat(key, def).coerceIn(min, max)
        val steps = ((max - min) / step).roundToInt()
        val prog = ((cur - min) / step).roundToInt().coerceIn(0, steps)
        val tv = TextView(this)
        tv.setText(title + "：" + fmt(cur))
        tv.setTextColor(Color.WHITE); tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f); tv.setPadding(0, dp(10), 0, 0)
        r.addView(tv)
        val sb = SeekBar(this)
        sb.max = steps
        sb.progress = prog
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, p: Int, fromUser: Boolean) {
                val v = min + p * step
                tv.setText(title + "：" + fmt(v))
                if (fromUser) editBoth { it.putFloat(key, v) }
            }
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {}
        })
        r.addView(sb)
    }

    private fun addInt(r: LinearLayout, title: String, key: String, min: Int, max: Int, def: Int) {
        val cur = prefs().getInt(key, def).coerceIn(min, max)
        val tv = TextView(this)
        tv.setText(title + "：" + cur)
        tv.setTextColor(Color.WHITE); tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f); tv.setPadding(0, dp(10), 0, 0)
        r.addView(tv)
        val sb = SeekBar(this)
        sb.max = (max - min)
        sb.progress = cur - min
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, p: Int, fromUser: Boolean) {
                val v = min + p
                tv.setText(title + "：" + v)
                if (fromUser) editBoth { it.putInt(key, v) }
            }
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {}
        })
        r.addView(sb)
    }

    private fun fmt(f: Float): String =
        if (f == f.roundToInt().toFloat()) f.roundToInt().toString() else String.format("%.1f", f)

    private fun prefs(): SharedPreferences = getSharedPreferences(G, Context.MODE_PRIVATE)

    private fun editBoth(block: (SharedPreferences.Editor) -> Unit) {
        val le = prefs().edit(); block(le); le.apply()
        SbApp.holder?.let { svc -> runCatching { val re = svc.getRemotePreferences(G).edit(); block(re); re.apply() } }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).roundToInt()
}
