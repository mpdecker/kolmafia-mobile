package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.CrimboPastChoiceSync
import net.sourceforge.kolmafia.request.MiscShopTokenResponseParse
import net.sourceforge.kolmafia.request.SkeletonOfCrimboPastRequest
import net.sourceforge.kolmafia.shop.CoinmasterVisitInventory

/**
 * Focused HTTP parse-depth leftovers Track C coverage (phases 7311–7330).
 * Parent wrap bumps REVISION to phase7330.
 */
class GameRuntimeLibraryPhase7330Test {

    @AfterTest
    fun cleanup() {
        CoinmasterVisitInventory.resetForTest()
    }

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    @Test
    fun revision_isPhase7330() {
        assertEquals("phase7510", GameRuntimeLibrary.REVISION)
        assertEquals("7510", outputLib(GameRuntimeLibrary(), "print(get_revision());").trim())
    }

    @Test
    fun applySpecial_overlaysDailySpecialBelowSmokingPope() {
        CoinmasterVisitInventory.resetForTest()
        val p = prefs {
            putInt("_crimboPastDailySpecialItem", 100)
            putInt("_crimboPastDailySpecialPrice", 4)
        }
        assertTrue(SkeletonOfCrimboPastRequest.applySpecial(p))
        val row = CoinmasterVisitInventory.findBuyRow(SkeletonOfCrimboPastRequest.SHOP_ID, 100)
        assertEquals(100, row?.item?.itemId)
        assertEquals(4, row?.costs?.firstOrNull()?.count)
        assertEquals(MiscShopTokenResponseParse.KNUCKLEBONE, row?.costs?.firstOrNull()?.itemId)
        CoinmasterVisitInventory.resetForTest()
    }

    @Test
    fun crimboPastVisit_injectsDailySpecialOverlay() {
        CoinmasterVisitInventory.resetForTest()
        val p = prefs()
        val html = """
            <b>Daily Special:</b> descitem(555) (9 knucklebones)
            Buy a Smoking Pope
            Buy a prize turkey
            Buy medical gruel
        """.trimIndent()
        assertTrue(
            CrimboPastChoiceSync.applyVisit(
                choiceId = 1567,
                html = html,
                preferences = p,
                itemIdFromDesc = { if (it == "555") 42 else null },
            ),
        )
        assertEquals(42, p.getInt("_crimboPastDailySpecialItem", 0))
        assertEquals(9, p.getInt("_crimboPastDailySpecialPrice", 0))
        val row = CoinmasterVisitInventory.findBuyRow(SkeletonOfCrimboPastRequest.SHOP_ID, 42)
        assertEquals(9, row?.costs?.firstOrNull()?.count)
        CoinmasterVisitInventory.resetForTest()
    }
}
