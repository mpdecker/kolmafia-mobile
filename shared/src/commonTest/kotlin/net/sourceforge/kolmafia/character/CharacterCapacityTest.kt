package net.sourceforge.kolmafia.character

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.skill.SkillData

class CharacterCapacityTest {

    @Test
    fun canExpandStomach_falseInRobocore() {
        val state = CharacterState(challengePath = "You, Robot")
        assertFalse(CharacterCapacity.canExpandStomachCapacity(state))
        assertFalse(ConsumptionEligibility.canExpandStomach(state))
    }

    @Test
    fun canExpandStomach_falseInSmallcore() {
        val state = CharacterState(challengePath = "Small")
        assertFalse(CharacterCapacity.canExpandStomachCapacity(state))
    }

    @Test
    fun canExpandLiver_falseInVampyre() {
        val state = CharacterState(challengePath = "Vampyre")
        assertFalse(CharacterCapacity.canExpandLiverCapacity(state))
        assertFalse(ConsumptionEligibility.canExpandLiver(state))
    }

    @Test
    fun canExpandStomach_trueForStandardPath() {
        val state = CharacterState(challengePath = "Standard")
        assertTrue(CharacterCapacity.canExpandStomachCapacity(state))
        assertTrue(ConsumptionEligibility.canExpandStomach(state))
    }

    @Test
    fun canExpandLiver_trueForStandardPath() {
        val state = CharacterState(challengePath = "Standard")
        assertTrue(CharacterCapacity.canExpandLiverCapacity(state))
        assertTrue(ConsumptionEligibility.canExpandLiver(state))
    }
}
