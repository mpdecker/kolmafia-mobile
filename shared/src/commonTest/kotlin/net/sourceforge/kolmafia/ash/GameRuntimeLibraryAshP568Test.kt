package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.PalindomeSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP568Test {

    private fun libWithDb(configure: Preferences.() -> Unit = {}): Pair<GameRuntimeLibrary, Preferences> {
        val prefs = Preferences(MapSettings()).also(configure)
        val db = QuestDatabase(prefs)
        return GameRuntimeLibrary(preferences = prefs, questDatabase = db) to prefs
    }

    @Test
    fun revision_phase605() {
        assertEquals("phase605", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun palindomeAdventure_startsQuest() {
        val (lib, prefs) = libWithDb()
        lib.processVisitResponseHooks(
            """<html>Welcome to the Palindome</html>""",
            "https://www.kingdomofloathing.com/adventure.php?snarfblat=386",
        )
        assertEquals("started", prefs.getString(Quest.PALINDOME.prefKey, ""))
    }

    @Test
    fun palMr_setsStep3() {
        val (lib, prefs) = libWithDb {
            setString(Quest.PALINDOME.prefKey, "started")
        }
        lib.processVisitResponseHooks(
            """<html>in the mood for a bowl of wet stunt nut stew</html>""",
            "https://www.kingdomofloathing.com/place.php?whichplace=palindome&action=pal_mr",
        )
        assertEquals("step3", prefs.getString(Quest.PALINDOME.prefKey, ""))
    }

    @Test
    fun drabBard_incrementsDudeCounter() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setString(Quest.PALINDOME.prefKey, "started")
        assertTrue(
            PalindomeSync.applyCombatWin(
                questDatabase = db,
                preferences = prefs,
                monster = "Drab Bard",
                won = true,
            ),
        )
        assertEquals(1, prefs.getInt("palindomeDudesDefeated", 0))
    }

    @Test
    fun drabBard_capsAt20() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setString(Quest.PALINDOME.prefKey, "started")
        prefs.setInt("palindomeDudesDefeated", 20)
        assertEquals(
            false,
            PalindomeSync.applyCombatWin(
                questDatabase = db,
                preferences = prefs,
                monster = "Bob Racecar",
                won = true,
            ),
        )
        assertEquals(20, prefs.getInt("palindomeDudesDefeated", 0))
    }

    @Test
    fun drAwkward_finishesPalindome() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setString(Quest.PALINDOME.prefKey, "step3")
        assertTrue(
            PalindomeSync.applyCombatWin(
                questDatabase = db,
                preferences = prefs,
                monster = "Dr. Awkward",
                won = true,
            ),
        )
        assertEquals("finished", prefs.getString(Quest.PALINDOME.prefKey, ""))
    }
}
