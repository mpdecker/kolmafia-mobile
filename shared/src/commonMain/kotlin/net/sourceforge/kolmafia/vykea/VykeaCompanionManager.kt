package net.sourceforge.kolmafia.vykea

import net.sourceforge.kolmafia.modifiers.VykeaCompanionData
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * VYKEA companion state — preference-backed charpane sync for live [my_vykea_companion].
 */
class VykeaCompanionManager(
    private val preferences: Preferences,
) {

    fun currentCompanionString(): String = preferences.getString(CURRENT_VYKEA_PREF, "")

    fun hasStoredCompanion(): Boolean {
        val current = currentCompanionString()
        return current.isNotBlank() && VykeaCompanionData.isValid(current)
    }

    fun syncFromCharpane(html: String) {
        if (hasStoredCompanion()) return
        val savedRune = preferences.getString(RUNE_PREF, "")
        val companion = VykeaCharpaneSync.parse(html, savedRune) ?: return
        storeCompanion(companion)
    }

    private fun storeCompanion(companion: VykeaCompanionData.Companion) {
        preferences.setString(NAME_PREF, companion.name)
        preferences.setInt(LEVEL_PREF, companion.level)
        preferences.setString(TYPE_PREF, VykeaCompanionData.typeToString(companion.type))
        preferences.setString(RUNE_PREF, VykeaCompanionData.runeToString(companion.rune))
        preferences.setString(CURRENT_VYKEA_PREF, VykeaCompanionData.toAshString(companion))
    }

    companion object {
        const val CURRENT_VYKEA_PREF = "_currentVykea"
        const val NAME_PREF = "_VYKEACompanionName"
        const val LEVEL_PREF = "_VYKEACompanionLevel"
        const val TYPE_PREF = "_VYKEACompanionType"
        const val RUNE_PREF = "_VYKEACompanionRune"
    }
}
