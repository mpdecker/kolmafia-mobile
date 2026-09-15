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
import net.sourceforge.kolmafia.request.LegacyCoinmasterResponseParse
import net.sourceforge.kolmafia.request.MiscShopTokenResponseParse
import net.sourceforge.kolmafia.request.NuggletCraftingRequestHub

/**
 * Focused HTTP Residual LV Track A coverage (phases 7151–7170).
 * Parent wrap bumps REVISION to phase7210.
 */
class GameRuntimeLibraryPhase7170Test {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    private fun inventory(): InventoryManager =
        InventoryManager(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }), GameEventBus())

    @Test
    fun miscShop_parsesTopiaryAndChronerInventory() {
        val inv = inventory()
        val p = prefs()
        assertTrue(
            MiscShopTokenResponseParse.parseResponse(
                "shop.php?whichshop=topiary",
                "<td>7 topiary nugglet",
                p,
                inv,
            ),
        )
        assertEquals(7, inv.getCount(MiscShopTokenResponseParse.TOPIARY_NUGGLET))
        assertEquals(7, p.getInt("availableNugglets", 0))
        assertTrue(
            MiscShopTokenResponseParse.parseResponse(
                "shop.php?whichshop=applestore",
                "You have 3 Chroner",
                p,
                inv,
            ),
        )
        assertEquals(3, inv.getCount(MiscShopTokenResponseParse.CHRONER))
    }

    @Test
    fun miscShop_rubeesFdkolFunFundsInventory() {
        val inv = inventory()
        val p = prefs()
        assertTrue(
            MiscShopTokenResponseParse.parseResponse(
                "shop.php?whichshop=fantasyrealm",
                "<td>11 Rubees",
                p,
                inv,
            ),
        )
        assertEquals(11, inv.getCount(MiscShopTokenResponseParse.RUBEE))
        assertEquals(11, p.getInt("availableRubees", 0))
        assertTrue(
            MiscShopTokenResponseParse.parseResponse(
                "shop.php?whichshop=fdkol",
                "<td>2 FDKOL commendation",
                p,
                inv,
            ),
        )
        assertEquals(2, inv.getCount(MiscShopTokenResponseParse.FDKOL_COMMENDATION))
        assertTrue(
            MiscShopTokenResponseParse.parseResponse(
                "shop.php?whichshop=landfillstore",
                "<td>4 FunFunds",
                p,
                inv,
            ),
        )
        assertEquals(4, inv.getCount(MiscShopTokenResponseParse.FUN_FUNDS))
    }

    @Test
    fun legacyIsotope_syncsInventory() {
        val inv = inventory()
        val p = prefs()
        assertTrue(
            LegacyCoinmasterResponseParse.parseResponse(
                "shop.php?whichshop=isotope",
                "You have 5 lunar isotopes",
                p,
                inv,
            ),
        )
        assertEquals(5, p.getInt("availableLunarIsotopes", 0))
        assertEquals(5, inv.getCount(MiscShopTokenResponseParse.LUNAR_ISOTOPE))
    }

    @Test
    fun nuggletHub_delegatesToMiscShopToken() {
        val inv = inventory()
        val p = prefs()
        NuggletCraftingRequestHub.parseResponse(
            "shop.php?whichshop=topiary",
            "<td>9 topiary nugglet",
            p,
            inv,
        )
        assertEquals(9, inv.getCount(MiscShopTokenResponseParse.TOPIARY_NUGGLET))
        assertEquals(9, p.getInt("availableNugglets", 0))
    }
}
