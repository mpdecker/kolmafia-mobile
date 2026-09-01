package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.request.StorageRequest

class GameRuntimeLibraryAshP540Test {

    data class PullCall(val itemId: Int, val qty: Int)

    private class RecordingStorage : StorageRequest(HttpClient(MockEngine { respond("ok") })) {
        val calls = mutableListOf<PullCall>()
        override suspend fun withdraw(itemId: Int, quantity: Int): Result<String> {
            calls += PullCall(itemId, quantity)
            return Result.success("ok")
        }
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
    fun revision_phase540() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun pull_commaList_qtyOptional() {
        val storage = RecordingStorage()
        outputLib(
            GameRuntimeLibrary(storageRequest = storage),
            """cli_execute("pull seal tooth, 2 helmet");""",
        )
        assertEquals(
            listOf(PullCall(2, 1), PullCall(3, 2)),
            storage.calls,
        )
    }

    @Test
    fun hagnk_commaList_qtyOptional() {
        val storage = RecordingStorage()
        outputLib(
            GameRuntimeLibrary(storageRequest = storage),
            """cli_execute("hagnk 3 seal tooth, helmet");""",
        )
        assertEquals(
            listOf(PullCall(2, 3), PullCall(3, 1)),
            storage.calls,
        )
    }
}
