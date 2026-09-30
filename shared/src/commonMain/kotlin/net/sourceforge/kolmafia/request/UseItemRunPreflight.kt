package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.ConsumableDatabase
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.inventory.LimitModeGates
import net.sourceforge.kolmafia.modifiers.ExpressionContext
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.math.min

/**
 * Desktop [UseItemRequest.run] preflight: memento binge refusal, seal-figurine club,
 * bedding and dwelling replacement, and the deck / bricko / sticker / diary / volcano
 * early returns.
 *
 * [confirmReplacement] is true because there is no desktop frame to ask.
 */
object UseItemRunPreflight {
    var lastUpdate: String = ""

    const val MEMENTO_MESSAGE = "Don't feed mementos to your familiars."
    const val CLUB_MESSAGE = "You really should wield a club before using that."
    const val MISSING_MESSAGE = "You don't have one of those."
    const val SPLITTING_BRICKS = "Splitting bricks..."
    const val DIARY_READ = "Your father's diary has been read."
    const val VOLCANO_READ = "The secret tropical island volcano lair map has been read."

    const val DECK_OF_EVERY_CARD = 8382
    const val REPLICA_DECK_OF_EVERY_CARD = 11230
    const val BRICKO_HAT = 4471
    const val BRICKO_PANTS = 4472
    const val BRICKO_SWORD = 4473
    const val STICKER_SWORD = 3508
    const val STICKER_CROSSBOW = 3526
    const val MACGUFFIN_DIARY = 2044
    const val ED_DIARY = 7960
    const val VOLCANO_MAP = 3291
    const val BIG_ROCK = 30
    const val SPICE_MELANGE = 3433
    const val ULTRA_MEGA_SOUR_BALL = 6852
    const val FANCY_CHOCOLATE_SCULPTURE = 9269
    const val ALIEN_PLANT_POD = 9421
    const val ALIEN_ANIMAL_MILK = 9429
    const val POWER_SPHERE = 3049
    const val OVERCHARGED_POWER_SPHERE = 3215
    const val YUMMY_TUMMY_BEAN = 905
    const val PACK_OF_POGS = 5505
    const val PHIAL_OF_HOTNESS = 6556
    const val PHIAL_OF_COLDNESS = 6557
    const val PHIAL_OF_SPOOKINESS = 6558
    const val PHIAL_OF_STENCH = 6559
    const val PHIAL_OF_SLEAZINESS = 6560
    const val JUMBO_DR_LUCIFER = 571
    const val SUPPORT_CUMMERBUND = 778
    const val MAFIA_ARIA = 781
    const val WHAT_CARD = 5511
    const val WHEN_CARD = 5512
    const val WHO_CARD = 5513
    const val WHERE_CARD = 5514

    private val ELEMENT_PHIAL_IDS = listOf(
        PHIAL_OF_HOTNESS,
        PHIAL_OF_COLDNESS,
        PHIAL_OF_SPOOKINESS,
        PHIAL_OF_STENCH,
        PHIAL_OF_SLEAZINESS,
    )

    sealed class Route {
        data object Proceed : Route()
        data class Refuse(val message: String) : Route()
        data class BreakBricko(val itemId: Int) : Route()
        data object FoldSticker : Route()
        data object ReadDiary : Route()
        data object ReadVolcanoMap : Route()
        data object PlayRandomDeck : Route()
    }

    fun mementoRefusal(binge: Boolean, preferences: Preferences?, itemName: String): String? {
        if (!binge || itemName.isBlank()) return null
        if (preferences?.getBoolean("mementoListActive", false) != true) return null
        val listed = preferences.getString("mementoList", "")
        val match = listed.split('|').any { it.equals(itemName, ignoreCase = true) }
        return if (match) MEMENTO_MESSAGE else null
    }

    /** No [GenericFrame]: replacement confirms do not block. */
    fun confirmReplacement(): Boolean = true

