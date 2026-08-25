package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConsumableDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.EquipmentManager

/**
 * Desktop [UseItemRequest.parseConsumption] hub + Eat/Drink/Spleen delegates
 * (Phases 2031–2090). Soft-fails when HTML does not confirm consumption.
 * Session-log lines for inv_use/eat/booze/spleen remain in [RequestLogger].
 */
object UseItemConsumptionSync {

    @Volatile
    var lastItemUsedId: Int = 0
        private set

    @Volatile
    var lastItemUsedCount: Int = 0
        private set

    @Volatile
    var lastUpdate: String = ""
        private set

    /** Optional DI for gear-mutation arms (bootskin/folder/sticker/discard). */
    var equipmentManagerProvider: (() -> EquipmentManager?)? = null

    fun rememberLastItem(itemId: Int, count: Int) {
        lastItemUsedId = itemId
        lastItemUsedCount = count.coerceAtLeast(1)
        lastUpdate = ""
    }

    fun clearLastItem() {
        lastItemUsedId = 0
        lastItemUsedCount = 0
    }

    /**
     * @return false when consumption failed / was rejected
     */
    fun parseConsumption(
        responseText: String,
        itemId: Int = lastItemUsedId,
        count: Int = lastItemUsedCount,
        preferences: Preferences? = null,
        character: KoLCharacter? = null,
        inventory: InventoryManager? = null,
        consumeConfirmed: Boolean = true,
        equipmentManager: EquipmentManager? = equipmentManagerProvider?.invoke(),
    ): Boolean {
        if (itemId <= 0) return true
        val qty = count.coerceAtLeast(1)
        clearLastItem()

        if (isFailureGate(responseText)) {
            lastUpdate = failureMessage(responseText)
            return false
        }

        // Pref-writing reject paths before consumption-type routing (item DB may be unloaded in tests)
        if (responseText.contains("may only eat one of those per day", ignoreCase = true)) {
            lastUpdate = "You may only eat one of those per day."
            if (itemId == AFFIRMATION_COOKIE) {
                preferences?.setBoolean("_affirmationCookieEaten", true)
            }
            return false
        }
        if (responseText.contains("may only eat one of those per lifetime", ignoreCase = true)) {
            lastUpdate = "You may only eat one of those per lifetime."
            when (itemId) {
                DEEP_DISH_OF_LEGEND -> preferences?.setBoolean("deepDishOfLegendEaten", true)
                CALZONE_OF_LEGEND -> preferences?.setBoolean("calzoneOfLegendEaten", true)
                PIZZA_OF_LEGEND -> preferences?.setBoolean("pizzaOfLegendEaten", true)
            }
            return false
        }
        if (responseText.contains("only use one pirate fork per day", ignoreCase = true)) {
            lastUpdate = "You may only eat from the pirate fork once per day."
            preferences?.setBoolean("_pirateForkUsed", true)
            return false
        }

        if (applyGearMutation(responseText, itemId, qty, inventory, equipmentManager)) {
            return true
        }

        val name = ItemDatabase.getItemName(itemId)
        val primary = ItemDatabase.getById(itemId)?.primaryUse

        return when {
            primary == ItemPrimaryUse.FOOD ||
                primary == ItemPrimaryUse.FOOD_HELPER ||
                ConsumableDatabase.getFullnessByName(name) > 0 ||
                itemId == MAGICAL_SAUSAGE ->
                parseEat(responseText, itemId, name, qty, preferences, character, inventory)
            primary == ItemPrimaryUse.DRINK ||
                primary == ItemPrimaryUse.DRINK_HELPER ||
                ConsumableDatabase.getInebrietyByName(name) > 0 ->
                parseDrink(responseText, itemId, name, qty, preferences, character, inventory)
            primary == ItemPrimaryUse.SPLEEN ||
                ConsumableDatabase.getSpleenByName(name) > 0 ->
                parseSpleen(responseText, itemId, name, qty, preferences, character, inventory)
            else ->
                parseUse(
                    responseText, itemId, name, qty,
                    preferences, character, inventory, consumeConfirmed,
                )
        }
    }

    fun isFailureGate(responseText: String): Boolean =
        responseText.contains("You don't have the item you're trying to use.", ignoreCase = true) ||
            responseText.contains("You are too scared of Bs", ignoreCase = true) ||
            responseText.contains("too in love with G to use that item", ignoreCase = true) ||
            responseText.contains("can't figure out where to put that potion", ignoreCase = true) ||
            responseText.contains("You've already absorbed this pattern", ignoreCase = true) ||
            responseText.contains("be at least level", ignoreCase = true) ||
            responseText.contains("That item is too old to be used on this path", ignoreCase = true) ||
            responseText.contains("no|in a special area", ignoreCase = true)

