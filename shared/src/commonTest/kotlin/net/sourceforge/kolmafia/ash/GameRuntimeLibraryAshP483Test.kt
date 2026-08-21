package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.chat.ChatProbe
import net.sourceforge.kolmafia.chat.PlayerIdRegistry
import net.sourceforge.kolmafia.session.PvpManager

class GameRuntimeLibraryAshP483Test {

    @BeforeTest
    fun reset() {
        PvpManager.resetForTest()
        PlayerIdRegistry.clearForTest()
    }

    @AfterTest
    fun cleanup() {
        PvpManager.resetForTest()
        PlayerIdRegistry.clearForTest()
    }

    private val stanceHtml = """
        You have 4 fights remaining today.
        <select name="stance"><option value="0" >Bear Hugs All Around</option><option value="1" selected>Beary Famous</option></select>
    """.trimIndent()

    private fun winHtml(remaining: Int) = """
        You have $remaining fights remaining today.
        <div class="fight"><a href="showplayer.php?who=1"><b>Hero</b></a> calls out <a href="showplayer.php?who=2"><b>Target</b></a> for battle!
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

    private data class Capture(
        val urls: MutableList<String> = mutableListOf(),
        val bodies: MutableList<String> = mutableListOf(),
        var fightPosts: Int = 0,
    )

    private fun client(
        whoisBody: String = """<a href="showplayer.php?who=42">Target</a> (#42) is currently online.""",
        profileHtml: String = "<td>Level 10 Seal Clubber</td>",
        fightHtml: String = winHtml(3),
        capture: Capture = Capture(),
    ): Pair<HttpClient, Capture> {
        val http = HttpClient(MockEngine { request ->
            val url = request.url.toString()
            capture.urls += url
            val body = if (request.method == HttpMethod.Post) {
                request.body.toByteArray().decodeToString()
            } else {
                ""
            }
            capture.bodies += body
            when {
                url.contains("submitnewchat") -> respond(whoisBody, HttpStatusCode.OK)
                url.contains("showplayer") -> respond(profileHtml, HttpStatusCode.OK)
                request.method == HttpMethod.Post && url.contains("peevpee") -> {
                    capture.fightPosts++
                    respond(fightHtml, HttpStatusCode.OK)
                }
                else -> respond(stanceHtml, HttpStatusCode.OK)
            }
        })
        return http to capture
    }

    @Test
    fun revision_phase483() {
        assertEquals("phase826", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun cliAttack_bareListsStances() {
        val char = character()
        val (http, capture) = client()
        val lib = GameRuntimeLibrary(character = char, httpClient = http)
        val out = outputLib(lib, """cli_execute("attack");""")
        assertTrue(out.contains("0: Bear Hugs All Around"))
        assertTrue(out.contains("1: Beary Famous"))
        assertEquals(0, capture.fightPosts)
    }

    @Test
    fun cliAttack_namedTargetRankedZero() {
        val char = character(fights = 4, stoneBroken = true)
        PvpManager.parseStances(stanceHtml)
        val capture = Capture()
        val (http, _) = client(capture = capture)
        val lib = GameRuntimeLibrary(
            character = char,
            httpClient = http,
            chatProbe = ChatProbe(http),
            preferences = prefs(),
        )
        val out = outputLib(lib, """cli_execute("attack Target stance=Beary");""")
        assertTrue(out.contains("Retrieving player data for Target"))
        assertTrue(out.contains("Attacking Target"))
        assertEquals(1, capture.fightPosts)
        val fightBody = capture.bodies.first { it.contains("action=fight") }
        assertTrue(fightBody.contains("ranked=0"))
        assertTrue(fightBody.contains("who=Target"))
        assertTrue(fightBody.contains("attacktype=lootwhatever"))
    }

    @Test
    fun cliPvpAttack_aliasUsesSameParser() {
        val char = character(fights = 4, stoneBroken = true)
        PvpManager.parseStances(stanceHtml)
        val capture = Capture()
        val (http, _) = client(capture = capture)
        val lib = GameRuntimeLibrary(
            character = char,
            httpClient = http,
            chatProbe = ChatProbe(http),
            preferences = prefs(),
        )
        outputLib(lib, """cli_execute("pvp attack Target stance=Beary");""")
        assertEquals(1, capture.fightPosts)
        val fightBody = capture.bodies.first { it.contains("action=fight") }
        assertTrue(fightBody.contains("ranked=0"))
        assertTrue(fightBody.contains("who=Target"))
    }

    @Test
    fun cliAttack_roninAttackerUsesFlowers() {
        val char = character(fights = 4, stoneBroken = true, roninLeft = "40")
        PvpManager.parseStances(stanceHtml)
        val capture = Capture()
        val (http, _) = client(capture = capture)
        val lib = GameRuntimeLibrary(
            character = char,
            httpClient = http,
            chatProbe = ChatProbe(http),
            preferences = prefs(),
        )
        outputLib(lib, """cli_execute("attack Target stance=Beary");""")
        val fightBody = capture.bodies.first { it.contains("action=fight") }
        assertTrue(fightBody.contains("attacktype=flowers"))
        assertFalse(fightBody.contains("attacktype=lootwhatever"))
    }

    @Test
    fun cliAttack_beforePvpScriptRunsBeforeFight() {
        val char = character(fights = 4, stoneBroken = true)
        PvpManager.parseStances(stanceHtml)
        val capture = Capture()
        val (http, _) = client(capture = capture)
        val p = prefs()
        p.setString("beforePVPScript", "joke")
        val lib = GameRuntimeLibrary(
            character = char,
            httpClient = http,
            chatProbe = ChatProbe(http),
            preferences = p,
        )
        val out = outputLib(lib, """cli_execute("attack Target stance=Beary");""")
        assertTrue(out.contains("That's funny."))
        val jokeIdx = out.indexOf("That's funny.")
        val attackIdx = out.indexOf("Attacking Target")
        assertTrue(jokeIdx >= 0 && attackIdx > jokeIdx)
        assertEquals(1, capture.fightPosts)
    }

    @Test
    fun cliAttack_unresolvedWhoisDropsTarget() {
        val char = character(fights = 4, stoneBroken = true)
        PvpManager.parseStances(stanceHtml)
        val capture = Capture()
        val (http, _) = client(whoisBody = "No such player.", capture = capture)
        val lib = GameRuntimeLibrary(
            character = char,
            httpClient = http,
            chatProbe = ChatProbe(http),
            preferences = prefs(),
        )
        outputLib(lib, """cli_execute("attack Nobody stance=Beary");""")
        assertEquals(0, capture.fightPosts)
        assertFalse(capture.urls.any { it.contains("showplayer") })
    }

    @Test
    fun pvpAttackParser_requiresStance() {
        PvpManager.parseStances(stanceHtml)
        val parsed = PvpAttackParser.parse("Target")
        assertTrue(parsed is PvpAttackParseResult.Error)
        assertEquals(
            "You must specify stance=STANCE",
            (parsed as PvpAttackParseResult.Error).message,
        )
    }

    @Test
    fun pvpAttackParser_commaTargets() {
        PvpManager.parseStances(stanceHtml)
        val parsed = PvpAttackParser.parse("Alice, Bob stance=Beary")
        assertTrue(parsed is PvpAttackParseResult.Run)
        val run = parsed as PvpAttackParseResult.Run
        assertEquals(listOf("Alice", "Bob"), run.targets)
        assertEquals(1, run.stance)
    }
}
