package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuestFightRulesTest {

    @Test
    fun applyCombat_unknownClassWinAdvancesStep2() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step1")
        assertTrue(QuestFightRules.applyCombat(db, "The Unknown Seal Clubber", won = true))
        assertEquals("step2", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun applyCombat_beelzebozoWinAdvancesStep6() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step5")
        assertTrue(QuestFightRules.applyCombat(db, "Clownlord Beelzebozo", won = true))
        assertEquals("step6", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun applyCombat_lossAdvancesToStep18() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step17")
        assertTrue(QuestFightRules.applyCombat(db, "menacing thug", won = false))
        assertEquals("step18", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun applyCombat_winAdvancesToStep19() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step18")
        assertTrue(QuestFightRules.applyCombat(db, "menacing thug", won = true))
        assertEquals("step19", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun applyCombat_volcanoMapFinishesStep25() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step24")
        assertTrue(
            QuestFightRules.applyCombat(
                db, "", won = true, itemsGained = listOf("volcano map"),
            )
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
            )
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
            )
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
        assertTrue(QuestFightRules.applyCombat(db, "Cake Lord", won = true))
        assertEquals("step3", db.getProgress(Quest.ARMORER))
    }

    @Test
    fun applyCombat_biclopsWinAdvancesCitadelStep5() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.CITADEL, "step4")
        assertTrue(QuestFightRules.applyCombat(db, "biclops", won = true))
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
            )
        )
        assertEquals("step4", db.getProgress(Quest.CITADEL))
        assertEquals(30, prefs.getInt(QuestFightRules.BURNOUTS_DEFEATED_PREF, 0))
    }
}
