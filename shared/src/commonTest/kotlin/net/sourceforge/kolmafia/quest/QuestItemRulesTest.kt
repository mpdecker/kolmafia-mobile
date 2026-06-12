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
}
