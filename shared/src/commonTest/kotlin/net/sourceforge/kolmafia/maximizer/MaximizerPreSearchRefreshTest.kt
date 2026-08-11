package net.sourceforge.kolmafia.maximizer

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillManager

class MaximizerPreSearchRefreshTest {

    @Test
    fun preSearchRefresh_callsFetchSkillsBeforeStatus() = runBlocking {
        val requestOrder = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            requestOrder += request.url.parameters["what"].orEmpty()
            respond("{}", HttpStatusCode.OK)
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val character = KoLCharacter()
        val inv = InventoryManager(
            client = client,
            eventBus = GameEventBus(),
            characterRequest = CharacterRequest(client),
            character = character,
        )
        val skills = SkillManager(client, SkillCastRequest(client), GameEventBus())
        MaximizerPreSearchRefresh.refresh(
            inventoryManager = inv,
            effectManager = null,
            character = character,
            characterRequest = CharacterRequest(client),
            preferences = null,
            skillManager = skills,
        )
        assertTrue("skills" in requestOrder, "requestOrder=$requestOrder")
        assertTrue(
            requestOrder.indexOf("skills") < requestOrder.indexOf("status"),
            "requestOrder=$requestOrder",
        )
    }
}
