package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.familiar.FamiliarState
import net.sourceforge.kolmafia.familiar.FamiliarUsability
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Auto-discover carry-capable familiars for Crown/Bjorn when goals omit enthrone/bjornify races.
 * Phase 378 — mirrors desktop Evaluator.carriedFamiliarsNeeded discovery pass.
 */
object MaximizerCarriedFamiliars {

    data class DiscoveryContext(
        val familiarState: FamiliarState,
        val charState: CharacterState,
        val preferences: Preferences?,
        val excludeRaces: Set<String> = emptySet(),
        val scoreFamiliar: (String) -> Double,
    )

    fun throneBjornDiscoveryAllowed(state: CharacterState): Boolean =
        !state.isSneakyPete &&
            !state.isAxecore &&
            state.ascensionPath != AscensionPath.AVATAR_OF_JARLSBERG

    fun hasExplicitEnthroneGoals(spec: MaximizeSpec): Boolean =
        spec.enthronedFamiliars.any { !it.equals("none", ignoreCase = true) }

    fun hasExplicitBjornGoals(spec: MaximizeSpec): Boolean =
        spec.bjornifiedFamiliars.any { !it.equals("none", ignoreCase = true) }

    fun needsEnthroneDiscovery(
        spec: MaximizeSpec,
        charState: CharacterState,
        rankedBuckets: SlotList<MaximizerRankedItem>,
        bestPerSlot: Map<EquipmentSlot, Pair<String, Double>>,
    ): Boolean =
        !hasExplicitEnthroneGoals(spec) &&
            throneBjornDiscoveryAllowed(charState) &&
            crownCandidate(spec, rankedBuckets, bestPerSlot)

    fun needsBjornDiscovery(
        spec: MaximizeSpec,
        charState: CharacterState,
        rankedBuckets: SlotList<MaximizerRankedItem>,
        bestPerSlot: Map<EquipmentSlot, Pair<String, Double>>,
    ): Boolean =
        !hasExplicitBjornGoals(spec) &&
            throneBjornDiscoveryAllowed(charState) &&
            bjornCandidate(spec, rankedBuckets, bestPerSlot)

    fun discoverCarryFamiliars(context: DiscoveryContext): List<String> {
        val excluded = context.excludeRaces.map { it.lowercase() }.toSet()
        return context.familiarState.ownedFamiliars
            .asSequence()
            .filter { familiar ->
                FamiliarUsability.isUsable(familiar, context.charState, context.preferences) &&
                    FamiliarCarryRules.canCarry(familiar.race) &&
                    !familiar.race.equals(context.charState.familiarName, ignoreCase = true) &&
                    !excluded.contains(familiar.race.lowercase())
            }
            .map { it.race }
            .distinctBy { it.lowercase() }
            .sortedByDescending { context.scoreFamiliar(it) }
            .toList()
    }

    fun defaultExcludeRaces(charState: CharacterState): Set<String> = buildSet {
        if (charState.familiarName.isNotBlank()) add(charState.familiarName)
        if (charState.enthronedFamiliarName.isNotBlank()) add(charState.enthronedFamiliarName)
        if (charState.bjornedFamiliarName.isNotBlank()) add(charState.bjornedFamiliarName)
    }

    private fun crownCandidate(
        spec: MaximizeSpec,
        rankedBuckets: SlotList<MaximizerRankedItem>,
        bestPerSlot: Map<EquipmentSlot, Pair<String, Double>>,
    ): Boolean {
        if (spec.equipRequired.any { it.equals(MaximizerManager.CROWN_OF_THRONES, ignoreCase = true) }) {
            return true
        }
        if (bestPerSlot[EquipmentSlot.HAT]?.first.equals(MaximizerManager.CROWN_OF_THRONES, ignoreCase = true)) {
            return true
        }
        return rankedBuckets.allItems(MaximizerSlot.HAT).any {
            it.name.equals(MaximizerManager.CROWN_OF_THRONES, ignoreCase = true)
        }
    }

    private fun bjornCandidate(
        spec: MaximizeSpec,
        rankedBuckets: SlotList<MaximizerRankedItem>,
        bestPerSlot: Map<EquipmentSlot, Pair<String, Double>>,
    ): Boolean {
        if (spec.equipRequired.any { it.equals(MaximizerManager.BUDDY_BJORN, ignoreCase = true) }) {
            return true
        }
        if (bestPerSlot[EquipmentSlot.CONTAINER]?.first.equals(MaximizerManager.BUDDY_BJORN, ignoreCase = true)) {
            return true
        }
        return rankedBuckets.allItems(MaximizerSlot.CONTAINER).any {
            it.name.equals(MaximizerManager.BUDDY_BJORN, ignoreCase = true)
        }
    }
}
