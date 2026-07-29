package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.NpcStoreData
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.shop.CoinmasterData
import net.sourceforge.kolmafia.shop.CoinmasterPurchaseAccessibility
import net.sourceforge.kolmafia.shop.CoinmasterPurchasePrefs
import net.sourceforge.kolmafia.shop.NpcPurchaseAccessibility
import net.sourceforge.kolmafia.shop.NpcShopSync

class GameRuntimeLibraryAshP150Test {

    @Test
    fun dv_talesBlockedAfterPurchaseHook() {
        val p = com.russhwolf.settings.MapSettings().let { net.sourceforge.kolmafia.preferences.Preferences(it) }
        val master = CoinmasterData(
            masterName = "The Terrified Eagle Inn",
            nickname = "dv",
            token = "1000000",
            shopId = "dv",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )
        CoinmasterPurchasePrefs.applyPurchasedItem(master, 6423, p)
        assertFalse(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                master,
                6423,
                CharacterState(),
                p,
                accessibleCount = { 100 },
            ),
        )
    }

    @Test
    fun fwshop_hatAllowedWhenSyncShowsSection() {
        val p = com.russhwolf.settings.MapSettings().let { net.sourceforge.kolmafia.preferences.Preferences(it) }
        NpcShopSync.syncFromStoreHtml(
            storeKey = "fwshop",
            html = """<b>Combat Explosives</b><b>Dangerous Hats</b>""",
            prefs = p,
            ascensionNumber = 1,
        )
        assertTrue(
            NpcPurchaseAccessibility.canPurchaseIgnoringMeat(
                itemId = 10762,
                store = NpcStoreData("fwshop", "Clan Underground Fireworks Shop", "NPC"),
                state = CharacterState(),
                prefs = p,
            ),
        )
    }
}
