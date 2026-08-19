package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestFightStartedSync
import net.sourceforge.kolmafia.quest.QuestItemUsedSync

class GameRuntimeLibraryAshP668Test {

    @Test
    fun revision_phase671() {
        assertEquals("phase671", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun spiderWeb_addsThreeLairProgress() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setInt("_villainLairProgress", 2)
        assertTrue(
            QuestItemUsedSync.apply(
                QuestItemUsedSync.SPIDER_WEB,
                "Three other minions stumble into the web.",
                db,
                prefs,
            ),
        )
        assertEquals(5, prefs.getInt("_villainLairProgress"))
        assertTrue(prefs.getBoolean("_villainLairWebUsed"))
    }

    @Test
    fun bowlingBall_resetsReturnAndIncrementsAlley() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setInt("cosmicBowlingBallReturnCombats", 7)
        prefs.setInt("hiddenBowlingAlleyProgress", 3)
        assertTrue(
            QuestItemUsedSync.apply(
                QuestItemUsedSync.COSMIC_BOWLING_BALL,
                "you hurl it down the ancient lanes and knock pins.",
                db,
                prefs,
            ),
        )
        assertEquals(0, prefs.getInt("cosmicBowlingBallReturnCombats"))
        assertEquals(4, prefs.getInt("hiddenBowlingAlleyProgress"))
    }

    @Test
    fun zeppelinBomb_addsTenProtestors() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setInt("zeppelinProtestors", 4)
        assertTrue(
            QuestItemUsedSync.apply(
                QuestItemUsedSync.BOMB_OF_UNKNOWN_ORIGIN,
                "They decide to find something else to protest.",
                db,
                prefs,
            ),
        )
        assertEquals(14, prefs.getInt("zeppelinProtestors"))
    }

    @Test
    fun dnaPair_appendsCyrusAdjective() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestItemUsedSync.apply(QuestItemUsedSync.CA_BASE_PAIR, "used", db, prefs),
        )
        assertEquals("stronger", prefs.getString("cyrusAdjectives"))
        QuestItemUsedSync.apply(QuestItemUsedSync.AG_BASE_PAIR, "used", db, prefs)
        assertEquals("stronger,faster", prefs.getString("cyrusAdjectives"))
    }

    @Test
    fun cyrusFightStart_resetsAminoAcids() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("aminoAcidsUsed", 4)
        assertTrue(
            QuestFightStartedSync.apply(
                monster = "Cyrus the Virus",
                html = "fight start",
                preferences = prefs,
                turnsPlayed = 10,
            ),
        )
        assertEquals(0, prefs.getInt("aminoAcidsUsed"))
    }

    @Test
    fun malware_setsDailyDungeonFlag() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestItemUsedSync.apply(
                QuestItemUsedSync.DAILY_DUNGEON_MALWARE,
                "It's a UNIX system. I know this.",
                db,
                prefs,
            ),
        )
        assertTrue(prefs.getBoolean("_dailyDungeonMalwareUsed"))
    }
}
