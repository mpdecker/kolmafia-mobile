package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP575Test {

    @Test
    fun revision_phase605() {
        assertEquals("phase826", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun woods_emptybm_setsWuTangDefeated() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val lib = GameRuntimeLibrary(preferences = prefs, questDatabase = db)
        lib.processVisitResponseHooks(
            """<html><a href="woods.php?action=emptybm">Empty Black Market</a></html>""",
            "https://www.kingdomofloathing.com/woods.php",
        )
        assertEquals(0, prefs.getInt("lastWuTangDefeated", -1))
    }

    @Test
    fun woods_templeGif_setsLastTempleUnlock() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val lib = GameRuntimeLibrary(preferences = prefs, questDatabase = db)
        lib.processVisitResponseHooks(
            """<html><img src="temple.gif"></html>""",
            "https://www.kingdomofloathing.com/woods.php",
        )
        assertEquals(0, prefs.getInt("lastTempleUnlock", -1))
    }

    @Test
    fun woods_blackmarket_setsMacguffinStep1() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val lib = GameRuntimeLibrary(preferences = prefs, questDatabase = db)
        lib.processVisitResponseHooks(
            """<html><img src="blackmarket.gif"></html>""",
            "https://www.kingdomofloathing.com/woods.php",
        )
        assertEquals("step1", prefs.getString(Quest.MACGUFFIN.prefKey, ""))
        assertEquals("step2", prefs.getString(Quest.BLACK.prefKey, ""))
    }
}
