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
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.ash.outputLib
import net.sourceforge.kolmafia.campground.CampgroundInventorySync
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences

class PottedTeaTreeRequestTest {

    @Test
    fun shake_postsCampgroundThenChoice1104() = runTest {
        val requests = mutableListOf<Triple<HttpMethod, String, String>>()
        val client = recordingClient(requests)
        val preferences = plantedPrefs()
        val inventory = inventory(client)
        val request = PottedTeaTreeRequest(
            client,
            CampgroundRequest(client, preferences, inventoryManager = inventory),
            ChoiceRequest(client),
            inventory,
            preferences,
            null,
        )

        val result = request.shake()

        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertEquals(listOf("/campground.php", "/choice.php"), requests.map { it.second })
        assertEquals(listOf(HttpMethod.Post, HttpMethod.Post), requests.map { it.first })
        assertForm(requests[0].third, "action", "teatree")
        assertForm(requests[1].third, "whichchoice", "1104")
        assertForm(requests[1].third, "option", "1")
        assertTrue(preferences.getBoolean("_pottedTeaTreeUsed", false))
        assertEquals(1, inventory.getCount(TEA_ID))
    }

    @Test
    fun select_postsCampgroundThenChoices1104And1105WithItemid() = runTest {
        val requests = mutableListOf<Triple<HttpMethod, String, String>>()
        val client = recordingClient(requests)
        val preferences = plantedPrefs()
        val inventory = inventory(client)
        val request = PottedTeaTreeRequest(
            client,
            CampgroundRequest(client, preferences, inventoryManager = inventory),
            ChoiceRequest(client),
            inventory,
            preferences,
            null,
        )

        val result = request.select(TEA_ID)

        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertEquals(
            listOf("/campground.php", "/choice.php", "/choice.php"),
            requests.map { it.second },
        )
        assertEquals(
            listOf(HttpMethod.Post, HttpMethod.Post, HttpMethod.Post),
            requests.map { it.first },
        )
        assertForm(requests[0].third, "action", "teatree")
        assertForm(requests[1].third, "whichchoice", "1104")
        assertForm(requests[1].third, "option", "2")
        assertForm(requests[2].third, "whichchoice", "1105")
        assertForm(requests[2].third, "option", "1")
        assertForm(requests[2].third, "itemid", TEA_ID.toString())
        assertTrue(preferences.getBoolean("_pottedTeaTreeUsed", false))
        assertEquals(1, inventory.getCount(TEA_ID))
    }

