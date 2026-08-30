package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP566Test {

    private fun libWithDb(configure: Preferences.() -> Unit = {}): Pair<GameRuntimeLibrary, Preferences> {
        val prefs = Preferences(MapSettings()).also(configure)
        val db = QuestDatabase(prefs)
        return GameRuntimeLibrary(preferences = prefs, questDatabase = db) to prefs
    }

    @Test
    fun revision_phase605() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun pyramidPlace_syncsChamberPrefsAndPosition() {
        val (lib, prefs) = libWithDb()
        lib.processVisitResponseHooks(
            """
            <html>
            <img src="pyramid_middle.gif">
            <img src="pyramid_bottom.gif">
            <img src="pyramid_controlroom.gif">
            <a href="place.php?whichplace=pyramid&action=pyramid_state3">chamber</a>
            <a href="place.php?whichplace=pyramid&action=pyramid_state1a">bomb hole</a>
            </html>
            """.trimIndent(),
            "https://www.kingdomofloathing.com/place.php?whichplace=pyramid",
        )
        assertEquals("step3", prefs.getString(Quest.PYRAMID.prefKey, ""))
        assertTrue(prefs.getBoolean("middleChamberUnlock", false))
        assertTrue(prefs.getBoolean("lowerChamberUnlock", false))
        assertTrue(prefs.getBoolean("controlRoomUnlock", false))
        assertEquals(3, prefs.getInt("pyramidPosition", 0))
        assertTrue(prefs.getBoolean("pyramidBombUsed", false))
    }

    @Test
    fun desertBeachModel_startsPyramid() {
        val (lib, prefs) = libWithDb()
        lib.processVisitResponseHooks(
            """<html>the model bursts into flames and is quickly consumed</html>""",
            "https://www.kingdomofloathing.com/place.php?whichplace=desertbeach&action=db_pyramid1",
        )
        assertEquals("started", prefs.getString(Quest.PYRAMID.prefKey, ""))
    }
}
