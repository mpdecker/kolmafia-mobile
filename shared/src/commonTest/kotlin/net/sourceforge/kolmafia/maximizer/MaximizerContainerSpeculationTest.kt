package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.FoldGroup
import net.sourceforge.kolmafia.data.FoldGroupDatabase
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
import kotlin.test.assertNull

class MaximizerContainerSpeculationTest {

    @BeforeTest
    fun setup() {
        ModifierDatabase.resetForTest()
        FoldGroupDatabase.resetForTest()
    }

    @AfterTest
    fun teardown() {
        ModifierDatabase.resetForTest()
        FoldGroupDatabase.resetForTest()
    }

    @Test
    fun tryContainers_containerSeedWinsLoadout() {
        ModifierDatabase.injectForTest("Item", "buddy-bjorn", "Meat Drop: +40")
        ModifierDatabase.injectForTest("Item", "filler-acc", "Meat Drop: +1")
        val db = stubDb(
            801 to container(801, "buddy-bjorn"),
            802 to accessory(802, "filler-acc"),
            modifiers = mapOf(
                "buddy-bjorn" to "Meat Drop: +40",
                "filler-acc" to "Meat Drop: +1",
            ),
        )
        val spec = MaximizeSpec(
            primary = DoubleModifier.MEATDROP,
            evaluator = Evaluator("meat"),
        )
        val buckets = SlotList<MaximizerRankedItem>()
        buckets.get(MaximizerSlot.CONTAINER).add(
            MaximizerRankedItem(801, "buddy-bjorn", 40.0, MaximizerCheckedItem(801, "buddy-bjorn", initial = 1)),
        )
        val candidates = mapOf(
            EquipmentSlot.CONTAINER to listOf("buddy-bjorn" to 40.0),
            EquipmentSlot.ACC1 to listOf("filler-acc" to 1.0),
        )

        val result = MaximizerContainerSpeculation.tryContainers(
            spec = spec,
            baseState = CharacterState(),
            candidatesBySlot = candidates,
            rankedBuckets = buckets,
            budget = ComboBudget(20),
            currentBest = emptyMap(),
            gameDatabase = db,
        )

        assertEquals("buddy-bjorn", result.bestPerSlot[EquipmentSlot.CONTAINER]?.first)
    }

    @Test
    fun tryContainers_skipsWhenContainerAlreadySet() {
        val db = stubDb()
        val spec = MaximizeSpec(
            primary = DoubleModifier.MEATDROP,
            evaluator = Evaluator("meat"),
        )
        val preset = mapOf(EquipmentSlot.CONTAINER to ("preset-container" to 99.0))
        val candidates = mapOf(
            EquipmentSlot.CONTAINER to listOf("other-container" to 50.0),
        )

        val result = MaximizerContainerSpeculation.tryContainers(
            spec = spec,
            baseState = CharacterState(),
            candidatesBySlot = candidates,
            rankedBuckets = SlotList(),
            budget = ComboBudget(10),
            currentBest = preset,
            gameDatabase = db,
        )

        assertEquals("preset-container", result.bestPerSlot[EquipmentSlot.CONTAINER]?.first)
    }

