package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.equipment.Modeable
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Builds desktop-style ranked equipment buckets from accessible candidate item IDs.
 * Phase 365: [MaximizerCheckedItem] acquisition channels (creatable/fold/pull/buy).
 * Phase 375: per-switch-familiar carry buckets via [SlotList.getFamiliar].
 */
object MaximizerEquipmentEnumerator {

    fun enumerate(
        candidateIds: Set<Int>,
        spec: MaximizeSpec,
        gameDatabase: GameDatabase,
        checkedItem: (Int) -> MaximizerCheckedItem,
        scoreItem: (String, Evaluator) -> Double,
        itemMeetsConstraints: (String, MaximizeSpec) -> Boolean,
        priceFor: (String) -> Int = { gameDatabase.npcPrice(it) },
        autoContext: MaximizerAutoContext? = null,
        switchFamiliars: List<String> = emptyList(),
        familiarWeight: Int = 10,
        charState: CharacterState? = null,
        preferences: Preferences? = null,
    ): SlotList<MaximizerRankedItem> {
        val buckets = SlotList<MaximizerRankedItem>(switchFamiliars.size)
        val dual = spec.requireHands
        val accessible: (Int) -> Int = { id -> checkedItem(id).totalCount() }
        val gloveAvail = MaximizerWeaponGates.gloveAvailable(accessible)
        val hasRigatoni: (Int) -> Boolean = { false }
        val canChefstaff = MaximizerWeaponGates.canUseChefstaff(charState, hasRigatoni, gloveAvail)

        for (itemId in candidateIds) {
            val itemData = gameDatabase.item(itemId) ?: continue
            if (charState != null && MaximizerSubSlotItems.skipFolderHolderEnumeration(charState, itemId)) {
                continue
            }
            if (!itemData.isEquipment) continue
            val checked = checkedItem(itemId)
            if (checked.totalCount() <= 0) continue
            if (!itemMeetsConstraints(itemData.name, spec)) continue
            if (spec.evaluator.isNegEquip(itemData.name)) continue
            if (!MaximizerWeaponGates.passesWeaponConstraints(
                    itemId = itemId,
                    primaryUse = itemData.primaryUse,
                    evaluator = spec.evaluator,
                    charState = charState,
                    canChefstaff = canChefstaff,
                )
            ) {
                continue
            }
            val entry = gameDatabase.itemModifier(itemData.name)
            val itemMods = if (entry != null) {
                net.sourceforge.kolmafia.modifiers.ModifierParser.parse(entry.modifiers)
            } else {
                net.sourceforge.kolmafia.modifiers.ModifierValues.EMPTY
            }
            if (entry != null &&
                spec.evaluator.checkConstraints(itemMods) == Evaluator.Constraint.VIOLATES
            ) {
                continue
            }

            val score = scoreItem(itemData.name, spec.evaluator)
            if (!passesZeroDeltaGate(
                    score = score,
                    itemName = itemData.name,
                    checked = checked,
                    automatic = false,
                    evaluator = spec.evaluator,
                    charState = charState,
                )
            ) {
                continue
            }
            val automatic = autoContext?.shouldPinAutomatic(itemData.name, itemMods) == true ||
                Modeable.find(itemId) != null ||
                (itemId == MaximizerWeaponGates.SPECIAL_SAUCE_GLOVE && !canChefstaff)
            val conditional = itemMods.doubles.keys.any { it.name.contains("CONDITIONAL", ignoreCase = true) } ||
                entry?.modifiers?.contains("Conditional", ignoreCase = true) == true
            val single = EquipmentDatabase.isChefStaff(itemId) ||
                itemData.primaryUse == ItemPrimaryUse.ACCESSORY && checked.totalCount() <= 1
            var ranked = MaximizerRankedItem(
                itemId = itemId,
                name = itemData.name,
                score = score,
                checked = checked,
                automatic = automatic,
                conditional = conditional,
                single = single,
            )
            if (itemId == MaximizerWeaponGates.SPECIAL_SAUCE_GLOVE) {
                ranked.automatic = true
                ranked.required = charState?.characterClassEnum ==
                    net.sourceforge.kolmafia.character.CharacterClass.SAUCEROR
            }
            ranked = MaximizerGarbageAuto.pinIfGarbage(
                ranked, itemId, spec.evaluator, preferences,
            )

            when (itemData.primaryUse) {
                ItemPrimaryUse.SIXGUN -> {
                    buckets.get(MaximizerSlot.HOLSTER).add(ranked)
                    if (dual) {
                        buckets.get(MaximizerSlot.OFFHAND_RANGED).add(ranked)
                    }
                }
                ItemPrimaryUse.WEAPON -> {
                    routeWeapon(ranked, itemData, dual, buckets)
                }
                ItemPrimaryUse.OFFHAND -> {
                    if (dual || fitsOffhandSlot(itemData)) {
                        buckets.get(MaximizerSlot.OFFHAND).add(ranked)
                    }
                }
                ItemPrimaryUse.HAT -> buckets.get(MaximizerSlot.HAT).add(ranked)
                ItemPrimaryUse.SHIRT -> buckets.get(MaximizerSlot.SHIRT).add(ranked)
                ItemPrimaryUse.PANTS -> buckets.get(MaximizerSlot.PANTS).add(ranked)
                ItemPrimaryUse.ACCESSORY -> {
                    buckets.get(MaximizerSlot.ACC1).add(ranked)
                    buckets.get(MaximizerSlot.ACC2).add(ranked)
                    buckets.get(MaximizerSlot.ACC3).add(ranked)
                }
                ItemPrimaryUse.FAMILIAR -> buckets.get(MaximizerSlot.FAMILIAR).add(ranked)
                ItemPrimaryUse.CONTAINER -> buckets.get(MaximizerSlot.CONTAINER).add(ranked)
                else -> Unit
            }

            routeToFamiliarBuckets(
                ranked = ranked,
                itemData = itemData,
                spec = spec,
                buckets = buckets,
                switchFamiliars = switchFamiliars,
                familiarWeight = familiarWeight,
                gameDatabase = gameDatabase,
                automatic = automatic,
            )
        }

        sortAllBuckets(buckets, spec, priceFor)
        return buckets
    }

