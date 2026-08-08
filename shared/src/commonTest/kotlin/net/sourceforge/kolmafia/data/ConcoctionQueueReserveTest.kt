package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.item.FreeCraftingTurns
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillType

class ConcoctionQueueReserveTest {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun reserveSmith_usesFreeTurnsBeforeAdventures() {
        registerItem(8101, "queue smith leaf")
        registerItem(8102, "queue smith parent")
        injectLeafAndParent()

        val runtime = ConcoctionRuntimeState(initial = 0, creatable = 5)
        val context = ConcoctionQueueContext(
            freeCrafting = FreeCraftingTurns.Context(
                effects = listOf(
                    EffectData(id = 716, name = "Inigo's Incantation of Inspiration", duration = 10),
                ),
            ),
        )

        ConcoctionQueueReserve.reserve(
            ConcoctionDatabase.getByResult("queue smith parent")!!,
            quantity = 5,
            runtime = runtime,
            context = context,
        )
        assertEquals(2, ConcoctionQueueBudget.freeCraftingTurns)
        assertEquals(3, ConcoctionQueueBudget.adventuresUsed)
    }

    @Test
    fun reserveStill_incrementsStillsUsed() {
        registerItem(8111, "queue still leaf")
        registerItem(8112, "queue still parent")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "queue still leaf",
                resultQuantity = 1,
                methods = setOf("STILL"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "queue still parent",
                resultQuantity = 1,
                methods = setOf("STILL"),
                ingredients = listOf(ConcoctionIngredient("queue still leaf", 1)),
            ),
        )

        val leafRuntime = ConcoctionRuntimeState(initial = 5, creatable = 0)
        val parentRuntime = ConcoctionRuntimeState(initial = 0, creatable = 2)
        val context = ConcoctionQueueContext(
            runtimeFor = { name ->
                when (name.lowercase()) {
                    "queue still leaf" -> leafRuntime
                    "queue still parent" -> parentRuntime
                    else -> null
                }
            },
        )

        ConcoctionQueueReserve.reserve(
            ConcoctionDatabase.getByResult("queue still parent")!!,
            quantity = 2,
            runtime = parentRuntime,
            context = context,
        )
        assertEquals(2, ConcoctionQueueBudget.stillsUsed)
    }

    @Test
    fun reservePricedItem_incrementsMeatSpent() {
        registerItem(8121, "queue priced item")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "queue priced item",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )

        val runtime = ConcoctionRuntimeState(initial = 0, creatable = 0, price = 50)
        val reservation = ConcoctionQueueReserve.reserve(
            ConcoctionDatabase.getByResult("queue priced item")!!,
            quantity = 2,
            runtime = runtime,
            context = ConcoctionQueueContext(),
        )
        assertEquals(100, ConcoctionQueueBudget.meatSpent)
        assertEquals(100, reservation.meatSpent)
    }

    @Test
    fun release_reversesBudgetCounters() {
        registerItem(8131, "queue release item")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "queue release item",
                resultQuantity = 1,
                methods = setOf("STILL"),
                ingredients = emptyList(),
            ),
        )

        val runtime = ConcoctionRuntimeState(initial = 0, creatable = 2)
        val reservation = ConcoctionQueueReserve.reserve(
            ConcoctionDatabase.getByResult("queue release item")!!,
            quantity = 2,
            runtime = runtime,
            context = ConcoctionQueueContext(),
        )
        assertEquals(2, ConcoctionQueueBudget.stillsUsed)

        ConcoctionQueueReserve.release(reservation)
        assertEquals(0, ConcoctionQueueBudget.stillsUsed)
        assertEquals(0, ConcoctionQueueBudget.adventuresUsed)
        assertEquals(0, ConcoctionQueueBudget.meatSpent)
    }

    @Test
    fun reserveOverCreatable_incrementsPullsUsed() {
        registerItem(8151, "queue pull item")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "queue pull item",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )

        val runtime = ConcoctionRuntimeState(initial = 2, creatable = 3)
        ConcoctionQueueReserve.reserve(
            ConcoctionDatabase.getByResult("queue pull item")!!,
            quantity = 8,
            runtime = runtime,
            context = ConcoctionQueueContext(),
        )
        assertEquals(3, ConcoctionQueueBudget.pullsUsed)
    }

    @Test
    fun reservePricedItem_skipsPullAmount() {
        registerItem(8152, "queue pull priced")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "queue pull priced",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )

        val runtime = ConcoctionRuntimeState(initial = 0, creatable = 0, price = 50)
        ConcoctionQueueReserve.reserve(
            ConcoctionDatabase.getByResult("queue pull priced")!!,
            quantity = 2,
            runtime = runtime,
            context = ConcoctionQueueContext(),
        )
        assertEquals(0, ConcoctionQueueBudget.pullsUsed)
    }

    @Test
    fun release_reversesPullsUsed() {
        registerItem(8153, "queue pull release")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "queue pull release",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )

        val runtime = ConcoctionRuntimeState(initial = 1, creatable = 1)
        val reservation = ConcoctionQueueReserve.reserve(
            ConcoctionDatabase.getByResult("queue pull release")!!,
            quantity = 5,
            runtime = runtime,
            context = ConcoctionQueueContext(),
        )
        assertEquals(3, ConcoctionQueueBudget.pullsUsed)
        assertEquals(3, reservation.pullsUsed)

        ConcoctionQueueReserve.release(reservation)
        assertEquals(0, ConcoctionQueueBudget.pullsUsed)
    }

    @Test
    fun craftQueue_popRestoresBudgetAndRuntimeQueued() {
        registerItem(8141, "queue craft leaf")
        registerItem(8142, "queue craft parent")
        injectLeafAndParent(resultPrefix = "queue craft")

        val refreshContext = ConcoctionRefreshContext(
            itemCount = { name ->
                when (name.lowercase()) {
                    "queue craft leaf" -> 5
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
        assertEquals(true, ConcoctionCraftQueue.push("queue craft parent", 2, queueContext))
        assertEquals(2, ConcoctionQueueBudget.adventuresUsed)
        assertEquals(2, ConcoctionDatabase.getRuntime("queue craft parent")?.queued)

        val popped = ConcoctionCraftQueue.pop()
        assertEquals("queue craft parent", popped?.resultName)
        assertEquals(0, ConcoctionQueueBudget.adventuresUsed)
        assertEquals(0, ConcoctionDatabase.getRuntime("queue craft parent")?.queued)
    }

    private fun injectLeafAndParent(resultPrefix: String = "queue smith") {
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "$resultPrefix leaf",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "$resultPrefix parent",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = listOf(ConcoctionIngredient("$resultPrefix leaf", 1)),
            ),
        )
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
