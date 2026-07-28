package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.CoinmasterPurchaseProbe
import net.sourceforge.kolmafia.shop.CoinmasterShopSync
import net.sourceforge.kolmafia.shop.FunALogUnlockPrefs

class GameRuntimeLibraryAshP156Test {

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
    fun piraterealm_crabsicleBlockedBeforeSyncAllowedAfter() {
        registerItem(10199, "crabsicle")
        CoinmasterDatabase.loadFromText(
            shopsText = "piraterealm\tPirateRealm Fun-a-Log\n",
            coinText = "PirateRealm Fun-a-Log\tbuy\t100\tcrabsicle\tROW1053\n",
        )
        val p = Preferences(MapSettings())
        p.setBoolean("autoSatisfyWithCoinmasters", true)
        val state = CharacterState(meat = 100_000)
        val accessibleCount: (Int) -> Int = { id ->
            when (id) {
                FunALogUnlockPrefs.PIRATE_REALM_FUN_LOG -> 1
                else -> 0
            }
        }
        assertFalse(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                10199,
                state,
                p,
                accessibleCount = accessibleCount,
            ),
        )
        CoinmasterShopSync.apply(
            html = """<tr rel="10199"><td>crabsicle</td></tr>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=piraterealm",
            prefs = p,
        )
        assertTrue(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                10199,
                state,
                p,
                accessibleCount = accessibleCount,
            ),
        )
    }

    @Test
    fun piraterealm_blockedWithoutFunLog() {
        registerItem(10199, "crabsicle")
        CoinmasterDatabase.loadFromText(
            shopsText = "piraterealm\tPirateRealm Fun-a-Log\n",
            coinText = "PirateRealm Fun-a-Log\tbuy\t100\tcrabsicle\tROW1053\n",
        )
        val p = Preferences(MapSettings())
        p.setBoolean("autoSatisfyWithCoinmasters", true)
        p.setBoolean("pirateRealmUnlockedCrabsicle", true)
        assertFalse(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                10199,
                CharacterState(meat = 100_000),
                p,
                accessibleCount = { 0 },
            ),
        )
    }
}
