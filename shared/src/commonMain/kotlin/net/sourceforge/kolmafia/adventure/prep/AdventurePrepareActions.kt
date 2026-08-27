package net.sourceforge.kolmafia.adventure.prep

import net.sourceforge.kolmafia.data.AdventureZone
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.equipment.OutfitManager
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.request.UseItemRequest

/**
 * Desktop [KoLAdventure.prepareForAdventure] high-traffic action clusters
 * (Phases 1896–1910 + residual 1941–1955). Pref-driven zoneItem/zoneUse still
 * handled by the hub.
 */
object AdventurePrepareActions {

    data class PrepareDeps(
        val outfitManager: OutfitManager?,
        val retrieveItemService: RetrieveItemService?,
        val useItemRequest: UseItemRequest?,
        val gameDatabase: GameDatabase?,
        /** Optional HTTP visit (e.g. tavern barkeep / PlaceRequest unlocks). */
        val visitUrl: (suspend (String) -> Boolean)? = null,
        /** Equip a single item id when possible (pirate fledges, Spacegate gear). */
        val equipItem: (suspend (Int) -> Boolean)? = null,
        /** Unequip a slot api key (e.g. offhand for cola uniforms). */
        val unequipSlot: (suspend (String) -> Boolean)? = null,
        /** Active effect name check for consumable-entry zones. */
        val hasEffect: (String) -> Boolean = { false },
        /** Desktop [KoLCharacter.getElementalResistanceLevels] for STENCH (Guano Junction). Null = skip. */
        val stenchResistanceLevels: (() -> Int)? = null,
        /** Prefer Machine Elf familiar for Deep Machine Tunnels when effect absent. */
        val preferFamiliar: (suspend (String) -> Boolean)? = null,
    )

