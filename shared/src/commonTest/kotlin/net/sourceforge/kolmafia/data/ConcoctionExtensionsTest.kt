package net.sourceforge.kolmafia.data

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConcoctionExtensionsTest {

    @Test
    fun suseCraftable_singleIngredient() {
        val c = ConcoctionData(
            result = "tasty paste",
            resultQuantity = 1,
            methods = setOf("SUSE"),
            ingredients = listOf(ConcoctionIngredient("meat paste", 1)),
        )
        assertTrue(c.isSuseCraftable())
        assertTrue(c.isAutoCraftable())
        assertFalse(c.isStationCraftable())
    }

    @Test
    fun stationCraftable_requiresTwoIngredients() {
        val c = ConcoctionData(
            result = "hi mein",
            resultQuantity = 1,
            methods = setOf("COOK"),
            ingredients = listOf(
                ConcoctionIngredient("dry noodles", 1),
                ConcoctionIngredient("sweet s-sauce", 1),
            ),
        )
        assertTrue(c.isStationCraftable())
        assertTrue(c.isAutoCraftable())
        assertFalse(c.isSuseCraftable())
    }

    @Test
    fun manualNotAutoCraftable() {
        val c = ConcoctionData(
            result = "weird item",
            resultQuantity = 1,
            methods = setOf("SUSE", "MANUAL"),
            ingredients = listOf(ConcoctionIngredient("source", 1)),
        )
        assertFalse(c.isSuseCraftable())
        assertFalse(c.isAutoCraftable())
    }
}
