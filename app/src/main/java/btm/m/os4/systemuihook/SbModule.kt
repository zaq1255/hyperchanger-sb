// SPDX-License-Identifier: AGPL-3.0-or-later
package btm.m.os4.systemuihook

import android.view.View
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import java.lang.reflect.Executable

/** Minimal module: only installs the status-bar (signal / network-type / icon) hooks. */
class SbModule : XposedModule() {
    private val statusBarHooks by lazy {
        StatusBarHooks(
            module = this,
            hookDeoptimized = ::hookDeoptimized,
            readInstanceField = ::readInstanceField,
            readInheritedField = ::readInheritedField,
            systemUiPackageName = SYSTEM_UI,
            keepThemeKey = KEY_KEEP_SOFT_GLASS_AFTER_GLOBAL_THEME,
            hideClockTextKey = KEY_HIDE_STATUS_BAR_CLOCK_TEXT,
            mobileNetworkTypeModeKey = KEY_MOBILE_NETWORK_TYPE_MODE,
            stackedMobileDualContainerId = View.generateViewId(),
            logTag = TAG,
            applicationContext = { null },
        )
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (param.packageName != SYSTEM_UI) return
        runCatching {
            val prefs = getRemotePreferences(REMOTE_PREFERENCE_GROUP)
            val e = prefs.edit()
            if (!prefs.contains(KEY_STACKED_MOBILE_SIGNAL_ENABLED)) e.putBoolean(KEY_STACKED_MOBILE_SIGNAL_ENABLED, true)
            if (!prefs.contains(KEY_MOBILE_NETWORK_TYPE_MODE)) e.putInt(KEY_MOBILE_NETWORK_TYPE_MODE, 1)
            e.apply()
            statusBarHooks.installAdjustmentHooks(param.defaultClassLoader, prefs)
            statusBarHooks.installStackedMobileSignalHooks(param.defaultClassLoader, prefs)
        }.onFailure { HookModuleLogger.log(this, android.util.Log.ERROR, TAG, "install failed", it) }
    }

    internal fun hookDeoptimized(member: Executable) = runCatching { deoptimize(member) }.let { hook(member) }

    private fun readInstanceField(instance: Any, name: String): Any? {
        var type: Class<*>? = instance.javaClass
        while (type != null) {
            val field = runCatching { type.getDeclaredField(name) }.getOrNull()
            if (field != null) return runCatching { field.isAccessible = true; field.get(instance) }.getOrNull()
            type = type.superclass
        }
        return null
    }
    private fun readInheritedField(target: Any?, name: String): Any? {
        var type = target?.javaClass
        while (type != null) {
            val value = runCatching { type.getDeclaredField(name).apply { isAccessible = true }.get(target) }.getOrNull()
            if (value != null) return value
            type = type.superclass
        }
        return null
    }

    companion object {
        private const val SYSTEM_UI = "com.android.systemui"
        private const val TAG = "HyperChangerSB"
    }
}
