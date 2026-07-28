package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase

class EquipmentRequestCodpieceTest {

    @AfterTest
    fun tearDown() {
        ModifierDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

    private fun registerGem(id: Int = 91001, name: String = "ash test gem") {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "desc$id",
                image = "gem.gif",
                primaryUse = ItemPrimaryUse.ACCESSORY,
                secondaryUses = emptySet(),
                access = emptySet(),
                autosellPrice = 0,
                plural = null,
            ),
        )
        ModifierDatabase.injectForTest("EternityCodpiece", name, "Muscle: +1")
    }

    private fun makeClient(
        onRequest: (method: HttpMethod, path: String, body: String) -> Unit = { _, _, _ -> },
    ): HttpClient = HttpClient(
        MockEngine { request ->
            val body = if (request.method == HttpMethod.Post) {
                request.body.toByteArray().decodeToString()
            } else {
                ""
            }
            onRequest(request.method, request.url.encodedPath, body)
            when {
                request.url.parameters["what"] == "status" ->
                    respond(
                        """{"name":"Player","eternitycod":[91001,0,0,0,0]}""",
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                else -> respond("ok", HttpStatusCode.OK)
            }
        },
    ) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    @Test
    fun equipCodpieceGem_opensEditorThenPostsChoice1588() = runTest {
        registerGem()
        val steps = mutableListOf<String>()
        val char = KoLCharacter()
        val client = makeClient { method, path, body ->
            when {
                method == HttpMethod.Get && path == "/inventory.php" &&
                    body.isEmpty() -> steps += "docodpiece"
                method == HttpMethod.Post && path == "/choice.php" -> {
                    steps += "choice"
                    assertTrue(body.contains("whichchoice=1588"))
                    assertTrue(body.contains("option=1"))
                    assertTrue(body.contains("which=3"))
                    assertTrue(body.contains("iid=91001"))
                }
            }
        }
        val req = EquipmentRequest(
            client,
            CharacterRequest(client),
            char,
        )
        assertTrue(req.equipCodpieceGem(91001, EquipmentSlot.CODPIECE3).isSuccess)
        assertEquals(listOf("docodpiece", "choice"), steps)
        assertEquals("ash test gem", char.state.value.equipment[EquipmentSlot.CODPIECE1])
    }

    @Test
    fun unequipCodpieceSlot_postsOption2() = runTest {
        registerGem()
        val char = KoLCharacter()
        char.updateFromApiResponse(
            CharacterApiResponse(name = "Player", eternitycod = listOf(91001, 0, 0, 0, 0)),
        )
        var choiceBody = ""
        val client = makeClient { method, path, body ->
            if (method == HttpMethod.Post && path == "/choice.php") {
                choiceBody = body
            }
        }
        val req = EquipmentRequest(
            client,
            CharacterRequest(client),
            char,
        )
        assertTrue(req.unequipCodpieceSlot(EquipmentSlot.CODPIECE1).isSuccess)
        assertTrue(choiceBody.contains("option=2"))
        assertTrue(choiceBody.contains("which=1"))
    }

    @Test
    fun equipCodpieceGem_rejectsNonGem() = runTest {
        ItemDatabase.registerForTest(
            ItemData(
                id = 999,
                name = "not a gem",
                descId = "desc999",
                image = "x.gif",
                primaryUse = ItemPrimaryUse.ACCESSORY,
                secondaryUses = emptySet(),
                access = emptySet(),
                autosellPrice = 0,
                plural = null,
            ),
        )
        val client = makeClient()
        val req = EquipmentRequest(client)
        assertTrue(req.equipCodpieceGem(999, EquipmentSlot.CODPIECE1).isFailure)
    }

    @Test
    fun equipItem_codpieceSlot_delegatesToCodpieceFlow() = runTest {
        registerGem()
        val paths = mutableListOf<String>()
        val client = makeClient { _, path, _ -> paths += path }
        val req = EquipmentRequest(client, CharacterRequest(client), KoLCharacter())
        assertTrue(req.equipItem(91001, EquipmentSlot.CODPIECE2).isSuccess)
        assertTrue(paths.contains("/inventory.php"))
        assertTrue(paths.contains("/choice.php"))
        assertTrue(!paths.contains("/inv_equip.php"))
    }
}
