package net.sourceforge.kolmafia.request

import kotlin.math.min
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.ConsumableDatabase
import net.sourceforge.kolmafia.data.DailyLimitDatabase
import net.sourceforge.kolmafia.data.DailyLimitKind
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.OutfitDatabase
import net.sourceforge.kolmafia.data.RestoreDatabase
import net.sourceforge.kolmafia.equipment.OutfitManager
import net.sourceforge.kolmafia.inventory.LimitModeGates
import net.sourceforge.kolmafia.modifiers.ExpressionContext
import net.sourceforge.kolmafia.modifiers.StringModifier
import net.sourceforge.kolmafia.preferences.Preferences

data class ItemUseLimitsContext(
    val character: CharacterState,
    val preferences: Preferences?,
    val expressionContext: ExpressionContext,
    val inMultiFight: Boolean = false,
    val choiceFollowsFight: Boolean = false,
    val inChoiceAdventure: Boolean = false,
    val canWalkAwayFromChoice: Boolean = true,
    val canUsePotions: Boolean = true,
    val accessibleCount: (Int) -> Int = { 0 },
)

/** Desktop UseItemRequest maximumUses early guards (fight/choice/limit-mode/path/item cases). */
private fun earlyMaximumUses(itemId: Int, ctx: ItemUseLimitsContext): Int? {
    if (ctx.inMultiFight) return 0
    if (ctx.choiceFollowsFight) return 0
    if (ctx.inChoiceAdventure && !ctx.canWalkAwayFromChoice) return 0
    if (LimitModeGates.limitItem(ctx.character.limitMode, itemId)) return 0

    when (itemId) {
        ItemDatabase.BALL_POLISH,
        ItemDatabase.FRATHOUSE_BLUEPRINTS,
        ItemDatabase.BINDER_CLIP,
        ItemDatabase.ICE_BABY,
        ItemDatabase.JUGGLERS_BALLS,
        ItemDatabase.EYEBALL_PENDANT,
        ItemDatabase.SPOOKY_PUTTY_BALL,
        ItemDatabase.LOATHING_LEGION_ABACUS,
        ItemDatabase.LOATHING_LEGION_DEFIBRILLATOR,
        ItemDatabase.LOATHING_LEGION_DOUBLE_PRISM,
        ItemDatabase.LOATHING_LEGION_ROLLERBLADES,
        -> return Int.MAX_VALUE
        ItemDatabase.COBBS_KNOB_MAP ->
            return ctx.accessibleCount(ItemDatabase.ENCRYPTION_KEY)
        ItemDatabase.ASTRAL_MUSHROOM,
        ItemDatabase.GONG,
        -> return 1
        ItemDatabase.PHOTOCOPIER ->
            return if (ctx.preferences?.getBoolean("_photocopyUsed", false) == true) 0 else 1
        ItemDatabase.PHOTOCOPIED_MONSTER ->
            return if (ctx.preferences?.getBoolean("_photocopyUsed", false) == true) 0 else 1
        ItemDatabase.MOJO_FILTER -> {
            val used = ctx.preferences?.getInt("currentMojoFilters", 0) ?: 0
            return (3 - used).coerceAtLeast(0)
        }
        ItemDatabase.DANCE_CARD -> {
            if ((ctx.preferences?.getInt("_danceCardFightsLeft", 0) ?: 0) > 0) return 0
            return 1
        }
        ItemDatabase.TOASTER ->
            return if (ctx.preferences?.getBoolean("_toastSummoned", false) == true) 0 else 1
    }

    if (ctx.character.inBeecore && ItemDatabase.unusableInBeecore(itemId)) return 0
    if (ctx.character.inGLover && ItemDatabase.unusableInGLover(itemId)) return 0

    if (ctx.character.inRobocore &&
        ItemDatabase.isPotion(itemId) &&
        !ctx.canUsePotions
    ) {
        return 0
    }

    if (itemId in CLASS_BOOKS) {
        val bookClass = itemToClass(itemId)
        return if (bookClass != null && bookClass == ctx.character.className) {
            Int.MAX_VALUE
        } else {
            0
        }
    }

    return null
}

/** Desktop UseItemRequest.itemToClass — item `Class:` modifier, or null when unset. */
fun itemToClass(itemId: Int): String? {
    val name = ItemDatabase.getItemName(itemId)
    if (name.isBlank()) return null
    return ModifierDatabase.getStringModifier(name, StringModifier.CLASS).ifBlank { null }
}

