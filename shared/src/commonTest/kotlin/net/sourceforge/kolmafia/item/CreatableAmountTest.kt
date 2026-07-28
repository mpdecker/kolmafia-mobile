package net.sourceforge.kolmafia.item

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse

class CreatableAmountTest {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        ConcoctionDatabase.resetForTest()
    }

    @Test
    fun quantityPossible_limitedByScarcestIngredient() {
        registerItem(3001, "result item")
        registerItem(3002, "ingredient a")
        registerItem(3003, "ingredient b")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "result item",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(
                    ConcoctionIngredient("ingredient a", 2),
                    ConcoctionIngredient("ingredient b", 3),
                ),
            ),
        )
        val counts = mapOf(
            3002 to 10,
            3003 to 9,
        )
        val possible = CreatableAmount.quantityPossible(3001) { id, _ ->
            counts[id] ?: 0
        }
        assertEquals(3, possible)
    }

    @Test
    fun quantityPossible_respectsResultQuantity() {
        registerItem(3101, "bulk result")
        registerItem(3102, "single ingredient")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "bulk result",
                resultQuantity = 3,
                methods = setOf("COMBINE"),
                ingredients = listOf(ConcoctionIngredient("single ingredient", 1)),
            ),
        )
        val possible = CreatableAmount.quantityPossible(3101) { _, _ -> 4 }
        assertEquals(12, possible)
    }

    @Test
    fun quantityPossible_zeroWhenNoConcoction() {
        registerItem(3201, "not craftable")
        assertEquals(0, CreatableAmount.quantityPossible(3201) { _, _ -> 99 })
    }

    private fun registerItem(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }
}
