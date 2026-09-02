package net.sourceforge.kolmafia.modifiers

/**
 * Desktop KoLCharacter post-equipment outfit bitmap bonuses (Brimstone / Cloathing / McHugeLarge).
 * Applied when multiple set pieces are worn but the full named outfit bonus is not active.
 */
object OutfitBitmapBonuses {

    fun apply(total: ModifierValues, inNoobcore: Boolean): ModifierValues {
        var result = total
        result = applyBrimstone(result)
        result = applyCloathing(result)
        result = applyMcHugeLarge(result, inNoobcore)
        return result
    }

    fun mcHugeLargeLevel(total: ModifierValues, inNoobcore: Boolean): Int {
        if (inNoobcore) return 0
        val totalItems = total.bitmapCount(BitmapModifier.MCHUGELARGE)
        val itemLevel = when (totalItems) {
            0, 1 -> 0
            2, 3 -> 1
            4 -> 2
            5 -> 3
            else -> 0
        }
        return itemLevel * totalItems
    }

    private fun applyBrimstone(total: ModifierValues): ModifierValues {
        val brimstoneLevel = 1 shl total.bitmapCount(BitmapModifier.BRIMSTONE)
        if (brimstoneLevel <= 1) return total
        return total + ModifierValues(
            doubles = mapOf(
                DoubleModifier.MONSTER_LEVEL to brimstoneLevel.toDouble(),
                DoubleModifier.MEATDROP to brimstoneLevel.toDouble(),
                DoubleModifier.ITEMDROP to brimstoneLevel.toDouble(),
            ),
        )
    }

    private fun applyCloathing(total: ModifierValues): ModifierValues {
        val cloathingLevel = 1 shl total.bitmapCount(BitmapModifier.CLOATHING)
        if (cloathingLevel <= 1) return total
        return total + ModifierValues(
            doubles = mapOf(
                DoubleModifier.MOX_PCT to cloathingLevel.toDouble(),
                DoubleModifier.MUS_PCT to cloathingLevel.toDouble(),
                DoubleModifier.MYS_PCT to cloathingLevel.toDouble(),
                DoubleModifier.MEATDROP to cloathingLevel.toDouble(),
                DoubleModifier.ITEMDROP to (cloathingLevel / 2).toDouble(),
            ),
        )
    }

    private fun applyMcHugeLarge(total: ModifierValues, inNoobcore: Boolean): ModifierValues {
        val level = mcHugeLargeLevel(total, inNoobcore)
        if (level <= 0) return total
        return total + ModifierValues(
            doubles = mapOf(
                DoubleModifier.COLD_RESISTANCE to level.toDouble(),
                DoubleModifier.HOT_DAMAGE to (5 * level).toDouble(),
                DoubleModifier.INITIATIVE to (10 * level).toDouble(),
            ),
        )
    }
}
