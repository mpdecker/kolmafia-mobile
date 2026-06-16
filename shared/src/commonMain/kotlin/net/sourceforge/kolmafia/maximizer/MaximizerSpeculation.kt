package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.modifiers.CurrentModifiers
import net.sourceforge.kolmafia.modifiers.DoubleModifier

/**
 * Outfit-aware loadout scoring and recursive equipment speculation.
 * Mirrors desktop MaximizerSpeculation search shape with a shared combination budget.
 */
object MaximizerSpeculation {

    private val searchSlots = listOf(
        EquipmentSlot.HAT,
        EquipmentSlot.WEAPON,
        EquipmentSlot.OFFHAND,
        EquipmentSlot.SHIRT,
        EquipmentSlot.PANTS,
        EquipmentSlot.ACC1,
        EquipmentSlot.ACC2,
        EquipmentSlot.ACC3,
        EquipmentSlot.FAMILIAR,
    )

    fun scoreLoadout(
        baseState: CharacterState,
        assignment: Map<EquipmentSlot, Pair<String, Double>>,
        modifier: DoubleModifier,
        familiarBonus: Double = 0.0,
        thrallBonus: Double = 0.0,
        itemScorer: ((String, DoubleModifier) -> Double)? = null,
        isFamiliarCarriedItem: (String) -> Boolean = { false },
        familiarCarryScorer: ((String, DoubleModifier) -> Double)? = null,
    ): Double {
        val equipment = buildMap {
            for (slot in searchSlots) {
                val name = assignment[slot]?.first?.takeIf { it.isNotBlank() }
                    ?: baseState.equipment[slot]?.takeIf { it.isNotBlank() }
                if (!name.isNullOrBlank()) put(slot, name)
            }
        }
        val state = baseState.copy(equipment = equipment)
        var equipmentScore = CurrentModifiers(state).values.get(modifier)
        if (equipmentScore == 0.0 && itemScorer != null) {
            equipmentScore = assignment.entries.sumOf { (slot, pair) ->
                val name = pair.first
                if (name.isBlank()) 0.0
                else scoreAssignmentItem(
                    slot, name, modifier, itemScorer, isFamiliarCarriedItem, familiarCarryScorer,
                )
            }
        }
        return equipmentScore + familiarBonus + thrallBonus
    }

    private fun scoreAssignmentItem(
        slot: EquipmentSlot,
        name: String,
        modifier: DoubleModifier,
        itemScorer: (String, DoubleModifier) -> Double,
        isFamiliarCarriedItem: (String) -> Boolean,
        familiarCarryScorer: ((String, DoubleModifier) -> Double)?,
    ): Double {
        if (slot == EquipmentSlot.FAMILIAR &&
            familiarCarryScorer != null &&
            isFamiliarCarriedItem(name)
        ) {
            return familiarCarryScorer(name, modifier)
        }
        return itemScorer(name, modifier)
    }

    fun tiebreakerScore(
        baseState: CharacterState,
        assignment: Map<EquipmentSlot, Pair<String, Double>>,
    ): Double {
        val equipment = buildMap {
            for (slot in searchSlots) {
                val name = assignment[slot]?.first?.takeIf { it.isNotBlank() }
                    ?: baseState.equipment[slot]?.takeIf { it.isNotBlank() }
                if (!name.isNullOrBlank()) put(slot, name)
            }
        }
        val mods = CurrentModifiers(baseState.copy(equipment = equipment)).values
        return mods.get(DoubleModifier.INITIATIVE) +
            mods.get(DoubleModifier.ITEMDROP) +
            mods.get(DoubleModifier.MUS) +
            mods.get(DoubleModifier.MYS) +
            mods.get(DoubleModifier.MOX) +
            mods.get(DoubleModifier.MEATDROP)
    }

    private fun assignmentPrice(
        assignment: Map<EquipmentSlot, Pair<String, Double>>,
        priceFor: (String) -> Int,
    ): Int = assignment.values.sumOf { (name, _) ->
        if (name.isBlank()) 0 else priceFor(name)
    }

