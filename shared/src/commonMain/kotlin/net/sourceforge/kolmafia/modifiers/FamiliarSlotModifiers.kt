package net.sourceforge.kolmafia.modifiers

/**
 * Item modifiers adjusted when equipped in a Disembodied Hand / Left-Hand familiar slot.
 * Mirrors desktop [ModifierDatabase.getItemModifiersInFamiliarSlot].
 */
object FamiliarSlotModifiers {

    fun forHandSlot(values: ModifierValues): ModifierValues {
        if (values.isEmpty) return values
        val doubles = if (values.doubles.containsKey(DoubleModifier.SLIME_HATES_IT)) {
            values.doubles - DoubleModifier.SLIME_HATES_IT
        } else {
            values.doubles
        }
        val booleans = values.booleans.toMutableMap().apply {
            remove(BooleanModifier.MOXIE_MAY_CONTROL_MP)
            remove(BooleanModifier.MOXIE_CONTROLS_MP)
        }
        val bitmaps = values.bitmaps.toMutableMap().apply {
            remove(BitmapModifier.BRIMSTONE)
            remove(BitmapModifier.CLOATHING)
            remove(BitmapModifier.SYNERGETIC)
        }
        return ModifierValues(doubles, booleans, values.strings, bitmaps)
    }
}
