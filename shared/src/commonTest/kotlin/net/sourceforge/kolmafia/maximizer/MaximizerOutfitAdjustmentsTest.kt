package net.sourceforge.kolmafia.maximizer

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterState
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MaximizerOutfitAdjustmentsTest {

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
    fun weakOutfit_clearsAutomaticWhenFillersBeatPieces() = runBlocking {
        registerOutfit(
            id = 1,
            name = "weak-suit",
            pieces = listOf("outfit-a", "outfit-b"),
            outfitMods = "Meat Drop: +5",
        )
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
        val spec = MaximizeSpec(
            primary = DoubleModifier.MEATDROP,
            evaluator = Evaluator("meat"),
        )
        val autoContext = MaximizerAutoContext.from(spec.evaluator)
        val buckets = enumerateWithAuto(db, spec, setOf(801, 802, 803, 804), autoContext)
        assertTrue(buckets.allItems(MaximizerSlot.ACC1).any { it.name == "outfit-a" && it.automatic })
        assertTrue(buckets.allItems(MaximizerSlot.ACC1).any { it.name == "outfit-b" && it.automatic })

        MaximizerOutfitAdjustments.apply(buckets, autoContext.usefulOutfits, spec, CharacterState(), db)

        assertFalse(buckets.allItems(MaximizerSlot.ACC1).any { it.name == "outfit-a" && it.automatic })
        assertFalse(buckets.allItems(MaximizerSlot.ACC1).any { it.name == "outfit-b" && it.automatic })
    }

    @Test
    fun strongOutfit_keepsAutomaticWhenOutfitBonusWins() = runBlocking {
        registerOutfit(
            id = 2,
            name = "strong-suit",
            pieces = listOf("strong-a", "strong-b"),
            outfitMods = "Meat Drop: +50",
        )
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
        val spec = MaximizeSpec(
            primary = DoubleModifier.MEATDROP,
            evaluator = Evaluator("meat"),
        )
        val autoContext = MaximizerAutoContext.from(spec.evaluator)
        val buckets = enumerateWithAuto(db, spec, setOf(811, 812, 813, 814), autoContext)

        MaximizerOutfitAdjustments.apply(buckets, autoContext.usefulOutfits, spec, CharacterState(), db)

        assertTrue(buckets.allItems(MaximizerSlot.ACC1).any { it.name == "strong-a" && it.automatic })
        assertTrue(buckets.allItems(MaximizerSlot.ACC1).any { it.name == "strong-b" && it.automatic })
    }

    @Test
    fun threePieceAccessoryOutfit_usesThirdSecondFirstBestCompare() = runBlocking {
        registerOutfit(
            id = 3,
            name = "acc-suit",
            pieces = listOf("acc-piece-a", "acc-piece-b", "acc-piece-c"),
            outfitMods = "Meat Drop: +5",
        )
        val db = stubDb(
            registerAccessory(821, "acc-piece-a", "Meat Drop: +10"),
            registerAccessory(822, "acc-piece-b", "Meat Drop: +10"),
            registerAccessory(823, "acc-piece-c", "Meat Drop: +10"),
            registerAccessory(824, "rank-1", "Meat Drop: +30"),
            registerAccessory(825, "rank-2", "Meat Drop: +29"),
            registerAccessory(826, "rank-3", "Meat Drop: +28"),
            registerAccessory(827, "rank-4", "Meat Drop: +27"),
            registerAccessory(828, "rank-5", "Meat Drop: +26"),
            modifiers = mapOf(
                "acc-piece-a" to "Meat Drop: +10",
                "acc-piece-b" to "Meat Drop: +10",
                "acc-piece-c" to "Meat Drop: +10",
                "rank-1" to "Meat Drop: +30",
                "rank-2" to "Meat Drop: +29",
                "rank-3" to "Meat Drop: +28",
                "rank-4" to "Meat Drop: +27",
                "rank-5" to "Meat Drop: +26",
            ),
        )
        val spec = MaximizeSpec(
            primary = DoubleModifier.MEATDROP,
            evaluator = Evaluator("meat"),
        )
        val autoContext = MaximizerAutoContext.from(spec.evaluator)
        val buckets = enumerateWithAuto(
            db,
            spec,
            setOf(821, 822, 823, 824, 825, 826, 827, 828),
            autoContext,
        )

        MaximizerOutfitAdjustments.apply(buckets, autoContext.usefulOutfits, spec, CharacterState(), db)

        assertFalse(buckets.allItems(MaximizerSlot.ACC1).any { it.name == "acc-piece-a" && it.automatic })
        assertFalse(buckets.allItems(MaximizerSlot.ACC1).any { it.name == "acc-piece-b" && it.automatic })
        assertFalse(buckets.allItems(MaximizerSlot.ACC1).any { it.name == "acc-piece-c" && it.automatic })
    }

    @Test
    fun mergeBuckets_respectsClearedOutfitAutomatic() = runBlocking {
        registerOutfit(
            id = 4,
            name = "merge-suit",
            pieces = listOf("merge-a", "merge-b"),
            outfitMods = "Meat Drop: +5",
        )
        val db = stubDb(
            registerAccessory(831, "merge-a", "Meat Drop: +10"),
            registerAccessory(832, "merge-b", "Meat Drop: +10"),
            registerAccessory(833, "filler-a", "Meat Drop: +30"),
            registerAccessory(834, "filler-b", "Meat Drop: +28"),
            modifiers = mapOf(
                "merge-a" to "Meat Drop: +10",
                "merge-b" to "Meat Drop: +10",
                "filler-a" to "Meat Drop: +30",
                "filler-b" to "Meat Drop: +28",
            ),
        )
        val spec = MaximizeSpec(
            primary = DoubleModifier.MEATDROP,
            evaluator = Evaluator("meat"),
        )
        val autoContext = MaximizerAutoContext.from(spec.evaluator)
        val buckets = enumerateWithAuto(db, spec, setOf(831, 832, 833, 834), autoContext)
        MaximizerOutfitAdjustments.apply(buckets, autoContext.usefulOutfits, spec, CharacterState(), db)
        val merged = MaximizerEquipmentEnumerator.mergeBuckets(
            buckets,
            listOf(MaximizerSlot.ACC1),
            limit = 1,
        )
        assertFalse("merge-a" in merged.map { it.first })
        assertFalse("merge-b" in merged.map { it.first })
        assertEquals("filler-a", merged.first().first)
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
