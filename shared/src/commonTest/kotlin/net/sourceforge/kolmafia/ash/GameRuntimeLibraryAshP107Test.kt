package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.familiar.FamiliarState
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.request.StandardRequest
import net.sourceforge.kolmafia.request.StorageRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameRuntimeLibraryAshP107Test {

    private val widgetId = 42
    private val widgetName = "phase107 widget"

    private fun widgetDb() = object : GameDatabase() {
        override fun item(id: Int) = if (id == widgetId) {
            ItemData(widgetId, widgetName, "", "", ItemPrimaryUse.NONE, emptySet(), setOf('t'), 0, null)
        } else null
        override fun item(name: String) = if (name == widgetName) item(widgetId) else null
    }

    @Test
    fun inTerrarium_ownedOnBeecoreWhileHaveFamiliarFalse() = runBlocking {
        val barrrnacle = FamiliarData(
            id = 8, name = "Barn", race = "Barrrnacle",
            weight = 10, experience = 0, kills = 0,
        )
        val fm = FamiliarManager(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
            GameEventBus(),
        ).also {
            it.testSetState(FamiliarState(ownedFamiliars = listOf(barrrnacle)))
        }
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(path = "Bees Hate You"))
        }
        val lib = GameRuntimeLibrary(character = char, familiarManager = fm)
        assertEquals(
            "false",
            outputLib(lib, """print(to_string(have_familiar(to_familiar("Barrrnacle"))));"""),
        )
        assertEquals(
            "true",
            outputLib(lib, """print(to_string(in_terrarium(to_familiar("Barrrnacle"))));"""),
        )
    }

    @Test
    fun retrieveItem_blockedAfterLazyStandardListInit() = runBlocking {
        var storageWithdrawn = false
        val storage = object : StorageRequest(HttpClient(MockEngine { respond("") })) {
            override suspend fun withdraw(itemId: Int, quantity: Int): Result<String> {
                storageWithdrawn = true
                return Result.success("ok")
            }
        }
        var initCalled = false
        val standard = object : StandardRequest(HttpClient(MockEngine { respond("") })) {
            override suspend fun ensureInitialized() {
                initCalled = true
                parseResponse(
                    """
                    <b>Items</b><p><span class="i">$widgetName</span><p>
                    """.trimIndent(),
                )
            }
        }
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(hardcore = "1"))
        }
        val inv = object : InventoryManager(HttpClient(MockEngine { respond("") }), GameEventBus()) {
            override val state = kotlinx.coroutines.flow.MutableStateFlow(InventoryState())
        }
        val retrieve = RetrieveItemService(
            inventoryManager = inv,
            closetRequest = null,
            storageRequest = storage,
            npcBuyRequest = null,
            mallManager = null,
            gameDatabase = widgetDb(),
            character = char,
            standardRequest = standard,
        )
        val lib = GameRuntimeLibrary(
            character = char,
            gameDatabase = widgetDb(),
            retrieveItemService = retrieve,
        )
        try {
            assertEquals(
                "false",
                outputLib(
                    lib,
                    """print(to_string(retrieve_item(1, to_item("$widgetName"))));""",
                ),
            )
            assertTrue(initCalled)
            assertFalse(storageWithdrawn)
        } finally {
            StandardRequest.resetForTest()
        }
    }

    @Test
    fun revision_phase150() {
        assertEquals("phase160", GameRuntimeLibrary.REVISION)
    }
}