    fun route(itemId: Int, wieldingClub: Boolean, ownedCount: Int): Route {
        if (isSealFigurine(itemId) && !wieldingClub) return Route.Refuse(CLUB_MESSAGE)
        return when (itemId) {
            DECK_OF_EVERY_CARD, REPLICA_DECK_OF_EVERY_CARD -> Route.PlayRandomDeck
            BRICKO_SWORD, BRICKO_HAT, BRICKO_PANTS ->
                if (ownedCount < 1) Route.Refuse(MISSING_MESSAGE) else Route.BreakBricko(itemId)
            STICKER_SWORD, STICKER_CROSSBOW ->
                if (ownedCount < 1) Route.Refuse(MISSING_MESSAGE) else Route.FoldSticker
            MACGUFFIN_DIARY, ED_DIARY -> Route.ReadDiary
            VOLCANO_MAP -> Route.ReadVolcanoMap
            else -> Route.Proceed
        }
    }

    fun isSealFigurine(itemId: Int): Boolean =
        itemId in 3902..3911 || itemId == 4296

    fun isBedding(itemId: Int): Boolean = itemId in BEDDING_IDS

    fun needsBeddingReplacementConfirm(itemId: Int, currentBedId: Int): Boolean =
        isBedding(itemId) && currentBedId > 0

    fun dwellingLevel(itemId: Int): Int = DWELLING_LEVELS[itemId] ?: 0

    fun needsDwellingReplacementConfirm(itemId: Int, currentDwellingId: Int): Boolean {
        if (itemId !in DWELLING_LEVELS) return false
        val oldLevel = dwellingLevel(currentDwellingId)
        val newLevel = dwellingLevel(itemId)
        return oldLevel >= 7 || newLevel < oldLevel
    }

    /**
     * Desktop [UseItemRequest.needsConfirmation] — bed/dwelling replacement form field
     * `confirm=true` on inv_use (distinct from the headless dialog [confirmReplacement]).
     */
    fun needsConfirmFormField(
        itemId: Int,
        currentBedId: Int,
        currentDwellingId: Int,
    ): Boolean {
        if (isBedding(itemId)) return currentBedId > 0
        if (itemId !in DWELLING_LEVELS) return false
        return currentDwellingId != BIG_ROCK
    }

    /** Desktop skips [InventoryManager.retrieveItem] for [ItemPrimaryUse.REUSABLE] / USE_INFINITE. */
    fun shouldRetrieveBeforeUse(itemId: Int): Boolean =
        !ItemDatabase.isReusable(itemId)

    const val INSUFFICIENT_ITEMS = "Insufficient items to use."

    /**
     * Desktop [KoLCharacter.canEat] for the organ warning: path and limit gates,
     * not remaining stomach room.
     */
    fun pathCanEat(state: CharacterState): Boolean {
        if (LimitModeGates.limitEating(state.limitMode)) return false
        if (state.inNoobcore || state.isActuallyEd) return false
        return state.ascensionPath.canEat
    }

    /** Desktop [KoLCharacter.canDrink] for the organ warning. */
    fun pathCanDrink(state: CharacterState): Boolean {
        if (LimitModeGates.limitDrinking(state.limitMode)) return false
        if (state.inNoobcore || state.inRobocore || state.isMeat || state.isActuallyEd) return false
        if (state.ascensionPath == AscensionPath.PLUMBER ||
            state.ascensionPath == AscensionPath.PATH_OF_THE_PLUMBER
        ) {
            return false
        }
        return state.ascensionPath.canDrink
    }