    /**
     * Returns false only on hard failure (e.g. required outfit wear failed).
     * Missing optional prep items is not a failure — automation continues.
     */
    suspend fun prepare(
        locationName: String,
        zone: AdventureZone?,
        ctx: AdventureGateContext,
        deps: PrepareDeps,
    ): Boolean {
        // Built-in + expanded outfit map (hub may already have worn via prefs)
        if (deps.outfitManager != null) {
            val outfit = resolveOutfit(locationName, zone)
            if (outfit != null) {
                if (!deps.outfitManager.wearOutfit(outfit)) return false
            }
        }

        // Tavern cellar — talk to barkeep to open
        if (needsCellarBarkeep(locationName, zone, ctx)) {
            deps.visitUrl?.invoke("tavern.php?place=barkeep")
        }

        AdventurePrepareVisits.prepareUnlockVisits(locationName, zone?.zoneName, ctx, deps)

        when {
            needsSkeletonOpener(locationName, ctx) ->
                useIfPossible(ItemIds.BONE_WITH_A_PRICE_TAG, deps)
            needsMadnessOpener(locationName, ctx) ->
                useIfPossible(ItemIds.HYPNOTIC_BREADCRUMBS, deps)
            needsLotOpener(locationName, ctx) ->
                useIfPossible(ItemIds.BOOZE_MAP, deps)
            else -> AdventurePrepareVisits.prepareMarketNpcFallbacks(locationName, ctx, deps)
        }

        prepareConsumableEntry(locationName, zone, ctx, deps)

        if (locationName.contains("Guano Junction", ignoreCase = true)) {
            val levels = deps.stenchResistanceLevels?.invoke()
            if (levels != null && levels < 1) return false
        }

        when {
            needsSonars(locationName, zone) -> {
                val qty = sonarsToUse(locationName, ctx)
                if (qty > 0) useIfPossible(ItemIds.SONAR, deps, qty = qty)
            }
            needsKnobPerfume(locationName, zone) ->
                useIfPossible(ItemIds.KNOB_GOBLIN_PERFUME, deps)
            needsKnobCake(locationName, zone) ->
                useIfPossible(ItemIds.KNOB_CAKE, deps)
            needsAstralMushroom(locationName, zone) -> {
                useIfPossible(ItemIds.ASTRAL_MUSHROOM, deps)
                selectAstralTrip(locationName, ctx)
            }
            needsGong(locationName, zone) -> {
                ctx.preferences?.setInt("choiceAdventure276", 2)
                useIfPossible(ItemIds.GONG, deps)
            }
            needsEnchantedBean(locationName, zone) ->
                useIfPossible(ItemIds.ENCHANTED_BEAN, deps)
            needsFilthwormGland(locationName) != null -> {
                val gland = needsFilthwormGland(locationName)!!
                useIfPossible(gland, deps)
            }
            needsCasino(locationName, zone) ->
                deps.retrieveItemService?.retrieve(ItemIds.CASINO_PASS, 1)
            needsAbyss(locationName, zone) ->
                deps.retrieveItemService?.retrieve(ItemIds.BLACK_GLASS, 1)
            needsFantasyRealm(locationName, zone) -> {
                deps.retrieveItemService?.retrieve(ItemIds.FANTASY_REALM_GEM, 1)
                if (ctx.hasItem(ItemIds.FANTASY_REALM_GEM) &&
                    !ctx.hasEquipped(ItemIds.FANTASY_REALM_GEM)
                ) {
                    deps.equipItem?.invoke(ItemIds.FANTASY_REALM_GEM)
                }
            }
            needsDrip(locationName, zone) -> {
                deps.retrieveItemService?.retrieve(ItemIds.DRIP_HARNESS, 1)
                if (ctx.hasItem(ItemIds.DRIP_HARNESS) && !ctx.hasEquipped(ItemIds.DRIP_HARNESS)) {
                    deps.equipItem?.invoke(ItemIds.DRIP_HARNESS)
                }
            }
            needsMemories(locationName, zone) ->
                deps.retrieveItemService?.retrieve(ItemIds.EMPTY_AGUA_DE_VIDA_BOTTLE, 1)
            needsPortal(locationName, zone) -> {
                if (ctx.hasItem(ItemIds.TRAPEZOID)) {
                    useIfPossible(ItemIds.TRAPEZOID, deps)
                }
            }
            needsTransfunctioner(locationName, zone) && !ctx.hasItem(ItemIds.TRANSFUNCTIONER) -> {
                if (!AdventureUnlockHelpers.woodsOpen(ctx)) return false
            }
        }

        if (needsIslandAccess(locationName, zone) || needsPirateGear(locationName, zone)) {
            AdventurePrepareVisits.buildDinghy(ctx, deps)
        }

        if (needsPirateGear(locationName, zone)) {
            if (ctx.hasItem(ItemIds.PIRATE_FLEDGES) && !ctx.hasEquipped(ItemIds.PIRATE_FLEDGES)) {
                deps.equipItem?.invoke(ItemIds.PIRATE_FLEDGES)
            }
        }

        if (needsRiftColaUnequip(locationName, zone)) {
            deps.unequipSlot?.invoke("offhand")
        }

        if (zone?.zoneName?.contains("Spacegate", ignoreCase = true) == true) {
            prepareSpacegateGear(ctx, deps)
        }

        if (needsIslandAccess(locationName, zone) &&
            !AdventureUnlockHelpers.islandAccessible(ctx) &&
            !ctx.hasItem(ItemIds.DINGHY_PLANS) &&
            !ctx.hasItem(ItemIds.DINGY_DINGHY)
        ) {
            return false
        }

        return true
    }

    private suspend fun prepareConsumableEntry(
        locationName: String,
        zone: AdventureZone?,
        @Suppress("UNUSED_PARAMETER") ctx: AdventureGateContext,
        deps: PrepareDeps,
    ) {
        val z = zone?.zoneName.orEmpty()
        when {
            z.contains("Rabbit Hole", ignoreCase = true) ||
                locationName.contains("Rabbit Hole", ignoreCase = true) -> {
                if (!deps.hasEffect("Down the Rabbit Hole")) {
                    useIfPossible(ItemIds.DRINK_ME_POTION, deps)
                }
            }
            z.contains("Suburb", ignoreCase = true) -> {
                if (!deps.hasEffect("Dis Abled")) {
                    useIfPossible(ItemIds.DEVILISH_FOLIO, deps)
                }
            }
            z.contains("Wormwood", ignoreCase = true) -> {
                if (!deps.hasEffect("Absinthe-Minded")) {
                    useIfPossible(ItemIds.ABSINTHE, deps)
                }
            }
            z.contains("Spaaace", ignoreCase = true) -> {
                if (!deps.hasEffect("Transpondent")) {
                    useIfPossible(ItemIds.TRANSPONDER, deps)
                }
            }
            z.contains("Deep Machine", ignoreCase = true) ||
                locationName.contains("Deep Machine Tunnels", ignoreCase = true) -> {
                if (!deps.hasEffect("Inside the Snowglobe")) {
                    val switched = deps.preferFamiliar?.invoke("Machine Elf") == true
                    if (!switched) {
                        useIfPossible(ItemIds.MACHINE_SNOWGLOBE, deps)
                    }
                }
            }
        }
    }

