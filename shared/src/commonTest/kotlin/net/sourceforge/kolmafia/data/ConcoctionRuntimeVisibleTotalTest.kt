package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterState

class ConcoctionRuntimeVisibleTotalTest {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        ItemDatabase.resetForTest()
        ConcoctionQueueBudget.resetForTest()
    }

    @Test
    fun refresh_setsVisibleTotalEqualToTotal() {
        registerItem(8701, "visible total leaf")
        registerItem(8702, "visible total parent")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "visible total leaf",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "visible total parent",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(ConcoctionIngredient("visible total leaf", 1)),
            ),
        )

        val refreshContext = ConcoctionRefreshContext(
            itemCount = { name ->
                when (name.lowercase()) {
                    "visible total leaf" -> 5
                    else -> 0
                }
            },
            limitPoolsFactory = {
                ConcoctionLimitPools.forTest(adventureLimit = 10)
            },
        )
        ConcoctionDatabase.refreshConcoctionsNow(refreshContext)

        assertEquals(5, ConcoctionDatabase.totalCount("visible total parent"))
        assertEquals(5, ConcoctionDatabase.availableCount("visible total parent"))
        assertEquals(
            5,
            ConcoctionDatabase.getRuntime("visible total parent")?.visibleTotal,
        )
    }

    @Test
    fun pullablePass_updatesVisibleTotal() {
        registerItem(8703, "visible pull item")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "visible pull item",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )

        ConcoctionDatabase.setPullsRemaining(5)
        ConcoctionDatabase.setPullsBudgeted(5)

        val refreshContext = ConcoctionRefreshContext(
            itemCount = { 0 },
            storageCountById = { id -> if (id == 8703) 4 else 0 },
            considerPulls = true,
        )
        ConcoctionDatabase.refreshConcoctionsNow(refreshContext)

        assertEquals(4, ConcoctionDatabase.pullableCount("visible pull item"))
        assertEquals(4, ConcoctionDatabase.totalCount("visible pull item"))
        assertEquals(4, ConcoctionDatabase.availableCount("visible pull item"))
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
