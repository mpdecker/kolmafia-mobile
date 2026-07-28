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
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase.Companion.UNSTARTED
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.CoinmasterPurchaseProbe
import net.sourceforge.kolmafia.shop.CoinmasterShopSync

class GameRuntimeLibraryAshP154Test {

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
    fun mrreplica_allowedAfterVisitYearSync() {
        registerItem(11325, "august scepter")
        CoinmasterDatabase.loadFromText(
            shopsText = "mrreplica\tReplica Mr. Store\n",
            coinText = "Replica Mr. Store\tbuy\t1\taugust scepter\tROW11325\n",
        )
        val p = Preferences(MapSettings())
        p.setBoolean("autoSatisfyWithCoinmasters", true)
        CoinmasterShopSync.apply(
            html = """<td colspan=14 align=center>&mdash; <b>2023</b> &mdash;</td>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=mrreplica",
            prefs = p,
        )
        assertTrue(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                11325,
                CharacterState(
                    challengePath = AscensionPath.LEGACY_OF_LOATHING.apiName,
                    meat = 100,
                ),
                p,
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun mrreplica_wrongYearBlockedAfterVisitSync() {
        registerItem(11190, "replica Dark Jill-O-Lantern")
        CoinmasterDatabase.loadFromText(
            shopsText = "mrreplica\tReplica Mr. Store\n",
            coinText = "Replica Mr. Store\tbuy\t1\treplica Dark Jill-O-Lantern\tROW11190\n",
        )
        val p = Preferences(MapSettings())
        p.setBoolean("autoSatisfyWithCoinmasters", true)
        CoinmasterShopSync.apply(
            html = """<td colspan=14 align=center>&mdash; <b>2023</b> &mdash;</td>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=mrreplica",
            prefs = p,
        )
        assertFalse(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                11190,
                CharacterState(
                    challengePath = AscensionPath.LEGACY_OF_LOATHING.apiName,
                    meat = 100,
                ),
                p,
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun blackmarket_zeppelinAllowedAfterVisitUnlock() {
        registerItem(7185, "Red Zeppelin ticket")
        registerItem(7221, "priceless diamond")
        CoinmasterDatabase.loadFromText(
            shopsText = "blackmarket\tThe Black Market\n",
            coinText = "The Black Market\tROW290\tRed Zeppelin ticket\tpriceless diamond (1)\n",
        )
        val p = Preferences(MapSettings())
        p.setBoolean("autoSatisfyWithCoinmasters", true)
        p.setString(Quest.MACGUFFIN.prefKey, UNSTARTED)
        CoinmasterShopSync.apply(
            html = "<html>The Black Market</html>",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=blackmarket",
            prefs = p,
            state = CharacterState(ascensionNumber = 3, meat = 100_000),
        )
        assertTrue(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                7185,
                CharacterState(ascensionNumber = 3, meat = 100_000),
                p,
                accessibleCount = { id -> if (id == 7221) 5 else 0 },
            ),
        )
    }
}
