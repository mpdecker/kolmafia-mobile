package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.DisplayCaseRequest
import net.sourceforge.kolmafia.request.StoragePullRules
import net.sourceforge.kolmafia.request.StorageRequest

class GameRuntimeLibraryAshP490Test {

    @AfterTest
    fun tearDown() {
        ItemDatabase.resetForTest()
    }

    private fun lines(out: String): List<String> =
        out.lines().map { it.trim() }.filter { it.isNotEmpty() }

    private fun registerItem(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    private fun dummyClient(): HttpClient = HttpClient(MockEngine { respond("") })

    private fun storageLib(
        storage: Map<Int, Int>,
        freepulls: Map<Int, Int> = emptyMap(),
    ): GameRuntimeLibrary {
        val request = object : StorageRequest(dummyClient()) {
            override suspend fun fetchClassifiedContents(
                characterState: CharacterState?,
                prefs: Preferences?,
            ): StoragePullRules.StorageContents =
                StoragePullRules.StorageContents(storage = storage, freepulls = freepulls)
        }
        return GameRuntimeLibrary(storageRequest = request)
    }

    private fun displayLib(contents: Map<Int, Int>): GameRuntimeLibrary {
        val request = object : DisplayCaseRequest(dummyClient()) {
            override suspend fun fetchContents(): Map<Int, Int> = contents
        }
        return GameRuntimeLibrary(displayCaseRequest = request)
    }

    @Test
    fun revision_phase490() {
        assertEquals("phase814", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun storage_listsClassifiedStorageQuantities() {
        registerItem(4, "seal tooth")
        registerItem(2, "meat paste")
        registerItem(1, "hot wing")
        val out = outputLib(
            storageLib(
                storage = mapOf(4 to 1, 2 to 10),
                freepulls = mapOf(1 to 5),
            ),
            """cli_execute("storage");""",
        )
        val listed = lines(out)
        assertTrue(listed.contains("seal tooth"))
        assertTrue(listed.contains("meat paste (10)"))
        assertFalse(listed.any { it.contains("hot wing") })
    }

    @Test
    fun storage_filtersByLeftover() {
        registerItem(4, "seal tooth")
        registerItem(2, "meat paste")
        val out = outputLib(
            storageLib(mapOf(4 to 1, 2 to 10)),
            """cli_execute("storage paste");""",
        )
        val listed = lines(out)
        assertTrue(listed.contains("meat paste (10)"))
        assertFalse(listed.any { it.contains("seal tooth") })
    }

    @Test
    fun storagePut_doesNotList() {
        registerItem(4, "seal tooth")
        registerItem(2, "meat paste")
        val out = outputLib(
            storageLib(mapOf(4 to 1, 2 to 10)),
            """cli_execute("storage put 1 meat paste");""",
        )
        assertFalse(lines(out).any { it.contains("meat paste") || it.contains("seal tooth") })
    }

    @Test
    fun display_listsQuantities() {
        registerItem(4, "seal tooth")
        registerItem(2, "meat paste")
        val out = outputLib(
            displayLib(mapOf(4 to 1, 2 to 10)),
            """cli_execute("display");""",
        )
        val listed = lines(out)
        assertTrue(listed.contains("seal tooth"))
        assertTrue(listed.contains("meat paste (10)"))
    }

    @Test
    fun display_filtersByLeftover() {
        registerItem(4, "seal tooth")
        registerItem(2, "meat paste")
        val out = outputLib(
            displayLib(mapOf(4 to 1, 2 to 10)),
            """cli_execute("display paste");""",
        )
        val listed = lines(out)
        assertTrue(listed.contains("meat paste (10)"))
        assertFalse(listed.any { it.contains("seal tooth") })
    }

    @Test
    fun displayPut_doesNotList() {
        registerItem(4, "seal tooth")
        registerItem(2, "meat paste")
        val out = outputLib(
            displayLib(mapOf(4 to 1, 2 to 10)),
            """cli_execute("display put 1 meat paste");""",
        )
        assertFalse(lines(out).any { it.contains("meat paste") || it.contains("seal tooth") })
    }
}
