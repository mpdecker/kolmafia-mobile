package net.sourceforge.kolmafia.data

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.preferences.Preferences

class ConcoctionMayoQueueTest {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        ItemDatabase.resetForTest()
        ConsumableDatabase.resetForTest()
    }

    @Test
    fun canQueueFood_mayoWithClinicAndFoodQueue() {
        registerItem(ConcoctionMayoQueue.MAYONEX, "mayonex")
        val prefs = Preferences(MapSettings())
        prefs.setInt(CampgroundItemSync.CURRENT_WORKSHED_ITEM_ID_PREF, ConcoctionMayoQueue.MAYO_CLINIC)
        val context = ConcoctionQueueContext(
            preferences = prefs,
            foodQueueDepth = { 1 },
            getStringPref = { "" },
        )
        assertTrue(ConcoctionMayoQueue.canQueueFood(ConcoctionMayoQueue.MAYONEX, context))
        assertEquals(
            ConcoctionOrganAmounts.QueueBucket.FOOD,
            ConcoctionOrganAmounts.queueBucket("mayonex", context = context),
        )
    }

    @Test
    fun canQueueBooze_thermos() {
        val thermosId = 4413
        registerItem(thermosId, "schrodinger's thermos")
        assertTrue(ConcoctionMayoQueue.canQueueBooze(thermosId))
        assertEquals(
            ConcoctionOrganAmounts.QueueBucket.BOOZE,
            ConcoctionOrganAmounts.queueBucket(
                "schrodinger's thermos",
                context = ConcoctionQueueContext(),
            ),
        )
    }

    @Test
    fun mayodiol_swapAdjustsOrganCounters() {
        registerItem(ConcoctionMayoQueue.MAYODIOL, "mayodiol")
        registerConsumable("mayodiol", ConsumableType.FOOD, amount = 1)
        registerConsumable("queued food after mayo", ConsumableType.FOOD, amount = 2)

        ConcoctionQueueBudget.lastQueuedMayo = ConcoctionMayoQueue.MAYODIOL
        val delta = ConcoctionOrganQueueReserve.reserve("queued food after mayo", 1, ConcoctionQueueContext())
        assertTrue(delta.mayodiolSwapApplied)
        assertEquals(1, ConcoctionQueueBudget.queuedFullness)
        assertEquals(1, ConcoctionQueueBudget.queuedInebriety)
    }

    private fun registerConsumable(name: String, type: ConsumableType, amount: Int) {
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
