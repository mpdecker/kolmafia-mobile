package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP563Test {

    private fun libWithDb(configure: Preferences.() -> Unit = {}): Pair<GameRuntimeLibrary, Preferences> {
        val prefs = Preferences(MapSettings()).also(configure)
        val db = QuestDatabase(prefs)
        return GameRuntimeLibrary(preferences = prefs, questDatabase = db) to prefs
    }

    @Test
    fun revision_phase605() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun manor1_kitchenUnlock_startsNecklace() {
        val (lib, prefs) = libWithDb()
        lib.processVisitResponseHooks(
            """<html>Spookyraven Manor First Floor <a href="adventure.php?snarfblat=388">Kitchen</a></html>""",
            "https://www.kingdomofloathing.com/place.php?whichplace=manor1",
        )
        assertEquals("started", prefs.getString(Quest.SPOOKYRAVEN_NECKLACE.prefKey, ""))
    }

    @Test
    fun manor1_secondFloorLink_finishesNecklace() {
        val (lib, prefs) = libWithDb {
            setString(Quest.SPOOKYRAVEN_NECKLACE.prefKey, "started")
        }
        lib.processVisitResponseHooks(
            """<html>whichplace=manor2 upstairs</html>""",
            "https://www.kingdomofloathing.com/place.php?whichplace=manor1",
        )
        assertEquals("finished", prefs.getString(Quest.SPOOKYRAVEN_NECKLACE.prefKey, ""))
        assertEquals(0, prefs.getInt("lastSecondFloorUnlock", -1))
    }

    @Test
    fun manor2_bathroomLink_startsDanceStep1() {
        val (lib, prefs) = libWithDb()
        lib.processVisitResponseHooks(
            """
            <html>Spookyraven Manor Second Floor
            <a href="adventure.php?snarfblat=392">Bathroom</a>
            </html>
            """.trimIndent(),
            "https://www.kingdomofloathing.com/place.php?whichplace=manor2",
        )
        assertEquals("step1", prefs.getString(Quest.SPOOKYRAVEN_DANCE.prefKey, ""))
        assertEquals("finished", prefs.getString(Quest.SPOOKYRAVEN_NECKLACE.prefKey, ""))
    }
}
