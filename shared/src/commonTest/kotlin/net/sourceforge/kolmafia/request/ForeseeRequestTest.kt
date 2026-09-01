package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.ash.outputLib
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.EquipmentManager

class ForeseeRequestTest {

    @Test
    fun foresee_getsInventoryActionThenChoice1558WithWho() = runTest {
        val requests = mutableListOf<Triple<HttpMethod, String, String>>()
        val client = HttpClient(MockEngine { request ->
            val body = request.body.toByteArray().decodeToString()
            val query = request.url.encodedQuery
            requests += Triple(request.method, request.url.encodedPath, "$query|$body")
            when (request.url.encodedPath) {
                "/inventory.php" -> respond(VISIT_HTML, HttpStatusCode.OK)
                "/choice.php" -> respond(SUCCESS_HTML, HttpStatusCode.OK)
                else -> respond("", HttpStatusCode.NotFound)
            }
        })
        val preferences = Preferences(MapSettings()).apply { setInt("_perilsForeseen", 1) }
        val inventory = inventory(client, peridotCount = 1)
        val request = ForeseeRequest(
            client,
            ChoiceRequest(client),
            inventory,
            null,
            preferences,
            null,
        )

        val result = request.foresee(PLAYER_ID)

        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertEquals(listOf("/inventory.php", "/choice.php"), requests.map { it.second })
        assertEquals(listOf(HttpMethod.Get, HttpMethod.Post), requests.map { it.first })
        assertTrue(requests[0].third.contains("action=foresee"), requests[0].third)
        assertForm(requests[1].third.substringAfter('|'), "whichchoice", "1558")
        assertForm(requests[1].third.substringAfter('|'), "option", "1")
        assertForm(requests[1].third.substringAfter('|'), "who", PLAYER_ID.toString())
        assertEquals(2, preferences.getInt("_perilsForeseen", 0))
        assertEquals(1, inventory.getCount(PERIDOT_ID))
    }

