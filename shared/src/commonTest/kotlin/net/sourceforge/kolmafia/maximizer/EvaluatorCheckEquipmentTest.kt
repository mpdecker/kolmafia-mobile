package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.OutfitDatabase
import net.sourceforge.kolmafia.data.OutfitData
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EvaluatorCheckEquipmentTest {

    @AfterTest
    fun cleanup() {
        OutfitDatabase.resetForTest()
    }

    @Test
    fun checkEquipment_failsWhenRequiredEquipMissing() {
        val evaluator = Evaluator("mus")
        evaluator.addPosEquip("required hat")
        val equipment = mapOf(EquipmentSlot.HAT to "other hat")
        evaluator.checkEquipment(equipment, beeosity = 0, maxBeeosity = 2)
        assertTrue(evaluator.failed)
    }

    @Test
    fun checkEquipment_passesWhenRequiredEquipPresent() {
        val evaluator = Evaluator("mus")
        evaluator.addPosEquip("required hat")
        val equipment = mapOf(EquipmentSlot.HAT to "required hat")
        evaluator.checkEquipment(equipment, beeosity = 0, maxBeeosity = 2)
        assertFalse(evaluator.failed)
    }

    @Test
    fun checkEquipment_failsWhenPosOutfitNotWorn() {
        OutfitDatabase.registerStatic(
            OutfitData(
                id = 9001,
                name = "Test Outfit",
                image = "test.gif",
                equipment = listOf("piece a", "piece b"),
                halloweenDrops = emptyList(),
            ),
        )
        val evaluator = Evaluator("mus")
        evaluator.addPosOutfit("Test Outfit")
        val equipment = mapOf(EquipmentSlot.HAT to "piece a")
        evaluator.checkEquipment(equipment, beeosity = 0, maxBeeosity = 2)
        assertTrue(evaluator.failed)
    }

    @Test
    fun checkEquipment_failsWhenBeeosityExceededInBeecore() {
        val evaluator = Evaluator("mus")
        evaluator.checkEquipment(emptyMap(), beeosity = 5, maxBeeosity = 2, inBeecore = true)
        assertTrue(evaluator.failed)
    }

    @Test
    fun checkEquipment_allowsHighBeeosityOutsideBeecore() {
        val evaluator = Evaluator("mus")
        evaluator.checkEquipment(emptyMap(), beeosity = 5, maxBeeosity = 2, inBeecore = false)
        assertFalse(evaluator.failed)
    }

    @Test
    fun considerCurrent_defaultsFalseWithoutTieDisable() {
        val evaluator = Evaluator("mus")
        assertFalse(evaluator.considerCurrent())
    }

    @Test
    fun considerCurrent_enabledWithMinusTie() {
        val evaluator = Evaluator("mus, -tie")
        assertTrue(evaluator.considerCurrent())
    }

    @Test
    fun considerCurrent_explicitMinusCurrentOverridesMinusTieDefault() {
        val evaluator = Evaluator("mus, -tie, -current")
        assertFalse(evaluator.considerCurrent())
    }
}
