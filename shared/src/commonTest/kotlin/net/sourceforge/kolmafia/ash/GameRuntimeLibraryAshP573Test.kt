package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP573Test {

    private fun libWithDb(): Pair<GameRuntimeLibrary, Preferences> {
        val prefs = Preferences(MapSettings())
        return GameRuntimeLibrary(preferences = prefs, questDatabase = QuestDatabase(prefs)) to prefs
    }

    @Test
    fun revision_phase605() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun airship_startsGarbageStep1() {
        val (lib, prefs) = libWithDb()
        lib.processVisitResponseHooks(
            """<html>The Airship</html>""",
            "https://www.kingdomofloathing.com/adventure.php?snarfblat=81",
        )
        assertEquals("step1", prefs.getString(Quest.GARBAGE.prefKey, ""))
    }

    @Test
    fun airship_immateria_step2() {
        val (lib, prefs) = libWithDb()
        lib.processVisitResponseHooks(
            """<html>we're looking for the Four Immateria</html>""",
            "https://www.kingdomofloathing.com/adventure.php?snarfblat=81",
        )
        assertEquals("step2", prefs.getString(Quest.GARBAGE.prefKey, ""))
    }

    @Test
    fun castleBasement_unlocksGround() {
        val (lib, prefs) = libWithDb()
        lib.processVisitResponseHooks(
            """<html>New Area Unlocked The Ground Floor</html>""",
            "https://www.kingdomofloathing.com/adventure.php?snarfblat=322",
        )
        assertEquals("step8", prefs.getString(Quest.GARBAGE.prefKey, ""))
        assertEquals(0, prefs.getInt("lastCastleGroundUnlock", -1))
    }

    @Test
    fun castleTop_setsStep9() {
        val (lib, prefs) = libWithDb()
        lib.processVisitResponseHooks(
            """<html>Castle Top Floor open</html>""",
            "https://www.kingdomofloathing.com/adventure.php?snarfblat=324",
        )
        assertEquals("step9", prefs.getString(Quest.GARBAGE.prefKey, ""))
    }
}
