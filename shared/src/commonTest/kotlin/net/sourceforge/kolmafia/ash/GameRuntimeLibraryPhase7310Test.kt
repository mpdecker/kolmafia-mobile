package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.data.ConcoctionMayoQueue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.NpcShopSync

/**
 * Focused HTTP parse-depth leftovers Track B coverage (phases 7291–7310).
 * Parent wrap bumps REVISION to phase7330.
 */
class GameRuntimeLibraryPhase7310Test {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    @Test
    fun mayoclinic_setsCurrentWorkshedItem() {
        val p = prefs()
        NpcShopSync.syncFromStoreHtml(
            storeKey = "mayoclinic",
            html = "Mayo clinic open. blood mayonnaise concentration: 9 mayograms",
            prefs = p,
            ascensionNumber = 1,
            url = "shop.php?whichshop=mayoclinic",
        )
        assertEquals(ConcoctionMayoQueue.MAYO_CLINIC, CampgroundItemSync.currentWorkshedItemId(p))
        assertEquals("9", p.getString("mayoLevel"))
        assertTrue(CampgroundItemSync.hasWorkshedItem(p, ConcoctionMayoQueue.MAYO_CLINIC))
    }

    @Test
    fun mayoclinic_ajaxVisitStillSetsWorkshed() {
        val p = prefs()
        NpcShopSync.applyShopVisit(
            html = "Mayo clinic installed.",
            url = "shop.php?whichshop=mayoclinic&ajax=1",
            prefs = p,
            ascensionNumber = 1,
        )
        assertEquals(ConcoctionMayoQueue.MAYO_CLINIC, CampgroundItemSync.currentWorkshedItemId(p))
        assertEquals("", p.getString("mayoLevel", ""))
    }

    @Test
    fun setCurrentWorkshedItem_recordsPrevious() {
        val p = prefs { putInt(CampgroundItemSync.CURRENT_WORKSHED_ITEM_ID_PREF, 9508) }
        CampgroundItemSync.setCurrentWorkshedItem(p, ConcoctionMayoQueue.MAYO_CLINIC)
        assertEquals(9508, p.getInt("_previousWorkshedItemId", 0))
        assertTrue(p.getBoolean("_workshedChanged", false))
        assertEquals(ConcoctionMayoQueue.MAYO_CLINIC, CampgroundItemSync.currentWorkshedItemId(p))
    }
}
