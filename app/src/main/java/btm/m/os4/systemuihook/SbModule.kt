// SPDX-License-Identifier: AGPL-3.0-or-later
package btm.m.os4.systemuihook

import android.util.Log
import android.view.View
import io.github.libxposed.api.XposedInterface.ExceptionMode
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import java.lang.reflect.Executable

/** Minimal module: status-bar hooks (systemui) + hide AI-call button (incallui). */
class SbModule : XposedModule() {
    private val statusBarHooks by lazy {
        StatusBarHooks(
            module = this,
            hookDeoptimized = ::hookDeoptimized,
            readInstanceField = ::readInstanceField,
            readInheritedField = ::readInheritedField,
            systemUiPackageName = SYSTEM_UI,
            keepThemeKey = "keep_soft_glass_after_global_theme",
            hideClockTextKey = "hide_status_bar_clock_text",
            mobileNetworkTypeModeKey = "mobile_network_type_mode",
            stackedMobileDualContainerId = View.generateViewId(),
            logTag = TAG,
            applicationContext = { null },
        )
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        when (param.packageName) {
            SYSTEM_UI -> installStatusBarHooks(param)
            INCALLUI -> installAiCallHooks(param.defaultClassLoader)
        }
    }

    private fun installStatusBarHooks(param: PackageLoadedParam) {
        runCatching {
            val prefs = getRemotePreferences(REMOTE_PREFERENCE_GROUP)
            statusBarHooks.installAdjustmentHooks(param.defaultClassLoader, prefs)
            statusBarHooks.installStackedMobileSignalHooks(param.defaultClassLoader, prefs)
        }.onFailure { HookModuleLogger.log(this, Log.ERROR, TAG, "install status-bar failed", it) }
    }

    /** Hide the Xiaomi AI-call (AI接听) button so it can't be mis-tapped while answering. */
    private fun installAiCallHooks(cl: ClassLoader) {
        runCatching {
            val utils = cl.loadClass("com.android.incallui.aiassistant.AiAssistantUtils")
            val m = utils.getDeclaredMethod("supportXiaoAiAutoAnswer", android.content.Context::class.java)
            hookDeoptimized(m).setExceptionMode(ExceptionMode.PROTECTIVE)
                .setId("incallui:no-ai-auto-answer").intercept { false }
            HookModuleLogger.log(this, Log.INFO, TAG, "AI-call auto-answer disabled")
        }.onFailure { HookModuleLogger.log(this, Log.WARN, TAG, "hook supportXiaoAiAutoAnswer failed", it) }
        runCatching {
            val frag = cl.loadClass("com.android.incallui.CallCardFragment")
            val m2 = frag.getDeclaredMethod("showAiAssistant")
            hookDeoptimized(m2).setId("incallui:hide-ai-button").intercept { null }
            HookModuleLogger.log(this, Log.INFO, TAG, "AI-call button hidden")
        }.onFailure { HookModuleLogger.log(this, Log.WARN, TAG, "hook showAiAssistant failed", it) }
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
        private const val INCALLUI = "com.android.incallui"
        private const val TAG = "HyperChangerSB"
    }
}
