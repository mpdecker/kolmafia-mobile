package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ModifierValues

/** Desktop EquipmentDatabase.IMPLICATIONS — modifier doubles to pulverize element flags. */
object PulverizeImplications {

    private val IMPLICATIONS: Map<DoubleModifier, Int> = mapOf(
        DoubleModifier.COLD_RESISTANCE to (PulverizeFlags.ELEM_HOT or PulverizeFlags.ELEM_SPOOKY),
        DoubleModifier.HOT_RESISTANCE to (PulverizeFlags.ELEM_STENCH or PulverizeFlags.ELEM_SLEAZE),
        DoubleModifier.SLEAZE_RESISTANCE to (PulverizeFlags.ELEM_COLD or PulverizeFlags.ELEM_SPOOKY),
        DoubleModifier.SPOOKY_RESISTANCE to (PulverizeFlags.ELEM_HOT or PulverizeFlags.ELEM_STENCH),
        DoubleModifier.STENCH_RESISTANCE to (PulverizeFlags.ELEM_COLD or PulverizeFlags.ELEM_SLEAZE),
        DoubleModifier.COLD_DAMAGE to PulverizeFlags.ELEM_COLD,
        DoubleModifier.HOT_DAMAGE to PulverizeFlags.ELEM_HOT,
        DoubleModifier.SLEAZE_DAMAGE to PulverizeFlags.ELEM_SLEAZE,
        DoubleModifier.SPOOKY_DAMAGE to PulverizeFlags.ELEM_SPOOKY,
        DoubleModifier.STENCH_DAMAGE to PulverizeFlags.ELEM_STENCH,
        DoubleModifier.COLD_SPELL_DAMAGE to PulverizeFlags.ELEM_COLD,
        DoubleModifier.HOT_SPELL_DAMAGE to PulverizeFlags.ELEM_HOT,
        DoubleModifier.SLEAZE_SPELL_DAMAGE to PulverizeFlags.ELEM_SLEAZE,
        DoubleModifier.SPOOKY_SPELL_DAMAGE to PulverizeFlags.ELEM_SPOOKY,
        DoubleModifier.STENCH_SPELL_DAMAGE to PulverizeFlags.ELEM_STENCH,
    )

    fun apply(mods: ModifierValues, base: Int): Int {
        var pulver = base
        for ((modifier, flags) in IMPLICATIONS) {
            if (mods.get(modifier) > 0.0) {
                pulver = pulver or flags
            }
        }
        return pulver
    }
}
