package net.sourceforge.kolmafia.adventure

import net.sourceforge.kolmafia.combat.Macrofier
import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ChoiceCombatAshState

object MacroStrategy {
    const val SAFE_DEFAULT = "attack; if (hpbelow 30) use healing potion; attack"

    fun forLocation(
        zoneId: String,
        preferences: Preferences,
        maximumMp: Int = Int.MAX_VALUE,
    ): String {
        Macrofier.macrofy(
            monsterName = MonsterStatusTracker.getLastMonsterName(),
            preferences = preferences,
            filterOverride = ChoiceCombatAshState.combatFilterOverride,
            maximumMp = maximumMp,
        )?.takeIf { it.isNotBlank() }?.let { return it }

        val perZone = preferences.getString("combatMacro_$zoneId")
        if (perZone.isNotBlank()) return perZone
        val global = preferences.getString("combatMacroDefault")
        return global.ifBlank { SAFE_DEFAULT }
    }
}
