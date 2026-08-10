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
        val eval = Evaluator("mysticality")
        val hatScore = MaximizerSpeculation.scoreLoadout(state, hatOnly, eval)
        val outfitScore = MaximizerSpeculation.scoreLoadout(state, fullOutfit, eval)
        assertTrue(outfitScore > hatScore, "outfit bonus expected: hat=$hatScore outfit=$outfitScore")
        assertEquals(3.0, outfitScore, 0.01)
    }

    @Test
    fun scoreLoadout_multiWeightGoal_prefersItemHeavyAssignment() = runBlocking {
        ModifierDatabase.load()
        OutfitDatabase.load()
        val state = CharacterState()
        val eval = Evaluator("2 item, 1 meat")
        val itemHeavy = mapOf(EquipmentSlot.HAT to ("bounty-hunting helmet" to 0.0))
        val meatHeavy = mapOf(EquipmentSlot.HAT to ("gold crown" to 0.0))
        val itemScore = MaximizerSpeculation.scoreLoadout(state, itemHeavy, eval)
        val meatScore = MaximizerSpeculation.scoreLoadout(state, meatHeavy, eval)
        assertTrue(itemScore > meatScore, "2 item / 1 meat should prefer item drop: item=$itemScore meat=$meatScore")
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
    fun speculate_prefersNonFailedLoadout() = runBlocking {
        ModifierDatabase.load()
        OutfitDatabase.load()
        val state = CharacterState()
        val spec = MaximizeSpec(
            primary = DoubleModifier.MUS,
            evaluator = Evaluator("100 meat, 1 muscle, 2 min"),
        )
        val candidates = mapOf(
            EquipmentSlot.WEAPON to listOf(
                "flamingo mallet" to 999.0,
                "muculent machete" to 1.0,
            ),
        )
        val budget = ComboBudget(100)
        val result = MaximizerSpeculation.speculate(spec, state, candidates, budget)
        assertEquals("muculent machete", result[EquipmentSlot.WEAPON]?.first)
    }

    @Test
    fun speculate_stopsEarlyWhenExceeded() = runBlocking {
        ModifierDatabase.load()
        OutfitDatabase.load()
        val state = CharacterState()
        val spec = MaximizeSpec(
            primary = DoubleModifier.MYS,
            evaluator = Evaluator("1 max, 1 mysticality"),
        )
        val candidates = mapOf(
            EquipmentSlot.HAT to listOf(
                "chef's hat" to 1.0,
                "papier-mitre" to 2.0,
                "crumpled felt fedora" to 3.0,
                "bounty-hunting helmet" to 4.0,
                "bugbear beanie" to 5.0,
            ),
        )
        val budget = ComboBudget(100)
        MaximizerSpeculation.speculate(spec, state, candidates, budget)
        assertTrue(budget.remaining() > 85, "exceeded should stop search before exhausting budget")
    }

    @Test
    fun speculate_returnsBestAssignmentWithinBudget() {
        val state = CharacterState()
        val spec = MaximizeSpec(DoubleModifier.MYS)
        val candidates = mapOf(
            EquipmentSlot.HAT to listOf("hat-a" to 1.0, "hat-b" to 2.0),
            EquipmentSlot.SHIRT to listOf("shirt-a" to 1.0),
        )
        val budget = ComboBudget(25)
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
        val budget = ComboBudget(25)
        val result = MaximizerSpeculation.speculate(
            spec, state, candidates, budget,
            priceFor = { name -> if (name == "cheap-hat") 100 else 500 },
        )
        assertEquals("cheap-hat", result[EquipmentSlot.HAT]?.first)
    }

    @Test
    fun tiebreakerScore_prefersHigherFamiliarWeight() = runBlocking {
        ModifierDatabase.load()
        OutfitDatabase.load()
        val state = CharacterState()
        val light = mapOf(EquipmentSlot.HAT to ("papier-mitre" to 0.0))
        val heavy = mapOf(EquipmentSlot.HAT to ("crumpled felt fedora" to 0.0))
        val lightTie = MaximizerSpeculation.tiebreakerScore(state, light)
        val heavyTie = MaximizerSpeculation.tiebreakerScore(state, heavy)
        assertTrue(heavyTie > lightTie, "desktop tiebreaker: heavy=$heavyTie light=$lightTie")
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

    @Test
    fun topCandidatesPerSlot_includesHatrackCarriedHats() {
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(id: Int) = when (id) {
                1 -> net.sourceforge.kolmafia.data.ItemData(
                    1, "carried-hat", "", "", net.sourceforge.kolmafia.data.ItemPrimaryUse.HAT,
                    emptySet(), emptySet(), 0, null,
                )
                else -> null
            }
            override fun itemModifier(name: String) =
                net.sourceforge.kolmafia.data.ModifierEntry("Item", name, "Mysticality: +3")
        }
        val spec = MaximizeSpec(DoubleModifier.MYS)
        val ranked = MaximizerSpeculation.topCandidatesPerSlot(
            spec, db, setOf(1), emptySet(), 2,
            { _, _ -> 1.0 },
            { _, _ -> true },
            familiarCarryRaces = listOf(FamiliarCarryRules.HATRACK_RACE),
        )
        val familiar = ranked[EquipmentSlot.FAMILIAR] ?: emptyList()
        assertEquals("carried-hat", familiar.first().first)
    }

    @Test
    fun topCandidatesPerSlot_includesHandCarriedWeapons() {
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(id: Int) = when (id) {
                2 -> net.sourceforge.kolmafia.data.ItemData(
                    2, "hand-weapon", "", "", net.sourceforge.kolmafia.data.ItemPrimaryUse.WEAPON,
                    emptySet(), emptySet(), 0, null,
                )
                else -> null
            }
            override fun itemModifier(name: String) =
                net.sourceforge.kolmafia.data.ModifierEntry("Item", name, "Mysticality: +4")
        }
        val spec = MaximizeSpec(DoubleModifier.MYS)
        val ranked = MaximizerSpeculation.topCandidatesPerSlot(
            spec, db, setOf(2), emptySet(), 2,
            { _, _ -> 1.0 },
            { _, _ -> true },
            familiarCarryRaces = listOf(FamiliarCarryRules.HAND_RACE),
        )
        val familiar = ranked[EquipmentSlot.FAMILIAR] ?: emptyList()
        assertEquals("hand-weapon", familiar.first().first)
    }

    @Test
    fun topCandidatesPerSlot_excludesLeftHandBlockedItems() {
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(id: Int) = when (id) {
                9133 -> net.sourceforge.kolmafia.data.ItemData(
                    9133, "blocked-offhand", "", "", net.sourceforge.kolmafia.data.ItemPrimaryUse.OFFHAND,
                    emptySet(), emptySet(), 0, null,
                )
                3 -> net.sourceforge.kolmafia.data.ItemData(
                    3, "allowed-offhand", "", "", net.sourceforge.kolmafia.data.ItemPrimaryUse.OFFHAND,
                    emptySet(), emptySet(), 0, null,
                )
                else -> null
            }
            override fun itemModifier(name: String) =
                net.sourceforge.kolmafia.data.ModifierEntry("Item", name, "Mysticality: +1")
        }
        val spec = MaximizeSpec(DoubleModifier.MYS)
        val ranked = MaximizerSpeculation.topCandidatesPerSlot(
            spec, db, setOf(9133, 3), emptySet(), 5,
            { _, _ -> 1.0 },
            { _, _ -> true },
            familiarCarryRaces = listOf(FamiliarCarryRules.LEFT_HAND_RACE),
        )
        val familiar = ranked[EquipmentSlot.FAMILIAR] ?: emptyList()
        assertEquals(listOf("allowed-offhand"), familiar.map { it.first })
    }
}
