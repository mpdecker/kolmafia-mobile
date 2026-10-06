package net.sourceforge.kolmafia.request

import kotlin.math.min
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.ZodiacSign
import net.sourceforge.kolmafia.data.ConcoctionQueueBudget
import net.sourceforge.kolmafia.data.ConsumableDatabase
import net.sourceforge.kolmafia.data.DailyLimitDatabase
import net.sourceforge.kolmafia.data.DailyLimitKind
import net.sourceforge.kolmafia.data.HolidayCalendar
import net.sourceforge.kolmafia.data.HolidayNames
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.OutfitDatabase
import net.sourceforge.kolmafia.data.RestoreDatabase
import net.sourceforge.kolmafia.data.SpeakeasyDatabase
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
    /** Null uses the live KoL holiday string. Tests pass an explicit holiday. */
    val holiday: String? = null,
    /** Null uses the live September–November check. Tests pass an explicit season. */
    val autumn: Boolean? = null,
    /** Null uses the live Monday check. Tests pass an explicit weekday. */
    val monday: Boolean? = null,
    /** Effect names currently on the character. Empty means none are active. */
    val activeEffectNames: Set<String> = emptySet(),
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
private const val SCHOOL_OF_HARD_KNOCKS_DIPLOMA = 9123
private const val PUNCHING_MIRROR = 11451
private const val SPARKLER = 2679
private const val SNAKE = 2680
private const val M282 = 2681
private const val VICTOR_SPOILS = 9489
private const val GREEN_ROCKET = 9827
private const val CRYSTALLIZED_PUMPKIN_SPICE = 11738
private const val TINY_BOTTLE_OF_ABSINTHE = 2655
private const val ELEVEN_LEAF_CLOVER = 10881

private fun currentHoliday(ctx: ItemUseLimitsContext): String =
    ctx.holiday ?: HolidayNames.getHoliday()

private fun inAutumn(ctx: ItemUseLimitsContext): Boolean =
    ctx.autumn ?: HolidayCalendar.isAutumn()

private fun isMonday(ctx: ItemUseLimitsContext): Boolean =
    ctx.monday ?: HolidayCalendar.isMonday()

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

