package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.event.GameEventBus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryAshP23Test {

    @Test
    fun elementNumericModifier_returnsLiveColdResistance() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter()
        char.updateFromApiResponse(
            net.sourceforge.kolmafia.character.CharacterApiResponse(sign = "Marmot"),
        )
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        assertEquals(
            "1.0",
            outputLib(lib, """print(to_string(numeric_modifier(to_element("cold"), "Cold Resistance")));"""),
        )
    }

    @Test
    fun numericsModifier_effectDuration_returnsActiveDurations() = runBlocking {
        val effectsJson = """{"1":{"name":"Adventurerlike","duration":10},"2":{"name":"Muscular","duration":5}}"""
        val client = HttpClient(MockEngine {
            respond(effectsJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val effectMgr = EffectManager(client, GameEventBus())
        effectMgr.fetchEffects()
        val lib = GameRuntimeLibrary(effectManager = effectMgr)
        val out = outputLib(lib, """print(to_string(numerics_modifier("Effect Duration")));""")
        assertTrue(out.contains("10"))
        assertTrue(out.contains("5"))
    }

    @Test
    fun numericsModifier_unknownModifier_returnsEmptyAggregate() {
        val lib = GameRuntimeLibrary()
        assertEquals("{}", outputLib(lib, """print(to_string(numerics_modifier("Muscle")));"""))
    }
}