    private fun failureMessage(responseText: String): String = when {
        responseText.contains("don't have the item", ignoreCase = true) -> "You don't have that item."
        responseText.contains("too scared of Bs", ignoreCase = true) -> "You are too scared of Bs."
        responseText.contains("too in love with G", ignoreCase = true) -> "You are too in love with G."
        responseText.contains("put that potion", ignoreCase = true) ->
            "You need the Biomass Processing Function CPU upgrade to use potions."
        responseText.contains("already absorbed", ignoreCase = true) -> "Already absorbed."
        responseText.contains("be at least level", ignoreCase = true) -> "Item level too high."
        responseText.contains("too old to be used", ignoreCase = true) -> "Restricted by Standard."
        responseText.contains("special area", ignoreCase = true) -> "Restricted by limitmode."
        else -> "Item use failed."
    }

    private fun parseEat(
        responseText: String,
        itemId: Int,
        itemName: String,
        count: Int,
        preferences: Preferences?,
        character: KoLCharacter?,
        inventory: InventoryManager?,
    ): Boolean {
        if (EatFoodRequest.isEatAbort(responseText) ||
            responseText.contains("that isn't what you're hungry for", ignoreCase = true) ||
            responseText.contains("That's what breakfast means", ignoreCase = true)
        ) {
            lastUpdate = EatFoodRequest.eatAbortReason(responseText)
            return false
        }
        if (responseText.contains("may only eat one of those per day", ignoreCase = true)) {
            lastUpdate = "You may only eat one of those per day."
            if (itemId == AFFIRMATION_COOKIE) {
                preferences?.setBoolean("_affirmationCookieEaten", true)
            }
            return false
        }
        if (responseText.contains("may only eat one of those per lifetime", ignoreCase = true)) {
            lastUpdate = "You may only eat one of those per lifetime."
            when (itemId) {
                DEEP_DISH_OF_LEGEND -> preferences?.setBoolean("deepDishOfLegendEaten", true)
                CALZONE_OF_LEGEND -> preferences?.setBoolean("calzoneOfLegendEaten", true)
                PIZZA_OF_LEGEND -> preferences?.setBoolean("pizzaOfLegendEaten", true)
            }
            return false
        }
        if (responseText.contains("only use one pirate fork per day", ignoreCase = true)) {
            lastUpdate = "You may only eat from the pirate fork once per day."
            preferences?.setBoolean("_pirateForkUsed", true)
            return false
        }

        inventory?.consumeItemLocally(itemId, count)
        applyEatPrefs(itemId, count, preferences)

        if (!responseText.contains(" Fullness")) {
            var fullnessUsed = ConsumableDatabase.getFullnessByName(itemName) * count
            if (responseText.contains("Mayodiol kicks in", ignoreCase = true)) {
                fullnessUsed = (fullnessUsed - 1).coerceAtLeast(0)
            }
            if (fullnessUsed > 0 && character != null) {
                val s = character.state.value
                character.updateConsumables(
                    fullness = s.fullness + fullnessUsed,
                    inebriety = s.inebriety,
                    spleenUsed = s.spleenUsed,
                )
            }
        }
        return true
    }

    private fun parseDrink(
        responseText: String,
        itemId: Int,
        itemName: String,
        count: Int,
        preferences: Preferences?,
        character: KoLCharacter?,
        inventory: InventoryManager?,
    ): Boolean {
        if (DrinkBoozeRequest.isDrinkAbort(responseText)) {
            lastUpdate = DrinkBoozeRequest.drinkAbortReason(responseText)
            return false
        }

        inventory?.consumeItemLocally(itemId, count)
        applyDrinkPrefs(itemId, preferences)

        if (!responseText.contains(" Drunkenness") && !responseText.contains(" Inebriety")) {
            val inebrietyUsed = ConsumableDatabase.getInebrietyByName(itemName) * count
            if (inebrietyUsed > 0 && character != null) {
                val s = character.state.value
                character.updateConsumables(
                    fullness = s.fullness,
                    inebriety = s.inebriety + inebrietyUsed,
                    spleenUsed = s.spleenUsed,
                )
            }
        }
        return true
    }