    @Test
    fun tryContainers_foldPeerBlocksSecondContainerCandidate() {
        FoldGroupDatabase.registerGroupForTest(
            FoldGroup(hpDamagePct = 5, items = listOf("fold-container-a", "fold-container-b")),
        )
        ModifierDatabase.injectForTest("Item", "fold-container-a", "Meat Drop: +10")
        ModifierDatabase.injectForTest("Item", "fold-container-b", "Meat Drop: +20")
        ModifierDatabase.injectForTest("Item", "fold-hat", "Meat Drop: +1")
        val db = stubDb(
            811 to container(811, "fold-container-a"),
            812 to container(812, "fold-container-b"),
            813 to hat(813, "fold-hat"),
            modifiers = mapOf(
                "fold-container-a" to "Meat Drop: +10",
                "fold-container-b" to "Meat Drop: +20",
                "fold-hat" to "Meat Drop: +1",
            ),
        )
        val spec = MaximizeSpec(
            primary = DoubleModifier.MEATDROP,
            evaluator = Evaluator("meat"),
        )
        val buckets = SlotList<MaximizerRankedItem>()
        buckets.get(MaximizerSlot.CONTAINER).addAll(
            listOf(
                MaximizerRankedItem(811, "fold-container-a", 10.0, MaximizerCheckedItem(811, "fold-container-a", initial = 1)),
                MaximizerRankedItem(812, "fold-container-b", 20.0, MaximizerCheckedItem(812, "fold-container-b", initial = 1)),
            ),
        )
        val candidates = mapOf(
            EquipmentSlot.CONTAINER to listOf(
                "fold-container-a" to 10.0,
                "fold-container-b" to 20.0,
            ),
            EquipmentSlot.HAT to listOf("fold-hat" to 1.0),
        )
        val preset = mapOf(
            EquipmentSlot.HAT to ("fold-container-a" to 10.0),
        )

        val result = MaximizerContainerSpeculation.tryContainers(
            spec = spec,
            baseState = CharacterState(),
            candidatesBySlot = candidates,
            rankedBuckets = buckets,
            budget = ComboBudget(20),
            currentBest = preset,
            gameDatabase = db,
            foldablesEnabled = true,
        )

        assertNull(result.bestPerSlot[EquipmentSlot.CONTAINER])
    }

    @Test
    fun tryContainers_buddyBjornLoopsHigherBjornifyFamiliar() {
        ModifierDatabase.injectForTest("Familiar", "Frumious Bandersnatch", "Meat Drop: +5")
        ModifierDatabase.injectForTest("Familiar", "Miniature Donkey", "Meat Drop: +30")
        ModifierDatabase.injectForTest("Item", "Buddy Bjorn", "Meat Drop: +1")
        ModifierDatabase.injectForTest("Item", "filler-acc", "Meat Drop: +1")
        val db = stubDb(
            801 to container(801, "Buddy Bjorn"),
            802 to accessory(802, "filler-acc"),
            modifiers = mapOf(
                "Buddy Bjorn" to "Meat Drop: +1",
                "filler-acc" to "Meat Drop: +1",
            ),
        )
        val spec = MaximizeSpec(
            primary = DoubleModifier.MEATDROP,
            evaluator = Evaluator("meat"),
        )
        val buckets = SlotList<MaximizerRankedItem>()
        buckets.get(MaximizerSlot.CONTAINER).add(
            MaximizerRankedItem(801, "Buddy Bjorn", 1.0, MaximizerCheckedItem(801, "Buddy Bjorn", initial = 1)),
        )
        val candidates = mapOf(
            EquipmentSlot.CONTAINER to listOf("Buddy Bjorn" to 1.0),
            EquipmentSlot.ACC1 to listOf("filler-acc" to 1.0),
        )
        val scoreFamiliar: (String?) -> Double = { race ->
            race?.let {
                ModifierDatabase.getFamiliar(it)?.let { entry ->
                    net.sourceforge.kolmafia.modifiers.ModifierParser.parse(entry.modifiers)
                        .get(DoubleModifier.MEATDROP)
                }
            } ?: 0.0
        }

        val result = MaximizerContainerSpeculation.tryContainers(
            spec = spec,
            baseState = CharacterState(),
            candidatesBySlot = candidates,
            rankedBuckets = buckets,
            budget = ComboBudget(30),
            currentBest = emptyMap(),
            gameDatabase = db,
            usableBjornFamiliars = listOf("Frumious Bandersnatch", "Miniature Donkey"),
            scoreFamiliar = scoreFamiliar,
        )

        assertEquals("Buddy Bjorn", result.bestPerSlot[EquipmentSlot.CONTAINER]?.first)
        assertEquals("Miniature Donkey", result.bjornifiedRace)
    }

    private fun container(id: Int, name: String) =
        ItemData(id, name, "", "", ItemPrimaryUse.CONTAINER, emptySet(), setOf('t'), 0, null)

    private fun accessory(id: Int, name: String) =
        ItemData(id, name, "", "", ItemPrimaryUse.ACCESSORY, emptySet(), setOf('t'), 0, null)

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