    @Test
    fun foresee_rejectsThreePerilCapBeforeHttp() = runTest {
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            respond(SUCCESS_HTML, HttpStatusCode.OK)
        })
        val preferences = Preferences(MapSettings()).apply { setInt("_perilsForeseen", 3) }
        val inventory = inventory(client, peridotCount = 1)
        val request = ForeseeRequest(
            client,
            ChoiceRequest(client),
            inventory,
            null,
            preferences,
            null,
        )

        val result = request.foresee(PLAYER_ID)

        assertTrue(result.isFailure)
        assertEquals(0, calls)
        assertEquals(3, preferences.getInt("_perilsForeseen", 0))
        assertEquals(1, inventory.getCount(PERIDOT_ID))
    }

    @Test
    fun foresee_rejectsUnavailableEquipmentBeforeHttp() = runTest {
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            respond(SUCCESS_HTML, HttpStatusCode.OK)
        })
        val preferences = Preferences(MapSettings())
        val inventory = inventory(client, peridotCount = 0)
        val character = KoLCharacter()
        val equipment = EquipmentManager(character, inventory)
        val request = ForeseeRequest(
            client,
            ChoiceRequest(client),
            inventory,
            equipment,
            preferences,
            null,
        )

        val result = request.foresee(PLAYER_ID)

        assertTrue(result.isFailure)
        assertEquals(0, calls)
        assertEquals(0, preferences.getInt("_perilsForeseen", 0))
        assertEquals(0, inventory.getCount(PERIDOT_ID))
    }

    @Test
    fun foresee_allowsEquippedPeridotWithoutInventoryCopy() = runTest {
        val requests = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            requests += request.url.encodedPath
            when (request.url.encodedPath) {
                "/inventory.php" -> respond(VISIT_HTML, HttpStatusCode.OK)
                "/choice.php" -> respond(SUCCESS_HTML, HttpStatusCode.OK)
                else -> respond("", HttpStatusCode.NotFound)
            }
        })
        val preferences = Preferences(MapSettings())
        val inventory = inventory(client, peridotCount = 0)
        ItemDatabase.registerItem(PERIDOT_ID, "Peridot of Peril", "519151496")
        val character = KoLCharacter().also {
            it.updateEquipment(EquipmentSlot.ACC1, "Peridot of Peril")
        }
        val request = ForeseeRequest(
            client,
            ChoiceRequest(client),
            inventory,
            EquipmentManager(character, inventory),
            preferences,
            null,
        )

        val result = request.foresee(PLAYER_ID)

        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertEquals(listOf("/inventory.php", "/choice.php"), requests)
        assertEquals(1, preferences.getInt("_perilsForeseen", 0))
        assertEquals(0, inventory.getCount(PERIDOT_ID))
    }

    @Test
    fun foresee_failedInventoryGetLeavesCountUnchanged() = runTest {
        val client = HttpClient(MockEngine {
            respond("no", HttpStatusCode.InternalServerError)
        })
        val preferences = Preferences(MapSettings()).apply { setInt("_perilsForeseen", 1) }
        val inventory = inventory(client, peridotCount = 1)
        val request = ForeseeRequest(
            client,
            ChoiceRequest(client),
            inventory,
            null,
            preferences,
            null,
        )

        val result = request.foresee(PLAYER_ID)

        assertTrue(result.isFailure)
        assertEquals(1, preferences.getInt("_perilsForeseen", 0))
        assertEquals(1, inventory.getCount(PERIDOT_ID))
    }

    @Test
    fun foresee_malformedChoiceResponseLeavesCountUnchanged() = runTest {
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            if (calls == 1) respond(VISIT_HTML, HttpStatusCode.OK)
            else respond("<html>choice page without a gaze</html>", HttpStatusCode.OK)
        })
        val preferences = Preferences(MapSettings()).apply { setInt("_perilsForeseen", 1) }
        val inventory = inventory(client, peridotCount = 1)
        val request = ForeseeRequest(
            client,
            ChoiceRequest(client),
            inventory,
            null,
            preferences,
            null,
        )

        val result = request.foresee(PLAYER_ID)

        assertTrue(result.isFailure)
        assertEquals(1, preferences.getInt("_perilsForeseen", 0))
        assertEquals(1, inventory.getCount(PERIDOT_ID))
    }

    @Test
    fun foresee_failedChoiceResponseLeavesCountUnchanged() = runTest {
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            if (calls == 1) respond(VISIT_HTML, HttpStatusCode.OK)
            else respond(SUCCESS_HTML, HttpStatusCode.InternalServerError)
        })
        val preferences = Preferences(MapSettings()).apply { setInt("_perilsForeseen", 1) }
        val inventory = inventory(client, peridotCount = 1)
        val request = ForeseeRequest(
            client,
            ChoiceRequest(client),
            inventory,
            null,
            preferences,
            null,
        )

        val result = request.foresee(PLAYER_ID)

        assertTrue(result.isFailure)
        assertEquals(1, preferences.getInt("_perilsForeseen", 0))
        assertEquals(1, inventory.getCount(PERIDOT_ID))
    }

    @Test
    fun parseResponse_writesDailyUseOnlyOnce() {
        val preferences = Preferences(MapSettings()).apply { setInt("_perilsForeseen", 1) }
        val request = ForeseeRequest(
            HttpClient(MockEngine { respond("") }),
            ChoiceRequest(HttpClient(MockEngine { respond("") })),
            inventory(HttpClient(MockEngine { respond("") }), peridotCount = 1),
            null,
            preferences,
            null,
        )
        val url = "choice.php?whichchoice=1558&option=1&who=$PLAYER_ID"

        assertTrue(request.parseResponse(url, SUCCESS_HTML))
        assertTrue(request.parseResponse(url, SUCCESS_HTML))

        assertEquals(2, preferences.getInt("_perilsForeseen", 0))
    }

    @Test
    fun cliForesee_statusAndCapDoNotIssueHttp() {
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            respond("", HttpStatusCode.OK)
        })
        val inventory = inventory(client, peridotCount = 1)
        val statusLib = GameRuntimeLibrary(
            preferences = Preferences(MapSettings()).apply { setInt("_perilsForeseen", 1) },
            inventoryManager = inventory,
            foreseeRequest = ForeseeRequest(client, ChoiceRequest(client), inventory),
        )
        val cappedLib = GameRuntimeLibrary(
            preferences = Preferences(MapSettings()).apply { setInt("_perilsForeseen", 3) },
            inventoryManager = inventory,
            foreseeRequest = ForeseeRequest(client, ChoiceRequest(client), inventory),
        )

        val status = outputLib(statusLib, """cli_execute("foresee");""")
        val blocked = outputLib(cappedLib, """cli_execute("foresee $PLAYER_ID");""")

        assertTrue(status.contains("1"), status)
        assertTrue(blocked.contains("thrice") || blocked.contains("already"), blocked)
        assertEquals(0, calls)
    }

    @Test
    fun cliForesee_usesTypedRequestAndPrintsItsFailure() {
        val client = HttpClient(MockEngine { respond("") })
        val fake = object : ForeseeRequest(client, ChoiceRequest(client)) {
            override suspend fun foresee(perilId: Int?): Result<String> =
                Result.failure(IllegalStateException("typed foresee failure"))
        }
        val lib = GameRuntimeLibrary(
            preferences = Preferences(MapSettings()),
            inventoryManager = inventory(client, peridotCount = 1),
            foreseeRequest = fake,
        )

        val out = outputLib(lib, """cli_execute("foresee $PLAYER_ID");""")

        assertTrue(out.contains("typed foresee failure"), out)
    }

    private fun inventory(client: HttpClient, peridotCount: Int): InventoryManager =
        InventoryManager(client, GameEventBus()).also {
            if (peridotCount > 0) {
                it.applyParsedInventory(
                    mapOf(
                        PERIDOT_ID to InventoryItem(
                            PERIDOT_ID,
                            "Peridot of Peril",
                            peridotCount,
                            ItemType.ACCESSORY,
                        ),
                    ),
                )
            }
        }

    private fun assertForm(body: String, key: String, expected: String) {
        val actual = Regex("""(?:^|&)$key=([^&]+)""").find(body)?.groupValues?.get(1)
        assertEquals(expected, actual, body)
    }

    companion object {
        private const val PERIDOT_ID = 11905
        private const val PLAYER_ID = 12345
        private const val VISIT_HTML = "You can foresee peril 2 more times today."
        private const val SUCCESS_HTML = "You gaze into your Peridot and foresee a horrible future"
    }
}
