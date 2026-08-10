package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase

/**
 * Desktop Evaluator.enumerateEquipment synergy post-compare (Phase 370).
 * Clears automatic pins on weak two-item synergies; reinstates MMMM triple-accessory pins.
 */
object MaximizerSynergyAdjustments {

    fun apply(
        buckets: SlotList<MaximizerRankedItem>,
        spec: MaximizeSpec,
        baseState: CharacterState,
        gameDatabase: GameDatabase,
    ) {
        adjustTwoItemSynergies(buckets, spec, baseState, gameDatabase)
        adjustTripleAccessorySynergies(buckets, spec, baseState, gameDatabase)
    }

    private fun adjustTwoItemSynergies(
        buckets: SlotList<MaximizerRankedItem>,
        spec: MaximizeSpec,
        baseState: CharacterState,
        gameDatabase: GameDatabase,
    ) {
        for (entry in ModifierDatabase.synergies()) {
            val slash = entry.name.indexOf('/')
            if (slash <= 0 || slash >= entry.name.lastIndex) continue
            val itemName1 = entry.name.substring(0, slash).trim()
            val itemName2 = entry.name.substring(slash + 1).trim()
            if (itemName1.isEmpty() || itemName2.isEmpty()) continue

            val item1 = gameDatabase.item(itemName1) ?: continue
            val item2 = gameDatabase.item(itemName2) ?: continue
            val slot1 = lookupBucket(item1, spec) ?: continue
            val slot2 = lookupBucket(item2, spec) ?: continue
            val slot1Lookup = weapon1hLookupBucket(item1, slot1) ?: continue

            if (findByName(buckets, slot1Lookup, itemName1) == null ||
                findByName(buckets, slot2, itemName2) == null
            ) {
                continue
            }

            var accCompared = 0
            val equipSlot1 = equipmentSlotFor(item1, accCompared)
            val synergyAssignment = mutableMapOf<EquipmentSlot, String>()
            synergyAssignment[equipSlot1] = item1.name

            if (item1.primaryUse == ItemPrimaryUse.ACCESSORY) accCompared++
            val equipSlot2 = equipmentSlotFor(item2, accCompared)
            synergyAssignment[equipSlot2] = item2.name

            val compareAssignment = mutableMapOf<EquipmentSlot, String>()
            val compareName1 = pickCompareName(
                buckets = buckets,
                bucket = slot1Lookup,
                isAccessoryBucket = item1.primaryUse == ItemPrimaryUse.ACCESSORY,
                accessoryRankFromBest = if (item1.primaryUse == ItemPrimaryUse.ACCESSORY) 3 else 1,
                excludeNames = emptySet(),
            )
            if (compareName1 != null) {
                compareAssignment[equipSlot1] = compareName1
            }

            val compareEquipSlot2 = equipmentSlotFor(item2, if (item1.primaryUse == ItemPrimaryUse.ACCESSORY) 1 else 0)
            val compareName2 = pickCompareName(
                buckets = buckets,
                bucket = slot2,
                isAccessoryBucket = item2.primaryUse == ItemPrimaryUse.ACCESSORY,
                accessoryRankFromBest = if (slot2 == MaximizerSlot.ACC1 && item2.primaryUse == ItemPrimaryUse.ACCESSORY) 2 else 1,
                excludeNames = compareName1?.let { setOf(it) }.orEmpty(),
            )
            if (compareName2 != null) {
                compareAssignment[compareEquipSlot2] = compareName2
            }

            val synergyScore = scorePartial(baseState, synergyAssignment, spec)
            val synergyFailed = spec.evaluator.failed
            val compareScore = scorePartial(baseState, compareAssignment, spec)

            if (synergyScore <= compareScore || synergyFailed) {
                MaximizerEquipmentEnumerator.setAutomaticByName(buckets, item1.name, false)
                MaximizerEquipmentEnumerator.setAutomaticByName(buckets, item2.name, false)
            }
        }
    }

    private fun adjustTripleAccessorySynergies(
        buckets: SlotList<MaximizerRankedItem>,
        spec: MaximizeSpec,
        baseState: CharacterState,
        gameDatabase: GameDatabase,
    ) {
        val accItems = buckets.allItems(MaximizerSlot.ACC1)
        if (accItems.isEmpty()) return

        for (itemIds in MaximizerSynergyItemIds.tripleAccessorySets) {
            val ranked = itemIds.mapNotNull { id ->
                val name = gameDatabase.item(id)?.name ?: return@mapNotNull null
                findByName(buckets, MaximizerSlot.ACC1, name)
            }
            if (ranked.size != 3) continue

            val synergyAssignment = mapOf(
                EquipmentSlot.ACC1 to ranked[0].name,
                EquipmentSlot.ACC2 to ranked[1].name,
                EquipmentSlot.ACC3 to ranked[2].name,
            )
            val compareAssignment = pickTopThreeAccessories(buckets)
            val synergyScore = scorePartial(baseState, synergyAssignment, spec)
            val synergyFailed = spec.evaluator.failed
            val compareScore = scorePartial(baseState, compareAssignment, spec)

            if (synergyScore > compareScore && !synergyFailed) {
                for (item in ranked) {
                    MaximizerEquipmentEnumerator.setAutomaticByName(buckets, item.name, true)
                }
            }
        }
    }

