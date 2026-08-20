package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.item.CreateItemIngredients
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CampgroundRequest
import net.sourceforge.kolmafia.request.ConcoctionCreateRequest
import net.sourceforge.kolmafia.request.CraftRequest
import net.sourceforge.kolmafia.request.ClipArtCreateRequest
import net.sourceforge.kolmafia.request.FalloutShelterRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.MuseCreateRequest
import net.sourceforge.kolmafia.request.SewerCreateRequest
import net.sourceforge.kolmafia.request.VykeaCreateRequest
import net.sourceforge.kolmafia.vykea.VykeaCompanionManager
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.request.TerminalExtrudeCreateRequest
import net.sourceforge.kolmafia.request.TerminalRequest
import net.sourceforge.kolmafia.request.UseItemRequest
import net.sourceforge.kolmafia.shop.CoinmasterManager
import net.sourceforge.kolmafia.shop.CoinmasterRequest
import net.sourceforge.kolmafia.shop.ShopRequest

class GameRuntimeLibraryAshP304Test {

    private class TrackingRetrieveItemService(
        private val retrievedIds: MutableList<Int>,
        private val retrieveFn: suspend (Int, Int) -> Int = { _, qty -> qty },
    ) : RetrieveItemService(
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
    ) {
        override suspend fun retrieve(itemId: Int, qty: Int): Int {
            retrievedIds += itemId
            return retrieveFn(itemId, qty)
        }
    }

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        ConcoctionDatabase.resetForTest()
    }

    @Test
    fun revision_isphase333() {
        assertEquals("phase743", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun create_usesConcoctionCreateRequestForAutoCraftable() {
        registerItem(92001, "ash craft soda")
        registerItem(92002, "ash seltzer")
        registerItem(92003, "ash sweetener")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "ash craft soda",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(
                    ConcoctionIngredient("ash seltzer", 1),
                    ConcoctionIngredient("ash sweetener", 1),
                ),
            ),
        )
        val craftUrls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            craftUrls += req.url.toString()
            respond("<!-- cr:1x92002,92003=92001 -->")
        })
        val retrievedIds = mutableListOf<Int>()
        val retrieve = TrackingRetrieveItemService(retrievedIds)
        val db = stubDb()
        val createRequest = ConcoctionCreateRequest(
            retrieveItemService = retrieve,
            craftRequest = CraftRequest(client),
            useItemRequest = UseItemRequest(client),
            gameDatabase = db,
            createItemIngredients = CreateItemIngredients(retrieve, db),
        )
        val lib = GameRuntimeLibrary(
            gameDatabase = db,
            concoctionCreateRequest = createRequest,
            retrieveItemService = retrieve,
        )
        val output = outputLib(
            lib,
            """
            boolean ok = create(1, to_item("ash craft soda"));
            print(ok);
            """.trimIndent(),
        ).trim()
        assertEquals("true", output)
        assertTrue(craftUrls.any { it.contains("craft.php") })
        assertTrue(92002 in retrievedIds)
        assertTrue(92003 in retrievedIds)
        assertTrue(92001 !in retrievedIds)
    }

    @Test
    fun create_usesStillShopForStillCraftable() {
        registerItem(92021, "ash still booze")
        registerItem(92022, "ash still gin")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "ash still booze",
                resultQuantity = 1,
                methods = setOf("STILL", "ROW55"),
                ingredients = listOf(ConcoctionIngredient("ash still gin", 1)),
            ),
        )
        val shopBodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            shopBodies += req.body.toByteArray().decodeToString()
            respond("You acquire an item: <b>ash still booze</b>")
        })
        val retrievedIds = mutableListOf<Int>()
        val retrieve = TrackingRetrieveItemService(retrievedIds)
        val db = stubDb()
        val createRequest = ConcoctionCreateRequest(
            retrieveItemService = retrieve,
            craftRequest = CraftRequest(client),
            useItemRequest = UseItemRequest(client),
            gameDatabase = db,
            createItemIngredients = CreateItemIngredients(retrieve, db),
            shopRequest = ShopRequest(client),
        )
        val lib = GameRuntimeLibrary(
            gameDatabase = db,
            concoctionCreateRequest = createRequest,
            retrieveItemService = retrieve,
        )
        val output = outputLib(
            lib,
            """
            boolean ok = create(1, to_item("ash still booze"));
            print(ok);
            """.trimIndent(),
        ).trim()
        assertEquals("true", output)
        assertTrue(shopBodies.single().contains("whichrow=55"))
        assertTrue(92022 in retrievedIds)
    }

    @Test
    fun create_usesCoinmasterForCoinmasterCraftable() {
        registerItem(92031, "ash coin item")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "ash coin item",
                resultQuantity = 1,
                methods = setOf("COINMASTER"),
                ingredients = emptyList(),
            ),
        )
        var bought: Pair<Int, Int>? = null
        val client = HttpClient(MockEngine { respond("ok") })
        val coinmaster = object : CoinmasterManager(
            coinmasterRequest = CoinmasterRequest(client),
            inventoryManager = null,
            gameDatabase = null,
            client = client,
        ) {
            override suspend fun buyItem(itemId: Int, quantity: Int): Int {
                bought = itemId to quantity
                return quantity
            }
        }
        val db = stubDb()
        val lib = GameRuntimeLibrary(
            gameDatabase = db,
            concoctionCreateRequest = ConcoctionCreateRequest(
                retrieveItemService = TrackingRetrieveItemService(mutableListOf()),
                craftRequest = CraftRequest(client),
                useItemRequest = UseItemRequest(client),
                gameDatabase = db,
                coinmasterManager = coinmaster,
            ),
        )
        val output = outputLib(
            lib,
            """
            boolean ok = create(2, to_item("ash coin item"));
            print(ok);
            """.trimIndent(),
        ).trim()
        assertEquals("true", output)
        assertEquals(92031 to 2, bought)
    }

    @Test
    fun create_usesClipArtForClipArtCraftable() {
        registerItem(92041, "ash clip art item")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "ash clip art item",
                resultQuantity = 1,
                methods = setOf("CLIPART"),
                ingredients = emptyList(),
                param = 0x020304,
            ),
        )
        val campgroundBodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            campgroundBodies += req.body.toByteArray().decodeToString()
            respond("You acquire an item: <b>ash clip art item</b>")
        })
        val db = stubDb()
        val lib = GameRuntimeLibrary(
            gameDatabase = db,
            concoctionCreateRequest = ConcoctionCreateRequest(
                retrieveItemService = TrackingRetrieveItemService(mutableListOf()),
                craftRequest = CraftRequest(client),
                useItemRequest = UseItemRequest(client),
                gameDatabase = db,
                clipArtCreateRequest = ClipArtCreateRequest(client),
            ),
        )
        val output = outputLib(
            lib,
            """
            boolean ok = create(1, to_item("ash clip art item"));
            print(ok);
            """.trimIndent(),
        ).trim()
        assertEquals("true", output)
        assertTrue(campgroundBodies.single().contains("clip2=3"))
    }

    @Test
    fun create_usesTerminalForTerminalCraftable() {
        registerItem(9034, "Source essence")
        registerItem(92051, "browser cookie")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "browser cookie",
                resultQuantity = 1,
                methods = setOf("TERMINAL"),
                ingredients = listOf(ConcoctionIngredient("Source essence", 10)),
            ),
        )
        val requestBodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            requestBodies += req.body.toByteArray().decodeToString()
            respond("You acquire an item: <b>browser cookie</b>")
        })
        val db = stubDb()
        val terminalRequest = TerminalRequest(
            client = client,
            campgroundRequest = CampgroundRequest(client),
            falloutShelterRequest = FalloutShelterRequest(client),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("_campgroundHasSourceTerminal", true)
        val lib = GameRuntimeLibrary(
            gameDatabase = db,
            preferences = prefs,
            concoctionCreateRequest = ConcoctionCreateRequest(
                retrieveItemService = TrackingRetrieveItemService(mutableListOf()),
                craftRequest = CraftRequest(client),
                useItemRequest = UseItemRequest(client),
                gameDatabase = db,
                terminalExtrudeCreateRequest = TerminalExtrudeCreateRequest(
                    terminalRequest = terminalRequest,
                    createItemIngredients = CreateItemIngredients(
                        TrackingRetrieveItemService(mutableListOf()),
                        gameDatabase = db,
                    ),
                ),
            ),
        )
        val output = outputLib(
            lib,
            """
            boolean ok = create(1, to_item("browser cookie"));
            print(ok);
            """.trimIndent(),
        ).trim()
        assertEquals("true", output)
        assertTrue(requestBodies.any { it.contains("whichchoice=1191") })
    }

    @Test
    fun create_usesSewerForSewerCraftable() {
        registerItem(23, "chewing gum on a string")
        registerItem(2283, "seal-skull helmet")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "seal-skull helmet",
                resultQuantity = 1,
                methods = setOf("SEWER"),
                ingredients = listOf(ConcoctionIngredient("chewing gum on a string", 1)),
            ),
        )
        val usedItems = mutableListOf<Int>()
        val client = HttpClient(MockEngine { req ->
            usedItems += req.url.parameters["whichitem"]?.toInt() ?: -1
            respond("You acquire an item: <b>seal-skull helmet</b>")
        })
        val inventory = mutableMapOf<Int, Int>()
        val db = stubDb()
        val lib = GameRuntimeLibrary(
            gameDatabase = db,
            concoctionCreateRequest = ConcoctionCreateRequest(
                retrieveItemService = TrackingRetrieveItemService(mutableListOf()),
                craftRequest = CraftRequest(client),
                useItemRequest = UseItemRequest(client),
                gameDatabase = db,
                sewerCreateRequest = SewerCreateRequest(
                    useItemRequest = UseItemRequest(client),
                    closetRequest = ClosetRequest(client),
                    createItemIngredients = CreateItemIngredients(
                        TrackingRetrieveItemService(mutableListOf()),
                        gameDatabase = db,
                    ),
                    gameDatabase = db,
                    inventoryCountById = { id -> inventory[id] ?: 0 },
                ),
            ),
        )
        val output = outputLib(
            lib,
            """
            boolean ok = create(1, to_item("seal-skull helmet"));
            print(ok);
            """.trimIndent(),
        ).trim()
        assertEquals("true", output)
        assertEquals(listOf(23), usedItems)
    }

    @Test
    fun create_usesVykeaForVykeaCraftable() {
        registerItem(92001, "level 1 bookshelf")
        registerItem(8730, "VYKEA instructions")
        registerItem(8729, "VYKEA hex key")
        registerItem(8725, "VYKEA plank")
        registerItem(8726, "VYKEA rail")
        registerItem(8727, "VYKEA bracket")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "level 1 bookshelf",
                resultQuantity = 1,
                methods = setOf("VYKEA"),
                ingredients = listOf(
                    ConcoctionIngredient("VYKEA instructions", 1),
                    ConcoctionIngredient("VYKEA plank", 5),
                    ConcoctionIngredient("VYKEA plank", 5),
                ),
            ),
        )
        var choiceStep = 0
        val choiceSequence = listOf(1121, 1122, 1123)
        val client = HttpClient(MockEngine { request ->
            when {
                request.url.toString().contains("inv_use.php") -> {
                    respond("""<input name="whichchoice" value="1120">""")
                }
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("choice.php") -> {
                    val nextChoice = choiceSequence.getOrElse(choiceStep) { 0 }
                    if (choiceStep < choiceSequence.size) choiceStep++
                    respond("""<input name="whichchoice" value="$nextChoice">""")
                }
                else -> respond("ok")
            }
        })
        val db = stubDb()
        val lib = GameRuntimeLibrary(
            gameDatabase = db,
            preferences = Preferences(MapSettings()),
            concoctionCreateRequest = ConcoctionCreateRequest(
                retrieveItemService = TrackingRetrieveItemService(mutableListOf()),
                craftRequest = CraftRequest(client),
                useItemRequest = UseItemRequest(client),
                gameDatabase = db,
                vykeaCreateRequest = VykeaCreateRequest(
                    useItemRequest = UseItemRequest(client),
                    choiceRequest = ChoiceRequest(client),
                    retrieveItemService = TrackingRetrieveItemService(mutableListOf()),
                    createItemIngredients = CreateItemIngredients(
                        TrackingRetrieveItemService(mutableListOf()),
                        gameDatabase = db,
                    ),
                    vykeaCompanionManager = VykeaCompanionManager(Preferences(MapSettings())),
                    gameDatabase = db,
                ),
            ),
        )
        val output = outputLib(
            lib,
            """
            boolean ok = create(1, to_item("level 1 bookshelf"));
            print(ok);
            """.trimIndent(),
        ).trim()
        assertEquals("true", output)
    }

    @Test
    fun create_usesMuseForMuseCraftable() {
        registerItem(92031, "pottery yo-yo")
        registerItem(92032, "smoked potsherd")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "pottery yo-yo",
                resultQuantity = 1,
                methods = setOf("MUSE"),
                ingredients = listOf(ConcoctionIngredient("smoked potsherd", 5)),
            ),
        )
        val multiUsePosts = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            when {
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("multiuse.php") -> {
                    multiUsePosts += request.body.toByteArray().decodeToString()
                    respond("You acquire an item: <b>pottery yo-yo</b>")
                }
                else -> respond("ok")
            }
        })
        val db = stubDb()
        val lib = GameRuntimeLibrary(
            gameDatabase = db,
            preferences = Preferences(MapSettings()),
            concoctionCreateRequest = ConcoctionCreateRequest(
                retrieveItemService = TrackingRetrieveItemService(mutableListOf()),
                craftRequest = CraftRequest(client),
                useItemRequest = UseItemRequest(client),
                gameDatabase = db,
                museCreateRequest = MuseCreateRequest(
                    useItemRequest = UseItemRequest(client),
                    createItemIngredients = CreateItemIngredients(
                        TrackingRetrieveItemService(mutableListOf()),
                        gameDatabase = db,
                    ),
                    gameDatabase = db,
                ),
            ),
        )
        val output = outputLib(
            lib,
            """
            boolean ok = create(1, to_item("pottery yo-yo"));
            print(ok);
            """.trimIndent(),
        ).trim()
        assertEquals("true", output)
        assertEquals(1, multiUsePosts.size)
    }

    @Test
    fun create_fallsBackToRetrieveForNonAutoCraftable() {
        registerItem(92011, "ash mall item")
        val retrievedIds = mutableListOf<Int>()
        val db = stubDb()
        val retrieve = TrackingRetrieveItemService(retrievedIds)
        val lib = GameRuntimeLibrary(
            gameDatabase = db,
            retrieveItemService = retrieve,
        )
        val output = outputLib(
            lib,
            """
            boolean ok = create(2, to_item("ash mall item"));
            print(ok);
            """.trimIndent(),
        ).trim()
        assertEquals("true", output)
        assertEquals(listOf(92011), retrievedIds)
    }

    private fun stubDb(): GameDatabase = object : GameDatabase() {
        override fun item(id: Int) = ItemDatabase.getById(id)?.let { data ->
            ItemData(
                id = data.id,
                name = data.name,
                descId = data.descId,
                image = data.image,
                primaryUse = data.primaryUse,
                secondaryUses = data.secondaryUses,
                access = data.access,
                autosellPrice = data.autosellPrice,
                plural = data.plural,
            )
        }

        override fun item(name: String) = ItemDatabase.getByName(name)?.let { data ->
            item(data.id)
        }
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