    /**
     * Message desktop would confirm. A blank headless reply is N, so a non-null
     * message means the use stops.
     */
    fun organWarning(
        itemId: Int,
        itemName: String,
        canEat: Boolean,
        canDrink: Boolean,
        fullness: Int,
        inebriety: Int,
        chocolateSculpturesUsed: Int,
    ): String? = when (itemId) {
        SPICE_MELANGE, ULTRA_MEGA_SOUR_BALL -> {
            val stomach = canEat && fullness < 3
            val liver = canDrink && inebriety < 3
            if (!stomach && !liver) null
            else {
                val organ = when {
                    stomach && liver -> "stomach and liver"
                    stomach -> "stomach"
                    else -> "liver"
                }
                "A ${itemName}clears 3 $organ and you have not filled that yet.  Are you sure you want to use it?"
            }
        }
        FANCY_CHOCOLATE_SCULPTURE ->
            if (chocolateSculpturesUsed < 3) null
            else "Fancy chocolate sculptures are wasted after using 3. Are you sure you want to use it?"
        ALIEN_ANIMAL_MILK ->
            if (fullness >= 3) null
            else "Alien animal milk clears 3 stomach and you have not filled that yet.  Are you sure you want to use it?"
        ALIEN_PLANT_POD ->
            if (inebriety >= 3) null
            else "Alien plant pod clears 3 liver and you have not filled that yet.  Are you sure you want to use it?"
        else -> null
    }

    sealed class Consumption {
        data class Equip(val slot: EquipmentSlot) : Consumption()
        data object Portal : Consumption()
        data object Curse : Consumption()
        data class Unusable(val message: String) : Consumption()
    }

    /**
     * Desktop [UseItemRequest.getConsumptionType] then the equipment / sphere / curse switch.
     * Null means the existing inv_use path.
     */
    fun consumption(
        itemId: Int,
        equipped: Map<EquipmentSlot, String> = emptyMap(),
        inKoLHS: Boolean = false,
    ): Consumption? {
        val resolved = resolvedUse(itemId)
        return when (resolved) {
            ItemPrimaryUse.HAT,
            ItemPrimaryUse.WEAPON,
            ItemPrimaryUse.SIXGUN,
            ItemPrimaryUse.OFFHAND,
            ItemPrimaryUse.SHIRT,
            ItemPrimaryUse.PANTS,
            ItemPrimaryUse.CONTAINER,
            ItemPrimaryUse.ACCESSORY,
            ItemPrimaryUse.FAMILIAR,
            ItemPrimaryUse.STICKER,
            ItemPrimaryUse.CARD,
            ItemPrimaryUse.FOLDER,
            ItemPrimaryUse.BOOTSKIN,
            ItemPrimaryUse.BOOTSPUR,
            -> chooseEquipSlot(resolved, equipped, inKoLHS)?.let { Consumption.Equip(it) }
                ?: Consumption.Unusable("No suitable slot available for ${ItemDatabase.getItemName(itemId)}")
            ItemPrimaryUse.SPHERE -> Consumption.Portal
            ItemPrimaryUse.NONE, ItemPrimaryUse.UNKNOWN ->
                if (isCurse(itemId)) Consumption.Curse
                else Consumption.Unusable("${ItemDatabase.getItemName(itemId)} is unusable.")
            else -> null
        }
    }

    fun resolvedUse(itemId: Int): ItemPrimaryUse {
        val item = ItemDatabase.getById(itemId) ?: return ItemPrimaryUse.USABLE
        // Desktop consumptionType ZAP short-circuits useOnce → ZapCommand before retrieve.
        if (item.primaryUse == ItemPrimaryUse.ZAP) return ItemPrimaryUse.ZAP
        if (item.primaryUse == ItemPrimaryUse.SPLEEN || item.primaryUse == ItemPrimaryUse.GROW) {
            return item.primaryUse
        }
        if (ItemDatabase.isReusable(itemId)) return ItemPrimaryUse.REUSABLE
        if (ItemDatabase.isMultiUsable(itemId)) return ItemPrimaryUse.MULTIPLE
        if (ItemDatabase.isUsable(itemId)) return ItemPrimaryUse.USABLE
        return item.primaryUse
    }

    fun isCurse(itemId: Int): Boolean =
        ItemDatabase.getById(itemId)?.secondaryUses?.any { it.equals("curse", ignoreCase = true) } == true

