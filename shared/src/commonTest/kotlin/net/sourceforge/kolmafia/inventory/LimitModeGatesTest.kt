package net.sourceforge.kolmafia.inventory

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LimitModeGatesTest {

    @Test
    fun limitRecovery_normalPlay_false() {
        assertFalse(LimitModeGates.limitRecovery(""))
        assertFalse(LimitModeGates.limitRecovery("none"))
        assertFalse(LimitModeGates.limitRecovery("bird"))
        assertFalse(LimitModeGates.limitRecovery("astral"))
    }

    @Test
    fun limitRecovery_restrictedModes_true() {
        assertTrue(LimitModeGates.limitRecovery("spelunky"))
        assertTrue(LimitModeGates.limitRecovery("batman"))
        assertTrue(LimitModeGates.limitRecovery("ed"))
    }

    @Test
    fun limitEating_spelunky_true() {
        assertTrue(LimitModeGates.limitEating("spelunky"))
        assertTrue(LimitModeGates.limitDrinking("batman"))
        assertTrue(LimitModeGates.limitSpleening("ed"))
    }

    @Test
    fun limitEating_normalPlay_false() {
        assertFalse(LimitModeGates.limitEating(""))
        assertFalse(LimitModeGates.limitDrinking("bird"))
    }
}
