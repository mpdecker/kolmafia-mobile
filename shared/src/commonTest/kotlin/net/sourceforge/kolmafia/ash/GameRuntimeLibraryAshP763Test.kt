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
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.session.PvpManager

class GameRuntimeLibraryAshP763Test {

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

    private fun character(fights: Int = 4, stoneBroken: Boolean = true): KoLCharacter =
        KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(
                    pvpfights = fights.toString(),
                    hippystone = if (stoneBroken) "1" else "0",
                ),
            )
        }

    private data class Capture(
        val bodies: MutableList<String> = mutableListOf(),
        var fightPosts: Int = 0,
    )

    private fun client(fightHtml: String = winHtml(3), capture: Capture = Capture()): HttpClient =
        HttpClient(MockEngine { request ->
            val url = request.url.toString()
            val body = if (request.method == HttpMethod.Post) {
                request.body.toByteArray().decodeToString()
            } else {
                ""
            }
            when {
                request.method == HttpMethod.Post && url.contains("peevpee") -> {
                    capture.fightPosts++
                    capture.bodies += body
                    respond(fightHtml, HttpStatusCode.OK)
                }
                else -> respond(stanceHtml, HttpStatusCode.OK)
            }
        })

    @Test
    fun revision_phase763() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun ranked_fam_withoutClient_returnsFalse() {
        val lib = GameRuntimeLibrary(character = character())
        assertEquals("false", outputLib(lib, "print(to_string(ranked_fam()));").trim())
    }

    @Test
    fun ranked_fam_oneTougherFight() {
        val char = character(fights = 4, stoneBroken = true)
        PvpManager.parseStances(stanceHtml)
        val capture = Capture()
        val lib = GameRuntimeLibrary(
            character = char,
            httpClient = client(capture = capture),
            preferences = prefs(),
        )
        assertEquals("true", outputLib(lib, "print(to_string(ranked_fam()));").trim())
        assertEquals(1, capture.fightPosts)
        assertTrue(capture.bodies.any { it.contains("ranked=2") })
        assertTrue(capture.bodies.any { it.contains("attacktype=flowers") })
    }

    @Test
    fun ranked_fam_abort_returnsFalse() {
        val char = character(fights = 4, stoneBroken = true)
        PvpManager.parseStances(stanceHtml)
        val capture = Capture()
        val http = HttpClient(MockEngine { request ->
            val url = request.url.toString()
            when {
                request.method == HttpMethod.Post && url.contains("peevpee") -> {
                    capture.fightPosts++
                    respond("error", HttpStatusCode.InternalServerError)
                }
                else -> respond(stanceHtml, HttpStatusCode.OK)
            }
        })
        val lib = GameRuntimeLibrary(character = char, httpClient = http, preferences = prefs())
        assertEquals("false", outputLib(lib, "print(to_string(ranked_fam()));").trim())
        assertEquals(1, capture.fightPosts)
    }
}
