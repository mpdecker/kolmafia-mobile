package net.sourceforge.kolmafia.campground

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import net.sourceforge.kolmafia.preferences.Preferences

class GardenCropSyncTest {

    private fun prefs() = Preferences(MapSettings())

    @Test
    fun syncFromHtml_pumpkinPatchLevel2() {
        val p = prefs()
        GardenCropSync.syncFromHtml("""<img src="pumpkinpatch_2.gif">""", p)
        assertEquals(2, CampgroundInventorySync.load(p)[GardenCropIds.PUMPKIN])
        assertEquals(2, CampgroundInventorySync.load(p)[GardenCropIds.PUMPKIN_SEEDS])
    }

    @Test
    fun syncFromHtml_mushgardenUsesCropLevelPref() {
        val p = prefs()
        p.setInt("mushroomGardenCropLevel", 4)
        GardenCropSync.syncFromHtml("""<img src="mushgarden.gif">""", p)
        assertEquals(4, CampgroundInventorySync.load(p)[GardenCropIds.MUSHROOM_SPORES])
    }

    @Test
    fun syncFromHtml_grassGardenTallGrassCount() {
        val p = prefs()
        GardenCropSync.syncFromHtml("""<img src="grassgarden3.gif">""", p)
        assertEquals(3, CampgroundInventorySync.load(p)[GardenCropIds.TALL_GRASS_SEEDS])
    }

    @Test
    fun syncFromHtml_rockGardenGravelRow() {
        val p = prefs()
        GardenCropSync.syncFromHtml("""<img src="/rockgarden/a2.gif">""", p)
        val load = CampgroundInventorySync.load(p)
        assertEquals(2, load[GardenCropIds.GROVELING_GRAVEL])
        assertEquals(1, load[GardenCropIds.ROCK_SEEDS])
    }

    @Test
    fun clearCrop_removesStaleRows() {
        val p = prefs()
        CampgroundInventorySync.setItem(p, GardenCropIds.PUMPKIN, 3)
        CampgroundInventorySync.setItem(p, GardenCropIds.PUMPKIN_SEEDS, 2)
        GardenCropSync.clearCrop(p)
        val load = CampgroundInventorySync.load(p)
        assertNull(load[GardenCropIds.PUMPKIN])
        assertNull(load[GardenCropIds.PUMPKIN_SEEDS])
    }
}
