package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.TavernCellarSync

class GameRuntimeLibraryAshP620Test {

    @Test
    fun revision_phase623() {
        assertEquals("phase743", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun exploreUrl_setsLastSquareAndExplored() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            TavernCellarSync.applyFromVisit(
                url = "cellar.php?action=explore&whichspot=4",
                html = "You wander into the gloom.",
                preferences = prefs,
                questDatabase = QuestDatabase(prefs),
                ascensionNumber = 1,
            ),
        )
        assertEquals(4, prefs.getInt("lastTavernSquare"))
        assertEquals('1', prefs.getString("tavernLayout")[3])
    }

    @Test
    fun faucetNc_setsCode3AndRatStep2() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.RAT, "step1")
        assertTrue(
            TavernCellarSync.applyFromVisit(
                url = "cellar.php?action=explore&whichspot=8",
                html = "Hot and Cold Running Rats",
                preferences = prefs,
                questDatabase = db,
                ascensionNumber = 1,
            ),
        )
        assertEquals('3', prefs.getString("tavernLayout")[7])
        assertEquals("step2", db.getProgress(Quest.RAT))
    }

    @Test
    fun fightBaron_setsCode4() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("lastTavernSquare", 12)
        prefs.setString("tavernLayout", TavernCellarSync.EMPTY_LAYOUT)
        assertTrue(
            TavernCellarSync.postVisit(
                url = "fight.php",
                html = "Baron von Ratsworth glares.",
                preferences = prefs,
                questDatabase = QuestDatabase(prefs),
                ascensionNumber = 1,
            ),
        )
        assertEquals('4', prefs.getString("tavernLayout")[11])
    }

    @Test
    fun skipExplore_isNoOp() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            TavernCellarSync.postVisit(
                url = "cellar.php?action=explore&whichspot=4",
                html = "You wander into the gloom.",
                preferences = prefs,
                questDatabase = QuestDatabase(prefs),
                ascensionNumber = 1,
                shouldSkipExplore = { true },
            ),
        )
        assertEquals(TavernCellarSync.EMPTY_LAYOUT, TavernCellarSync.tavernLayout(prefs, 1))
    }

    @Test
    fun barrelsChoice_setsCode5() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("lastTavernSquare", 9)
        assertTrue(
            TavernCellarSync.postVisit(
                url = "choice.php",
                html = "<form><input name=\"whichchoice\" value=\"509\">",
                preferences = prefs,
                questDatabase = QuestDatabase(prefs),
                ascensionNumber = 1,
            ),
        )
        assertEquals('5', prefs.getString("tavernLayout")[8])
    }
}
