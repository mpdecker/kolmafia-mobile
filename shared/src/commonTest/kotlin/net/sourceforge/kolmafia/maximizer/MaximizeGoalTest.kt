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
}
