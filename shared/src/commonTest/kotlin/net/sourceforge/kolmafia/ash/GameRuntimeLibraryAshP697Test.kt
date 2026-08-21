package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.GarbageBeanstalkSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP697Test {

    @Test
    fun revision_phase701() {
        assertEquals("phase814", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun melonCollie_consumesRecordOnDecision2() {
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            GarbageBeanstalkSync.applyFromChoice(
                choiceId = 675,
                questDatabase = null,
                decision = 2,
                consumeItem = { id, qty -> consumed.add(id to qty) },
            ),
        )
        assertTrue(consumed.contains(GarbageBeanstalkSync.DRUM_N_BASS_RECORD to 1))
    }

    @Test
    fun melonCollie_skipsOtherDecisions() {
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertFalse(
            GarbageBeanstalkSync.applyFromChoice(
                choiceId = 675,
                questDatabase = null,
                decision = 1,
                consumeItem = { id, qty -> consumed.add(id to qty) },
            ),
        )
        assertTrue(consumed.isEmpty())
    }

    @Test
    fun questChoiceRules_wires675() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 675,
                responseText = "",
                questDatabase = db,
                decision = 2,
                preferences = prefs,
            ),
        )
    }
}
