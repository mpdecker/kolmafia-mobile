package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.LimitModeGates
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

/** Desktop KoLCharacter.desertBeachAccessible(). */
object DesertBeachAccessibility {

    fun isAvailable(
        state: CharacterState,
        prefs: Preferences?,
        limitMode: String = state.limitMode,
        questDatabase: QuestDatabase? = null,
        inventory: InventoryManager? = null,
    ): Boolean {
        if (LimitModeGates.limitZone("Beach", limitMode)) return false
        val lastUnlock = prefs?.getInt("lastDesertUnlock", -1) ?: -1
        if (lastUnlock == state.ascensionNumber) return true
        if (prefs?.getString(Quest.MEATCAR.prefKey, QuestDatabase.UNSTARTED) ==
            QuestDatabase.FINISHED
        ) {
            return true
        }
        if (questDatabase?.isQuestFinished(Quest.MEATCAR) == true) return true
        val gasTankId = ItemDatabase.getByName("large motorbike gas tank")?.id
            ?: ItemDatabase.getByName("motorbike gas tank")?.id
        return gasTankId?.let { inventory?.getCount(it) ?: 0 > 0 } == true
    }
}
