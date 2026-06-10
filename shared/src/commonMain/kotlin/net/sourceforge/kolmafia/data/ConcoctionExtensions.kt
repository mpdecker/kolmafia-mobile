package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.inventory.CraftMode

fun ConcoctionData.craftMode(): CraftMode? = when {
    methods.any { it == "COMBINE" || it == "ACOMBINE" } -> CraftMode.COMBINE
    methods.any { it.contains("COOK") || it.contains("PASTA") || it.contains("SAUCE") } -> CraftMode.COOK
    methods.any { it.contains("MIX") || it.contains("COCK") || it == "STILL" } -> CraftMode.COCKTAIL
    methods.any { it.contains("SMITH") } -> CraftMode.SMITH
    else -> null
}

fun ConcoctionData.isAutoCraftable(): Boolean =
    !methods.contains("MANUAL") && craftMode() != null && ingredients.size >= 2
