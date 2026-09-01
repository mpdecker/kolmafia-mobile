package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.GarbageBeanstalkSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP685Test {

    @Test
    fun revision_phase689() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun groundFloorUnlock_setsStep8() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            GarbageBeanstalkSync.applyFromChoice(
                choiceId = 669,
                questDatabase = db,
                html = "New Area Unlocked<br>The Ground Floor",
                preferences = prefs,
                ascensionNumber = 12,
            ),
        )
        assertEquals("step8", db.getProgress(Quest.GARBAGE))
        assertEquals(12, prefs.getInt("lastCastleGroundUnlock"))
    }

    @Test
    fun groundFloorUnlock_requiresBothPhrases() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertFalse(
            GarbageBeanstalkSync.applyFromChoice(
                choiceId = 670,
                questDatabase = db,
                html = "New Area Unlocked",
                preferences = prefs,
                ascensionNumber = 1,
            ),
        )
    }

    @Test
    fun questChoiceRules_wires671() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 671,
                responseText = "New Area Unlocked: The Ground Floor",
                questDatabase = db,
                preferences = prefs,
                ascensionNumber = 3,
            ),
        )
        assertEquals("step8", db.getProgress(Quest.GARBAGE))
        assertEquals(3, prefs.getInt("lastCastleGroundUnlock"))
    }
}
