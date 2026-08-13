package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.request.UntinkerRequest

class GameRuntimeLibraryAshP230Test {

    @Test
    fun revision_isphase222() {
        assertEquals("phase475", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun untinker_cliRunsQuestKnoll() = runBlocking {
        var questPosted = false
        var innaboxFetched = false
        val client = HttpClient(
            MockEngine { request ->
                val body = if (request.method == HttpMethod.Post) {
                    request.body.toByteArray().decodeToString()
                } else {
                    ""
                }
                val action = request.url.parameters["action"]
                when {
                    request.url.encodedPath.contains("place.php") &&
                        body.contains("fv_untinker_quest") &&
                        body.contains("screwquest") -> {
                        questPosted = true
                    }
                    request.url.encodedPath.contains("place.php") && action == "dk_innabox" -> {
                        innaboxFetched = true
                    }
                }
                respond(
                    """<select name=whichitem></select>""",
                    HttpStatusCode.OK,
                )
            },
        )
        val character = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(sign = "Mongoose"))
        }
        val untinker = UntinkerRequest(client, character = character)
        val lib = GameRuntimeLibrary(
            character = character,
            untinkerRequest = untinker,
        )

        runLib(lib, """cli_execute("untinker");""")

        assertTrue(questPosted)
        assertTrue(innaboxFetched)
    }
}