    private fun routeToFamiliarBuckets(
        ranked: MaximizerRankedItem,
        itemData: ItemData,
        spec: MaximizeSpec,
        buckets: SlotList<MaximizerRankedItem>,
        switchFamiliars: List<String>,
        familiarWeight: Int,
        gameDatabase: GameDatabase,
        automatic: Boolean,
    ) {
        if (switchFamiliars.isEmpty()) return
        for ((index, race) in switchFamiliars.withIndex()) {
            if (!FamiliarCarryRules.canCarryItem(race, itemData)) continue
            if (spec.requireMelee && itemData.primaryUse == ItemPrimaryUse.SIXGUN) continue
            if (spec.requireHands && itemData.primaryUse != ItemPrimaryUse.OFFHAND &&
                race == FamiliarCarryRules.LEFT_HAND_RACE
            ) continue
            val famScore = FamiliarCarriedScoring.score(
                race = race,
                itemName = itemData.name,
                modifier = spec.primary,
                gameDatabase = gameDatabase,
                familiarWeight = familiarWeight,
            )
            if (famScore <= 0.0 && !automatic) continue
            buckets.getFamiliar(index).add(
                ranked.copy(score = famScore, automatic = automatic || ranked.automatic),
            )
        }
    }

    private fun routeWeapon(
        ranked: MaximizerRankedItem,
        itemData: ItemData,
        dualWield: Boolean,
        buckets: SlotList<MaximizerRankedItem>,
    ) {
        val hands = EquipmentDatabase.getHands(itemData.id)
        when {
            hands == 1 -> {
                buckets.get(MaximizerSlot.WEAPON_1H).add(ranked)
                if (dualWield) {
                    val aux = if (itemData.primaryUse == ItemPrimaryUse.SIXGUN) {
                        MaximizerSlot.OFFHAND_RANGED
                    } else {
                        MaximizerSlot.OFFHAND_MELEE
                    }
                    buckets.get(aux).add(ranked)
                }
            }
            hands >= 2 -> buckets.get(MaximizerSlot.WEAPON).add(ranked)
            else -> buckets.get(MaximizerSlot.WEAPON).add(ranked)
        }
    }

    private fun fitsOffhandSlot(item: ItemData): Boolean =
        item.primaryUse == ItemPrimaryUse.OFFHAND

    private fun sortAllBuckets(
        buckets: SlotList<MaximizerRankedItem>,
        spec: MaximizeSpec,
        priceFor: (String) -> Int,
    ) {
        val comparator = rankedComparator(spec, priceFor)
        for ((slot, _) in buckets.slotEntries()) {
            val sorted = buckets.get(slot).sortedWith(comparator)
            buckets.set(slot, sorted)
        }
        for (index in 0 until buckets.familiarCount()) {
            val sorted = buckets.getFamiliar(index).sortedWith(comparator)
            buckets.setFamiliar(index, sorted)
        }
    }

    private fun rankedComparator(
        spec: MaximizeSpec,
        priceFor: (String) -> Int,
    ): Comparator<MaximizerRankedItem> = when {
        spec.maxPrice != null -> compareBy<MaximizerRankedItem> { priceFor(it.name) }
            .thenByDescending { it.score }
        spec.minPrice != null -> compareByDescending<MaximizerRankedItem> { priceFor(it.name) }
            .thenByDescending { it.score }
        else -> compareByDescending { it.score }
    }

