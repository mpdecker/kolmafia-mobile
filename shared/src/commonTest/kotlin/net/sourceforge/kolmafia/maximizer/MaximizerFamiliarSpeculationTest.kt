package net.sourceforge.kolmafia.maximizer

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.ModifierEntry
import net.sourceforge.kolmafia.data.OutfitDatabase
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MaximizerFamiliarSpeculationTest {

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
    fun tryAll_picksFamiliarWhoseCarryItemWins() = runBlocking {
        ModifierDatabase.injectForTest("Familiar", "Mad Hatrack", "Meat Drop: +1")
        ModifierDatabase.injectForTest("Familiar", "Miniature Donkey", "Meat Drop: +8")
        ModifierDatabase.injectForTest("Item", "hattrack-acc", "Meat Drop: +50")
        ModifierDatabase.injectForTest("Item", "donkey-acc", "Meat Drop: +10")
        val db = stubDb(
            901 to accessory(901, "hattrack-acc"),
            902 to accessory(902, "donkey-acc"),
            modifiers = mapOf(
                "hattrack-acc" to "Meat Drop: +50",
                "donkey-acc" to "Meat Drop: +10",
            ),
        )
        val spec = MaximizeSpec(
            primary = DoubleModifier.MEATDROP,
            evaluator = Evaluator("meat"),
            switchFamiliars = listOf("Mad Hatrack", "Miniature Donkey"),
        )
        val state = CharacterState(familiarName = "", familiarWeight = 10)

        val result = MaximizerFamiliarSpeculation.tryAll(
            spec = spec,
            charState = state,
            survivingOutfits = emptyList(),
            rankedBuckets = SlotList(),
            refineBestPerSlot = emptyMap(),
            comboBudget = ComboBudget(50),
            thrallBonus = 0.0,
            gameDatabase = db,
            usableSwitchFamiliars = listOf("Mad Hatrack", "Miniature Donkey"),
            buildCandidates = { familiarRace, _ ->
                when (familiarRace) {
                    FamiliarCarryRules.HATRACK_RACE -> mapOf(
                        EquipmentSlot.ACC1 to listOf("hattrack-acc" to 50.0),
                    )
                    "Miniature Donkey" -> mapOf(
                        EquipmentSlot.ACC1 to listOf("donkey-acc" to 10.0),
                    )
                    else -> emptyMap()
                }
            },
            scoreFamiliar = { familiarRace ->
                familiarRace?.let {
                    ModifierDatabase.getFamiliar(it)?.let { entry ->
                        net.sourceforge.kolmafia.modifiers.ModifierParser.parse(entry.modifiers)
                            .get(DoubleModifier.MEATDROP)
                    }
                } ?: 0.0
            },
        )

        assertEquals(FamiliarCarryRules.HATRACK_RACE, result.familiarSwitch)
        assertEquals("hattrack-acc", result.bestPerSlot[EquipmentSlot.ACC1]?.first)
        val totalScore = MaximizerSpeculation.scoreLoadout(
            state, result.bestPerSlot, spec.evaluator, result.familiarBonus,
        )
        assertTrue(totalScore > 18.0)
    }

    @Test
    fun tryAll_skipsWhenNoSwitchGoal() = runBlocking {
        val db = stubDb(
            911 to accessory(911, "acc-a", "Meat Drop: +20"),
            modifiers = mapOf("acc-a" to "Meat Drop: +20"),
        )
        val spec = MaximizeSpec(
            primary = DoubleModifier.MEATDROP,
            evaluator = Evaluator("meat"),
        )
        val greedy = mapOf(EquipmentSlot.ACC1 to ("acc-a" to 20.0))
        val candidates = mapOf(EquipmentSlot.ACC1 to listOf("acc-a" to 20.0))

        val result = MaximizerFamiliarSpeculation.tryAll(
            spec = spec,
            charState = CharacterState(),
            survivingOutfits = emptyList(),
            rankedBuckets = SlotList(),
            refineBestPerSlot = greedy,
            comboBudget = ComboBudget(10),
            thrallBonus = 0.0,
            gameDatabase = db,
            usableSwitchFamiliars = emptyList(),
            buildCandidates = { _, _ -> candidates },
            scoreFamiliar = { 0.0 },
        )

        assertNull(result.familiarSwitch)
        assertEquals(0.0, result.familiarBonus)
        assertEquals("acc-a", result.bestPerSlot[EquipmentSlot.ACC1]?.first)
    }

    @Test
    fun tryAll_basePassWinsWithoutSwitch() = runBlocking {
        ModifierDatabase.injectForTest("Familiar", "Miniature Donkey", "Meat Drop: +50")
        ModifierDatabase.injectForTest("Familiar", "Mad Hatrack", "Meat Drop: +1")
        val db = stubDb()
        val spec = MaximizeSpec(
            primary = DoubleModifier.MEATDROP,
            evaluator = Evaluator("meat"),
            switchFamiliars = listOf("Mad Hatrack", "Miniature Donkey"),
        )
        val state = CharacterState(familiarName = "Miniature Donkey", familiarWeight = 10)

        val result = MaximizerFamiliarSpeculation.tryAll(
            spec = spec,
            charState = state,
            survivingOutfits = emptyList(),
            rankedBuckets = SlotList(),
            refineBestPerSlot = emptyMap(),
            comboBudget = ComboBudget(10),
            thrallBonus = 0.0,
            gameDatabase = db,
            usableSwitchFamiliars = listOf("Mad Hatrack", "Miniature Donkey"),
            buildCandidates = { _, _ -> emptyMap() },
            scoreFamiliar = { familiarRace ->
                familiarRace?.let {
                    ModifierDatabase.getFamiliar(it)?.let { entry ->
                        net.sourceforge.kolmafia.modifiers.ModifierParser.parse(entry.modifiers)
                            .get(DoubleModifier.MEATDROP)
                    }
                } ?: 0.0
            },
        )

        assertNull(result.familiarSwitch)
        assertEquals(50.0, result.familiarBonus)
    }

    @Test
    fun tryAll_sharedBudgetStopsLaterPasses() = runBlocking {
        registerOutfit(1, "budget-suit", listOf("budget-a", "budget-b"), "Meat Drop: +50")
        val db = stubDb(
            registerAccessory(931, "budget-a", "Meat Drop: +10"),
            registerAccessory(932, "budget-b", "Meat Drop: +10"),
            registerAccessory(933, "filler-a", "Meat Drop: +5"),
            modifiers = mapOf(
                "budget-a" to "Meat Drop: +10",
                "budget-b" to "Meat Drop: +10",
                "filler-a" to "Meat Drop: +5",
            ),
        )
        ModifierDatabase.injectForTest("Familiar", "Mad Hatrack", "Meat Drop: +100")
        ModifierDatabase.injectForTest("Familiar", "Miniature Donkey", "Meat Drop: +1")
        val spec = MaximizeSpec(
            primary = DoubleModifier.MEATDROP,
            evaluator = Evaluator("meat"),
            switchFamiliars = listOf("Mad Hatrack", "Miniature Donkey"),
        )
        val autoContext = MaximizerAutoContext.from(spec.evaluator)
        val buckets = enumerateWithAuto(db, spec, setOf(931, 932, 933), autoContext)
        MaximizerOutfitAdjustments.apply(buckets, autoContext.usefulOutfits, spec, CharacterState(), db)
        val surviving = MaximizerOutfitSpeculation.survivingUsefulOutfits(buckets, autoContext.usefulOutfits)
        val greedy = mapOf(EquipmentSlot.ACC1 to ("filler-a" to 5.0))
        val budget = ComboBudget(1)
        var buildCount = 0

        val result = MaximizerFamiliarSpeculation.tryAll(
            spec = spec,
            charState = CharacterState(familiarName = "Miniature Donkey"),
            survivingOutfits = surviving,
            rankedBuckets = buckets,
            refineBestPerSlot = greedy,
            comboBudget = budget,
            thrallBonus = 0.0,
            gameDatabase = db,
            usableSwitchFamiliars = listOf("Mad Hatrack", "Miniature Donkey"),
            buildCandidates = { _, _ ->
                buildCount++
                emptyMap()
            },
            scoreFamiliar = { familiarRace ->
                familiarRace?.let {
                    ModifierDatabase.getFamiliar(it)?.let { entry ->
                        net.sourceforge.kolmafia.modifiers.ModifierParser.parse(entry.modifiers)
                            .get(DoubleModifier.MEATDROP)
                    }
                } ?: 0.0
            },
        )

        assertTrue(budget.exhausted())
        assertTrue(buildCount <= 2)
        assertEquals("budget-a", result.bestPerSlot[EquipmentSlot.ACC1]?.first)
    }

    @Test
    fun tryAll_returnsWinningEnthroneAndBjornRaces() = runBlocking {
        ModifierDatabase.injectForTest("Familiar", "Frumious Bandersnatch", "Meat Drop: +5")
        ModifierDatabase.injectForTest("Familiar", "Miniature Donkey", "Meat Drop: +30")
        ModifierDatabase.injectForTest("Item", "Buddy Bjorn", "Meat Drop: +1")
        ModifierDatabase.injectForTest("Item", "Crown of Thrones", "Meat Drop: +1")
        ModifierDatabase.injectForTest("Item", "filler-acc", "Meat Drop: +1")
        val db = stubDb(
            951 to container(951, "Buddy Bjorn"),
            952 to hat(952, "Crown of Thrones"),
            953 to accessory(953, "filler-acc"),
            modifiers = mapOf(
                "Buddy Bjorn" to "Meat Drop: +1",
                "Crown of Thrones" to "Meat Drop: +1",
                "filler-acc" to "Meat Drop: +1",
            ),
        )
        val spec = MaximizeSpec(
            primary = DoubleModifier.MEATDROP,
            evaluator = Evaluator("meat"),
            enthronedFamiliars = listOf("Frumious Bandersnatch", "Miniature Donkey"),
            bjornifiedFamiliars = listOf("Frumious Bandersnatch", "Miniature Donkey"),
        )
        val candidates = mapOf(
            EquipmentSlot.CONTAINER to listOf("Buddy Bjorn" to 1.0),
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

        val result = MaximizerFamiliarSpeculation.tryAll(
            spec = spec,
            charState = CharacterState(),
            survivingOutfits = emptyList(),
            rankedBuckets = SlotList(),
            refineBestPerSlot = emptyMap(),
            comboBudget = ComboBudget(60),
            thrallBonus = 0.0,
            gameDatabase = db,
            usableSwitchFamiliars = emptyList(),
            usableEnthroneFamiliars = listOf("Frumious Bandersnatch", "Miniature Donkey"),
            usableBjornFamiliars = listOf("Frumious Bandersnatch", "Miniature Donkey"),
            buildCandidates = { _, _ -> candidates },
            scoreFamiliar = scoreFamiliar,
        )

        assertEquals("Miniature Donkey", result.bjornifiedRace)
        assertEquals("Frumious Bandersnatch", result.enthronedRace)
        assertEquals("Buddy Bjorn", result.bestPerSlot[EquipmentSlot.CONTAINER]?.first)
        assertEquals("Crown of Thrones", result.bestPerSlot[EquipmentSlot.HAT]?.first)
    }

    @Test
    fun tryAll_discoversEnthroneRaceFromCarryFamiliarsList() = runBlocking {
        ModifierDatabase.injectForTest("Familiar", "Miniature Donkey", "Meat Drop: +30")
        ModifierDatabase.injectForTest("Familiar", "Mosquito", "Meat Drop: +5")
        ModifierDatabase.injectForTest("Item", "Crown of Thrones", "Meat Drop: +1")
        ModifierDatabase.injectForTest("Item", "filler-acc", "Meat Drop: +1")
        val db = stubDb(
            952 to hat(952, "Crown of Thrones"),
            953 to accessory(953, "filler-acc"),
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
        val discovered = listOf("Mosquito", "Miniature Donkey")

        val result = MaximizerFamiliarSpeculation.tryAll(
            spec = spec,
            charState = CharacterState(familiarName = "Mosquito"),
            survivingOutfits = emptyList(),
            rankedBuckets = SlotList(),
            refineBestPerSlot = emptyMap(),
            comboBudget = ComboBudget(60),
            thrallBonus = 0.0,
            gameDatabase = db,
            usableSwitchFamiliars = emptyList(),
            usableEnthroneFamiliars = discovered,
            usableBjornFamiliars = emptyList(),
            buildCandidates = { _, _ -> candidates },
            scoreFamiliar = scoreFamiliar,
        )

        assertEquals("Miniature Donkey", result.enthronedRace)
        assertEquals("Crown of Thrones", result.bestPerSlot[EquipmentSlot.HAT]?.first)
    }

    private fun container(id: Int, name: String) =
        ItemData(id, name, "", "", ItemPrimaryUse.CONTAINER, emptySet(), setOf('t'), 0, null)

    private fun hat(id: Int, name: String) =
        ItemData(id, name, "", "", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null)

    private fun registerOutfit(
        id: Int,
        name: String,
        pieces: List<String>,
        outfitMods: String,
    ) {
        ModifierDatabase.injectForTest("Outfit", name, outfitMods)
        net.sourceforge.kolmafia.data.OutfitDatabase.registerStatic(
            net.sourceforge.kolmafia.data.OutfitData(
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
        return id to accessory(id, name, mods)
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

    private fun accessory(id: Int, name: String, mods: String = "") =
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
