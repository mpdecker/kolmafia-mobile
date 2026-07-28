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
import net.sourceforge.kolmafia.shop.ItemStack
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.StoragePullRules
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.CoinmasterPurchaseProbe
import net.sourceforge.kolmafia.shop.CoinmasterVisitInventory
import net.sourceforge.kolmafia.shop.MerchTableSync
import net.sourceforge.kolmafia.shop.SeptEmberSync
import net.sourceforge.kolmafia.shop.ShopRow
import net.sourceforge.kolmafia.shop.TimeTowerSync

class GameRuntimeLibraryAshP164Test {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        CoinmasterDatabase.resetForTest()
    }

    @Test
    fun swaggerNonSeasonItemBlockedAfterVisitWhenAbsent() {
        registerItem(9999, "non-season swagger item")
        CoinmasterDatabase.loadFromText(
            shopsText = "swagger\tThe Swagger Shop\n",
            coinText = "The Swagger Shop\tbuy\t50\tnon-season swagger item\tROW1\n",
        )
        val p = Preferences(MapSettings())
        p.setBoolean("autoSatisfyWithCoinmasters", true)
        CoinmasterVisitInventory.replaceBuyRows(CoinmasterVisitInventory.SWAGGER, emptyList())
        val state = CharacterState(meat = 100_000)
        assertFalse(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                9999,
                state,
                p,
                accessibleCount = { if (it == -1) 0 else 1000 },
            ),
        )
    }

    @Test
    fun swaggerNonSeasonItemAllowedWhenPresentInVisitInventory() {
        registerItem(9999, "non-season swagger item")
        CoinmasterDatabase.loadFromText(
            shopsText = "swagger\tThe Swagger Shop\n",
            coinText = "The Swagger Shop\tbuy\t50\tnon-season swagger item\tROW1\n",
        )
        val p = Preferences(MapSettings())
        p.setBoolean("autoSatisfyWithCoinmasters", true)
        CoinmasterVisitInventory.replaceBuyRows(
            CoinmasterVisitInventory.SWAGGER,
            listOf(ShopRow(rowId = 1, item = ItemStack(9999, 1), price = 50)),
        )
        val state = CharacterState(meat = 100_000)
        assertTrue(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                9999,
                state,
                p,
                accessibleCount = { 1000 },
            ),
        )
    }

    @Test
    fun conmerchValidateUsesVisitInventoryAfterSync() {
        registerItem(MerchTableSync.TWITCHING_TELEVISION_TATTOO, "Twitching Television Tattoo")
        registerItem(7567, "Chroner")
        CoinmasterDatabase.loadFromText(
            shopsText = "conmerch\tKoL Con 13 Merch Table\n",
            coinText = "KoL Con 13 Merch Table\tbuy\t1111\tTwitching Television Tattoo\tROW895\n",
        )
        val p = Preferences(MapSettings())
        p.setBoolean("autoSatisfyWithCoinmasters", true)
        p.setBoolean(TimeTowerSync.PREF, true)
        CoinmasterVisitInventory.replaceBuyRows(
            CoinmasterVisitInventory.CONMERCH,
            listOf(
                ShopRow(
                    rowId = 895,
                    item = ItemStack(MerchTableSync.TWITCHING_TELEVISION_TATTOO, 1),
                    costs = listOf(ItemStack(7567, 1111)),
                ),
            ),
        )
        val state = CharacterState(meat = 100_000)
        assertTrue(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                MerchTableSync.TWITCHING_TELEVISION_TATTOO,
                state,
                p,
                accessibleCount = { if (it == 7567) 2000 else 0 },
            ),
        )
    }

    @Test
    fun conmerchValidateBlockedWhenItemNotInVisitInventory() {
        registerItem(MerchTableSync.TWITCHING_TELEVISION_TATTOO, "Twitching Television Tattoo")
        CoinmasterDatabase.loadFromText(
            shopsText = "conmerch\tKoL Con 13 Merch Table\n",
            coinText = "KoL Con 13 Merch Table\tbuy\t1111\tTwitching Television Tattoo\tROW895\n",
        )
        val p = Preferences(MapSettings())
        p.setBoolean("autoSatisfyWithCoinmasters", true)
        p.setBoolean(TimeTowerSync.PREF, true)
        CoinmasterVisitInventory.replaceBuyRows(CoinmasterVisitInventory.CONMERCH, emptyList())
        val state = CharacterState(meat = 100_000)
        assertFalse(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                MerchTableSync.TWITCHING_TELEVISION_TATTOO,
                state,
                p,
                accessibleCount = { if (it == 7567) 2000 else 0 },
            ),
        )
    }

    @Test
    fun septEmberCheckBalance_skipsWhenAlreadyChecked() {
        val p = Preferences(MapSettings())
        p.setBoolean(SeptEmberSync.BALANCE_CHECKED_PREF, true)
        var visits = 0
        SeptEmberSync.checkBalance(
            prefs = p,
            accessibleCount = { 1 },
            isKingdomOfExploathing = false,
            onVisit = { visits++ },
        )
        assertEquals(0, visits)
    }

    @Test
    fun septEmberCheckBalance_visitsWhenCenserAccessible() {
        val p = Preferences(MapSettings())
        var visits = 0
        SeptEmberSync.checkBalance(
            prefs = p,
            accessibleCount = { if (it == SeptEmberSync.SEPTEMBER_CENSER) 1 else 0 },
            isKingdomOfExploathing = false,
            onVisit = { visits++ },
        )
        assertEquals(1, visits)
    }

    @Test
    fun timeTowerOpen_migratesToolbeltToFreepullCache() {
        val toolbeltId = StoragePullRules.TIME_TWITCHING_TOOLBELT
        val p = Preferences(MapSettings())
        p.setString(Preferences.CACHED_STORAGE, "$toolbeltId:1")
        p.setBoolean(TimeTowerSync.PREF, false)
        TimeTowerSync.syncFromChronerShopHtml("Welcome to the merch table.", p)
        assertTrue(p.getBoolean(TimeTowerSync.PREF, false))
        assertEquals("", p.getString(Preferences.CACHED_STORAGE, "unset"))
        assertEquals("$toolbeltId:1", p.getString(Preferences.CACHED_FREEPULLS, "unset"))
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
                access = setOf('t'),
                autosellPrice = 0,
                plural = null,
            ),
        )
    }
}
