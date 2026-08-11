package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.equipment.Modeable
import net.sourceforge.kolmafia.equipment.ModeableState
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop Evaluator best-mode pre-selection (~1693–1724).
 * Picks the highest-scoring mode per needed modeable before speculation.
 */
object MaximizerModeSelection {

    fun selectBestModes(
        spec: MaximizeSpec,
        charState: CharacterState,
        rankedBuckets: SlotList<MaximizerRankedItem>,
        bestPerSlot: Map<EquipmentSlot, Pair<String, Double>>,
        preferences: Preferences?,
        familiarBonus: Double = 0.0,
        thrallBonus: Double = 0.0,
        carryFamiliars: List<String> = emptyList(),
        gameDatabase: GameDatabase? = null,
        activeEffects: List<EffectData> = emptyList(),
    ): Map<Modeable, String> {
        val needed = modeablesNeeded(rankedBuckets, bestPerSlot, carryFamiliars, gameDatabase)
        if (needed.isEmpty()) return emptyMap()

        val liveModes = ModeableState.currentModes(preferences)
        val result = mutableMapOf<Modeable, String>()
        for (modeable in needed) {
            val forced = spec.forcedModeables[modeable]?.takeIf { it.isNotBlank() }
            if (forced != null) {
                result[modeable] = modeable.normalizeMode(forced) ?: forced
                continue
            }
            var bestMode = liveModes[modeable] ?: modeable.modes.first()
            var bestScore = Double.NEGATIVE_INFINITY
            for (mode in modeable.modes) {
                val assignment = mapOf(modeable.slot to (modeable.itemName to 0.0))
                val score = MaximizerSpeculation.scoreLoadout(
                    baseState = charState,
                    assignment = assignment,
                    evaluator = spec.evaluator,
                    familiarBonus = familiarBonus,
                    thrallBonus = thrallBonus,
                    modeOverrides = mapOf(modeable to mode),
                    preferences = preferences,
                    maxBeeosity = spec.maxBeeosity,
                    validateEquipment = false,
                    activeEffects = activeEffects,
                )
                if (spec.evaluator.failed) continue
                if (score > bestScore) {
                    bestScore = score
                    bestMode = mode
                }
            }
            result[modeable] = bestMode
        }
        return result
    }

    /** Desktop Evaluator modeablesNeeded + slot-weight backup slots (~1473–1487). */
    fun modeablesNeeded(
        rankedBuckets: SlotList<MaximizerRankedItem>,
        bestPerSlot: Map<EquipmentSlot, Pair<String, Double>>,
        carryFamiliars: List<String> = emptyList(),
        gameDatabase: GameDatabase? = null,
    ): Set<Modeable> {
        val candidates = mutableSetOf<Modeable>()
        for (entry in rankedBuckets.entries()) {
            val items = when (entry) {
                is SlotListEntry.Slot -> entry.items
                is SlotListEntry.Familiar -> entry.items
            }
            for (ranked in items) {
                Modeable.find(ranked.itemId)?.let { candidates.add(it) }
            }
        }
        for ((_, pair) in bestPerSlot) {
            Modeable.find(pair.first)?.let { candidates.add(it) }
        }
        return candidates.filter { modeable ->
            val backupSlots = modeableBackupSlots(modeable, carryFamiliars, gameDatabase)
            backupSlots.any { slotEnabled(it) }
        }.toSet()
    }

    fun modeableBackupSlots(
        modeable: Modeable,
        carryFamiliars: List<String> = emptyList(),
        gameDatabase: GameDatabase? = null,
    ): Set<EquipmentSlot> {
        val slots = mutableSetOf(modeable.slot)
        when (modeable.slot) {
            EquipmentSlot.ACC1 -> {
                slots.add(EquipmentSlot.ACC2)
                slots.add(EquipmentSlot.ACC3)
            }
            EquipmentSlot.OFFHAND -> {
                val item = gameDatabase?.item(modeable.itemId)
                if (item != null && carryFamiliars.any { FamiliarCarryRules.canCarryItem(it, item) }) {
                    slots.add(EquipmentSlot.FAMILIAR)
                }
            }
            else -> {}
        }
        return slots
    }

    /** Apply best modes only when the modeable item is equipped on a backup slot. */
    fun assignmentModeOverrides(
        assignment: Map<EquipmentSlot, Pair<String, Double>>,
        bestModes: Map<Modeable, String>,
        carryFamiliars: List<String> = emptyList(),
        gameDatabase: GameDatabase? = null,
    ): Map<Modeable, String> {
        if (bestModes.isEmpty()) return emptyMap()
        val result = mutableMapOf<Modeable, String>()
        for ((modeable, mode) in bestModes) {
            val backupSlots = modeableBackupSlots(modeable, carryFamiliars, gameDatabase)
            val present = backupSlots.any { slot ->
                assignment[slot]?.first?.equals(modeable.itemName, ignoreCase = true) == true
            }
            if (present) result[modeable] = mode
        }
        return result
    }

    /** @deprecated use [modeablesNeeded] */
    fun neededModeables(
        rankedBuckets: SlotList<MaximizerRankedItem>,
        bestPerSlot: Map<EquipmentSlot, Pair<String, Double>>,
    ): Set<Modeable> = modeablesNeeded(rankedBuckets, bestPerSlot)

    fun modeForItem(
        itemName: String?,
        bestModes: Map<Modeable, String>,
        preferences: Preferences?,
    ): String? {
        if (itemName.isNullOrBlank()) return null
        val modeable = Modeable.find(itemName) ?: return null
        return bestModes[modeable] ?: ModeableState.currentMode(preferences, modeable)
    }

    private fun slotEnabled(slot: EquipmentSlot): Boolean =
        slot in MaximizerSpeculation.searchSlots || slot == EquipmentSlot.CARDSLEEVE
}
