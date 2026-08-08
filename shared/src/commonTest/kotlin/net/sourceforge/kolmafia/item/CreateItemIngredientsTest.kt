package net.sourceforge.kolmafia.item

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionBuyables
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.ConcoctionRuntimeState
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse

class CreateItemIngredientsTest {

    private class StubRetrieveItemService(
        private val retrieveFn: suspend (Int, Int) -> Int = { _, qty -> qty },
    ) : RetrieveItemService(
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
    ) {
        override suspend fun retrieve(itemId: Int, qty: Int): Int = retrieveFn(itemId, qty)
    }

    @Test
    fun makeIngredients_twoPassRetrieve_callsEachIngredientTwice() = runTest {
        registerItem(FLANGE_ID, "flange")
        registerItem(SPRING_ID, "spring")
        registerItem(WIDGET_ID, "clockwork widget")
        val concoction = ConcoctionData(
            result = "clockwork clockwise dome",
            resultQuantity = 1,
            methods = setOf("TINKER"),
            ingredients = listOf(
                ConcoctionIngredient("clockwork widget", 1),
                ConcoctionIngredient("flange", 1),
                ConcoctionIngredient("spring", 1),
            ),
        )
        val retrieved = mutableListOf<Pair<Int, Int>>()
        val helper = CreateItemIngredients(
            StubRetrieveItemService { id, qty ->
                retrieved += id to qty
                qty
            },
            gameDatabase = null,
        )

        assertTrue(helper.makeIngredients(concoction, 1))

        assertEquals(
            listOf(
                WIDGET_ID to 1,
                FLANGE_ID to 1,
                SPRING_ID to 1,
                WIDGET_ID to 1,
                FLANGE_ID to 1,
                SPRING_ID to 1,
            ),
            retrieved,
        )
    }

    @Test
    fun makeIngredients_duplicateIngredientIds_usesMultiplier() = runTest {
        registerItem(FLANGE_ID, "flange")
        val concoction = ConcoctionData(
            result = "double flange thing",
            resultQuantity = 1,
            methods = setOf("TINKER"),
            ingredients = listOf(
                ConcoctionIngredient("flange", 1),
                ConcoctionIngredient("flange", 2),
            ),
        )
        val retrieved = mutableListOf<Pair<Int, Int>>()
        val helper = CreateItemIngredients(
            StubRetrieveItemService { id, qty ->
                retrieved += id to qty
                qty
            },
            gameDatabase = null,
        )

        assertTrue(helper.makeIngredients(concoction, 2))

        assertEquals(
            listOf(FLANGE_ID to 6, FLANGE_ID to 6, FLANGE_ID to 6, FLANGE_ID to 6),
            retrieved,
        )
    }

    @Test
    fun makeIngredients_sortsByCreatableCount_ascending() = runTest {
        ConcoctionDatabase.resetForTest()
        ConcoctionDatabase.setRuntimeForTest("hard part", ConcoctionRuntimeState(creatable = 0))
        ConcoctionDatabase.setRuntimeForTest("easy part", ConcoctionRuntimeState(creatable = 10))
        registerItem(HARD_ID, "hard part")
        registerItem(EASY_ID, "easy part")
        val concoction = ConcoctionData(
            result = "sorted craft",
            resultQuantity = 1,
            methods = setOf("TINKER"),
            ingredients = listOf(
                ConcoctionIngredient("easy part", 1),
                ConcoctionIngredient("hard part", 1),
            ),
        )
        val order = mutableListOf<Int>()
        val helper = CreateItemIngredients(
            StubRetrieveItemService { id, qty ->
                if (order.size < 2) order += id
                qty
            },
            gameDatabase = null,
        )

        try {
            assertTrue(helper.makeIngredients(concoction, 1))
            assertEquals(listOf(HARD_ID, EASY_ID), order)
        } finally {
            ConcoctionDatabase.resetForTest()
        }
    }

    @Test
    fun makeIngredients_missingRetrieve_returnsFalse() = runTest {
        registerItem(FLANGE_ID, "flange")
        val concoction = ConcoctionData(
            result = "flange only",
            resultQuantity = 1,
            methods = setOf("TINKER"),
            ingredients = listOf(ConcoctionIngredient("flange", 1)),
        )
        val helper = CreateItemIngredients(
            StubRetrieveItemService { _, _ -> 0 },
            gameDatabase = null,
        )

        assertFalse(helper.makeIngredients(concoction, 1))
    }

    @Test
    fun makeIngredients_combineWithoutKnoll_retrievesPasteBeforeIngredients() = runTest {
        registerItem(PART_ID, "combine part")
        val concoction = ConcoctionData(
            result = "combined item",
            resultQuantity = 1,
            methods = setOf("COMBINE"),
            ingredients = listOf(ConcoctionIngredient("combine part", 1)),
        )
        val retrieved = mutableListOf<Pair<Int, Int>>()
        val helper = CreateItemIngredients(
            StubRetrieveItemService { id, qty ->
                retrieved += id to qty
                qty
            },
            gameDatabase = null,
        )
        val state = CharacterState(zodiacSign = "Seal")

        assertTrue(helper.makeIngredients(concoction, 2, state))

        assertEquals(ConcoctionBuyables.MEAT_PASTE, retrieved.first().first)
        assertEquals(2, retrieved.first().second)
        assertTrue(retrieved.drop(1).all { it.first == PART_ID })
    }

    @Test
    fun makeIngredients_knollAvailable_skipsPaste() = runTest {
        registerItem(PART_ID, "combine part")
        val concoction = ConcoctionData(
            result = "combined item",
            resultQuantity = 1,
            methods = setOf("COMBINE"),
            ingredients = listOf(ConcoctionIngredient("combine part", 1)),
        )
        val retrieved = mutableListOf<Int>()
        val helper = CreateItemIngredients(
            StubRetrieveItemService { id, qty ->
                retrieved += id
                qty
            },
            gameDatabase = null,
        )
        val state = CharacterState(zodiacSign = "Mongoose")

        assertTrue(helper.makeIngredients(concoction, 2, state))

        assertFalse(retrieved.contains(ConcoctionBuyables.MEAT_PASTE))
    }

    @Test
    fun makeIngredients_pasteFailure_returnsFalse() = runTest {
        registerItem(PART_ID, "combine part")
        val concoction = ConcoctionData(
            result = "combined item",
            resultQuantity = 1,
            methods = setOf("COMBINE"),
            ingredients = listOf(ConcoctionIngredient("combine part", 1)),
        )
        val helper = CreateItemIngredients(
            StubRetrieveItemService { id, qty ->
                if (id == ConcoctionBuyables.MEAT_PASTE) 0 else qty
            },
            gameDatabase = null,
        )

        assertFalse(helper.makeIngredients(concoction, 1, CharacterState(zodiacSign = "Seal")))
    }

    private fun registerItem(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
    }

    companion object {
        private const val FLANGE_ID = 88401
        private const val SPRING_ID = 88402
        private const val WIDGET_ID = 88403
        private const val HARD_ID = 88404
        private const val EASY_ID = 88405
        private const val PART_ID = 88406
    }
}
