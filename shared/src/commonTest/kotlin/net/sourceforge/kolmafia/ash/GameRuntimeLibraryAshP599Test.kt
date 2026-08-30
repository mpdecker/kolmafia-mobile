package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.BatholeSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.ToppingPlaceSync

class GameRuntimeLibraryAshP599Test {

    @Test
    fun revision_phase605() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun chasm_bridgeFinishesStep1() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            ToppingPlaceSync.applyFromChasm(
                url = "place.php?whichplace=orc_chasm",
                html = "Huzzah!  The bridge is finished!",
                questDatabase = db,
                itemCount = { if (it == ToppingPlaceSync.MORNINGWOOD_PLANK) 2 else 0 },
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals("step1", db.getProgress(Quest.TOPPING))
        assertEquals(listOf(ToppingPlaceSync.MORNINGWOOD_PLANK to 2), consumed)
    }

    @Test
    fun highlands_firesAdvanceToStep3() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.TOPPING, "step1")
        assertTrue(
            ToppingPlaceSync.applyFromHighlands(
                url = "place.php?whichplace=highlands",
                html = "orcchasm/fire1.gif orcchasm/fire2.gif orcchasm/fire3.gif",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("booPeakLit", false))
        assertEquals(15, prefs.getInt("twinPeakProgress", 0))
        assertTrue(prefs.getBoolean("oilPeakLit", false))
        assertEquals("step3", db.getProgress(Quest.TOPPING))
    }

    @Test
    fun highlandsDude_pizzaSetsStep2() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            ToppingPlaceSync.applyFromHighlands(
                url = "place.php?whichplace=highlands&action=highlands_dude",
                html = "trying to, like, order a pizza",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals("step2", db.getProgress(Quest.TOPPING))
    }

    @Test
    fun bathole_imageSetsBatStep() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            BatholeSync.applyFromVisit(
                url = "place.php?whichplace=bathole",
                html = """<img src="bathole_3.gif">""",
                questDatabase = db,
            ),
        )
        assertEquals("step2", db.getProgress(Quest.BAT))
    }
}
