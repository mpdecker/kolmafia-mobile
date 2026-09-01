package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.ThingWithNoNameSync

class GameRuntimeLibraryAshP637Test {

    @Test
    fun revision_phase635() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun win_consumesStonesResetsQuestsAndRecordsAscension() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.CLUMSINESS, "step2")
        db.setProgress(Quest.GLACIER, "started")
        db.setProgress(Quest.MAELSTROM, "step1")
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            ThingWithNoNameSync.apply(
                monster = "The Thing with No Name",
                won = true,
                questDatabase = db,
                preferences = prefs,
                ascensionNumber = 9,
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals(
            listOf(
                ThingWithNoNameSync.FURIOUS_STONE to 1,
                ThingWithNoNameSync.VANITY_STONE to 1,
                ThingWithNoNameSync.LECHEROUS_STONE to 1,
                ThingWithNoNameSync.JEALOUSY_STONE to 1,
                ThingWithNoNameSync.AVARICE_STONE to 1,
                ThingWithNoNameSync.GLUTTONOUS_STONE to 1,
            ),
            consumed,
        )
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.CLUMSINESS))
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.GLACIER))
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.MAELSTROM))
        assertEquals(9, prefs.getInt("lastThingWithNoNameDefeated"))
    }

    @Test
    fun loss_isNoOp() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.CLUMSINESS, "step2")
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertFalse(
            ThingWithNoNameSync.apply(
                monster = "The Thing with No Name",
                won = false,
                questDatabase = db,
                preferences = prefs,
                ascensionNumber = 3,
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertTrue(consumed.isEmpty())
        assertEquals("step2", db.getProgress(Quest.CLUMSINESS))
        assertEquals(0, prefs.getInt("lastThingWithNoNameDefeated", 0))
    }

    @Test
    fun otherMonster_isNoOp() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertFalse(
            ThingWithNoNameSync.apply(
                monster = "smut orc jacker",
                won = true,
                questDatabase = db,
                preferences = prefs,
                ascensionNumber = 1,
            ),
        )
    }
}
