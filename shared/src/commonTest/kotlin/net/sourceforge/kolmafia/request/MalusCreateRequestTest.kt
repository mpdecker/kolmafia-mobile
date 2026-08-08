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

class MalusCreateRequestTest {

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
    fun create_twinklyNuggets_postsGuildAndRetrievesFivePowder() = runTest {
        registerItem(POWDER_ID, "twinkly powder")
        val retrieved = mutableListOf<Pair<Int, Int>>()
        val malusPosts = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            when {
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("guild.php") -> {
                    malusPosts += request.body.toByteArray().decodeToString()
                    respond("You acquire an item: <b>twinkly nuggets</b>", HttpStatusCode.OK)
                }
                else -> respond("ok", HttpStatusCode.OK)
            }
        })
        val request = MalusCreateRequest(
            client = client,
            createItemIngredients = createItemIngredients { id, qty ->
                retrieved += id to qty
                qty
            },
            gameDatabase = null,
        )

        val result = request.create(twinklyNuggetsConcoction(), 1, state = null, preferences = null)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow())
        assertEquals(listOf(POWDER_ID to 5, POWDER_ID to 5), retrieved)
        assertEquals(1, malusPosts.size)
        assertEquals("malussmash", formParam(malusPosts.single(), "action"))
        assertEquals(POWDER_ID.toString(), formParam(malusPosts.single(), "whichitem"))
        assertEquals("1", formParam(malusPosts.single(), "quantity"))
    }

    @Test
    fun create_missingIngredients_stopsPartialBatch() = runTest {
        registerItem(POWDER_ID, "twinkly powder")
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val request = MalusCreateRequest(
            client = client,
            createItemIngredients = createItemIngredients { _, qty -> 0 },
            gameDatabase = null,
        )

        val result = request.create(twinklyNuggetsConcoction(), 2, state = null, preferences = null)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow())
    }

    @Test
    fun create_notPermitted_returnsFailureWithoutPulverizeSkill() = runTest {
        registerItem(POWDER_ID, "twinkly powder")
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val request = MalusCreateRequest(
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

        val result = request.create(twinklyNuggetsConcoction(), 1, state = state, preferences = prefs)

        assertTrue(result.isFailure)
    }

    private fun twinklyNuggetsConcoction() = ConcoctionData(
        result = "twinkly nuggets",
        resultQuantity = 1,
        methods = setOf("MALUS"),
        ingredients = listOf(ConcoctionIngredient("twinkly powder", 5)),
    )

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
        private const val POWDER_ID = 88401
    }
}
