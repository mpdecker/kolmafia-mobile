package net.sourceforge.kolmafia.shop

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class CoinmasterPurchasePrefsTest {

    private fun prefs(): Preferences = Preferences(MapSettings())

    @Test
    fun purchasedItem_setsBaconPref() {
        val p = prefs()
        val master = CoinmasterData(
            masterName = "Internet Meme Shop",
            nickname = "bacon",
            token = "BACON",
            shopId = "bacon",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )
        CoinmasterPurchasePrefs.applyPurchasedItem(master, 9017, p)
        assertTrue(p.getBoolean("_internetViralVideoBought", false))
    }

    @Test
    fun purchasedItem_setsShoreToasterPref() {
        val p = prefs()
        val master = CoinmasterData(
            masterName = "The Shore, Inc. Gift Shop",
            nickname = "shore",
            token = "scrip",
            shopId = "shore",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )
        CoinmasterPurchasePrefs.applyPurchasedItem(master, 637, p)
        assertTrue(p.getBoolean("itemBoughtPerAscension637", false))
    }

    @Test
    fun purchasedItem_setsDvFlaskPref() {
        val p = prefs()
        val master = CoinmasterData(
            masterName = "The Terrified Eagle Inn",
            nickname = "dv",
            token = "1000000",
            shopId = "dv",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )
        CoinmasterPurchasePrefs.applyPurchasedItem(master, 6423, p)
        assertTrue(p.getBoolean("itemBoughtPerCharacter6423", false))
    }

    @Test
    fun purchasedItem_setsJarlCosmicSixPackPref() {
        val p = prefs()
        val master = CoinmasterData(
            masterName = "Jarlsberg's Cosmic Kitchen",
            nickname = "jarl",
            token = null,
            shopId = "jarl",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )
        CoinmasterPurchasePrefs.applyPurchasedItem(master, 6237, p)
        assertTrue(p.getBoolean("_cosmicSixPackConjured", false))
    }
}
