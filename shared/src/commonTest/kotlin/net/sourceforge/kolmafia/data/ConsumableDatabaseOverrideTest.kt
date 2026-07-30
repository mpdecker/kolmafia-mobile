package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConsumableDatabaseOverrideTest {

    private val foodName = "override-test-food"

    @BeforeTest
    fun setUp() {
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = foodName,
                type = ConsumableType.FOOD,
                amount = 2,
                levelReq = 3,
                quality = ConsumableQuality.DECENT,
                advMin = 4,
                advMax = 5,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "bundled",
            ),
        )
    }

    @AfterTest
    fun tearDown() {
        ConsumableDatabase.resetForTest()
    }

    @Test
    fun updateConsumable_setsFoodAmountAndAdventures() {
        assertTrue(
            ConsumableDatabase.updateConsumable(
                itemName = foodName,
                size = 3,
                level = 3,
                quality = ConsumableQuality.GOOD,
                adv = "9",
                mus = "0",
                myst = "0",
                mox = "0",
                notes = "override",
            ),
        )
        assertEquals(3, ConsumableDatabase.getFullnessByName(foodName))
        assertEquals("9", ConsumableDatabase.getAdventureRange(foodName))
        assertEquals("good", ConsumableDatabase.getQualityName(foodName))
        assertEquals("override", ConsumableDatabase.getNotesByName(foodName))
    }

    @Test
    fun resetOverrides_restoresBundledAfterOverride() {
        ConsumableDatabase.updateConsumable(
            itemName = foodName,
            size = 3,
            level = 3,
            quality = ConsumableQuality.GOOD,
            adv = "9",
            mus = "0",
            myst = "0",
            mox = "0",
            notes = "override",
        )
        ConsumableDatabase.resetOverrides()
        assertEquals(2, ConsumableDatabase.getFullnessByName(foodName))
        assertEquals("4-5", ConsumableDatabase.getAdventureRange(foodName))
        assertEquals("decent", ConsumableDatabase.getQualityName(foodName))
        assertEquals("bundled", ConsumableDatabase.getNotesByName(foodName))
    }
}
