package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConcoctionCreatableRegistryTest {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun refresh_addsEntryWhenCreatablePositive() {
        registerItems(
            8501 to "registry parent",
            8502 to "registry leaf",
        )
        injectSmithTree("registry")

        val context = ConcoctionRefreshContext.fromAggregatedCounts(mapOf(8502 to 3))
        ConcoctionDatabase.refreshConcoctionsNow(context)

        val entries = ConcoctionDatabase.getCreatables()
        assertEquals(1, entries.size)
        assertEquals("registry parent", entries.single().resultName)
        assertEquals(8501, entries.single().itemId)
        assertEquals(3, entries.single().creatable)
        assertEquals(0, entries.single().pullable)
        assertEquals(setOf("SMITH"), entries.single().methods)
        assertTrue(ConcoctionDatabase.getRuntime("registry parent")?.wasPossible == true)
    }

    @Test
    fun refresh_removesEntryWhenCreatableDropsToZero() {
        registerItems(
            8511 to "registry drop parent",
            8512 to "registry drop leaf",
        )
        injectSmithTree("registry drop")

        ConcoctionDatabase.refreshConcoctionsNow(
            ConcoctionRefreshContext.fromAggregatedCounts(mapOf(8512 to 2)),
        )
        assertEquals(1, ConcoctionDatabase.getCreatables().size)

        ConcoctionDatabase.refreshConcoctionsNow(
            ConcoctionRefreshContext.fromAggregatedCounts(mapOf(8512 to 0)),
        )
        assertEquals(0, ConcoctionDatabase.getCreatables().size)
        assertFalse(ConcoctionDatabase.getRuntime("registry drop parent")?.wasPossible == true)
    }

    @Test
    fun refresh_includesPullableOnly() {
        registerItems(8521 to "registry pull item")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "registry pull item",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.setPullsRemaining(5)
        ConcoctionDatabase.setPullsBudgeted(5)

        ConcoctionDatabase.refreshConcoctionsNow(
            ConcoctionRefreshContext(
                itemCount = { 0 },
                storageCountById = { id -> if (id == 8521) 3 else 0 },
                considerPulls = true,
            ),
        )

        val entry = ConcoctionCreatableRegistry.get("registry pull item")
        assertEquals(0, entry?.creatable)
        assertEquals(3, entry?.pullable)
        assertTrue(ConcoctionDatabase.getRuntime("registry pull item")?.wasPossible == true)
    }

    @Test
    fun refresh_skipsNoItemConcoction() {
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "registry ghost item",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )

        ConcoctionDatabase.refreshConcoctionsNow(ConcoctionRefreshContext.EMPTY)
        assertEquals(0, ConcoctionDatabase.getCreatables().size)
        assertNull(ConcoctionCreatableRegistry.get("registry ghost item"))
    }

    @Test
    fun wasPossible_preservedAcrossRefresh() {
        registerItems(
            8531 to "registry toggle parent",
            8532 to "registry toggle leaf",
        )
        injectSmithTree("registry toggle")

        ConcoctionDatabase.refreshConcoctionsNow(
            ConcoctionRefreshContext.fromAggregatedCounts(mapOf(8532 to 1)),
        )
        assertTrue(ConcoctionDatabase.getRuntime("registry toggle parent")?.wasPossible == true)

        ConcoctionDatabase.refreshConcoctionsNow(
            ConcoctionRefreshContext.fromAggregatedCounts(mapOf(8532 to 1)),
        )
        assertTrue(ConcoctionDatabase.getRuntime("registry toggle parent")?.wasPossible == true)

        ConcoctionDatabase.refreshConcoctionsNow(
            ConcoctionRefreshContext.fromAggregatedCounts(mapOf(8532 to 0)),
        )
        assertFalse(ConcoctionDatabase.getRuntime("registry toggle parent")?.wasPossible == true)
    }

    @Test
    fun getCreatables_sortedByResultName() {
        registerItems(
            8541 to "registry z parent",
            8542 to "registry z leaf",
            8551 to "registry a parent",
            8552 to "registry a leaf",
        )
        injectSmithTree("registry z", leafId = 8542, parentId = 8541)
        injectSmithTree("registry a", leafId = 8552, parentId = 8551)

        ConcoctionDatabase.refreshConcoctionsNow(
            ConcoctionRefreshContext.fromAggregatedCounts(
                mapOf(
                    8542 to 2,
                    8552 to 2,
                ),
            ),
        )

        val names = ConcoctionDatabase.getCreatables().map { it.resultName.lowercase() }
        assertEquals(listOf("registry a parent", "registry z parent"), names)
    }

    @Test
    fun refresh_skipsEmptyMethodsConcoction() {
        registerItems(8561 to "registry no method")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "registry no method",
                resultQuantity = 1,
                methods = emptySet(),
                ingredients = emptyList(),
            ),
        )

        ConcoctionDatabase.setRuntimeForTest(
            "registry no method",
            ConcoctionRuntimeState(initial = 0, creatable = 5, total = 5, visibleTotal = 5),
        )
        ConcoctionCreatableRegistry.updateFromRefresh()
        assertEquals(0, ConcoctionDatabase.getCreatables().size)
    }

    private fun injectSmithTree(
        prefix: String,
        leafId: Int = when (prefix) {
            "registry" -> 8502
            "registry drop" -> 8512
            "registry toggle" -> 8532
            else -> 8502
        },
        parentId: Int = leafId - 1,
    ) {
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "$prefix leaf",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "$prefix parent",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = listOf(ConcoctionIngredient("$prefix leaf", 1)),
            ),
        )
    }

    private fun registerItems(vararg items: Pair<Int, String>) {
        for ((id, name) in items) {
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
}
