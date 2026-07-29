package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterAccessibility
import net.sourceforge.kolmafia.shop.CoinmasterData
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.CoinmasterPurchaseProbe
import net.sourceforge.kolmafia.shop.ShopInventorySync

class GameRuntimeLibraryAshP160Test {

    private companion object {
        const val FLAK_SHIELD = 11920
        const val CHRONER = 7567
    }

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        CoinmasterDatabase.resetForTest()
    }

    private fun registerItem(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    @Test
    fun alliedHq_flakShieldBlockedBeforeSyncAllowedAfter() {
        registerItem(FLAK_SHIELD, "flak shield")
        registerItem(CHRONER, "Chroner")
        CoinmasterDatabase.loadFromText(
            shopsText = "twitch_alliedhq\tAllied HQ\n",
            coinText = "Allied HQ\tROW1599\tflak shield\tChroner (20)\n",
        )
        val p = Preferences(MapSettings())
        p.setBoolean("autoSatisfyWithCoinmasters", true)
        val state = CharacterState(meat = 100_000)
        assertFalse(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                FLAK_SHIELD,
                state,
                p,
                accessibleCount = { if (it == CHRONER) 100 else 0 },
            ),
        )
        ShopInventorySync.parseAndLearn(
            html = """<b>flak shield</b> Chroner (20)""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=twitch_alliedhq",
            prefs = p,
        )
        assertTrue(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                FLAK_SHIELD,
                state,
                p,
                accessibleCount = { if (it == CHRONER) 100 else 0 },
            ),
        )
    }

    @Test
    fun alliedHq_inaccessibleWhenTimeTowerUnavailable() {
        val master = CoinmasterData(
            masterName = "Allied HQ",
            nickname = "twitch_alliedhq",
            token = "Chroner",
            shopId = "twitch_alliedhq",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )
        val p = Preferences(MapSettings())
        assertEquals(
            "You can't get to the Allied HQ",
            CoinmasterAccessibility.inaccessibleReason(master, CharacterState(), p),
        )
    }
}
