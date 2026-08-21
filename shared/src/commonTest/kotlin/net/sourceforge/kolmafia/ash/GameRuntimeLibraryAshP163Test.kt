package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.CoinmasterVisitInventory
import net.sourceforge.kolmafia.shop.MerchTableSync
import net.sourceforge.kolmafia.shop.SleazeAirportSync
import net.sourceforge.kolmafia.shop.TimeTowerSync

class GameRuntimeLibraryAshP163Test {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        CoinmasterDatabase.resetForTest()
    }

    @Test
    fun revision_phase184() {
        assertEquals("phase826", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun sleazeAirportSync_adventureVisitSetsPref() {
        val p = Preferences(MapSettings())
        SleazeAirportSync.syncFromVisit(
            html = """<center><b>Adventure Results</b></center>""",
            url = "https://www.kingdomofloathing.com/adventure.php?snarfblat=402",
            prefs = p,
        )
        assertTrue(p.getBoolean(SleazeAirportSync.PREF, false))
    }

    @Test
    fun sleazeAirportSync_blockedResponseDoesNotSetPref() {
        val p = Preferences(MapSettings())
        SleazeAirportSync.syncFromVisit(
            html = "You don't know where that is.",
            url = "https://www.kingdomofloathing.com/adventure.php?snarfblat=402",
            prefs = p,
        )
        assertFalse(p.getBoolean(SleazeAirportSync.PREF, false))
    }

    @Test
    fun sleazeAirportSync_placeAirportSleazeSetsPref() {
        val p = Preferences(MapSettings())
        SleazeAirportSync.syncFromVisit(
            html = """<a href="place.php?whichplace=airport_sleaze">Sleaze</a>""",
            url = "https://www.kingdomofloathing.com/place.php?whichplace=airport_sleaze",
            prefs = p,
        )
        assertTrue(p.getBoolean(SleazeAirportSync.PREF, false))
    }

    @Test
    fun shopVisitHook_appliesConmerchSync() {
        registerItem(MerchTableSync.TWITCHING_TELEVISION_TATTOO, "Twitching Television Tattoo")
        registerItem(7567, "Chroner")
        CoinmasterDatabase.loadFromText(
            shopsText = "conmerch\tKoL Con 13 Merch Table\n",
            coinText = """
                KoL Con 13 Merch Table	buy	1	Twitching Television Tattoo	ROW895
            """.trimIndent(),
        )
        val p = Preferences(MapSettings())
        val html = """
            <tr rel="9148"><td></td><td><img onClick='javascript:descitem(216403537)'></td>
            <td><b>Twitching Television Tattoo</b></td>
            <td><img title="Chroner"></td><td><b>1,111</b></td>
            <td><input whichrow=895 value='Buy'></td></tr>
        """.trimIndent()
        val lib = GameRuntimeLibrary(preferences = p, character = KoLCharacter())
        lib.processVisitResponseHooks(
            html = html,
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=conmerch",
        )
        assertTrue(p.getBoolean(TimeTowerSync.PREF, false))
        assertTrue(CoinmasterVisitInventory.hasVisited(CoinmasterVisitInventory.CONMERCH))
        val row = CoinmasterVisitInventory.findBuyRow(
            CoinmasterVisitInventory.CONMERCH,
            MerchTableSync.TWITCHING_TELEVISION_TATTOO,
        )
        assertEquals(895, row?.rowId)
        assertEquals(7567, row?.costs?.single()?.itemId)
        assertEquals(1111, row?.costs?.single()?.count)
        CoinmasterVisitInventory.resetForTest()
    }

    @Test
    fun shopVisitHook_sleazeAdventureViaProcessVisitHooks() {
        val p = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(preferences = p, character = KoLCharacter())
        lib.processVisitResponseHooks(
            html = """<center><b>Adventure Results</b></center>""",
            url = "https://www.kingdomofloathing.com/adventure.php?snarfblat=403",
        )
        assertTrue(p.getBoolean(SleazeAirportSync.PREF, false))
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
