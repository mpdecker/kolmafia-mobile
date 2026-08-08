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
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.item.CreateItemIngredients
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.vykea.VykeaCompanionManager

class VykeaCreateRequestTest {

    private class StubRetrieveItemService(
        private val retrieveFn: suspend (Int, Int) -> Int = { _, qty -> qty },
    ) : RetrieveItemService(
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
    ) {
        override suspend fun retrieve(itemId: Int, qty: Int): Int = retrieveFn(itemId, qty)
    }

    private fun formParam(body: String, key: String): Int? =
        Regex("""$key=(\d+)""").find(body)?.groupValues?.get(1)?.toIntOrNull()

    private fun choiceHtml(choiceId: Int): String =
        """<html><input name="whichchoice" value="$choiceId"></html>"""

    @Test
    fun create_level1Bookshelf_postsChoiceChain() = runTest {
        registerVykeaItems()
        val retrieved = mutableListOf<Pair<Int, Int>>()
        val choicePosts = mutableListOf<Pair<Int, Int>>()
        var choiceStep = 0
        val choiceSequence = listOf(1121, 1122, 1123)
        val client = HttpClient(MockEngine { request ->
            when {
                request.url.toString().contains("inv_use.php") -> respond(choiceHtml(1120), HttpStatusCode.OK)
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("choice.php") -> {
                    val body = request.body.toByteArray().decodeToString()
                    val choiceId = formParam(body, "whichchoice") ?: -1
                    val option = formParam(body, "option") ?: -1
                    choicePosts += choiceId to option
                    val nextChoice = choiceSequence.getOrElse(choiceStep) { 0 }
                    if (choiceStep < choiceSequence.size) choiceStep++
                    respond(choiceHtml(nextChoice), HttpStatusCode.OK)
                }
                else -> respond("ok", HttpStatusCode.OK)
            }
        })
        val prefs = Preferences(MapSettings())
        val request = VykeaCreateRequest(
            useItemRequest = UseItemRequest(client),
            choiceRequest = ChoiceRequest(client),
            retrieveItemService = StubRetrieveItemService { id, qty ->
                retrieved += id to qty
                qty
            },
            createItemIngredients = CreateItemIngredients(
                StubRetrieveItemService { id, qty ->
                    retrieved += id to qty
                    qty
                },
                gameDatabase = null,
            ),
            vykeaCompanionManager = VykeaCompanionManager(prefs),
            gameDatabase = null,
        )
        val concoction = level1BookshelfConcoction()

        val result = request.create(concoction, 1, state = null, preferences = prefs)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow())
        assertTrue(retrieved.any { it.first == VykeaChoiceMapper.HEX_KEY_ID })
        assertTrue(retrieved.any { it.first == VykeaChoiceMapper.PLANK_ID && it.second == 5 })
        assertEquals(
            listOf(
                1120 to 1,
                1121 to VykeaChoiceMapper.SKIP_OPTION,
                1122 to VykeaChoiceMapper.SKIP_OPTION,
                1123 to 1,
            ),
            choicePosts,
        )
    }

    @Test
    fun create_existingCompanion_returnsFailure() = runTest {
        registerVykeaItems()
        val prefs = Preferences(MapSettings())
        prefs.setString(VykeaCompanionManager.CURRENT_VYKEA_PREF, "level 1 bookshelf")
        prefs.setString(VykeaCompanionManager.TYPE_PREF, "bookshelf")
        prefs.setInt(VykeaCompanionManager.LEVEL_PREF, 1)
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val request = VykeaCreateRequest(
            useItemRequest = UseItemRequest(client),
            choiceRequest = ChoiceRequest(client),
            retrieveItemService = StubRetrieveItemService(),
            createItemIngredients = CreateItemIngredients(
                StubRetrieveItemService(),
                gameDatabase = null,
            ),
            vykeaCompanionManager = VykeaCompanionManager(prefs),
            gameDatabase = null,
        )

        val result = request.create(level1BookshelfConcoction(), 1, state = null, preferences = prefs)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("jealous") == true)
    }

    @Test
    fun create_missingHexKey_returnsFailure() = runTest {
        registerVykeaItems()
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val request = VykeaCreateRequest(
            useItemRequest = UseItemRequest(client),
            choiceRequest = ChoiceRequest(client),
            retrieveItemService = StubRetrieveItemService { id, _ ->
                if (id == VykeaChoiceMapper.HEX_KEY_ID) 0 else 1
            },
            createItemIngredients = CreateItemIngredients(
                StubRetrieveItemService { _, qty -> qty },
                gameDatabase = null,
            ),
            vykeaCompanionManager = VykeaCompanionManager(Preferences(MapSettings())),
            gameDatabase = null,
        )

        val result = request.create(level1BookshelfConcoction(), 1, state = null, preferences = null)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("hex key") == true)
    }

    @Test
    fun create_frenzyBookshelf_usesRuneOption() = runTest {
        registerVykeaItems()
        val choicePosts = mutableListOf<Pair<Int, Int>>()
        var choiceStep = 0
        val choiceSequence = listOf(1121, 1122, 1123)
        val client = HttpClient(MockEngine { request ->
            when {
                request.url.toString().contains("inv_use.php") -> respond(choiceHtml(1120), HttpStatusCode.OK)
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("choice.php") -> {
                    val body = request.body.toByteArray().decodeToString()
                    val choiceId = formParam(body, "whichchoice") ?: -1
                    val option = formParam(body, "option") ?: -1
                    choicePosts += choiceId to option
                    val nextChoice = choiceSequence.getOrElse(choiceStep) { 0 }
                    if (choiceStep < choiceSequence.size) choiceStep++
                    respond(choiceHtml(nextChoice), HttpStatusCode.OK)
                }
                else -> respond("ok", HttpStatusCode.OK)
            }
        })
        val request = VykeaCreateRequest(
            useItemRequest = UseItemRequest(client),
            choiceRequest = ChoiceRequest(client),
            retrieveItemService = StubRetrieveItemService { _, qty -> qty },
            createItemIngredients = CreateItemIngredients(
                StubRetrieveItemService { _, qty -> qty },
                gameDatabase = null,
            ),
            vykeaCompanionManager = VykeaCompanionManager(Preferences(MapSettings())),
            gameDatabase = null,
        )
        val concoction = ConcoctionData(
            result = "level 1 frenzy bookshelf",
            resultQuantity = 1,
            methods = setOf("VYKEA"),
            ingredients = listOf(
                ConcoctionIngredient("VYKEA instructions", 1),
                ConcoctionIngredient("VYKEA plank", 5),
                ConcoctionIngredient("VYKEA frenzy rune", 1),
                ConcoctionIngredient("VYKEA plank", 5),
            ),
        )

        val result = request.create(concoction, 1, state = null, preferences = null)

        assertTrue(result.isSuccess)
        assertEquals(1120 to 1, choicePosts[0])
        assertEquals(1121 to 1, choicePosts[1])
    }

    private fun level1BookshelfConcoction() = ConcoctionData(
        result = "level 1 bookshelf",
        resultQuantity = 1,
        methods = setOf("VYKEA"),
        ingredients = listOf(
            ConcoctionIngredient("VYKEA instructions", 1),
            ConcoctionIngredient("VYKEA plank", 5),
            ConcoctionIngredient("VYKEA plank", 5),
        ),
    )

    private fun registerVykeaItems() {
        registerItem(VykeaChoiceMapper.INSTRUCTIONS_ID, "VYKEA instructions")
        registerItem(VykeaChoiceMapper.HEX_KEY_ID, "VYKEA hex key")
        registerItem(VykeaChoiceMapper.PLANK_ID, "VYKEA plank")
        registerItem(VykeaChoiceMapper.RAIL_ID, "VYKEA rail")
        registerItem(VykeaChoiceMapper.BRACKET_ID, "VYKEA bracket")
        registerItem(VykeaChoiceMapper.FRENZY_RUNE_ID, "VYKEA frenzy rune")
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
}
