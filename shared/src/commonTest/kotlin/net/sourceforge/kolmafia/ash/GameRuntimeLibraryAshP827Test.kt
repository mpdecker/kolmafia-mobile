package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.FantasyRealmChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP827Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun chateau_leavesWithoutHourBurn() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_frHoursLeft", 3)
        prefs.setString("_frAreasUnlocked", "Duke Vampire's Chateau,")
        assertTrue(FantasyRealmChoiceSync.apply(1300, 1, prefs))
        assertEquals(3, prefs.getInt("_frHoursLeft", 0))
        assertFalse(prefs.getString("_frAreasUnlocked", "").contains("Duke Vampire's Chateau,"))
    }

    @Test
    fun arch_consumesChargedOrb() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_frHoursLeft", 2)
        prefs.setString("_frAreasUnlocked", "The Archwizard's Tower,")
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            FantasyRealmChoiceSync.apply(
                choiceId = 1302,
                decision = 1,
                preferences = prefs,
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals(2, prefs.getInt("_frHoursLeft", 0))
        assertEquals(listOf(FantasyRealmChoiceSync.FR_CHARGED_ORB to 1), consumed)
        assertFalse(prefs.getString("_frAreasUnlocked", "").contains("The Archwizard's Tower,"))
    }

    @Test
    fun leaveDecision6_keepsArea() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_frAreasUnlocked", "Duke Vampire's Chateau,")
        assertTrue(FantasyRealmChoiceSync.apply(1300, 6, prefs))
        assertTrue(prefs.getString("_frAreasUnlocked", "").contains("Duke Vampire's Chateau,"))
    }

    @Test
    fun questChoiceRules_wires1307() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_frAreasUnlocked", "The Master Thief's Chalet,")
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            FantasyRealmChoiceSync.apply(
                choiceId = 1307,
                decision = 1,
                preferences = prefs,
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals(listOf(FantasyRealmChoiceSync.FR_NOTARIZED_WARRANT to 1), consumed)
        assertFalse(prefs.getString("_frAreasUnlocked", "").contains("The Master Thief's Chalet,"))

        val wired = Preferences(MapSettings())
        wired.setString("_frAreasUnlocked", "The Master Thief's Chalet,")
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1307,
                responseText = "",
                questDatabase = QuestDatabase(wired),
                decision = 1,
                preferences = wired,
            ),
        )
        assertFalse(wired.getString("_frAreasUnlocked", "").contains("The Master Thief's Chalet,"))
    }
}