    private fun needsCasino(locationName: String, zone: AdventureZone?): Boolean =
        zone?.zoneName?.equals("Casino", ignoreCase = true) == true ||
            locationName.contains("Casino", ignoreCase = true)

    private fun needsAbyss(locationName: String, zone: AdventureZone?): Boolean =
        locationName.contains("Caliginous Abyss", ignoreCase = true) ||
            zone?.zoneName?.contains("Abyss", ignoreCase = true) == true

    private fun needsFantasyRealm(locationName: String, zone: AdventureZone?): Boolean =
        zone?.zoneName?.contains("FantasyRealm", ignoreCase = true) == true ||
            locationName.contains("FantasyRealm", ignoreCase = true)

    private fun needsDrip(locationName: String, zone: AdventureZone?): Boolean =
        zone?.zoneName?.equals("The Drip", ignoreCase = true) == true ||
            locationName.contains("The Drip", ignoreCase = true)

    private fun needsMemories(locationName: String, zone: AdventureZone?): Boolean =
        zone?.zoneName?.equals("Memories", ignoreCase = true) == true ||
            locationName.contains("Memory of", ignoreCase = true)

    private fun needsPortal(locationName: String, zone: AdventureZone?): Boolean =
        zone?.zoneName?.equals("Portal", ignoreCase = true) == true ||
            locationName.contains("El Vibrato", ignoreCase = true)

    private fun needsRiftColaUnequip(locationName: String, zone: AdventureZone?): Boolean =
        (zone?.zoneName?.equals("Rift", ignoreCase = true) == true ||
            locationName.contains("Battlefield", ignoreCase = true)) &&
            locationName.contains("No Uniform", ignoreCase = true)

    fun resolveOutfit(locationName: String, zone: AdventureZone?): String? {
        ZONE_OUTFITS[locationName]?.let { return it }
        ZONE_OUTFITS.entries.firstOrNull { (key, _) ->
            locationName.equals(key, ignoreCase = true) ||
                locationName.contains(key, ignoreCase = true)
        }?.value?.let { return it }

        return when {
            locationName.contains("Cobb's Knob", ignoreCase = true) &&
                (locationName.contains("Harem", ignoreCase = true) ||
                    locationName.contains("Treasury", ignoreCase = true)) ->
                "Knob Goblin Harem Girl Disguise"
            locationName.contains("Cobb's Knob Menagerie", ignoreCase = true) ->
                "Knob Goblin Elite Guard Uniform"
            locationName.contains("Orcish Frat House", ignoreCase = true) ||
                locationName.contains("Frat House", ignoreCase = true) ->
                "Frat Boy Ensemble"
            locationName.contains("Hippy Camp", ignoreCase = true) &&
                !locationName.contains("Wartime", ignoreCase = true) ->
                "Filthy Hippy Disguise"
            locationName.contains("Battlefield", ignoreCase = true) &&
                locationName.contains("Frat", ignoreCase = true) ->
                "Frat Warrior Fatigues"
            locationName.contains("Battlefield", ignoreCase = true) &&
                locationName.contains("Hippy", ignoreCase = true) ->
                "War Hippy Fatigues"
            needsPirateGear(locationName, zone) ->
                "Swashbuckling Getup"
            zone?.zoneName?.contains("Spacegate", ignoreCase = true) == true ->
                null
            else -> null
        }
    }

