package net.sourceforge.kolmafia.maximizer

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MaximizerGarbageAutoTest {

    @Test
    fun pinsGarbageShirtWhenExpWeightedAndChargePositive() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("garbageShirtCharge", 5)
        val evaluator = Evaluator("exp")
        assertTrue(MaximizerGarbageAuto.shouldPinGarbageShirt(evaluator, prefs))
    }

    @Test
    fun doesNotPinGarbageShirtWhenChargeZero() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("garbageShirtCharge", 0)
        val evaluator = Evaluator("exp")
        assertFalse(MaximizerGarbageAuto.shouldPinGarbageShirt(evaluator, prefs))
    }

    @Test
    fun pinsBrokenChampagneWhenItemWeightedAndChargePositive() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("garbageChampagneCharge", 3)
        val evaluator = Evaluator("item")
        assertTrue(MaximizerGarbageAuto.shouldPinBrokenChampagne(evaluator, prefs))
    }

    @Test
    fun pinsBrokenChampagneWhenGarbageUnchangedEvenWithoutCharge() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("garbageChampagneCharge", 0)
        prefs.setBoolean("_garbageItemChanged", false)
        val evaluator = Evaluator("item")
        assertTrue(MaximizerGarbageAuto.shouldPinBrokenChampagne(evaluator, prefs))
    }

    @Test
    fun doesNotPinBrokenChampagneWhenChangedAndNoCharge() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("garbageChampagneCharge", 0)
        prefs.setBoolean("_garbageItemChanged", true)
        val evaluator = Evaluator("item")
        assertFalse(MaximizerGarbageAuto.shouldPinBrokenChampagne(evaluator, prefs))
    }

    @Test
    fun pinIfGarbage_setsRequiredAndAutomatic() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("garbageShirtCharge", 1)
        val evaluator = Evaluator("exp")
        val checked = MaximizerCheckedItem(
            itemId = MaximizerGarbageAuto.GARBAGE_SHIRT_ID,
            name = "makeshift garbage shirt",
            initial = 1,
        )
        val ranked = MaximizerRankedItem(
            MaximizerGarbageAuto.GARBAGE_SHIRT_ID,
            "makeshift garbage shirt",
            0.0,
            checked,
        )
        MaximizerGarbageAuto.pinIfGarbage(ranked, ranked.itemId, evaluator, prefs)
        assertTrue(ranked.automatic)
        assertTrue(ranked.required)
    }
}
