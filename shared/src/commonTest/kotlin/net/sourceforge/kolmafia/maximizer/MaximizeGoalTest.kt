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

    @Test fun parseSpec_priceAndCreatableConstraints() {
        val spec = MaximizeGoal.parseSpec("muscle, -price 1000, +price 50, creatable, -nocreat")
        assertNotNull(spec)
        assertEquals(1000, spec.maxPrice)
        assertEquals(50, spec.minPrice)
        assertTrue(spec.allowCreatable)
        assertTrue(spec.forbidCreatable)
    }

    @Test fun parseSpec_beeosityKeyword() {
        val defaultSpec = MaximizeGoal.parseSpec("muscle")
        assertNotNull(defaultSpec)
        assertEquals(2, defaultSpec.maxBeeosity)

        val limitOne = MaximizeGoal.parseSpec("muscle, beeosity")
        assertNotNull(limitOne)
        assertEquals(1, limitOne.maxBeeosity)

        val limitFive = MaximizeGoal.parseSpec("muscle, beeosity 5")
        assertNotNull(limitFive)
        assertEquals(5, limitFive.maxBeeosity)
    }

    @Test fun parseSpec_equipRaisesBeeosityFloor() {
        val spec = MaximizeGoal.parseSpec("muscle, beeosity, equip \"babbling book\"")
        assertNotNull(spec)
        assertEquals(4, spec.maxBeeosity)
    }

    @Test fun parseSpec_multiWeightGoalWithHandsConstraint() {
        val spec = MaximizeGoal.parseSpec("2 item, 1 meat, +hands")
        assertNotNull(spec)
        assertEquals(2.0, spec.evaluator.weightOf(DoubleModifier.ITEMDROP))
        assertEquals(1.0, spec.evaluator.weightOf(DoubleModifier.MEATDROP))
        assertEquals(DoubleModifier.ITEMDROP, spec.primary)
        assertTrue(spec.requireHands)
    }
}
