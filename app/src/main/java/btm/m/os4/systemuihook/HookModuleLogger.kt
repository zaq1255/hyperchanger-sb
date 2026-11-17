// SPDX-License-Identifier: AGPL-3.0-or-later
package btm.m.os4.systemuihook
import android.content.SharedPreferences
object HookModuleLogger {
    @JvmStatic
    fun attach(api: Any?, prefs: SharedPreferences) {}
    @JvmStatic @JvmOverloads
    fun log(api: Any?, priority: Int, tag: String, message: String, error: Throwable? = null) {
        runCatching { android.util.Log.println(priority, tag, if (error != null) "$message: $error" else message) }
    }
}
