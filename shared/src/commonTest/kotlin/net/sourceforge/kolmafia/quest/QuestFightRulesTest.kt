package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuestFightRulesTest {

    @Test
    fun applyCombat_unknownClassWinAdvancesStep2() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step1")
        assertTrue(QuestFightRules.applyCombat(db, "The Unknown Seal Clubber", won = true).advanced)
        assertEquals("step2", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun applyCombat_beelzebozoWinAdvancesStep6() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step5")
        assertTrue(QuestFightRules.applyCombat(db, "Clownlord Beelzebozo", won = true).advanced)
        assertEquals("step6", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun applyCombat_lossAdvancesToStep18() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step17")
        assertTrue(QuestFightRules.applyCombat(db, "menacing thug", won = false).advanced)
        assertEquals("step18", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun applyCombat_winAdvancesToStep19() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step18")
        assertTrue(QuestFightRules.applyCombat(db, "menacing thug", won = true).advanced)
        assertEquals("step19", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun applyCombat_volcanoMapFinishesStep25() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step24")
        assertTrue(
            QuestFightRules.applyCombat(
                db, "", won = true, itemsGained = listOf("volcano map"),
            ).advanced,
        )
        assertEquals("step25", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun applyFightStarted_volcanicCaveAdvancesStep28() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step27")
        assertTrue(
            QuestFightRules.applyFightStarted(
                db, "Gorgolok, the Infernal Seal (Volcanic Cave)",
            )
        )
        assertEquals("step28", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun applyCombat_volcanicWinAdvancesStep29() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step28")
        assertTrue(
            QuestFightRules.applyCombat(
                db, "Spice Ghost (Volcanic Cave)", won = true,
            ).advanced,
        )
        assertEquals("step29", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun applyCombat_volcanoMapByItemIdAdvancesStep25() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step24")
        assertTrue(
            QuestFightRules.applyCombat(
                db, "", won = true, itemIdsGained = listOf(QuestFightRules.VOLCANO_MAP_ID),
            ).advanced,
        )
        assertEquals("step25", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun applyFightStarted_cakeLordAdvancesArmorerStep2() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.ARMORER, QuestDatabase.STARTED)
        assertTrue(QuestFightRules.applyFightStarted(db, "Cake Lord"))
        assertEquals("step2", db.getProgress(Quest.ARMORER))
    }

    @Test
    fun applyCombat_cakeLordWinAdvancesArmorerStep3() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.ARMORER, "step2")
        assertTrue(QuestFightRules.applyCombat(db, "Cake Lord", won = true).advanced)
        assertEquals("step3", db.getProgress(Quest.ARMORER))
    }

    @Test
    fun applyCombat_biclopsWinAdvancesCitadelStep5() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.CITADEL, "step4")
        assertTrue(QuestFightRules.applyCombat(db, "biclops", won = true).advanced)
        assertEquals("step5", db.getProgress(Quest.CITADEL))
    }

    @Test
    fun applyCombat_burnoutsCounterReachesStep4() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(QuestFightRules.BURNOUTS_DEFEATED_PREF, 29)
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.CITADEL, "step3")
        assertTrue(
            QuestFightRules.applyCombat(
                db, "pair of burnouts", won = true, preferences = prefs,
            ).advanced,
        )
        assertEquals("step4", db.getProgress(Quest.CITADEL))
        assertEquals(30, prefs.getInt(QuestFightRules.BURNOUTS_DEFEATED_PREF, 0))
    }

    @Test
    fun applyCombat_telegramBossResetsQuest() {
        val prefs = Preferences(MapSettings())
        prefs.setString("lttQuestName", "Missing: Fancy Man")
        prefs.setInt("lttQuestDifficulty", 1)
        prefs.setInt("lttQuestStageCount", 2)
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.TELEGRAM, "step4")
        assertTrue(
            QuestFightRules.applyCombat(db, "Jeff the Fancy Skeleton", won = true, preferences = prefs).advanced,
        )
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.TELEGRAM))
        assertEquals("", prefs.getString("lttQuestName", "x"))
        assertEquals(0, prefs.getInt("lttQuestDifficulty", -1))
        assertEquals(0, prefs.getInt("lttQuestStageCount", -1))
    }

    @Test
    fun applyCombat_telegramStageIncrementsCounter() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.TELEGRAM, "step2")
        assertTrue(QuestFightRules.applyCombat(db, "buzzard", won = true, preferences = prefs).advanced)
        assertEquals(1, prefs.getInt("lttQuestStageCount", 0))
    }

    @Test
    fun applyCombat_partyFairPartiersDecrementsProgress() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_questPartyFairQuest", "partiers")
        prefs.setString("_questPartyFairProgress", "2")
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.PARTY_FAIR, "step1")
        assertTrue(
            QuestFightRules.applyCombat(db, "biker", won = true, preferences = prefs).advanced,
        )
        assertEquals("1", prefs.getString("_questPartyFairProgress", ""))
    }

    @Test
    fun applyCombat_partyFairPartiersChainsawDoublesKill() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_questPartyFairQuest", "partiers")
        prefs.setString("_questPartyFairProgress", "3")
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.PARTY_FAIR, "step1")
        assertTrue(
            QuestFightRules.applyCombat(
                db, "jock", won = true, preferences = prefs,
                hasItemEquipped = { it == QuestFightRules.INTIMIDATING_CHAINSAW_ID },
            ).advanced,
        )
        assertEquals("1", prefs.getString("_questPartyFairProgress", ""))
    }

    @Test
    fun applyCombat_partyFairDjMeatAdvancesStep2() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_questPartyFairQuest", "dj")
        prefs.setString("_questPartyFairProgress", "500")
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.PARTY_FAIR, "step1")
        assertTrue(
            QuestFightRules.applyCombat(
                db, "party girl", won = true, preferences = prefs,
                responseText = "You collect 500 Meat for the DJ.",
            ).advanced,
        )
        assertEquals("step2", db.getProgress(Quest.PARTY_FAIR))
        assertEquals("0", prefs.getString("_questPartyFairProgress", ""))
    }

    @Test
    fun applyCombat_partyFairTrashAdvancesStep2() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_questPartyFairQuest", "trash")
        prefs.setString("_questPartyFairProgress", "10")
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.PARTY_FAIR, "step1")
        assertTrue(
            QuestFightRules.applyCombat(
                db, "burnout", won = true, preferences = prefs,
                responseText = "You clean up 10 for the environment.",
            ).advanced,
        )
        assertEquals("step2", db.getProgress(Quest.PARTY_FAIR))
    }

    @Test
    fun applyCombat_partyFairWootsRequestsQuestLogResync() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_questPartyFairQuest", "woots")
        prefs.setString("_questPartyFairProgress", "42")
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.PARTY_FAIR, "step1")
        val result = QuestFightRules.applyCombat(db, "biker", won = true, preferences = prefs)
        assertFalse(result.advanced)
        assertTrue(result.resyncQuestLogPage1)
        assertEquals("42", prefs.getString("_questPartyFairProgress", ""))
    }

    @Test
    fun applyCombat_ghostBossWin_resetsGhostQuest() {
        val prefs = Preferences(MapSettings())
        prefs.setString("ghostLocation", "The Spooky Forest")
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.GHOST, QuestDatabase.STARTED)
        assertTrue(
            QuestFightRules.applyCombat(
                db, "The Headless Horseman", won = true, preferences = prefs,
            ).advanced,
        )
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.GHOST))
        assertEquals("", prefs.getString("ghostLocation", "x"))
    }

    @Test
    fun applyCombat_nonGhostBoss_noReset() {
        val prefs = Preferences(MapSettings())
        prefs.setString("ghostLocation", "The Spooky Forest")
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.GHOST, QuestDatabase.STARTED)
        assertFalse(
            QuestFightRules.applyCombat(
                db, "spooky vampire", won = true, preferences = prefs,
            ).advanced,
        )
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.GHOST))
        assertEquals("The Spooky Forest", prefs.getString("ghostLocation", ""))
    }

    @Test
    fun applyCombat_ghostBossLoss_noReset() {
        val prefs = Preferences(MapSettings())
        prefs.setString("ghostLocation", "The Spooky Forest")
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.GHOST, QuestDatabase.STARTED)
        assertFalse(
            QuestFightRules.applyCombat(
                db, "The Headless Horseman", won = false, preferences = prefs,
            ).advanced,
        )
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.GHOST))
        assertEquals("The Spooky Forest", prefs.getString("ghostLocation", ""))
    }
}
