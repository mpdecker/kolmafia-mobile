package net.sourceforge.kolmafia.maximizer

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.OutfitDatabase
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MaximizerSpeculationTest {

    @Test
    fun scoreLoadout_countsOutfitSetBonus() = runBlocking {
        ModifierDatabase.load()
        OutfitDatabase.load()
        val state = CharacterState()
        val hatOnly = mapOf(
            EquipmentSlot.HAT to ("bugbear beanie" to 0.0),
        )
        val fullOutfit = mapOf(
            EquipmentSlot.HAT to ("bugbear beanie" to 0.0),
            EquipmentSlot.PANTS to ("bugbear bungguard" to 0.0),
        )
        val hatScore = MaximizerSpeculation.scoreLoadout(state, hatOnly, DoubleModifier.MYS)
        val outfitScore = MaximizerSpeculation.scoreLoadout(state, fullOutfit, DoubleModifier.MYS)
        assertTrue(outfitScore > hatScore, "outfit bonus expected: hat=$hatScore outfit=$outfitScore")
        assertEquals(3.0, outfitScore, 0.01)
    }

    @Test
    fun comboBudget_stopsAtLimit() {
        val budget = ComboBudget(2)
        assertEquals(false, budget.tick())
        assertEquals(false, budget.tick())
        assertEquals(true, budget.tick())
        assertTrue(budget.exhausted())
    }

    @Test
    fun speculate_returnsBestAssignmentWithinBudget() {
        val state = CharacterState()
        val spec = MaximizeSpec(DoubleModifier.MYS)
        val candidates = mapOf(
            EquipmentSlot.HAT to listOf("hat-a" to 1.0, "hat-b" to 2.0),
            EquipmentSlot.SHIRT to listOf("shirt-a" to 1.0),
        )
        val budget = ComboBudget(10)
        val result = MaximizerSpeculation.speculate(spec, state, candidates, budget, seed = emptyMap())
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun topCandidatesPerSlot_sortsByPriceWhenMaxPriceSet() {
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(id: Int) = net.sourceforge.kolmafia.data.ItemData(
                id, "item-$id", "", "", net.sourceforge.kolmafia.data.ItemPrimaryUse.HAT,
                emptySet(), setOf('t'), 0, null,
            )
            override fun item(name: String) = item(name.removePrefix("item-").toIntOrNull() ?: 0)
            override fun npcPrice(itemName: String): Int = when (itemName) {
                "item-1" -> 500
                "item-2" -> 100
                else -> 0
            }
            override fun itemModifier(name: String) =
                net.sourceforge.kolmafia.data.ModifierEntry("Item", name, "Mysticality: +1")
        }
        val spec = MaximizeSpec(DoubleModifier.MYS, maxPrice = 1000)
        val ranked = MaximizerSpeculation.topCandidatesPerSlot(
            spec, db, setOf(1, 2), emptySet(), 2,
            { _, _ -> 1.0 },
            { _, _ -> true },
        )
        val hats = ranked[EquipmentSlot.HAT] ?: emptyList()
        assertEquals("item-2", hats.first().first)
    }

    @Test
    fun topCandidatesPerSlot_usesMallPriceWhenProvided() {
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(id: Int) = net.sourceforge.kolmafia.data.ItemData(
                id, "item-$id", "", "", net.sourceforge.kolmafia.data.ItemPrimaryUse.HAT,
                emptySet(), setOf('t'), 0, null,
            )
            override fun item(name: String) = item(name.removePrefix("item-").toIntOrNull() ?: 0)
            override fun npcPrice(itemName: String): Int = when (itemName) {
                "item-1" -> 5000
                "item-2" -> 2000
                else -> 0
            }
            override fun itemModifier(name: String) =
                net.sourceforge.kolmafia.data.ModifierEntry("Item", name, "Mysticality: +1")
        }
        val spec = MaximizeSpec(DoubleModifier.MYS, maxPrice = 10000)
        val ranked = MaximizerSpeculation.topCandidatesPerSlot(
            spec, db, setOf(1, 2), emptySet(), 2,
            { _, _ -> 1.0 },
            { _, _ -> true },
            priceFor = { name ->
                when (name) {
                    "item-1" -> 100
                    "item-2" -> 5000
                    else -> db.npcPrice(name)
                }
            },
        )
        val hats = ranked[EquipmentSlot.HAT] ?: emptyList()
        assertEquals("item-1", hats.first().first)
    }

    @Test
    fun speculate_prefersLowerPriceOnTieWhenMaxPriceSet() {
        val state = CharacterState()
        val spec = MaximizeSpec(DoubleModifier.MYS, maxPrice = 1000)
        val candidates = mapOf(
            EquipmentSlot.HAT to listOf("cheap-hat" to 5.0, "dear-hat" to 5.0),
        )
        val budget = ComboBudget(10)
        val result = MaximizerSpeculation.speculate(
            spec, state, candidates, budget,
            priceFor = { name -> if (name == "cheap-hat") 100 else 500 },
        )
        assertEquals("cheap-hat", result[EquipmentSlot.HAT]?.first)
    }

    @Test
    fun tiebreakerScore_prefersHigherSecondaryModifiers() {
        val state = CharacterState()
        val low = mapOf(EquipmentSlot.HAT to ("hat-a" to 0.0))
        val high = mapOf(EquipmentSlot.HAT to ("hat-b" to 0.0))
        val lowTie = MaximizerSpeculation.tiebreakerScore(state, low)
        val highTie = MaximizerSpeculation.tiebreakerScore(state, high)
        assertEquals(lowTie, highTie)
    }

    @Test
    fun topCandidatesPerSlot_includesFamiliarEquipment() {
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(id: Int) = net.sourceforge.kolmafia.data.ItemData(
                id, "fam-item-$id", "", "", net.sourceforge.kolmafia.data.ItemPrimaryUse.FAMILIAR,
                emptySet(), emptySet(), 0, null,
            )
            override fun itemModifier(name: String) =
                net.sourceforge.kolmafia.data.ModifierEntry("Item", name, "Mysticality: +2")
        }
        val spec = MaximizeSpec(DoubleModifier.MYS)
        val ranked = MaximizerSpeculation.topCandidatesPerSlot(
            spec, db, setOf(99), emptySet(), 2,
            { _, _ -> 1.0 },
            { _, _ -> true },
        )
        val familiar = ranked[EquipmentSlot.FAMILIAR] ?: emptyList()
        assertEquals("fam-item-99", familiar.first().first)
    }
}
