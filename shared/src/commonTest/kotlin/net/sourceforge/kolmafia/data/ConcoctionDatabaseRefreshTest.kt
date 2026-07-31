package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConcoctionDatabaseRefreshTest {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        ConsumableDatabase.resetForTest()
        ItemDatabase.resetForTest()
        ModifierDatabase.resetOverridesForTest()
    }

    @Test
    fun refreshConcoctionsNow_updatesEffectNameFromModifiers() {
        ModifierDatabase.injectForTest("Item", "test brew", "Effect: Bundled Effect")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "test brew",
                resultQuantity = 1,
                methods = setOf("MIX"),
                ingredients = listOf(ConcoctionIngredient("olive oil", 1)),
            ),
        )
        assertNull(ConcoctionDatabase.getEffectName("test brew"))

        ConcoctionDatabase.refreshConcoctionsNow()

        assertEquals("Bundled Effect", ConcoctionDatabase.getEffectName("test brew"))
    }

    @Test
    fun refreshConcoctionsNow_picksUpRuntimeModifierOverride() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 999,
                name = "test brew",
                descId = "test_brew",
                image = "testbrew.gif",
                primaryUse = ItemPrimaryUse.DRINK,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ModifierDatabase.injectForTest("Item", "test brew", "Effect: Bundled Effect")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "test brew",
                resultQuantity = 1,
                methods = setOf("MIX"),
                ingredients = emptyList(),
            ),
        )
        ModifierDatabase.updateItem(999, "Effect: Runtime Effect")

        ConcoctionDatabase.refreshConcoctionsNow()

        assertEquals("Runtime Effect", ConcoctionDatabase.getEffectName("test brew"))
    }

    @Test
    fun markRecalculateAdventureRange_clearedByRefreshConcoctionsNow() {
        ConcoctionDatabase.markRecalculateAdventureRange()
        assertTrue(ConcoctionDatabase.recalculateAdventureRangeForTest())

        ConcoctionDatabase.refreshConcoctionsNow()

        assertFalse(ConcoctionDatabase.recalculateAdventureRangeForTest())
    }

    @Test
    fun refreshConcoctions_deferredWhenRefreshNotNeeded() {
        ConcoctionDatabase.resetRefreshStateForTest()
        ConcoctionDatabase.refreshConcoctions(force = false)
        assertFalse(ConcoctionDatabase.recalculateAdventureRangeForTest())
    }

    @Test
    fun markRecalculateAdventureRange_refreshRebuildsAverageCache() {
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = "refresh food",
                type = ConsumableType.FOOD,
                amount = 2,
                levelReq = 1,
                quality = ConsumableQuality.GOOD,
                advMin = 3,
                advMax = 7,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "",
            ),
        )
        ConcoctionDatabase.markRecalculateAdventureRange()
        ConcoctionDatabase.refreshConcoctionsNow()

        assertEquals(5.0, ConsumableDatabase.getAverageAdventures("refresh food"))
    }
}
