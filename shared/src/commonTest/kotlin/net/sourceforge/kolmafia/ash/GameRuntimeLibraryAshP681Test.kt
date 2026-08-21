package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.GarbageBeanstalkSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP681Test {

    @Test
    fun revision_phase683() {
        assertEquals("phase814", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun wheel_setsStep10() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.GARBAGE, "step9")
        assertTrue(GarbageBeanstalkSync.applyFromChoice(GarbageBeanstalkSync.KEEP_ON_TURNIN, db))
        assertEquals("step10", db.getProgress(Quest.GARBAGE))
    }

    @Test
    fun questChoiceRules_wires679() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 679,
                responseText = "Keep On Turnin' the Wheel in the Sky",
                questDatabase = db,
            ),
        )
        assertEquals("step10", db.getProgress(Quest.GARBAGE))
    }
}
