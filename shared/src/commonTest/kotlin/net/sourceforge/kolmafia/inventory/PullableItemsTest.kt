package net.sourceforge.kolmafia.inventory

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse

class PullableItemsTest {

    private val food = ItemData(
        id = 100,
        name = "test food",
        descId = "desc100",
        image = "f.gif",
        primaryUse = ItemPrimaryUse.FOOD,
        secondaryUses = emptySet(),
        access = emptySet(),
        autosellPrice = 0,
        plural = null,
    )

    private val weapon = ItemData(
        id = 200,
        name = "test weapon",
        descId = "desc200",
        image = "w.gif",
        primaryUse = ItemPrimaryUse.WEAPON,
        secondaryUses = emptySet(),
        access = emptySet(),
        autosellPrice = 0,
        plural = null,
    )

    private val db = object : GameDatabase() {
        override fun item(id: Int): ItemData? = when (id) {
            food.id -> food
            weapon.id -> weapon
            else -> null
        }
    }

    @Test
    fun pullableInLoL_foodIsPullable() {
        assertTrue(PullableItems.pullableInLoL(food.id, food))
    }

    @Test
    fun pullableInLoL_equipmentIsNotPullable() {
        assertFalse(PullableItems.pullableInLoL(weapon.id, weapon))
    }

    @Test
    fun pullableInLoL_mayamCalendarBlocked() {
        assertFalse(PullableItems.pullableInLoL(PullableItems.MAYAM_CALENDAR_ID, food))
    }

    @Test
    fun pullableInSeaPath_blockedSeaItem() {
        assertFalse(PullableItems.pullableInSeaPath(3487))
    }

    @Test
    fun pullableInSeaPath_normalItem() {
        assertTrue(PullableItems.pullableInSeaPath(9999))
    }
}
