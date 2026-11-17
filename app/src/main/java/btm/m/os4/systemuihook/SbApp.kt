// SPDX-License-Identifier: AGPL-3.0-or-later
package btm.m.os4.systemuihook

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

/** Holds the framework service and mirrors local settings to the framework remote prefs. */
class SbApp : Application(), XposedServiceHelper.OnServiceListener {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        seedDefaults()
        runCatching { XposedServiceHelper.registerListener(this) }
    }

    private fun seedDefaults() {
        val sp = getSharedPreferences(REMOTE_PREFERENCE_GROUP, Context.MODE_PRIVATE)
        val e = sp.edit()
        if (!sp.contains(KEY_STACKED_MOBILE_SIGNAL_ENABLED)) e.putBoolean(KEY_STACKED_MOBILE_SIGNAL_ENABLED, true)
        if (!sp.contains(KEY_MOBILE_NETWORK_TYPE_MODE)) e.putInt(KEY_MOBILE_NETWORK_TYPE_MODE, 1)
        e.apply()
    }

    override fun onServiceBind(service: XposedService) {
        holder = service
        runCatching { syncAllToRemote(service) }
    }

    override fun onServiceDied(service: XposedService) {
        if (holder === service) holder = null
    }

    companion object {
        @Volatile var holder: XposedService? = null
        @Volatile private var appContext: Context? = null

        fun syncAllToRemote(service: XposedService) {
            val ctx = appContext ?: return
            val local = ctx.getSharedPreferences(REMOTE_PREFERENCE_GROUP, Context.MODE_PRIVATE)
            val remote = service.getRemotePreferences(REMOTE_PREFERENCE_GROUP)
            val e = remote.edit()
            for ((k, v) in local.all) {
                when (v) {
                    is Boolean -> e.putBoolean(k, v)
                    is Int -> e.putInt(k, v)
                    is Long -> e.putLong(k, v)
                    is Float -> e.putFloat(k, v)
                    is String -> e.putString(k, v)
                }
            }
            e.apply()
        }
    }
}