    /** Desktop BatHole sonar quantity from quest step vs location. */
    fun sonarsToUse(locationName: String, ctx: AdventureGateContext): Int {
        val needed = when {
            locationName.contains("Boss Bat", ignoreCase = true) -> 3
            locationName.contains("Beanbat", ignoreCase = true) -> 2
            locationName.contains("Batrat", ignoreCase = true) ||
                locationName.contains("Ratbat", ignoreCase = true) -> 1
            else -> 0
        }
        if (needed == 0) return 0 // Guano Junction — stench gate, not sonars
        val used = when {
            ctx.isLaterThan(Quest.BAT, "step2") || ctx.isFinished(Quest.BAT) -> 3
            ctx.isAtLeast(Quest.BAT, "step2") -> 2
            ctx.isAtLeast(Quest.BAT, "step1") -> 1
            else -> 0
        }
        return (needed - used).coerceAtLeast(0)
    }

    fun needsFilthwormGland(locationName: String): Int? = when {
        locationName.contains("Filthworm Feeding Grounds", ignoreCase = true) ||
            locationName.contains("feeding chamber", ignoreCase = true) ->
            ItemIds.FILTHWORM_HATCHLING_GLAND
        locationName.contains("Filthworm Royal Guard Chamber", ignoreCase = true) ||
            locationName.contains("royal guard chamber", ignoreCase = true) ->
            ItemIds.FILTHWORM_DRONE_GLAND
        locationName.contains("Filthworm Queen's Chamber", ignoreCase = true) ||
            locationName.contains("queen", ignoreCase = true) &&
            locationName.contains("filthworm", ignoreCase = true) ->
            ItemIds.FILTHWORM_GUARD_GLAND
        else -> null
    }

    private suspend fun prepareSpacegateGear(ctx: AdventureGateContext, deps: PrepareDeps) {
        val gear = ctx.prefString("_spacegateGear")
        if (gear.isBlank()) {
            deps.visitUrl?.invoke("adventure.php?snarfblat=494")
            return
        }
        for (raw in gear.split('|')) {
            val name = raw.trim()
            if (name.isEmpty()) continue
            val id = deps.gameDatabase?.item(name)?.id ?: continue
            deps.retrieveItemService?.retrieve(id, 1)
            if (!ctx.hasEquipped(id)) {
                deps.equipItem?.invoke(id)
            }
        }
    }

    private fun selectAstralTrip(locationName: String, ctx: AdventureGateContext) {
        if (ctx.prefString("currentAstralTrip").isNotEmpty()) return
        val option = when {
            locationName.contains("Bad Trip", ignoreCase = true) -> 1
            locationName.contains("Mediocre Trip", ignoreCase = true) -> 2
            locationName.contains("Great Trip", ignoreCase = true) -> 3
            else -> return
        }
        ctx.preferences?.setInt("choiceAdventure71", option)
    }

    private suspend fun useIfPossible(itemId: Int, deps: PrepareDeps, qty: Int = 1): Boolean {
        val retrieve = deps.retrieveItemService
        val use = deps.useItemRequest
        if (retrieve != null) {
            if (retrieve.retrieve(itemId, qty) < qty) return true // soft fail
        }
        if (use != null) {
            use.use(itemId, qty)
        }
        return true
    }

    private fun needsCellarBarkeep(
        locationName: String,
        zone: AdventureZone?,
        ctx: AdventureGateContext,
    ): Boolean {
        val cellar = locationName.contains("Tavern Cellar", ignoreCase = true) ||
            zone?.urlParams?.contains("cellar.php", ignoreCase = true) == true
        if (!cellar) return false
        return !ctx.isLaterThan(Quest.RAT, QuestDatabase.STARTED)
    }

    private fun needsSkeletonOpener(locationName: String, ctx: AdventureGateContext): Boolean =
        locationName.contains("Skeleton Store", ignoreCase = true) &&
            !ctx.prefBool("skeletonStoreAvailable") &&
            ctx.hasItem(ItemIds.BONE_WITH_A_PRICE_TAG)

    private fun needsMadnessOpener(locationName: String, ctx: AdventureGateContext): Boolean =
        locationName.contains("Madness Bakery", ignoreCase = true) &&
            !ctx.prefBool("madnessBakeryAvailable") &&
            ctx.hasItem(ItemIds.HYPNOTIC_BREADCRUMBS)

    private fun needsLotOpener(locationName: String, ctx: AdventureGateContext): Boolean =
        locationName.contains("Overgrown Lot", ignoreCase = true) &&
            !ctx.prefBool("overgrownLotAvailable") &&
            ctx.hasItem(ItemIds.BOOZE_MAP)

