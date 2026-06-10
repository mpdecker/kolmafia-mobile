package net.sourceforge.kolmafia.equipment

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.OutfitData
import net.sourceforge.kolmafia.data.OutfitDatabase
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.request.ClanStashRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.CustomOutfitRequest
import net.sourceforge.kolmafia.request.DisplayCaseRequest
import net.sourceforge.kolmafia.request.EquipmentRequest
import net.sourceforge.kolmafia.request.StorageRequest

open class OutfitManager(
    private val retrieveItemService: RetrieveItemService?,
    private val equipmentRequest: EquipmentRequest,
    private val customOutfitRequest: CustomOutfitRequest,
    private val character: KoLCharacter,
    private val gameDatabase: GameDatabase,
    private val closetRequest: ClosetRequest?,
    private val storageRequest: StorageRequest?,
    private val displayCaseRequest: DisplayCaseRequest?,
    private val clanStashRequest: ClanStashRequest?,
    private val inventoryManager: net.sourceforge.kolmafia.inventory.InventoryManager?,
) {
    open suspend fun refreshCustomOutfits() {
        OutfitDatabase.clearCustom()
        val options = equipmentRequest.fetchOutfitOptions()
        options.find { it.second.contains("Previous Outfit", ignoreCase = true) }?.first?.let { id ->
            if (id < 0) OutfitSpecial.resolvedPreviousOutfitId = id
        }
        customOutfitRequest.fetchCustomOutfits(options).forEach { OutfitDatabase.registerCustom(it) }
    }

    open fun getMatchingOutfit(name: String): ResolvedOutfit? {
        val trimmed = name.trim()
        if (OutfitSpecial.isBirthdaySuitAlias(trimmed)) {
            return ResolvedOutfit(OutfitSpecial.BIRTHDAY_SUIT_ID, "Birthday Suit", emptyList())
        }
        if (OutfitSpecial.isPreviousOutfitAlias(trimmed)) {
            val id = OutfitSpecial.resolvedPreviousOutfitId ?: return null
            val data = OutfitDatabase.getById(id)
            return data?.toResolved() ?: ResolvedOutfit(id, "Your Previous Outfit", emptyList())
        }

        val lookupName = EmbeddedOutfitActions.displayName(trimmed)
        OutfitDatabase.getByName(lookupName)?.let { return it.toResolved() }
        OutfitDatabase.getByName(trimmed)?.let { return it.toResolved() }

        val lower = lookupName.lowercase()
        return OutfitDatabase.allOutfits()
            .firstOrNull {
                val n = it.name.lowercase()
                n.contains(lower) || lower.contains(n) ||
                    EmbeddedOutfitActions.displayName(it.name).lowercase().contains(lower)
            }
            ?.toResolved()
    }

    open suspend fun hasOutfit(outfitId: Int): Boolean {
        if (outfitId < 0) return OutfitDatabase.getById(outfitId) != null
        val data = OutfitDatabase.getById(outfitId) ?: return false
        return hasAllPieces(data.toResolved())
    }

    open suspend fun saveOutfit(name: String): Boolean {
        val result = equipmentRequest.saveCustomOutfit(name)
        if (result.isSuccess) {
            refreshCustomOutfits()
        }
        return result.isSuccess
    }

    open suspend fun hasAllPieces(outfit: ResolvedOutfit): Boolean {
        if (outfit.isBirthdaySuit) return true
        val counts = pieceCounts(outfit.pieces)
        for ((pieceName, needed) in counts) {
            val item = gameDatabase.item(pieceName) ?: return false
            if (!item.isEquipment) return false
            if (accessibleCount(item.id, item.name) < needed) return false
        }
        return true
    }

    open fun isWearingOutfit(outfit: ResolvedOutfit, equipment: Map<EquipmentSlot, String> = character.state.value.equipment): Boolean {
        if (outfit.isBirthdaySuit) {
            return equipment.values.all { it.isBlank() } || equipment.isEmpty()
        }
        return isWearingPieces(outfit.pieces, equipment)
    }

    open suspend fun retrieveOutfit(outfit: ResolvedOutfit): Boolean {
        if (outfit.isBirthdaySuit) return true
        val equipment = character.state.value.equipment
        val counts = pieceCounts(outfit.pieces)
        for ((pieceName, needed) in counts) {
            val item = gameDatabase.item(pieceName) ?: return false
            val equipped = equippedCount(pieceName, equipment)
            val missing = needed - equipped
            if (missing <= 0) continue
            if (accessibleCount(item.id, pieceName) < needed) return false
            val service = retrieveItemService ?: return false
            if (service.retrieve(item.id, missing) < missing) return false
        }
        return true
    }

    open suspend fun wearOutfit(name: String, postWear: ((String) -> Unit)? = null): Boolean {
        val outfit = getMatchingOutfit(name) ?: return false
        if (outfit.isBirthdaySuit) {
            return equipmentRequest.unequipAll().isSuccess
        }
        val isPrevious = OutfitSpecial.isPreviousOutfitAlias(name)
        if (!isPrevious && !retrieveOutfit(outfit)) return false
        if (!equipmentRequest.wearOutfit(outfit.id).isSuccess) return false
        val rawName = OutfitDatabase.getById(outfit.id)?.name ?: name
        postWear?.let { EmbeddedOutfitActions.runAfterWear(rawName, it) }
        return true
    }

    fun treatChances(outfit: ResolvedOutfit): List<Pair<String, Double>> {
        val data = OutfitDatabase.getById(outfit.id) ?: return emptyList()
        return data.halloweenDrops.mapNotNull { drop ->
            val trimmed = drop.trim()
            when {
                trimmed.equals("none", ignoreCase = true) || trimmed.isEmpty() -> null
                else -> {
                    val paren = Regex("""^(.+?)\s*\(([\d.]+)\)\s*$""").find(trimmed)
                    if (paren != null) {
                        paren.groupValues[1].trim() to (paren.groupValues[2].toDoubleOrNull() ?: 1.0)
                    } else {
                        trimmed to 1.0
                    }
                }
            }
        }
    }

    open suspend fun getOutfitsWithPieces(): List<ResolvedOutfit> {
        val result = mutableListOf<ResolvedOutfit>()
        for (data in OutfitDatabase.allOutfits()) {
            val resolved = data.toResolved()
            if (hasAllPieces(resolved)) result.add(resolved)
        }
        for (data in OutfitDatabase.customOutfits()) {
            result.add(data.toResolved())
        }
        return result
    }

    open suspend fun accessibleCount(itemId: Int, itemName: String): Int {
        var total = inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0
        total += closetRequest?.fetchContents()?.get(itemId) ?: 0
        total += storageRequest?.fetchContents()?.get(itemId) ?: 0
        total += displayCaseRequest?.fetchContents()?.get(itemId) ?: 0
        total += clanStashRequest?.fetchContents()?.get(itemId) ?: 0
        total += equippedCount(itemName, character.state.value.equipment)
        return total
    }

    companion object {
        fun pieceCounts(pieces: List<String>): Map<String, Int> =
            pieces.groupingBy { it.lowercase() }.eachCount()

        fun equippedCount(itemName: String, equipment: Map<EquipmentSlot, String>): Int {
            val lower = itemName.lowercase()
            return equipment.values.count { it.equals(lower, ignoreCase = true) }
        }

        /** Slot-aware multiset check: duplicate weapons/accessories require multiple equipped copies. */
        fun isWearingPieces(pieces: List<String>, equipment: Map<EquipmentSlot, String>): Boolean {
            val required = pieceCounts(pieces)
            val equippedNames = equipment.values.filter { it.isNotBlank() }
            val equippedCounts = equippedNames.groupingBy { it.lowercase() }.eachCount()
            return required.all { (name, count) -> (equippedCounts[name] ?: 0) >= count }
        }
    }
}

private fun OutfitData.toResolved() = ResolvedOutfit(id = id, name = name, pieces = equipment)
