package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.StoragePullRules
import net.sourceforge.kolmafia.request.StorageRequest

class GameRuntimeLibraryAshP542Test {

    data class PullCall(val itemId: Int, val qty: Int)

    private class RecordingStorage(
        private var storage: Map<Int, Int> = emptyMap(),
        private var freepulls: Map<Int, Int> = emptyMap(),
    ) : StorageRequest(HttpClient(MockEngine { respond("ok") })) {
        val calls = mutableListOf<PullCall>()

        override suspend fun withdraw(itemId: Int, quantity: Int): Result<String> {
            calls += PullCall(itemId, quantity)
            return Result.success("ok")
        }

        override suspend fun fetchClassifiedContents(
            characterState: CharacterState?,
            prefs: Preferences?,
        ): StoragePullRules.StorageContents =
            StoragePullRules.StorageContents(storage = storage, freepulls = freepulls)
    }

    private fun hardcoreChar(): KoLCharacter {
        val char = KoLCharacter()
        char.updateFromApiResponse(CharacterApiResponse(hardcore = "1"))
        return char
    }

    private fun roninChar(): KoLCharacter {
        val char = KoLCharacter()
        char.updateFromApiResponse(CharacterApiResponse(hardcore = "0", roninleft = "40"))
        return char
    }

    @BeforeTest
    fun setUp() {
        ConcoctionDatabase.resetForTest()
        ItemDatabase.registerForTest(
            ItemData(2, "seal tooth", "d2", "t.gif", ItemPrimaryUse.FOOD, emptySet(), setOf('t'), 0, null),
        )
        ItemDatabase.registerForTest(
            ItemData(3, "helmet", "d3", "h.gif", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null),
        )
    }

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun revision_phase544() {
        assertEquals("phase826", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun pull_hardcore_onlyFreepullItems() {
        val storage = RecordingStorage(
            storage = mapOf(3 to 2),
            freepulls = mapOf(2 to 4),
        )
        outputLib(
            GameRuntimeLibrary(character = hardcoreChar(), storageRequest = storage),
            """cli_execute("pull seal tooth, helmet");""",
        )
        assertEquals(listOf(PullCall(2, 4)), storage.calls)
    }

    @Test
    fun pull_ronin_printsRemainingMessage() {
        ConcoctionDatabase.setPullsRemaining(4)
        ConcoctionDatabase.setPullsBudgeted(2)
        val storage = RecordingStorage(storage = mapOf(2 to 1))
        val out = outputLib(
            GameRuntimeLibrary(character = roninChar(), storageRequest = storage),
            """cli_execute("pull seal tooth");""",
        )
        assertEquals(listOf(PullCall(2, 1)), storage.calls)
        assertTrue(out.contains("4 pulls remaining, 2 budgeted for automatic use."))
    }
}
