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
import net.sourceforge.kolmafia.chat.PlayerIdRegistry
import net.sourceforge.kolmafia.session.PvpManager

class GameRuntimeLibraryAshP762Test {

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

    private fun lossHtml() = """
        You have 3 fights remaining today.
        You lost some dignity in the attempt.
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
    fun revision_phase762() {
        assertEquals("phase1070", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun pvp_attack_withoutClient_returnsFalse() {
        val lib = GameRuntimeLibrary(character = character())
        assertEquals("false", outputLib(lib, """print(to_string(pvp_attack("Target")));""").trim())
    }

    @Test
    fun pvp_attack_success_directedFight() {
        val char = character(fights = 4, stoneBroken = true)
        PvpManager.parseStances(stanceHtml)
        PlayerIdRegistry.register("Target", "42")
        val capture = Capture()
        val (http, _) = client(capture = capture)
        val lib = GameRuntimeLibrary(character = char, httpClient = http, preferences = prefs())
        assertEquals("true", outputLib(lib, """print(to_string(pvp_attack("Target")));""").trim())
        assertEquals(1, capture.fightPosts)
        assertTrue(capture.bodies.any { it.contains("attacktype=lootwhatever") })
        assertTrue(capture.bodies.any { it.contains("ranked=0") })
        assertTrue(capture.bodies.any { it.contains("stance=0") })
    }

    @Test
    fun pvp_attack_flowersWhenCannotInteract() {
        val char = character(fights = 4, stoneBroken = true, roninLeft = "40")
        PvpManager.parseStances(stanceHtml)
        PlayerIdRegistry.register("Target", "42")
        val capture = Capture()
        val (http, _) = client(capture = capture)
        val lib = GameRuntimeLibrary(character = char, httpClient = http, preferences = prefs())
        assertEquals("true", outputLib(lib, """print(to_string(pvp_attack("Target")));""").trim())
        assertTrue(capture.bodies.any { it.contains("attacktype=flowers") })
    }

    @Test
    fun pvp_attack_abortOnDignityLoss_returnsFalse() {
        val char = character(fights = 4, stoneBroken = true)
        PvpManager.parseStances(stanceHtml)
        PlayerIdRegistry.register("Target", "42")
        val capture = Capture()
        val (http, _) = client(fightHtml = lossHtml(), capture = capture)
        val lib = GameRuntimeLibrary(character = char, httpClient = http, preferences = prefs())
        assertEquals("false", outputLib(lib, """print(to_string(pvp_attack("Target")));""").trim())
        assertEquals(1, capture.fightPosts)
        assertFalse(PvpManager.abortReason.isNullOrEmpty())
    }

    @Test
    fun pvp_attack_unresolvableTarget_returnsFalse() {
        val char = character()
        PvpManager.parseStances(stanceHtml)
        val (http, capture) = client(
            whoisBody = "No player named NobodyHere was found in the Kingdom.",
        )
        val lib = GameRuntimeLibrary(character = char, httpClient = http, preferences = prefs())
        assertEquals("false", outputLib(lib, """print(to_string(pvp_attack("NobodyHere")));""").trim())
        assertEquals(0, capture.fightPosts)
    }
}
