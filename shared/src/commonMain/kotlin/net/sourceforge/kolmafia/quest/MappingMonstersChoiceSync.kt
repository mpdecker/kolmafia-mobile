package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] choice 1435 mappingMonsters clear.
 */
object MappingMonstersChoiceSync {

    const val CHOICE_ID = 1435

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (decision != 1) return false
        preferences.setBoolean("mappingMonsters", false)
        return true
    }
}
