package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.familiar.FamiliarRequest
import net.sourceforge.kolmafia.session.PvpManager

class GameRuntimeLibraryAshP486Test {

    @BeforeTest
    fun reset() {
        PvpManager.resetForTest()
    }

    @AfterTest
    fun cleanup() {
        PvpManager.resetForTest()
    }

    private val stanceHtml = """
        You have 4 fights remaining today.
        <select name="stance"><option value="0" >Bear Hugs All Around</option><option value="1" selected>Beary Famous</option></select>
    """.trimIndent()

    private fun winHtml(remaining: Int) = """
        You have $remaining fights remaining today.
        <div class="fight"><a href="showplayer.php?who=1"><b>Hero</b></a> calls out <a href="showplayer.php?who=2"><b>Villain</b></a> for battle!
        <span class="win"><b>Hero</b> won the fight, <b>8</b> to <b>3</b>!
    """.trimIndent()

    private fun character(
        fights: Int = 4,
        stoneBroken: Boolean = true,
        roninLeft: String = "0",
        hardcore: String = "0",
    ): KoLCharacter = KoLCharacter().also {
        it.updateFromApiResponse(
            CharacterApiResponse(
                pvpfights = fights.toString(),
                hippystone = if (stoneBroken) "1" else "0",
                roninleft = roninLeft,
                hardcore = hardcore,
            ),
        )
    }

    private fun trackingFamiliar(): Pair<FamiliarRequest, () -> Int> {
        var stealCalls = 0
        val req = object : FamiliarRequest(HttpClient(MockEngine { respond("ok") })) {
            override suspend fun stealItem(itemId: Int): Result<String> {
                stealCalls++
                return Result.success("ok")
            }
        }
        return req to { stealCalls }
    }

    private fun firecrackerDb(): GameDatabase = object : GameDatabase() {
        override fun item(name: String) = ItemData(
            id = 88,
            name = "knob goblin firecracker",
            descId = "",
            image = "",
            primaryUse = ItemPrimaryUse.NONE,
            secondaryUses = emptySet(),
            access = setOf('t'),
            autosellPrice = 0,
            plural = null,
        )
    }

    @Test
    fun revision_phase486() {
        assertEquals("phase671", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun cliSteal_bareListsStances() {
        val char = character()
        val client = HttpClient(MockEngine { respond(stanceHtml, HttpStatusCode.OK) })
        val lib = GameRuntimeLibrary(character = char, httpClient = client)
        val out = outputLib(lib, """cli_execute("steal");""")
        assertTrue(out.contains("0: Bear Hugs All Around"))
        assertTrue(out.contains("1: Beary Famous"))
    }

    @Test
    fun cliSteal_flowersUsesPvp() {
        val char = character(fights = 2, stoneBroken = true)
        PvpManager.parseStances(stanceHtml)
        var fightPosts = 0
        val client = HttpClient(MockEngine {
            fightPosts++
            respond(winHtml(2 - fightPosts), HttpStatusCode.OK)
        })
        val lib = GameRuntimeLibrary(character = char, httpClient = client, preferences = prefs())
        val out = outputLib(lib, """cli_execute("steal flowers Beary Famous");""")
        assertTrue(out.contains("Use all remaining PVP attacks to steal flowers via Beary Famous"))
        assertEquals(2, fightPosts)
        assertTrue(out.contains("You have 0 attacks remaining."))
    }

    @Test
    fun cliSteal_countedFlowersIsPvpNotFamiliar() {
        val char = character(fights = 5, stoneBroken = true)
        PvpManager.parseStances(stanceHtml)
        var fightPosts = 0
        val client = HttpClient(MockEngine {
            fightPosts++
            respond(winHtml(4), HttpStatusCode.OK)
        })
        val (fam, stealCalls) = trackingFamiliar()
        val lib = GameRuntimeLibrary(
            character = char,
            httpClient = client,
            preferences = prefs(),
            familiarRequest = fam,
            gameDatabase = firecrackerDb(),
        )
        val out = outputLib(lib, """cli_execute("steal 5 flowers Beary Famous");""")
        assertTrue(out.contains("Use 5 PVP attacks to steal flowers via Beary Famous"))
        assertTrue(fightPosts > 0)
        assertEquals(0, stealCalls())
    }

    @Test
    fun cliSteal_itemQtyStillFamiliarSteals() {
        val (fam, stealCalls) = trackingFamiliar()
        val lib = GameRuntimeLibrary(familiarRequest = fam, gameDatabase = firecrackerDb())
        runLib(lib, """cli_execute("steal 2 knob goblin firecracker");""")
        assertEquals(2, stealCalls())
    }

    @Test
    fun cliSteal_lootGatedInRonin() {
        val char = character(roninLeft = "40")
        val (fam, stealCalls) = trackingFamiliar()
        val client = HttpClient(MockEngine { respond(stanceHtml, HttpStatusCode.OK) })
        val lib = GameRuntimeLibrary(
            character = char,
            httpClient = client,
            familiarRequest = fam,
            gameDatabase = firecrackerDb(),
        )
        val out = outputLib(lib, """cli_execute("steal loot Beary");""")
        assertTrue(out.contains("You cannot attack for loot now."))
        assertEquals(0, stealCalls())
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun shouldFallbackToFamiliarSteal_itemQty() {
        assertTrue(shouldFallbackToFamiliarSteal("2 knob goblin firecracker"))
        assertFalse(shouldFallbackToFamiliarSteal(""))
        assertFalse(shouldFallbackToFamiliarSteal("flowers Beary Famous"))
        assertFalse(shouldFallbackToFamiliarSteal("5 flowers Beary Famous"))
        assertFalse(shouldFallbackToFamiliarSteal("loot Beary Famous"))
        assertFalse(shouldFallbackToFamiliarSteal("foo"))
    }
}
