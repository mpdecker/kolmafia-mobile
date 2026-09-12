package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

/**
 * Desktop quest / Fun-a-Log coinmaster [accessible] gates (Behavioral Deepen XLIX Track B).
 */
object QuestShopAccessibility {

    fun grandmaInaccessible(prefs: Preferences?): String? {
        val progress = prefs?.getString(Quest.SEA_MONKEES.prefKey, QuestDatabase.UNSTARTED)
            ?: QuestDatabase.UNSTARTED
        return if (QuestDatabase.stepOrdinal(progress) > QuestDatabase.stepOrdinal("step8")) {
            null
        } else {
            "You must rescue Grandma first."
        }
    }

    fun blackMarketInaccessible(char: CharacterState, prefs: Preferences?): String? {
        if (prefs?.getInt("lastWuTangDefeated", -1) == char.ascensionNumber) {
            return "The Black Market is not currently available"
        }
        if (char.inNuclearAutumn) {
            return "The Black Market is not currently available"
        }
        val progress = prefs?.getString(Quest.MACGUFFIN.prefKey, QuestDatabase.UNSTARTED)
            ?: QuestDatabase.UNSTARTED
        val available =
            progress == QuestDatabase.FINISHED || progress.contains("step", ignoreCase = true)
        return if (available) null else "The Black Market is not currently available"
    }

    fun funALogInaccessible(accessibleCount: (Int) -> Int): String? =
        if (accessibleCount(FunALogUnlockPrefs.PIRATE_REALM_FUN_LOG) > 0) null
        else "Need PirateRealm fun-a-log"
}
