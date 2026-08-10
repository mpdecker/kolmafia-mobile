package net.sourceforge.kolmafia.maximizer

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
import kotlin.test.assertNull

class MaximizerHatSpeculationTest {

    @BeforeTest
    fun setup() {
        ModifierDatabase.resetForTest()
    }

    @AfterTest
    fun teardown() {
        ModifierDatabase.resetForTest()
    }

    @Test
    fun tryHats_crownLoopsHigherEnthroneFamiliar() {
        ModifierDatabase.injectForTest("Familiar", "Frumious Bandersnatch", "Meat Drop: +5")
        ModifierDatabase.injectForTest("Familiar", "Miniature Donkey", "Meat Drop: +30")
        ModifierDatabase.injectForTest("Item", "Crown of Thrones", "Meat Drop: +1")
        ModifierDatabase.injectForTest("Item", "filler-acc", "Meat Drop: +1")
        val db = stubDb(
            701 to hat(701, "Crown of Thrones"),
            702 to accessory(702, "filler-acc"),
            modifiers = mapOf(
                "Crown of Thrones" to "Meat Drop: +1",
                "filler-acc" to "Meat Drop: +1",
            ),
        )
        val spec = MaximizeSpec(
            primary = DoubleModifier.MEATDROP,
            evaluator = Evaluator("meat"),
        )
        val buckets = SlotList<MaximizerRankedItem>()
        buckets.get(MaximizerSlot.HAT).add(
            MaximizerRankedItem(701, "Crown of Thrones", 1.0, MaximizerCheckedItem(701, "Crown of Thrones", initial = 1)),
        )
        val candidates = mapOf(
            EquipmentSlot.HAT to listOf("Crown of Thrones" to 1.0),
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

        val result = MaximizerHatSpeculation.tryHats(
            spec = spec,
            baseState = CharacterState(),
            candidatesBySlot = candidates,
            rankedBuckets = buckets,
            budget = ComboBudget(30),
            currentBest = emptyMap(),
            gameDatabase = db,
            usableEnthroneFamiliars = listOf("Frumious Bandersnatch", "Miniature Donkey"),
            scoreFamiliar = scoreFamiliar,
        )

        assertEquals("Crown of Thrones", result.bestPerSlot[EquipmentSlot.HAT]?.first)
        assertEquals("Miniature Donkey", result.enthronedRace)
    }

    @Test
    fun tryHats_skipsWhenHatAlreadySet() {
        val db = stubDb()
        val spec = MaximizeSpec(
            primary = DoubleModifier.MEATDROP,
            evaluator = Evaluator("meat"),
        )
        val preset = mapOf(EquipmentSlot.HAT to ("preset-hat" to 99.0))
        val candidates = mapOf(
            EquipmentSlot.HAT to listOf("Crown of Thrones" to 50.0),
        )

        val result = MaximizerHatSpeculation.tryHats(
            spec = spec,
            baseState = CharacterState(),
            candidatesBySlot = candidates,
            rankedBuckets = SlotList(),
            budget = ComboBudget(10),
            currentBest = preset,
            gameDatabase = db,
        )

        assertEquals("preset-hat", result.bestPerSlot[EquipmentSlot.HAT]?.first)
    }

    @Test
    fun tryHats_excludesActiveBjornRaceFromCrownLoop() {
        ModifierDatabase.injectForTest("Familiar", "Shared Familiar", "Meat Drop: +20")
        ModifierDatabase.injectForTest("Familiar", "Better Familiar", "Meat Drop: +40")
        ModifierDatabase.injectForTest("Item", "Crown of Thrones", "Meat Drop: +1")
        ModifierDatabase.injectForTest("Item", "filler-acc", "Meat Drop: +1")
        val db = stubDb(
            711 to hat(711, "Crown of Thrones"),
            712 to accessory(712, "filler-acc"),
            modifiers = mapOf(
                "Crown of Thrones" to "Meat Drop: +1",
                "filler-acc" to "Meat Drop: +1",
            ),
        )
        val spec = MaximizeSpec(
            primary = DoubleModifier.MEATDROP,
            evaluator = Evaluator("meat"),
        )
        val candidates = mapOf(
            EquipmentSlot.HAT to listOf("Crown of Thrones" to 1.0),
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

        val result = MaximizerHatSpeculation.tryHats(
            spec = spec,
            baseState = CharacterState(),
            candidatesBySlot = candidates,
            rankedBuckets = SlotList(),
            budget = ComboBudget(30),
            currentBest = emptyMap(),
            gameDatabase = db,
            usableEnthroneFamiliars = listOf("Shared Familiar", "Better Familiar"),
            activeBjornRace = "Shared Familiar",
            scoreFamiliar = scoreFamiliar,
        )

        assertEquals("Better Familiar", result.enthronedRace)
    }

    @Test
    fun tryHats_noEnthroneBranchesWhenCrownFilteredOut() {
        ModifierDatabase.injectForTest("Familiar", "Only Familiar", "Meat Drop: +20")
        ModifierDatabase.injectForTest("Item", "Crown of Thrones", "Meat Drop: +1")
        val db = stubDb(
            721 to hat(721, "Crown of Thrones"),
            modifiers = mapOf("Crown of Thrones" to "Meat Drop: +1"),
        )
        val spec = MaximizeSpec(
            primary = DoubleModifier.MEATDROP,
            evaluator = Evaluator("meat"),
        )
        val candidates = mapOf(
            EquipmentSlot.HAT to listOf("Crown of Thrones" to 1.0),
        )

        val result = MaximizerHatSpeculation.tryHats(
            spec = spec,
            baseState = CharacterState(),
            candidatesBySlot = candidates,
            rankedBuckets = SlotList(),
            budget = ComboBudget(10),
            currentBest = emptyMap(),
            gameDatabase = db,
            usableEnthroneFamiliars = listOf("Only Familiar"),
            activeBjornRace = "Only Familiar",
            scoreFamiliar = { 0.0 },
        )

        assertNull(result.enthronedRace)
    }

    private fun hat(id: Int, name: String) =
        ItemData(id, name, "", "", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null)

    private fun accessory(id: Int, name: String) =
        ItemData(id, name, "", "", ItemPrimaryUse.ACCESSORY, emptySet(), setOf('t'), 0, null)

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
