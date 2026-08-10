package net.sourceforge.kolmafia.maximizer

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.ModifierEntry
import net.sourceforge.kolmafia.data.OutfitData
import net.sourceforge.kolmafia.data.OutfitDatabase
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MaximizerOutfitSpeculationTest {

    @BeforeTest
    fun setup() {
        ModifierDatabase.resetForTest()
        OutfitDatabase.resetForTest()
    }

    @AfterTest
    fun teardown() {
        ModifierDatabase.resetForTest()
        OutfitDatabase.resetForTest()
    }

    @Test
    fun survivingUsefulOutfits_keepsOutfitsWithAutomaticPieces() = runBlocking {
        registerOutfit(1, "strong-suit", listOf("strong-a", "strong-b"), "Meat Drop: +50")
        val db = stubDb(
            registerAccessory(811, "strong-a", "Meat Drop: +10"),
            registerAccessory(812, "strong-b", "Meat Drop: +10"),
            modifiers = mapOf(
                "strong-a" to "Meat Drop: +10",
                "strong-b" to "Meat Drop: +10",
            ),
        )
        val spec = MaximizeSpec(primary = DoubleModifier.MEATDROP, evaluator = Evaluator("meat"))
        val autoContext = MaximizerAutoContext.from(spec.evaluator)
        val buckets = enumerateWithAuto(db, spec, setOf(811, 812), autoContext)
        MaximizerOutfitAdjustments.apply(buckets, autoContext.usefulOutfits, spec, CharacterState(), db)

        val surviving = MaximizerOutfitSpeculation.survivingUsefulOutfits(buckets, autoContext.usefulOutfits)
        assertEquals(1, surviving.size)
        assertEquals("strong-suit", surviving.first().name)
    }

    @Test
    fun survivingUsefulOutfits_skipsClearedOutfits() = runBlocking {
        registerOutfit(1, "weak-suit", listOf("outfit-a", "outfit-b"), "Meat Drop: +5")
        val db = stubDb(
            registerAccessory(801, "outfit-a", "Meat Drop: +10"),
            registerAccessory(802, "outfit-b", "Meat Drop: +10"),
            registerAccessory(803, "filler-a", "Meat Drop: +30"),
            registerAccessory(804, "filler-b", "Meat Drop: +28"),
            modifiers = mapOf(
                "outfit-a" to "Meat Drop: +10",
                "outfit-b" to "Meat Drop: +10",
                "filler-a" to "Meat Drop: +30",
                "filler-b" to "Meat Drop: +28",
            ),
        )
        val spec = MaximizeSpec(primary = DoubleModifier.MEATDROP, evaluator = Evaluator("meat"))
        val autoContext = MaximizerAutoContext.from(spec.evaluator)
        val buckets = enumerateWithAuto(db, spec, setOf(801, 802, 803, 804), autoContext)
        MaximizerOutfitAdjustments.apply(buckets, autoContext.usefulOutfits, spec, CharacterState(), db)

        val surviving = MaximizerOutfitSpeculation.survivingUsefulOutfits(buckets, autoContext.usefulOutfits)
        assertTrue(surviving.isEmpty())
    }

    @Test
    fun buildOutfitAssignment_mapsThreeAccessoriesToAccSlots() = runBlocking {
        registerOutfit(
            3,
            "acc-suit",
            listOf("acc-piece-a", "acc-piece-b", "acc-piece-c"),
            "Meat Drop: +5",
        )
        val db = stubDb(
            registerAccessory(821, "acc-piece-a", "Meat Drop: +10"),
            registerAccessory(822, "acc-piece-b", "Meat Drop: +10"),
            registerAccessory(823, "acc-piece-c", "Meat Drop: +10"),
            modifiers = mapOf(
                "acc-piece-a" to "Meat Drop: +10",
                "acc-piece-b" to "Meat Drop: +10",
                "acc-piece-c" to "Meat Drop: +10",
            ),
        )
        val spec = MaximizeSpec(primary = DoubleModifier.MEATDROP, evaluator = Evaluator("meat"))
        val autoContext = MaximizerAutoContext.from(spec.evaluator)
        val buckets = enumerateWithAuto(db, spec, setOf(821, 822, 823), autoContext)

        val assignment = MaximizerOutfitSlots.buildOutfitAssignment(
            autoContext.usefulOutfits.first(),
            buckets,
            spec,
            db,
        )

        assertEquals("acc-piece-a", assignment?.get(EquipmentSlot.ACC1))
        assertEquals("acc-piece-b", assignment?.get(EquipmentSlot.ACC2))
        assertEquals("acc-piece-c", assignment?.get(EquipmentSlot.ACC3))
    }

    @Test
    fun tryOutfits_prefersOutfitSeedOverGreedyFillers() = runBlocking {
        registerOutfit(2, "strong-suit", listOf("strong-a", "strong-b"), "Meat Drop: +50")
        val db = stubDb(
            registerAccessory(811, "strong-a", "Meat Drop: +10"),
            registerAccessory(812, "strong-b", "Meat Drop: +10"),
            registerAccessory(813, "filler-a", "Meat Drop: +25"),
            registerAccessory(814, "filler-b", "Meat Drop: +24"),
            modifiers = mapOf(
                "strong-a" to "Meat Drop: +10",
                "strong-b" to "Meat Drop: +10",
                "filler-a" to "Meat Drop: +25",
                "filler-b" to "Meat Drop: +24",
            ),
        )
        val spec = MaximizeSpec(primary = DoubleModifier.MEATDROP, evaluator = Evaluator("meat"))
        val autoContext = MaximizerAutoContext.from(spec.evaluator)
        val buckets = enumerateWithAuto(db, spec, setOf(811, 812, 813, 814), autoContext)
        MaximizerOutfitAdjustments.apply(buckets, autoContext.usefulOutfits, spec, CharacterState(), db)
        val surviving = MaximizerOutfitSpeculation.survivingUsefulOutfits(buckets, autoContext.usefulOutfits)
        val state = CharacterState()
        val greedyBest = mapOf(
            EquipmentSlot.ACC1 to ("filler-a" to 25.0),
            EquipmentSlot.ACC2 to ("filler-b" to 24.0),
        )
        val candidates = mapOf(
            EquipmentSlot.ACC1 to listOf(
                "filler-a" to 25.0,
                "strong-a" to 10.0,
            ),
            EquipmentSlot.ACC2 to listOf(
                "filler-b" to 24.0,
                "strong-b" to 10.0,
            ),
        )
        val budget = ComboBudget(100)

        val result = MaximizerOutfitSpeculation.tryOutfits(
            spec = spec,
            baseState = state,
            survivingOutfits = surviving,
            rankedBuckets = buckets,
            candidatesBySlot = candidates,
            budget = budget,
            currentBest = greedyBest,
            gameDatabase = db,
        )

        assertEquals("strong-a", result[EquipmentSlot.ACC1]?.first)
        assertEquals("strong-b", result[EquipmentSlot.ACC2]?.first)
        val greedyScore = MaximizerSpeculation.scoreLoadout(state, greedyBest, spec.evaluator)
        val resultScore = MaximizerSpeculation.scoreLoadout(state, result, spec.evaluator)
        assertTrue(resultScore > greedyScore)
    }

    @Test
    fun tryOutfits_skipsWhenNoSurvivingOutfits() = runBlocking {
        registerOutfit(1, "weak-suit", listOf("outfit-a", "outfit-b"), "Meat Drop: +5")
        val db = stubDb(
            registerAccessory(801, "outfit-a", "Meat Drop: +10"),
            registerAccessory(802, "outfit-b", "Meat Drop: +10"),
            registerAccessory(803, "filler-a", "Meat Drop: +30"),
            registerAccessory(804, "filler-b", "Meat Drop: +28"),
            modifiers = mapOf(
                "outfit-a" to "Meat Drop: +10",
                "outfit-b" to "Meat Drop: +10",
                "filler-a" to "Meat Drop: +30",
                "filler-b" to "Meat Drop: +28",
            ),
        )
        val spec = MaximizeSpec(primary = DoubleModifier.MEATDROP, evaluator = Evaluator("meat"))
        val autoContext = MaximizerAutoContext.from(spec.evaluator)
        val buckets = enumerateWithAuto(db, spec, setOf(801, 802, 803, 804), autoContext)
        MaximizerOutfitAdjustments.apply(buckets, autoContext.usefulOutfits, spec, CharacterState(), db)
        val surviving = MaximizerOutfitSpeculation.survivingUsefulOutfits(buckets, autoContext.usefulOutfits)
        val greedyBest = mapOf(
            EquipmentSlot.ACC1 to ("filler-a" to 30.0),
            EquipmentSlot.ACC2 to ("filler-b" to 28.0),
        )

        val result = MaximizerOutfitSpeculation.tryOutfits(
            spec = spec,
            baseState = CharacterState(),
            survivingOutfits = surviving,
            rankedBuckets = buckets,
            candidatesBySlot = emptyMap(),
            budget = ComboBudget(10),
            currentBest = greedyBest,
            gameDatabase = db,
        )

        assertEquals(greedyBest, result)
        assertTrue(surviving.isEmpty())
    }

    private fun registerOutfit(
        id: Int,
        name: String,
        pieces: List<String>,
        outfitMods: String,
    ) {
        ModifierDatabase.injectForTest("Outfit", name, outfitMods)
        OutfitDatabase.registerStatic(
            OutfitData(
                id = id,
                name = name,
                image = "",
                equipment = pieces,
                halloweenDrops = emptyList(),
            ),
        )
    }

    private fun registerAccessory(
        id: Int,
        name: String,
        mods: String,
    ): Pair<Int, ItemData> {
        ModifierDatabase.injectForTest("Item", name, mods)
        return id to accessory(id, name)
    }

    private fun enumerateWithAuto(
        db: GameDatabase,
        spec: MaximizeSpec,
        candidateIds: Set<Int>,
        autoContext: MaximizerAutoContext,
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
            autoContext = autoContext,
        )

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
