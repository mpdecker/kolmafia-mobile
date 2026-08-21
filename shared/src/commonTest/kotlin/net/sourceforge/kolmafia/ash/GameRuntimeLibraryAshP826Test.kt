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

class GameRuntimeLibraryAshP826Test {

    @Test
    fun revision_phase826() {
        assertEquals("phase826", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun rubeeMine_leavesAndBurnsHour() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_frHoursLeft", 2)
        prefs.setString("_frAreasUnlocked", "The Old Rubee Mine,")
        assertTrue(FantasyRealmChoiceSync.apply(1288, 1, prefs))
        assertEquals(1, prefs.getInt("_frHoursLeft", 0))
        assertFalse(prefs.getString("_frAreasUnlocked", "").contains("The Old Rubee Mine,"))
    }

    @Test
    fun cave_unlocksPhoenix() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_frHoursLeft", 2)
        prefs.setString("_frAreasUnlocked", "The Foreboding Cave,")
        assertTrue(FantasyRealmChoiceSync.apply(1289, 3, prefs))
        assertTrue(prefs.getString("_frAreasUnlocked", "").contains("The Lair of the Phoenix,"))
    }

    @Test
    fun phoenix_consumesHolyWaterWithoutHourBurn() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_frHoursLeft", 2)
        prefs.setString("_frAreasUnlocked", "The Lair of the Phoenix,")
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            FantasyRealmChoiceSync.apply(
                choiceId = 1298,
                decision = 1,
                preferences = prefs,
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals(2, prefs.getInt("_frHoursLeft", 0))
        assertEquals(listOf(FantasyRealmChoiceSync.FR_HOLY_WATER to 1), consumed)
        assertFalse(prefs.getString("_frAreasUnlocked", "").contains("The Lair of the Phoenix,"))
    }

    @Test
    fun dragonMoor_leavesWithoutHourBurn() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_frHoursLeft", 3)
        prefs.setString("_frAreasUnlocked", "The Dragon's Moor,")
        assertTrue(FantasyRealmChoiceSync.apply(1299, 1, prefs))
        assertEquals(3, prefs.getInt("_frHoursLeft", 0))
        assertFalse(prefs.getString("_frAreasUnlocked", "").contains("The Dragon's Moor,"))
    }

    @Test
    fun questChoiceRules_wires1290() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_frHoursLeft", 2)
        prefs.setString("_frAreasUnlocked", "The Faerie Cyrkle,")
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1290,
                responseText = "",
                questDatabase = QuestDatabase(prefs),
                decision = 3,
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getString("_frAreasUnlocked", "").contains("The Spider Queen's Lair,"))
    }
}
