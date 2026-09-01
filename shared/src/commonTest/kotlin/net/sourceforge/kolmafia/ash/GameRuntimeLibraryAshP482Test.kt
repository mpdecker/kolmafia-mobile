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
import net.sourceforge.kolmafia.session.PvpManager

class GameRuntimeLibraryAshP482Test {

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

    @Test
    fun revision_phase482() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun current_pvp_stances_prefetchesFightPageWhenUnknown() {
        val char = character()
        val client = HttpClient(MockEngine { respond(stanceHtml, HttpStatusCode.OK) })
        val lib = GameRuntimeLibrary(character = char, httpClient = client)
        assertEquals("2", outputLib(lib, "print(count(current_pvp_stances()));").trim())
        assertEquals("1", outputLib(lib, """print(current_pvp_stances()["Beary Famous"]);""").trim())
        assertTrue(PvpManager.stancesKnown)
    }

    @Test
    fun cliPvp_bareListsStances() {
        val char = character()
        val client = HttpClient(MockEngine { respond(stanceHtml, HttpStatusCode.OK) })
        val lib = GameRuntimeLibrary(character = char, httpClient = client)
        val out = outputLib(lib, """cli_execute("pvp");""")
        assertTrue(out.contains("0: Bear Hugs All Around"))
        assertTrue(out.contains("1: Beary Famous"))
    }

    @Test
    fun cliPvp_oneFlowerFight() {
        val char = character(fights = 3, stoneBroken = true)
        PvpManager.parseStances(stanceHtml)
        var fightPosts = 0
        val client = HttpClient(MockEngine {
            fightPosts++
            respond(winHtml(2), HttpStatusCode.OK)
        })
        val lib = GameRuntimeLibrary(character = char, httpClient = client, preferences = prefs())
        val out = outputLib(lib, """cli_execute("pvp 1 flowers Beary");""")
        assertTrue(out.contains("Use 1 PVP attacks to steal flowers via Beary Famous"))
        assertEquals(1, fightPosts)
        assertTrue(out.contains("You have 2 attacks remaining."))
    }

    @Test
    fun cliPvp_lootGatedInRonin() {
        val char = character(roninLeft = "40")
        val client = HttpClient(MockEngine { respond(stanceHtml, HttpStatusCode.OK) })
        val lib = GameRuntimeLibrary(character = char, httpClient = client)
        val out = outputLib(lib, """cli_execute("pvp loot Beary");""")
        assertTrue(out.contains("You cannot attack for loot now."))
    }

    @Test
    fun cliFlowers_usesAllRemainingAttacks() {
        val char = character(fights = 2, stoneBroken = true)
        var fightPosts = 0
        val client = HttpClient(MockEngine {
            fightPosts++
            respond(winHtml(2 - fightPosts), HttpStatusCode.OK)
        })
        val lib = GameRuntimeLibrary(character = char, httpClient = client, preferences = prefs())
        val out = outputLib(lib, """cli_execute("flowers");""")
        assertEquals(2, fightPosts)
        assertTrue(out.contains("You have 0 attacks remaining."))
    }

    @Test
    fun cliPvpAttack_withoutClientCannotDetermineStances() {
        val lib = GameRuntimeLibrary.forTesting()
        val out = outputLib(lib, """cli_execute("pvp attack someone");""")
        assertTrue(out.contains("Cannot determine valid stances"))
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun pvpStealParser_tougherLoot() {
        PvpManager.parseStances(stanceHtml)
        val parsed = PvpStealParser.parse("tougher loot Beary", canInteract = true)
        assertTrue(parsed is PvpStealParseResult.Run)
        val run = parsed as PvpStealParseResult.Run
        assertEquals(0, run.attacks)
        assertTrue(run.tougher)
        assertEquals("lootwhatever", run.mission)
        assertEquals(1, run.stance)
    }

    @Test
    fun pvpStealParser_unknownMission() {
        PvpManager.parseStances(stanceHtml)
        val parsed = PvpStealParser.parse("bananas Beary", canInteract = true)
        assertTrue(parsed is PvpStealParseResult.Error)
        assertEquals("What do you want to steal?", (parsed as PvpStealParseResult.Error).message)
    }
}
