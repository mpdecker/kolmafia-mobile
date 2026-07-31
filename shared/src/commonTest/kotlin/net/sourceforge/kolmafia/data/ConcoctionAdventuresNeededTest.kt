package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ConcoctionAdventuresNeededTest {

    private val leafIngredientA = ConcoctionData(
        result = "test ing a",
        resultQuantity = 1,
        methods = setOf("COMBINE"),
        ingredients = emptyList(),
    )

    private val leafIngredientB = ConcoctionData(
        result = "test ing b",
        resultQuantity = 1,
        methods = setOf("COMBINE"),
        ingredients = emptyList(),
    )

    private val leafSmith = ConcoctionData(
        result = "test smith leaf",
        resultQuantity = 1,
        methods = setOf("SMITH"),
        ingredients = listOf(
            ConcoctionIngredient("test ing a", 1),
            ConcoctionIngredient("test ing b", 1),
        ),
    )

    private val nestedSmith = ConcoctionData(
        result = "test nested smith",
        resultQuantity = 1,
        methods = setOf("SMITH"),
        ingredients = listOf(
            ConcoctionIngredient("test smith leaf", 1),
            ConcoctionIngredient("test ing b", 1),
        ),
    )

    @BeforeTest
    fun setUp() {
        ConcoctionDatabase.resetForTest()
        ConcoctionDatabase.injectForTest(leafIngredientA)
        ConcoctionDatabase.injectForTest(leafIngredientB)
        ConcoctionDatabase.injectForTest(leafSmith)
        ConcoctionDatabase.injectForTest(nestedSmith)
    }

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
    }

    @Test
    fun smithLeaf_costsOneAdventure() {
        assertEquals(1, getAdventuresNeeded(leafSmith, 1, considerFree = false))
    }

    @Test
    fun nestedSmith_costsParentAndChild() {
        assertEquals(2, getAdventuresNeeded(nestedSmith, 1, considerFree = false))
    }

    @Test
    fun resultQuantityGreaterThanOne_returnsZero() {
        val multi = leafSmith.copy(resultQuantity = 2)
        assertEquals(0, getAdventuresNeeded(multi, 1, considerFree = false))
    }

    @Test
    fun initialCountCoversNeed_returnsZero() {
        val context = ConcoctionAdventuresContext(
            initialCount = { name ->
                if (name == "test smith leaf") 1 else 0
            },
        )
        assertEquals(0, getAdventuresNeeded(leafSmith, 1, considerFree = true, context))
    }

    @Test
    fun craftYield_halvesTurnCost() {
        val highYield = leafSmith.copy(craftYield = 2)
        assertEquals(1, getAdventuresNeeded(highYield, 2, considerFree = false))
    }

    @Test
    fun missingIngredientConcoction_returnsZero() {
        val orphan = ConcoctionData(
            result = "test orphan",
            resultQuantity = 1,
            methods = setOf("SMITH"),
            ingredients = listOf(ConcoctionIngredient("nonexistent item", 1)),
        )
        ConcoctionDatabase.injectForTest(orphan)
        assertEquals(0, getAdventuresNeeded(orphan, 1, considerFree = false))
    }

    @Test
    fun considerFree_subtractsSmithingPool() {
        val context = ConcoctionAdventuresContext(freeSmithingTurns = 1)
        assertEquals(0, getAdventuresNeeded(leafSmith, 1, considerFree = true, context))
    }

    @Test
    fun notPermitted_returnsZero() {
        val context = ConcoctionAdventuresContext(isPermitted = { false })
        assertEquals(0, getAdventuresNeeded(leafSmith, 1, considerFree = false, context))
    }

    @Test
    fun cacheIntegration_deductsFromAverage() {
        val craftedFood = ConsumableData(
            name = "test nested smith",
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
        )
        ConsumableDatabase.resetConsumablesForTest()
        ConsumableDatabase.injectConsumableForTest(craftedFood)
        ConsumableDatabase.calculateAllAverageAdventures()
        assertEquals(8.0, ConsumableDatabase.getAverageAdventures("test nested smith"))
    }

    @Test
    fun nonConcoctionConsumable_noDeduction() {
        val plainFood = ConsumableData(
            name = "plain crafted food",
            type = ConsumableType.FOOD,
            amount = 2,
            levelReq = 1,
            quality = ConsumableQuality.DECENT,
            advMin = 6,
            advMax = 6,
            muscMin = 0,
            muscMax = 0,
            mystMin = 0,
            mystMax = 0,
            moxieMin = 0,
            moxieMax = 0,
            notes = "",
        )
        ConsumableDatabase.resetConsumablesForTest()
        ConsumableDatabase.injectConsumableForTest(plainFood)
        ConsumableDatabase.calculateAllAverageAdventures()
        assertEquals(6.0, ConsumableDatabase.getAverageAdventures("plain crafted food"))
    }

    @Test
    fun phase269ConditionalExtra_regressionAfterCacheDeduction() {
        val martini = ConsumableData(
            name = "deduct martini",
            type = ConsumableType.DRINK,
            amount = 2,
            levelReq = 1,
            quality = ConsumableQuality.GOOD,
            advMin = 5,
            advMax = 7,
            muscMin = 0,
            muscMax = 0,
            mystMin = 0,
            mystMax = 0,
            moxieMin = 0,
            moxieMax = 0,
            notes = "MARTINI",
        )
        ConsumableDatabase.resetConsumablesForTest()
        ConsumableDatabase.injectConsumableForTest(martini)
        ConsumableDatabase.calculateAllAverageAdventures()
        val withTuxedo = ConsumableDatabase.getAverageAdventures(
            "deduct martini",
            extraContext = ConditionalExtraAdventureContext(
                equippedItemNames = setOf("tuxedo shirt"),
            ),
        )
        assertEquals(8.0, withTuxedo)
    }
}
