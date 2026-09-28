package net.sourceforge.kolmafia.data

import kotlin.test.Test
import kotlin.test.assertEquals

class ConcoctionYieldTest {

    private val potion = ConcoctionData(
        result = "banana smoothie",
        resultQuantity = 1,
        methods = setOf("SAUCE", "SX3"),
        ingredients = listOf(
            ConcoctionIngredient("scrumptious reagent", 1),
            ConcoctionIngredient("banana", 1),
        ),
        craftYield = 1,
    )

    private val gin = ConcoctionData(
        result = "bottle of gin",
        resultQuantity = 3,
        methods = setOf("MIX"),
        ingredients = listOf(
            ConcoctionIngredient("fermenting powder", 1),
            ConcoctionIngredient("juniper berries", 1),
        ),
        craftYield = 3,
    )

    @Test
    fun saucePotion_isOneWithoutSauceror() {
        assertEquals(1, ConcoctionYield.getYield(potion, tripleReagent = false))
    }

    @Test
    fun saucePotion_isThreeForSauceror() {
        assertEquals(3, ConcoctionYield.getYield(potion, tripleReagent = true))
    }

    @Test
    fun multiCountRecipe_usesResultQuantity() {
        assertEquals(3, ConcoctionYield.getYield(gin, tripleReagent = false))
        assertEquals(3, ConcoctionYield.getYield(gin, tripleReagent = true))
    }
}