private const val RESOLUTION_ADVENTUROUS = 5471
private const val DARK_CHOCOLATE_HEART = 5498
private const val CSA_FIRE_STARTING_KIT = 5739
private const val RIGHT_BEAR_ARM = 5791
private const val LEFT_BEAR_ARM = 5792
private const val SUSHI_ROLLING_MAT = 3581
private const val ETERNAL_CAR_BATTERY = 6741
private const val FOLDER_01 = 6618
private const val FOLDER_23 = 6640
private const val PASTA_ADDITIVE = 6900
private const val CHRONER = 7567
private const val CHRONER_CROSS = 7723
private const val GAUDY_KEY = 4874
private const val PIRATE_FLEDGES = 3033
private const val SWASHBUCKLING_GETUP = 9
private const val BITTYCAR_MEATCAR = 5926
private const val BITTYCAR_HOTCAR = 5927
private const val BITTYCAR_SOULCAR = 6046
private const val STILL_BEATING_SPLEEN = 8086
private const val MAYO_CLINIC = 8260
private const val MAYONEX = 8261
private const val MAYOFLEX = 8265
private const val WRIST_BOY = 9102
private const val HOLORECORD_SHRIEKING_WEASEL = 9109
private const val HOLORECORD_DRUNK_UNCLES = 9115

private fun bittycarUses(ctx: ItemUseLimitsContext, model: String): Int {
    val active = ctx.preferences?.getString("_bittycar") ?: ""
    return if (active == model) 0 else 1
}

private fun wearingItem(ctx: ItemUseLimitsContext, itemId: Int): Boolean {
    val name = ItemDatabase.getItemName(itemId)
    if (name.isBlank()) return false
    return ctx.character.equipment.values.any { it.equals(name, ignoreCase = true) }
}

private val CLASS_BOOKS = setOf(
    4406, 5354, // The Art of Slapfighting
    4407, 5355, // Uncle Romulus
    4408, 5356, // A Beginner's Guide to Charming Snakes
    4409, 5357, // Zu Mannkäse Dienen
    4410, 5358, // Dynamite Superman Jones
    4411, 5359, // Inigo's Incantation of Inspiration
)

fun maximumUses(itemId: Int, itemName: String, ctx: ItemUseLimitsContext): Int {
    earlyMaximumUses(itemId, ctx)?.let { return it }

    val fullness = ConsumableDatabase.getFullnessByName(itemName)
    val inebriety = ConsumableDatabase.getInebrietyByName(itemName)
    val spleenHit = ConsumableDatabase.getSpleenByName(itemName)

    if (fullness > 0) {
        return eatMaximumUses(itemId, itemName, fullness, ctx)
    }
    if (inebriety > 0) {
        return drinkMaximumUses(itemId, itemName, inebriety, ctx, allowOverDrink = true)
    }
    if (spleenHit > 0) {
        return spleenMaximumUses(itemId, itemName, spleenHit, ctx)
    }

    when (itemId) {
        DARK_CHOCOLATE_HEART -> {
            if (restorationCap(itemId, itemName, ctx) == 0L) return 0
            DailyLimitDatabase.getEntry(itemId, DailyLimitKind.USE)?.let { entry ->
                return DailyLimitDatabase.getUsesRemaining(entry, ctx.preferences)
            }
        }
        RESOLUTION_ADVENTUROUS -> {
            if ((ctx.preferences?.getInt("_resolutionAdv", 0) ?: 0) == 10) return 0
        }
        CSA_FIRE_STARTING_KIT -> {
            val choice = ctx.preferences?.getInt("choiceAdventure595", 0) ?: 0
            if (!ctx.character.hippyStoneBroken && choice == 1) return 0
        }
        LEFT_BEAR_ARM -> return ctx.accessibleCount(RIGHT_BEAR_ARM)
        SUSHI_ROLLING_MAT ->
            return if (ctx.preferences?.getBoolean("hasSushiMat", false) == true) 0 else 1
        ETERNAL_CAR_BATTERY -> {
            if (restorationCap(itemId, itemName, ctx) == 0L) return 0
            DailyLimitDatabase.getEntry(itemId, DailyLimitKind.USE)?.let { entry ->
                return DailyLimitDatabase.getUsesRemaining(entry, ctx.preferences)
            }
        }
        in FOLDER_01..FOLDER_23 -> {
            val slots = EquipmentSlot.folderSlotsFor(ctx.character.inKoLHS)
            val open = slots.any { ctx.character.equippedItem(it) == null }
            return if (open) 1 else 0
        }
        PASTA_ADDITIVE -> {
            if (!ctx.character.isPastamancer) return 0
            if (ctx.preferences?.getBoolean("_pastaAdditive", false) == true) return 0
        }
        CHRONER_CROSS -> {
            if (ctx.accessibleCount(CHRONER) == 0) return 0
        }
        GAUDY_KEY -> {
            val wearingFledges = wearingItem(ctx, PIRATE_FLEDGES)
            val pieces = OutfitDatabase.getById(SWASHBUCKLING_GETUP)?.equipment
            val wearingOutfit = pieces != null &&
                OutfitManager.isWearingPieces(pieces, ctx.character.equipment)
            if (!wearingFledges && !wearingOutfit) return 0
        }
        BITTYCAR_HOTCAR -> return bittycarUses(ctx, "hotcar")
        BITTYCAR_MEATCAR -> return bittycarUses(ctx, "meatcar")
        BITTYCAR_SOULCAR -> return bittycarUses(ctx, "soulcar")
        STILL_BEATING_SPLEEN -> {
            val last = ctx.preferences?.getInt("lastStillBeatingSpleen") ?: -1
            return if (last == ctx.character.ascensionNumber) 0 else 1
        }
        in MAYONEX..MAYOFLEX -> {
            if (!CampgroundItemSync.hasWorkshedItem(ctx.preferences, MAYO_CLINIC)) return 0
            val inMouth = ctx.preferences?.getString("mayoInMouth") ?: ""
            return if (inMouth.isEmpty()) 1 else 0
        }
        in HOLORECORD_SHRIEKING_WEASEL..HOLORECORD_DRUNK_UNCLES ->
            return if (ctx.accessibleCount(WRIST_BOY) > 0) Int.MAX_VALUE else 0
    }

    if (!ItemDatabase.isPotion(itemId) && RestoreDatabase.isRestoreItem(itemId)) {
        val hpAvg = RestoreDatabase.getHpAverageByName(itemName, ctx.expressionContext)
        val mpAvg = RestoreDatabase.getMpAverageByName(itemName, ctx.expressionContext)
        if (hpAvg == 0.0 && mpAvg == 0.0) {
            return 0
        }
        val restoration = RestoreDatabase.restorationMaximum(
            itemName,
            ctx.character.currentHp,
            ctx.character.maxHp,
            ctx.character.currentMp,
            ctx.character.maxMp,
            ctx.expressionContext,
        )
        if (restoration < Long.MAX_VALUE) {
            return min(Int.MAX_VALUE.toLong(), restoration).toInt()
        }
    }

    DailyLimitDatabase.getEntry(itemId, DailyLimitKind.USE)?.let { entry ->
        return DailyLimitDatabase.getUsesRemaining(entry, ctx.preferences)
    }

    return Int.MAX_VALUE
}

