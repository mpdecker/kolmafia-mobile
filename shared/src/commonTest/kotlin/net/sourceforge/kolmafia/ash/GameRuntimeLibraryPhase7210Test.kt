package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.AppleStoreRequestHub
import net.sourceforge.kolmafia.request.MiscShopTokenResponseParse
import net.sourceforge.kolmafia.shop.NpcShopSync
import net.sourceforge.kolmafia.shop.TimeTowerSync

/**
 * Focused HTTP Residual LV Track C coverage (phases 7191–7210).
 * Parent wrap bumps REVISION to phase7210.
 */
class GameRuntimeLibraryPhase7210Test {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    private fun inventory(): InventoryManager =
        InventoryManager(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }), GameEventBus())

    @Test
    fun revision_isPhase7210() {
        assertEquals("phase7210", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun npcShopSync_mayoclinic_setsMayoLevelString() {
        val p = prefs()
        NpcShopSync.syncFromStoreHtml(
            storeKey = "mayoclinic",
            html = """
                Mayo clinic open.
                blood mayonnaise concentration: 42 mayograms
                Soak in the Mayo Tank
            """.trimIndent(),
            prefs = p,
            ascensionNumber = 1,
            url = "shop.php?whichshop=mayoclinic",
        )
        assertEquals("42", p.getString("mayoLevel"))
    }

    @Test
    fun appleStoreHub_syncsTimeTowerAndChronerInventory() {
        val p = prefs { putBoolean(TimeTowerSync.PREF, false) }
        val inv = inventory()
        assertTrue(
            AppleStoreRequestHub.parseResponse(
                url = "shop.php?whichshop=applestore",
                html = "Welcome. You have 15 Chroner in your pocket.",
                preferences = p,
                inventory = inv,
            ),
        )
        assertEquals(true, p.getBoolean(TimeTowerSync.PREF, false))
        assertEquals(15, inv.getCount(MiscShopTokenResponseParse.CHRONER))
    }

    @Test
    fun appleStoreHub_marksTimeTowerUnavailableWhenShopGone() {
        val p = prefs { putBoolean(TimeTowerSync.PREF, true) }
        AppleStoreRequestHub.parseResponse(
            url = "shop.php?whichshop=applestore",
            html = "That store isn't there anymore.",
            preferences = p,
            inventory = null,
        )
        assertEquals(false, p.getBoolean(TimeTowerSync.PREF, true))
    }

    @Test
    fun chronerTokenItemId_is7567() {
        assertEquals(7567, MiscShopTokenResponseParse.CHRONER)
    }
}
