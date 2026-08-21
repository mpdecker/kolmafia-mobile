package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.request.StorageRequest

class GameRuntimeLibraryAshP543Test {

    private class RecordingStorage : StorageRequest(HttpClient(MockEngine { respond("ok") })) {
        var meatPulled: Int? = null
        var withdrawn: Pair<Int, Int>? = null

        override suspend fun pullMeat(quantity: Int): Result<String> {
            meatPulled = quantity
            return Result.success("ok")
        }

        override suspend fun withdraw(itemId: Int, quantity: Int): Result<String> {
            withdrawn = itemId to quantity
            return Result.success("ok")
        }
    }

    private fun hardcoreChar(): KoLCharacter {
        val char = KoLCharacter()
        char.updateFromApiResponse(CharacterApiResponse(hardcore = "1"))
        return char
    }

    @Test
    fun revision_phase544() {
        assertEquals("phase814", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun pull_meat_recordsQuantity() {
        val storage = RecordingStorage()
        outputLib(
            GameRuntimeLibrary(storageRequest = storage),
            """cli_execute("pull 100 meat");""",
        )
        assertEquals(100, storage.meatPulled)
        assertNull(storage.withdrawn)
    }

    @Test
    fun pull_meat_defaultsToOne() {
        val storage = RecordingStorage()
        outputLib(
            GameRuntimeLibrary(storageRequest = storage),
            """cli_execute("pull meat");""",
        )
        assertEquals(1, storage.meatPulled)
    }

    @Test
    fun pull_meat_hardcore_skips() {
        val storage = RecordingStorage()
        outputLib(
            GameRuntimeLibrary(character = hardcoreChar(), storageRequest = storage),
            """cli_execute("pull 50 meat");""",
        )
        assertNull(storage.meatPulled)
    }
}
