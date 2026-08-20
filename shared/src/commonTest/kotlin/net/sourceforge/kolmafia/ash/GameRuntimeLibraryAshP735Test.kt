package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.BlechHouseChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.SmutOrcCombatSync

class GameRuntimeLibraryAshP735Test {

    @Test
    fun resetsProgress() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(SmutOrcCombatSync.PREF, 12)
        assertTrue(BlechHouseChoiceSync.apply(1345, prefs))
        assertEquals(0, prefs.getInt(SmutOrcCombatSync.PREF, -1))
    }

    @Test
    fun questChoiceRules_wires1345() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(SmutOrcCombatSync.PREF, 8)
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1345,
                responseText = "",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals(0, prefs.getInt(SmutOrcCombatSync.PREF, -1))
    }

    @Test
    fun ignoresOtherChoices() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(SmutOrcCombatSync.PREF, 5)
        assertFalse(BlechHouseChoiceSync.apply(1219, prefs))
        assertEquals(5, prefs.getInt(SmutOrcCombatSync.PREF, 0))
    }
}