    private fun parseSpleen(
        responseText: String,
        itemId: Int,
        itemName: String,
        count: Int,
        @Suppress("UNUSED_PARAMETER") preferences: Preferences?,
        character: KoLCharacter?,
        inventory: InventoryManager?,
    ): Boolean {
        if (responseText.contains("too much spleen", ignoreCase = true) ||
            responseText.contains("don't feel like chewing", ignoreCase = true)
        ) {
            lastUpdate = "Spleen limit reached."
            return false
        }

        inventory?.consumeItemLocally(itemId, count)

        if (!responseText.contains(" Spleen")) {
            val spleenHit = ConsumableDatabase.getSpleenByName(itemName) * count
            if (spleenHit > 0 && character != null) {
                val s = character.state.value
                character.updateConsumables(
                    fullness = s.fullness,
                    inebriety = s.inebriety,
                    spleenUsed = s.spleenUsed + spleenHit,
                )
            }
        }
        return true
    }

    private fun parseUse(
        responseText: String,
        itemId: Int,
        @Suppress("UNUSED_PARAMETER") itemName: String,
        count: Int,
        preferences: Preferences?,
        character: KoLCharacter?,
        inventory: InventoryManager?,
        consumeConfirmed: Boolean,
    ): Boolean {
        when (itemId) {
            PHOTOCOPIER -> {
                if (!responseText.contains("you drop your pants and giggle", ignoreCase = true)) {
                    return false
                }
                preferences?.setString("photocopyMonster", "Your butt")
                inventory?.consumeItemLocally(itemId, count)
            }
            PHOTOCOPIED_MONSTER -> {
                preferences?.setBoolean("_photocopyUsed", true)
                return true
            }
            MOJO_FILTER -> {
                if (responseText.contains("three is the number of filters", ignoreCase = true)) {
                    val current = preferences?.getInt("currentMojoFilters", 0) ?: 0
                    preferences?.setInt("currentMojoFilters", maxOf(4 - count, current))
                    return false
                }
                if (!responseText.contains("now-grodulated", ignoreCase = true)) {
                    return false
                }
                preferences?.setInt(
                    "currentMojoFilters",
                    (preferences.getInt("currentMojoFilters", 0) + count),
                )
                if (character != null) {
                    val s = character.state.value
                    character.updateConsumables(
                        fullness = s.fullness,
                        inebriety = s.inebriety,
                        spleenUsed = (s.spleenUsed - count).coerceAtLeast(0),
                    )
                }
                inventory?.consumeItemLocally(itemId, count)
            }
            ASTRAL_MUSHROOM, GONG -> {
                if (consumeConfirmed || looksUsed(responseText)) {
                    inventory?.consumeItemLocally(itemId, count)
                }
            }
            DANCE_CARD -> {
                if (looksUsed(responseText) || responseText.contains("dance card", ignoreCase = true)) {
                    inventory?.consumeItemLocally(itemId, count)
                    preferences?.setInt("_danceCardFightsLeft", 3)
                }
            }
            else -> {
                if (consumeConfirmed || looksUsed(responseText)) {
                    if (!ItemDatabase.isReusable(itemId)) {
                        inventory?.consumeItemLocally(itemId, count)
                    }
                }
            }
        }
        return true
    }

    private fun looksUsed(responseText: String): Boolean =
        responseText.contains("You acquire", ignoreCase = true) ||
            responseText.contains("You gain", ignoreCase = true) ||
            responseText.contains("You eat", ignoreCase = true) ||
            responseText.contains("You drink", ignoreCase = true) ||
            responseText.contains("You use", ignoreCase = true) ||
            responseText.contains("You chew", ignoreCase = true) ||
            responseText.contains("choice.php", ignoreCase = true)

