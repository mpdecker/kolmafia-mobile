package net.sourceforge.kolmafia.ash

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.request.EquipmentRequest

class GameRuntimeLibraryAshP100Test {

    @AfterTest
    fun tearDown() {
        ModifierDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

    private class TestInventoryManager(
        items: Map<Int, InventoryItem>,
    ) : InventoryManager(HttpClient(MockEngine { respond("ok") }), GameEventBus()) {
        private val flow = MutableStateFlow(InventoryState(items = items))
        override val state = flow.asStateFlow()
    }

    @Test
    fun revision_phase142() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun cliEquip_codpieceSlot_usesEquipmentRequestAndSyncsGem() {
        val gemName = "ash test gem"
        val gemId = 91001
        val gem = ItemData(
            id = gemId,
            name = gemName,
            descId = "desc$gemId",
            image = "gem.gif",
            primaryUse = ItemPrimaryUse.ACCESSORY,
            secondaryUses = emptySet(),
            access = emptySet(),
            autosellPrice = 0,
            plural = null,
        )
        ItemDatabase.registerForTest(gem)
        ModifierDatabase.injectForTest("EternityCodpiece", gemName, "Muscle: +1")

        val steps = mutableListOf<String>()
        val choiceBodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            val body = if (request.method == HttpMethod.Post) {
                request.body.toByteArray().decodeToString()
            } else {
                ""
            }
            when {
                request.url.encodedPath == "/inventory.php" &&
                    request.url.parameters["action"] == "docodpiece" -> {
                    steps += "docodpiece"
                    respond("ok", HttpStatusCode.OK)
                }
                request.url.encodedPath == "/choice.php" -> {
                    steps += "choice"
                    choiceBodies += body
                    respond("ok", HttpStatusCode.OK)
                }
                request.url.parameters["what"] == "status" -> {
                    steps += "sync"
                    respond(
                        """{"name":"Player","eternitycod":[$gemId,0,0,0,0]}""",
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
                else -> respond("ok", HttpStatusCode.OK)
            }
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val char = KoLCharacter()
        val inv = TestInventoryManager(
            mapOf(gemId to InventoryItem(gemId, gemName, 1, ItemType.OTHER)),
        )
        val db = object : GameDatabase() {
            override fun item(name: String): ItemData? =
                if (name.equals(gemName, ignoreCase = true)) gem else null
        }
        val lib = GameRuntimeLibrary(
            character = char,
            inventoryManager = inv,
            equipmentRequest = EquipmentRequest(client, CharacterRequest(client), char),
            gameDatabase = db,
        )
        runLib(lib, """cli_execute("equip codpiece1 $gemName");""")
        assertEquals(listOf("docodpiece", "choice", "sync"), steps)
        assertTrue(choiceBodies.single().contains("whichchoice=1588"))
        assertTrue(choiceBodies.single().contains("iid=$gemId"))
        assertEquals(gemName, char.state.value.equipment[EquipmentSlot.CODPIECE1])
    }
}