    private fun needsSonars(locationName: String, zone: AdventureZone?): Boolean =
        zone?.zoneName?.contains("Bat", ignoreCase = true) == true ||
            locationName.contains("Guano Junction", ignoreCase = true) ||
            locationName.contains("Batrat", ignoreCase = true) ||
            locationName.contains("Beanbat", ignoreCase = true) ||
            locationName.contains("Boss Bat", ignoreCase = true)

    private fun needsKnobPerfume(locationName: String, zone: AdventureZone?): Boolean =
        locationName.contains("Harem", ignoreCase = true) ||
            (zone?.zoneName?.contains("Knob", ignoreCase = true) == true &&
                locationName.contains("Treasury", ignoreCase = true))

    private fun needsKnobCake(locationName: String, zone: AdventureZone?): Boolean =
        locationName.contains("Throne Room", ignoreCase = true) &&
            (zone?.zoneName?.contains("Knob", ignoreCase = true) == true ||
                locationName.contains("Knob", ignoreCase = true))

    private fun needsAstralMushroom(locationName: String, zone: AdventureZone?): Boolean =
        zone?.zoneName?.equals("Astral", ignoreCase = true) == true ||
            locationName.contains("Astral", ignoreCase = true)

    private fun needsGong(locationName: String, zone: AdventureZone?): Boolean =
        locationName.contains("Molehill", ignoreCase = true) ||
            zone?.zoneName?.contains("Mole", ignoreCase = true) == true

    private fun needsEnchantedBean(locationName: String, zone: AdventureZone?): Boolean =
        locationName.contains("Beanstalk", ignoreCase = true) ||
            zone?.zoneName?.contains("Beanstalk", ignoreCase = true) == true

    private fun needsTransfunctioner(locationName: String, zone: AdventureZone?): Boolean =
        locationName.contains("8-Bit", ignoreCase = true) ||
            zone?.zoneName?.contains("8-Bit", ignoreCase = true) == true

    private fun needsPirateGear(locationName: String, zone: AdventureZone?): Boolean =
        zone?.zoneName?.equals("Pirate", ignoreCase = true) == true ||
            locationName.contains("Poop Deck", ignoreCase = true) ||
            locationName.contains("Below Deck", ignoreCase = true) ||
            (locationName.contains("Pirate", ignoreCase = true) &&
                !locationName.contains("PirateRealm", ignoreCase = true))

    private fun needsIslandAccess(locationName: String, zone: AdventureZone?): Boolean =
        zone?.zoneName?.contains("Island", ignoreCase = true) == true ||
            zone?.zoneName?.contains("IsleWar", ignoreCase = true) == true ||
            locationName.contains("Mysterious Island", ignoreCase = true)

    val ZONE_OUTFITS: Map<String, String> = mapOf(
        "The Mine Office" to "Mining Gear",
        "Dwarf Factory" to "Mining Gear",
        "The Frat House (Disguised)" to "Frat Boy Ensemble",
        "The Hippy Camp (Disguised)" to "Filthy Hippy Disguise",
        "The Wartime Frat House (Disguised)" to "War Hippy Fatigues",
        "The Wartime Hippy Camp (Disguised)" to "Frat Warrior Fatigues",
        "The Battlefield (Cloaca-Cola)" to "Cloaca-Cola Uniform",
        "The Battlefield (Dyspepsi-Cola)" to "Dyspepsi-Cola Uniform",
        "Mer-kin Elementary School" to "Crappy Mer-Kin Disguise",
        "Mer-kin Gymnasium" to "Crappy Mer-Kin Disguise",
        "Mer-kin Library" to "Mer-kin Scholar's Vestments",
        "Mer-kin Colosseum" to "Mer-kin Gladiatorial Gear",
        "Mer-kin Study" to "Mer-kin Scholar's Vestments",
        "Mer-kin Deep Temple" to "Crappy Mer-Kin Disguise",
        "The Poop Deck" to "Swashbuckling Getup",
        "Below Deck" to "Swashbuckling Getup",
        "The Orcish Frat House" to "Frat Boy Ensemble",
        "Cobb's Knob Harem" to "Knob Goblin Harem Girl Disguise",
        "The Haunted Ballroom" to "Antique Arms and Armor",
    )
}
