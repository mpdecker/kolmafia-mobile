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
        const val HP_RECOVERY_STOP_PCT     = "hpRecoveryStopPct"     // above → stop recovering

        // MP recovery
        const val AUTO_RECOVER_MP          = "autoRecoverMp"
        const val MP_RECOVERY_TARGET_PCT   = "mpRecoveryTargetPct"
        const val MP_RECOVERY_STOP_PCT     = "mpRecoveryStopPct"     // above → stop recovering

        // Mood
        const val AUTO_BUFF                = "autoBuff"
        const val REMOVE_MALIGNANT_EFFECTS = "removeMalignantEffects"  // default true
        const val ACTIVE_MOOD_NAME         = "activeMoodName"        // persisted active mood name
        const val ACTIVE_MOOD_TRIGGERS     = "activeMoodTriggers"    // serialized trigger list
        const val MOOD_LIBRARY_NAMES       = "moodLibraryNames"      // |-separated saved mood names
        // Per-mood data stored under "moodTriggers_${name}" (dynamic key, not a constant)

        // Banish tracking
        const val BANISHED_MONSTERS = "banishedMonsters"   // serialized banish list (same key as desktop)

        // ManaBurn
        const val MANA_BURN_ENABLED        = "manaBurnEnabled"       // default false
        const val MANA_BURN_MIN_MP_PCT     = "manaBurnMinMpPct"      // burn when MP% >= this; default 90
    }
}
