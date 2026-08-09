package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase.Companion.UNSTARTED
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.FunALogUnlockPrefs

class GameRuntimeLibraryAshP212Test {

    private companion object {
        const val AUGUST_SCEPTER = 11325
        const val REPLICA_JOL = 11190
        const val RED_ZEPPELIN = 7185
        const val PRICELESS_DIAMOND = 7221
        const val CRABSICLE = 10199
        const val BLACK_BARTS_BOOTY = 7732
        const val FLAK_SHIELD = 11920
        const val CHRONER = 7567
    }

    @AfterTest
    fun tearDown() {
        CoinmasterDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun revision_phase222() {
        assertEquals("phase350", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun mrreplica_validateDeniedUntilVisitHook() {
        registerItem(REPLICA_JOL, "replica Dark Jill-O-Lantern")
        registerItem(AUGUST_SCEPTER, "august scepter")
        CoinmasterDatabase.loadFromText(
            shopsText = "mrreplica\tReplica Mr. Store\n",
            coinText = """
                Replica Mr. Store	buy	1	replica Dark Jill-O-Lantern	ROW11190
                Replica Mr. Store	buy	1	august scepter	ROW11325
            """.trimIndent(),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(
                    CharacterApiResponse(
                        path = AscensionPath.LEGACY_OF_LOATHING.apiName,
                        meat = "100000",
                    ),
                )
            },
            inventoryManager = inventoryWithAugustScepter(),
        )
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item($REPLICA_JOL, true));""").trim())
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item($AUGUST_SCEPTER, true));""").trim())
        lib.processVisitResponseHooks(
            html = """<td colspan=14 align=center>&mdash; <b>2023</b> &mdash;</td>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=mrreplica",
        )
        assertEquals(2023, prefs.getInt("currentReplicaStoreYear", 0))
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item($REPLICA_JOL, true));""").trim())
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item($AUGUST_SCEPTER, true));""").trim())
    }

    @Test
    fun blackmarket_validateDeniedUntilVisitHook() {
        registerItem(RED_ZEPPELIN, "Red Zeppelin ticket")
        registerItem(PRICELESS_DIAMOND, "priceless diamond")
        CoinmasterDatabase.loadFromText(
            shopsText = "blackmarket\tThe Black Market\n",
            coinText = "The Black Market\tROW290\tRed Zeppelin ticket\tpriceless diamond (1)\n",
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        prefs.setString(Quest.MACGUFFIN.prefKey, UNSTARTED)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(ascensions = "3", meat = "100000"))
            },
            inventoryManager = inventoryWithDiamond(),
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item($RED_ZEPPELIN, true));""").trim())
        lib.processVisitResponseHooks(
            html = "<html>The Black Market</html>",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=blackmarket",
        )
        assertEquals("step1", prefs.getString(Quest.MACGUFFIN.prefKey, ""))
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item($RED_ZEPPELIN, true));""").trim())
    }

    @Test
    fun piraterealm_validateDeniedUntilVisitHook() {
        registerItem(CRABSICLE, "crabsicle")
        registerItem(FunALogUnlockPrefs.PIRATE_REALM_FUN_LOG, "PirateRealm fun-a-log")
        CoinmasterDatabase.loadFromText(
            shopsText = "piraterealm\tPirateRealm Fun-a-Log\n",
            coinText = "PirateRealm Fun-a-Log\tbuy\t100\tcrabsicle\tROW1053\n",
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
            inventoryManager = inventoryWithFunLog(),
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item($CRABSICLE, true));""").trim())
        lib.processVisitResponseHooks(
            html = """<tr rel="$CRABSICLE"><td>crabsicle</td></tr>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=piraterealm",
        )
        assertTrue(prefs.getBoolean("pirateRealmUnlockedCrabsicle", false))
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item($CRABSICLE, true));""").trim())
    }

    @Test
    fun swagger_validateDeniedUntilVisitHook() {
        registerItem(BLACK_BARTS_BOOTY, "Black Bart's Booty")
        CoinmasterDatabase.loadFromText(
            shopsText = "swagger\tThe Swagger Shop\n",
            coinText = "The Swagger Shop\tbuy\t1000\tBlack Bart's Booty\tROW7732\n",
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item($BLACK_BARTS_BOOTY, true));""").trim())
        lib.processVisitResponseHooks(
            html = """
                You've earned 1200 swagger during a pirate season.
                <tr><td><b>Black Bart's Booty</b></td>
                <td><form><input type="hidden" name="whichitem" value="$BLACK_BARTS_BOOTY" />
                <input type="submit" value="Buy (1000 swagger)" /></form></td></tr>
            """.trimIndent(),
            url = "https://www.kingdomofloathing.com/peevpee.php?place=shop",
        )
        assertTrue(prefs.getBoolean("blackBartsBootyAvailable", false))
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item($BLACK_BARTS_BOOTY, true));""").trim())
    }

    @Test
    fun alliedhq_validateDeniedUntilVisitHook() {
        registerItem(FLAK_SHIELD, "flak shield")
        registerItem(CHRONER, "Chroner")
        CoinmasterDatabase.loadFromText(
            shopsText = "twitch_alliedhq\tAllied HQ\n",
            coinText = "Allied HQ\tROW1599\tflak shield\tChroner (20)\n",
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
            inventoryManager = inventoryWithChroner(),
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item($FLAK_SHIELD, true));""").trim())
        lib.processVisitResponseHooks(
            html = """<b>flak shield</b> Chroner (20)""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=twitch_alliedhq",
        )
        assertTrue(prefs.getBoolean("timeTowerAvailable", false))
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item($FLAK_SHIELD, true));""").trim())
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

    private fun inventoryWithDiamond(): InventoryManager =
        mockInventory(PRICELESS_DIAMOND, "priceless diamond", 5)

    private fun inventoryWithFunLog(): InventoryManager =
        mockInventory(FunALogUnlockPrefs.PIRATE_REALM_FUN_LOG, "PirateRealm fun-a-log", 1)

    private fun inventoryWithChroner(): InventoryManager =
        mockInventory(CHRONER, "Chroner", 100)

    private fun inventoryWithAugustScepter(): InventoryManager =
        mockInventory(AUGUST_SCEPTER, "august scepter", 1)

    private fun mockInventory(itemId: Int, name: String, count: Int): InventoryManager =
        object : InventoryManager(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
            net.sourceforge.kolmafia.event.GameEventBus(),
        ) {
            private val flow = MutableStateFlow(
                InventoryState(
                    items = mapOf(
                        itemId to InventoryItem(itemId, name, count, ItemType.OTHER),
                    ),
                ),
            )
            override val state = flow.asStateFlow()
        }
}