    /**
     * High-traffic UseItem gear arms (Phases 2121–2135): bootskin/spur, folder,
     * sticker install, worm-hook discard.
     * @return true when the item was handled as gear (skip organ routing)
     */
    private fun applyGearMutation(
        responseText: String,
        itemId: Int,
        count: Int,
        inventory: InventoryManager?,
        equipmentManager: EquipmentManager?,
    ): Boolean {
        val mgr = equipmentManager ?: equipmentManagerProvider?.invoke()
        when (itemId) {
            in BOOTSKINS -> {
                if (!looksUsed(responseText) && !responseText.contains("skin", ignoreCase = true)) {
                    return false
                }
                mgr?.setEquipment(EquipmentSlot.BOOTSKIN, itemId, swapInventory = true)
                    ?: inventory?.consumeItemLocally(itemId, count)
                return true
            }
            in BOOTSPURS -> {
                if (!looksUsed(responseText) && !responseText.contains("spur", ignoreCase = true)) {
                    return false
                }
                mgr?.setEquipment(EquipmentSlot.BOOTSPUR, itemId, swapInventory = true)
                    ?: inventory?.consumeItemLocally(itemId, count)
                return true
            }
            in FOLDERS -> {
                if (!looksUsed(responseText) && !responseText.contains("folder", ignoreCase = true)) {
                    return false
                }
                mgr?.autoequipItem(itemId, swapInventory = true)
                    ?: inventory?.consumeItemLocally(itemId, count)
                return true
            }
            STICKER_SWORD, STICKER_CROSSBOW -> {
                // Fold/equip sticker weapon — consume on confirmed use
                if (looksUsed(responseText)) {
                    inventory?.consumeItemLocally(itemId, count)
                }
                return true
            }
        }

        val primary = ItemDatabase.getById(itemId)?.primaryUse
        when (primary) {
            ItemPrimaryUse.BOOTSKIN -> {
                mgr?.setEquipment(EquipmentSlot.BOOTSKIN, itemId, swapInventory = true)
                return true
            }
            ItemPrimaryUse.BOOTSPUR -> {
                mgr?.setEquipment(EquipmentSlot.BOOTSPUR, itemId, swapInventory = true)
                return true
            }
            ItemPrimaryUse.FOLDER -> {
                mgr?.autoequipItem(itemId, swapInventory = true)
                return true
            }
            ItemPrimaryUse.STICKER -> {
                mgr?.autoequipItem(itemId, swapInventory = true)
                return true
            }
            else -> Unit
        }

        // Worm-riding hooks discarded when using desert progress items / gnasir manuals
        if (responseText.contains("worm-riding hooks", ignoreCase = true) ||
            responseText.contains("worm riding hooks", ignoreCase = true)
        ) {
            mgr?.discardEquipment(WORM_RIDING_HOOKS)
            return false // continue normal consume path
        }
        return false
    }

    private fun applyEatPrefs(itemId: Int, count: Int, preferences: Preferences?) {
        val prefs = preferences ?: return
        when (itemId) {
            AFFIRMATION_COOKIE -> prefs.setBoolean("_affirmationCookieEaten", true)
            DEEP_DISH_OF_LEGEND -> prefs.setBoolean("deepDishOfLegendEaten", true)
            CALZONE_OF_LEGEND -> prefs.setBoolean("calzoneOfLegendEaten", true)
            PIZZA_OF_LEGEND -> prefs.setBoolean("pizzaOfLegendEaten", true)
            PIRATE_FORK -> prefs.setBoolean("_pirateForkUsed", true)
        }
        prefs.setString("mayoInMouth", "")
        val munchies = prefs.getInt("munchiesPillsUsed", 0)
        if (munchies > 0) {
            prefs.setInt("munchiesPillsUsed", (munchies - count).coerceAtLeast(0))
        }
    }

    private fun applyDrinkPrefs(itemId: Int, preferences: Preferences?) {
        val prefs = preferences ?: return
        if (prefs.getBoolean("mimeShotglassAvailable", false) &&
            !prefs.getBoolean("_mimeShotglassUsed", false) &&
            ConsumableDatabase.getInebrietyByName(ItemDatabase.getItemName(itemId)) == 1
        ) {
            prefs.setBoolean("_mimeShotglassUsed", true)
        }
    }

    const val MAGICAL_SAUSAGE = 10653
    const val PHOTOCOPIER = ItemDatabase.PHOTOCOPIER
    const val PHOTOCOPIED_MONSTER = ItemDatabase.PHOTOCOPIED_MONSTER
    const val MOJO_FILTER = ItemDatabase.MOJO_FILTER
    const val ASTRAL_MUSHROOM = ItemDatabase.ASTRAL_MUSHROOM
    const val GONG = ItemDatabase.GONG
    const val DANCE_CARD = ItemDatabase.DANCE_CARD
    const val AFFIRMATION_COOKIE = 9486
    const val PIRATE_FORK = 10227
    const val PIZZA_OF_LEGEND = 10991
    const val CALZONE_OF_LEGEND = 10992
    const val DEEP_DISH_OF_LEGEND = 11000
    const val WORM_RIDING_HOOKS = 2302
    const val STICKER_SWORD = 3508
    const val STICKER_CROSSBOW = 3526
    const val FOLDER_01 = 6618
    val FOLDERS: IntRange = FOLDER_01..(FOLDER_01 + 27)
    val BOOTSKINS = setOf(8937, 8938, 8939, 8940, 8941, 8942)
    val BOOTSPURS = setOf(8947, 8948, 8949, 8950, 8951, 8952, 8953)
}