fun maximumUses(
    itemId: Int,
    itemName: String,
    ctx: ItemUseLimitsContext,
    consumptionType: ItemPrimaryUse = ItemPrimaryUse.NONE,
): Int {
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
        TINY_HOUSE, TEARS_ITEM -> {
            // Desktop: Beaten Up + no HP/MP restore needed → still allow one use.
            val restoration = restorationCap(itemId, itemName, ctx)
            if (ctx.activeEffectNames.any { it.equals("Beaten Up", ignoreCase = true) } &&
                restoration == 0L
            ) {
                return 1
            }
        }
        MEDICINAL_HERBS -> {
            if (restorationCap(itemId, itemName, ctx) > 0L) return 1
        }
        FIELD_GAR_POTION -> {
            if (isMonday(ctx)) return 0
            if (ctx.activeEffectNames.any { it.equals("Gar-ish", ignoreCase = true) }) return 0
            return 1
        }
        in GREEN_PEAWEE_MARBLE..BLACK_CATSEYE_MARBLE ->
            return ctx.accessibleCount(itemId) / 2
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
        ANCIENT_CURSED_FOOTLOCKER -> return ctx.accessibleCount(SIMPLE_CURSED_KEY)
        ORNATE_CURSED_CHEST -> return ctx.accessibleCount(ORNATE_CURSED_KEY)
        GILDED_CURSED_CHEST -> return ctx.accessibleCount(GILDED_CURSED_KEY)
        STUFFED_CHEST -> return ctx.accessibleCount(STUFFED_KEY)
        MAID, CLOCKWORK_MAID, MEAT_BUTLER, PORTABLE_HOUSEKEEPING_ROBOT,
        SCARECROW, MEAT_GOLEM, MEAT_GLOBE, BLACK_BLUE_LIGHT, LOUDMOUTH_LARRY,
        PLASMA_BALL, FENG_SHUI, LED_CLOCK, BONSAI_TREE,
        NEWBIESPORT_TENT, BARSKIN_TENT, COTTAGE, HOUSE, SANDCASTLE, TWIG_HOUSE,
        GINGERBREAD_HOUSE, HOBO_FORTRESS, BRICKO_PYRAMID, GIANT_FARADAY_CAGE,
        SNOW_FORT, ELEVENT, RESIDENCE_CUBE, GIANT_PILGRIM_HAT, HOUSE_SIZED_MUSHROOM,
        MINI_KIWI_TIPI,
        -> return 1
        in MAYONEX..MAYOFLEX -> {
            if (!CampgroundItemSync.hasWorkshedItem(ctx.preferences, MAYO_CLINIC)) return 0
            val inMouth = ctx.preferences?.getString("mayoInMouth") ?: ""
            return if (inMouth.isEmpty()) 1 else 0
        }
        in HOLORECORD_SHRIEKING_WEASEL..HOLORECORD_DRUNK_UNCLES ->
            return if (ctx.accessibleCount(WRIST_BOY) > 0) Int.MAX_VALUE else 0
        SCHOOL_OF_HARD_KNOCKS_DIPLOMA, PUNCHING_MIRROR -> {
            if (!ctx.character.hippyStoneBroken) return 0
        }
        VICTOR_SPOILS -> {
            if (ctx.character.ascensionPath != AscensionPath.LICENSE_TO_ADVENTURE) return 0
        }
        M282, SNAKE, SPARKLER, GREEN_ROCKET -> {
            if (!currentHoliday(ctx).contains("Dependence Day")) return 0
        }
        CRYSTALLIZED_PUMPKIN_SPICE -> {
            if (!inAutumn(ctx)) return 0
        }
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

    if (CampgroundItemSync.isWorkshedItem(itemId)) {
        return if (ctx.preferences?.getBoolean("_workshedItemUsed", false) == true) 0 else 1
    }

    slotMaximumUses(consumptionType, ctx)?.let { return it }
    unstackableEffectUses(itemId, ctx)?.let { return it }

    return Int.MAX_VALUE
}

/**
 * Desktop `UseItemRequest.maximumUses` consumption-type switch.
 * `$item[dailyusesleft]` passes [ItemPrimaryUse.NONE] and skips these caps.
 */
private fun slotMaximumUses(consumptionType: ItemPrimaryUse, ctx: ItemUseLimitsContext): Int? =
    when (consumptionType) {
        ItemPrimaryUse.GROW -> if (ctx.character.isAxecore) 0 else 1
        ItemPrimaryUse.WEAPON,
        ItemPrimaryUse.FAMILIAR,
        ItemPrimaryUse.HAT,
        ItemPrimaryUse.PANTS,
        ItemPrimaryUse.CONTAINER,
        ItemPrimaryUse.SHIRT,
        ItemPrimaryUse.OFFHAND,
        -> 1
        ItemPrimaryUse.ACCESSORY -> 3
        else -> null
    }

/** Desktop `LIMITED_USES`: absinthe and the eleven-leaf clover do not stack. */
private fun unstackableEffectUses(itemId: Int, ctx: ItemUseLimitsContext): Int? {
    val effectName = when (itemId) {
        TINY_BOTTLE_OF_ABSINTHE -> "Absinthe-Minded"
        ELEVEN_LEAF_CLOVER -> "Lucky!"
        else -> return null
    }
    val active = ctx.activeEffectNames.any { it.equals(effectName, ignoreCase = true) }
    return if (active) 0 else 1
}

