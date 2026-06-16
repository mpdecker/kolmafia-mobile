package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameRuntimeLibraryAshP20Test {

    private fun libWithFinalStep(step: String): GameRuntimeLibrary {
        val prefs = Preferences(MapSettings())
        prefs.setString(Quest.FINAL.prefKey, step)
        val db = QuestDatabase(prefs)
        return GameRuntimeLibrary(preferences = prefs, questDatabase = db)
    }

    @Test
    fun hedgeMaze_trapsAtStep4SetsChoicePrefs() {
        val lib = libWithFinalStep("step4")
        assertEquals(
            "true",
            outputLib(lib, """print(to_string(hedge_maze("traps")));"""),
        )
        val prefs = lib.preferences!!
        assertEquals(2, prefs.getInt("choiceAdventure1005", 0))
        assertEquals(2, prefs.getInt("choiceAdventure1008", 0))
        assertEquals(2, prefs.getInt("choiceAdventure1011", 0))
        assertEquals(1, prefs.getInt("choiceAdventure1013", 0))
        assertEquals(1, prefs.getInt("choiceAdventure1006", 0))
    }

    @Test
    fun hedgeMaze_gopherAtStep4SetsExpectedPrefs() {
        val lib = libWithFinalStep("step4")
        assertEquals(
            "true",
            outputLib(lib, """print(to_string(hedge_maze("gopher")));"""),
        )
        val prefs = lib.preferences!!
        assertEquals(1, prefs.getInt("choiceAdventure1005", 0))
        assertEquals(2, prefs.getInt("choiceAdventure1006", 0))
        assertEquals(2, prefs.getInt("choiceAdventure1009", 0))
    }

    @Test
    fun hedgeMaze_nuggletsAtStep4SetsAllLeftChoices() {
        val lib = libWithFinalStep("step4")
        assertEquals(
            "true",
            outputLib(lib, """print(to_string(hedge_maze("nugglets")));"""),
        )
        val prefs = lib.preferences!!
        for (room in 1005..1013) {
            assertEquals(1, prefs.getInt("choiceAdventure$room", 0), "room $room")
        }
    }

    @Test
    fun hedgeMaze_trapsBeforeMazeReturnsFalse() {
        val lib = libWithFinalStep("step2")
        assertEquals(
            "false",
            outputLib(lib, """print(to_string(hedge_maze("traps")));"""),
        )
        assertEquals(0, lib.preferences!!.getInt("choiceAdventure1005", 0))
    }

    @Test
    fun hedgeMaze_nuggletsBeforeMazeReturnsFalse() {
        val lib = libWithFinalStep("step2")
        assertEquals(
            "false",
            outputLib(lib, """print(to_string(hedge_maze("nugglets")));"""),
        )
        assertEquals(0, lib.preferences!!.getInt("choiceAdventure1005", 0))
    }

    @Test
    fun hedgeMaze_unknownTagReturnsFalse() {
        val lib = libWithFinalStep("step4")
        assertFalse(
            outputLib(lib, """print(to_string(hedge_maze("unknown")));""").toBooleanStrictOrNull() ?: true,
        )
    }
}
