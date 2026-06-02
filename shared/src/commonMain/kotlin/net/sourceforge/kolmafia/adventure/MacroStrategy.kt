package net.sourceforge.kolmafia.adventure

import net.sourceforge.kolmafia.preferences.Preferences

object MacroStrategy {
    const val SAFE_DEFAULT = "attack; if (hpbelow 30) use healing potion; attack"

    fun forLocation(zoneId: String, preferences: Preferences): String {
        val perZone = preferences.getString("combatMacro_$zoneId")
        if (perZone.isNotBlank()) return perZone
        val global = preferences.getString("combatMacroDefault")
        return global.ifBlank { SAFE_DEFAULT }
    }

    fun choiceOptionFor(choiceId: Int, preferences: Preferences): Int =
        preferences.getString("choiceAdventure$choiceId").toIntOrNull() ?: 1
}