    fun chooseEquipSlot(
        use: ItemPrimaryUse,
        equipped: Map<EquipmentSlot, String>,
        inKoLHS: Boolean,
    ): EquipmentSlot? = when (use) {
        ItemPrimaryUse.HAT -> EquipmentSlot.HAT
        ItemPrimaryUse.WEAPON -> EquipmentSlot.WEAPON
        ItemPrimaryUse.OFFHAND -> EquipmentSlot.OFFHAND
        ItemPrimaryUse.SHIRT -> EquipmentSlot.SHIRT
        ItemPrimaryUse.PANTS -> EquipmentSlot.PANTS
        ItemPrimaryUse.CONTAINER -> EquipmentSlot.CONTAINER
        ItemPrimaryUse.FAMILIAR -> EquipmentSlot.FAMILIAR
        ItemPrimaryUse.SIXGUN -> EquipmentSlot.HOLSTER
        ItemPrimaryUse.CARD -> EquipmentSlot.CARDSLEEVE
        ItemPrimaryUse.BOOTSKIN -> EquipmentSlot.BOOTSKIN
        ItemPrimaryUse.BOOTSPUR -> EquipmentSlot.BOOTSPUR
        ItemPrimaryUse.ACCESSORY ->
            firstEmpty(listOf(EquipmentSlot.ACC1, EquipmentSlot.ACC2, EquipmentSlot.ACC3), equipped)
                ?: EquipmentSlot.ACC1
        ItemPrimaryUse.STICKER -> firstEmpty(EquipmentSlot.STICKER_SLOTS, equipped)
        ItemPrimaryUse.FOLDER -> firstEmpty(EquipmentSlot.folderSlotsFor(inKoLHS), equipped)
        else -> null
    }

    private fun firstEmpty(
        slots: List<EquipmentSlot>,
        equipped: Map<EquipmentSlot, String>,
    ): EquipmentSlot? = slots.firstOrNull { equipped[it].isNullOrBlank() }

    /**
     * Desktop consume-loop plan after equipment redirects: level gate, maximumUses
     * clamp, and bean / pog / single-use iteration counts.
     */
    sealed class ConsumePlan {
        data class Refuse(val message: String) : ConsumePlan()
        data class Batches(
            val counts: List<Int>,
            val removeEffectId: Int? = null,
        ) : ConsumePlan()
    }

    fun meetsLevelRequirement(
        itemName: String,
        level: Int,
        canInteract: Boolean,
    ): Boolean {
        val req = ConsumableDatabase.getLevelReqByName(itemName) ?: return true
        if (level < req) return false
        if (req >= 13 && !canInteract) return false
        return true
    }

    fun planConsume(
        itemId: Int,
        itemName: String,
        quantity: Int,
        character: CharacterState,
        preferences: Preferences?,
        accessibleCount: (Int) -> Int = { 0 },
        activeEffectNames: Set<String> = emptySet(),
    ): ConsumePlan {
        if (!meetsLevelRequirement(itemName, character.level, character.canInteract)) {
            return ConsumePlan.Refuse("Insufficient level to consume $itemName")
        }
        val ctx = ItemUseLimitsContext(
            character = character,
            preferences = preferences,
            expressionContext = ExpressionContext(
                level = character.level,
                inebriety = character.inebriety,
                fullness = character.fullness,
                spleenUsed = character.spleenUsed,
                challengePath = character.challengePath,
                className = character.className,
                characterMaxHp = character.maxHp,
                characterMaxMp = character.maxMp,
                characterCurrentHp = character.currentHp,
            ),
            accessibleCount = accessibleCount,
            activeEffectNames = activeEffectNames,
        )
        val allowed = maximumUses(itemId, itemName, ctx)
        val count = min(quantity.coerceAtLeast(0), allowed)
        if (count < 1) return ConsumePlan.Batches(emptyList())
        return ConsumePlan.Batches(
            counts = consumeBatches(itemId, count),
            removeEffectId = conflictingFormEffectId(itemId, activeEffectNames),
        )
    }

