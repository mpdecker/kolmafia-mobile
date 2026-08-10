package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameRuntimeLibraryAshP106Test {

    private val barrrnacle = FamiliarData(
        id = 8, name = "Barn", race = "Barrrnacle",
        weight = 10, experience = 0, kills = 0,
    )

    @BeforeTest
    fun loadFamiliars() = runBlocking {
        FamiliarDefinitionDatabase.load()
    }

    @AfterTest
    fun tearDown() {
        StandardRequest.resetForTest()
    }

    private fun makeFamiliarManager(state: FamiliarState): FamiliarManager {
        val fm = FamiliarManager(HttpClient(MockEngine { respond("") }), GameEventBus())
        fm.testSetState(state)
        return fm
    }

    @Test
    fun revision_phase150() {
        assertEquals("phase380", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun have_familiar_beecoreBlocksBeeRaceName() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(path = "Bees Hate You"))
        }
        val fm = makeFamiliarManager(FamiliarState(ownedFamiliars = listOf(barrrnacle)))
        val lib = GameRuntimeLibrary(character = char, familiarManager = fm)
        assertEquals(
            "false",
            outputLib(lib, """print(to_string(have_familiar(to_familiar("Barrrnacle"))));"""),
        )
    }

    @Test
    fun retrieve_item_blockedWhenRestrictedAndNotCraftable() = runTest {
        StandardRequest.parseResponse(
            """
            <b>Items</b><p><span class="i">test widget</span><p>
            """.trimIndent(),
        )
        var storageWithdrawn = false
        val storage = object : StorageRequest(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })) {
            override suspend fun withdraw(itemId: Int, quantity: Int): Result<String> {
                storageWithdrawn = true
                return Result.success("ok")
            }
        }
        val inv = object : InventoryManager(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
            GameEventBus(),
        ) {
            private val flow = MutableStateFlow(InventoryState())
            override val state = flow.asStateFlow()
        }
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(hardcore = "1"))
        }
        val service = RetrieveItemService(
            inventoryManager = inv,
            closetRequest = null,
            storageRequest = storage,
            npcBuyRequest = null,
            mallManager = null,
            gameDatabase = object : net.sourceforge.kolmafia.data.GameDatabase() {
                override fun item(id: Int) = if (id == 42) testWidget() else null
                override fun item(name: String) = if (name == "test widget") testWidget() else null
            },
            character = char,
        )
        assertEquals(0, service.retrieve(42, 1))
        assertEquals(false, storageWithdrawn)
    }

    private fun testWidget() = ItemData(
        id = 42,
        name = "test widget",
        descId = "",
        image = "",
        primaryUse = ItemPrimaryUse.NONE,
        secondaryUses = emptySet(),
        access = setOf('t'),
        autosellPrice = 0,
        plural = null,
    )
}
