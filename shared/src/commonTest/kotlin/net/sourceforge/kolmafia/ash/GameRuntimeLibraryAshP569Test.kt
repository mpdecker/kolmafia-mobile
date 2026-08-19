package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DesertVisitSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP569Test {

    private fun libWithDb(configure: Preferences.() -> Unit = {}): Pair<GameRuntimeLibrary, Preferences> {
        val prefs = Preferences(MapSettings()).also(configure)
        val db = QuestDatabase(prefs)
        return GameRuntimeLibrary(preferences = prefs, questDatabase = db) to prefs
    }

    @Test
    fun revision_phase605() {
        assertEquals("phase671", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun desertBeach_literalPercent_setsExploration() {
        val (lib, prefs) = libWithDb()
        lib.processVisitResponseHooks(
            """
            <html>
            <div id=db_l11desertlabel><a href="#">(42%explored)</a></div>
            </html>
            """.trimIndent(),
            "https://www.kingdomofloathing.com/place.php?whichplace=desertbeach",
        )
        assertEquals(42, prefs.getInt("desertExploration", 0))
    }

    @Test
    fun desertBeach_zonefontGifs_setsExploration() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val html = """
            <div id=db_l11desertlabel><a href="#">
            <img src="otherimages/zonefont/lparen.gif">
            <img src="otherimages/zonefont/7.gif">
            <img src="otherimages/zonefont/5.gif">
            <img src="otherimages/zonefont/percent.gif">
            <img src="otherimages/zonefont/e.gif">
            <img src="otherimages/zonefont/x.gif">
            <img src="otherimages/zonefont/p.gif">
            <img src="otherimages/zonefont/l.gif">
            <img src="otherimages/zonefont/o.gif">
            <img src="otherimages/zonefont/r.gif">
            <img src="otherimages/zonefont/e.gif">
            <img src="otherimages/zonefont/d.gif">
            <img src="otherimages/zonefont/rparen.gif">
            </a></div>
        """.trimIndent()
        assertTrue(DesertVisitSync.applyFromVisit(
            url = "place.php?whichplace=desertbeach",
            html = html,
            questDatabase = db,
            preferences = prefs,
        ))
        assertEquals(75, prefs.getInt("desertExploration", 0))
    }

    @Test
    fun desertBeach_100Percent_finishesDesertQuest() {
        val (lib, prefs) = libWithDb {
            setString(Quest.DESERT.prefKey, "started")
        }
        lib.processVisitResponseHooks(
            """<div id=db_l11desertlabel><a>(100%explored)</a></div>""",
            "https://www.kingdomofloathing.com/place.php?whichplace=desertbeach",
        )
        assertEquals(100, prefs.getInt("desertExploration", 0))
        assertEquals("finished", prefs.getString(Quest.DESERT.prefKey, ""))
    }

    @Test
    fun desertBeach_dbOasis_setsOasisAvailable() {
        val (lib, prefs) = libWithDb()
        lib.processVisitResponseHooks(
            """<html><img src="db_oasis.gif"></html>""",
            "https://www.kingdomofloathing.com/place.php?whichplace=desertbeach",
        )
        assertTrue(prefs.getBoolean("oasisAvailable", false))
    }

    @Test
    fun pyramidUnlockAction_skipsBeachExploration() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertFalse(
            DesertVisitSync.applyFromVisit(
                url = "place.php?whichplace=desertbeach&action=db_pyramid1",
                html = """<div id=db_l11desertlabel><a>(50%explored)</a></div>""",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals(0, prefs.getInt("desertExploration", 0))
    }
}