    private fun pickTopThreeAccessories(
        buckets: SlotList<MaximizerRankedItem>,
    ): Map<EquipmentSlot, String> {
        val assignment = mutableMapOf<EquipmentSlot, String>()
        val seen = mutableSetOf<String>()
        var slot = EquipmentSlot.ACC1
        for (item in buckets.allItems(MaximizerSlot.ACC1)) {
            if (item.name.lowercase() in seen) continue
            seen.add(item.name.lowercase())
            assignment[slot] = item.name
            slot = incrementAccessory(slot) ?: break
        }
        return assignment
    }

    private fun pickCompareName(
        buckets: SlotList<MaximizerRankedItem>,
        bucket: MaximizerSlot,
        isAccessoryBucket: Boolean,
        accessoryRankFromBest: Int,
        excludeNames: Set<String>,
    ): String? {
        val items = buckets.allItems(bucket)
        if (items.isEmpty()) return null
        val excludeLower = excludeNames.map { it.lowercase() }.toSet()
        var rank = if (isAccessoryBucket) accessoryRankFromBest else 1
        for (item in items) {
            if (item.name.lowercase() in excludeLower) continue
            rank--
            if (rank == 0) return item.name
        }
        return null
    }

    private fun findByName(
        buckets: SlotList<MaximizerRankedItem>,
        bucket: MaximizerSlot,
        name: String,
    ): MaximizerRankedItem? =
        buckets.allItems(bucket).firstOrNull { it.name.equals(name, ignoreCase = true) }

    private fun lookupBucket(item: ItemData, spec: MaximizeSpec): MaximizerSlot? {
        val slot = primaryToMaximizerSlot(item, spec) ?: return null
        return weapon1hLookupBucket(item, slot)
    }

    private fun weapon1hLookupBucket(item: ItemData, slot: MaximizerSlot): MaximizerSlot {
        if (slot == MaximizerSlot.WEAPON &&
            item.primaryUse == ItemPrimaryUse.WEAPON &&
            EquipmentDatabase.getHands(item.id) == 1
        ) {
            return MaximizerSlot.WEAPON_1H
        }
        return slot
    }

    private fun primaryToMaximizerSlot(item: ItemData, spec: MaximizeSpec): MaximizerSlot? = when (item.primaryUse) {
        ItemPrimaryUse.HAT -> MaximizerSlot.HAT
        ItemPrimaryUse.WEAPON, ItemPrimaryUse.SIXGUN -> {
            if (EquipmentDatabase.getHands(item.id) == 1) MaximizerSlot.WEAPON_1H else MaximizerSlot.WEAPON
        }
        ItemPrimaryUse.OFFHAND -> MaximizerSlot.OFFHAND
        ItemPrimaryUse.SHIRT -> MaximizerSlot.SHIRT
        ItemPrimaryUse.PANTS -> MaximizerSlot.PANTS
        ItemPrimaryUse.ACCESSORY -> MaximizerSlot.ACC1
        ItemPrimaryUse.FAMILIAR -> MaximizerSlot.FAMILIAR
        ItemPrimaryUse.CONTAINER -> MaximizerSlot.CONTAINER
        else -> null
    }

    private fun equipmentSlotFor(item: ItemData, accCompared: Int): EquipmentSlot = when (item.primaryUse) {
        ItemPrimaryUse.HAT -> EquipmentSlot.HAT
        ItemPrimaryUse.WEAPON, ItemPrimaryUse.SIXGUN -> EquipmentSlot.WEAPON
        ItemPrimaryUse.OFFHAND -> EquipmentSlot.OFFHAND
        ItemPrimaryUse.SHIRT -> EquipmentSlot.SHIRT
        ItemPrimaryUse.PANTS -> EquipmentSlot.PANTS
        ItemPrimaryUse.ACCESSORY -> jumpAccessoryEquipSlot(accCompared)
        ItemPrimaryUse.FAMILIAR -> EquipmentSlot.FAMILIAR
        ItemPrimaryUse.CONTAINER -> EquipmentSlot.CONTAINER
        else -> EquipmentSlot.HAT
    }

    private fun jumpAccessoryEquipSlot(accCompared: Int): EquipmentSlot = when (accCompared) {
        0 -> EquipmentSlot.ACC1
        1 -> EquipmentSlot.ACC2
        else -> EquipmentSlot.ACC3
    }

    private fun incrementAccessory(slot: EquipmentSlot): EquipmentSlot? = when (slot) {
        EquipmentSlot.ACC1 -> EquipmentSlot.ACC2
        EquipmentSlot.ACC2 -> EquipmentSlot.ACC3
        else -> null
    }

    private fun scorePartial(
        baseState: CharacterState,
        assignment: Map<EquipmentSlot, String>,
        spec: MaximizeSpec,
    ): Double {
        val mapped = assignment.mapValues { (_, name) -> name to 0.0 }
        return MaximizerSpeculation.scoreLoadout(baseState, mapped, spec.evaluator)
    }
}
