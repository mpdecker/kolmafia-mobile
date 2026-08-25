package net.sourceforge.kolmafia.inventory

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.EquipmentSlot

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

    @Test
    fun limitZone_noneBlocksSpelunkyArea() {
        assertTrue(LimitModeGates.limitZone("Spelunky Area", ""))
        assertTrue(LimitModeGates.limitZone("Batfellow Area", "none"))
        assertFalse(LimitModeGates.limitZone("The Seaside Town", ""))
    }

    @Test
    fun limitZone_spelunkyAllowsOnlySpelunkyArea() {
        assertFalse(LimitModeGates.limitZone("Spelunky Area", "spelunky"))
        assertTrue(LimitModeGates.limitZone("Batfellow Area", "spelunky"))
        assertTrue(LimitModeGates.limitZone("The Seaside Town", "spelunky"))
    }

    @Test
    fun limitZone_batmanAllowsOnlyBatfellowArea() {
        assertFalse(LimitModeGates.limitZone("Batfellow Area", "batman"))
        assertTrue(LimitModeGates.limitZone("Spelunky Area", "batman"))
    }

    @Test
    fun limitSkill_spelunkyAllowsThrowSkills() {
        assertFalse(LimitModeGates.limitSkill("spelunky", 7238))
        assertFalse(LimitModeGates.limitSkill("spelunky", 7244))
        assertTrue(LimitModeGates.limitSkill("spelunky", 1000))
        assertTrue(LimitModeGates.limitSkill("batman", 7255))
    }

    @Test
    fun limitSlot_spelunkyAllowsCoreSlots() {
        assertFalse(LimitModeGates.limitSlot("spelunky", EquipmentSlot.HAT))
        assertFalse(LimitModeGates.limitSlot("spelunky", EquipmentSlot.WEAPON))
        assertTrue(LimitModeGates.limitSlot("spelunky", EquipmentSlot.PANTS))
        assertTrue(LimitModeGates.limitSlot("batman", EquipmentSlot.HAT))
    }

    @Test
    fun limitMeatPickpocketMcd() {
        assertTrue(LimitModeGates.limitMeat("spelunky"))
        assertTrue(LimitModeGates.limitPickpocket("batman"))
        assertTrue(LimitModeGates.limitMCD("spelunky"))
        assertFalse(LimitModeGates.limitMeat(""))
    }
}