    /** Merge ranked items from multiple maximizer buckets into name/score pairs. */
    fun mergeBuckets(
        buckets: SlotList<MaximizerRankedItem>,
        slots: List<MaximizerSlot>,
        usedElsewhere: Set<String> = emptySet(),
        limit: Int = Int.MAX_VALUE,
    ): List<Pair<String, Double>> {
        val seen = mutableSetOf<String>()
        val merged = mutableListOf<MaximizerRankedItem>()
        for (slot in slots) {
            for (item in buckets.allItems(slot)) {
                if (item.name in usedElsewhere || item.name in seen) continue
                seen.add(item.name)
                merged.add(item)
            }
        }
        val pinned = merged.filter { it.automatic || it.required }
        val scored = merged.filterNot { it.automatic || it.required }.take(limit)
        return (pinned + scored).map { it.name to it.score }
    }

    fun mergeFamiliarBucket(
        buckets: SlotList<MaximizerRankedItem>,
        familiarIndex: Int,
        usedElsewhere: Set<String> = emptySet(),
        limit: Int = Int.MAX_VALUE,
    ): List<Pair<String, Double>> {
        if (familiarIndex !in 0 until buckets.familiarCount()) return emptyList()
        val pinned = mutableListOf<MaximizerRankedItem>()
        val scored = mutableListOf<MaximizerRankedItem>()
        for (item in buckets.getFamiliar(familiarIndex)) {
            if (item.name in usedElsewhere) continue
            if (item.automatic || item.required) pinned.add(item) else scored.add(item)
        }
        return (pinned + scored.take(limit)).map { it.name to it.score }
    }

    private fun isCurrentlyEquipped(name: String, charState: CharacterState?): Boolean =
        charState?.equipment?.values?.any { it.equals(name, ignoreCase = true) } == true

    /** Desktop enumerateEquipment zero-delta filter (Phase 382). */
    internal fun passesZeroDeltaGate(
        score: Double,
        itemName: String,
        checked: MaximizerCheckedItem,
        automatic: Boolean,
        evaluator: Evaluator,
        charState: CharacterState?,
    ): Boolean {
        if (score < 0.0) return false
        if (score > 0.0) return true
        val equipped = isCurrentlyEquipped(itemName, charState)
        if (equipped) return evaluator.considerCurrent()
        if (checked.initial == 0 && !automatic) return false
        return !automatic
    }

    /** Update [MaximizerRankedItem.automatic] for every bucket entry matching [name]. */
    fun setAutomaticByName(
        buckets: SlotList<MaximizerRankedItem>,
        name: String,
        automatic: Boolean,
    ) {
        for ((slot, _) in buckets.slotEntries()) {
            for (item in buckets.get(slot)) {
                if (item.name.equals(name, ignoreCase = true)) {
                    item.automatic = automatic
                }
            }
        }
        for (index in 0 until buckets.familiarCount()) {
            for (item in buckets.getFamiliar(index)) {
                if (item.name.equals(name, ignoreCase = true)) {
                    item.automatic = automatic
                }
            }
        }
    }

    fun allRankedItems(buckets: SlotList<MaximizerRankedItem>): List<MaximizerRankedItem> {
        val seen = mutableSetOf<String>()
        val merged = mutableListOf<MaximizerRankedItem>()
        for ((_, items) in buckets.slotEntries()) {
            for (item in items) {
                if (item.name in seen) continue
                seen.add(item.name)
                merged.add(item)
            }
        }
        return merged
    }

