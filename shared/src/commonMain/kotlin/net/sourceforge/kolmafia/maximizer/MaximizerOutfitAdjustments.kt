package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.OutfitData

/**
 * Desktop Evaluator.enumerateEquipment outfit post-compare (Phase 371).
 * Clears automatic pins on useful outfits whose full piece set loses to best individual items.
 */
object MaximizerOutfitAdjustments {

    fun apply(
        buckets: SlotList<MaximizerRankedItem>,
        usefulOutfits: List<OutfitData>,
        spec: MaximizeSpec,
        baseState: CharacterState,
        gameDatabase: GameDatabase,
    ) {
        for (outfit in usefulOutfits) {
            adjustOutfit(buckets, outfit, spec, baseState, gameDatabase)
        }
    }

    private fun adjustOutfit(
        buckets: SlotList<MaximizerRankedItem>,
        outfit: OutfitData,
        spec: MaximizeSpec,
        baseState: CharacterState,
        gameDatabase: GameDatabase,
    ) {
        val outfitAssignment = MaximizerOutfitSlots.buildOutfitAssignment(
            outfit, buckets, spec, gameDatabase,
        ) ?: return
        val compareAssignment = buildCompareAssignment(
            buckets, outfit, spec, gameDatabase,
        ) ?: return

        val outfitScore = scorePartial(baseState, outfitAssignment, spec)
        val outfitFailed = spec.evaluator.failed
        val compareScore = scorePartial(baseState, compareAssignment, spec)

        if (outfitScore <= compareScore || outfitFailed) {
            for (pieceName in outfit.equipment) {
                MaximizerEquipmentEnumerator.setAutomaticByName(buckets, pieceName, false)
            }
        }
    }

    private fun buildCompareAssignment(
        buckets: SlotList<MaximizerRankedItem>,
        outfit: OutfitData,
        spec: MaximizeSpec,
        gameDatabase: GameDatabase,
    ): Map<EquipmentSlot, String>? {
        val compareAssignment = mutableMapOf<EquipmentSlot, String>()
        var accCompared = 0

        for (pieceName in outfit.equipment) {
            val item = gameDatabase.item(pieceName) ?: return null
            val bucket = lookupBucket(item, spec) ?: return null
            val bucketLookup = weapon1hLookupBucket(item, bucket)
            val isAccessory = item.primaryUse == net.sourceforge.kolmafia.data.ItemPrimaryUse.ACCESSORY
            val compareBucket = if (isAccessory) MaximizerSlot.ACC1 else bucketLookup
            val accessoryRankFromBest = if (isAccessory) 3 - accCompared else 1
            val compareName = pickCompareName(
                buckets = buckets,
                bucket = compareBucket,
                isAccessoryBucket = isAccessory,
                accessoryRankFromBest = accessoryRankFromBest,
                excludeNames = compareAssignment.values.toSet(),
            )
            if (compareName != null) {
                val compareEquipSlot = if (isAccessory) {
                    jumpAccessoryEquipSlot(accCompared)
                } else {
                    equipmentSlotFor(item, accCompared)
                }
                compareAssignment[compareEquipSlot] = compareName
            }
            if (isAccessory) accCompared++
        }

        return compareAssignment
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

    private fun lookupBucket(item: net.sourceforge.kolmafia.data.ItemData, spec: MaximizeSpec): MaximizerSlot? {
        val slot = primaryToMaximizerSlot(item, spec) ?: return null
        return weapon1hLookupBucket(item, slot)
    }

    private fun weapon1hLookupBucket(item: net.sourceforge.kolmafia.data.ItemData, slot: MaximizerSlot): MaximizerSlot {
        if (slot == MaximizerSlot.WEAPON &&
            item.primaryUse == net.sourceforge.kolmafia.data.ItemPrimaryUse.WEAPON &&
            net.sourceforge.kolmafia.data.EquipmentDatabase.getHands(item.id) == 1
        ) {
            return MaximizerSlot.WEAPON_1H
        }
        return slot
    }

    private fun primaryToMaximizerSlot(item: net.sourceforge.kolmafia.data.ItemData, spec: MaximizeSpec): MaximizerSlot? =
        when (item.primaryUse) {
            net.sourceforge.kolmafia.data.ItemPrimaryUse.HAT -> MaximizerSlot.HAT
            net.sourceforge.kolmafia.data.ItemPrimaryUse.WEAPON, net.sourceforge.kolmafia.data.ItemPrimaryUse.SIXGUN -> {
                if (net.sourceforge.kolmafia.data.EquipmentDatabase.getHands(item.id) == 1) {
                    MaximizerSlot.WEAPON_1H
                } else {
                    MaximizerSlot.WEAPON
                }
            }
            net.sourceforge.kolmafia.data.ItemPrimaryUse.OFFHAND -> MaximizerSlot.OFFHAND
            net.sourceforge.kolmafia.data.ItemPrimaryUse.SHIRT -> MaximizerSlot.SHIRT
            net.sourceforge.kolmafia.data.ItemPrimaryUse.PANTS -> MaximizerSlot.PANTS
            net.sourceforge.kolmafia.data.ItemPrimaryUse.ACCESSORY -> MaximizerSlot.ACC1
            net.sourceforge.kolmafia.data.ItemPrimaryUse.FAMILIAR -> MaximizerSlot.FAMILIAR
            net.sourceforge.kolmafia.data.ItemPrimaryUse.CONTAINER -> MaximizerSlot.CONTAINER
            else -> null
        }

    private fun equipmentSlotFor(item: net.sourceforge.kolmafia.data.ItemData, accCompared: Int): EquipmentSlot =
        when (item.primaryUse) {
            net.sourceforge.kolmafia.data.ItemPrimaryUse.HAT -> EquipmentSlot.HAT
            net.sourceforge.kolmafia.data.ItemPrimaryUse.WEAPON, net.sourceforge.kolmafia.data.ItemPrimaryUse.SIXGUN ->
                EquipmentSlot.WEAPON
            net.sourceforge.kolmafia.data.ItemPrimaryUse.OFFHAND -> EquipmentSlot.OFFHAND
            net.sourceforge.kolmafia.data.ItemPrimaryUse.SHIRT -> EquipmentSlot.SHIRT
            net.sourceforge.kolmafia.data.ItemPrimaryUse.PANTS -> EquipmentSlot.PANTS
            net.sourceforge.kolmafia.data.ItemPrimaryUse.ACCESSORY -> jumpAccessoryEquipSlot(accCompared)
            net.sourceforge.kolmafia.data.ItemPrimaryUse.FAMILIAR -> EquipmentSlot.FAMILIAR
            net.sourceforge.kolmafia.data.ItemPrimaryUse.CONTAINER -> EquipmentSlot.CONTAINER
            else -> EquipmentSlot.HAT
        }

    private fun jumpAccessoryEquipSlot(accCompared: Int): EquipmentSlot = when (accCompared) {
        0 -> EquipmentSlot.ACC1
        1 -> EquipmentSlot.ACC2
        else -> EquipmentSlot.ACC3
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
