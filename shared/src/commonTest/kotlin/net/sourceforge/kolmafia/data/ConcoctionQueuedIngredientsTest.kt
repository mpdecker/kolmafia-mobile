package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.preferences.Preferences

class ConcoctionQueuedIngredientsTest {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun trackFood_incrementsBucketMap() {
        ConcoctionQueuedIngredients.trackConsumption(ConcoctionQueuedIngredients.Bucket.FOOD, 8501, 2)
        assertEquals(-2, ConcoctionQueuedIngredients.creditForRefresh()[8501])
    }

    @Test
    fun release_reversesBucketMap() {
        ConcoctionQueuedIngredients.trackConsumption(ConcoctionQueuedIngredients.Bucket.FOOD, 8502, 3)
        ConcoctionQueuedIngredients.releaseConsumption(ConcoctionQueuedIngredients.Bucket.FOOD, 8502, 3)
        assertTrue(ConcoctionQueuedIngredients.creditForRefresh().isEmpty())
    }

    @Test
    fun creditForRefresh_negatesConsumption() {
        ConcoctionQueuedIngredients.trackConsumption(ConcoctionQueuedIngredients.Bucket.POTION, 8503, 2)
        assertEquals(-2, ConcoctionQueuedIngredients.creditForRefresh()[8503])
    }

    @Test
    fun aggregate_includesQueuedIngredientCredit() {
        val sources = ConcoctionIngredientSources(inventory = mapOf(8504 to 5))
        ConcoctionQueuedIngredients.trackConsumption(ConcoctionQueuedIngredients.Bucket.POTION, 8504, 2)
        val aggregated = ConcoctionAvailableIngredients.aggregate(sources)
        assertEquals(3, aggregated[8504])
    }

    @Test
    fun reserve_popTracksAndReleasesIngredientCredits() {
        registerItem(8511, "queued ingredient leaf")
        registerItem(8512, "queued ingredient parent")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "queued ingredient leaf",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "queued ingredient parent",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(ConcoctionIngredient("queued ingredient leaf", 1)),
            ),
        )

        val leafRuntime = ConcoctionRuntimeState(initial = 5, creatable = 0)
        val parentRuntime = ConcoctionRuntimeState(initial = 0, creatable = 2)
        val context = ConcoctionQueueContext(
            runtimeFor = { name ->
                when (name.lowercase()) {
                    "queued ingredient leaf" -> leafRuntime
                    "queued ingredient parent" -> parentRuntime
                    else -> null
                }
            },
        )

        val reservation = ConcoctionQueueReserve.reserve(
            ConcoctionDatabase.getByResult("queued ingredient parent")!!,
            quantity = 2,
            runtime = parentRuntime,
            context = context,
        )
        assertEquals(2, reservation.ingredientCredits[8511])
        assertEquals(-2, ConcoctionQueuedIngredients.creditForRefresh()[8511])

        ConcoctionQueueReserve.release(reservation)
        assertTrue(ConcoctionQueuedIngredients.creditForRefresh().isEmpty())
    }

    @Test
    fun refresh_includesQueuedIngredientCredit() {
        registerItem(8521, "queued refresh leaf")
        registerItem(8522, "queued refresh parent")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "queued refresh leaf",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "queued refresh parent",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(ConcoctionIngredient("queued refresh leaf", 1)),
            ),
        )

        val refreshContext = ConcoctionRefreshContext(
            itemCount = { name ->
                when (name.lowercase()) {
                    "queued refresh leaf" -> 5
                    else -> 0
                }
            },
            availableCountById = { id -> if (id == 8521) 5 else 0 },
        )
        ConcoctionDatabase.refreshConcoctionsNow(refreshContext)
        assertEquals(5, ConcoctionDatabase.creatableCount("queued refresh parent"))

        val queueContext = ConcoctionQueueContext.fromRefreshContext(refreshContext)
        ConcoctionCraftQueue.push("queued refresh parent", 2, queueContext)
        assertEquals(-2, ConcoctionQueuedIngredients.creditForRefresh()[8521])

        ConcoctionDatabase.refreshAfterQueueMutation()
        assertEquals(3, ConcoctionDatabase.creatableCount("queued refresh parent"))
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
