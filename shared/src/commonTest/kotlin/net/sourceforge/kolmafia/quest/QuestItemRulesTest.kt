package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuestItemRulesTest {

    @Test
    fun applyItemsGained_legendaryWeaponAdvancesStep8() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step7")
        assertTrue(QuestItemRules.applyItemsGained(listOf("Hammer of Smiting"), db))
        assertEquals("step8", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun applyInventory_sixSporePodsAdvanceStep14() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step13")
        val counts = mapOf(QuestItemRules.FIZZING_SPORE_POD_ID to 6)
        assertTrue(QuestItemRules.applyInventory({ counts[it] ?: 0 }, db))
        assertEquals("step14", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun applyInventory_scalpAdvancesStep16() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step15")
        val counts = mapOf(QuestItemRules.SCALP_OF_GORGOLOK_ID to 1)
        assertTrue(QuestItemRules.applyInventory({ counts[it] ?: 0 }, db))
        assertEquals("step16", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun applyInventory_volcanoMapByIdAdvancesStep25() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step24")
        val counts = mapOf(QuestFightRules.VOLCANO_MAP_ID to 1)
        assertTrue(QuestItemRules.applyInventory({ counts[it] ?: 0 }, db))
        assertEquals("step25", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun applyItemsGained_legendaryPantsAdvanceStep27() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step26")
        assertTrue(QuestItemRules.applyItemsGained(listOf("Krakrox's loincloth"), db))
        assertEquals("step27", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun applyInventory_beltBuckleFinishesNemesis() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step28")
        val counts = mapOf(QuestItemRules.BELT_BUCKLE_OF_LOPEZ_ID to 1)
        assertTrue(QuestItemRules.applyInventory({ counts[it] ?: 0 }, db))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun applyItemsGained_noHandedPieAdvancesArmorerStep4() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.ARMORER, "step3")
        assertTrue(QuestItemRules.applyItemsGained(listOf("no-handed pie"), db))
        assertEquals("step4", db.getProgress(Quest.ARMORER))
    }

    @Test
    fun applyInventory_popularPartFinishesArmorer() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.ARMORER, "step4")
        val counts = mapOf(QuestItemRules.POPULAR_PART_ID to 1)
        assertTrue(QuestItemRules.applyInventory({ counts[it] ?: 0 }, db))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.ARMORER))
    }

    @Test
    fun applyItemsGained_bigKnobSausageAdvancesMuscleStep1() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.MUSCLE, QuestDatabase.UNSTARTED)
        assertTrue(QuestItemRules.applyItemsGained(listOf("big knob sausage"), db))
        assertEquals("step1", db.getProgress(Quest.MUSCLE))
    }

    @Test
    fun applyInventory_docHerbsAdvanceStep1() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.DOC, QuestDatabase.UNSTARTED)
        val counts = mapOf(
            QuestItemRules.FRAUDWORT_ID to 3,
            QuestItemRules.SHYSTERWEED_ID to 3,
            QuestItemRules.SWINDLEBLOSSOM_ID to 3,
        )
        assertTrue(QuestItemRules.applyInventory({ counts[it] ?: 0 }, db))
        assertEquals("step1", db.getProgress(Quest.DOC))
    }

    @Test
    fun applyInventory_mossSphereFinishesCurses() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.CURSES, QuestDatabase.STARTED)
        val counts = mapOf(QuestItemRules.MOSS_COVERED_STONE_SPHERE_ID to 1)
        assertTrue(QuestItemRules.applyInventory({ counts[it] ?: 0 }, db))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.CURSES))
    }

    @Test
    fun applyInventory_allStoneSpheresAdvanceWorshipStep4() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.CURSES, QuestDatabase.FINISHED)
        db.setProgress(Quest.DOCTOR, QuestDatabase.FINISHED)
        db.setProgress(Quest.BUSINESS, QuestDatabase.FINISHED)
        db.setProgress(Quest.SPARE, QuestDatabase.UNSTARTED)
        val counts = mapOf(QuestItemRules.SCORCHED_STONE_SPHERE_ID to 1)
        assertTrue(QuestItemRules.applyInventory({ counts[it] ?: 0 }, db))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.SPARE))
        assertEquals("step4", db.getProgress(Quest.WORSHIP))
    }
}
