package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillManager

class GameRuntimeLibraryAshP126Test {

    private class TestInventoryManager(
        items: Map<Int, InventoryItem>,
    ) : InventoryManager(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }), GameEventBus()) {
        private val flow = MutableStateFlow(InventoryState(items = items))
        override val state = flow.asStateFlow()
    }

    @Test
    fun freeCrafts_ashReflectsInigoEffect() {
        val effectsJson = """{"716":{"name":"Inigo's Incantation of Inspiration","duration":10}}"""
        val engine = MockEngine { request ->
            when (request.url.parameters["what"]) {
                "effects" -> respond(
                    content = effectsJson,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
                else -> respond("ok", HttpStatusCode.OK)
            }
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val effectManager = EffectManager(client, GameEventBus())
        runBlocking { effectManager.fetchEffects() }
        val lib = GameRuntimeLibrary(effectManager = effectManager)
        assertEquals("2", outputLib(lib, """print(free_crafts());""").trim())
    }

    @Test
    fun freeSmiths_ashReflectsAutoAnvilInventory() {
        val prefs = Preferences(MapSettings())
        val inventory = TestInventoryManager(
            mapOf(6965 to InventoryItem(6965, "warbear auto-anvil", 1, ItemType.OTHER)),
        )
        val lib = GameRuntimeLibrary(inventoryManager = inventory, preferences = prefs)
        assertEquals("5", outputLib(lib, """print(free_smiths());""").trim())
    }

    @Test
    fun freeMixes_ashReflectsOldSchoolSkill() {
        val prefs = Preferences(MapSettings())
        val skillsJson =
            """{"230":{"name":"Old-School Cocktailcrafting","type":5,"dailylimit":0,"timescast":0,"mpcost":0}}"""
        val engine = MockEngine { request ->
            when (request.url.parameters["what"]) {
                "skills" -> respond(
                    content = skillsJson,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
                else -> respond("ok", HttpStatusCode.OK)
            }
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val skillManager = SkillManager(client, SkillCastRequest(client), GameEventBus())
        runBlocking { skillManager.fetchSkills() }
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(name = "Tester", path = "Standard"))
        }
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            skillManager = skillManager,
            character = char,
        )
        assertEquals("3", outputLib(lib, """print(free_mixes());""").trim())
    }
}
