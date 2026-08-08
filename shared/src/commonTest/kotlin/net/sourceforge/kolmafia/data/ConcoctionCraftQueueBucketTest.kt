package net.sourceforge.kolmafia.data

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.preferences.Preferences

class ConcoctionCraftQueueBucketTest {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        ItemDatabase.resetForTest()
        ConsumableDatabase.resetForTest()
    }

    @Test
    fun push_routesFoodAndBoozeToSeparateBuckets() {
        registerConsumable("bucket queue food", ConsumableType.FOOD, amount = 1)
        registerConsumable("bucket queue booze", ConsumableType.DRINK, amount = 1)
        registerCraftTarget("bucket queue food")
        registerCraftTarget("bucket queue booze")

        val context = queueContext()
        assertTrue(ConcoctionCraftQueue.push("bucket queue food", 1, context))
        assertTrue(ConcoctionCraftQueue.push("bucket queue booze", 1, context))
        assertEquals(1, ConcoctionCraftQueue.depth(ConcoctionOrganAmounts.QueueBucket.FOOD))
        assertEquals(1, ConcoctionCraftQueue.depth(ConcoctionOrganAmounts.QueueBucket.BOOZE))
    }

    @Test
    fun pop_bucketOnlyRemovesFromThatBucket() {
        registerConsumable("bucket pop food", ConsumableType.FOOD, amount = 1)
        registerConsumable("bucket pop booze", ConsumableType.DRINK, amount = 1)
        registerCraftTarget("bucket pop food")
        registerCraftTarget("bucket pop booze")

        val context = queueContext()
        ConcoctionCraftQueue.push("bucket pop food", 1, context)
        ConcoctionCraftQueue.push("bucket pop booze", 1, context)
        assertEquals(1, ConcoctionQueueBudget.queuedFullness)
        assertEquals(1, ConcoctionQueueBudget.queuedInebriety)

        val popped = ConcoctionCraftQueue.pop(ConcoctionOrganAmounts.QueueBucket.BOOZE)
        assertEquals("bucket pop booze", popped?.resultName)
        assertEquals(1, ConcoctionCraftQueue.depth(ConcoctionOrganAmounts.QueueBucket.FOOD))
        assertEquals(0, ConcoctionCraftQueue.depth(ConcoctionOrganAmounts.QueueBucket.BOOZE))
        assertEquals(1, ConcoctionQueueBudget.queuedFullness)
        assertEquals(0, ConcoctionQueueBudget.queuedInebriety)
    }

    @Test
    fun pop_globalPreservesLifoAcrossBuckets() {
        registerConsumable("bucket lifo food", ConsumableType.FOOD, amount = 1)
        registerConsumable("bucket lifo booze", ConsumableType.DRINK, amount = 1)
        registerCraftTarget("bucket lifo food")
        registerCraftTarget("bucket lifo booze")

        val context = queueContext()
        ConcoctionCraftQueue.push("bucket lifo food", 1, context)
        ConcoctionCraftQueue.push("bucket lifo booze", 1, context)

        val popped = ConcoctionCraftQueue.pop()
        assertEquals("bucket lifo booze", popped?.resultName)
        assertEquals(1, ConcoctionCraftQueue.depth(ConcoctionOrganAmounts.QueueBucket.FOOD))
        assertEquals(0, ConcoctionCraftQueue.depth(ConcoctionOrganAmounts.QueueBucket.BOOZE))
    }

    @Test
    fun clear_drainsDesktopOrder() {
        registerConsumable("bucket clear food a", ConsumableType.FOOD, amount = 1)
        registerConsumable("bucket clear food b", ConsumableType.FOOD, amount = 1)
        registerConsumable("bucket clear booze", ConsumableType.DRINK, amount = 1)
        registerCraftTarget("bucket clear food a")
        registerCraftTarget("bucket clear food b")
        registerCraftTarget("bucket clear booze")

        val context = queueContext()
        ConcoctionCraftQueue.push("bucket clear food a", 1, context)
        ConcoctionCraftQueue.push("bucket clear booze", 1, context)
        ConcoctionCraftQueue.push("bucket clear food b", 1, context)

        assertEquals("bucket clear food b", ConcoctionCraftQueue.pop(ConcoctionOrganAmounts.QueueBucket.FOOD)?.resultName)
        assertEquals(1, ConcoctionCraftQueue.depth(ConcoctionOrganAmounts.QueueBucket.BOOZE))

        ConcoctionCraftQueue.clear()
        assertEquals(0, ConcoctionCraftQueue.entries().size)
        assertEquals(0, ConcoctionQueueBudget.queuedFullness)
        assertEquals(0, ConcoctionQueueBudget.queuedInebriety)
    }

    @Test
    fun mayoTail_restoresFromFoodBucketOnly() {
        registerItem(ConcoctionMayoQueue.MAYONEX, "bucket mayo mayonex")
        registerConsumableOnly("bucket mayo mayonex", ConsumableType.FOOD, amount = 1)
        registerConsumable("bucket mayo booze", ConsumableType.DRINK, amount = 1)
        registerCraftTarget("bucket mayo mayonex")
        registerCraftTarget("bucket mayo booze")

        val prefs = Preferences(MapSettings())
        prefs.setInt(CampgroundItemSync.CURRENT_WORKSHED_ITEM_ID_PREF, ConcoctionMayoQueue.MAYO_CLINIC)
        val context = queueContext(
            preferences = prefs,
            getStringPref = { "" },
        )

        ConcoctionCraftQueue.push("bucket mayo mayonex", 1, context)
        assertEquals(ConcoctionMayoQueue.MAYONEX, ConcoctionQueueBudget.lastQueuedMayo)
        ConcoctionCraftQueue.push("bucket mayo booze", 1, context)
        assertEquals(0, ConcoctionQueueBudget.lastQueuedMayo)

        ConcoctionCraftQueue.pop(ConcoctionOrganAmounts.QueueBucket.BOOZE)
        assertEquals(0, ConcoctionQueueBudget.lastQueuedMayo)
    }

    @Test
    fun mayoTail_popFoodRestoresMayoFromFoodBucket() {
        registerItem(ConcoctionMayoQueue.MAYONEX, "bucket mayo restore mayonex")
        registerConsumableOnly("bucket mayo restore mayonex", ConsumableType.FOOD, amount = 1)
        registerConsumable("bucket mayo restore food", ConsumableType.FOOD, amount = 2)
        registerCraftTarget("bucket mayo restore mayonex")
        registerCraftTarget("bucket mayo restore food")

        val prefs = Preferences(MapSettings())
        prefs.setInt(CampgroundItemSync.CURRENT_WORKSHED_ITEM_ID_PREF, ConcoctionMayoQueue.MAYO_CLINIC)
        val context = queueContext(
            preferences = prefs,
            getStringPref = { "" },
        )

        ConcoctionCraftQueue.push("bucket mayo restore mayonex", 1, context)
        ConcoctionCraftQueue.push("bucket mayo restore food", 1, context)
        assertEquals(0, ConcoctionQueueBudget.lastQueuedMayo)

        ConcoctionCraftQueue.pop(ConcoctionOrganAmounts.QueueBucket.FOOD)
        assertEquals(ConcoctionMayoQueue.MAYONEX, ConcoctionQueueBudget.lastQueuedMayo)
    }

    @Test
    fun mimeShotglass_undoUsesBoozeBucketRemainder() {
        registerConsumable("bucket mime drink one", ConsumableType.DRINK, amount = 1)
        registerConsumable("bucket mime drink two", ConsumableType.DRINK, amount = 1)
        registerConsumable("bucket mime food", ConsumableType.FOOD, amount = 1)
        registerItem(9676, "mime army shotglass")
        registerCraftTarget("bucket mime drink one")
        registerCraftTarget("bucket mime drink two")
        registerCraftTarget("bucket mime food")

        val context = queueContext(
            availableCountById = { id -> if (id == 9676) 1 else 0 },
            getBooleanPref = { false },
        )

        ConcoctionCraftQueue.push("bucket mime drink one", 1, context)
        assertTrue(ConcoctionQueueBudget.queuedMimeShotglass)
        ConcoctionCraftQueue.push("bucket mime food", 1, context)
        ConcoctionCraftQueue.push("bucket mime drink two", 1, context)

        ConcoctionCraftQueue.pop(ConcoctionOrganAmounts.QueueBucket.BOOZE)
        assertTrue(ConcoctionQueueBudget.queuedMimeShotglass)

        ConcoctionCraftQueue.pop(ConcoctionOrganAmounts.QueueBucket.BOOZE)
        assertFalse(ConcoctionQueueBudget.queuedMimeShotglass)
    }

    @Test
    fun craftRoutesToPotionBucket() {
        registerItem(82001, "bucket craft potion")
        registerCraftTarget("bucket craft potion")

        val context = queueContext()
        assertTrue(ConcoctionCraftQueue.push("bucket craft potion", 1, context))
        assertEquals(0, ConcoctionCraftQueue.depth(ConcoctionOrganAmounts.QueueBucket.FOOD))
        assertEquals(1, ConcoctionCraftQueue.depth(ConcoctionOrganAmounts.QueueBucket.POTION))
        assertEquals(
            ConcoctionOrganAmounts.QueueBucket.CRAFT,
            ConcoctionCraftQueue.entries(ConcoctionOrganAmounts.QueueBucket.POTION).single().queueBucket,
        )
    }

    private fun queueContext(
        preferences: Preferences? = null,
        availableCountById: (Int) -> Int = { 0 },
        getBooleanPref: (String) -> Boolean = { DefaultsDatabase.getBoolean(it) },
        getStringPref: (String) -> String = { DefaultsDatabase.getString(it) },
    ): ConcoctionQueueContext =
        ConcoctionQueueContext(
            preferences = preferences,
            availableCountById = availableCountById,
            getBooleanPref = getBooleanPref,
            getStringPref = getStringPref,
            runtimeFor = { name -> ConcoctionDatabase.getRuntime(name) },
        )

    private fun registerCraftTarget(name: String) {
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = name,
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.setRuntimeForTest(
            name.lowercase(),
            ConcoctionRuntimeState(initial = 0, creatable = 1),
        )
    }

    private fun registerConsumable(name: String, type: ConsumableType, amount: Int) {
        registerConsumableOnly(name, type, amount)
        registerItem(name.hashCode() and 0x7FFF, name)
    }

    private fun registerConsumableOnly(name: String, type: ConsumableType, amount: Int) {
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = name,
                type = type,
                amount = amount,
                levelReq = 1,
                quality = ConsumableQuality.DECENT,
                advMin = 0,
                advMax = 0,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "",
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
