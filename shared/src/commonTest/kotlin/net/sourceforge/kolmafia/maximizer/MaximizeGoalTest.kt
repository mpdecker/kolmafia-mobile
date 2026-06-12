package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.modifiers.BooleanModifier
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MaximizeGoalTest {

    @Test
    fun parseSpec_singleModifier() {
        val spec = MaximizeGoal.parseSpec("mysticality")
        assertNotNull(spec)
        assertEquals(DoubleModifier.MYS, spec.primary)
    }

    @Test
    fun parseSpec_commaGoalWithBooleanAndSwitch() {
        val spec = MaximizeGoal.parseSpec("mysticality, +Volleyball, switch Miniature Donkey")
        assertNotNull(spec)
        assertEquals(DoubleModifier.MYS, spec.primary)
        assertTrue(spec.requiredBooleans.contains(BooleanModifier.VOLLEYBALL_OR_SOMBRERO))
        assertEquals(listOf("Miniature Donkey"), spec.switchFamiliars)
    }

    @Test
    fun parseSpec_equipConstraint() {
        val spec = MaximizeGoal.parseSpec("muscle, equip \"myst hat\"")
        assertNotNull(spec)
        assertEquals(listOf("myst hat"), spec.equipRequired)
    }

    @Test
    fun parseSpec_meleeHandsEnthroneBjornify() {
        val spec = MaximizeGoal.parseSpec("muscle, +melee, +hands, enthrone Mosquito, bjornify none")
        assertNotNull(spec)
        assertTrue(spec.requireMelee)
        assertTrue(spec.requireHands)
        assertEquals(listOf("Mosquito"), spec.enthronedFamiliars)
        assertEquals(listOf("none"), spec.bjornifiedFamiliars)
    }

    @Test fun parseSpec_switchThrall() {
        val spec = MaximizeGoal.parseSpec("item, switch thrall Spice Ghost")
        assertNotNull(spec)
        assertEquals(DoubleModifier.ITEMDROP, spec.primary)
        assertEquals(listOf("Spice Ghost"), spec.switchThralls)
    }
}
