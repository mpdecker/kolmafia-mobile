package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.equipment.OutfitManager
import net.sourceforge.kolmafia.equipment.ResolvedOutfit
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.request.StoragePullRules
import net.sourceforge.kolmafia.request.StorageRequest

class GameRuntimeLibraryAshP498Test {

    private fun itemData(id: Int, name: String) = ItemData(
        id = id,
        name = name,
        descId = "",
        image = "",
        primaryUse = ItemPrimaryUse.NONE,
        secondaryUses = emptySet(),
        access = setOf('t'),
        autosellPrice = 0,
        plural = null,
    )

    private fun db(vararg items: Pair<String, Int>) = object : GameDatabase() {
        override fun item(name: String) = items.firstOrNull { it.first.equals(name, ignoreCase = true) }
            ?.let { itemData(it.second, it.first) }
    }

    private fun mockClient() = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })

    private fun hardcoreChar(): KoLCharacter {
        val char = KoLCharacter()
        char.updateFromApiResponse(CharacterApiResponse(hardcore = "1"))
        return char
    }

    private fun interactChar(): KoLCharacter {
        val char = KoLCharacter()
        char.updateFromApiResponse(CharacterApiResponse(hardcore = "0", roninleft = "0"))
        return char
    }

    @Test
    fun revision_phase498() {
        assertEquals("phase3050", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun pull_all_hardcore_errorsWithoutHttp() {
        var emptied = false
        val storage = object : StorageRequest(mockClient()) {
            override suspend fun emptyStorage(): Result<String> {
                emptied = true
                return Result.success("ok")
            }
        }
        val lib = GameRuntimeLibrary(character = hardcoreChar(), storageRequest = storage)
        val out = outputLib(lib, """cli_execute("pull all");""")
        assertTrue(out.contains("You cannot empty storage when you are in Hardcore."))
        assertEquals(false, emptied)
    }

    @Test
    fun pull_all_interact_hitsPullall() {
        var emptied = false
        val storage = object : StorageRequest(mockClient()) {
            override suspend fun emptyStorage(): Result<String> {
                emptied = true
                return Result.success("ok")
            }
        }
        val lib = GameRuntimeLibrary(character = interactChar(), storageRequest = storage)
        outputLib(lib, """cli_execute("pull all");""")
        assertTrue(emptied)
    }

    @Test
    fun hagnk_all_alias_hitsPullall() {
        var emptied = false
        val storage = object : StorageRequest(mockClient()) {
            override suspend fun emptyStorage(): Result<String> {
                emptied = true
                return Result.success("ok")
            }
        }
        val lib = GameRuntimeLibrary(character = interactChar(), storageRequest = storage)
        outputLib(lib, """cli_execute("hagnk all");""")
        assertTrue(emptied)
    }

    @Test
    fun pull_all_ronin_errorsWithoutHttp() {
        var emptied = false
        val storage = object : StorageRequest(mockClient()) {
            override suspend fun emptyStorage(): Result<String> {
                emptied = true
                return Result.success("ok")
            }
        }
        val char = KoLCharacter()
        char.updateFromApiResponse(CharacterApiResponse(hardcore = "0", roninleft = "40"))
        val lib = GameRuntimeLibrary(character = char, storageRequest = storage)
        val out = outputLib(lib, """cli_execute("pull all");""")
        assertTrue(out.contains("You cannot pull everything while your pulls are limited."))
        assertEquals(false, emptied)
    }

    @Test
    fun pull_outfit_withdrawsMissingPiecesOnly() {
        val withdrawn = mutableListOf<Pair<Int, Int>>()
        val storage = object : StorageRequest(mockClient()) {
            override suspend fun fetchClassifiedContents(
                characterState: net.sourceforge.kolmafia.character.CharacterState?,
                prefs: net.sourceforge.kolmafia.preferences.Preferences?,
            ) = StoragePullRules.StorageContents(
                storage = mapOf(20 to 1),
                freepulls = emptyMap(),
            )

            override suspend fun withdraw(itemId: Int, quantity: Int): Result<String> {
                withdrawn += itemId to quantity
                return Result.success("ok")
            }
        }
        val outfits = object : OutfitManager(
            retrieveItemService = null,
            equipmentRequest = net.sourceforge.kolmafia.request.EquipmentRequest(mockClient()),
            customOutfitRequest = net.sourceforge.kolmafia.request.CustomOutfitRequest(mockClient()),
            character = KoLCharacter(),
            gameDatabase = GameDatabase(),
            closetRequest = null,
            storageRequest = storage,
            displayCaseRequest = null,
            clanStashRequest = null,
            inventoryManager = null,
        ) {
            override fun getMatchingOutfit(name: String) =
                ResolvedOutfit(1, "Test Outfit", listOf("helmet turtle", "turtle chest"))
        }
        val inv = object : InventoryManager(mockClient(), GameEventBus()) {
            override val state = MutableStateFlow(
                InventoryState(
                    items = mapOf(
                        10 to InventoryItem(10, "helmet turtle", 1, ItemType.HAT),
                    ),
                ),
            ).asStateFlow()
        }
        val lib = GameRuntimeLibrary(
            character = interactChar(),
            gameDatabase = db("helmet turtle" to 10, "turtle chest" to 20),
            storageRequest = storage,
            outfitManager = outfits,
            inventoryManager = inv,
        )
        val out = outputLib(lib, """cli_execute("pull outfit Test Outfit");""")
        assertTrue(out.contains("helmet turtle is available without pulling."))
        assertEquals(listOf(20 to 1), withdrawn)
    }

    @Test
    fun pull_outfit_hardcore_errors() {
        var withdrawn: Pair<Int, Int>? = null
        val storage = object : StorageRequest(mockClient()) {
            override suspend fun withdraw(itemId: Int, quantity: Int): Result<String> {
                withdrawn = itemId to quantity
                return Result.success("ok")
            }
        }
        val outfits = object : OutfitManager(
            retrieveItemService = null,
            equipmentRequest = net.sourceforge.kolmafia.request.EquipmentRequest(mockClient()),
            customOutfitRequest = net.sourceforge.kolmafia.request.CustomOutfitRequest(mockClient()),
            character = KoLCharacter(),
            gameDatabase = GameDatabase(),
            closetRequest = null,
            storageRequest = storage,
            displayCaseRequest = null,
            clanStashRequest = null,
            inventoryManager = null,
        ) {
            override fun getMatchingOutfit(name: String) =
                ResolvedOutfit(1, "Test Outfit", listOf("helmet turtle"))
        }
        val lib = GameRuntimeLibrary(
            character = hardcoreChar(),
            storageRequest = storage,
            outfitManager = outfits,
        )
        val out = outputLib(lib, """cli_execute("pull outfit Test Outfit");""")
        assertTrue(out.contains("You cannot pull things from storage when you are in Hardcore."))
        assertNull(withdrawn)
    }

    @Test
    fun pull_item_withoutQty_withdrawsOne() {
        var withdrawn: Pair<Int, Int>? = null
        val storage = object : StorageRequest(mockClient()) {
            override suspend fun withdraw(itemId: Int, quantity: Int): Result<String> {
                withdrawn = itemId to quantity
                return Result.success("ok")
            }
        }
        val lib = GameRuntimeLibrary(
            gameDatabase = db("meat paste" to 7),
            storageRequest = storage,
        )
        outputLib(lib, """cli_execute("pull meat paste");""")
        assertEquals(7 to 1, withdrawn)
    }
}
