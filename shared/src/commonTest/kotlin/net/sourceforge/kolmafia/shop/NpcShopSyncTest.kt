package net.sourceforge.kolmafia.shop

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class NpcShopSyncTest {

    private fun prefs(): Preferences = Preferences(MapSettings())

    @Test
    fun wildfire_visitMarksBlartBoughtWhenRowMissing() {
        val p = prefs()
        NpcShopSync.applyWildfireVisit(
            html = "<html></html>",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=wildfire",
            prefs = p,
        )
        assertTrue(p.getBoolean("itemBoughtPerAscension10790", false))
    }

    @Test
    fun wildfire_visitLeavesBlartAvailableWhenRowPresent() {
        val p = prefs()
        NpcShopSync.applyWildfireVisit(
            html = """<tr rel="10790"><td>B. L. A. R. T.</td></tr>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=wildfire",
            prefs = p,
        )
        assertFalse(p.getBoolean("itemBoughtPerAscension10790", false))
    }

    @Test
    fun wildfire_purchaseSetsBlartPref() {
        val p = prefs()
        NpcShopSync.applyWildfirePurchase(
            html = "You acquire an item: <b>B. L. A. R. T.</b>",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=wildfire&action=buyitem",
            itemId = 10790,
            prefs = p,
        )
        assertTrue(p.getBoolean("itemBoughtPerAscension10790", false))
    }

    @Test
    fun wildfire_skipsAjaxVisit() {
        val p = prefs()
        NpcShopSync.applyWildfireVisit(
            html = "<html></html>",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=wildfire&ajax=1",
            prefs = p,
        )
        assertFalse(p.getBoolean("itemBoughtPerAscension10790", false))
    }

    @Test
    fun syncFromStoreHtml_wildfireMarksBlartBoughtWhenRowMissing() {
        val p = prefs()
        NpcShopSync.syncFromStoreHtml("wildfire", "<html></html>", p, ascensionNumber = 1)
        assertTrue(p.getBoolean("itemBoughtPerAscension10790", false))
    }

    @Test
    fun needsSync_trueForWildfire() {
        assertTrue(NpcShopSync.needsSync("wildfire"))
        assertTrue(NpcShopSync.needsSync("hippy"))
        assertTrue(NpcShopSync.needsSync("fwshop"))
        assertTrue(NpcShopSync.needsSync("mayoclinic"))
        assertFalse(NpcShopSync.needsSync("gnoll"))
    }

    @Test
    fun hippy_syncDetectsHippySide() {
        val p = prefs()
        NpcShopSync.syncFromStoreHtml(
            storeKey = "hippy",
            html = "peach pear plum for sale",
            prefs = p,
            ascensionNumber = 3,
        )
        assertEquals(3, p.getInt("lastFilthClearance", -1))
        assertEquals("hippy", p.getString("currentHippyStore", ""))
        assertEquals("hippy", p.getString("sidequestOrchardCompleted", ""))
    }

    @Test
    fun hippy_syncDetectsFratboySide() {
        val p = prefs()
        NpcShopSync.syncFromStoreHtml(
            storeKey = "hippy",
            html = "bowl of rye sprouts cob of corn juniper berries",
            prefs = p,
            ascensionNumber = 2,
        )
        assertEquals(2, p.getInt("lastFilthClearance", -1))
        assertEquals("fratboy", p.getString("currentHippyStore", ""))
    }

    @Test
    fun fwshop_syncSetsSectionPrefs() {
        val p = prefs()
        NpcShopSync.syncFromStoreHtml(
            storeKey = "fwshop",
            html = """<b>Combat Explosives</b><b>Dangerous Hats</b><b>Explosive Equipment</b>""",
            prefs = p,
            ascensionNumber = 1,
        )
        assertTrue(p.getBoolean("_fireworksShop", false))
        assertFalse(p.getBoolean("_fireworksShopHatBought", true))
        assertFalse(p.getBoolean("_fireworksShopEquipmentBought", true))
    }

    @Test
    fun mayoclinic_syncSetsRentalPrefs() {
        val p = prefs()
        NpcShopSync.syncFromStoreHtml(
            storeKey = "mayoclinic",
            html = "Mayo Clinic miracle whip Soak in the Mayo Tank",
            prefs = p,
            ascensionNumber = 1,
        )
        assertFalse(p.getBoolean("_mayoDeviceRented", true))
        assertFalse(p.getBoolean("itemBoughtPerAscension8266", true))
        assertFalse(p.getBoolean("_mayoTankSoaked", false))
    }

    @Test
    fun applyShopVisit_routesHippyFromShopUrl() {
        val p = prefs()
        NpcShopSync.applyShopVisit(
            html = "peach pear plum",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=hippy",
            prefs = p,
            ascensionNumber = 4,
        )
        assertEquals("hippy", p.getString("currentHippyStore", ""))
    }

    @Test
    fun hiddentavern_syncSetsUnlockPref() {
        val p = prefs()
        NpcShopSync.syncFromStoreHtml(
            storeKey = "hiddentavern",
            html = "<html>The Hidden Tavern</html>",
            prefs = p,
            ascensionNumber = 5,
        )
        assertEquals(5, p.getInt("hiddenTavernUnlock", -1))
    }

    @Test
    fun applyShopVisit_routesHiddenTavernFromStoreUrl() {
        val p = prefs()
        NpcShopSync.applyShopVisit(
            html = "<html>Hidden Tavern</html>",
            url = "https://www.kingdomofloathing.com/store.php?whichstore=hiddentavern",
            prefs = p,
            ascensionNumber = 2,
        )
        assertEquals(2, p.getInt("hiddenTavernUnlock", -1))
    }

    @Test
    fun needsSync_trueForHiddenTavern() {
        assertTrue(NpcShopSync.needsSync("hiddentavern"))
    }
}
