package net.sourceforge.kolmafia.campground

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.preferences.Preferences

class CampgroundInventorySyncTest {

    private fun prefs() = Preferences(MapSettings())

    @Test
    fun syncFromHtml_recordsWorkshedAndOutsideItems() {
        val p = prefs()
        CampgroundInventorySync.syncFromHtml(
            """
            <img src="/rest3.gif">
            <img src="wbchemset.gif">
            <img src="chesstable.gif">
            """.trimIndent(),
            p,
        )
        assertEquals(143, p.getInt(DwellingSync.CURRENT_DWELLING_ITEM_ID_PREF, -1))
        assertEquals(1, CampgroundInventorySync.load(p)[6967])
        assertEquals(1, CampgroundInventorySync.load(p)[8989])
    }

    @Test
    fun setItem_removesZeroCounts() {
        val p = prefs()
        CampgroundInventorySync.setItem(p, 101, 1)
        CampgroundInventorySync.setItem(p, 101, 0)
        assertEquals(emptyMap(), CampgroundInventorySync.load(p))
    }
}