    private fun isBetterLoadout(
        score: Double,
        tie: Double,
        price: Int,
        bestScore: Double,
        bestTie: Double,
        bestPrice: Int,
        preferLowerPrice: Boolean,
    ): Boolean {
        if (score > bestScore + 1e-9) return true
        if (score < bestScore - 1e-9) return false
        if (tie > bestTie + 1e-9) return true
        if (tie < bestTie - 1e-9) return false
        return preferLowerPrice && price < bestPrice
    }

    fun speculate(
        spec: MaximizeSpec,
        baseState: CharacterState,
        candidatesBySlot: Map<EquipmentSlot, List<Pair<String, Double>>>,
        budget: ComboBudget,
        familiarBonus: Double = 0.0,
        thrallBonus: Double = 0.0,
        seed: Map<EquipmentSlot, Pair<String, Double>> = emptyMap(),
        itemScorer: ((String, DoubleModifier) -> Double)? = null,
        priceFor: ((String) -> Int)? = null,
        isFamiliarCarriedItem: (String) -> Boolean = { false },
        familiarCarryScorer: ((String, DoubleModifier) -> Double)? = null,
    ): Map<EquipmentSlot, Pair<String, Double>> {
        var best = seed
        var bestScore = if (seed.isNotEmpty()) {
            scoreLoadout(
                baseState, seed, spec.primary, familiarBonus, thrallBonus, itemScorer,
                isFamiliarCarriedItem, familiarCarryScorer,
            )
        } else {
            Double.NEGATIVE_INFINITY
        }
        var bestTie = if (seed.isNotEmpty()) tiebreakerScore(baseState, seed) else Double.NEGATIVE_INFINITY
        var bestPrice = if (seed.isNotEmpty() && priceFor != null) assignmentPrice(seed, priceFor) else Int.MAX_VALUE
        val preferLowerPrice = spec.maxPrice != null && priceFor != null

        fun search(
            slotIndex: Int,
            current: MutableMap<EquipmentSlot, Pair<String, Double>>,
            usedItems: MutableSet<String>,
        ) {
            if (budget.tick()) return
            if (slotIndex >= searchSlots.size) {
                val score = scoreLoadout(
                    baseState, current, spec.primary, familiarBonus, thrallBonus, itemScorer,
                    isFamiliarCarriedItem, familiarCarryScorer,
                )
                val tie = tiebreakerScore(baseState, current)
                val price = priceFor?.let { assignmentPrice(current, it) } ?: Int.MAX_VALUE
                if (isBetterLoadout(score, tie, price, bestScore, bestTie, bestPrice, preferLowerPrice)) {
                    bestScore = score
                    bestTie = tie
                    bestPrice = price
                    best = current.toMap()
                }
                return
            }
            val slot = searchSlots[slotIndex]
            val candidates = candidatesBySlot[slot].orEmpty()
            if (candidates.isEmpty()) {
                search(slotIndex + 1, current, usedItems)
                return
            }
            for ((name, _) in candidates) {
                if (name in usedItems) continue
                current[slot] = name to 0.0
                usedItems.add(name)
                search(slotIndex + 1, current, usedItems)
                usedItems.remove(name)
                current.remove(slot)
                if (budget.exhausted()) return
            }
        }

        search(0, seed.toMutableMap(), seed.values.map { it.first }.toMutableSet())
        return best
    }

