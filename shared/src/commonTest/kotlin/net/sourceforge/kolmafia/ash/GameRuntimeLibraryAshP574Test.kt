package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.ZeppelinRonSync

class GameRuntimeLibraryAshP574Test {

    @Test
    fun revision_phase605() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun protestors_startRonStep1AndIncrement() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            ZeppelinRonSync.applyFromAdventure(
                url = "adventure.php?snarfblat=384",
                html = "You fight a protester",
                questDatabase = db,
                preferences = prefs,
                won = true,
            ),
        )
        assertEquals("step1", prefs.getString(Quest.RON.prefKey, ""))
        assertEquals(1, prefs.getInt("zeppelinProtestors", 0))
    }

    @Test
    fun lighter_addsFlamingCount() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            ZeppelinRonSync.applyProtestors(
                html = "group of 7 nearby protesters do the same",
                questDatabase = db,
                preferences = prefs,
                won = true,
            ),
        )
        assertEquals(8, prefs.getInt("zeppelinProtestors", 0))
    }

    @Test
    fun mobCleared_setsRonStep2() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            ZeppelinRonSync.applyProtestors(
                html = "mob has cleared out",
                questDatabase = db,
                preferences = prefs,
                won = true,
            ),
        )
        assertEquals("step2", prefs.getString(Quest.RON.prefKey, ""))
    }

    @Test
    fun sneakAboard_setsRonStep3() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val lib = GameRuntimeLibrary(preferences = prefs, questDatabase = db)
        lib.processVisitResponseHooks(
            """<html>sneak aboard the Zeppelin</html>""",
            "https://www.kingdomofloathing.com/adventure.php?snarfblat=385",
        )
        assertEquals("step3", prefs.getString(Quest.RON.prefKey, ""))
    }
}
