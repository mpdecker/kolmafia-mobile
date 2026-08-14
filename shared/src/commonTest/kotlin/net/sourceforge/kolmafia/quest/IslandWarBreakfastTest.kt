package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CampgroundRequest
import net.sourceforge.kolmafia.request.ClanLoungeRequest
import net.sourceforge.kolmafia.request.ClanRumpusRequest
import net.sourceforge.kolmafia.request.HermitRequest
import net.sourceforge.kolmafia.request.UseItemRequest
import net.sourceforge.kolmafia.session.BreakfastManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IslandWarBreakfastTest {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    private fun charState(
        ascensionNumber: Int = 5,
        isFistcore: Boolean = false,
        equipment: Map<EquipmentSlot, String> = emptyMap(),
    ): CharacterState {
        val challengePath = if (isFistcore) "Way of the Surprising Fist" else ""
        return CharacterState(
            ascensionNumber = ascensionNumber,
            challengePath = challengePath,
            kingLiberated = false,
            equipment = equipment,
        )
    }

    private fun inventoryWithQuantity(itemId: Int, quantity: Int): InventoryState =
        InventoryState(
            items = mapOf(
                itemId to InventoryItem(
                    itemId = itemId,
                    name = "Item $itemId",
                    quantity = quantity,
                    type = ItemType.OTHER,
                ),
            ),
        )

    private fun manager(
        prefs: Preferences,
        urls: MutableList<String>,
        htmlByPath: Map<String, String> = emptyMap(),
        inventoryManager: InventoryManager? = null,
    ): BreakfastManager {
        val client = HttpClient(MockEngine { request ->
            val path = request.url.encodedPath.removePrefix("/") +
                (request.url.encodedQuery.takeIf { it.isNotEmpty() }?.let { "?$it" } ?: "")
            urls.add(path)
            val body = htmlByPath.entries.firstOrNull { path.contains(it.key) }?.value ?: "ok"
            respond(body, headers = headersOf(HttpHeaders.ContentType, "text/html"))
        })
        return BreakfastManager(
            campgroundRequest = object : CampgroundRequest(client) {
                override suspend fun harvestGarden() = Result.success(Unit)
                override suspend fun useSpinningWheel() = Result.success("ok")
            },
            clanRumpusRequest = object : ClanRumpusRequest(client) {
                override suspend fun visit() = Result.success(Unit)
            },
            clanLoungeRequest = ClanLoungeRequest(client),
            preferences = prefs,
            useItemRequest = UseItemRequest(client),
            hermitRequest = HermitRequest(client),
            httpClient = client,
            inventoryManager = inventoryManager,
        )
    }

    @Test
    fun hippyVisit_whenFilthClearanceMatches_hitsHippyShop() = runBlocking {
        val urls = mutableListOf<String>()
        val p = prefs {
            putInt("lastFilthClearance", 5)
            putBoolean(BreakfastManager.PREF_HIPPY_MEAT_COLLECTED, false)
            putString("warProgress", "unstarted")
        }
        manager(
            prefs = p,
            urls = urls,
            htmlByPath = mapOf("whichshop=hippy" to "Oh, hey, boss!  Welcome back!"),
        ).runBreakfast(charState(ascensionNumber = 5), InventoryState())

        assertTrue(urls.any { it.contains("shop.php") && it.contains("whichshop=hippy") })
        assertTrue(p.getBoolean(BreakfastManager.PREF_HIPPY_MEAT_COLLECTED, false))
        assertTrue(p.getBoolean(Preferences.BIG_ISLAND_VISITED, false))
        assertTrue(urls.none { it.contains("action=farmer") })
        assertTrue(urls.none { it.contains("action=pyro") })
    }

    @Test
    fun hippyVisit_skippedWhenFilthClearanceMismatch() = runBlocking {
        val urls = mutableListOf<String>()
        val p = prefs {
            putInt("lastFilthClearance", 3)
            putString("warProgress", "unstarted")
        }
        manager(prefs = p, urls = urls).runBreakfast(charState(ascensionNumber = 5), InventoryState())
        assertTrue(urls.none { it.contains("whichshop=hippy") })
        assertTrue(p.getBoolean(Preferences.BIG_ISLAND_VISITED, false))
    }

    @Test
    fun hippyVisit_skippedInFistcore() = runBlocking {
        val urls = mutableListOf<String>()
        val p = prefs {
            putInt("lastFilthClearance", 5)
            putString("warProgress", "unstarted")
        }
        manager(prefs = p, urls = urls)
            .runBreakfast(charState(ascensionNumber = 5, isFistcore = true), InventoryState())
        assertTrue(urls.none { it.contains("whichshop=hippy") })
    }

    @Test
    fun warUnstarted_skipsFarmerAndPyro() = runBlocking {
        val urls = mutableListOf<String>()
        val p = prefs {
            putString("warProgress", "unstarted")
            putString("sidequestFarmCompleted", "hippy")
        }
        manager(prefs = p, urls = urls).runBreakfast(charState(), InventoryState())
        assertTrue(urls.none { it.contains("action=farmer") })
        assertTrue(urls.none { it.contains("action=pyro") })
        assertTrue(p.getBoolean(Preferences.BIG_ISLAND_VISITED, false))
    }

    @Test
    fun farmCompleted_visitsFarmerAndSetsCollected() = runBlocking {
        val urls = mutableListOf<String>()
        val p = prefs {
            putString("warProgress", "started")
            putString("sidequestFarmCompleted", "hippy")
            putBoolean(IslandWarActionResponseSync.PREF_FARMER_ITEMS_COLLECTED, false)
            putString("sidequestLighthouseCompleted", "none")
        }
        // Without outfitManager.hasOutfit, farmOutfit stays null — force visit by mocking
        // via wearing nothing but still need outfits available. Inject stub outfitManager.
        val outfitManager = object : net.sourceforge.kolmafia.equipment.OutfitManager(
            retrieveItemService = null,
            equipmentRequest = net.sourceforge.kolmafia.request.EquipmentRequest(
                HttpClient(MockEngine { respond("ok") }),
            ),
            customOutfitRequest = net.sourceforge.kolmafia.request.CustomOutfitRequest(
                HttpClient(MockEngine { respond("[]") }),
            ),
            character = net.sourceforge.kolmafia.character.KoLCharacter(),
            gameDatabase = net.sourceforge.kolmafia.data.GameDatabase(),
            closetRequest = null,
            storageRequest = null,
            displayCaseRequest = null,
            clanStashRequest = null,
            inventoryManager = null,
        ) {
            override suspend fun hasOutfit(outfitId: Int): Boolean = true
            override suspend fun wearOutfit(
                name: String,
                postWear: ((String) -> Unit)?,
            ): Boolean = true
        }

        val client = HttpClient(MockEngine { request ->
            val path = request.url.encodedPath.removePrefix("/") +
                (request.url.encodedQuery.takeIf { it.isNotEmpty() }?.let { "?$it" } ?: "")
            urls.add(path)
            val body = if (path.contains("action=farmer")) {
                "Ach, here ye are, laddie."
            } else {
                "ok"
            }
            respond(body, headers = headersOf(HttpHeaders.ContentType, "text/html"))
        })

        BreakfastManager(
            campgroundRequest = object : CampgroundRequest(client) {
                override suspend fun harvestGarden() = Result.success(Unit)
                override suspend fun useSpinningWheel() = Result.success("ok")
            },
            clanRumpusRequest = object : ClanRumpusRequest(client) {
                override suspend fun visit() = Result.success(Unit)
            },
            clanLoungeRequest = ClanLoungeRequest(client),
            preferences = p,
            useItemRequest = UseItemRequest(client),
            hermitRequest = HermitRequest(client),
            httpClient = client,
            outfitManager = outfitManager,
        ).runBreakfast(charState(), InventoryState())

        assertTrue(urls.any { it.contains("action=farmer") })
        assertTrue(p.getBoolean(IslandWarActionResponseSync.PREF_FARMER_ITEMS_COLLECTED, false))
        assertTrue(p.getBoolean(Preferences.BIG_ISLAND_VISITED, false))
    }

    @Test
    fun lighthouseCompleted_withGunpowder_visitsPyroAndConsumes() = runBlocking {
        val urls = mutableListOf<String>()
        val consumed = mutableListOf<Pair<Int, Int>>()
        val p = prefs {
            putString("warProgress", "started")
            putString("sidequestLighthouseCompleted", "fratboy")
            putString("sidequestFarmCompleted", "none")
            putBoolean(IslandWarActionResponseSync.PREF_FARMER_ITEMS_COLLECTED, true)
        }

        val inventory = inventoryWithQuantity(BreakfastManager.GUNPOWDER_ID, 3)

        val outfitManager = object : net.sourceforge.kolmafia.equipment.OutfitManager(
            retrieveItemService = null,
            equipmentRequest = net.sourceforge.kolmafia.request.EquipmentRequest(
                HttpClient(MockEngine { respond("ok") }),
            ),
            customOutfitRequest = net.sourceforge.kolmafia.request.CustomOutfitRequest(
                HttpClient(MockEngine { respond("[]") }),
            ),
            character = net.sourceforge.kolmafia.character.KoLCharacter(),
            gameDatabase = net.sourceforge.kolmafia.data.GameDatabase(),
            closetRequest = null,
            storageRequest = null,
            displayCaseRequest = null,
            clanStashRequest = null,
            inventoryManager = null,
        ) {
            override suspend fun hasOutfit(outfitId: Int): Boolean = true
            override suspend fun wearOutfit(
                name: String,
                postWear: ((String) -> Unit)?,
            ): Boolean = true
        }

        val client = HttpClient(MockEngine { request ->
            val path = request.url.encodedPath.removePrefix("/") +
                (request.url.encodedQuery.takeIf { it.isNotEmpty() }?.let { "?$it" } ?: "")
            urls.add(path)
            val body = if (path.contains("action=pyro")) {
                "The Lighthouse Keeper's eyes light up as he sees your gunpowder."
            } else {
                "ok"
            }
            respond(body, headers = headersOf(HttpHeaders.ContentType, "text/html"))
        })

        val countingInv = object : InventoryManager(
            client = HttpClient(MockEngine { respond("ok") }),
            eventBus = net.sourceforge.kolmafia.event.GameEventBus(),
            characterRequest = null,
            character = null,
            preferences = p,
        ) {
            override val state: kotlinx.coroutines.flow.StateFlow<InventoryState> =
                kotlinx.coroutines.flow.MutableStateFlow(inventory)
            override fun consumeItemLocally(itemId: Int, quantity: Int) {
                consumed.add(itemId to quantity)
            }
        }

        BreakfastManager(
            campgroundRequest = object : CampgroundRequest(client) {
                override suspend fun harvestGarden() = Result.success(Unit)
                override suspend fun useSpinningWheel() = Result.success("ok")
            },
            clanRumpusRequest = object : ClanRumpusRequest(client) {
                override suspend fun visit() = Result.success(Unit)
            },
            clanLoungeRequest = ClanLoungeRequest(client),
            preferences = p,
            useItemRequest = UseItemRequest(client),
            hermitRequest = HermitRequest(client),
            httpClient = client,
            outfitManager = outfitManager,
            inventoryManager = countingInv,
        ).runBreakfast(charState(), inventory)

        assertTrue(urls.any { it.contains("action=pyro") })
        assertEquals(listOf(BreakfastManager.GUNPOWDER_ID to 3), consumed)
        assertTrue(p.getBoolean(Preferences.BIG_ISLAND_VISITED, false))
    }

    @Test
    fun alreadyVisited_noHttp() = runBlocking {
        val urls = mutableListOf<String>()
        val p = prefs {
            putBoolean(Preferences.BIG_ISLAND_VISITED, true)
            putInt("lastFilthClearance", 5)
            putString("warProgress", "started")
            putString("sidequestFarmCompleted", "hippy")
        }
        manager(prefs = p, urls = urls).runBreakfast(charState(ascensionNumber = 5), InventoryState())
        assertTrue(urls.none { it.contains("whichshop=hippy") })
        assertTrue(urls.none { it.contains("action=farmer") })
        assertTrue(urls.none { it.contains("bigisland.php") })
    }

    @Test
    fun gunpowderCount_readsInventoryQuantity() {
        assertEquals(0, BreakfastManager.gunpowderCount(InventoryState()))
        assertEquals(4, BreakfastManager.gunpowderCount(inventoryWithQuantity(2403, 4)))
    }
}
