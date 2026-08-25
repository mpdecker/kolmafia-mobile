package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.request.ClosetRequest

class GameRuntimeLibraryAshP489Test {

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

    private fun closetLib(contents: Map<Int, Int>): GameRuntimeLibrary {
        val closet = object : ClosetRequest(HttpClient(MockEngine { respond("") })) {
            override suspend fun fetchContents(): Map<Int, Int> = contents
        }
        return GameRuntimeLibrary(closetRequest = closet)
    }

    private fun familiarsLib(): GameRuntimeLibrary {
        val json = """
            [
              {"id": 5, "name": "Mr. Wiggles", "race": "Grue", "weight": 10, "exp": 150, "kills": 3, "active": true},
              {"id": 12, "name": "Fluffy", "race": "Bunny", "weight": 7, "exp": 50, "kills": 1, "active": false}
            ]
        """.trimIndent()
        val engine = MockEngine {
            respond(
                content = json,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return GameRuntimeLibrary(familiarManager = FamiliarManager(client, GameEventBus()))
    }

    @Test
    fun revision_phase489() {
        assertEquals("phase2450", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun closet_listsQuantities() {
        registerItem(4, "seal tooth")
        registerItem(2, "meat paste")
        val out = outputLib(
            closetLib(mapOf(4 to 1, 2 to 10)),
            """cli_execute("closet");""",
        )
        val listed = lines(out)
        assertTrue(listed.contains("seal tooth"))
        assertTrue(listed.contains("meat paste (10)"))
    }

    @Test
    fun closetList_filtersByLeftover() {
        registerItem(4, "seal tooth")
        registerItem(2, "meat paste")
        val out = outputLib(
            closetLib(mapOf(4 to 1, 2 to 10)),
            """cli_execute("closet list paste");""",
        )
        val listed = lines(out)
        assertTrue(listed.contains("meat paste (10)"))
        assertFalse(listed.any { it.contains("seal tooth") })
    }

    @Test
    fun familiars_listsRaceAndWeight() {
        val out = outputLib(familiarsLib(), """cli_execute("familiars");""")
        val listed = lines(out)
        assertTrue(listed.contains("Grue (10 lbs)"))
        assertTrue(listed.contains("Bunny (7 lbs)"))
    }

    @Test
    fun familiars_andFamiliarList_filterAndBareFamiliarListsAll() {
        val lib = familiarsLib()
        val filtered = lines(outputLib(lib, """cli_execute("familiars grue");"""))
        assertTrue(filtered.contains("Grue (10 lbs)"))
        assertFalse(filtered.any { it.contains("Bunny") })
        val fromList = lines(outputLib(lib, """cli_execute("familiar list grue");"""))
        assertEquals(filtered, fromList)
        val bare = lines(outputLib(lib, """cli_execute("familiar");"""))
        assertTrue(bare.contains("Grue (10 lbs)"))
        assertTrue(bare.contains("Bunny (7 lbs)"))
    }
}
