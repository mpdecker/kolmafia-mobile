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
import net.sourceforge.kolmafia.shop.DripArmoryPrefs

class GameRuntimeLibraryAshP158Test {

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
    fun driparmory_shieldBlockedBeforeSyncAllowedAfter() {
        registerItem(DripArmoryPrefs.DRIPPY_SHIELD, "drippy shield")
        CoinmasterDatabase.loadFromText(
            shopsText = "driparmory\tDrip Institute Armory\n",
            coinText = "Drip Institute Armory\tbuy\t50\tdrippy shield\tROW1132\n",
        )
        val p = Preferences(MapSettings())
        p.setBoolean("autoSatisfyWithCoinmasters", true)
        val state = CharacterState(meat = 100_000)
        assertFalse(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                DripArmoryPrefs.DRIPPY_SHIELD,
                state,
                p,
                accessibleCount = { 0 },
            ),
        )
        CoinmasterShopSync.apply(
            html = """<b>drippy shield</b>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=driparmory",
            prefs = p,
        )
        assertTrue(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                DripArmoryPrefs.DRIPPY_SHIELD,
                state,
                p,
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun driparmory_shieldBlockedWhenAlreadyOwned() {
        registerItem(DripArmoryPrefs.DRIPPY_SHIELD, "drippy shield")
        CoinmasterDatabase.loadFromText(
            shopsText = "driparmory\tDrip Institute Armory\n",
            coinText = "Drip Institute Armory\tbuy\t50\tdrippy shield\tROW1132\n",
        )
        val p = Preferences(MapSettings())
        p.setBoolean("autoSatisfyWithCoinmasters", true)
        p.setBoolean("drippyShieldUnlocked", true)
        assertFalse(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                DripArmoryPrefs.DRIPPY_SHIELD,
                CharacterState(meat = 100_000),
                p,
                accessibleCount = { id ->
                    if (id == DripArmoryPrefs.DRIPPY_SHIELD) 1 else 0
                },
            ),
        )
    }
}
