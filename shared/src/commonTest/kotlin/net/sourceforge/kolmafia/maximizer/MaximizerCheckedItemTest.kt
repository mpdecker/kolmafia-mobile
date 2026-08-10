package net.sourceforge.kolmafia.maximizer

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.ConcoctionRefreshContext
import net.sourceforge.kolmafia.data.FoldGroup
import net.sourceforge.kolmafia.data.FoldGroupDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.mall.MallPriceManager
import net.sourceforge.kolmafia.preferences.Preferences

class MaximizerCheckedItemTest {

    private val stubDb = object : GameDatabase() {
        override fun item(id: Int): ItemData? = ItemDatabase.getById(id)
        override fun item(name: String): ItemData? = ItemDatabase.getByName(name)
        override fun npcPrice(itemName: String): Int = when (itemName.lowercase()) {
            "npc hat" -> 100
            else -> 0
        }
    }

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        ConcoctionDatabase.resetForTest()
        FoldGroupDatabase.resetForTest()
    }

    @Test
    fun totalCount_physicalOnlyEqualsInitial() {
        registerEquipment(5001, "plain hat")
        val ctx = context(spec = MaximizeSpec(DoubleModifier.MYS, Evaluator("mys")))
        val checked = MaximizerCheckedItemBuilder.build(5001, "plain hat", ctx.copy(
            inventoryCount = { if (it == 5001) 2 else 0 },
        ))
        assertEquals(2, checked.initial)
        assertEquals(2, checked.totalCount())
    }

    @Test
    fun totalCount_creatableOnlyWhenIngredientsAvailable() {
        registerEquipment(5101, "craft hat")
        registerItem(5102, "paste")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "craft hat",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(ConcoctionIngredient("paste", 1)),
            ),
        )
        val ctx = context(
            spec = MaximizeSpec(
                primary = DoubleModifier.MYS,
                evaluator = Evaluator("mys"),
                allowCreatable = true,
            ),
            inventoryCount = { id -> if (id == 5102) 3 else 0 },
        )
        val checked = MaximizerCheckedItemBuilder.build(5101, "craft hat", ctx)
        assertEquals(0, checked.initial)
        assertTrue(checked.creatable > 0)
        assertTrue(checked.totalCount() > 0)
    }

    @Test
    fun totalCount_foldPeerAddsFoldable() {
        registerEquipment(5201, "fold target")
        registerEquipment(5202, "fold source")
        FoldGroupDatabase.registerGroupForTest(
            FoldGroup(hpDamagePct = 5, items = listOf("fold source", "fold target")),
        )
        val ctx = context(
            spec = MaximizeSpec(DoubleModifier.MYS, Evaluator("mys")),
            inventoryCount = { id -> if (id == 5202) 2 else 0 },
        )
        val checked = MaximizerCheckedItemBuilder.build(5201, "fold target", ctx)
        assertEquals(2, checked.foldable)
        assertEquals(5202, checked.foldItemId)
        assertEquals(2, checked.totalCount())
    }

    @Test
    fun totalCount_noAdventuresZeroesCreatableWhenTurnsNeeded() {
        registerEquipment(5301, "adv hat")
        registerItem(5302, "leaf")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "leaf",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "adv hat",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = listOf(ConcoctionIngredient("leaf", 1)),
            ),
        )
        ConcoctionDatabase.refreshConcoctionsNow(
            ConcoctionRefreshContext.fromLiveSession(
                aggregatedCounts = mapOf(5302 to 5),
                state = CharacterState(adventuresLeft = 0),
                accessibleCount = { id -> if (id == 5302) 5 else 0 },
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("maximizerNoAdventures", true)
        val ctx = context(
            spec = MaximizeSpec(
                primary = DoubleModifier.MYS,
                evaluator = Evaluator("mys"),
                allowCreatable = true,
            ),
            preferences = prefs,
            inventoryCount = { 0 },
        )
        val checked = MaximizerCheckedItemBuilder.build(5301, "adv hat", ctx)
        assertEquals(0, checked.creatable)
    }

    @Test
    fun totalCount_pullableUsesStorageWhenAllowed() {
        registerEquipment(5401, "stored hat")
        val ctx = context(
            spec = MaximizeSpec(DoubleModifier.MYS, Evaluator("mys")),
            inventoryCount = { 0 },
            storageContents = mapOf(5401 to 1),
        )
        val checked = MaximizerCheckedItemBuilder.build(5401, "stored hat", ctx)
        assertEquals(1, checked.pullable)
        assertEquals(1, checked.totalCount())
    }

    @Test
    fun validate_clearsMallBuyWhenPriceAboveMax() {
        val checked = MaximizerCheckedItem(
            itemId = 5501,
            name = "mall hat",
            mallBuyable = 1,
            buyableFlag = true,
        )
        val validated = checked.validate(
            maxPrice = 100L,
            priceLevel = MaximizerPriceLevel.ALL,
            availableMeat = 1_000L,
            storageMeat = 1_000L,
            mallPrice = { 150L },
        )
        assertEquals(0, validated.mallBuyable)
        assertEquals(0, validated.pullBuyable)
    }

    @Test
    fun validate_clearsMallBuyWhenInsufficientMeat() {
        val checked = MaximizerCheckedItem(
            itemId = 5502,
            name = "cheap hat",
            mallBuyable = 1,
            buyableFlag = true,
        )
        val validated = checked.validate(
            maxPrice = 500L,
            priceLevel = MaximizerPriceLevel.BUYABLE_ONLY,
            availableMeat = 50L,
            storageMeat = 1_000L,
            mallPrice = { 100L },
        )
        assertEquals(0, validated.mallBuyable)
    }

    @Test
    fun validate_clearsPullBuyWhenStorageMeatTooLow() {
        val checked = MaximizerCheckedItem(
            itemId = 5503,
            name = "pull buy hat",
            pullBuyable = 1,
            buyableFlag = true,
        )
        val validated = checked.validate(
            maxPrice = 500L,
            priceLevel = MaximizerPriceLevel.ALL,
            availableMeat = 1_000L,
            storageMeat = 50L,
            mallPrice = { 100L },
        )
        assertEquals(0, validated.pullBuyable)
    }

    @Test
    fun validate_noOpWhenDontCheck() {
        val checked = MaximizerCheckedItem(
            itemId = 5504,
            name = "expensive hat",
            mallBuyable = 1,
            pullBuyable = 1,
            buyableFlag = true,
        )
        val validated = checked.validate(
            maxPrice = 100L,
            priceLevel = MaximizerPriceLevel.DONT_CHECK,
            availableMeat = 0L,
            storageMeat = 0L,
            mallPrice = { 999L },
        )
        assertEquals(1, validated.mallBuyable)
        assertEquals(1, validated.pullBuyable)
    }

    @Test
    fun build_populatesPullBuyableWhenStoragePullAndMallAllowed() {
        registerEquipment(5601, "pull buy hat")
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithMall", true)
        val mallPrices = MallPriceManager()
        mallPrices.cachePrice(5601, price = 100L, quantity = 1, shopId = 1)
        val ctx = context(
            spec = MaximizeSpec(
                primary = DoubleModifier.MYS,
                evaluator = Evaluator("mys"),
                maxPrice = 500,
            ),
            preferences = prefs,
            mallPriceManager = mallPrices,
            characterState = CharacterState(meat = 0, storageMeat = 500L),
            inventoryCount = { 0 },
            storageContents = emptyMap(),
            priceLevel = MaximizerPriceLevel.BUYABLE_ONLY,
        )
        val checked = MaximizerCheckedItemBuilder.build(5601, "pull buy hat", ctx)
        assertEquals(1, checked.pullBuyable)
        assertTrue(checked.buyableFlag)
        assertEquals(1, checked.totalCount())
    }

    @Test
    fun build_mallBuyableWhenPriceLevelBuyableOnly() {
        registerEquipment(5701, "mall hat")
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithMall", true)
        val mallPrices = MallPriceManager()
        mallPrices.cachePrice(5701, price = 100L, quantity = 1, shopId = 1)
        val ctx = context(
            spec = MaximizeSpec(
                primary = DoubleModifier.MYS,
                evaluator = Evaluator("mys"),
                maxPrice = 500,
            ),
            preferences = prefs,
            mallPriceManager = mallPrices,
            characterState = CharacterState(meat = 500),
            inventoryCount = { 0 },
            priceLevel = MaximizerPriceLevel.BUYABLE_ONLY,
        )
        val checked = MaximizerCheckedItemBuilder.build(5701, "mall hat", ctx)
        assertEquals(1, checked.mallBuyable)
        assertTrue(checked.buyableFlag)
    }

    @Test
    fun build_skipsMallHistoricalGateWhenDontCheck() {
        registerEquipment(5702, "unknown mall hat")
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithMall", true)
        val ctx = context(
            spec = MaximizeSpec(
                primary = DoubleModifier.MYS,
                evaluator = Evaluator("mys"),
                maxPrice = 500,
            ),
            preferences = prefs,
            mallPriceManager = null,
            characterState = CharacterState(meat = 500),
            inventoryCount = { 0 },
            priceLevel = MaximizerPriceLevel.DONT_CHECK,
        )
        val checked = MaximizerCheckedItemBuilder.build(5702, "unknown mall hat", ctx)
        assertEquals(1, checked.mallBuyable)
        assertTrue(checked.buyableFlag)
    }

    @Test
    fun build_rejectsMallBuyWhenHistoricalTooHighForBuyableOnly() {
        registerEquipment(5703, "dear hat")
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithMall", true)
        val mallPrices = MallPriceManager()
        mallPrices.cachePrice(5703, price = 1200L, quantity = 1, shopId = 1)
        val ctx = context(
            spec = MaximizeSpec(
                primary = DoubleModifier.MYS,
                evaluator = Evaluator("mys"),
                maxPrice = 500,
            ),
            preferences = prefs,
            mallPriceManager = mallPrices,
            characterState = CharacterState(meat = 500),
            inventoryCount = { 0 },
            priceLevel = MaximizerPriceLevel.BUYABLE_ONLY,
        )
        val checked = MaximizerCheckedItemBuilder.build(5703, "dear hat", ctx)
        assertEquals(0, checked.mallBuyable)
    }

    @Test
    fun build_mallBuyableRequiresAutoSatisfyWithMallPref() {
        registerEquipment(5602, "mall hat")
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithMall", false)
        val mallPrices = MallPriceManager()
        mallPrices.cachePrice(5602, price = 100L, quantity = 1, shopId = 1)
        val ctx = context(
            spec = MaximizeSpec(
                primary = DoubleModifier.MYS,
                evaluator = Evaluator("mys"),
                maxPrice = 500,
            ),
            preferences = prefs,
            mallPriceManager = mallPrices,
            inventoryCount = { 0 },
        )
        val checked = MaximizerCheckedItemBuilder.build(5602, "mall hat", ctx)
        assertEquals(0, checked.mallBuyable)
    }

    private fun context(
        spec: MaximizeSpec,
        inventoryCount: (Int) -> Int = { 0 },
        storageContents: Map<Int, Int> = emptyMap(),
        preferences: Preferences? = null,
        mallPriceManager: MallPriceManager? = null,
        characterState: CharacterState = CharacterState(),
        priceLevel: MaximizerPriceLevel = MaximizerPriceLevel.DONT_CHECK,
    ) = MaximizerCheckedItemBuilder.Context(
        spec = spec,
        gameDatabase = stubDb,
        characterState = characterState,
        preferences = preferences,
        mallPriceManager = mallPriceManager,
        inventoryCount = inventoryCount,
        closetContents = emptyMap(),
        storageContents = storageContents,
        displayContents = emptyMap(),
        stashContents = emptyMap(),
        priceLevel = priceLevel,
    )

    private fun registerEquipment(id: Int, name: String) {
        registerItem(id, name, ItemPrimaryUse.HAT)
    }

    private fun registerItem(id: Int, name: String, primaryUse: ItemPrimaryUse = ItemPrimaryUse.USABLE) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = primaryUse,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }
}
