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
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.item.CreateItemIngredients
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.preferences.Preferences

class StaffCreateRequestTest {

    private fun createItemIngredients(
        retrieveFn: suspend (Int, Int) -> Int = { _, qty -> qty },
    ): CreateItemIngredients = CreateItemIngredients(StubRetrieveItemService(retrieveFn), null)

    private class StubRetrieveItemService(
        private val retrieveFn: suspend (Int, Int) -> Int = { _, qty -> qty },
    ) : RetrieveItemService(
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
    ) {
        override suspend fun retrieve(itemId: Int, qty: Int): Int = retrieveFn(itemId, qty)
    }

    private fun formParam(body: String, key: String): String? =
        Regex("""(?:^|&)$key=([^&]+)""").find(body)?.groupValues?.get(1)

    @Test
    fun create_teapotTempest_postsGuildAndRetrievesIngredients() = runTest {
        registerStaffIngredients()
        val retrieved = mutableListOf<Pair<Int, Int>>()
        val staffPosts = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            when {
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("guild.php") -> {
                    staffPosts += request.body.toByteArray().decodeToString()
                    respond("You acquire an item: <b>Staff of the Teapot Tempest</b>", HttpStatusCode.OK)
                }
                else -> respond("ok", HttpStatusCode.OK)
            }
        })
        val request = StaffCreateRequest(
            client = client,
            createItemIngredients = createItemIngredients { id, qty ->
                retrieved += id to qty
                qty
            },
            gameDatabase = null,
        )

        val result = request.create(teapotTempestConcoction(), 1, state = null, preferences = null)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow())
        assertEquals(
            listOf(
                BASE_STICK_ID to 1,
                MENUDO_ID to 1,
                SANGRIA_ID to 1,
                TEA_ID to 1,
                PILL_ID to 1,
                JUICE_ID to 1,
                BASE_STICK_ID to 1,
                MENUDO_ID to 1,
                SANGRIA_ID to 1,
                TEA_ID to 1,
                PILL_ID to 1,
                JUICE_ID to 1,
            ),
            retrieved,
        )
        assertEquals(1, staffPosts.size)
        assertEquals("makestaff", formParam(staffPosts.single(), "action"))
        assertEquals(BASE_STICK_ID.toString(), formParam(staffPosts.single(), "whichstaff"))
    }

    @Test
    fun create_missingIngredients_stopsPartialBatch() = runTest {
        registerStaffIngredients()
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val request = StaffCreateRequest(
            client = client,
            createItemIngredients = createItemIngredients { id, qty ->
                if (id == MENUDO_ID) 0 else qty
            },
            gameDatabase = null,
        )

        val result = request.create(teapotTempestConcoction(), 2, state = null, preferences = null)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow())
    }

    @Test
    fun create_notPermitted_returnsFailure() = runTest {
        registerItem(BASE_STICK_ID, "big stirring stick")
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val request = StaffCreateRequest(
            client = client,
            createItemIngredients = createItemIngredients { _, qty -> qty },
            gameDatabase = null,
        )
        val prefs = Preferences(MapSettings())
        prefs.setInt("lastGuildStoreOpen", 5)
        val state = CharacterState(
            characterClass = CharacterClass.SEAL_CLUBBER.id,
            ascensionNumber = 5,
        )

        val result = request.create(teapotTempestConcoction(), 1, state = state, preferences = prefs)

        assertTrue(result.isFailure)
    }

    private fun teapotTempestConcoction() = ConcoctionData(
        result = "Staff of the Teapot Tempest",
        resultQuantity = 1,
        methods = setOf("STAFF"),
        ingredients = listOf(
            ConcoctionIngredient("big stirring stick", 1),
            ConcoctionIngredient("menudo", 1),
            ConcoctionIngredient("sangria", 1),
            ConcoctionIngredient("hippy herbal tea", 1),
            ConcoctionIngredient("concentrated magicalness pill", 1),
            ConcoctionIngredient("magical mystery juice (3)", 1),
        ),
    )

    private fun registerStaffIngredients() {
        registerItem(BASE_STICK_ID, "big stirring stick")
        registerItem(MENUDO_ID, "menudo")
        registerItem(SANGRIA_ID, "sangria")
        registerItem(TEA_ID, "hippy herbal tea")
        registerItem(PILL_ID, "concentrated magicalness pill")
        registerItem(JUICE_ID, "magical mystery juice (3)")
    }

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
                autosellPrice = 100,
                plural = null,
            ),
        )
    }

    companion object {
        private const val BASE_STICK_ID = 88201
        private const val MENUDO_ID = 88202
        private const val SANGRIA_ID = 88203
        private const val TEA_ID = 88204
        private const val PILL_ID = 88205
        private const val JUICE_ID = 88206
    }
}