private const val STEEL_STOMACH = 2742
private const val STEEL_LIVER = 2743
private const val MAGICAL_SAUSAGE = 10060
private const val GHOST_PEPPER = 6468
private const val SPAGHETTI_BREAKFAST = 6616
private const val GETS_YOU_DRUNK = 6446
private const val GREEN_BEER = 1041
private const val RED_DRUNKI_BEAR = 5482
private const val GREEN_DRUNKI_BEAR = 5483
private const val YELLOW_DRUNKI_BEAR = 5484
private const val MIME_SHOTGLASS = 9676
private const val VIP_LOUNGE_KEY = 3947
private const val ICE_STEIN = 1618
private const val ICE_COLD_SIX_PACK = 138
private const val ANCIENT_CURSED_FOOTLOCKER = 3016
private const val ORNATE_CURSED_CHEST = 3017
private const val GILDED_CURSED_CHEST = 3018
private const val STUFFED_CHEST = 3949
private const val SIMPLE_CURSED_KEY = 3013
private const val ORNATE_CURSED_KEY = 3014
private const val GILDED_CURSED_KEY = 3015
private const val STUFFED_KEY = 3950
private const val MAID = 1000
private const val CLOCKWORK_MAID = 1113
private const val MEAT_BUTLER = 11262
private const val PORTABLE_HOUSEKEEPING_ROBOT = 11377
private const val SCARECROW = 104
private const val MEAT_GOLEM = 101
private const val MEAT_GLOBE = 636
private const val BLACK_BLUE_LIGHT = 3276
private const val LOUDMOUTH_LARRY = 3277
private const val PLASMA_BALL = 3281
private const val FENG_SHUI = 210
private const val LED_CLOCK = 6072
private const val BONSAI_TREE = 6120
private const val TINY_HOUSE = 592
private const val TEARS_ITEM = 869
private const val MEDICINAL_HERBS = 1274
private const val FIELD_GAR_POTION = 5257
private const val GREEN_PEAWEE_MARBLE = 4095
private const val BLACK_CATSEYE_MARBLE = 4104
private const val NEWBIESPORT_TENT = 69
private const val BARSKIN_TENT = 73
private const val COTTAGE = 143
private const val HOUSE = 526
private const val SANDCASTLE = 3127
private const val TWIG_HOUSE = 3374
private const val GINGERBREAD_HOUSE = 4347
private const val HOBO_FORTRESS = 3416
private const val BRICKO_PYRAMID = 4485
private const val GIANT_FARADAY_CAGE = 6668
private const val SNOW_FORT = 7089
private const val ELEVENT = 7295
private const val RESIDENCE_CUBE = 7758
private const val GIANT_PILGRIM_HAT = 9185
private const val HOUSE_SIZED_MUSHROOM = 10497
private const val MINI_KIWI_TIPI = 11600

private fun eatMaximumUses(
    itemId: Int,
    itemName: String,
    fullness: Int,
    ctx: ItemUseLimitsContext,
): Int {
    if (LimitModeGates.limitEating(ctx.character.limitMode)) return 0

    // Desktop EatItemRequest.maximumUses path gates (Grey Goo before organ canEat)
    if (ctx.character.isGreyGoo) return 1

    if (ctx.character.isJarlsberg && !JarlsbergianItems.isJarlsbergian(itemId)) {
        return 0
    }

    val notes = ConsumableDatabase.getNotesByName(itemName)
    if (ctx.character.inZombiecore &&
        itemId != STEEL_STOMACH &&
        !notes.startsWith("Zombie Slayer")
    ) {
        return 0
    }

    if (ctx.character.inNuclearAutumn && fullness > 1) {
        return 0
    }

    if (ctx.character.isVampyre) {
        if (itemId != MAGICAL_SAUSAGE && !notes.startsWith("Vampyre")) {
            return 0
        }
    } else if (notes.startsWith("Vampyre")) {
        return 0
    }

    when (itemId) {
        GHOST_PEPPER -> {
            if ((ctx.preferences?.getInt("ghostPepperTurnsLeft", 0) ?: 0) > 0) return 0
            return 1
        }
        SPAGHETTI_BREAKFAST -> {
            if (ctx.character.fullnessLimit == 0) return 0
            if (ctx.character.fullness > 0) return 0
        }
    }

    if (!ctx.character.canEat) return 0

    DailyLimitDatabase.getEntry(itemId, DailyLimitKind.EAT)?.let { entry ->
        return DailyLimitDatabase.getUsesRemaining(entry, ctx.preferences)
    }

    val fullnessLeft = ctx.character.fullnessRemaining
    return if (fullness == 0) Int.MAX_VALUE else fullnessLeft / fullness
}

