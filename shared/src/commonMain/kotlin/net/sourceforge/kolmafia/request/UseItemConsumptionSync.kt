package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConsumableDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.EquipmentManager
import net.sourceforge.kolmafia.session.TurnCounter

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

    /**
     * Set when a desktop `return` kept the item. Skips the post-success
     * uneffect sweep so a rejected antidote does not clear poison.
     */
    @Volatile
    var suppressEffectRemoval: Boolean = false
        private set

    /** Test / ASH harness hook — desktop UseItemRequest.lastUpdate write-back. */
    fun setLastUpdateForTest(message: String) {
        lastUpdate = message
    }

    /** Optional DI for gear-mutation arms (bootskin/folder/sticker/discard). */
    var equipmentManagerProvider: (() -> EquipmentManager?)? = null
    /** Optional active familiar for protogenetic soup weight. */
    var familiarManagerProvider: (() -> FamiliarManager?)? = null
    /** Optional active-effect state hook for antidotes, tiny houses, cocoa, and similar removers. */
    var effectManagerProvider: (() -> EffectManager?)? = null

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
        familiarManager: FamiliarManager? = familiarManagerProvider?.invoke(),
    ): Boolean {
        suppressEffectRemoval = false
        UseItemRequestState.clearFollowUps()
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
        if (responseText.contains("only drink from your everfull glass once a day")) {
            lastUpdate = "You may only drink from the everfull glass once a day."
            preferences?.setBoolean("_everfullGlassUsed", true)
            return false
        }

        if (applyGearMutation(responseText, itemId, qty, inventory, equipmentManager)) {
            return true
        }

        val name = ItemDatabase.getItemName(itemId)
        val primary = ItemDatabase.getById(itemId)?.primaryUse

        val success = when {
            // Glitch season reward is tagged food+reusable with 0 fullness; its side effects
            // live on the use path (desktop parseConsumption item switch), not eat.
            itemId == 10207 ->
                parseUse(
                    responseText, itemId, name, qty,
                    preferences, character, inventory, consumeConfirmed,
                    equipmentManager, familiarManager,
                )
            primary == ItemPrimaryUse.FOOD ||
                primary == ItemPrimaryUse.FOOD_HELPER ||
                ConsumableDatabase.getFullnessByName(name) > 0 ||
                itemId == MAGICAL_SAUSAGE ||
                itemId in KNOWN_FOOD_IDS ->
                parseEat(responseText, itemId, name, qty, preferences, character, inventory)
            primary == ItemPrimaryUse.DRINK ||
                primary == ItemPrimaryUse.DRINK_HELPER ||
                ConsumableDatabase.getInebrietyByName(name) > 0 ||
                itemId in KNOWN_DRINK_IDS ->
                parseDrink(responseText, itemId, name, qty, preferences, character, inventory)
            primary == ItemPrimaryUse.SPLEEN ||
                ConsumableDatabase.getSpleenByName(name) > 0 ||
                itemId in KNOWN_SPLEEN_IDS ->
                parseSpleen(responseText, itemId, name, qty, preferences, character, inventory)
            else ->
                parseUse(
                    responseText, itemId, name, qty,
                    preferences, character, inventory, consumeConfirmed,
                    equipmentManager, familiarManager,
                )
        }
        if (success && !suppressEffectRemoval) {
            val removed = UneffectRemovableMaps.removableEffectIdsForItem(itemId)
            effectManagerProvider?.invoke()?.removeEffects(removed)
        }
        return success
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
        // Item-specific consumption failure / message-only consume
        when (itemId) {
            FORTUNE_COOKIE -> {
                if (responseText.contains("You brutally smash the fortune cookie")) {
                    inventory?.consumeItemLocally(itemId, count)
                    return true
                }
            }
            CARTON_OF_SNAKE_MILK -> {
                if (responseText.contains("cream cheese")) {
                    inventory?.consumeItemLocally(itemId, count)
                    return true
                }
            }
            GHOST_PEPPER -> {
                if (responseText.contains("You shouldn't eat one of those")) {
                    if ((preferences?.getInt("ghostPepperTurnsLeft", 0) ?: 0) == 0) {
                        preferences?.setInt("ghostPepperTurnsLeft", 4)
                    }
                    lastUpdate = "You already have a ghost pepper terrifying your innards."
                    return false
                }
            }
        }

        if (responseText.contains("that isn't what you're hungry for", ignoreCase = true)) {
            lastUpdate = "You can only eat tasty, tasty brains."
            return false
        }
        if (responseText.contains("That's what breakfast means", ignoreCase = true)) {
            lastUpdate = "A spaghetti breakfast must be your the first food of the day."
            return false
        }
        if (responseText.contains("don't feel like eating", ignoreCase = true)) {
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

        val shouldUpdateFullness = !responseText.contains(" Fullness")
        if (responseText.contains("too full", ignoreCase = true)) {
            return applyTooFullPartial(
                itemId = itemId,
                itemName = itemName,
                count = count,
                shouldUpdateFullness = shouldUpdateFullness,
                preferences = preferences,
                character = character,
                inventory = inventory,
            )
        }

        val helperId = net.sourceforge.kolmafia.session.ConsumptionHelperState.currentFoodHelper()?.first
        if (helperId != null) {
            val helperOk = when (helperId) {
                SCRATCHS_FORK -> {
                    when {
                        responseText.contains("The salad fork cools") -> {
                            preferences?.setBoolean("_saladForkUsed", true)
                            true
                        }
                        responseText.contains("You may only use one of those per day.") -> {
                            preferences?.setBoolean("_saladForkUsed", true)
                            false
                        }
                        else -> false
                    }
                }
                FUDGE_SPORK -> {
                    if (responseText.contains("you eat your fudge spork")) {
                        preferences?.setBoolean("_fudgeSporkUsed", true)
                        true
                    } else {
                        false
                    }
                }
                else -> true
            }
            if (!helperOk) {
                lastUpdate = "Consumption helper failed."
                return false
            }
            inventory?.consumeItemLocally(helperId, 1)
        }

        // Black pudding fight-force: undo consume if we didn't actually eat blood sausage
        if (itemId == BLACK_PUDDING && !responseText.contains("blood sausage")) {
            // Desktop removes then re-adds; we simply skip consume + fullness for fight path.
            if (responseText.contains("don't have time")) {
                lastUpdate = "Insufficient adventures left."
            } else if (responseText.contains("too beaten up")) {
                lastUpdate = "Too beaten up."
            }
            if (lastUpdate.isNotEmpty()) return false
            // Fight redirect path — item consumed elsewhere
            return true
        }

        inventory?.consumeItemLocally(itemId, count)
        applyEatPrefs(itemId, count, preferences, responseText, character)
        updateTimeSpinner(itemId, preferences, timeSpinnerUsed = false)
        if (preferences?.getBoolean("universalSeasoningActive", false) == true) {
            preferences.setBoolean("universalSeasoningActive", false)
        }
        EatFoodRequest.handleFoodHelper(
            itemName = itemName,
            count = count,
            responseText = responseText,
            preferences = preferences,
            inventory = inventory,
            character = character,
            adjustFullness = false,
        )

        if (shouldUpdateFullness) {
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

    private fun applyTooFullPartial(
        itemId: Int,
        itemName: String,
        count: Int,
        shouldUpdateFullness: Boolean,
        preferences: Preferences?,
        character: KoLCharacter?,
        inventory: InventoryManager?,
    ): Boolean {
        lastUpdate = "Consumption limit reached."
        val fullness = ConsumableDatabase.getFullnessByName(itemName)
        if (fullness <= 0 || character == null) return false

        val state = character.state.value
        val maxFullness = state.fullnessLimit
        val maxEat = (maxFullness - state.fullness) / fullness
        val couldEat = maxOf(0, minOf(count - 1, maxEat))
        if (couldEat > 0) {
            if (shouldUpdateFullness) {
                character.updateConsumables(
                    fullness = state.fullness + couldEat * fullness,
                    inebriety = state.inebriety,
                    spleenUsed = state.spleenUsed,
                )
            }
            preferences?.setInt(
                "munchiesPillsUsed",
                (preferences.getInt("munchiesPillsUsed", 0) - couldEat).coerceAtLeast(0),
            )
            inventory?.consumeItemLocally(itemId, couldEat)
        }
        val estimatedFullness = maxFullness - fullness + 1
        val after = character.state.value
        if (estimatedFullness > after.fullness) {
            character.updateConsumables(
                fullness = estimatedFullness,
                inebriety = after.inebriety,
                spleenUsed = after.spleenUsed,
            )
        }
        return false
    }

    /**
     * Desktop [EatItemRequest.updateTimeSpinner] — track tradeable foods for Time-Spinner /
     * Thanksgetting.
     */
    fun updateTimeSpinner(
        itemId: Int,
        preferences: Preferences?,
        timeSpinnerUsed: Boolean = false,
    ) {
        val prefs = preferences ?: return
        if (timeSpinnerUsed) {
            prefs.increment("_timeSpinnerMinutesUsed", 3)
            return
        }
        if (!ItemDatabase.isDiscardable(itemId) ||
            !ItemDatabase.isTradeable(itemId) ||
            ItemDatabase.isGiftItem(itemId)
        ) {
            return
        }
        val itemString = itemId.toString()
        val foodAvailable = prefs.getString("_timeSpinnerFoodAvailable", "")
        if (foodAvailable.split(",").any { it == itemString }) return
        prefs.setString(
            "_timeSpinnerFoodAvailable",
            if (foodAvailable.isEmpty()) itemString else "$foodAvailable,$itemString",
        )
        if (itemId in CANDIED_SWEET_POTATOES..BREAD_ROLL) {
            prefs.increment("_thanksgettingFoodsEaten", 1)
        }
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
        if (responseText.contains("only drink from your everfull glass once a day")) {
            lastUpdate = "You may only drink from the everfull glass once a day."
            preferences?.setBoolean("_everfullGlassUsed", true)
            return false
        }

        // Helper success / fail (Frosty's Mug)
        val helperId = net.sourceforge.kolmafia.session.ConsumptionHelperState.currentDrinkHelper()?.first
        if (helperId == FROSTYS_MUG) {
            when {
                responseText.contains("discard the no-longer-frosty") ->
                    preferences?.setBoolean("_frostyMugUsed", true)
                responseText.contains("You may only use one of those per day.") -> {
                    preferences?.setBoolean("_frostyMugUsed", true)
                    lastUpdate = "Consumption helper failed."
                    return false
                }
                else -> {
                    lastUpdate = "Consumption helper failed."
                    return false
                }
            }
            inventory?.consumeItemLocally(helperId, 1)
        }

        when (itemId) {
            ICE_STEIN -> {
                if (responseText.contains("This is a job for a six-pack")) {
                    lastUpdate = "Your ice-stein needs an ice-cold six-pack."
                    return false
                }
                if (responseText.contains("pull a beer from your six-pack") ||
                    responseText.contains("pour it into the stein")
                ) {
                    inventory?.consumeItemLocally(ICE_COLD_SIX_PACK, count)
                }
            }
            GETS_YOU_DRUNK -> {
                if (responseText.contains("You shouldn't drink one of those")) {
                    if ((preferences?.getInt("getsYouDrunkTurnsLeft", 0) ?: 0) == 0) {
                        preferences?.setInt("getsYouDrunkTurnsLeft", 4)
                    }
                    lastUpdate = "You already have a Gets-You-Drunk melting your innards."
                    return false
                }
            }
        }

        // Everfull glass is not consumed
        if (itemId != EVERFULL_GLASS) {
            inventory?.consumeItemLocally(itemId, count)
        }
        applyDrinkPrefs(itemId, count, preferences, responseText, inventory, character)
        DrinkBoozeRequest.parseDrinkHelpers(responseText, preferences)

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
        preferences: Preferences?,
        character: KoLCharacter?,
        inventory: InventoryManager?,
    ): Boolean {
        if (responseText.contains("too much spleen", ignoreCase = true) ||
            responseText.contains("don't feel like chewing", ignoreCase = true)
        ) {
            lastUpdate = "Spleen limit reached."
            return false
        }

        val spleenHit = ConsumableDatabase.getSpleenByName(itemName)

        // Desktop SpleenItemRequest.parseConsumption rupture partial-consume math
        if (responseText.contains("rupture", ignoreCase = true)) {
            return applySpleenRupturePartial(
                itemId = itemId,
                spleenHit = spleenHit,
                count = count,
                character = character,
                inventory = inventory,
            )
        }

        inventory?.consumeItemLocally(itemId, count)

        if (!responseText.contains(" Spleen")) {
            val used = spleenHit * count
            if (used > 0 && character != null) {
                val s = character.state.value
                character.updateConsumables(
                    fullness = s.fullness,
                    inebriety = s.inebriety,
                    spleenUsed = s.spleenUsed + used,
                )
            }
        }

        applySpleenPrefs(itemId, count, responseText, preferences)
        return true
    }

    /** Desktop SpleenItemRequest.parseConsumption rupture branch. */
    private fun applySpleenRupturePartial(
        itemId: Int,
        spleenHit: Int,
        count: Int,
        character: KoLCharacter?,
        inventory: InventoryManager?,
    ): Boolean {
        lastUpdate = "Your spleen might go kablooie."
        if (spleenHit == 0 || character == null) return false

        val state = character.state.value
        val spleenLimit = state.spleenLimit
        val currentSpleen = state.spleenUsed
        val maxSpleen = (spleenLimit - currentSpleen) / spleenHit
        val couldSpleen = maxOf(0, minOf(count - 1, maxSpleen))
        if (couldSpleen > 0) {
            character.updateConsumables(
                fullness = state.fullness,
                inebriety = state.inebriety,
                spleenUsed = currentSpleen + couldSpleen * spleenHit,
            )
            inventory?.consumeItemLocally(itemId, couldSpleen)
        }
        val estimatedSpleen = spleenLimit - spleenHit + 1
        val after = character.state.value
        if (estimatedSpleen > after.spleenUsed) {
            character.updateConsumables(
                fullness = after.fullness,
                inebriety = after.inebriety,
                spleenUsed = estimatedSpleen,
            )
        }
        return false
    }

    /** Desktop SpleenItemRequest.parseConsumption item switch. */
    private fun applySpleenPrefs(
        itemId: Int,
        count: Int,
        responseText: String,
        preferences: Preferences?,
    ) {
        val prefs = preferences ?: return
        when (itemId) {
            STEEL_SPLEEN -> {
                if (responseText.contains("You acquire a skill")) {
                    learnSkillByName("Spleen of Steel", prefs)
                }
            }
            VOODOO_SNUFF -> prefs.setBoolean("_voodooSnuffUsed", true)
            TURKEY_BLASTER -> {
                if (responseText.contains("can't handle")) {
                    prefs.setInt("_turkeyBlastersUsed", 3)
                } else {
                    val lastAdv = prefs.getString("lastAdventure", "")
                    if (lastAdv.isNotEmpty()) {
                        adventureSpentProvider?.invoke()?.addTurns(lastAdv, 5 * count)
                    }
                    prefs.increment("_turkeyBlastersUsed", count)
                }
            }
            MANSQUITO_SERUM -> prefs.setBoolean("_mansquitoSerumUsed", true)
            AUTHORS_INK -> prefs.setBoolean("_authorsInkUsed", true)
            INQUISITORS_UNIDENTIFIABLE_OBJECT ->
                prefs.setBoolean("_inquisitorsUnidentifiableObjectUsed", true)
            HOT_JELLY -> prefs.increment("_hotJellyUses", count)
            SPOOKY_JELLY -> prefs.increment("_spookyJellyUses", count)
            STENCH_JELLY -> prefs.setBoolean("noncombatForcerActive", true)
            NIGHTMARE_FUEL -> prefs.increment("_nightmareFuelCharges", count)
            HOMEBODYL -> {
                if (responseText.contains("You pop the pill and feel an immediate desire")) {
                    prefs.increment("homebodylCharges", 11 * count)
                }
            }
            EXTROVERMECTIN -> {
                if (responseText.contains("You pop the pill and are immediately overcome")) {
                    prefs.increment("beGregariousCharges", count)
                }
            }
            BREATHITIN -> {
                if (responseText.contains("You pop the pill in your mouth")) {
                    prefs.increment("breathitinCharges", 5 * count)
                }
            }
            SCOOP_OF_PREWORKOUT_POWDER -> prefs.increment("preworkoutPowderUses", count)
            PHOSPHOR_TRACES -> prefs.increment("phosphorTracesUses", count)
            MIXED_BERRY_JELLY -> prefs.increment("mixedBerryJellyUses", count)
            LIQUID_ASSET -> prefs.increment("exerciseLiquidityCharges", count)
            INTANGIBLE_ASSET -> prefs.increment("intangibleAssetCharges", count)
            TOXIC_ASSET -> prefs.increment("toxicAssetCharges", count)
        }
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
        equipmentManager: EquipmentManager?,
        familiarManager: FamiliarManager?,
    ): Boolean {
        if (responseText.contains("too full", ignoreCase = true)) {
            if (itemId == UseItemSideEffectSync.AMINO_ACIDS) {
                preferences?.setInt("aminoAcidsUsed", 3)
            }
            lastUpdate = "Consumption limit reached."
            return false
        }

        val side = UseItemSideEffectSync.apply(
            responseText = responseText,
            itemId = itemId,
            count = count,
            preferences = preferences,
            character = character,
            inventory = inventory,
            equipmentManager = equipmentManager,
            familiarManager = familiarManager,
        )
        when (side.outcome) {
            UseItemSideEffectSync.Outcome.ABORT -> {
                lastUpdate = side.message
                return false
            }
            UseItemSideEffectSync.Outcome.KEEP,
            UseItemSideEffectSync.Outcome.CONSUME,
            -> {
                if (side.outcome == UseItemSideEffectSync.Outcome.KEEP) {
                    suppressEffectRemoval = true
                } else if (!ItemDatabase.isReusable(itemId)) {
                    inventory?.consumeItemLocally(itemId, count)
                }
                for ((extraId, qty) in side.extraConsumes) {
                    inventory?.consumeItemLocally(extraId, qty)
                }
                return true
            }
            UseItemSideEffectSync.Outcome.UNHANDLED -> Unit
        }

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
            ASTRAL_MUSHROOM -> {
                if (consumeConfirmed || looksUsed(responseText)) {
                    inventory?.consumeItemLocally(itemId, count)
                }
            }
            DANCE_CARD -> {
                if (looksUsed(responseText) || responseText.contains("dance card", ignoreCase = true)) {
                    inventory?.consumeItemLocally(itemId, count)
                    preferences?.setInt("_danceCardFightsLeft", 3)
                    if (preferences != null) {
                        TurnCounter.stopCounting(preferences, "Dance Card")
                        val run = character?.state?.value?.turnsPlayed ?: 0
                        TurnCounter.startCounting(
                            preferences,
                            run,
                            3,
                            "Dance Card loc=395",
                            "guildapp.gif",
                        )
                    }
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

    private fun applyEatPrefs(
        itemId: Int,
        count: Int,
        preferences: Preferences?,
        responseText: String = "",
        character: KoLCharacter? = null,
    ) {
        val prefs = preferences ?: return
        when (itemId) {
            AFFIRMATION_COOKIE -> {
                prefs.increment("affirmationCookiesEaten", count)
                prefs.setBoolean("_affirmationCookieEaten", true)
            }
            DEEP_DISH_OF_LEGEND -> prefs.setBoolean("deepDishOfLegendEaten", true)
            CALZONE_OF_LEGEND -> prefs.setBoolean("calzoneOfLegendEaten", true)
            PIZZA_OF_LEGEND -> prefs.setBoolean("pizzaOfLegendEaten", true)
            PIRATE_FORK -> {
                prefs.setBoolean("_pirateForkUsed", true)
                if (responseText.contains("You reach over and grab")) {
                    val food = PIRATE_FORK_PATTERN.find(responseText)?.groupValues?.getOrNull(1)
                    if (food != null) {
                        val message = "Your pirate fork grabbed some $food!"
                        sessionLogProvider?.invoke()?.appendRawLine(message)
                    }
                }
            }
            STEEL_STOMACH -> {
                if (responseText.contains("You acquire a skill")) {
                    learnSkillByName("Stomach of Steel", prefs)
                }
            }
            GHOST_PEPPER -> prefs.setInt("ghostPepperTurnsLeft", 4)
            DRIPPY_CAVIAR -> {
                prefs.setBoolean("_drippyCaviarUsed", true)
                prefs.increment("drippyJuice", 5)
            }
            DRIPPY_NUGGET -> {
                prefs.setBoolean("_drippyNuggetUsed", true)
                prefs.increment("drippyJuice", 5)
            }
            DRIPPY_PLUM -> {
                prefs.setBoolean("_drippyPlumUsed", true)
                prefs.increment("drippyJuice", 5)
            }
            EXTRA_GREASY_SLIDER -> {
                prefs.setBoolean("_extraGreasySliderEaten", true)
                if (character != null) {
                    val s = character.state.value
                    character.updateConsumables(
                        fullness = s.fullness,
                        inebriety = s.inebriety,
                        spleenUsed = (s.spleenUsed - 5 * count).coerceAtLeast(0),
                    )
                }
            }
            TIN_CUP_OF_MULLIGAN_STEW -> prefs.setBoolean("_mulliganStewEaten", true)
            SPAGHETTI_BREAKFAST -> prefs.setBoolean("_spaghettiBreakfastEaten", true)
            SMORE -> prefs.increment("smoresEaten", 1)
            KUDZU_SALAD -> prefs.setBoolean("_kudzuSaladEaten", true)
            PLUMBERS_MUSHROOM_STEW -> prefs.setBoolean("_plumbersMushroomStewEaten", true)
            MR_BURNSGER -> prefs.setBoolean("_mrBurnsgerEaten", true)
            MAGICAL_SAUSAGE -> prefs.increment("_sausagesEaten", count, max = 23)
            ELECTRIC_KOOL_AID -> prefs.increment("electricKoolAidEaten", count)
            STENCH_TOAST -> prefs.setBoolean("noncombatForcerActive", true)
            HOT_TOAST -> prefs.increment("_hotJellyUses", 1)
            SPOOKY_TOAST -> prefs.increment("_spookyJellyUses", 1)
        }
        prefs.setString("mayoInMouth", "")
    }

    private fun applyDrinkPrefs(
        itemId: Int,
        count: Int,
        preferences: Preferences?,
        responseText: String,
        inventory: InventoryManager?,
        character: KoLCharacter?,
    ) {
        val prefs = preferences ?: return
        if (prefs.getBoolean("mimeShotglassAvailable", false) &&
            !prefs.getBoolean("_mimeShotglassUsed", false) &&
            ConsumableDatabase.getInebrietyByName(ItemDatabase.getItemName(itemId)) == 1
        ) {
            prefs.setBoolean("_mimeShotglassUsed", true)
        }
        if (itemId == EVERFULL_GLASS) {
            prefs.setBoolean("_everfullGlassUsed", true)
            if (responseText.contains("You drink the liquid in the cup")) {
                val booze = EVERFULL_GLASS_PATTERN.find(responseText)?.groupValues?.getOrNull(1)
                if (booze != null) {
                    sessionLogProvider?.invoke()
                        ?.appendRawLine("Your everfull glass contained some $booze!")
                }
            }
        }
        if (prefs.getString("coolerYetiMode", "").isNotEmpty()) {
            prefs.setString("coolerYetiMode", "")
        }
        val swizzlerCount = inventory?.getCount(SWIZZLER) ?: 0
        if (swizzlerCount > 0) {
            inventory?.consumeItemLocally(SWIZZLER, minOf(count, swizzlerCount))
        }
        val limeCount = inventory?.getCount(TWIST_OF_LIME) ?: 0
        if (limeCount > 0) {
            inventory?.consumeItemLocally(TWIST_OF_LIME, minOf(count, limeCount))
        }
        val labelCount = inventory?.getCount(BLACK_LABEL) ?: 0
        if (labelCount > 0 && responseText.contains("You slap a black label on the bottle")) {
            inventory?.consumeItemLocally(BLACK_LABEL, minOf(count, labelCount))
        }
        if (responseText.contains("Some of the salt and lime")) {
            prefs.setInt(
                "cinchoSaltAndLime",
                (prefs.getInt("cinchoSaltAndLime", 0) - 1).coerceAtLeast(0),
            )
        }
        when (itemId) {
            STEEL_LIVER -> {
                if (responseText.contains("You acquire a skill")) {
                    learnSkillByName("Liver of Steel", prefs)
                }
            }
            FERMENTED_PICKLE_JUICE -> {
                prefs.setBoolean("_pickleJuiceDrunk", true)
                if (character != null) {
                    val s = character.state.value
                    character.updateConsumables(
                        fullness = s.fullness,
                        inebriety = s.inebriety,
                        spleenUsed = (s.spleenUsed - 5 * count).coerceAtLeast(0),
                    )
                }
            }
            HODGMANS_BLANKET -> prefs.setBoolean("_hodgmansBlanketDrunk", true)
            MINI_MARTINI -> prefs.increment("miniMartinisDrunk", count)
            GETS_YOU_DRUNK -> prefs.setInt("getsYouDrunkTurnsLeft", 4)
            BLOODWEISER -> prefs.increment("bloodweiserDrunk", count)
            MISS_GRAVES_VERMOUTH -> prefs.setBoolean("_missGravesVermouthDrunk", true)
            MAD_LIQUOR -> prefs.setBoolean("_madLiquorDrunk", true)
            DOC_CLOCKS_THYME_COCKTAIL -> prefs.setBoolean("_docClocksThymeCocktailDrunk", true)
            DRIPPY_PILSNER -> {
                prefs.setBoolean("_drippyPilsnerUsed", true)
                prefs.increment("drippyJuice", 5)
            }
            DRIPPY_WINE -> {
                prefs.setBoolean("_drippyWineUsed", true)
                prefs.increment("drippyJuice", 5)
            }
            VAMPIRE_VINTNER_WINE -> prefs.setInt("vintnerCharge", 0)
            PHEROMONE_COCKTAIL -> prefs.increment("markYourTerritoryCharges", count)
        }
    }

    private fun learnSkillByName(name: String, preferences: Preferences) {
        val skillId = net.sourceforge.kolmafia.data.SkillDefinitionDatabase.getByName(name)?.id
            ?: return
        net.sourceforge.kolmafia.skill.SkillLearner.learnSkill(
            skillId,
            preferences,
            skillManagerProvider?.invoke(),
        )
    }

    /** Optional DI for steel liver/stomach skill unlock. */
    var skillManagerProvider: (() -> net.sourceforge.kolmafia.skill.SkillManager?)? = null
    /** Optional session-log for everfull / pirate-fork grab messages. */
    var sessionLogProvider: (() -> net.sourceforge.kolmafia.session.SessionLogger?)? = null
    /** Optional DI for turkey-blaster adventure-spent jump-ahead. */
    var adventureSpentProvider: (() -> net.sourceforge.kolmafia.session.AdventureSpentTracker?)? = null

    const val MAGICAL_SAUSAGE = 10060
    const val STEEL_SPLEEN = 2744
    const val VOODOO_SNUFF = 3326
    const val TURKEY_BLASTER = 9166
    const val MANSQUITO_SERUM = 8817
    const val AUTHORS_INK = 8820
    const val INQUISITORS_UNIDENTIFIABLE_OBJECT = 8824
    const val HOT_JELLY = 9291
    const val SPOOKY_JELLY = 9293
    const val STENCH_JELLY = 9295
    const val NIGHTMARE_FUEL = 9925
    const val HOMEBODYL = 10828
    const val EXTROVERMECTIN = 10829
    const val BREATHITIN = 10830
    const val SCOOP_OF_PREWORKOUT_POWDER = 11862
    const val PHOSPHOR_TRACES = 11865
    const val MIXED_BERRY_JELLY = 11952
    const val TOXIC_ASSET = 12310
    const val INTANGIBLE_ASSET = 12311
    const val LIQUID_ASSET = 12312
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
    const val FORTUNE_COOKIE = 61
    const val CARTON_OF_SNAKE_MILK = 8172
    const val GHOST_PEPPER = 6468
    const val BLACK_PUDDING = 2338
    const val STEEL_STOMACH = 2742
    const val STEEL_LIVER = 2743
    const val DRIPPY_CAVIAR = 10447
    const val DRIPPY_NUGGET = 10445
    const val DRIPPY_PLUM = 10448
    const val EXTRA_GREASY_SLIDER = 3327
    const val TIN_CUP_OF_MULLIGAN_STEW = 3131
    const val SPAGHETTI_BREAKFAST = 6616
    const val SMORE = 5071
    const val KUDZU_SALAD = 8816
    const val PLUMBERS_MUSHROOM_STEW = 8819
    const val MR_BURNSGER = 8823
    const val ELECTRIC_KOOL_AID = 6483
    const val STENCH_TOAST = 9301
    const val HOT_TOAST = 9297
    const val SPOOKY_TOAST = 9299
    const val SCRATCHS_FORK = 3323
    const val FUDGE_SPORK = 5459
    const val FROSTYS_MUG = 3324
    const val ICE_STEIN = 1618
    const val ICE_COLD_SIX_PACK = 138
    const val GETS_YOU_DRUNK = 6446
    const val EVERFULL_GLASS = 9966
    const val FERMENTED_PICKLE_JUICE = 3325
    const val HODGMANS_BLANKET = 3398
    const val MINI_MARTINI = 7255
    const val BLOODWEISER = 6475
    const val MISS_GRAVES_VERMOUTH = 8818
    const val MAD_LIQUOR = 8821
    const val DOC_CLOCKS_THYME_COCKTAIL = 8822
    const val DRIPPY_PILSNER = 10525
    const val DRIPPY_WINE = 10446
    const val VAMPIRE_VINTNER_WINE = 10800
    const val PHEROMONE_COCKTAIL = 12045
    const val SWIZZLER = 6837
    const val TWIST_OF_LIME = 6417
    const val BLACK_LABEL = 7508
    const val CANDIED_SWEET_POTATOES = 9171
    const val BREAD_ROLL = 9179
    private val EVERFULL_GLASS_PATTERN =
        Regex("""Someone must have poured some of their (.*?) into it""")
    private val PIRATE_FORK_PATTERN =
        Regex("""You reach over and grab some (.*?) off of a random passerby's plate""")
    val FOLDERS: IntRange = FOLDER_01..(FOLDER_01 + 27)
    val BOOTSKINS = setOf(8937, 8938, 8939, 8940, 8941, 8942)
    val BOOTSPURS = setOf(8947, 8948, 8949, 8950, 8951, 8952, 8953)

    /** Route parseEat when ItemDatabase/ConsumableDatabase unloaded (unit tests). */
    private val KNOWN_FOOD_IDS = setOf(
        FORTUNE_COOKIE, CARTON_OF_SNAKE_MILK, GHOST_PEPPER, BLACK_PUDDING, STEEL_STOMACH,
        DRIPPY_CAVIAR, DRIPPY_NUGGET, DRIPPY_PLUM, EXTRA_GREASY_SLIDER, TIN_CUP_OF_MULLIGAN_STEW,
        SPAGHETTI_BREAKFAST, SMORE, AFFIRMATION_COOKIE, KUDZU_SALAD, PLUMBERS_MUSHROOM_STEW,
        MR_BURNSGER, MAGICAL_SAUSAGE, ELECTRIC_KOOL_AID, PIRATE_FORK, PIZZA_OF_LEGEND,
        CALZONE_OF_LEGEND, DEEP_DISH_OF_LEGEND, STENCH_TOAST, HOT_TOAST, SPOOKY_TOAST,
        CANDIED_SWEET_POTATOES, BREAD_ROLL,
    )

    /** Route parseDrink when ItemDatabase/ConsumableDatabase unloaded (unit tests). */
    private val KNOWN_DRINK_IDS = setOf(
        ICE_STEIN, GETS_YOU_DRUNK, EVERFULL_GLASS, STEEL_LIVER, FERMENTED_PICKLE_JUICE,
        HODGMANS_BLANKET, MINI_MARTINI, BLOODWEISER, MISS_GRAVES_VERMOUTH, MAD_LIQUOR,
        DOC_CLOCKS_THYME_COCKTAIL, DRIPPY_PILSNER, DRIPPY_WINE, VAMPIRE_VINTNER_WINE,
        PHEROMONE_COCKTAIL, FROSTYS_MUG,
    )

    /** Route parseSpleen when ItemDatabase/ConsumableDatabase unloaded (unit tests). */
    private val KNOWN_SPLEEN_IDS = setOf(
        STEEL_SPLEEN, VOODOO_SNUFF, TURKEY_BLASTER, MANSQUITO_SERUM, AUTHORS_INK,
        INQUISITORS_UNIDENTIFIABLE_OBJECT, HOT_JELLY, SPOOKY_JELLY, STENCH_JELLY,
        NIGHTMARE_FUEL, HOMEBODYL, EXTROVERMECTIN, BREATHITIN, SCOOP_OF_PREWORKOUT_POWDER,
        PHOSPHOR_TRACES, MIXED_BERRY_JELLY, LIQUID_ASSET, INTANGIBLE_ASSET, TOXIC_ASSET,
    )
}
