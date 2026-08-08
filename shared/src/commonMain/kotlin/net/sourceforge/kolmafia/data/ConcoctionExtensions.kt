package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.inventory.CraftMode

private val STILL_ROW_PATTERN = Regex("""ROW(\d+)""")

fun ConcoctionData.craftMode(): CraftMode? = when {
    methods.any { it == "COMBINE" || it == "ACOMBINE" } -> CraftMode.COMBINE
    methods.any { it.contains("COOK") || it.contains("PASTA") || it.contains("SAUCE") } -> CraftMode.COOK
    methods.any { it.contains("MIX") || it.contains("COCK") || it == "STILL" } -> CraftMode.COCKTAIL
    methods.any { it.contains("SMITH") } -> CraftMode.SMITH
    else -> null
}

fun ConcoctionData.stillShopRow(): Int? =
    methods.firstNotNullOfOrNull { token ->
        STILL_ROW_PATTERN.find(token)?.groupValues?.get(1)?.toIntOrNull()
    }

fun ConcoctionData.isStillCraftable(): Boolean =
    ConcoctionCreationCost.primaryMethod(methods) == "STILL" && stillShopRow() != null

fun ConcoctionData.isCoinmasterCraftable(): Boolean =
    ConcoctionCreationCost.primaryMethod(methods) == "COINMASTER"

fun ConcoctionData.clipArtParams(): Triple<Int, Int, Int>? {
    if (ConcoctionCreationCost.primaryMethod(methods) != "CLIPART" || param == 0) return null
    return Triple((param shr 16) and 0xFF, (param shr 8) and 0xFF, param and 0xFF)
}

fun ConcoctionData.isClipArtCraftable(): Boolean =
    ConcoctionCreationCost.primaryMethod(methods) == "CLIPART" && clipArtParams() != null

fun ConcoctionData.isRollCraftable(): Boolean =
    ConcoctionCreationCost.primaryMethod(methods) == "ROLL" && ingredients.size == 1

fun ConcoctionData.terminalExtrudeCommand(): String? = when (result) {
    "hacked gibson" -> "extrude -f booze.ext"
    "browser cookie" -> "extrude -f food.ext"
    "software bug" -> "extrude -f familiar.ext"
    "Source shades" -> "extrude -f goggles.ext"
    "Source terminal CRAM chip" -> "extrude -f cram.ext"
    "Source terminal DRAM chip" -> "extrude -f dram.ext"
    "Source terminal GRAM chip" -> "extrude -f gram.ext"
    "Source terminal PRAM chip" -> "extrude -f pram.ext"
    "Source terminal SPAM chip" -> "extrude -f spam.ext"
    "Source terminal TRAM chip" -> "extrude -f tram.ext"
    else -> null
}

fun ConcoctionData.isTerminalCraftable(): Boolean =
    ConcoctionCreationCost.primaryMethod(methods) == "TERMINAL" &&
        terminalExtrudeCommand() != null

fun ConcoctionData.isSewerCraftable(): Boolean =
    ConcoctionCreationCost.primaryMethod(methods) == "SEWER" &&
        ingredients.size == 1 &&
        ingredients[0].name.equals("chewing gum on a string", ignoreCase = true)

fun ConcoctionData.isVykeaCraftable(): Boolean =
    ConcoctionCreationCost.primaryMethod(methods) == "VYKEA" &&
        ingredients.size >= 3 &&
        ingredients[0].name.equals("VYKEA instructions", ignoreCase = true)

fun ConcoctionData.isMuseCraftable(): Boolean =
    ConcoctionCreationCost.primaryMethod(methods) == "MUSE" &&
        ingredients.isNotEmpty()

fun ConcoctionData.isPhineasCraftable(): Boolean =
    ConcoctionCreationCost.primaryMethod(methods) == "PHINEAS" &&
        ingredients.isNotEmpty()

fun ConcoctionData.isStaffCraftable(): Boolean =
    ConcoctionCreationCost.primaryMethod(methods) == "STAFF" &&
        ingredients.isNotEmpty()

fun ConcoctionData.isTinkerCraftable(): Boolean =
    ConcoctionCreationCost.primaryMethod(methods) == "GNOME_TINKER" &&
        ingredients.size == 3

fun ConcoctionData.isSushiCraftable(): Boolean =
    ConcoctionCreationCost.primaryMethod(methods) == "SUSHI"

fun ConcoctionData.isMalusCraftable(): Boolean =
    ConcoctionCreationCost.primaryMethod(methods) == "MALUS" &&
        ingredients.size == 1

fun ConcoctionData.isJewelCraftable(): Boolean {
    val primary = ConcoctionCreationCost.primaryMethod(methods) ?: return false
    return primary in setOf("JEWEL", "JEWELRY") && ingredients.size == 2
}

fun ConcoctionData.isBarrelCraftable(): Boolean =
    ConcoctionCreationCost.primaryMethod(methods) == "BARREL" &&
        ingredients.isEmpty()

fun ConcoctionData.isCreateAndConsume(): Boolean = isSushiCraftable()

fun ConcoctionData.isCreateSupported(): Boolean =
    isAutoCraftable() || isStillCraftable() || isCoinmasterCraftable() ||
        isClipArtCraftable() || isRollCraftable() || isTerminalCraftable() ||
        isSewerCraftable() || isVykeaCraftable() || isMuseCraftable() ||
        isPhineasCraftable() || isStaffCraftable() || isTinkerCraftable() ||
        isSushiCraftable() || isMalusCraftable() || isJewelCraftable() || isBarrelCraftable()

fun ConcoctionData.isAutoCraftable(): Boolean =
    isSuseCraftable() || isStationCraftable()

fun ConcoctionData.isSuseCraftable(): Boolean =
    methods.contains("SUSE") && !methods.contains("MANUAL") && ingredients.isNotEmpty()

fun ConcoctionData.isStationCraftable(): Boolean =
    !methods.contains("MANUAL") && craftMode() != null && ingredients.size >= 2

fun ConcoctionData.craftTypeDescription(): String = CraftTypeDescription.describe(methods)

/** Desktop ConsumablesDatabase.areAdventuresBoosted — sushi/stillsuit skip ode/lunch/etc. */
fun ConcoctionData.areAdventuresBoosted(): Boolean =
    !methods.contains("SUSHI") && !methods.contains("STILLSUIT")
