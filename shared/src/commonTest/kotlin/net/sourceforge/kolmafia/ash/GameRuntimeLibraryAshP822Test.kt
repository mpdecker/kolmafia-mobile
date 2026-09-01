package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.LanguageFluencyChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP822Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun monolith_parsesAndConsumesCore() {
        val prefs = Preferences(MapSettings())
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            LanguageFluencyChoiceSync.apply(
                choiceId = 1249,
                decision = 1,
                html = "Fluency is now 25%",
                preferences = prefs,
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals(25, prefs.getInt("procrastinatorLanguageFluency", 0))
        assertEquals(listOf(LanguageFluencyChoiceSync.MURDERBOT_DATA_CORE to 1), consumed)
    }

    @Test
    fun terminal_acquireResetsFluency() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("procrastinatorLanguageFluency", 50)
        assertTrue(
            LanguageFluencyChoiceSync.apply(
                choiceId = 1250,
                decision = 1,
                html = "You acquire an item: disk",
                preferences = prefs,
            ),
        )
        assertEquals(0, prefs.getInt("procrastinatorLanguageFluency", -1))
    }

    @Test
    fun curses_consumesLockerKey() {
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            LanguageFluencyChoiceSync.apply(
                choiceId = 1251,
                decision = 1,
                html = "You acquire an item: hex",
                preferences = Preferences(MapSettings()),
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals(listOf(LanguageFluencyChoiceSync.PROCRASTINATOR_LOCKER_KEY to 1), consumed)
    }

    @Test
    fun questChoiceRules_wires1249() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1249,
                responseText = "Fluency is now 5%",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals(5, prefs.getInt("procrastinatorLanguageFluency", 0))
    }
}
