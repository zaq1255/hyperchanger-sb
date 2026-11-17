// SPDX-License-Identifier: AGPL-3.0-or-later
package btm.m.os4.systemuihook
internal enum class SafetySection { STATUS_BAR }
internal object ModuleSafetyRuntime {
    @JvmStatic
    fun allows(section: SafetySection): Boolean = true
}
