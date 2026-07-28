package net.sourceforge.kolmafia.shop

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.preferences.Preferences

class NpcShopSyncTest {

    private fun prefs(): Preferences = Preferences(MapSettings())

    @Test
    fun bartlebys_setsEphemeraPrefsFromHtml() {
        val prefs = prefs()
        NpcShopSync.syncFromStoreHtml(
            shopId = "bartlebys",
            html = """<html>Today's special: pirate pamphlet on sale!</html>""",
            prefs = prefs,
            ascensionNumber = 7,
        )
        assertEquals(7, prefs.getInt("lastPirateEphemeraReset", -1))
        assertEquals("pirate pamphlet", prefs.getString("lastPirateEphemera", ""))
    }

    @Test
    fun hippy_setsFilthClearanceAndHippySide() {
        val prefs = prefs()
        NpcShopSync.syncFromStoreHtml(
            shopId = "hippy",
            html = """<html>peach pear plum for sale</html>""",
            prefs = prefs,
            ascensionNumber = 3,
        )
        assertEquals(3, prefs.getInt("lastFilthClearance", -1))
        assertEquals("hippy", prefs.getString("currentHippyStore", ""))
        assertEquals("hippy", prefs.getString("sidequestOrchardCompleted", ""))
    }

    @Test
    fun hippy_setsFratboySideFromSprouts() {
        val prefs = prefs()
        NpcShopSync.syncFromStoreHtml(
            shopId = "hippy",
            html = """<html>bowl of rye sprouts cob of corn juniper berries</html>""",
            prefs = prefs,
            ascensionNumber = 2,
        )
        assertEquals(2, prefs.getInt("lastFilthClearance", -1))
        assertEquals("fratboy", prefs.getString("currentHippyStore", ""))
    }
}
