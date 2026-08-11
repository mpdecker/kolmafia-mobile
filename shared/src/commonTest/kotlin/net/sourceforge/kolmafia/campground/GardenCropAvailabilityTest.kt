package net.sourceforge.kolmafia.campground

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class GardenCropAvailabilityTest {

    private fun prefs(configure: Preferences.() -> Unit = {}) =
        Preferences(MapSettings()).also(configure)

    @Test
    fun hasCropOrBetter_anyWhenCropPresent() {
        val p = prefs {
            CampgroundInventorySync.setItem(this, GardenCropIds.PUMPKIN, 1)
        }
        assertTrue(GardenCropAvailability.hasCropOrBetter(p, "any"))
    }

    @Test
    fun hasCropOrBetter_tierComparisonAcceptsBetterCrop() {
        val p = prefs {
            CampgroundInventorySync.setItem(this, GardenCropIds.HUGE_PUMPKIN, 1)
        }
        assertTrue(GardenCropAvailability.hasCropOrBetter(p, "pumpkin"))
    }

    @Test
    fun hasCropOrBetter_rejectsWhenGardenEmpty() {
        val p = prefs()
        assertFalse(GardenCropAvailability.hasCropOrBetter(p, "pumpkin"))
    }

    @Test
    fun parseCrop_tallGrassWithCount() {
        val crop = GardenCropAvailability.parseCrop("tall grass (3)")
        assertEquals(GardenCropIds.TALL_GRASS_SEEDS, crop.itemId)
        assertEquals(3, crop.count)
    }

    @Test
    fun getCrop_returnsFirstMatchingItem() {
        val p = prefs {
            CampgroundInventorySync.setItem(this, GardenCropIds.BARLEY, 6)
        }
        val crop = GardenCropAvailability.getCrop(p)
        assertEquals(GardenCropIds.BARLEY, crop?.itemId)
        assertEquals(6, crop?.count)
    }
}
