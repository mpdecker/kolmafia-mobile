package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuestChoiceRulesTest {

    @Test
    fun apply_choice1088_visitAdvancesStep13() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step12")
        assertTrue(QuestChoiceRules.apply(1088, "You enter the cave.", db))
        assertEquals("step13", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun apply_choice1049_epicWeaponYoursAdvancesStep3() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step2")
        assertTrue(QuestChoiceRules.apply(1049, "The Epic Weapon's yours!", db))
        assertEquals("step3", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun apply_choice1088_boomAdvancesStep15() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step14")
        assertTrue(QuestChoiceRules.apply(1088, "BOOOOOOM! The rubble is gone.", db))
        assertEquals("step15", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun apply_choice930_startsCitadel() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestChoiceRules.apply(930, "Welcome to the White Citadel quest.", db))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.CITADEL))
    }

    @Test
    fun apply_choice189_advancesStep26() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step25")
        assertTrue(QuestChoiceRules.apply(189, "O Cap'm, My Cap'm", db))
        assertEquals("step26", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun apply_choice931_advancesCitadelStep6() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.CITADEL, "step5")
        assertTrue(
            QuestChoiceRules.apply(
                931, "Life Ain't Nothin But Witches and Mummies", db,
            )
        )
        assertEquals("step6", db.getProgress(Quest.CITADEL))
    }

    @Test
    fun apply_choice932_noWhammiesAdvancesStep8() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.CITADEL, "step7")
        assertTrue(QuestChoiceRules.apply(932, "No Whammies!", db))
        assertEquals("step8", db.getProgress(Quest.CITADEL))
    }

    @Test
    fun apply_choice542_advancesMoxieStep1() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestChoiceRules.apply(542, "It is oddly chilly in here.", db))
        assertEquals("step1", db.getProgress(Quest.MOXIE))
    }

    @Test
    fun apply_choice1061_advancesArmorerStep1() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestChoiceRules.apply(1061, "Heart of Madness", db))
        assertEquals("step1", db.getProgress(Quest.ARMORER))
    }

    @Test
    fun choice1065_decision1StartsArmorer() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestChoiceRules.apply(1065, "Lending a Hand", db, decision = 1))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.ARMORER))
    }

    @Test
    fun choice1064_decision1StartsDoc() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestChoiceRules.apply(1064, "The Doctor is Out", db, decision = 1))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.DOC))
    }

    @Test
    fun choice125_decision3AdvancesWorshipStep3() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.WORSHIP, "step2")
        assertTrue(QuestChoiceRules.apply(125, "No visible means of support", db, decision = 3))
        assertEquals("step3", db.getProgress(Quest.WORSHIP))
    }

    @Test
    fun choice584_decision4AdvancesWorshipStep2() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.WORSHIP, QuestDatabase.STARTED)
        assertTrue(QuestChoiceRules.apply(584, "Unconfusing Buttons", db, decision = 4))
        assertEquals("step2", db.getProgress(Quest.WORSHIP))
    }
}
