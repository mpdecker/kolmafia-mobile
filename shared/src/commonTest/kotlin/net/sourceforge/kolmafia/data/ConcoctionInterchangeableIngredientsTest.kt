package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ConcoctionInterchangeableIngredientsTest {

    @AfterTest
    fun tearDown() {
        ItemDatabase.resetForTest()
    }

    @Test
    fun prefersHigherCount_willerOverSchlitz() {
        registerPairItems()
        val concoction = ConcoctionData(
            result = "mixed drink",
            resultQuantity = 1,
            methods = setOf("COMBINE"),
            ingredients = listOf(ConcoctionIngredient("schlitz", 1)),
        )
        val resolved = ConcoctionInterchangeableIngredients.resolve(
            concoction,
            resultItemId = 9001,
            availableCount = { id ->
                when (id) {
                    41 -> 2
                    81 -> 5
                    else -> 0
                }
            },
        )
        assertEquals("willer", resolved.single().name)
    }

    @Test
    fun tieBreakUsesPrice_prefersLowerMallPriceOnEqualCount() {
        registerPairItems()
        val concoction = ConcoctionData(
            result = "mixed drink",
            resultQuantity = 1,
            methods = setOf("COMBINE"),
            ingredients = listOf(ConcoctionIngredient("schlitz", 1)),
        )
        val resolved = ConcoctionInterchangeableIngredients.resolve(
            concoction,
            resultItemId = 9001,
            availableCount = { 3 },
            priceFor = { id ->
                when (id) {
                    41 -> 100
                    81 -> 50
                    else -> 0
                }
            },
        )
        assertEquals("willer", resolved.single().name)
    }

    @Test
    fun skipsWhenMoreThanTwoIngredients() {
        registerPairItems()
        registerItem(9004, "third ing", autosell = 1)
        val concoction = ConcoctionData(
            result = "complex mix",
            resultQuantity = 1,
            methods = setOf("COMBINE"),
            ingredients = listOf(
                ConcoctionIngredient("schlitz", 1),
                ConcoctionIngredient("ketchup", 1),
                ConcoctionIngredient("third ing", 1),
            ),
        )
        val resolved = ConcoctionInterchangeableIngredients.resolve(
            concoction,
            resultItemId = 9002,
            availableCount = { id ->
                when (id) {
                    41 -> 0
                    81 -> 10
                    else -> 0
                }
            },
        )
        assertEquals(listOf("schlitz", "ketchup", "third ing"), resolved.map { it.name })
    }

    @Test
    fun skipsNiceWarmBeer() {
        registerPairItems()
        val concoction = ConcoctionData(
            result = "nice warm beer",
            resultQuantity = 1,
            methods = setOf("COMBINE"),
            ingredients = listOf(ConcoctionIngredient("schlitz", 1)),
        )
        val resolved = ConcoctionInterchangeableIngredients.resolve(
            concoction,
            resultItemId = ConcoctionInterchangeableIngredients.NICE_WARM_BEER,
            availableCount = { id ->
                when (id) {
                    41 -> 0
                    81 -> 10
                    else -> 0
                }
            },
        )
        assertEquals("schlitz", resolved.single().name)
    }

    private fun registerPairItems() {
        registerItem(41, "schlitz", autosell = 10)
        registerItem(81, "willer", autosell = 20)
        registerItem(106, "ketchup", autosell = 5)
        registerItem(107, "catsup", autosell = 5)
    }

    private fun registerItem(id: Int, name: String, autosell: Int) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = autosell,
                plural = null,
            ),
        )
    }
}
