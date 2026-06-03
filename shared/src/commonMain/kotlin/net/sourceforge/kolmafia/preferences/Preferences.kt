package net.sourceforge.kolmafia.preferences

import com.russhwolf.settings.Settings

class Preferences(private val settings: Settings) {

    fun getString(key: String, default: String = ""): String =
        settings.getString(key, default)

    fun setString(key: String, value: String) =
        settings.putString(key, value)

    fun getBoolean(key: String, default: Boolean = false): Boolean =
        settings.getBoolean(key, default)

    fun setBoolean(key: String, value: Boolean) =
        settings.putBoolean(key, value)

    fun getInt(key: String, default: Int = 0): Int =
        settings.getInt(key, default)

    fun setInt(key: String, value: Int) =
        settings.putInt(key, value)

    companion object Keys {
        const val LAST_USERNAME = "lastUsername"

        // HP recovery
        const val AUTO_RECOVER_HP          = "autoRecoverHp"
        const val HP_RECOVERY_TARGET_PCT   = "hpRecoveryTargetPct"   // below → start recovering
        const val HP_RECOVERY_STOP_PCT     = "hpRecoveryStopPct"     // above → stop recovering (TODO: used when multi-use loop is implemented)

        // MP recovery
        const val AUTO_RECOVER_MP          = "autoRecoverMp"
        const val MP_RECOVERY_TARGET_PCT   = "mpRecoveryTargetPct"
        const val MP_RECOVERY_STOP_PCT     = "mpRecoveryStopPct"     // above → stop recovering (TODO: used when multi-use loop is implemented)

        // Mood
        const val AUTO_BUFF                = "autoBuff"
    }
}
