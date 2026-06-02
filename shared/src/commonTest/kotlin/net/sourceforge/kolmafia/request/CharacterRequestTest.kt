package net.sourceforge.kolmafia.request

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CharacterRequestTest {

    private val sampleResponse = """
        {
            "name": "TestPlayer",
            "playerid": "12345",
            "level": "10",
            "class": "3",
            "hp": "150",
            "hpmax": "200",
            "mp": "80",
            "mpmax": "100",
            "meat": "5000",
            "adventures": "40",
            "fullness": "5",
            "drunk": "2",
            "spleen": "1"
        }
    """.trimIndent()

    private fun makeClient(handler: MockRequestHandler): HttpClient = HttpClient(MockEngine(handler)) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    @Test
    fun fetchCharacterState_returnsCharacterData_onSuccess() = runTest {
        val client = makeClient {
            respond(
                content = sampleResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val result = CharacterRequest(client).fetchCharacterState()
        assertTrue(result.isSuccess)
        assertEquals("TestPlayer", result.getOrNull()?.name)
        assertEquals("12345", result.getOrNull()?.playerid)
        assertEquals("150", result.getOrNull()?.hp)
    }

    @Test
    fun fetchCharacterState_returnsFailure_onServerError() = runTest {
        val client = makeClient { respond("", HttpStatusCode.InternalServerError) }
        val result = CharacterRequest(client).fetchCharacterState()
        assertTrue(result.isFailure)
    }
}
