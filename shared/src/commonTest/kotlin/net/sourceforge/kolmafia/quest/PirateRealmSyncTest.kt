package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PirateRealmSyncTest {

    @Test
    fun parseResponse_sail1AdvancesToStep1() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.PIRATEREALM, QuestDatabase.STARTED)
        assertTrue(PirateRealmSync.parseResponse("""<img src="sail1.gif">""", db, prefs))
        assertEquals("step1", db.getProgress(Quest.PIRATEREALM))
        assertTrue(prefs.getBoolean("_prToday", false))
    }

    @Test
    fun parseResponse_sail2AdvancesToStep6() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.PIRATEREALM, "step1")
        assertTrue(PirateRealmSync.parseResponse("""<img src="sail2.gif">""", db, prefs))
        assertEquals("step6", db.getProgress(Quest.PIRATEREALM))
    }

    @Test
    fun parseResponse_sail3AdvancesToStep11() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.PIRATEREALM, "step6")
        assertTrue(PirateRealmSync.parseResponse("""<img src="sail3.gif">""", db, prefs))
        assertEquals("step11", db.getProgress(Quest.PIRATEREALM))
    }

    @Test
    fun parseResponse_envelopeFinishesAndUnlocksPrefs() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.PIRATEREALM, "step11")
        val html = """
            You found an envelope with your name on it and a piratical blunderbuss
            and a pirate fork and Scurvy and Sobriety Prevention and lucky gold ring
            and Menacing Man o' War and Swift Clipper.
        """.trimIndent()
        assertTrue(PirateRealmSync.parseResponse(html, db, prefs))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.PIRATEREALM))
        assertTrue(prefs.getBoolean("pirateRealmUnlockedBlunderbuss", false))
        assertTrue(prefs.getBoolean("pirateRealmUnlockedFork", false))
        assertTrue(prefs.getBoolean("pirateRealmUnlockedScurvySkillbook", false))
        assertTrue(prefs.getBoolean("pirateRealmUnlockedGoldRing", false))
        assertTrue(prefs.getBoolean("pirateRealmUnlockedManOWar", false))
        assertTrue(prefs.getBoolean("pirateRealmUnlockedClipper", false))
    }

    @Test
    fun parseResponse_prAlwaysSkipsPrToday() {
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("prAlways", true)
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.PIRATEREALM, QuestDatabase.STARTED)
        PirateRealmSync.parseResponse("""<img src="sail1.gif">""", db, prefs)
        assertEquals(false, prefs.getBoolean("_prToday", false))
    }

    @Test
    fun getPirateRealmIslandNumber_boundaries() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.PIRATEREALM, "step2")
        assertEquals(0, PirateRealmSync.getPirateRealmIslandNumber(db))
        db.setProgress(Quest.PIRATEREALM, "step7")
        assertEquals(1, PirateRealmSync.getPirateRealmIslandNumber(db))
        db.setProgress(Quest.PIRATEREALM, "step12")
        assertEquals(2, PirateRealmSync.getPirateRealmIslandNumber(db))
    }

    @Test
    fun setPirateRealmIslandQuestProgress_formula() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.PIRATEREALM, QuestDatabase.STARTED)
        assertTrue(PirateRealmSync.setPirateRealmIslandQuestProgress(db, 0, 0))
        assertEquals("step2", db.getProgress(Quest.PIRATEREALM))
        assertTrue(PirateRealmSync.setPirateRealmIslandQuestProgress(db, 1, 1))
        assertEquals("step8", db.getProgress(Quest.PIRATEREALM))
        assertTrue(PirateRealmSync.setPirateRealmIslandQuestProgress(db, 2, 1))
        assertEquals("step13", db.getProgress(Quest.PIRATEREALM))
    }

    @Test
    fun applyChoice_islandPickResetsCountersAndSetsIslandName() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.PIRATEREALM, "step2")
        prefs.setInt("_pirateRealmIslandMonstersDefeated", 3)
        prefs.setInt("_pirateRealmSailingTurns", 2)
        prefs.setBoolean("_pirateRealmWindicleUsed", true)
        PirateRealmSync.applyChoice(1352, "", 1, "Battle Island", db, prefs)
        assertEquals("step2", db.getProgress(Quest.PIRATEREALM))
        assertEquals(0, prefs.getInt("_pirateRealmIslandMonstersDefeated", -1))
        assertEquals(0, prefs.getInt("_pirateRealmSailingTurns", -1))
        assertEquals(false, prefs.getBoolean("_pirateRealmWindicleUsed", true))
        assertEquals("Battle Island", prefs.getString("_lastPirateRealmIsland", ""))
    }

    @Test
    fun applyChoice_sailingTurnsAdvanceToProgress1() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.PIRATEREALM, "step2")
        prefs.setInt("_pirateRealmShipSpeed", 2)
        prefs.setInt("_pirateRealmSailingTurns", 1)
        assertTrue(PirateRealmSync.applyChoice(1356, "", 1, null, db, prefs))
        assertEquals("step3", db.getProgress(Quest.PIRATEREALM))
    }

    @Test
    fun applyIslandCombatWin_thresholdIsland0() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.PIRATEREALM, "step3")
        prefs.setInt("_pirateRealmIslandMonstersDefeated", 3)
        assertTrue(PirateRealmSync.applyIslandCombatWin(db, prefs))
        assertEquals("step5", db.getProgress(Quest.PIRATEREALM))
    }

    @Test
    fun applyWindicleUse_addsThreeDefeats() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.PIRATEREALM, "step3")
        prefs.setInt("_pirateRealmIslandMonstersDefeated", 1)
        assertTrue(
            PirateRealmSync.applyWindicleUse(
                db,
                prefs,
                "Your foe is blown clear of the island",
            ),
        )
        assertEquals("step5", db.getProgress(Quest.PIRATEREALM))
    }

    @Test
    fun applyWindicleFromFightHtml_wiresIslandCombat() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.PIRATEREALM, "step3")
        prefs.setInt("_pirateRealmIslandMonstersDefeated", 1)
        val html = "You use your windicle. Your foe is blown clear of the island."
        assertTrue(
            PirateRealmSync.applyWindicleFromFightHtml(
                html,
                PirateRealmSync.PIRATEREALM_ISLAND_ADVENTURE.toString(),
                db,
                prefs,
            ),
        )
        assertEquals(true, prefs.getBoolean("_pirateRealmWindicleUsed", false))
        assertEquals("step5", db.getProgress(Quest.PIRATEREALM))
    }

    @Test
    fun applyWindicleFromFightHtml_skipsOtherAdventures() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.PIRATEREALM, "step3")
        assertEquals(
            false,
            PirateRealmSync.applyWindicleFromFightHtml(
                "Your foe is blown clear of the island",
                "999",
                db,
                prefs,
            ),
        )
    }
}