/** Desktop DrinkItemRequest.maximumUses path/item gates. */
private fun drinkMaximumUses(
    itemId: Int,
    itemName: String,
    inebriety: Int,
    ctx: ItemUseLimitsContext,
    allowOverDrink: Boolean,
): Int {
    if (LimitModeGates.limitDrinking(ctx.character.limitMode)) return 0

    if (ctx.character.isGreyGoo) return 1

    if (ctx.character.isJarlsberg &&
        !JarlsbergianItems.isJarlsbergian(itemId) &&
        itemId != STEEL_LIVER
    ) {
        return 0
    }

    val notes = ConsumableDatabase.getNotesByName(itemName)
    if (ctx.character.inKoLHS &&
        itemId != STEEL_LIVER &&
        !notes.startsWith("KOLHS")
    ) {
        return 0
    }

    if (ctx.character.inNuclearAutumn &&
        ConsumableDatabase.getInebrietyByName(itemName) > 1
    ) {
        return 0
    }

    if (ctx.character.ascensionPath == AscensionPath.LICENSE_TO_ADVENTURE &&
        ItemDatabase.getImage(itemId) != "martini.gif"
    ) {
        return 0
    }

    if (ctx.character.isVampyre) {
        if (!notes.startsWith("Vampyre")) return 0
    } else if (notes.startsWith("Vampyre")) {
        return 0
    }

    var limit = ctx.character.inebrietyLimit
    when (itemId) {
        GETS_YOU_DRUNK -> {
            if ((ctx.preferences?.getInt("getsYouDrunkTurnsLeft", 0) ?: 0) > 0) return 0
            return 1
        }
        GREEN_BEER -> {
            if (currentHoliday(ctx).contains("St. Sneaky Pete's Day")) {
                limit += 10
            }
        }
        RED_DRUNKI_BEAR, GREEN_DRUNKI_BEAR, YELLOW_DRUNKI_BEAR ->
            return eatMaximumUses(itemId, itemName, fullness = 4, ctx)
    }

    if (!ctx.character.ascensionPath.canDrink) return 0

    val inebrietyLeft = limit - ctx.character.inebriety
    if (inebrietyLeft < 0) return 0

    var shotglass = 0
    if (inebriety == 1 &&
        !ConcoctionQueueBudget.queuedMimeShotglass &&
        ctx.accessibleCount(MIME_SHOTGLASS) > 0 &&
        ctx.preferences?.getBoolean("_mimeArmyShotglassUsed", false) != true
    ) {
        shotglass = 1
    }

    var maxAvailable = Int.MAX_VALUE
    DailyLimitDatabase.getEntry(itemId, DailyLimitKind.DRINK)?.let { entry ->
        val remaining = DailyLimitDatabase.getUsesRemaining(entry, ctx.preferences)
        if (remaining == 0) return 0
        maxAvailable = remaining
    }

    if (SpeakeasyDatabase.isSpeakeasyDrink(itemId) || SpeakeasyDatabase.isSpeakeasyDrink(itemName)) {
        if (ZodiacSign.find(ctx.character.zodiacSign)?.isBadMoon == true) return 0
        if (ctx.accessibleCount(VIP_LOUNGE_KEY) == 0) return 0
    }

    var overDrink = allowOverDrink
    if (inebrietyLeft < inebriety) {
        overDrink = true
    }

    var maxNumber = if (inebriety == 0) Int.MAX_VALUE else (inebrietyLeft / inebriety) + shotglass
    if (overDrink && maxNumber != Int.MAX_VALUE) {
        maxNumber++
    }
    if (maxNumber > maxAvailable) {
        maxNumber = maxAvailable
    }

    if (itemId == ICE_STEIN) {
        val sixpacks = ctx.accessibleCount(ICE_COLD_SIX_PACK)
        if (maxNumber > sixpacks) return sixpacks
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

    // Desktop SpleenItemRequest.maximumUses path gates
    if (ctx.character.isGreyGoo) return 1

    if (ctx.character.inNuclearAutumn &&
        ConsumableDatabase.getSpleenByName(itemName) > 1
    ) {
        return 0
    }

    if (!ctx.character.ascensionPath.canChew) return 0

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