private fun eatMaximumUses(
    itemId: Int,
    itemName: String,
    fullness: Int,
    ctx: ItemUseLimitsContext,
): Int {
    if (LimitModeGates.limitEating(ctx.character.limitMode)) return 0
    if (!ctx.character.canEat) return 0

    DailyLimitDatabase.getEntry(itemId, DailyLimitKind.EAT)?.let { entry ->
        return DailyLimitDatabase.getUsesRemaining(entry, ctx.preferences)
    }

    val fullnessLeft = ctx.character.fullnessRemaining
    return if (fullness == 0) Int.MAX_VALUE else fullnessLeft / fullness
}

private fun drinkMaximumUses(
    itemId: Int,
    itemName: String,
    inebriety: Int,
    ctx: ItemUseLimitsContext,
    allowOverDrink: Boolean,
): Int {
    if (LimitModeGates.limitDrinking(ctx.character.limitMode)) return 0
    if (!ctx.character.canDrink) return 0

    val inebrietyLeft = ctx.character.inebrietyRemaining
    if (inebrietyLeft < 0) return 0

    var maxAvailable = Int.MAX_VALUE
    DailyLimitDatabase.getEntry(itemId, DailyLimitKind.DRINK)?.let { entry ->
        val remaining = DailyLimitDatabase.getUsesRemaining(entry, ctx.preferences)
        if (remaining == 0) return 0
        maxAvailable = remaining
    }

    var maxNumber = if (inebriety == 0) Int.MAX_VALUE else inebrietyLeft / inebriety
    if (allowOverDrink && inebrietyLeft < inebriety && maxNumber != Int.MAX_VALUE) {
        maxNumber++
    }
    if (maxNumber > maxAvailable) {
        maxNumber = maxAvailable
    }
    return maxNumber
}

private fun spleenMaximumUses(
    itemId: Int,
    itemName: String,
    spleenHit: Int,
    ctx: ItemUseLimitsContext,
): Int {
    if (LimitModeGates.limitSpleening(ctx.character.limitMode)) return 0
    if (!ctx.character.canChew) return 0

    val restorationMaximum = restorationCap(itemId, itemName, ctx)
    val spleenLeft = ctx.character.spleenRemaining
    val usableMaximum = if (spleenHit == 0) Int.MAX_VALUE else spleenLeft / spleenHit

    DailyLimitDatabase.getEntry(itemId, DailyLimitKind.CHEW)?.let { entry ->
        return DailyLimitDatabase.getUsesRemaining(entry, ctx.preferences)
    }

    return min(usableMaximum.toLong(), restorationMaximum).toInt()
}

private fun restorationCap(itemId: Int, itemName: String, ctx: ItemUseLimitsContext): Long {
    if (ItemDatabase.isPotion(itemId) || !RestoreDatabase.isRestoreItem(itemId)) {
        return Long.MAX_VALUE
    }
    return RestoreDatabase.restorationMaximum(
        itemName,
        ctx.character.currentHp,
        ctx.character.maxHp,
        ctx.character.currentMp,
        ctx.character.maxMp,
        ctx.expressionContext,
    )
}
