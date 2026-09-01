package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.SpookyravenCombatSync
import net.sourceforge.kolmafia.quest.SpookyravenManorVisitSync

class GameRuntimeLibraryAshP565Test {

    @Test
    fun revision_phase605() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun lordSpookyraven_finishesManor() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setString(Quest.MANOR.prefKey, "step3")
        assertTrue(
            SpookyravenCombatSync.applyCombatWin(
                questDatabase = db,
                preferences = prefs,
                monster = "Lord Spookyraven",
                won = true,
            ),
        )
        assertEquals("finished", prefs.getString(Quest.MANOR.prefKey, ""))
    }

    @Test
    fun writingDesk_incrementsCounter() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setString(Quest.SPOOKYRAVEN_NECKLACE.prefKey, "started")
        assertTrue(
            SpookyravenCombatSync.applyCombatWin(
                questDatabase = db,
                preferences = prefs,
                monster = "writing desk",
                won = true,
            ),
        )
        assertEquals(1, prefs.getInt("writingDesksDefeated", 0))
    }

    @Test
    fun ballroomHavingABall_finishesDance() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            SpookyravenManorVisitSync.applyFromVisit(
                url = "adventure.php?snarfblat=395",
                html = "Having a Ball in the Ballroom",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals("finished", prefs.getString(Quest.SPOOKYRAVEN_DANCE.prefKey, ""))
    }
}
