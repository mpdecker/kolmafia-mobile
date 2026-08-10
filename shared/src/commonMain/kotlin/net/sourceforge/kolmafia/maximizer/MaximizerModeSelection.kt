package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
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
    ): Map<Modeable, String> {
        val needed = neededModeables(rankedBuckets, bestPerSlot)
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

    fun neededModeables(
        rankedBuckets: SlotList<MaximizerRankedItem>,
        bestPerSlot: Map<EquipmentSlot, Pair<String, Double>>,
    ): Set<Modeable> {
        val needed = mutableSetOf<Modeable>()
        for (entry in rankedBuckets.entries()) {
            val items = when (entry) {
                is SlotListEntry.Slot -> entry.items
                is SlotListEntry.Familiar -> entry.items
            }
            for (ranked in items) {
                Modeable.find(ranked.itemId)?.let { needed.add(it) }
            }
        }
        for ((_, pair) in bestPerSlot) {
            Modeable.find(pair.first)?.let { needed.add(it) }
        }
        return needed
    }

    fun modeForItem(
        itemName: String?,
        bestModes: Map<Modeable, String>,
        preferences: Preferences?,
    ): String? {
        if (itemName.isNullOrBlank()) return null
        val modeable = Modeable.find(itemName) ?: return null
        return bestModes[modeable] ?: ModeableState.currentMode(preferences, modeable)
    }
}
