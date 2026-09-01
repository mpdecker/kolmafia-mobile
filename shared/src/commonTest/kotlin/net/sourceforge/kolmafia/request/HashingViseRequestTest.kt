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
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences

class HashingViseRequestTest {

    @Test
    fun use_postsViseThenChoice1551WithIid() = runTest {
        val requests = mutableListOf<Triple<HttpMethod, String, String>>()
        val client = HttpClient(MockEngine { request ->
            val body = request.body.toByteArray().decodeToString()
            requests += Triple(request.method, request.url.encodedPath, body)
            when (request.url.encodedPath) {
                "/inv_use.php" -> respond(CHOICE_HTML, HttpStatusCode.OK)
                "/choice.php" -> respond(SUCCESS_HTML, HttpStatusCode.OK)
                else -> respond("", HttpStatusCode.NotFound)
            }
        })
        val inventory = inventory(client, schematicCount = 1, checksumCount = 0)
        val request = HashingViseRequest(
            client,
            ChoiceRequest(client),
            inventory,
            Preferences(MapSettings()),
            null,
        )

        val result = request.use(SCHEMATIC_ID, CHECKSUM_ID)

        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertEquals(listOf("/inv_use.php", "/choice.php"), requests.map { it.second })
        assertEquals(listOf(HttpMethod.Post, HttpMethod.Post), requests.map { it.first })
        assertForm(requests[0].third, "whichitem", VISE_ID.toString())
        assertForm(requests[1].third, "whichchoice", "1551")
        assertForm(requests[1].third, "option", "1")
        assertForm(requests[1].third, "iid", SCHEMATIC_ID.toString())
        assertEquals(0, inventory.getCount(SCHEMATIC_ID))
        assertEquals(1, inventory.getCount(CHECKSUM_ID))
    }

    @Test
    fun use_successWithoutChecksumMetadataConsumesSchematicWithoutInventingChecksum() = runTest {
        val client = HttpClient(MockEngine { request ->
            when (request.url.encodedPath) {
                "/inv_use.php" -> respond(CHOICE_HTML, HttpStatusCode.OK)
                "/choice.php" -> respond(SUCCESS_HTML_NO_CHECKSUM, HttpStatusCode.OK)
                else -> respond("", HttpStatusCode.NotFound)
            }
        })
        val inventory = inventory(client, schematicCount = 1, checksumCount = 0)
        val request = HashingViseRequest(
            client,
            ChoiceRequest(client),
            inventory,
            Preferences(MapSettings()),
            null,
        )

        val result = request.use(SCHEMATIC_ID, CHECKSUM_ID)

        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertEquals(0, inventory.getCount(SCHEMATIC_ID))
        assertEquals(0, inventory.getCount(CHECKSUM_ID))
    }

    @Test
    fun use_failedItemUseLeavesInventoryUnchanged() = runTest {
        val client = HttpClient(MockEngine {
            respond("no", HttpStatusCode.InternalServerError)
        })
        val preferences = Preferences(MapSettings()).apply {
            setInt("checksumSentinel", 7)
        }
        val inventory = inventory(client, schematicCount = 1, checksumCount = 3)
        val request = HashingViseRequest(
            client,
            ChoiceRequest(client),
            inventory,
            preferences,
            null,
        )

        val result = request.use(SCHEMATIC_ID, CHECKSUM_ID)

        assertTrue(result.isFailure)
        assertEquals(1, inventory.getCount(SCHEMATIC_ID))
        assertEquals(3, inventory.getCount(CHECKSUM_ID))
        assertEquals(7, preferences.getInt("checksumSentinel", 0))
    }

    @Test
    fun use_malformedChoiceResponseLeavesInventoryUnchanged() = runTest {
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            if (calls == 1) respond(CHOICE_HTML, HttpStatusCode.OK)
            else respond("<html>still in a choice, but no result</html>", HttpStatusCode.OK)
        })
        val preferences = Preferences(MapSettings()).apply {
            setInt("checksumSentinel", 7)
        }
        val inventory = inventory(client, schematicCount = 1, checksumCount = 3)
        val request = HashingViseRequest(
            client,
            ChoiceRequest(client),
            inventory,
            preferences,
            null,
        )

        val result = request.use(SCHEMATIC_ID, CHECKSUM_ID)

        assertTrue(result.isFailure)
        assertEquals(1, inventory.getCount(SCHEMATIC_ID))
        assertEquals(3, inventory.getCount(CHECKSUM_ID))
        assertEquals(7, preferences.getInt("checksumSentinel", 0))
    }

    @Test
    fun use_failedChoiceResponseLeavesInventoryUnchanged() = runTest {
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            if (calls == 1) respond(CHOICE_HTML, HttpStatusCode.OK)
            else respond(SUCCESS_HTML, HttpStatusCode.InternalServerError)
        })
        val inventory = inventory(client, schematicCount = 1, checksumCount = 3)
        val request = HashingViseRequest(
            client,
            ChoiceRequest(client),
            inventory,
            Preferences(MapSettings()),
            null,
        )

        val result = request.use(SCHEMATIC_ID, CHECKSUM_ID)

        assertTrue(result.isFailure)
        assertEquals(1, inventory.getCount(SCHEMATIC_ID))
        assertEquals(3, inventory.getCount(CHECKSUM_ID))
    }

    @Test
    fun use_requiresOwnedSchematicAndHashingVise() = runTest {
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            respond("", HttpStatusCode.OK)
        })
        val noVise = inventory(client, schematicCount = 1, checksumCount = 0, viseCount = 0)
        val missingSchematic = inventory(client, schematicCount = 0, checksumCount = 0)

        assertTrue(
            HashingViseRequest(client, ChoiceRequest(client), noVise, null, null)
                .use(SCHEMATIC_ID).isFailure,
        )
        assertTrue(
            HashingViseRequest(client, ChoiceRequest(client), missingSchematic, null, null)
                .use(SCHEMATIC_ID).isFailure,
        )
        assertEquals(0, calls)
    }

    private fun inventory(
        client: HttpClient,
        schematicCount: Int,
        checksumCount: Int,
        viseCount: Int = 1,
    ): InventoryManager = InventoryManager(client, GameEventBus()).also {
        it.applyParsedInventory(
            buildMap {
                if (viseCount > 0) {
                    put(VISE_ID, InventoryItem(VISE_ID, "hashing vise", viseCount, ItemType.OTHER))
                }
                if (schematicCount > 0) {
                    put(
                        SCHEMATIC_ID,
                        InventoryItem(SCHEMATIC_ID, "test schematic", schematicCount, ItemType.OTHER),
                    )
                }
                if (checksumCount > 0) {
                    put(
                        CHECKSUM_ID,
                        InventoryItem(CHECKSUM_ID, "checksum", checksumCount, ItemType.OTHER),
                    )
                }
            },
        )
    }

    private fun assertForm(body: String, key: String, expected: String) {
        val actual = Regex("""(?:^|&)$key=([^&]+)""").find(body)?.groupValues?.get(1)
        assertEquals(expected, actual, body)
    }

    companion object {
        private const val VISE_ID = 11826
        private const val SCHEMATIC_ID = 11194
        private const val CHECKSUM_ID = 11789
        private const val CHOICE_HTML = "<input name=whichchoice value=1551>"
        private const val SUCCESS_HTML_NO_CHECKSUM =
            "You crush the schematic into little bits of checksum."
        private const val SUCCESS_HTML =
            """You crush the schematic into little bits of checksum.<table class="item" style="float: none" rel="id=11789&s=2&q=0&d=1&g=0&t=1&n=1&m=0&p=0&u=."></table>"""
    }
}
