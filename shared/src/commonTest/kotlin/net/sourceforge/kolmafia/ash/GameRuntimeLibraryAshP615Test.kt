package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.CouncilVisitSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP615Test {

    @Test
    fun revision_phase615() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_setsLastCouncilVisit() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            CouncilVisitSync.applyFromVisit(
                url = "council.php",
                html = "We require your aid",
                questDatabase = db,
                preferences = prefs,
                level = 8,
            ),
        )
        assertEquals(8, prefs.getInt("lastCouncilVisit", 0))
    }

    @Test
    fun exactLarvaString_consumesItem() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            CouncilVisitSync.applyFromVisit(
                url = "council.php",
                html = CouncilVisitSync.LARVA_CONSUME,
                questDatabase = db,
                preferences = prefs,
                level = 2,
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals(listOf(CouncilVisitSync.MOSQUITO_LARVA to 1), consumed)
    }

    @Test
    fun macguffinStarted_startsBlack() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.MACGUFFIN, QuestDatabase.STARTED)
        assertTrue(
            CouncilVisitSync.applyFromVisit(
                url = "place.php?whichplace=exploathing&action=expl_council",
                html = "",
                questDatabase = db,
                preferences = prefs,
                level = 11,
            ),
        )
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.BLACK))
    }

    @Test
    fun otherUrl_isNoOp() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertFalse(
            CouncilVisitSync.applyFromVisit(
                url = "main.php",
                html = CouncilVisitSync.LARVA_CONSUME,
                questDatabase = db,
                preferences = prefs,
                level = 4,
            ),
        )
        assertEquals(0, prefs.getInt("lastCouncilVisit", 0))
    }
}
