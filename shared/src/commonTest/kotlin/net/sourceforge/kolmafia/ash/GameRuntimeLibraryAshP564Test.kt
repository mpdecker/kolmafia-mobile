package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP564Test {

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
    fun manor3_finishesNecklaceAndDance() {
        val (lib, prefs) = libWithDb()
        lib.processVisitResponseHooks(
            """<html>Spookyraven Manor Third Floor</html>""",
            "https://www.kingdomofloathing.com/place.php?whichplace=manor3",
        )
        assertEquals("finished", prefs.getString(Quest.SPOOKYRAVEN_NECKLACE.prefKey, ""))
        assertEquals("finished", prefs.getString(Quest.SPOOKYRAVEN_DANCE.prefKey, ""))
    }

    @Test
    fun manor4_brickhole_setsManorStep3() {
        val (lib, prefs) = libWithDb()
        lib.processVisitResponseHooks(
            """<html>Spookyraven Manor Cellar <img src="sr_brickhole.gif"></html>""",
            "https://www.kingdomofloathing.com/place.php?whichplace=manor4",
        )
        assertEquals("step3", prefs.getString(Quest.MANOR.prefKey, ""))
    }

    @Test
    fun manor4_coldAsIce_finishesManor() {
        val (lib, prefs) = libWithDb {
            setString(Quest.MANOR.prefKey, "step3")
        }
        lib.processVisitResponseHooks(
            """<html>Spookyraven Manor Cellar Cold as ice and twice as smooth</html>""",
            "https://www.kingdomofloathing.com/place.php?whichplace=manor4",
        )
        assertEquals("finished", prefs.getString(Quest.MANOR.prefKey, ""))
        assertEquals("finished", prefs.getString(Quest.MACGUFFIN.prefKey, ""))
    }
}
