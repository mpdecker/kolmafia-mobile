package net.sourceforge.kolmafia.request

import kotlin.math.min
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConsumableDatabase
import net.sourceforge.kolmafia.data.DailyLimitDatabase
import net.sourceforge.kolmafia.data.DailyLimitKind
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.RestoreDatabase
import net.sourceforge.kolmafia.inventory.LimitModeGates
import net.sourceforge.kolmafia.modifiers.ExpressionContext
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

    return null
}

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
