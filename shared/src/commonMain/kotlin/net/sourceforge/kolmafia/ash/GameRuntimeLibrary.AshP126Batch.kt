package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.item.FreeCraftingTurns

/**
 * ASH-P126 behavioral batch — free crafting turn probes.
 */
internal fun GameRuntimeLibrary.registerAshP126Batch(scope: AshScope) {
    regFn(scope, "free_crafts", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(FreeCraftingTurns.freeCraftingTurns(buildFreeCraftingContext()).toLong())
    }

    regFn(scope, "free_cooks", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(FreeCraftingTurns.freeCookingTurns(buildFreeCraftingContext()).toLong())
    }

    regFn(scope, "free_mixes", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(FreeCraftingTurns.freeCocktailcraftingTurns(buildFreeCraftingContext()).toLong())
    }

    regFn(scope, "free_smiths", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(FreeCraftingTurns.freeSmithingTurns(buildFreeCraftingContext()).toLong())
    }
}
