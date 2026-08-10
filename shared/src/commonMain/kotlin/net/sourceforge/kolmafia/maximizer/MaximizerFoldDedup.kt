package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.FoldGroupDatabase
import net.sourceforge.kolmafia.data.GameDatabase

/**
 * Fold-group and same-item count dedup for maximizer speculation (Phase 376).
 * Mirrors desktop MaximizerSpeculation foldable count adjustment.
 */
object MaximizerFoldDedup {

    fun groupKey(itemName: String, gameDatabase: GameDatabase): String? {
        val group = FoldGroupDatabase.groupFor(itemName) ?: gameDatabase.foldGroup(itemName) ?: return null
        return group.items.firstOrNull()?.lowercase()
    }

    fun availableCount(
        itemName: String,
        assignment: Map<EquipmentSlot, Pair<String, Double>>,
        baseCount: Int,
        foldablesEnabled: Boolean,
        gameDatabase: GameDatabase,
        excludeSlot: EquipmentSlot? = null,
        excludeSlotsForSameItem: Set<EquipmentSlot> = emptySet(),
    ): Int {
        var count = baseCount
        for (slot in excludeSlotsForSameItem) {
            if (assignment[slot]?.first.equals(itemName, ignoreCase = true)) {
                count--
            }
        }
        if (!foldablesEnabled) return count

        val candidateGroupKey = groupKey(itemName, gameDatabase) ?: return count
        for ((slot, equipped) in assignment) {
            if (slot == excludeSlot) continue
            val equippedName = equipped.first
            if (equippedName.isBlank()) continue
            val equippedGroupKey = groupKey(equippedName, gameDatabase) ?: continue
            if (equippedGroupKey == candidateGroupKey) {
                count--
            }
        }
        return count
    }
}