    @Test
    fun shake_rejectsSecondDailyUseBeforeHttp() = runTest {
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            respond(SUCCESS_HTML, HttpStatusCode.OK)
        })
        val preferences = plantedPrefs().apply { setBoolean("_pottedTeaTreeUsed", true) }
        val inventory = inventory(client)
        val request = PottedTeaTreeRequest(
            client,
            CampgroundRequest(client, preferences, inventoryManager = inventory),
            ChoiceRequest(client),
            inventory,
            preferences,
            null,
        )

        val result = request.shake()

        assertTrue(result.isFailure)
        assertEquals(0, calls)
        assertTrue(preferences.getBoolean("_pottedTeaTreeUsed", false))
        assertEquals(0, inventory.getCount(TEA_ID))
    }

    @Test
    fun select_failedCampgroundLeavesDailyUseAndInventoryUnchanged() = runTest {
        val client = HttpClient(MockEngine {
            respond("no", HttpStatusCode.InternalServerError)
        })
        val preferences = plantedPrefs()
        val inventory = inventory(client)
        val request = PottedTeaTreeRequest(
            client,
            CampgroundRequest(client, preferences, inventoryManager = inventory),
            ChoiceRequest(client),
            inventory,
            preferences,
            null,
        )

        val result = request.select(TEA_ID)

        assertTrue(result.isFailure)
        assertFalse(preferences.getBoolean("_pottedTeaTreeUsed", false))
        assertEquals(0, inventory.getCount(TEA_ID))
    }

    @Test
    fun select_malformedChoiceResponseLeavesDailyUseAndInventoryUnchanged() = runTest {
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            if (calls == 1) respond(CHOICE_HTML, HttpStatusCode.OK)
            else respond("<html>still in a choice, but no tea</html>", HttpStatusCode.OK)
        })
        val preferences = plantedPrefs()
        val inventory = inventory(client)
        val request = PottedTeaTreeRequest(
            client,
            CampgroundRequest(client, preferences, inventoryManager = inventory),
            ChoiceRequest(client),
            inventory,
            preferences,
            null,
        )

        val result = request.select(TEA_ID)

        assertTrue(result.isFailure)
        assertFalse(preferences.getBoolean("_pottedTeaTreeUsed", false))
        assertEquals(0, inventory.getCount(TEA_ID))
    }

    @Test
    fun select_failedChoiceResponseLeavesDailyUseAndInventoryUnchanged() = runTest {
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            if (calls <= 2) respond(CHOICE_HTML, HttpStatusCode.OK)
            else respond(SUCCESS_HTML, HttpStatusCode.InternalServerError)
        })
        val preferences = plantedPrefs()
        val inventory = inventory(client)
        val request = PottedTeaTreeRequest(
            client,
            CampgroundRequest(client, preferences, inventoryManager = inventory),
            ChoiceRequest(client),
            inventory,
            preferences,
            null,
        )

        val result = request.select(TEA_ID)

        assertTrue(result.isFailure)
        assertFalse(preferences.getBoolean("_pottedTeaTreeUsed", false))
        assertEquals(0, inventory.getCount(TEA_ID))
    }

    @Test
    fun parseResponse_writesDailyUseOnlyOnce() {
        val preferences = plantedPrefs()
        val inventory = inventory(HttpClient(MockEngine { respond("") }))
        val request = PottedTeaTreeRequest(
            HttpClient(MockEngine { respond("") }),
            CampgroundRequest(HttpClient(MockEngine { respond("") })),
            ChoiceRequest(HttpClient(MockEngine { respond("") })),
            inventory,
            preferences,
            null,
        )
        val url = "choice.php?whichchoice=1105&option=1&itemid=$TEA_ID"

        assertTrue(request.parseResponse(url, SUCCESS_HTML, TEA_ID))
        assertTrue(request.parseResponse(url, SUCCESS_HTML, TEA_ID))

        assertTrue(preferences.getBoolean("_pottedTeaTreeUsed", false))
        assertEquals(1, inventory.getCount(TEA_ID))
    }

    @Test
    fun cliTeatree_statusAndAlreadyUsedDoNotIssueHttp() {
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            respond("", HttpStatusCode.OK)
        })
        val unused = GameRuntimeLibrary(
            preferences = plantedPrefs(),
            pottedTeaTreeRequest = PottedTeaTreeRequest(
                client,
                CampgroundRequest(client),
                ChoiceRequest(client),
            ),
        )
        val usedPrefs = plantedPrefs().apply { setBoolean("_pottedTeaTreeUsed", true) }
        val used = GameRuntimeLibrary(
            preferences = usedPrefs,
            pottedTeaTreeRequest = PottedTeaTreeRequest(
                client,
                CampgroundRequest(client),
                ChoiceRequest(client),
            ),
        )

        val status = outputLib(unused, """cli_execute("teatree");""")
        val blocked = outputLib(used, """cli_execute("teatree shake");""")

        assertTrue(status.contains("used today: false"), status)
        assertTrue(blocked.contains("already harvested"), blocked)
        assertEquals(0, calls)
    }

    @Test
    fun cliTeatree_usesTypedRequestAndPrintsItsFailure() {
        val client = HttpClient(MockEngine { respond("") })
        val fake = object : PottedTeaTreeRequest(client, CampgroundRequest(client), ChoiceRequest(client)) {
            override suspend fun shake(): Result<String> =
                Result.failure(IllegalStateException("typed teatree failure"))
        }
        val lib = GameRuntimeLibrary(
            preferences = plantedPrefs(),
            pottedTeaTreeRequest = fake,
        )

        val out = outputLib(lib, """cli_execute("teatree shake");""")

        assertTrue(out.contains("typed teatree failure"), out)
    }

    private fun recordingClient(
        requests: MutableList<Triple<HttpMethod, String, String>>,
    ): HttpClient = HttpClient(MockEngine { request ->
        val body = request.body.toByteArray().decodeToString()
        requests += Triple(request.method, request.url.encodedPath, body)
        when (request.url.encodedPath) {
            "/campground.php" -> respond(CHOICE_HTML, HttpStatusCode.OK)
            "/choice.php" -> respond(SUCCESS_HTML, HttpStatusCode.OK)
            else -> respond("", HttpStatusCode.NotFound)
        }
    })

    private fun plantedPrefs(): Preferences = Preferences(MapSettings()).also {
        CampgroundInventorySync.setItem(it, TREE_ID, 1)
    }

    private fun inventory(client: HttpClient): InventoryManager =
        InventoryManager(client, GameEventBus())

    private fun assertForm(body: String, key: String, expected: String) {
        val actual = Regex("""(?:^|&)$key=([^&]+)""").find(body)?.groupValues?.get(1)
        assertEquals(expected, actual, body)
    }

    companion object {
        private const val TREE_ID = 8600
        private const val TEA_ID = 8624
        private const val CHOICE_HTML = "<input name=whichchoice value=1104>"
        private const val SUCCESS_HTML =
            """You acquire an item: cuppa Activi tea.<table class="item" style="float: none" rel="id=8624&s=2&q=0&d=1&g=0&t=1&n=1&m=0&p=0&u=."></table>"""
    }
}
