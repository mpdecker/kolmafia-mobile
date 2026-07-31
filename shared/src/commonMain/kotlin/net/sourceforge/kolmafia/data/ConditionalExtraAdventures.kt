package net.sourceforge.kolmafia.data

import kotlin.math.ceil
import kotlin.math.floor

/** Desktop Math.rint — round to nearest double, ties to even. */
private fun rint(value: Double): Double {
    val f = floor(value)
    val c = ceil(value)
    val d = value - f
    return when {
        d > 0.5 -> c
        d < 0.5 -> f
        f.toLong() % 2L == 0L -> f
        else -> c
    }
}

/** Desktop ConsumablesDatabase.conditionalExtraAdventures — v2 full tag/skill/bondcore paths. */
fun conditionalExtraAdventures(
    consumable: ConsumableData,
    perUnit: Boolean,
    context: ConditionalExtraAdventureContext,
): Double {
    val start = consumable.advMin
    val end = consumable.advMax
    val fullness = consumable.amount
    val inebriety = if (consumable.amount == 0) 1 else consumable.amount

    if (context.inBondcore && context.itemImage(consumable.name) == "martini.gif") {
        var bonus = 0.0
        if (consumable.isMartini() && context.tuxedoAccessible()) {
            bonus += 2.0
        }
        if (context.bondMartiniTurn) {
            bonus += 1.0
        }
        if (context.bondMartiniPlus) {
            val rangeSize = end - start + 1
            for (i in start..end) {
                if (i < 10) {
                    bonus += 4.0 / rangeSize
                }
            }
        }
        return if (perUnit) bonus / inebriety else bonus
    }

    if (consumable.isMartini()) {
        if (!context.tuxedoAccessible()) {
            return 0.0
        }
        return if (perUnit) 2.0 / inebriety else 2.0
    }

    if (consumable.isWine()) {
        val refinedPalate = context.hasEffect(ConditionalExtraAdventureEffects.REFINED_PALATE)
        var bonus = 0.0
        val rangeSize = end - start + 1
        for (i in start..end) {
            bonus += if (refinedPalate) floor(i * 0.25) / rangeSize else 0.0
            if (context.pinkyRingAccessible()) {
                val adjustedBase = if (refinedPalate) floor(i * 1.25) else i.toDouble()
                bonus += rint(adjustedBase * 0.125) / rangeSize
            }
        }
        return if (perUnit) bonus / inebriety else bonus
    }

    if (consumable.isLasagna()) {
        if (context.isMonday || !context.garishAccessible()) {
            return 0.0
        }
        return if (perUnit) 5.0 / fullness else 5.0
    }

    if (consumable.isPizza() && context.hasSkill(ConditionalExtraAdventureSkills.PIZZA_LOVER)) {
        return if (perUnit) 1.0 else fullness.toDouble()
    }

    if (consumable.isBeans() && context.hasSkill(ConditionalExtraAdventureSkills.BEANWEAVER)) {
        return 2.0
    }

    if (consumable.isSaucy() && context.hasSkill(ConditionalExtraAdventureSkills.SAUCEMAVEN)) {
        return if (context.isMysticalityClass) {
            if (perUnit) 5.0 / fullness else 5.0
        } else {
            if (perUnit) 3.0 / fullness else 3.0
        }
    }

    return 0.0
}