    /** Map ranked buckets to per-[EquipmentSlot] candidate lists for speculation DFS. */
    fun toCandidatesByEquipmentSlot(
        buckets: SlotList<MaximizerRankedItem>,
        spec: MaximizeSpec,
        usedElsewhere: Set<String>,
        perSlotLimit: Int,
        gameDatabase: GameDatabase,
        scoreItem: (String, Evaluator) -> Double,
        itemMeetsConstraints: (String, MaximizeSpec) -> Boolean,
        priceFor: (String) -> Int,
        familiarCarryRaces: List<String> = emptyList(),
        familiarCarryScorer: ((String, net.sourceforge.kolmafia.modifiers.DoubleModifier) -> Double)? = null,
        familiarBucketIndex: Int? = null,
    ): Map<EquipmentSlot, List<Pair<String, Double>>> {
        val result = mutableMapOf<EquipmentSlot, List<Pair<String, Double>>>()
        for (equipSlot in MaximizerSpeculation.searchSlots) {
            val maximizerSlots = when (equipSlot) {
                EquipmentSlot.WEAPON -> MaximizerSlot.weaponSearchSlots()
                EquipmentSlot.OFFHAND -> MaximizerSlot.offhandBuckets(spec)
                EquipmentSlot.ACC1, EquipmentSlot.ACC2, EquipmentSlot.ACC3 ->
                    listOf(MaximizerSlot.ACC1)
                else -> MaximizerSlot.fromEquipmentSlot(equipSlot)?.let { listOf(it) }.orEmpty()
            }
            var ranked = if (equipSlot == EquipmentSlot.FAMILIAR && familiarBucketIndex != null) {
                mergeFamiliarBucket(buckets, familiarBucketIndex, usedElsewhere, perSlotLimit)
            } else {
                mergeBuckets(buckets, maximizerSlots, usedElsewhere, perSlotLimit)
            }
            if (equipSlot == EquipmentSlot.FAMILIAR &&
                familiarBucketIndex == null &&
                familiarCarryRaces.isNotEmpty()
            ) {
                val extra = mutableListOf<Pair<String, Double>>()
                for (race in familiarCarryRaces) {
                    for (item in allRankedItems(buckets)) {
                        if (item.name in usedElsewhere) continue
                        val itemData = gameDatabase.item(item.itemId) ?: continue
                        if (!FamiliarCarryRules.canCarryItem(race, itemData)) continue
                        if (spec.requireMelee && itemData.primaryUse == ItemPrimaryUse.SIXGUN) continue
                        if (spec.requireHands && itemData.primaryUse != ItemPrimaryUse.OFFHAND &&
                            race == FamiliarCarryRules.LEFT_HAND_RACE
                        ) continue
                        if (!itemMeetsConstraints(item.name, spec)) continue
                        val score = if (familiarCarryScorer != null) {
                            familiarCarryScorer(item.name, spec.primary)
                        } else {
                            scoreItem(item.name, spec.evaluator)
                        }
                        extra.add(item.name to score)
                    }
                }
                ranked = (ranked + extra).distinctBy { it.first }
                    .sortedByDescending { it.second }
                    .take(perSlotLimit)
            }
            if (equipSlot == EquipmentSlot.OFFHAND && spec.requireHands) {
                ranked = ranked.filter { (name, _) ->
                    val item = gameDatabase.item(name)
                    item?.primaryUse == ItemPrimaryUse.OFFHAND ||
                        item?.let { EquipmentDatabase.getHands(it.id) == 1 } == true
                }
            }
            result[equipSlot] = ranked
        }
        return result
    }

    fun fitsEquipmentSlot(item: ItemData, slot: EquipmentSlot, spec: MaximizeSpec): Boolean {
        val maximizerSlot = MaximizerSlot.fromEquipmentSlot(slot) ?: return false
        return when (slot) {
            EquipmentSlot.WEAPON -> {
                if (item.primaryUse !in setOf(ItemPrimaryUse.WEAPON)) return false
                if (spec.requireMelee && item.primaryUse == ItemPrimaryUse.SIXGUN) return false
                val hands = EquipmentDatabase.getHands(item.id)
                when {
                    spec.requireHands -> hands == 1
                    hands == 1 -> false
                    else -> true
                }
            }
            EquipmentSlot.OFFHAND -> {
                if (spec.requireHands) {
                    item.primaryUse == ItemPrimaryUse.OFFHAND ||
                        (item.primaryUse in setOf(ItemPrimaryUse.WEAPON, ItemPrimaryUse.SIXGUN) &&
                            EquipmentDatabase.getHands(item.id) == 1 &&
                            !(spec.requireMelee && item.primaryUse == ItemPrimaryUse.SIXGUN))
                } else {
                    item.primaryUse == ItemPrimaryUse.OFFHAND
                }
            }
            EquipmentSlot.ACC1, EquipmentSlot.ACC2, EquipmentSlot.ACC3 ->
                item.primaryUse == ItemPrimaryUse.ACCESSORY
            EquipmentSlot.HOLSTER ->
                item.primaryUse == ItemPrimaryUse.SIXGUN
            EquipmentSlot.CODPIECE1, EquipmentSlot.CODPIECE2, EquipmentSlot.CODPIECE3,
            EquipmentSlot.CODPIECE4, EquipmentSlot.CODPIECE5,
            -> ModifierDatabase.isCodpieceGem(item.id)
            else -> when (maximizerSlot) {
                MaximizerSlot.HAT -> item.primaryUse == ItemPrimaryUse.HAT
                MaximizerSlot.SHIRT -> item.primaryUse == ItemPrimaryUse.SHIRT
                MaximizerSlot.PANTS -> item.primaryUse == ItemPrimaryUse.PANTS
                MaximizerSlot.FAMILIAR -> item.primaryUse == ItemPrimaryUse.FAMILIAR
                MaximizerSlot.CONTAINER -> item.primaryUse == ItemPrimaryUse.CONTAINER
                else -> false
            }
        }
    }
}
