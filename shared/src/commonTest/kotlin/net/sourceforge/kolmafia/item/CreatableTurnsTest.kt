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

class CreatableTurnsTest {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        ConcoctionDatabase.resetForTest()
    }

    @Test
    fun adventuresNeeded_recursiveIngredientTree_sumsChildTurns() {
        registerItem(9501, "layered product")
        registerItem(9502, "smith intermediate")
        registerItem(338, "tenderizing hammer")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "layered product",
                resultQuantity = 1,
                methods = setOf("SMITH", "HAMMER"),
                ingredients = listOf(ConcoctionIngredient("smith intermediate", 1)),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "smith intermediate",
                resultQuantity = 1,
                methods = setOf("SMITH", "HAMMER"),
                ingredients = emptyList(),
            ),
        )
        val counts = mutableMapOf(338 to 1)
        val turns = CreatableTurns.adventuresNeeded(
            itemId = 9501,
            quantityNeeded = 1,
            context = CreatableTurns.Context(
                inventoryCount = { id -> counts[id] ?: 0 },
                isPermitted = { true },
            ),
        )
        assertEquals(2, turns)
    }

    @Test
    fun adventuresNeeded_flatOnlyWouldBeOneTurn() {
        registerItem(9510, "flat smith product")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "flat smith product",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = emptyList(),
            ),
        )
        val turns = CreatableTurns.adventuresNeeded(
            itemId = 9510,
            quantityNeeded = 1,
            inventoryCount = { 0 },
            isPermitted = { true },
        )
        assertEquals(1, turns)
    }

    @Test
    fun adventuresNeeded_considerFreeCrafting_subtractsSmithingFreeTurns() {
        registerItem(9511, "freeable smith product")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "freeable smith product",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = emptyList(),
            ),
        )
        val turns = CreatableTurns.adventuresNeeded(
            itemId = 9511,
            quantityNeeded = 1,
            inventoryCount = { 0 },
            isPermitted = { true },
            considerFreeCrafting = true,
            freeCrafting = FreeCraftingTurns.Context(
                itemCount = { if (it == 6965) 1 else 0 },
            ),
        )
        assertEquals(0, turns)
    }

    @Test
    fun adventuresNeeded_multiStackInventory_returnsZero() {
        registerItem(9512, "stacked product")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "stacked product",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )
        val turns = CreatableTurns.adventuresNeeded(
            itemId = 9512,
            quantityNeeded = 3,
            inventoryCount = { 2 },
            isPermitted = { true },
        )
        assertEquals(0, turns)
    }

    @Test
    fun adventuresNeeded_cyclesPreventRunawayRecursion() {
        registerItem(9601, "cycle a")
        registerItem(9602, "cycle b")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "cycle a",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = listOf(ConcoctionIngredient("cycle b", 1)),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "cycle b",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = listOf(ConcoctionIngredient("cycle a", 1)),
            ),
        )
        val turns = CreatableTurns.adventuresNeeded(
            itemId = 9601,
            quantityNeeded = 1,
            inventoryCount = { 0 },
            isPermitted = { true },
        )
        assertEquals(2, turns)
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
