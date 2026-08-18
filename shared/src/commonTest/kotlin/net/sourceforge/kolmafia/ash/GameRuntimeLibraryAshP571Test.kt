package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.BlackForestSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP571Test {

    @Test
    fun revision_phase605() {
        assertEquals("phase635", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun blackberryBushes_setsProgress1() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            BlackForestSync.applyCombatWin(
                questDatabase = db,
                preferences = prefs,
                adventureId = "405",
                responseText = "find a row of blackberry bushes so thick",
                won = true,
            ),
        )
        assertEquals(1, prefs.getInt("blackForestProgress", 0))
        assertEquals("step1", prefs.getString(Quest.BLACK.prefKey, ""))
    }

    @Test
    fun blackMarketTrail_finishesForest() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            BlackForestSync.applyCombatWin(
                questDatabase = db,
                preferences = prefs,
                adventureId = "405",
                responseText = "discover the trail leading to the Black Market",
                won = true,
            ),
        )
        assertEquals(5, prefs.getInt("blackForestProgress", 0))
        assertEquals("step2", prefs.getString(Quest.BLACK.prefKey, ""))
        assertEquals("step1", prefs.getString(Quest.MACGUFFIN.prefKey, ""))
    }

    @Test
    fun woodsVisit_blackmarketGif() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val lib = GameRuntimeLibrary(preferences = prefs, questDatabase = db)
        lib.processVisitResponseHooks(
            """<html><img src="blackmarket.gif"></html>""",
            "https://www.kingdomofloathing.com/woods.php",
        )
        assertEquals(5, prefs.getInt("blackForestProgress", 0))
        assertEquals("step2", prefs.getString(Quest.BLACK.prefKey, ""))
    }
}
