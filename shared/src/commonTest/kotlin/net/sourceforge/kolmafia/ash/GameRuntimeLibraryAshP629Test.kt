package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestCombatWinExtrasSync
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestFightRules
import net.sourceforge.kolmafia.quest.TavernCellarSync

class GameRuntimeLibraryAshP629Test {

    @Test
    fun revision_phase629() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun screambat_advancesBatThroughStep2() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.BAT, "step1")
        assertTrue(QuestCombatWinExtrasSync.apply("screambat", db, prefs))
        assertEquals("step2", db.getProgress(Quest.BAT))
        assertTrue(QuestCombatWinExtrasSync.apply("screambat", db, prefs))
        assertEquals("step3", db.getProgress(Quest.BAT))
        assertFalse(QuestCombatWinExtrasSync.apply("screambat", db, prefs))
        assertEquals("step3", db.getProgress(Quest.BAT))
    }

    @Test
    fun sourceAgentWin_incrementsDefeated() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("sourceAgentsDefeated", 2)
        val db = QuestDatabase(prefs)
        assertTrue(QuestCombatWinExtrasSync.apply("Source Agent", db, prefs))
        assertEquals(3, prefs.getInt("sourceAgentsDefeated"))
    }

    @Test
    fun wartDinsey_recordsAscension() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(QuestCombatWinExtrasSync.apply("Wart Dinsey", db, prefs, ascensionNumber = 7))
        assertEquals(7, prefs.getInt("lastWartDinseyDefeated"))
    }

    @Test
    fun baron_marksTavernMansion() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("lastTavernSquare", 8)
        prefs.setInt("lastTavernAscension", 3)
        prefs.setString("tavernLayout", TavernCellarSync.EMPTY_LAYOUT)
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestCombatWinExtrasSync.apply(
                "Baron Von Ratsworth",
                db,
                prefs,
                ascensionNumber = 3,
            ),
        )
        assertEquals('6', prefs.getString("tavernLayout")[7])
    }

    @Test
    fun snojoWin_tracksFreeFightsAndStatWins() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_snojoParts", 4)
        prefs.setString("snojoSetting", "MUSCLE")
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestCombatWinExtrasSync.apply("X-32-F Combat Training Snowman", db, prefs),
        )
        assertEquals(4, prefs.getInt("_snojoFreeFights"))
        assertEquals(1, prefs.getInt("snojoMuscleWins"))
    }

    @Test
    fun snojoWin_overTenPartsSkipsStatWins() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_snojoParts", 12)
        prefs.setString("snojoSetting", "MOXIE")
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestCombatWinExtrasSync.apply("X-32-F Combat Training Snowman", db, prefs),
        )
        assertEquals(10, prefs.getInt("_snojoFreeFights"))
        assertEquals(0, prefs.getInt("snojoMoxieWins", 0))
    }

    @Test
    fun applyCombatWin_wiresScreambat() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.BAT, QuestDatabase.STARTED)
        assertTrue(
            QuestFightRules.applyCombat(db, "screambat", won = true, preferences = prefs).advanced,
        )
        assertEquals("step1", db.getProgress(Quest.BAT))
    }
}