    /** Desktop Yummy Tummy / pack of pogs / single-use iteration sizes. */
    fun consumeBatches(itemId: Int, count: Int): List<Int> {
        if (count <= 0) return emptyList()
        if (count == 1) return listOf(1)
        return when (itemId) {
            YUMMY_TUMMY_BEAN -> {
                val first = (count + 19) % 20 + 1
                val rest = (count - first) / 20
                buildList {
                    add(first)
                    repeat(rest) { add(20) }
                }
            }
            PACK_OF_POGS -> {
                val first = (count + 10) % 11 + 1
                val rest = (count - first) / 11
                buildList {
                    add(first)
                    repeat(rest) { add(11) }
                }
            }
            else -> {
                val use = resolvedUse(itemId)
                val raw = ItemDatabase.getById(itemId)?.primaryUse
                when {
                    use == ItemPrimaryUse.MULTIPLE -> listOf(count)
                    use == ItemPrimaryUse.REUSABLE && raw == ItemPrimaryUse.MULTIPLE ->
                        listOf(count)
                    use == ItemPrimaryUse.REUSABLE -> List(count) { 1 }
                    else -> List(count) { 1 }
                }
            }
        }
    }

    /**
     * Desktop elemental-phial preflight: remove the first other active form.
     * Returns that effect id, or null when none conflict.
     */
    fun conflictingFormEffectId(itemId: Int, activeEffectNames: Set<String>): Int? {
        val phialIndex = ELEMENT_PHIAL_IDS.indexOf(itemId)
        if (phialIndex < 0) return null
        val forms = BasementSync.ELEMENT_FORMS
        for (i in forms.indices) {
            if (i == phialIndex) continue
            val form = forms[i]
            val active = activeEffectNames.any { it.equals(form, ignoreCase = true) }
            if (!active) continue
            return EffectDatabase.getByName(form)?.id
        }
        return null
    }

    /** Desktop [ItemDatabase.isBRICKOMonster]. */
    fun isBrickoMonster(itemId: Int): Boolean = itemId in BRICKO_MONSTER_IDS

    /** Trivia cards need `answerplz=1` to resolve rather than show the question. */
    fun needsAnswerPlz(itemId: Int): Boolean = itemId in WHAT_CARD..WHERE_CARD

    /** Seal figurines and BRICKO monsters confirm the fight with a second `checked=1` use. */
    fun needsCheckedFollowUp(itemId: Int): Boolean =
        isSealFigurine(itemId) || isBrickoMonster(itemId)

    /**
     * Desktop Lucifer pre-use mana burn reserve:
     * `maxMP - 9 * (currentHP - 1)`.
     */
    fun luciferMinimumMp(maxMp: Int, currentHp: Int): Long =
        (maxMp - 9 * (currentHp - 1)).toLong()

    fun hasEquipped(equipment: Map<EquipmentSlot, String>, itemName: String): Boolean =
        equipment.values.any { it.equals(itemName, ignoreCase = true) }

    private val BRICKO_MONSTER_IDS = setOf(
        4474, 4475, 4476, 4477, 4478, 4479, 4480, 4481, 4482, 4483, 4484,
    )

    private val BEDDING_IDS = setOf(
        429, // beanbag chair
        2638, // gauze hammock
        3344, // hot bedding
        3345, // cold bedding
        3346, // stench bedding
        3347, // spooky bedding
        3348, // sleaze bedding
        4842, // sleeping stocking
        5888, // lazybones recliner
        6338, // saltwaterbed
        6890, // spirit bed
        11345, // forest canopy bed
    )

    private val DWELLING_LEVELS = mapOf(
        69 to 1, // newbiesport tent
        73 to 2, // barskin tent
        143 to 3, // cottage
        4485 to 4, // bricko pyramid
        526 to 5, // house
        3127 to 6, // sandcastle
        4771 to 7, // ginormous pumpkin
        3374 to 8, // twig house
        4347 to 9, // gingerbread house
        3416 to 10, // hobo fortress
        6668 to 11, // giant faraday cage
        7089 to 12, // snow fort
        7295 to 13, // elevent
        7758 to 14, // residence cube
        9185 to 15, // giant pilgrim hat
        10497 to 16, // house-sized mushroom
    )
}
