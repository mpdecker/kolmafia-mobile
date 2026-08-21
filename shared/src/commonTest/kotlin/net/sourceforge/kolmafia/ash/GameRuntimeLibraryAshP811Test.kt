package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DaycareLobbyChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP811Test {

    @Test
    fun revision_phase814() {
        assertEquals("phase814", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun lobby_nap() {
        val prefs = Preferences(MapSettings())
        assertTrue(DaycareLobbyChoiceSync.apply(1334, 1, "", prefs))
        assertEquals(true, prefs.getBoolean("_daycareNap", false))
    }

    @Test
    fun lobby_spaGate() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            DaycareLobbyChoiceSync.apply(
                1334,
                2,
                "You are only allowed one spa treatment per day",
                prefs,
            ),
        )
        assertEquals(true, prefs.getBoolean("_daycareSpa", false))
    }

    @Test
    fun spa_setsUsed() {
        val prefs = Preferences(MapSettings())
        assertTrue(DaycareLobbyChoiceSync.apply(1335, 1, "", prefs))
        assertEquals(true, prefs.getBoolean("_daycareSpa", false))
        assertFalse(DaycareLobbyChoiceSync.apply(1335, 5, "", prefs))
    }

    @Test
    fun questChoiceRules_wires1335() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1335,
                responseText = "",
                questDatabase = QuestDatabase(prefs),
                decision = 3,
                preferences = prefs,
            ),
        )
        assertEquals(true, prefs.getBoolean("_daycareSpa", false))
    }
}