    fun topCandidatesPerSlot(
        spec: MaximizeSpec,
        gameDatabase: GameDatabase,
        candidateIds: Set<Int>,
        usedElsewhere: Set<String>,
        perSlotLimit: Int,
        scoreItem: (String, DoubleModifier) -> Double,
        itemMeetsConstraints: (String, MaximizeSpec) -> Boolean,
        priceFor: (String) -> Int = { gameDatabase.npcPrice(it) },
        familiarCarryRaces: List<String> = emptyList(),
        familiarCarryScorer: ((String, DoubleModifier) -> Double)? = null,
    ): Map<EquipmentSlot, List<Pair<String, Double>>> {
        val carryScorer = familiarCarryScorer ?: scoreItem
        val result = mutableMapOf<EquipmentSlot, List<Pair<String, Double>>>()
        for (slot in searchSlots) {
            val ranked = mutableListOf<Pair<String, Double>>()
            for (itemId in candidateIds) {
                val itemData = gameDatabase.item(itemId) ?: continue
                if (!fitsSlot(itemData, slot) || itemData.name in usedElsewhere) continue
                if (spec.requireMelee && slot == EquipmentSlot.WEAPON &&
                    itemData.primaryUse == ItemPrimaryUse.SIXGUN
                ) continue
                if (spec.requireHands && slot == EquipmentSlot.OFFHAND &&
                    itemData.primaryUse != ItemPrimaryUse.OFFHAND
                ) continue
                if (!itemMeetsConstraints(itemData.name, spec)) continue
                ranked.add(itemData.name to scoreItem(itemData.name, spec.primary))
            }
            if (slot == EquipmentSlot.FAMILIAR) {
                for (race in familiarCarryRaces) {
                    for (itemId in candidateIds) {
                        val itemData = gameDatabase.item(itemId) ?: continue
                        if (itemData.name in usedElsewhere) continue
                        if (!FamiliarCarryRules.canCarryItem(race, itemData)) continue
                        if (spec.requireMelee && itemData.primaryUse == ItemPrimaryUse.SIXGUN) continue
                        if (spec.requireHands && itemData.primaryUse != ItemPrimaryUse.OFFHAND &&
                            race == FamiliarCarryRules.LEFT_HAND_RACE
                        ) continue
                        if (!itemMeetsConstraints(itemData.name, spec)) continue
                        ranked.add(itemData.name to carryScorer(itemData.name, spec.primary))
                    }
                }
            }
            result[slot] = when {
                spec.maxPrice != null -> ranked.sortedWith(
                    compareBy<Pair<String, Double>> { priceFor(it.first) }
                        .thenByDescending { it.second },
                ).take(perSlotLimit)
                spec.minPrice != null -> ranked.sortedWith(
                    compareByDescending<Pair<String, Double>> { priceFor(it.first) }
                        .thenByDescending { it.second },
                ).take(perSlotLimit)
                else -> ranked.sortedByDescending { it.second }.take(perSlotLimit)
            }
        }
        return result
    }

    private fun fitsSlot(item: ItemData, slot: EquipmentSlot): Boolean = when (slot) {
        EquipmentSlot.HAT -> item.primaryUse == ItemPrimaryUse.HAT
        EquipmentSlot.WEAPON -> item.primaryUse in setOf(ItemPrimaryUse.WEAPON, ItemPrimaryUse.SIXGUN)
        EquipmentSlot.OFFHAND -> item.primaryUse == ItemPrimaryUse.OFFHAND
        EquipmentSlot.SHIRT -> item.primaryUse == ItemPrimaryUse.SHIRT
        EquipmentSlot.PANTS -> item.primaryUse == ItemPrimaryUse.PANTS
        EquipmentSlot.ACC1, EquipmentSlot.ACC2, EquipmentSlot.ACC3 ->
            item.primaryUse == ItemPrimaryUse.ACCESSORY
        EquipmentSlot.FAMILIAR -> item.primaryUse == ItemPrimaryUse.FAMILIAR
        else -> false
    }
}

/** Shared combination budget across maximizer refine passes. */
class ComboBudget(private val limit: Int) {
    private var checked = 0

    fun tick(): Boolean {
        if (limit <= 0) return false
        checked++
        return checked > limit
    }

    fun exhausted(): Boolean = limit > 0 && checked > limit

    fun remaining(): Int = if (limit <= 0) Int.MAX_VALUE else (limit - checked).coerceAtLeast(0)
}
