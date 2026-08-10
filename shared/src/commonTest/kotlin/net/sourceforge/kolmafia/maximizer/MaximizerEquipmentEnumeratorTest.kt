package net.sourceforge.kolmafia.maximizer

import kotlinx.coroutines.runBlocking
import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.EquipmentData
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.ModifierEntry
import net.sourceforge.kolmafia.mall.MallPriceManager
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MaximizerEquipmentEnumeratorTest {

    @BeforeTest
    fun setup() {
        EquipmentDatabase.resetForTest()
        ModifierDatabase.resetForTest()
        ConcoctionDatabase.resetForTest()
    }

    @AfterTest
    fun teardown() {
        EquipmentDatabase.resetForTest()
        ItemDatabase.resetForTest()
        ModifierDatabase.resetForTest()
        ConcoctionDatabase.resetForTest()
    }

    private fun GameDatabase.syncTestItemModifiers(vararg names: String) {
        runBlocking { ModifierDatabase.load() }
        for (name in names) {
            itemModifier(name)?.let { entry ->
                ModifierDatabase.overrideModifier("Item", entry.name, entry.modifiers)
            }
        }
    }

    private fun registerHands(itemId: Int, name: String, hands: Int, primaryUse: ItemPrimaryUse) {
        EquipmentDatabase.registerForTest(
            itemId,
            EquipmentData(name, 100, null, hands, if (primaryUse == ItemPrimaryUse.SIXGUN) "sixgun" else "knife"),
        )
    }

    @Test
    fun enumerate_oneHandedMeleeWithHands_routesToWeapon1hAndOffhandMelee() {
        val db = stubDb(
            101 to ItemData(101, "sharp knife", "", "", ItemPrimaryUse.WEAPON, emptySet(), setOf('t'), 0, null),
            modifiers = mapOf("sharp knife" to "Muscle: +5"),
        ).also {
            registerHands(101, "sharp knife", 1, ItemPrimaryUse.WEAPON)
            it.syncTestItemModifiers("sharp knife")
        }
        val spec = MaximizeSpec(
            primary = DoubleModifier.MUS,
            evaluator = Evaluator("muscle"),
            requireHands = true,
        )
        val buckets = MaximizerEquipmentEnumerator.enumerate(
            candidateIds = setOf(101),
            spec = spec,
            gameDatabase = db,
            checkedItem = checkedAvailable(),
            scoreItem = { name, ev ->
                val entry = db.itemModifier(name) ?: return@enumerate 0.0
                ev.getItemContribution(net.sourceforge.kolmafia.modifiers.ModifierParser.parse(entry.modifiers))
            },
            itemMeetsConstraints = { _, _ -> true },
        )
        assertEquals(listOf("sharp knife"), buckets.allItems(MaximizerSlot.WEAPON_1H).map { it.name })
        assertEquals(listOf("sharp knife"), buckets.allItems(MaximizerSlot.OFFHAND_MELEE).map { it.name })
        assertTrue(buckets.isEmpty(MaximizerSlot.WEAPON))
    }

    @Test
    fun enumerate_twoHandedWeapon_weaponBucketOnly() {
        val db = stubDb(
            201 to ItemData(201, "big sword", "", "", ItemPrimaryUse.WEAPON, emptySet(), setOf('t'), 0, null),
            modifiers = mapOf("big sword" to "Muscle: +8"),
        ).also {
            registerHands(201, "big sword", 2, ItemPrimaryUse.WEAPON)
            it.syncTestItemModifiers("big sword")
        }
        val spec = MaximizeSpec(
            primary = DoubleModifier.MUS,
            evaluator = Evaluator("muscle"),
            requireHands = true,
        )
        val buckets = MaximizerEquipmentEnumerator.enumerate(
            candidateIds = setOf(201),
            spec = spec,
            gameDatabase = db,
            checkedItem = checkedAvailable(),
            scoreItem = { name, ev ->
                val entry = db.itemModifier(name) ?: return@enumerate 0.0
                ev.getItemContribution(net.sourceforge.kolmafia.modifiers.ModifierParser.parse(entry.modifiers))
            },
            itemMeetsConstraints = { _, _ -> true },
        )
        assertEquals(listOf("big sword"), buckets.allItems(MaximizerSlot.WEAPON).map { it.name })
        assertTrue(buckets.isEmpty(MaximizerSlot.WEAPON_1H))
        assertTrue(buckets.isEmpty(MaximizerSlot.OFFHAND_MELEE))
    }

    @Test
    fun enumerate_sixgunWithHands_routesToWeapon1hAndOffhandRanged() {
        val db = stubDb(
            301 to ItemData(301, "peashooter", "", "", ItemPrimaryUse.SIXGUN, emptySet(), setOf('t'), 0, null),
            modifiers = mapOf("peashooter" to "Mysticality: +3"),
        ).also {
            registerHands(301, "peashooter", 1, ItemPrimaryUse.SIXGUN)
            it.syncTestItemModifiers("peashooter")
        }
        val spec = MaximizeSpec(
            primary = DoubleModifier.MYS,
            evaluator = Evaluator("mysticality"),
            requireHands = true,
        )
        val buckets = MaximizerEquipmentEnumerator.enumerate(
            candidateIds = setOf(301),
            spec = spec,
            gameDatabase = db,
            checkedItem = checkedAvailable(),
            scoreItem = { name, ev ->
                val entry = db.itemModifier(name) ?: return@enumerate 0.0
                ev.getItemContribution(net.sourceforge.kolmafia.modifiers.ModifierParser.parse(entry.modifiers))
            },
            itemMeetsConstraints = { _, _ -> true },
        )
        assertEquals(listOf("peashooter"), buckets.allItems(MaximizerSlot.WEAPON_1H).map { it.name })
        assertEquals(listOf("peashooter"), buckets.allItems(MaximizerSlot.OFFHAND_RANGED).map { it.name })
    }

    @Test
    fun enumerate_sortsByScoreDescending() {
        val db = stubDb(
            401 to ItemData(401, "weak hat", "", "", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null),
            402 to ItemData(402, "strong hat", "", "", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null),
            modifiers = mapOf(
                "weak hat" to "Mysticality: +1",
                "strong hat" to "Mysticality: +10",
            ),
        ).also { it.syncTestItemModifiers("weak hat", "strong hat") }
        val spec = MaximizeSpec(
            primary = DoubleModifier.MYS,
            evaluator = Evaluator("mysticality"),
        )
        val buckets = MaximizerEquipmentEnumerator.enumerate(
            candidateIds = setOf(401, 402),
            spec = spec,
            gameDatabase = db,
            checkedItem = checkedAvailable(),
            scoreItem = { name, ev ->
                val entry = db.itemModifier(name) ?: return@enumerate 0.0
                ev.getItemContribution(net.sourceforge.kolmafia.modifiers.ModifierParser.parse(entry.modifiers))
            },
            itemMeetsConstraints = { _, _ -> true },
        )
        assertEquals(
            listOf("strong hat", "weak hat"),
            buckets.allItems(MaximizerSlot.HAT).map { it.name },
        )
    }

    @Test
    fun enumerate_skipsItemsViolatingForbiddenBooleanConstraint() {
        val db = stubDb(
            501 to ItemData(501, "volley hat", "", "", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null),
            502 to ItemData(502, "plain hat", "", "", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null),
            modifiers = mapOf(
                "volley hat" to "Mysticality: +5, Volleyball or Sombrero",
                "plain hat" to "Mysticality: +3",
            ),
        ).also { it.syncTestItemModifiers("volley hat", "plain hat") }
        val spec = MaximizeGoal.parseSpec("mysticality, -volleyball")!!
        val buckets = MaximizerEquipmentEnumerator.enumerate(
            candidateIds = setOf(501, 502),
            spec = spec,
            gameDatabase = db,
            checkedItem = checkedAvailable(),
            scoreItem = { name, ev ->
                val entry = db.itemModifier(name) ?: return@enumerate 0.0
                ev.getItemContribution(net.sourceforge.kolmafia.modifiers.ModifierParser.parse(entry.modifiers))
            },
            itemMeetsConstraints = { _, _ -> true },
        )
        assertEquals(listOf("plain hat"), buckets.allItems(MaximizerSlot.HAT).map { it.name })
    }

    @Test
    fun enumerate_parseSpecSyncsRequiredBooleanToEvaluator() {
        val spec = MaximizeGoal.parseSpec("item, +volleyball")!!
        val mods = net.sourceforge.kolmafia.modifiers.ModifierParser.parse("Volleyball or Sombrero")
        assertEquals(Evaluator.Constraint.MEETS, spec.evaluator.checkConstraints(mods))
    }

    @Test
    fun enumerate_creatableOnlyItemIncludedWhenCheckedCreatablePositive() {
        val db = stubDb(
            601 to ItemData(601, "craft hat", "", "", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null),
            602 to ItemData(602, "paste", "", "", ItemPrimaryUse.USABLE, emptySet(), setOf('t'), 0, null),
            modifiers = mapOf("craft hat" to "Mysticality: +8"),
        ).also { it.syncTestItemModifiers("craft hat") }
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "craft hat",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(ConcoctionIngredient("paste", 1)),
            ),
        )
        val spec = MaximizeSpec(
            primary = DoubleModifier.MYS,
            evaluator = Evaluator("mysticality"),
            allowCreatable = true,
        )
        val buckets = MaximizerEquipmentEnumerator.enumerate(
            candidateIds = setOf(601),
            spec = spec,
            gameDatabase = db,
            checkedItem = { itemId ->
                when (itemId) {
                    601 -> MaximizerCheckedItem(601, "craft hat", creatable = 2)
                    else -> MaximizerCheckedItem(itemId, "other", initial = 0)
                }
            },
            scoreItem = { name, ev ->
                val entry = db.itemModifier(name) ?: return@enumerate 0.0
                ev.getItemContribution(net.sourceforge.kolmafia.modifiers.ModifierParser.parse(entry.modifiers))
            },
            itemMeetsConstraints = { _, _ -> true },
        )
        assertEquals(listOf("craft hat"), buckets.allItems(MaximizerSlot.HAT).map { it.name })
        assertEquals(2, buckets.allItems(MaximizerSlot.HAT).single().accessibleCount)
    }

    @Test
    fun enumerate_pinsHodgmanWhenHoboUseful() = runBlocking {
        ModifierDatabase.load()
        val db = stubDb(
            701 to ItemData(701, "Hodgman's whisk", "", "", ItemPrimaryUse.ACCESSORY, emptySet(), setOf('t'), 0, null),
            702 to ItemData(702, "plain ring", "", "", ItemPrimaryUse.ACCESSORY, emptySet(), setOf('t'), 0, null),
            modifiers = mapOf(
                "Hodgman's whisk" to "Muscle: +1",
                "plain ring" to "Muscle: +50",
            ),
        ).also {
            it.syncTestItemModifiers("Hodgman's whisk", "plain ring")
        }
        val spec = MaximizeSpec(
            primary = net.sourceforge.kolmafia.modifiers.DoubleModifier.MEATDROP,
            evaluator = Evaluator("meat"),
        )
        val autoContext = MaximizerAutoContext.from(spec.evaluator)
        val buckets = MaximizerEquipmentEnumerator.enumerate(
            candidateIds = setOf(701, 702),
            spec = spec,
            gameDatabase = db,
            checkedItem = checkedAvailable(),
            scoreItem = { name, ev ->
                val entry = db.itemModifier(name) ?: return@enumerate 0.0
                ev.getItemContribution(net.sourceforge.kolmafia.modifiers.ModifierParser.parse(entry.modifiers))
            },
            itemMeetsConstraints = { _, _ -> true },
            autoContext = autoContext,
        )
        val hodgman = buckets.allItems(MaximizerSlot.ACC1).find { it.name == "Hodgman's whisk" }
        assertTrue(hodgman?.automatic == true, "Hodgman's item should be automatic for meat goal")
    }

    @Test
    fun mergeBuckets_keepsAutomaticBeyondLimit() {
        val buckets = SlotList<MaximizerRankedItem>()
        buckets.get(MaximizerSlot.ACC1).add(
            MaximizerRankedItem(
                1, "pinned-acc", 1.0,
                MaximizerCheckedItem(1, "pinned-acc"), automatic = true,
            ),
        )
        buckets.get(MaximizerSlot.ACC1).add(
            MaximizerRankedItem(
                2, "scored-acc", 100.0,
                MaximizerCheckedItem(2, "scored-acc"),
            ),
        )
        val merged = MaximizerEquipmentEnumerator.mergeBuckets(
            buckets,
            listOf(MaximizerSlot.ACC1),
            limit = 1,
        )
        assertEquals(listOf("pinned-acc", "scored-acc"), merged.map { it.first })
    }

    @Test
    fun mergeBuckets_synergyPairPinnedForMeatGoal() = runBlocking {
        ModifierDatabase.injectForTest("Item", "bewitching boots", "Meat Drop: +10, Synergetic")
        ModifierDatabase.injectForTest("Item", "bitter bowtie", "Cold Resistance: +1, Meat Drop: +10, Synergetic")
        ModifierDatabase.injectForTest(
            "Synergy",
            "bewitching boots/bitter bowtie",
            "Meat Drop: +10, Cold Resistance: +1",
        )
        val db = stubDb(
            801 to ItemData(801, "bewitching boots", "", "", ItemPrimaryUse.ACCESSORY, emptySet(), setOf('t'), 0, null),
            802 to ItemData(802, "bitter bowtie", "", "", ItemPrimaryUse.ACCESSORY, emptySet(), setOf('t'), 0, null),
            803 to ItemData(803, "plain tie", "", "", ItemPrimaryUse.ACCESSORY, emptySet(), setOf('t'), 0, null),
            modifiers = mapOf(
                "bewitching boots" to "Meat Drop: +10, Synergetic",
                "bitter bowtie" to "Cold Resistance: +1, Meat Drop: +10, Synergetic",
                "plain tie" to "Muscle: +100",
            ),
        ).also {
            ModifierDatabase.overrideModifier("Item", "bewitching boots", "Meat Drop: +10, Synergetic")
            ModifierDatabase.overrideModifier("Item", "bitter bowtie", "Cold Resistance: +1, Meat Drop: +10, Synergetic")
        }
        val spec = MaximizeSpec(
            primary = net.sourceforge.kolmafia.modifiers.DoubleModifier.MEATDROP,
            evaluator = Evaluator("meat"),
        )
        val buckets = MaximizerEquipmentEnumerator.enumerate(
            candidateIds = setOf(801, 802, 803),
            spec = spec,
            gameDatabase = db,
            checkedItem = checkedAvailable(),
            scoreItem = { name, ev ->
                val entry = db.itemModifier(name) ?: return@enumerate 0.0
                ev.getItemContribution(net.sourceforge.kolmafia.modifiers.ModifierParser.parse(entry.modifiers))
            },
            itemMeetsConstraints = { _, _ -> true },
            autoContext = MaximizerAutoContext.from(spec.evaluator),
        )
        val merged = MaximizerEquipmentEnumerator.mergeBuckets(
            buckets,
            listOf(MaximizerSlot.ACC1),
            limit = 1,
        )
        assertTrue("bewitching boots" in merged.map { it.first })
        assertTrue("bitter bowtie" in merged.map { it.first })
    }

    @Test
    fun enumerate_applySynergyAdjustments_weakPairNotPinnedInMerge() = runBlocking {
        ModifierDatabase.injectForTest("Item", "syn-a", "Meat Drop: +10, Synergetic")
        ModifierDatabase.injectForTest("Item", "syn-b", "Meat Drop: +10, Synergetic")
        ModifierDatabase.injectForTest("Item", "filler-a", "Meat Drop: +30")
        ModifierDatabase.injectForTest("Item", "filler-b", "Meat Drop: +28")
        ModifierDatabase.injectForTest("Synergy", "syn-a/syn-b", "Meat Drop: +5")
        val db = stubDb(
            851 to ItemData(851, "syn-a", "", "", ItemPrimaryUse.ACCESSORY, emptySet(), setOf('t'), 0, null),
            852 to ItemData(852, "syn-b", "", "", ItemPrimaryUse.ACCESSORY, emptySet(), setOf('t'), 0, null),
            853 to ItemData(853, "filler-a", "", "", ItemPrimaryUse.ACCESSORY, emptySet(), setOf('t'), 0, null),
            854 to ItemData(854, "filler-b", "", "", ItemPrimaryUse.ACCESSORY, emptySet(), setOf('t'), 0, null),
            modifiers = mapOf(
                "syn-a" to "Meat Drop: +10, Synergetic",
                "syn-b" to "Meat Drop: +10, Synergetic",
                "filler-a" to "Meat Drop: +30",
                "filler-b" to "Meat Drop: +28",
            ),
        )
        val spec = MaximizeSpec(
            primary = DoubleModifier.MEATDROP,
            evaluator = Evaluator("meat"),
        )
        val buckets = MaximizerEquipmentEnumerator.enumerate(
            candidateIds = setOf(851, 852, 853, 854),
            spec = spec,
            gameDatabase = db,
            checkedItem = checkedAvailable(),
            scoreItem = { name, ev ->
                val entry = db.itemModifier(name) ?: return@enumerate 0.0
                ev.getItemContribution(net.sourceforge.kolmafia.modifiers.ModifierParser.parse(entry.modifiers))
            },
            itemMeetsConstraints = { _, _ -> true },
            autoContext = MaximizerAutoContext.from(spec.evaluator),
        )
        MaximizerSynergyAdjustments.apply(buckets, spec, CharacterState(), db)
        val merged = MaximizerEquipmentEnumerator.mergeBuckets(
            buckets,
            listOf(MaximizerSlot.ACC1),
            limit = 1,
        )
        assertFalse("syn-a" in merged.map { it.first })
        assertFalse("syn-b" in merged.map { it.first })
        assertEquals("filler-a", merged.first().first)
    }

    @Test
    fun enumerate_includesMallBuyableUnownedHat() {
        ItemDatabase.registerForTest(
            ItemData(
                9101, "mall hat", "d9101", "img", ItemPrimaryUse.HAT,
                emptySet(), setOf('t'), 0, null,
            ),
        )
        EquipmentDatabase.registerForTest(
            9101,
            EquipmentData("mall hat", 100, null, 0, "hat"),
        )
        ModifierDatabase.injectForTest("Item", "mall hat", "Mysticality: +8")
        val db = stubDb(
            9101 to ItemData(
                9101, "mall hat", "d9101", "img", ItemPrimaryUse.HAT,
                emptySet(), setOf('t'), 0, null,
            ),
            modifiers = mapOf("mall hat" to "Mysticality: +8"),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithMall", true)
        val mallPrices = MallPriceManager()
        mallPrices.cachePrice(9101, price = 100L, quantity = 1, shopId = 1)
        val ctx = MaximizerCheckedItemBuilder.Context(
            spec = MaximizeSpec(
                primary = DoubleModifier.MYS,
                evaluator = Evaluator("mysticality"),
                maxPrice = 500,
            ),
            gameDatabase = db,
            characterState = CharacterState(meat = 500),
            preferences = prefs,
            mallPriceManager = mallPrices,
            inventoryCount = { 0 },
            closetContents = emptyMap(),
            storageContents = emptyMap(),
            displayContents = emptyMap(),
            stashContents = emptyMap(),
            priceLevel = MaximizerPriceLevel.BUYABLE_ONLY,
        )
        val spec = MaximizeSpec(
            primary = DoubleModifier.MYS,
            evaluator = Evaluator("mysticality"),
            maxPrice = 500,
        )
        val buckets = MaximizerEquipmentEnumerator.enumerate(
            candidateIds = EquipmentDatabase.allEquipmentItemIds().toSet(),
            spec = spec,
            gameDatabase = db,
            checkedItem = { itemId ->
                val name = db.item(itemId)?.name ?: ""
                MaximizerCheckedItemBuilder.build(itemId, name, ctx)
                    .validate(
                        maxPrice = 500L,
                        priceLevel = MaximizerPriceLevel.BUYABLE_ONLY,
                        availableMeat = 500L,
                        storageMeat = 0L,
                        mallPrice = { id -> mallPrices.getMallPrice(id) },
                    )
            },
            scoreItem = { name, ev ->
                val entry = db.itemModifier(name) ?: return@enumerate 0.0
                ev.getItemContribution(net.sourceforge.kolmafia.modifiers.ModifierParser.parse(entry.modifiers))
            },
            itemMeetsConstraints = { _, _ -> true },
        )
        assertEquals(listOf("mall hat"), buckets.allItems(MaximizerSlot.HAT).map { it.name })
    }

    @Test
    fun enumerate_populatesPerFamiliarBucketsForCarryEligibleItems() {
        ModifierDatabase.injectForTest("Item", "carry-hat", """Meat Drop: +1, Familiar Effect: "1xLep, cap 12"""")
        val db = stubDb(
            701 to ItemData(701, "carry-hat", "", "", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null),
            modifiers = mapOf("carry-hat" to """Meat Drop: +1, Familiar Effect: "1xLep, cap 12""""),
        )
        val spec = MaximizeSpec(
            primary = DoubleModifier.MEATDROP,
            evaluator = Evaluator("meat"),
            switchFamiliars = listOf(FamiliarCarryRules.HATRACK_RACE, "Miniature Donkey"),
        )
        val buckets = MaximizerEquipmentEnumerator.enumerate(
            candidateIds = setOf(701),
            spec = spec,
            gameDatabase = db,
            checkedItem = checkedAvailable(),
            scoreItem = { name, ev ->
                val entry = db.itemModifier(name) ?: return@enumerate 0.0
                ev.getItemContribution(net.sourceforge.kolmafia.modifiers.ModifierParser.parse(entry.modifiers))
            },
            itemMeetsConstraints = { _, _ -> true },
            switchFamiliars = spec.switchFamiliars,
            familiarWeight = 10,
        )
        assertEquals(2, buckets.familiarCount())
        assertEquals(listOf("carry-hat"), buckets.getFamiliar(0).map { it.name })
        assertTrue(buckets.getFamiliar(0).first().score > 0.0)
        assertTrue(buckets.getFamiliar(1).isEmpty())
        assertEquals(listOf("carry-hat"), buckets.allItems(MaximizerSlot.HAT).map { it.name })
    }

    @Test
    fun allRankedItems_excludesFamiliarOnlyBuckets() {
        val buckets = SlotList<MaximizerRankedItem>(1)
        buckets.getFamiliar(0).add(
            MaximizerRankedItem(801, "fam-only", 12.0, MaximizerCheckedItem(801, "fam-only", initial = 1)),
        )
        assertTrue(MaximizerEquipmentEnumerator.allRankedItems(buckets).none { it.name == "fam-only" })
    }

    @Test
    fun toCandidatesByEquipmentSlot_familiarBucketIndexUsesPerFamiliarOnly() {
        val db = stubDb(
            711 to ItemData(711, "carry-only", "", "", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null),
            712 to ItemData(712, "native-fam", "", "", ItemPrimaryUse.FAMILIAR, emptySet(), setOf('t'), 0, null),
            modifiers = mapOf(
                "carry-only" to "Meat Drop: +50",
                "native-fam" to "Meat Drop: +5",
            ),
        )
        val spec = MaximizeSpec(
            primary = DoubleModifier.MEATDROP,
            evaluator = Evaluator("meat"),
            switchFamiliars = listOf(FamiliarCarryRules.HATRACK_RACE),
        )
        val buckets = SlotList<MaximizerRankedItem>(1)
        buckets.get(MaximizerSlot.FAMILIAR).add(
            MaximizerRankedItem(712, "native-fam", 5.0, MaximizerCheckedItem(712, "native-fam", initial = 1)),
        )
        buckets.getFamiliar(0).add(
            MaximizerRankedItem(711, "carry-only", 50.0, MaximizerCheckedItem(711, "carry-only", initial = 1)),
        )
        val ranked = MaximizerEquipmentEnumerator.toCandidatesByEquipmentSlot(
            buckets = buckets,
            spec = spec,
            usedElsewhere = emptySet(),
            perSlotLimit = 5,
            gameDatabase = db,
            scoreItem = { _, _ -> 0.0 },
            itemMeetsConstraints = { _, _ -> true },
            priceFor = { 0 },
            familiarBucketIndex = 0,
        )
        assertEquals(listOf("carry-only"), ranked[EquipmentSlot.FAMILIAR]?.map { it.first })
    }

    private fun checkedAvailable(count: Int = 1): (Int) -> MaximizerCheckedItem = { itemId ->
        MaximizerCheckedItem(itemId, "item", initial = count)
    }

    private fun stubDb(
        vararg items: Pair<Int, ItemData>,
        modifiers: Map<String, String> = emptyMap(),
    ): GameDatabase = object : GameDatabase() {
        private val byId = items.toMap()
        override fun item(id: Int): ItemData? = byId[id]
        override fun item(name: String): ItemData? =
            byId.values.find { it.name.equals(name, ignoreCase = true) }
        override fun itemModifier(name: String): ModifierEntry? =
            modifiers[name.lowercase()]?.let { ModifierEntry("Item", name, it) }
    }
}
