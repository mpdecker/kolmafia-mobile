package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop garbage shirt / broken champagne automatic bucket pins (Phase 382). */
object MaximizerGarbageAuto {
    const val GARBAGE_SHIRT_ID = 9699
    const val BROKEN_CHAMPAGNE_ID = 9692

    fun shouldPinGarbageShirt(evaluator: Evaluator, preferences: Preferences?): Boolean {
        if (preferences == null) return false
        if (preferences.getInt("garbageShirtCharge", 0) <= 0) return false
        return evaluator.weightOf(DoubleModifier.EXPERIENCE) > 0.0 ||
            evaluator.weightOf(DoubleModifier.MUS_EXPERIENCE) > 0.0 ||
            evaluator.weightOf(DoubleModifier.MYS_EXPERIENCE) > 0.0 ||
            evaluator.weightOf(DoubleModifier.MOX_EXPERIENCE) > 0.0
    }

    fun shouldPinBrokenChampagne(evaluator: Evaluator, preferences: Preferences?): Boolean {
        if (preferences == null) return false
        if (evaluator.weightOf(DoubleModifier.ITEMDROP) <= 0.0) return false
        val charge = preferences.getInt("garbageChampagneCharge", 0)
        val unchanged = !preferences.getBoolean("_garbageItemChanged", false)
        return charge > 0 || unchanged
    }

    fun pinIfGarbage(
        ranked: MaximizerRankedItem,
        itemId: Int,
        evaluator: Evaluator,
        preferences: Preferences?,
    ): MaximizerRankedItem {
        val pin = (itemId == GARBAGE_SHIRT_ID && shouldPinGarbageShirt(evaluator, preferences)) ||
            (itemId == BROKEN_CHAMPAGNE_ID && shouldPinBrokenChampagne(evaluator, preferences))
        if (!pin) return ranked
        ranked.automatic = true
        ranked.required = true
        return ranked
    }
}
