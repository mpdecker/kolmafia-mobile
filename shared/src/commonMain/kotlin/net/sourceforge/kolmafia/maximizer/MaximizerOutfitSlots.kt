package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.OutfitData

/** Shared outfit piece → equipment slot mapping (Phase 371/372). */
internal object MaximizerOutfitSlots {

    fun isPieceAutomatic(
        buckets: SlotList<MaximizerRankedItem>,
        pieceName: String,
    ): Boolean = buckets.slotEntries().any { (_, items) ->
        items.any { it.name.equals(pieceName, ignoreCase = true) && it.automatic }
    }

    fun survivingUsefulOutfits(
        buckets: SlotList<MaximizerRankedItem>,
        usefulOutfits: List<OutfitData>,
    ): List<OutfitData> = usefulOutfits.filter { outfit ->
        outfit.equipment.isNotEmpty() &&
            outfit.equipment.all { isPieceAutomatic(buckets, it) }
    }

    fun buildOutfitAssignment(
        outfit: OutfitData,
        buckets: SlotList<MaximizerRankedItem>,
        spec: MaximizeSpec,
        gameDatabase: GameDatabase,
    ): Map<EquipmentSlot, String>? {
        val assignment = mutableMapOf<EquipmentSlot, String>()
        var accCompared = 0

        for (pieceName in outfit.equipment) {
            val item = gameDatabase.item(pieceName) ?: return null
            val bucket = lookupBucket(item, spec) ?: return null
            val bucketLookup = weapon1hLookupBucket(item, bucket)
            if (findByName(buckets, bucketLookup, pieceName) == null &&
                (item.primaryUse != ItemPrimaryUse.ACCESSORY ||
                    findByName(buckets, MaximizerSlot.ACC1, pieceName) == null)
            ) {
                return null
            }

            val equipSlot = equipmentSlotFor(item, accCompared)
            val existing = assignment[equipSlot]
            if (existing != null && !existing.equals(item.name, ignoreCase = true)) {
                return null
            }
            assignment[equipSlot] = item.name
            if (item.primaryUse == ItemPrimaryUse.ACCESSORY) accCompared++
        }

        return assignment
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
}
