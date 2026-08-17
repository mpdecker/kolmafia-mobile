package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.BlackForestSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP577Test {

    @Test
    fun woods_wcroad_setsCitadelStep1() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            BlackForestSync.applyWoodsVisit(
                url = "woods.php",
                html = """<img src="wcroad.gif">""",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals("step1", db.getProgress(Quest.CITADEL))
    }

    @Test
    fun woods_dakota_startsTemple() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            BlackForestSync.applyWoodsVisit(
                url = "woods.php?action=woods_dakota",
                html = "I need you to pick up a couple things for me",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.TEMPLE))
    }

    @Test
    fun woods_dakota_unlock_consumesItemsAndSetsTemple() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val consumed = mutableListOf<Int>()
        assertTrue(
            BlackForestSync.applyWoodsVisit(
                url = "woods.php?action=woods_dakota",
                html = "I'll make a note of the temple's location",
                questDatabase = db,
                preferences = prefs,
                ascensionNumber = 7,
                consumeItem = { consumed.add(it) },
            ),
        )
        assertEquals(7, prefs.getInt("lastTempleUnlock", -1))
        assertTrue(consumed.contains(BlackForestSync.BENDY_STRAW))
        assertTrue(consumed.contains(BlackForestSync.PLANT_FOOD))
        assertTrue(consumed.contains(BlackForestSync.SEWING_KIT))
    }

    @Test
    fun woods_hippy_finishesHippyQuest() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            BlackForestSync.applyWoodsVisit(
                url = "woods.php?action=woods_hippy",
                html = "You've got this cool boat now",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.HIPPY))
    }
}
