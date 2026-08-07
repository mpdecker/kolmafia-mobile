package net.sourceforge.kolmafia.character

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ConcoctionQueueBudget
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillType

class ConsumptionEligibilityTest {

    private fun standardState(limitMode: String = "") =
        CharacterState(challengePath = "Standard", limitMode = limitMode)

    @Test
    fun canEat_standardPath_true() {
        assertTrue(ConsumptionEligibility.canEat(standardState()))
    }

    @Test
    fun canEat_boozetafarian_false() {
        val state = CharacterState(challengePath = "Boozetafarian")
        assertFalse(ConsumptionEligibility.canEat(state))
        assertEquals(0, ConsumptionEligibility.stomachCapacity(state))
    }

    @Test
    fun canDrink_teetotaler_false() {
        val state = CharacterState(challengePath = "Teetotaler")
        assertFalse(ConsumptionEligibility.canDrink(state))
        assertEquals(0, ConsumptionEligibility.liverCapacity(state))
    }

    @Test
    fun canEat_spelunkyLimitMode_false() {
        val state = standardState(limitMode = "spelunky")
        assertFalse(ConsumptionEligibility.canEat(state))
        assertFalse(ConsumptionEligibility.canDrink(state))
        assertFalse(ConsumptionEligibility.canChew(state))
    }

    @Test
    fun canEat_edWithoutReplacementStomach_false() {
        val state = CharacterState(challengePath = "Actually Ed the Undying")
        assertFalse(ConsumptionEligibility.canEat(state))
    }

    @Test
    fun canEat_edWithReplacementStomach_true() {
        val state = CharacterState(challengePath = "Actually Ed the Undying")
        val skills = listOf(
            SkillData(17028, "Replacement Stomach", SkillType.PASSIVE, 0, 0, 0),
        )
        assertTrue(ConsumptionEligibility.canEat(state, skills))
        assertEquals(15, ConsumptionEligibility.stomachCapacity(state, skills))
    }

    @Test
    fun stomachCapacity_whenEligible_returnsPathDefault() {
        val state = CharacterState(challengePath = "Standard", fullnessLimit = 20)
        assertEquals(15, ConsumptionEligibility.stomachCapacity(state))
    }

    @Test
    fun spleenCapacity_robocore_zero() {
        val state = CharacterState(challengePath = "You, Robot", spleenLimit = 15)
        assertFalse(ConsumptionEligibility.canChew(state))
        assertEquals(0, ConsumptionEligibility.spleenCapacity(state))
    }

    @Test
    fun effectiveFullnessRemaining_subtractsQueuedFullness() {
        ConcoctionQueueBudget.queuedFullness = 5
        val state = CharacterState(challengePath = "Standard", fullness = 2)
        assertEquals(8, ConsumptionEligibility.effectiveFullnessRemaining(state))
        ConcoctionQueueBudget.resetForTest()
    }

    @Test
    fun effectiveInebrietyRemaining_subtractsQueuedInebriety() {
        ConcoctionQueueBudget.queuedInebriety = 3
        val state = CharacterState(challengePath = "Standard", inebriety = 4)
        assertEquals(7, ConsumptionEligibility.effectiveInebrietyRemaining(state))
        ConcoctionQueueBudget.resetForTest()
    }

    @Test
    fun effectiveSpleenRemaining_subtractsQueuedSpleenHit() {
        ConcoctionQueueBudget.queuedSpleenHit = 2
        val state = CharacterState(challengePath = "Standard", spleenUsed = 1)
        assertEquals(12, ConsumptionEligibility.effectiveSpleenRemaining(state))
        ConcoctionQueueBudget.resetForTest()
    }
}
