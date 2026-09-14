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
import net.sourceforge.kolmafia.request.CrimboHubResponseParse
import net.sourceforge.kolmafia.request.MiscShopTokenResponseParse
import net.sourceforge.kolmafia.shop.CoinmasterAccessContext
import net.sourceforge.kolmafia.shop.CoinmasterData
import net.sourceforge.kolmafia.shop.CoinmasterPurchasePrefs
import net.sourceforge.kolmafia.shop.ResidualLegacyShopAccessibility

/**
 * Focused HTTP Residual LIII Track A–C coverage (phases 7031–7090).
 * Parent wrap bumps REVISION to phase7150.
 */
class GameRuntimeLibraryPhase7050Test {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    private fun inventory(): InventoryManager =
        InventoryManager(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }), GameEventBus())

    @Test
    fun revision_isPhase7090() {
        assertEquals("phase7150", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun accessContext_carriesFamiliarAndGenerator() {
        val ctx = CoinmasterAccessContext(
            ownsFamiliar = { id -> id == ResidualLegacyShopAccessibility.SKELETON_OF_CRIMBO_PAST },
            generatorQuestFinished = true,
        )
        assertTrue(ctx.ownsFamiliar(ResidualLegacyShopAccessibility.SKELETON_OF_CRIMBO_PAST))
        assertTrue(ctx.generatorQuestFinished)
        assertEquals(
            null,
            ResidualLegacyShopAccessibility.skeletonOfCrimboPastInaccessible(ctx.ownsFamiliar),
        )
    }

    @Test
    fun miscShop_parsesBaconAndGloverTokens() {
        val inv = inventory()
        assertTrue(
            MiscShopTokenResponseParse.parseResponse(
                "shop.php?whichshop=bacon",
                "You have 42 BACON",
                prefs(),
                inv,
            ),
        )
        assertEquals(42, inv.getCount(MiscShopTokenResponseParse.BACON_ITEM))

        assertTrue(
            MiscShopTokenResponseParse.parseResponse(
                "shop.php?whichshop=glover",
                "You have 7 G",
                prefs(),
                inv,
            ),
        )
        assertEquals(7, inv.getCount(MiscShopTokenResponseParse.G_ITEM))
    }

    @Test
    fun miscShop_parsesBoutiqueBlackmarketAndSi() {
        val inv = inventory()
        assertTrue(
            MiscShopTokenResponseParse.parseResponse(
                "shop.php?whichshop=cindy",
                "<td>3 odd silver coin",
                prefs(),
                inv,
            ),
        )
        assertEquals(3, inv.getCount(MiscShopTokenResponseParse.ODD_SILVER_COIN))

        assertTrue(
            MiscShopTokenResponseParse.parseResponse(
                "shop.php?whichshop=blackmarket",
                "<td>11 priceless diamond",
                prefs(),
                inv,
            ),
        )
        assertEquals(11, inv.getCount(MiscShopTokenResponseParse.PRICELESS_DIAMOND))

        assertTrue(
            MiscShopTokenResponseParse.parseResponse(
                "shop.php?whichshop=si_shop1",
                "<td>99 Coins-spiracy",
                prefs(),
                inv,
            ),
        )
        assertEquals(99, inv.getCount(MiscShopTokenResponseParse.COINSPIRACY))
    }

    @Test
    fun purchasePrefs_acceptsGameDatabaseArg() {
        val p = prefs()
        CoinmasterPurchasePrefs.applyPurchasedItem(
            CoinmasterData(
                masterName = "Internet Meme Shop",
                nickname = "bacon",
                token = "BACON",
                shopId = "bacon",
                buyItems = emptyList(),
                sellItems = emptyList(),
            ),
            itemId = 9017,
            prefs = p,
            gameDatabase = null,
        )
        assertTrue(p.getBoolean("_internetViralVideoBought", false))
    }

    @Test
    fun crimbo19toys_claimsVisit() {
        val p = prefs()
        assertTrue(
            CrimboHubResponseParse.parseResponse(
                "shop.php?whichshop=crimbo19toys",
                "<html>workshop</html>",
                p,
            ),
        )
        assertTrue(p.getBoolean("_crimbo19ToysVisited", false))
    }
}
