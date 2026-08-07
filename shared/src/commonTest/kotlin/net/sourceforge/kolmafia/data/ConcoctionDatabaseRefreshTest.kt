package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterDatabase

class ConcoctionDatabaseRefreshTest {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        ConsumableDatabase.resetForTest()
        ItemDatabase.resetForTest()
        ModifierDatabase.resetOverridesForTest()
        NpcStoreDatabase.resetForTest()
        CoinmasterDatabase.resetForTest()
    }

    @Test
    fun refreshConcoctionsNow_updatesEffectNameFromModifiers() {
        ModifierDatabase.injectForTest("Item", "test brew", "Effect: Bundled Effect")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "test brew",
                resultQuantity = 1,
                methods = setOf("MIX"),
                ingredients = listOf(ConcoctionIngredient("olive oil", 1)),
            ),
        )
        assertNull(ConcoctionDatabase.getEffectName("test brew"))

        ConcoctionDatabase.refreshConcoctionsNow()

        assertEquals("Bundled Effect", ConcoctionDatabase.getEffectName("test brew"))
    }

    @Test
    fun refreshConcoctionsNow_picksUpRuntimeModifierOverride() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 999,
                name = "test brew",
                descId = "test_brew",
                image = "testbrew.gif",
                primaryUse = ItemPrimaryUse.DRINK,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ModifierDatabase.injectForTest("Item", "test brew", "Effect: Bundled Effect")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "test brew",
                resultQuantity = 1,
                methods = setOf("MIX"),
                ingredients = emptyList(),
            ),
        )
        ModifierDatabase.updateItem(999, "Effect: Runtime Effect")

        ConcoctionDatabase.refreshConcoctionsNow()

        assertEquals("Runtime Effect", ConcoctionDatabase.getEffectName("test brew"))
    }

    @Test
    fun markRecalculateAdventureRange_clearedByRefreshConcoctionsNow() {
        ConcoctionDatabase.markRecalculateAdventureRange()
        assertTrue(ConcoctionDatabase.recalculateAdventureRangeForTest())

        ConcoctionDatabase.refreshConcoctionsNow()

        assertFalse(ConcoctionDatabase.recalculateAdventureRangeForTest())
    }

    @Test
    fun refreshConcoctions_deferredWhenRefreshNotNeeded() {
        ConcoctionDatabase.resetRefreshStateForTest()
        ConcoctionDatabase.refreshConcoctions(force = false)
        assertFalse(ConcoctionDatabase.recalculateAdventureRangeForTest())
    }

    @Test
    fun markRecalculateAdventureRange_refreshRebuildsAverageCache() {
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = "refresh food",
                type = ConsumableType.FOOD,
                amount = 2,
                levelReq = 1,
                quality = ConsumableQuality.GOOD,
                advMin = 3,
                advMax = 7,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "",
            ),
        )
        ConcoctionDatabase.markRecalculateAdventureRange()
        ConcoctionDatabase.refreshConcoctionsNow()

        assertEquals(5.0, ConsumableDatabase.getAverageAdventures("refresh food"))
    }

    @Test
    fun refreshConcoctionsNow_populatesInitialFromContext() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 501,
                name = "test smith leaf",
                descId = "test_smith_leaf",
                image = "leaf.gif",
                primaryUse = ItemPrimaryUse.WEAPON,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "test smith leaf",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = emptyList(),
            ),
        )
        val context = ConcoctionRefreshContext.fromInventoryCounts(mapOf(501 to 2))

        ConcoctionDatabase.refreshConcoctionsNow(context)

        assertEquals(2, ConcoctionDatabase.getRuntime("test smith leaf")?.initial)
        assertEquals(2, ConcoctionDatabase.initialCount("test smith leaf"))
    }

    @Test
    fun refreshConcoctionsNow_wiresAdventuresContextForOnHandItems() {
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "test smith leaf",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = emptyList(),
            ),
        )
        val context = ConcoctionRefreshContext(itemCount = { name ->
            if (name == "test smith leaf") 1 else 0
        })

        ConcoctionDatabase.refreshConcoctionsNow(context)

        assertEquals(1, ConcoctionDatabase.initialCount("test smith leaf"))
        val concoction = ConcoctionDatabase.getByResult("test smith leaf")!!
        val advContext = ConcoctionAdventuresContext(
            initialCount = { name -> ConcoctionDatabase.initialCount(name) },
        )
        assertEquals(0, getAdventuresNeeded(concoction, 1, considerFree = true, advContext))
    }

    @Test
    fun refreshConcoctionsFromInventory_cacheSkipsDeductionWhenOnHand() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 502,
                name = "test smith leaf",
                descId = "test_smith_leaf_cache",
                image = "leaf.gif",
                primaryUse = ItemPrimaryUse.FOOD,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "test smith leaf",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = emptyList(),
            ),
        )
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = "test smith leaf",
                type = ConsumableType.FOOD,
                amount = 2,
                levelReq = 1,
                quality = ConsumableQuality.DECENT,
                advMin = 10,
                advMax = 10,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "",
            ),
        )

        ConcoctionDatabase.refreshConcoctionsNow()
        ConsumableDatabase.calculateAllAverageAdventures()
        assertEquals(9.0, ConsumableDatabase.getAverageAdventures("test smith leaf"))

        ConcoctionDatabase.refreshConcoctionsFromInventory(mapOf(502 to 1))
        assertEquals(10.0, ConsumableDatabase.getAverageAdventures("test smith leaf"))
    }

    @Test
    fun refreshConcoctionsFromInventory_zeroInitial_stillDeductsCraftTurns() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 503,
                name = "test smith leaf",
                descId = "test_smith_leaf2",
                image = "leaf2.gif",
                primaryUse = ItemPrimaryUse.WEAPON,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "test smith leaf",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = emptyList(),
            ),
        )
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = "test smith leaf",
                type = ConsumableType.FOOD,
                amount = 2,
                levelReq = 1,
                quality = ConsumableQuality.DECENT,
                advMin = 10,
                advMax = 10,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "",
            ),
        )

        ConcoctionDatabase.refreshConcoctionsFromInventory(emptyMap())
        assertEquals(9.0, ConsumableDatabase.getAverageAdventures("test smith leaf"))
    }

    @Test
    fun refreshConcoctionsFromAggregated_nestedIngredientInCloset_reducesDeduction() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 601,
                name = "test ing a",
                descId = "test_ing_a",
                image = "inga.gif",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = 602,
                name = "test parent smith",
                descId = "test_parent_smith",
                image = "parent.gif",
                primaryUse = ItemPrimaryUse.FOOD,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "test ing a",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "test parent smith",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = listOf(ConcoctionIngredient("test ing a", 1)),
            ),
        )
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = "test parent smith",
                type = ConsumableType.FOOD,
                amount = 2,
                levelReq = 1,
                quality = ConsumableQuality.DECENT,
                advMin = 10,
                advMax = 10,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "",
            ),
        )

        ConcoctionDatabase.refreshConcoctionsFromAggregated(emptyMap())
        assertEquals(8.0, ConsumableDatabase.getAverageAdventures("test parent smith"))

        val aggregated = ConcoctionAvailableIngredients.aggregate(
            ConcoctionIngredientSources(closet = mapOf(601 to 1)),
        )
        ConcoctionDatabase.refreshConcoctionsFromAggregated(aggregated)
        assertEquals(9.0, ConsumableDatabase.getAverageAdventures("test parent smith"))
        assertEquals(1, ConcoctionDatabase.initialCount("test ing a"))
        assertEquals(0, ConcoctionDatabase.initialCount("test parent smith"))
        assertEquals(1, ConcoctionDatabase.creatableCount("test parent smith"))
        assertEquals(1, ConcoctionDatabase.totalCount("test parent smith"))
    }

    @Test
    fun refreshConcoctionsFromAggregated_closetOnly_populatesLeafInitial() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 603,
                name = "closet leaf",
                descId = "closet_leaf",
                image = "closet.gif",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "closet leaf",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = emptyList(),
            ),
        )

        val aggregated = ConcoctionAvailableIngredients.aggregate(
            ConcoctionIngredientSources(closet = mapOf(603 to 2)),
        )
        ConcoctionDatabase.refreshConcoctionsNow(ConcoctionRefreshContext.fromAggregatedCounts(aggregated))

        assertEquals(2, ConcoctionDatabase.initialCount("closet leaf"))
    }

    @Test
    fun refreshConcoctionsFromLiveSession_cookWithoutOven_creatableZero() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 701,
                name = "perm stew",
                descId = "perm_stew",
                image = "stew.gif",
                primaryUse = ItemPrimaryUse.FOOD,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = 702,
                name = "perm meat",
                descId = "perm_meat",
                image = "meat.gif",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "perm stew",
                resultQuantity = 1,
                methods = setOf("COOK"),
                ingredients = listOf(ConcoctionIngredient("perm meat", 1)),
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("hasOven", false)
        val context = ConcoctionRefreshContext.fromLiveSession(
            aggregatedCounts = mapOf(702 to 5),
            state = CharacterState(),
            prefs = prefs,
            accessibleCount = { id -> if (id == 702) 5 else 0 },
        )

        ConcoctionDatabase.refreshConcoctionsNow(context)

        assertEquals(0, ConcoctionDatabase.creatableCount("perm stew"))
        assertEquals(0, ConcoctionDatabase.totalCount("perm stew"))
    }

    @Test
    fun refreshConcoctionsFromLiveSession_zeroAdventures_smithCreatableZero() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 711,
                name = "adv smith parent",
                descId = "adv_smith_parent",
                image = "parent.gif",
                primaryUse = ItemPrimaryUse.WEAPON,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = 712,
                name = "adv smith leaf",
                descId = "adv_smith_leaf",
                image = "leaf.gif",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "adv smith leaf",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "adv smith parent",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = listOf(ConcoctionIngredient("adv smith leaf", 1)),
            ),
        )
        val context = ConcoctionRefreshContext.fromLiveSession(
            aggregatedCounts = mapOf(712 to 5),
            state = CharacterState(adventuresLeft = 0),
            accessibleCount = { id -> if (id == 712) 5 else 0 },
        )

        ConcoctionDatabase.refreshConcoctionsNow(context)

        assertEquals(0, ConcoctionDatabase.creatableCount("adv smith parent"))
        assertEquals(0, ConcoctionDatabase.totalCount("adv smith parent"))
    }

    @Test
    fun refreshConcoctionsFromLiveSession_zeroStills_stillCreatableZero() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 721,
                name = "still cocktail",
                descId = "still_cocktail",
                image = "cocktail.gif",
                primaryUse = ItemPrimaryUse.DRINK,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = 722,
                name = "still ingredient",
                descId = "still_ingredient",
                image = "ingredient.gif",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "still ingredient",
                resultQuantity = 1,
                methods = setOf("STILL"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "still cocktail",
                resultQuantity = 1,
                methods = setOf("STILL"),
                ingredients = listOf(ConcoctionIngredient("still ingredient", 1)),
            ),
        )
        val context = ConcoctionRefreshContext.fromLiveSession(
            aggregatedCounts = mapOf(722 to 5),
            state = CharacterState(stillsAvailable = 0, adventuresLeft = 100),
            accessibleCount = { id -> if (id == 722) 5 else 0 },
        )

        ConcoctionDatabase.refreshConcoctionsNow(context)

        assertEquals(0, ConcoctionDatabase.creatableCount("still cocktail"))
        assertEquals(0, ConcoctionDatabase.totalCount("still cocktail"))
    }

    @Test
    fun refreshConcoctionsFromLiveSession_lowMeat_capsNpcBuyCreatable() {
        NpcStoreDatabase.loadFromText(
            """
            1
            Test Store	teststore	npc buy item	100	ROW1
            """.trimIndent(),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = 731,
                name = "npc buy item",
                descId = "npc_buy_item",
                image = "buy.gif",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "npc buy item",
                resultQuantity = 1,
                methods = setOf("NOCREATE"),
                ingredients = emptyList(),
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithNPCs", true)
        val context = ConcoctionRefreshContext.fromLiveSession(
            aggregatedCounts = emptyMap(),
            state = CharacterState(meat = 250),
            prefs = prefs,
        )

        ConcoctionDatabase.refreshConcoctionsNow(context)

        assertEquals(2, ConcoctionDatabase.totalCount("npc buy item"))
        assertEquals(0, ConcoctionDatabase.creatableCount("npc buy item"))
    }

    @Test
    fun refresh_coinmasterOnly_setsCreatableFromAffordableCount() {
        registerCoinWidgetItem()
        CoinmasterDatabase.loadFromText(
            shopsText = "testcoin\tTest Coinmaster\n",
            coinText = "Test Coinmaster\tROW9101\tcoin widget (1)\tmeat (100)\n",
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "coin widget",
                resultQuantity = 1,
                methods = setOf("COINMASTER"),
                ingredients = emptyList(),
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val context = ConcoctionRefreshContext.fromLiveSession(
            aggregatedCounts = emptyMap(),
            state = CharacterState(meat = 500),
            prefs = prefs,
        )

        ConcoctionDatabase.refreshConcoctionsNow(context)

        assertEquals(5, ConcoctionDatabase.creatableCount("coin widget"))
        assertEquals(5, ConcoctionDatabase.totalCount("coin widget"))
        assertEquals(0, ConcoctionDatabase.getRuntime("coin widget")?.price)
        assertTrue(ConcoctionDatabase.getRuntime("coin widget")?.skipCalculate == true)
    }

    @Test
    fun refresh_coinmasterOnly_prefOff_zeroCreatable() {
        registerCoinWidgetItem()
        CoinmasterDatabase.loadFromText(
            shopsText = "testcoin\tTest Coinmaster\n",
            coinText = "Test Coinmaster\tROW9101\tcoin widget (1)\tmeat (100)\n",
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "coin widget",
                resultQuantity = 1,
                methods = setOf("COINMASTER"),
                ingredients = emptyList(),
            ),
        )
        val prefs = Preferences(MapSettings())
        val context = ConcoctionRefreshContext.fromLiveSession(
            aggregatedCounts = emptyMap(),
            state = CharacterState(meat = 500),
            prefs = prefs,
        )

        ConcoctionDatabase.refreshConcoctionsNow(context)

        assertEquals(0, ConcoctionDatabase.creatableCount("coin widget"))
        assertEquals(0, ConcoctionDatabase.totalCount("coin widget"))
        assertTrue(ConcoctionDatabase.getRuntime("coin widget")?.skipCalculate == true)
    }

    @Test
    fun refresh_coinmasterOnly_insufficientTokens_zeroCreatable() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 9201,
                name = "token prize",
                descId = "token_prize",
                image = "prize.gif",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = 9202,
                name = "shop token",
                descId = "shop_token",
                image = "token.gif",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        CoinmasterDatabase.loadFromText(
            shopsText = "testcoin\tTest Coinmaster\n",
            coinText = "Test Coinmaster\tROW9201\ttoken prize (1)\tshop token (10)\n",
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "token prize",
                resultQuantity = 1,
                methods = setOf("COINMASTER"),
                ingredients = emptyList(),
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val context = ConcoctionRefreshContext.fromLiveSession(
            aggregatedCounts = emptyMap(),
            state = CharacterState(meat = 500),
            prefs = prefs,
            accessibleCount = { 0 },
        )

        ConcoctionDatabase.refreshConcoctionsNow(context)

        assertEquals(0, ConcoctionDatabase.creatableCount("token prize"))
        assertEquals(0, ConcoctionDatabase.totalCount("token prize"))
    }

    private fun registerCoinWidgetItem() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 9101,
                name = "coin widget",
                descId = "coin_widget",
                image = "widget.gif",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }
}
