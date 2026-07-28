package net.sourceforge.kolmafia.data

import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.modifiers.ModifierParser

class PulverizeImplicationsTest {

    @Test
    fun coldResistance_impliesHotAndSpooky() {
        val mods = ModifierParser.parse("Cold Resistance: +5")
        val base = PulverizeFlags.PULVERIZE_BITS or PulverizeFlags.ELEM_TWINKLY
        val result = PulverizeImplications.apply(mods, base)
        assertEquals(
            base or PulverizeFlags.ELEM_HOT or PulverizeFlags.ELEM_SPOOKY,
            result,
        )
    }

    @Test
    fun hotDamage_impliesHotElement() {
        val mods = ModifierParser.parse("Hot Damage: +10")
        val base = PulverizeFlags.PULVERIZE_BITS or PulverizeFlags.ELEM_TWINKLY
        val result = PulverizeImplications.apply(mods, base)
        assertEquals(base or PulverizeFlags.ELEM_HOT, result)
    }

    @Test
    fun zeroModifiers_leaveBaseUnchanged() {
        val base = PulverizeFlags.PULVERIZE_BITS or PulverizeFlags.ELEM_TWINKLY
        val mods = ModifierParser.parse("Muscle: +5")
        assertEquals(base, PulverizeImplications.apply(mods, base))
    }

    @Test
    fun multipleDamageTypes_combineElementFlags() {
        val mods = ModifierParser.parse(
            "Hot Damage: +5, Cold Damage: +5, Stench Damage: +5",
        )
        val base = PulverizeFlags.PULVERIZE_BITS or PulverizeFlags.ELEM_TWINKLY
        val result = PulverizeImplications.apply(mods, base)
        assertEquals(
            base or PulverizeFlags.ELEM_HOT or PulverizeFlags.ELEM_COLD or PulverizeFlags.ELEM_STENCH,
            result,
        )
    }
}
