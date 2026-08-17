package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.session.PvpManager

class GameRuntimeLibraryAshP484Test {

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
    ): KoLCharacter = KoLCharacter().also {
        it.updateFromApiResponse(
            CharacterApiResponse(
                pvpfights = fights.toString(),
                hippystone = if (stoneBroken) "1" else "0",
            ),
        )
    }

    @Test
    fun revision_phase484() {
        assertEquals("phase605", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun cliSwagger_usesAllRemainingAttacks() {
        val char = character(fights = 2, stoneBroken = true)
        var fightPosts = 0
        val client = HttpClient(MockEngine {
            fightPosts++
            respond(winHtml(2 - fightPosts), HttpStatusCode.OK)
        })
        val lib = GameRuntimeLibrary(character = char, httpClient = client, preferences = prefs())
        val out = outputLib(lib, """cli_execute("swagger");""")
        assertEquals(2, fightPosts)
        assertTrue(out.contains("You have 0 attacks remaining."))
    }

    @Test
    fun cliFlowers_stillUsesAllRemainingAttacks() {
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
}
