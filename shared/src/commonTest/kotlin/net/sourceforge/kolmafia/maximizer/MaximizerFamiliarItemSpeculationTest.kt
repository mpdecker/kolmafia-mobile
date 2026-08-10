package net.sourceforge.kolmafia.maximizer

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.ModifierEntry
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MaximizerFamiliarItemSpeculationTest {

    @BeforeTest
    fun setup() {
        ModifierDatabase.resetForTest()
    }

    @AfterTest
    fun teardown() {
        ModifierDatabase.resetForTest()
    }

    @Test
    fun tryFamiliarItems_picksFamiliarSeed() = runBlocking {
        ModifierDatabase.injectForTest("Item", "fam-toy", "Meat Drop: +50")
        val db = stubDb(
            951 to familiar(951, "fam-toy"),
            modifiers = mapOf("fam-toy" to "Meat Drop: +50"),
        )
        val spec = MaximizeSpec(primary = DoubleModifier.MEATDROP, evaluator = Evaluator("meat"))
        val buckets = enumerate(db, spec, setOf(951))
        val candidates = mapOf(
            EquipmentSlot.FAMILIAR to listOf("fam-toy" to 50.0),
        )
        val state = CharacterState()

        val result = MaximizerFamiliarItemSpeculation.tryFamiliarItems(
            spec = spec,
            baseState = state,
            candidatesBySlot = candidates,
            rankedBuckets = buckets,
            budget = ComboBudget(50),
            currentBest = emptyMap(),
            gameDatabase = db,
        )

        assertEquals("fam-toy", result[EquipmentSlot.FAMILIAR]?.first)
        val score = MaximizerSpeculation.scoreLoadout(state, result, spec.evaluator)
        assertTrue(score > 0.0)
    }

    @Test
    fun tryFamiliarItems_skipsWhenFamiliarAlreadySet() = runBlocking {
        ModifierDatabase.injectForTest("Item", "fam-toy", "Meat Drop: +50")
        ModifierDatabase.injectForTest("Item", "fam-other", "Meat Drop: +10")
        val db = stubDb(
            951 to familiar(951, "fam-toy"),
            952 to familiar(952, "fam-other"),
            modifiers = mapOf(
                "fam-toy" to "Meat Drop: +50",
                "fam-other" to "Meat Drop: +10",
            ),
        )
        val spec = MaximizeSpec(primary = DoubleModifier.MEATDROP, evaluator = Evaluator("meat"))
        val buckets = enumerate(db, spec, setOf(951, 952))
        val currentBest = mapOf(EquipmentSlot.FAMILIAR to ("fam-toy" to 50.0))
        val candidates = mapOf(
            EquipmentSlot.FAMILIAR to listOf(
                "fam-toy" to 50.0,
                "fam-other" to 10.0,
            ),
        )

        val result = MaximizerFamiliarItemSpeculation.tryFamiliarItems(
            spec = spec,
            baseState = CharacterState(),
            candidatesBySlot = candidates,
            rankedBuckets = buckets,
            budget = ComboBudget(50),
            currentBest = currentBest,
            gameDatabase = db,
        )

        assertEquals(currentBest, result)
    }

    @Test
    fun tryFamiliarItems_dedupesWornElsewhere() = runBlocking {
        ModifierDatabase.injectForTest("Item", "carry-hat", "Meat Drop: +50")
        val db = stubDb(
            961 to hat(961, "carry-hat"),
            modifiers = mapOf("carry-hat" to "Meat Drop: +50"),
        )
        val spec = MaximizeSpec(primary = DoubleModifier.MEATDROP, evaluator = Evaluator("meat"))
        val buckets = enumerate(db, spec, setOf(961))
        val currentBest = mapOf(EquipmentSlot.HAT to ("carry-hat" to 50.0))
        val candidates = mapOf(
            EquipmentSlot.FAMILIAR to listOf("carry-hat" to 50.0),
        )

        val result = MaximizerFamiliarItemSpeculation.tryFamiliarItems(
            spec = spec,
            baseState = CharacterState(),
            candidatesBySlot = candidates,
            rankedBuckets = buckets,
            budget = ComboBudget(50),
            currentBest = currentBest,
            gameDatabase = db,
        )

        assertEquals(currentBest, result)
        assertEquals(null, result[EquipmentSlot.FAMILIAR])
    }

    @Test
    fun tryFamiliarItems_sharedBudgetStopsIteration() = runBlocking {
        ModifierDatabase.injectForTest("Item", "fam-a", "Meat Drop: +30")
        ModifierDatabase.injectForTest("Item", "fam-b", "Meat Drop: +20")
        val db = stubDb(
            971 to familiar(971, "fam-a"),
            972 to familiar(972, "fam-b"),
            modifiers = mapOf(
                "fam-a" to "Meat Drop: +30",
                "fam-b" to "Meat Drop: +20",
            ),
        )
        val spec = MaximizeSpec(primary = DoubleModifier.MEATDROP, evaluator = Evaluator("meat"))
        val buckets = enumerate(db, spec, setOf(971, 972))
        val candidates = mapOf(
            EquipmentSlot.FAMILIAR to listOf(
                "fam-a" to 30.0,
                "fam-b" to 20.0,
            ),
        )
        val budget = ComboBudget(1)

        val result = MaximizerFamiliarItemSpeculation.tryFamiliarItems(
            spec = spec,
            baseState = CharacterState(),
            candidatesBySlot = candidates,
            rankedBuckets = buckets,
            budget = budget,
            currentBest = emptyMap(),
            gameDatabase = db,
        )

        assertTrue(budget.exhausted())
        assertEquals("fam-a", result[EquipmentSlot.FAMILIAR]?.first)
    }

    @Test
    fun availableCount_subtractsUsesInOtherSlots() {
        val assignment = mapOf(
            EquipmentSlot.HAT to ("shared-item" to 1.0),
            EquipmentSlot.FAMILIAR to ("shared-item" to 1.0),
        )
        assertEquals(0, MaximizerFamiliarItemSpeculation.availableCount("shared-item", assignment, 1))
    }

    private fun enumerate(
        db: GameDatabase,
        spec: MaximizeSpec,
        candidateIds: Set<Int>,
    ): SlotList<MaximizerRankedItem> =
        MaximizerEquipmentEnumerator.enumerate(
            candidateIds = candidateIds,
            spec = spec,
            gameDatabase = db,
            checkedItem = { itemId -> MaximizerCheckedItem(itemId, db.item(itemId)?.name ?: "", initial = 1) },
            scoreItem = { name, ev ->
                val entry = db.itemModifier(name) ?: return@enumerate 0.0
                ev.getItemContribution(net.sourceforge.kolmafia.modifiers.ModifierParser.parse(entry.modifiers))
            },
            itemMeetsConstraints = { _, _ -> true },
        )

    private fun familiar(id: Int, name: String) =
        ItemData(id, name, "", "", ItemPrimaryUse.FAMILIAR, emptySet(), setOf('t'), 0, null)

    private fun hat(id: Int, name: String) =
        ItemData(id, name, "", "", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null)

    private fun stubDb(
        vararg items: Pair<Int, ItemData>,
        modifiers: Map<String, String> = emptyMap(),
    ): GameDatabase = object : GameDatabase() {
        private val byId = items.toMap()
        override fun item(id: Int): ItemData? = byId[id]
        override fun item(name: String): ItemData? =
            byId.values.find { it.name.equals(name, ignoreCase = true) }
        override fun itemModifier(name: String): ModifierEntry? =
            modifiers.entries.find { it.key.equals(name, ignoreCase = true) }
                ?.let { ModifierEntry("Item", it.key, it.value) }
    }
}
