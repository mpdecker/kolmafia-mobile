package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.StorageRequest
import net.sourceforge.kolmafia.request.StoragePullRules

class GameRuntimeLibraryAshP539Test {

    data class StorageCall(val op: String, val itemId: Int, val qty: Int)

    private class RecordingStorage : StorageRequest(HttpClient(MockEngine { respond("ok") })) {
        val calls = mutableListOf<StorageCall>()
        private var contents: Map<Int, Int> = emptyMap()

        fun seedContents(map: Map<Int, Int>) {
            contents = map
        }

        override suspend fun deposit(itemId: Int, quantity: Int): Result<String> {
            calls += StorageCall("put", itemId, quantity)
            return Result.success("ok")
        }

        override suspend fun withdraw(itemId: Int, quantity: Int): Result<String> {
            calls += StorageCall("take", itemId, quantity)
            return Result.success("ok")
        }

        override suspend fun fetchContents(): Map<Int, Int> = contents

        override suspend fun fetchContents(characterState: CharacterState?): Map<Int, Int> = contents

        override suspend fun fetchClassifiedContents(
            characterState: CharacterState?,
            prefs: Preferences?,
        ): StoragePullRules.StorageContents =
            StoragePullRules.StorageContents(storage = contents, freepulls = emptyMap())
    }

    private fun invWith(vararg items: Pair<Int, Int>): InventoryManager =
        object : InventoryManager(
            HttpClient(MockEngine { respond("ok") }),
            GameEventBus(),
        ) {
            override val state = MutableStateFlow(
                InventoryState(
                    items = items.associate { (id, qty) ->
                        id to InventoryItem(id, "item$id", qty, ItemType.OTHER)
                    },
                ),
            ).asStateFlow()
        }

    @BeforeTest
    fun setUp() {
        ItemDatabase.registerForTest(
            ItemData(2, "seal tooth", "d2", "t.gif", ItemPrimaryUse.FOOD, emptySet(), setOf('t'), 0, null),
        )
        ItemDatabase.registerForTest(
            ItemData(3, "helmet", "d3", "h.gif", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null),
        )
    }

    @AfterTest
    fun tearDown() {
        ItemDatabase.resetForTest()
    }

    @Test
    fun revision_phase539() {
        assertEquals("phase550", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun storage_put_commaList_qtyOptional() {
        val storage = RecordingStorage()
        val out = outputLib(
            GameRuntimeLibrary(
                storageRequest = storage,
                inventoryManager = invWith(2 to 3, 3 to 1),
            ),
            """cli_execute("storage put 2 seal tooth, helmet");""",
        )
        assertEquals(
            listOf(StorageCall("put", 2, 2), StorageCall("put", 3, 1)),
            storage.calls,
        )
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun storage_take_commaList_usesStorageCount() {
        val storage = RecordingStorage().also { it.seedContents(mapOf(2 to 4, 3 to 1)) }
        outputLib(
            GameRuntimeLibrary(storageRequest = storage),
            """cli_execute("storage take seal tooth, 2 helmet");""",
        )
        assertEquals(
            listOf(StorageCall("take", 2, 4), StorageCall("take", 3, 2)),
            storage.calls,
        )
    }
}
