package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConcoctionQueuedPseudoIngredientsTest {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        ItemDatabase.resetForTest()
        ConcoctionCraftQueue.resetForTest()
        ConcoctionQueueBudget.resetForTest()
    }

    @Test
    fun reserveSmith_tracksAdventurePseudo() {
        registerItem(8601, "pseudo smith leaf")
        registerItem(8602, "pseudo smith parent")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "pseudo smith leaf",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "pseudo smith parent",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = listOf(ConcoctionIngredient("pseudo smith leaf", 1)),
            ),
        )

        val refreshContext = ConcoctionRefreshContext(
            itemCount = { name ->
                when (name.lowercase()) {
                    "pseudo smith leaf" -> 5
                    else -> 0
                }
            },
            limitPoolsFactory = {
                ConcoctionLimitPools.forTest(
                    adventureSmithingLimit = 10,
                    adventuresUsed = ConcoctionQueueBudget.adventuresUsed,
                )
            },
        )
        ConcoctionDatabase.refreshConcoctionsNow(refreshContext)

        val queueContext = ConcoctionQueueContext.fromRefreshContext(refreshContext)
        assertEquals(
            true,
            ConcoctionCraftQueue.push("pseudo smith parent", 2, queueContext),
        )

        assertEquals(
            2,
            ConcoctionQueuedPseudoIngredients.count(
                ConcoctionQueuedIngredients.Bucket.POTION,
                ConcoctionQueuedPseudoIngredients.PseudoType.ADV,
            ),
        )
    }

    @Test
    fun release_reversesPseudoCounters() {
        registerItem(8603, "pseudo release leaf")
        registerItem(8604, "pseudo release parent")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "pseudo release leaf",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "pseudo release parent",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = listOf(ConcoctionIngredient("pseudo release leaf", 1)),
            ),
        )

        val refreshContext = ConcoctionRefreshContext(
            itemCount = { name ->
                when (name.lowercase()) {
                    "pseudo release leaf" -> 5
                    else -> 0
                }
            },
            limitPoolsFactory = {
                ConcoctionLimitPools.forTest(
                    adventureSmithingLimit = 10,
                    adventuresUsed = ConcoctionQueueBudget.adventuresUsed,
                )
            },
        )
        ConcoctionDatabase.refreshConcoctionsNow(refreshContext)

        val queueContext = ConcoctionQueueContext.fromRefreshContext(refreshContext)
        ConcoctionCraftQueue.push("pseudo release parent", 2, queueContext)
        ConcoctionCraftQueue.pop()

        assertEquals(
            0,
            ConcoctionQueuedPseudoIngredients.count(
                ConcoctionQueuedIngredients.Bucket.POTION,
                ConcoctionQueuedPseudoIngredients.PseudoType.ADV,
            ),
        )
    }

    @Test
    fun totalsByType_matchQueueBudget() {
        registerItem(8605, "pseudo budget leaf")
        registerItem(8606, "pseudo budget parent")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "pseudo budget leaf",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "pseudo budget parent",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = listOf(ConcoctionIngredient("pseudo budget leaf", 1)),
            ),
        )

        val refreshContext = ConcoctionRefreshContext(
            itemCount = { name ->
                when (name.lowercase()) {
                    "pseudo budget leaf" -> 5
                    else -> 0
                }
            },
            limitPoolsFactory = {
                ConcoctionLimitPools.forTest(
                    adventureSmithingLimit = 10,
                    adventuresUsed = ConcoctionQueueBudget.adventuresUsed,
                )
            },
        )
        ConcoctionDatabase.refreshConcoctionsNow(refreshContext)

        val queueContext = ConcoctionQueueContext.fromRefreshContext(refreshContext)
        ConcoctionCraftQueue.push("pseudo budget parent", 2, queueContext)

        assertEquals(2, ConcoctionQueuedPseudoIngredients.totalAdventures())
        assertEquals(2, ConcoctionQueueBudget.adventuresUsed)
        assertTrue(ConcoctionQueuedPseudoIngredients.reconcileWithBudget())
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
}
