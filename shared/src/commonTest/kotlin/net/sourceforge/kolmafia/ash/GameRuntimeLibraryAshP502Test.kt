package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.MonsterDefinition
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.LocketRequest

class GameRuntimeLibraryAshP502Test {

    @AfterTest
    fun tearDown() {
        ItemDatabase.resetForTest()
    }

    private fun locketItem() = ItemData(
        id = LocketRequest.LOCKET_ITEM_ID,
        name = "combat lover's locket",
        descId = "d10893",
        image = "locket.gif",
        primaryUse = ItemPrimaryUse.ACCESSORY,
        secondaryUses = emptySet(),
        access = setOf('t'),
        autosellPrice = 0,
        plural = null,
    )

    private fun monsterDb() = object : GameDatabase() {
        override fun monster(name: String) =
            if (name.equals("test monster", ignoreCase = true)) testMonster() else null
        override fun monster(id: Int) = if (id == 42) testMonster() else null
    }

    private fun testMonster() = MonsterDefinition(
        name = "test monster",
        id = 42,
        image = "test.gif",
        attack = 1,
        defense = 1,
        hp = 1,
        initiative = 0,
        meatDrop = 0,
        phylum = "beast",
        isBoss = false,
        isGhost = false,
        isLucky = false,
        isScaling = false,
        scale = 0,
        cap = 0,
        floor = 0,
        drops = emptyList(),
    )

    private fun invWithLocket(): InventoryManager =
        object : InventoryManager(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
            GameEventBus(),
        ) {
            override val state = MutableStateFlow(
                InventoryState(
                    items = mapOf(
                        LocketRequest.LOCKET_ITEM_ID to InventoryItem(
                            LocketRequest.LOCKET_ITEM_ID,
                            "combat lover's locket",
                            1,
                            ItemType.ACCESSORY,
                        ),
                    ),
                ),
            ).asStateFlow()
        }

    @Test
    fun revision_phase502() {
        assertEquals("phase743", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun reminisce_bare_errorsNoMonster() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("reminisce");""")
        assertTrue(out.contains("No monster specified."))
    }

    @Test
    fun reminisce_missingLocket_errors() {
        ItemDatabase.registerForTest(locketItem())
        val lib = GameRuntimeLibrary(
            gameDatabase = monsterDb(),
            preferences = Preferences(MapSettings()),
        )
        val out = outputLib(lib, """cli_execute("reminisce test monster");""")
        assertTrue(out.contains("You do not own a combat lover's locket."))
    }

    @Test
    fun reminisce_successfulMidPost() {
        ItemDatabase.registerForTest(locketItem())
        val captured = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            captured += request.url.toString() + " " + request.method.value
            if (request.method == HttpMethod.Post) {
                val body = request.body.toByteArray().decodeToString()
                captured += body
            }
            respond("ok", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html"))
        })
        val lib = GameRuntimeLibrary(
            gameDatabase = monsterDb(),
            inventoryManager = invWithLocket(),
            preferences = Preferences(MapSettings()),
            httpClient = client,
        )
        outputLib(lib, """cli_execute("reminisce test monster");""")
        assertTrue(captured.any { it.contains("reminisce=1") })
        assertTrue(captured.any { it.contains("mid=42") })
        assertTrue(captured.any { it.contains("whichchoice=1463") })
    }

    @Test
    fun reminisce_thirdFightOfDay_blocked() {
        ItemDatabase.registerForTest(locketItem())
        var posted = false
        val client = HttpClient(MockEngine { request ->
            if (request.method == HttpMethod.Post) posted = true
            respond("ok", HttpStatusCode.OK)
        })
        val prefs = Preferences(MapSettings())
        prefs.setString("_locketMonstersFought", "1,2,3")
        val lib = GameRuntimeLibrary(
            gameDatabase = monsterDb(),
            inventoryManager = invWithLocket(),
            preferences = prefs,
            httpClient = client,
        )
        val out = outputLib(lib, """cli_execute("reminisce test monster");""")
        assertTrue(out.contains("You can only reminisce thrice daily."))
        assertFalse(posted)
    }
}
