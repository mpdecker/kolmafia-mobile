package net.sourceforge.kolmafia.character

import kotlin.test.Test
import kotlin.test.assertEquals

class ConsumptionEligibilityCapacityTest {

    @Test
    fun stomachCapacity_smallPath_usesPathBase() {
        val state = CharacterState(challengePath = "Small")
        assertEquals(2, ConsumptionEligibility.stomachCapacity(state))
    }

    @Test
    fun liverCapacity_smallPath_usesPathBase() {
        val state = CharacterState(challengePath = "Small")
        assertEquals(1, ConsumptionEligibility.liverCapacity(state))
    }

    @Test
    fun liverCapacity_youRobot_zeroWhenIneligible() {
        val state = CharacterState(challengePath = "You, Robot")
        assertEquals(0, ConsumptionEligibility.liverCapacity(state))
    }

    @Test
    fun spleenCapacity_usesPathDefault() {
        val state = CharacterState(challengePath = "Standard")
        assertEquals(15, ConsumptionEligibility.spleenCapacity(state))
    }
}
