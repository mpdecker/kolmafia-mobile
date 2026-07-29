package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.NpcStoreData
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterData
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.CoinmasterPurchaseAccessibility
import net.sourceforge.kolmafia.shop.CoinmasterPurchaseProbe
import net.sourceforge.kolmafia.shop.CoinmasterPurchasePrefs
import net.sourceforge.kolmafia.shop.NpcPurchaseAccessibility
import net.sourceforge.kolmafia.shop.NpcShopSync
import net.sourceforge.kolmafia.shop.SwaggerShopSync

class GameRuntimeLibraryAshP152Test {

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
    fun hiddentavern_allowedAfterStoreSync() {
        val p = Preferences(MapSettings())
        NpcShopSync.syncFromStoreHtml(
            storeKey = "hiddentavern",
            html = "<html>Hidden Tavern</html>",
            prefs = p,
            ascensionNumber = 4,
        )
        assertTrue(
            NpcPurchaseAccessibility.canPurchaseIgnoringMeat(
                itemId = 175,
                store = NpcStoreData("hiddentavern", "The Hidden Tavern", "NPC"),
                state = CharacterState(ascensionNumber = 4),
                prefs = p,
            ),
        )
    }

    @Test
    fun jarl_cosmicSixPackBlockedAfterPurchaseHook() {
        val p = Preferences(MapSettings())
        val master = CoinmasterData(
            masterName = "Jarlsberg's Cosmic Kitchen",
            nickname = "jarl",
            token = null,
            shopId = "jarl",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )
        CoinmasterPurchasePrefs.applyPurchasedItem(master, 6237, p)
        assertFalse(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                master,
                6237,
                CharacterState(challengePath = AscensionPath.AVATAR_OF_JARLSBERG.apiName),
                p,
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun swagger_blackBartsBootyAllowedAfterVisitSync() {
        registerItem(7732, "Black Bart's Booty")
        CoinmasterDatabase.loadFromText(
            shopsText = "swagger\tThe Swagger Shop\n",
            coinText = "The Swagger Shop\tbuy\t1000\tBlack Bart's Booty\tROW7732\n",
        )
        val p = Preferences(MapSettings())
        p.setBoolean("autoSatisfyWithCoinmasters", true)
        SwaggerShopSync.applyVisitShop(
            html = """
                You've earned 1200 swagger during a pirate season.
                <tr><td><b>Black Bart's Booty</b></td>
                <td><form><input type="hidden" name="whichitem" value="7732" />
                <input type="submit" value="Buy (1000 swagger)" /></form></td></tr>
            """.trimIndent(),
            url = "https://www.kingdomofloathing.com/peevpee.php?place=shop",
            prefs = p,
            sessionLogger = null,
            state = null,
        )
        assertTrue(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                7732,
                CharacterState(meat = 100_000),
                p,
                accessibleCount = { 0 },
            ),
        )
        CoinmasterDatabase.resetForTest()
    }
}
