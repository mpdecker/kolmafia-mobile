package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContestBoothSyncTest {

    @Test
    fun parseContestBooth_setsRankOnEntry() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val text = "You qualify to begin the contest at rank <b>#3</b>."
        assertTrue(ContestBoothSync.parseContestBooth(1, text, prefs, db))
        assertEquals(2, prefs.getInt("nsContestants1", -1))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.FINAL))
    }

    @Test
    fun parseContestBooth_claimPrizeAdvancesStep3() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.FINAL, QuestDatabase.STARTED)
        val text = "Here is your World's Best Adventurer sash."
        assertTrue(ContestBoothSync.parseContestBooth(4, text, prefs, db))
        assertEquals("step3", db.getProgress(Quest.FINAL))
    }

    @Test
    fun parseContestBooth_setsOptimismChallenge() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            ContestBoothSync.parseContestBooth(
                0,
                "You feel pretty good about your chances in the Strongest Adventurer contest.",
                prefs,
                null,
            ),
        )
        assertEquals("Muscle", prefs.getString("nsChallenge1", ""))
    }

    @Test
    fun parseMazeTrap_setsElementChallenge() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            ContestBoothSync.parseMazeTrap(
                1005,
                "You take 12 hot damage from the smoldering bushes.",
                prefs,
            ),
        )
        assertEquals("hot", prefs.getString("nsChallenge3", ""))
    }

    @Test
    fun visitHedgeMazeChoice_setsRoomAndStep4() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(ContestBoothSync.visitHedgeMazeChoice(1007, prefs, db))
        assertEquals(3, prefs.getInt("currentHedgeMazeRoom", 0))
        assertEquals("step4", db.getProgress(Quest.FINAL))
    }
}
