# Phase 16: Mall Buy & Retrieve Item — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement `buy()` + `retrieve_item()` ASH functions backed by a full compound acquisition chain (inventory → closet → Hagnk's storage → NPC store → mall) plus `mall_price()` live lookup.

**Architecture:** Six layers built bottom-up — NpcStoreDatabase item index, NpcBuyRequest HTTP wrapper, MallManager orchestrator (wraps existing `MallSearchRequest` + `MallPurchaseRequest`), RetrieveItemService compound chain, GameRuntimeLibrary.Mall.kt ASH bridge, and DI wiring. Each layer is independently testable.

**Tech Stack:** Kotlin Multiplatform (commonMain), Ktor HttpClient + MockEngine (tests), Koin DI, existing `MallSearchRequest` / `MallPurchaseRequest` / `ClosetRequest` / `StorageRequest`.

---

## File Structure

**Create:**
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/npc/NpcBuyRequest.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mall/MallManager.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/item/RetrieveItemService.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Mall.kt`
- `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/npc/NpcBuyRequestTest.kt`
- `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/mall/MallManagerTest.kt`
- `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/item/RetrieveItemServiceTest.kt`
- `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryMallTest.kt`

**Modify:**
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/NpcStoreDatabase.kt` — add `_byItemName` index + `storeForItem()`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/GameDatabase.kt` — add `open fun npcStoreFor()`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt` — add `mallManager` + `retrieveItemService` params, imports, `registerMallFunctions` call
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Pricing.kt` — add `mall_price` live regFn
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt` — register NpcBuyRequest, MallManager, RetrieveItemService + update GRL block
- `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/data/NpcStoreDatabaseTest.kt` — add `storeForItem` tests

---

### Task 1: NpcStoreDatabase item index + GameDatabase.npcStoreFor()

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/NpcStoreDatabase.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/GameDatabase.kt`
- Modify: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/data/NpcStoreDatabaseTest.kt`

The existing `NpcStoreDatabaseTest` has this `sampleText` with two stores: `guildstore1` (Shadowy Store, items: "ye olde golde frontes" 1500, "pretentious paintbrush" 1000) and `generalstore` (General Store, items: "a beret" 500, "BORT license plate" 99).

- [ ] **Step 1: Write the failing tests in NpcStoreDatabaseTest.kt**

Add these four tests inside the existing `NpcStoreDatabaseTest` class, after the last `@Test` method:

```kotlin
@Test
fun `storeForItem returns the store that sells the item`() {
    NpcStoreDatabase.loadFromText(sampleText)
    val store = NpcStoreDatabase.storeForItem("ye olde golde frontes")
    assertNotNull(store)
    assertEquals("guildstore1", store!!.storeKey)
}

@Test
fun `storeForItem is case-insensitive`() {
    NpcStoreDatabase.loadFromText(sampleText)
    val store = NpcStoreDatabase.storeForItem("YE OLDE GOLDE FRONTES")
    assertNotNull(store)
    assertEquals("guildstore1", store!!.storeKey)
}

@Test
fun `storeForItem returns null for unknown item`() {
    NpcStoreDatabase.loadFromText(sampleText)
    assertNull(NpcStoreDatabase.storeForItem("imaginary widget"))
}

@Test
fun `storeForItem returns null after resetForTest`() {
    NpcStoreDatabase.loadFromText(sampleText)
    NpcStoreDatabase.resetForTest()
    assertNull(NpcStoreDatabase.storeForItem("ye olde golde frontes"))
}
```

- [ ] **Step 2: Run the test to verify it fails**

```
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.data.NpcStoreDatabaseTest" -q
```

Expected: FAIL with `Unresolved reference: storeForItem`

- [ ] **Step 3: Add `_byItemName` to NpcStoreDatabase.kt**

File: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/NpcStoreDatabase.kt`

Add the field after `_itemPrices`:

```kotlin
private val _byItemName = mutableMapOf<String, NpcStoreData>()
```

In `loadFromText()`, add the index population **after** the `storeItems.forEach` block and **before** `loaded = true`:

```kotlin
// Build item-name → store index after all stores are fully populated
_byKey.values.forEach { store ->
    store.items.forEach { item ->
        _byItemName.putIfAbsent(item.itemName.lowercase(), store)
    }
}

loaded = true
```

In `resetForTest()`, add `_byItemName.clear()`:

```kotlin
internal fun resetForTest() {
    _byKey.clear()
    _byName.clear()
    _itemPrices.clear()
    _byItemName.clear()
    loaded = false
}
```

Add the accessor function after `npcPrice()`:

```kotlin
fun storeForItem(itemName: String): NpcStoreData? = _byItemName[itemName.lowercase()]
```

- [ ] **Step 4: Add `npcStoreFor()` to GameDatabase.kt**

File: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/GameDatabase.kt`

Add after the existing `open fun npcPrice(itemName: String): Int` line:

```kotlin
open fun npcStoreFor(itemName: String): NpcStoreData? = NpcStoreDatabase.storeForItem(itemName)
```

- [ ] **Step 5: Run tests to verify they pass**

```
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.data.NpcStoreDatabaseTest" -q
```

Expected: All tests PASS (including the 4 new + 7 existing = 11 total)

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/NpcStoreDatabase.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/GameDatabase.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/data/NpcStoreDatabaseTest.kt
git commit -m "feat: NpcStoreDatabase.storeForItem() item→store index + GameDatabase.npcStoreFor()"
```

---

### Task 2: NpcBuyRequest

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/npc/NpcBuyRequest.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/npc/NpcBuyRequestTest.kt`

NPC store purchases POST to `store.php` with `whichstore`, `buying=1`, `whichitem`, `howmany`, `ajax=1`. A successful purchase returns the page HTML; "You can't afford" in the response means insufficient meat; "That store doesn't" means item not sold there. Both return 0.

- [ ] **Step 1: Write the failing test**

Create `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/npc/NpcBuyRequestTest.kt`:

```kotlin
package net.sourceforge.kolmafia.npc

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class NpcBuyRequestTest {

    @Test
    fun buy_sendsCorrectFormFields() = runTest {
        val captured = mutableListOf<String>()
        val engine = MockEngine { request ->
            captured += request.body.toByteArray().decodeToString()
            respond("<html>You acquire 1 pretentious paintbrush.</html>", HttpStatusCode.OK)
        }
        NpcBuyRequest(HttpClient(engine)).buy(storeKey = "guildstore1", itemId = 456, quantity = 2)
        val body = captured[0]
        assertTrue(body.contains("whichstore=guildstore1"), "body: $body")
        assertTrue(body.contains("buying=1"), "body: $body")
        assertTrue(body.contains("whichitem=456"), "body: $body")
        assertTrue(body.contains("howmany=2"), "body: $body")
        assertTrue(body.contains("ajax=1"), "body: $body")
    }

    @Test
    fun buy_success_returnsQuantity() = runTest {
        val engine = MockEngine { respond("<html>You acquire the item.</html>", HttpStatusCode.OK) }
        val result = NpcBuyRequest(HttpClient(engine)).buy("guildstore1", 456, 3)
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrThrow())
    }

    @Test
    fun buy_cantAfford_returnsZero() = runTest {
        val engine = MockEngine {
            respond("<html>You can't afford that item.</html>", HttpStatusCode.OK)
        }
        val result = NpcBuyRequest(HttpClient(engine)).buy("guildstore1", 456, 1)
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow())
    }

    @Test
    fun buy_storeDoesNotCarryItem_returnsZero() = runTest {
        val engine = MockEngine {
            respond("<html>That store doesn't carry that item.</html>", HttpStatusCode.OK)
        }
        val result = NpcBuyRequest(HttpClient(engine)).buy("nosuchstore", 456, 1)
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow())
    }

    @Test
    fun buy_networkError_returnsFailure() = runTest {
        val engine = MockEngine { throw Exception("connection refused") }
        val result = NpcBuyRequest(HttpClient(engine)).buy("guildstore1", 456, 1)
        assertTrue(result.isFailure)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.npc.NpcBuyRequestTest" -q
```

Expected: FAIL with compilation error (class not found)

- [ ] **Step 3: Create NpcBuyRequest.kt**

Create `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/npc/NpcBuyRequest.kt`:

```kotlin
package net.sourceforge.kolmafia.npc

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.sourceforge.kolmafia.http.KOL_BASE_URL

open class NpcBuyRequest(private val client: HttpClient) {

    open suspend fun buy(storeKey: String, itemId: Int, quantity: Int): Result<Int> = runCatching {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/store.php",
            formParameters = parameters {
                append("whichstore", storeKey)
                append("buying", "1")
                append("whichitem", itemId.toString())
                append("howmany", quantity.toString())
                append("ajax", "1")
            }
        )
        val body = response.bodyAsText()
        if (body.contains("You can't afford") || body.contains("That store doesn't")) {
            return@runCatching 0
        }
        quantity
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.npc.NpcBuyRequestTest" -q
```

Expected: All 5 tests PASS

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/npc/NpcBuyRequest.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/npc/NpcBuyRequestTest.kt
git commit -m "feat: NpcBuyRequest — POST to store.php for NPC item purchases"
```

---

### Task 3: MallManager

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mall/MallManager.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/mall/MallManagerTest.kt`

`MallManager` wraps the **existing** `MallSearchRequest` + `MallPurchaseRequest` (both already in `SharedModule`). It searches for listings, filters by `maxPrice`, sorts cheapest-first, and buys from each offer until the requested count is filled. All-or-nothing per offer: if `MallPurchaseRequest.buy()` succeeds, assume the full `qty` was purchased.

`MallSearchRequest.search()` parses HTML in this format (from `MallSearchRequestTest`):
```
<a href="mallstore.php?whichstore=SHOPID">Shop Name</a>
<input type="hidden" name="whichitem" value="ITEMID">
<b>PRICE</b> Meat<br>
Quantity: QTY<br>
```

- [ ] **Step 1: Write the failing test**

Create `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/mall/MallManagerTest.kt`:

```kotlin
package net.sourceforge.kolmafia.mall

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import kotlin.test.*

// Two offers: shopId=1 price=100 qty=5, shopId=2 price=200 qty=5 (KoL mall HTML format)
private val SEARCH_HTML = """
    <html><body>
    <a href="mallstore.php?whichstore=1">Shop One</a>
    <input type="hidden" name="whichitem" value="42">
    <b>100</b> Meat<br>
    Quantity: 5<br>
    <a href="mallstore.php?whichstore=2">Shop Two</a>
    <input type="hidden" name="whichitem" value="42">
    <b>200</b> Meat<br>
    Quantity: 5<br>
    </body></html>
""".trimIndent()

private val BUY_HTML = "<html>You acquire: test widget</html>"

private fun stubDb(): GameDatabase = object : GameDatabase() {
    override fun item(id: Int) = if (id == 42) ItemData(
        id = 42, name = "test widget", descId = "", image = "",
        primaryUse = ItemPrimaryUse.NONE, secondaryUses = emptySet(),
        access = setOf('t', 'd'), autosellPrice = 10, plural = null
    ) else null
    override fun item(name: String) = if (name == "test widget") item(42) else null
}

private fun buildEngine(searchHtml: String = SEARCH_HTML, buyHtml: String = BUY_HTML): MockEngine {
    return MockEngine { request ->
        val body = request.body.toByteArray().decodeToString()
        if (body.contains("searchmall")) respond(searchHtml, HttpStatusCode.OK)
        else respond(buyHtml, HttpStatusCode.OK)
    }
}

class MallManagerTest {

    @Test
    fun buy_purchasesRequestedCount() = runTest {
        val engine = buildEngine()
        val client = HttpClient(engine)
        val manager = MallManager(MallSearchRequest(client), MallPurchaseRequest(client), stubDb())
        assertEquals(3, manager.buy(itemId = 42, count = 3))
    }

    @Test
    fun buy_respectsMaxPrice_skipsExpensiveOffers() = runTest {
        val engine = buildEngine()
        val client = HttpClient(engine)
        val manager = MallManager(MallSearchRequest(client), MallPurchaseRequest(client), stubDb())
        // maxPrice=150 → only the 100-meat shop qualifies (qty=5), so 3 can be filled
        assertEquals(3, manager.buy(itemId = 42, count = 3, maxPrice = 150))
    }

    @Test
    fun buy_noListingsForItem_returnsZero() = runTest {
        val engine = buildEngine(searchHtml = "<html><body>No results.</body></html>")
        val client = HttpClient(engine)
        val manager = MallManager(MallSearchRequest(client), MallPurchaseRequest(client), stubDb())
        assertEquals(0, manager.buy(itemId = 42, count = 1))
    }

    @Test
    fun buy_unknownItemId_returnsZero() = runTest {
        val engine = buildEngine()
        val client = HttpClient(engine)
        val manager = MallManager(MallSearchRequest(client), MallPurchaseRequest(client), stubDb())
        // itemId 999 not in stubDb → no item name resolved → early return 0
        assertEquals(0, manager.buy(itemId = 999, count = 1))
    }

    @Test
    fun cheapestPrice_returnsLowestListedPrice() = runTest {
        val engine = buildEngine()
        val client = HttpClient(engine)
        val manager = MallManager(MallSearchRequest(client), MallPurchaseRequest(client), stubDb())
        assertEquals(100L, manager.cheapestPrice("test widget"))
    }

    @Test
    fun cheapestPrice_noListings_returnsMinusOne() = runTest {
        val engine = buildEngine(searchHtml = "<html><body>No results.</body></html>")
        val client = HttpClient(engine)
        val manager = MallManager(MallSearchRequest(client), MallPurchaseRequest(client), stubDb())
        assertEquals(-1L, manager.cheapestPrice("test widget"))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.mall.MallManagerTest" -q
```

Expected: FAIL with `Unresolved reference: MallManager`

- [ ] **Step 3: Create MallManager.kt**

Create `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mall/MallManager.kt`:

```kotlin
package net.sourceforge.kolmafia.mall

import net.sourceforge.kolmafia.data.GameDatabase

open class MallManager(
    private val searchRequest: MallSearchRequest,
    private val purchaseRequest: MallPurchaseRequest,
    private val gameDatabase: GameDatabase?
) {
    open suspend fun buy(itemId: Int, count: Int, maxPrice: Int = Int.MAX_VALUE): Int {
        val itemName = gameDatabase?.item(itemId)?.name ?: return 0
        val offers = searchRequest.search(itemName, limit = 50)
            .filter { it.price <= maxPrice && it.quantity > 0 }
            .sortedBy { it.price }
        var remaining = count
        for (offer in offers) {
            if (remaining <= 0) break
            val qty = minOf(remaining, offer.quantity)
            val result = purchaseRequest.buy(
                shopId = offer.shopId,
                itemId = itemId,
                quantity = qty,
                price = offer.price
            )
            if (result.isSuccess) remaining -= qty
        }
        return count - remaining
    }

    open suspend fun cheapestPrice(itemName: String): Long =
        searchRequest.search(itemName, limit = 5).minOfOrNull { it.price } ?: -1L
}
```

- [ ] **Step 4: Run tests to verify they pass**

```
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.mall.MallManagerTest" -q
```

Expected: All 6 tests PASS

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mall/MallManager.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/mall/MallManagerTest.kt
git commit -m "feat: MallManager — buy() + cheapestPrice() using existing MallSearchRequest/MallPurchaseRequest"
```

---

### Task 4: RetrieveItemService

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/item/RetrieveItemService.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/item/RetrieveItemServiceTest.kt`

`RetrieveItemService.retrieve(itemId, qty)` tries sources in order:
1. **Inventory**: count already-held items; if enough, return immediately.
2. **Closet** (`ClosetRequest.takeOut`): if success, treat as fully filled.
3. **Hagnk's storage** (`StorageRequest.withdraw`): if success, treat as fully filled.
4. **NPC store** (`NpcBuyRequest.buy`): look up store via `GameDatabase.npcStoreFor()`, subtract returned count.
5. **Mall** (`MallManager.buy`): buy remaining count, subtract.

Returns actual total retrieved (may be less than `qty` if sources ran dry). `InventoryState.items` is keyed by `itemId: Int`. `InventoryItem` has field `itemId: Int` (not `id`).

- [ ] **Step 1: Write the failing test**

Create `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/item/RetrieveItemServiceTest.kt`:

```kotlin
package net.sourceforge.kolmafia.item

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.NpcStoreData
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.mall.MallPurchaseRequest
import net.sourceforge.kolmafia.mall.MallSearchRequest
import net.sourceforge.kolmafia.npc.NpcBuyRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.StorageRequest
import kotlin.test.*

// --- Shared test helpers ---

private const val ITEM_ID = 42
private const val ITEM_NAME = "test widget"

private fun testItemData() = ItemData(
    id = ITEM_ID, name = ITEM_NAME, descId = "", image = "",
    primaryUse = ItemPrimaryUse.NONE, secondaryUses = emptySet(),
    access = setOf('t', 'd'), autosellPrice = 10, plural = null
)

private fun dbWithNpc(npcStore: NpcStoreData? = NpcStoreData("generalstore", "General Store", "NPC")) =
    object : GameDatabase() {
        override fun item(id: Int) = if (id == ITEM_ID) testItemData() else null
        override fun item(name: String) = if (name == ITEM_NAME) testItemData() else null
        override fun npcStoreFor(itemName: String): NpcStoreData? =
            if (itemName == ITEM_NAME) npcStore else null
    }

private fun dbNoNpc() = dbWithNpc(npcStore = null)

private fun fakeInventory(qty: Int): InventoryManager {
    val items = if (qty > 0) mapOf(ITEM_ID to InventoryItem(ITEM_ID, ITEM_NAME, qty, ItemType.OTHER))
    else emptyMap()
    return object : InventoryManager(HttpClient(MockEngine { respond("") }), GameEventBus()) {
        private val _s = MutableStateFlow(InventoryState(items = items))
        override val state: StateFlow<InventoryState> = _s
    }
}

private fun closetSucceeds() = object : ClosetRequest(HttpClient(MockEngine { respond("") })) {
    override suspend fun takeOut(itemId: Int, quantity: Int) = Result.success("ok")
}

private fun closetFails() = object : ClosetRequest(HttpClient(MockEngine { respond("") })) {
    override suspend fun takeOut(itemId: Int, quantity: Int) = Result.failure<String>(Exception("empty"))
}

private fun storageSucceeds() = StorageRequest(
    HttpClient(MockEngine { respond("<ok>", HttpStatusCode.OK) })
)

private fun storageFails() = StorageRequest(
    HttpClient(MockEngine { throw Exception("empty") })
)

private fun npcSucceeds(qty: Int) = object : NpcBuyRequest(HttpClient(MockEngine { respond("") })) {
    override suspend fun buy(storeKey: String, itemId: Int, quantity: Int) = Result.success(qty)
}

private fun npcFails() = object : NpcBuyRequest(HttpClient(MockEngine { respond("") })) {
    override suspend fun buy(storeKey: String, itemId: Int, quantity: Int) = Result.success(0)
}

private fun mallSucceeds(qty: Int): MallManager {
    val dummyClient = HttpClient(MockEngine { respond("") })
    return object : MallManager(MallSearchRequest(dummyClient), MallPurchaseRequest(dummyClient), null) {
        override suspend fun buy(itemId: Int, count: Int, maxPrice: Int) = qty
    }
}

// --- Tests ---

class RetrieveItemServiceTest {

    @Test
    fun retrieve_itemAlreadyInInventory_returnsQtyWithoutFetching() = runTest {
        val service = RetrieveItemService(
            inventoryManager = fakeInventory(qty = 5),
            closetRequest = null,
            storageRequest = null,
            npcBuyRequest = null,
            mallManager = null,
            gameDatabase = dbNoNpc()
        )
        assertEquals(3, service.retrieve(ITEM_ID, 3))
    }

    @Test
    fun retrieve_takesFromCloset_whenInventoryShort() = runTest {
        val service = RetrieveItemService(
            inventoryManager = fakeInventory(qty = 0),
            closetRequest = closetSucceeds(),
            storageRequest = null,
            npcBuyRequest = null,
            mallManager = null,
            gameDatabase = dbNoNpc()
        )
        assertEquals(2, service.retrieve(ITEM_ID, 2))
    }

    @Test
    fun retrieve_pullsFromStorage_whenClosetFails() = runTest {
        val service = RetrieveItemService(
            inventoryManager = fakeInventory(qty = 0),
            closetRequest = closetFails(),
            storageRequest = storageSucceeds(),
            npcBuyRequest = null,
            mallManager = null,
            gameDatabase = dbNoNpc()
        )
        assertEquals(2, service.retrieve(ITEM_ID, 2))
    }

    @Test
    fun retrieve_buysFromNpc_whenStorageFails() = runTest {
        val service = RetrieveItemService(
            inventoryManager = fakeInventory(qty = 0),
            closetRequest = closetFails(),
            storageRequest = storageFails(),
            npcBuyRequest = npcSucceeds(2),
            mallManager = null,
            gameDatabase = dbWithNpc()
        )
        assertEquals(2, service.retrieve(ITEM_ID, 2))
    }

    @Test
    fun retrieve_skipsNpc_whenNoNpcStoreForItem() = runTest {
        var npcCalled = false
        val fakeNpc = object : NpcBuyRequest(HttpClient(MockEngine { respond("") })) {
            override suspend fun buy(storeKey: String, itemId: Int, quantity: Int): Result<Int> {
                npcCalled = true
                return Result.success(quantity)
            }
        }
        val service = RetrieveItemService(
            inventoryManager = fakeInventory(qty = 0),
            closetRequest = closetFails(),
            storageRequest = storageFails(),
            npcBuyRequest = fakeNpc,
            mallManager = mallSucceeds(1),
            gameDatabase = dbNoNpc()   // no NPC store → NpcBuyRequest should NOT be called
        )
        service.retrieve(ITEM_ID, 1)
        assertFalse(npcCalled, "NpcBuyRequest.buy() must not be called when no NPC store sells the item")
    }

    @Test
    fun retrieve_buysFromMall_whenNpcFails() = runTest {
        val service = RetrieveItemService(
            inventoryManager = fakeInventory(qty = 0),
            closetRequest = closetFails(),
            storageRequest = storageFails(),
            npcBuyRequest = npcFails(),
            mallManager = mallSucceeds(3),
            gameDatabase = dbWithNpc()
        )
        assertEquals(3, service.retrieve(ITEM_ID, 3))
    }

    @Test
    fun retrieve_unknownItemId_returnsZero() = runTest {
        val service = RetrieveItemService(
            inventoryManager = fakeInventory(qty = 0),
            closetRequest = null,
            storageRequest = null,
            npcBuyRequest = null,
            mallManager = null,
            gameDatabase = dbNoNpc()
        )
        // itemId 999 not in database → cannot resolve item name → 0
        assertEquals(0, service.retrieve(999, 2))
    }

    @Test
    fun retrieve_allSourcesNull_returnsZero() = runTest {
        val service = RetrieveItemService(
            inventoryManager = null,
            closetRequest = null,
            storageRequest = null,
            npcBuyRequest = null,
            mallManager = null,
            gameDatabase = dbNoNpc()
        )
        assertEquals(0, service.retrieve(ITEM_ID, 1))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.item.RetrieveItemServiceTest" -q
```

Expected: FAIL with `Unresolved reference: RetrieveItemService`

- [ ] **Step 3: Create RetrieveItemService.kt**

Create `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/item/RetrieveItemService.kt`:

```kotlin
package net.sourceforge.kolmafia.item

import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.npc.NpcBuyRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.StorageRequest

open class RetrieveItemService(
    private val inventoryManager: InventoryManager?,
    private val closetRequest: ClosetRequest?,
    private val storageRequest: StorageRequest?,
    private val npcBuyRequest: NpcBuyRequest?,
    private val mallManager: MallManager?,
    private val gameDatabase: GameDatabase?
) {
    open suspend fun retrieve(itemId: Int, qty: Int): Int {
        val itemName = gameDatabase?.item(itemId)?.name ?: return 0
        var remaining = qty - inventoryCount(itemId)
        if (remaining <= 0) return qty

        if (remaining > 0 && closetRequest != null) {
            val result = closetRequest.takeOut(itemId, remaining)
            if (result.isSuccess) remaining = 0
        }

        if (remaining > 0 && storageRequest != null) {
            val result = storageRequest.withdraw(itemId, remaining)
            if (result.isSuccess) remaining = 0
        }

        if (remaining > 0 && npcBuyRequest != null) {
            val npcStore = gameDatabase?.npcStoreFor(itemName)
            if (npcStore != null) {
                val bought = npcBuyRequest.buy(npcStore.storeKey, itemId, remaining).getOrDefault(0)
                remaining -= bought
            }
        }

        if (remaining > 0 && mallManager != null) {
            remaining -= mallManager.buy(itemId, remaining)
        }

        return qty - remaining
    }

    private fun inventoryCount(itemId: Int): Int =
        inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0
}
```

- [ ] **Step 4: Run tests to verify they pass**

```
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.item.RetrieveItemServiceTest" -q
```

Expected: All 8 tests PASS

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/item/RetrieveItemService.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/item/RetrieveItemServiceTest.kt
git commit -m "feat: RetrieveItemService — compound retrieve chain (inventory/closet/storage/NPC/mall)"
```

---

### Task 5: ASH layer — GameRuntimeLibrary.Mall.kt + mall_price live + GRL constructor

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Mall.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Pricing.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryMallTest.kt`

ASH signatures (all use KoLmafia-standard arg order: count first, item second):
- `buy(count: int, it: item) → int` — mall buy, no price cap
- `buy(count: int, it: item, maxPrice: int) → int` — mall buy with price cap
- `retrieve_item(count: int, it: item) → boolean` — compound retrieve
- `retrieve_item(count: int, it: item, retrieve: boolean) → boolean` — same (mobile ignores `retrieve` flag)
- `mall_price(it: item) → int` — cheapest listed price (live search)

In tests, use `outputLib()` from `GameRuntimeLibraryTestHelpers.kt` (already on the test classpath in the `ash` package) to run ASH source against a `GameRuntimeLibrary` instance.

- [ ] **Step 1: Write the failing tests**

Create `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryMallTest.kt`:

```kotlin
package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.mall.MallPurchaseRequest
import net.sourceforge.kolmafia.mall.MallSearchRequest
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlin.test.*

private const val TEST_ITEM = "test widget"
private const val TEST_ITEM_ID = 42

private fun stubDb(): GameDatabase = object : GameDatabase() {
    override fun item(id: Int) = if (id == TEST_ITEM_ID) ItemData(
        id = TEST_ITEM_ID, name = TEST_ITEM, descId = "", image = "",
        primaryUse = ItemPrimaryUse.NONE, secondaryUses = emptySet(),
        access = setOf('t', 'd'), autosellPrice = 10, plural = null
    ) else null
    override fun item(name: String) = if (name == TEST_ITEM) item(TEST_ITEM_ID) else null
}

private fun mallThatBuys(qty: Int): MallManager {
    val dummyClient = HttpClient(MockEngine { respond("") })
    return object : MallManager(MallSearchRequest(dummyClient), MallPurchaseRequest(dummyClient), null) {
        override suspend fun buy(itemId: Int, count: Int, maxPrice: Int) = qty
        override suspend fun cheapestPrice(itemName: String) = if (itemName == TEST_ITEM) 500L else -1L
    }
}

// Returns a service that always reports full success (returns whatever qty was requested).
private fun retrieveAlwaysSucceeds(): RetrieveItemService =
    object : RetrieveItemService(null, null, null, null, null, null) {
        override suspend fun retrieve(itemId: Int, qty: Int) = qty
    }

class GameRuntimeLibraryMallTest {

    @Test
    fun buy_callsMallManagerAndReturnsPurchasedCount() {
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            mallManager = mallThatBuys(2)
        )
        assertEquals("2", outputLib(lib, """print(to_string(buy(2, to_item("$TEST_ITEM"))));"""))
    }

    @Test
    fun buy_withMaxPrice_passesCapToMallManager() {
        val db = stubDb()
        var capturedMax = Int.MAX_VALUE
        val dummyClient = HttpClient(MockEngine { respond("") })
        val mall = object : MallManager(MallSearchRequest(dummyClient), MallPurchaseRequest(dummyClient), null) {
            override suspend fun buy(itemId: Int, count: Int, maxPrice: Int): Int {
                capturedMax = maxPrice
                return count
            }
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, mallManager = mall)
        outputLib(lib, """buy(1, to_item("$TEST_ITEM"), 1000);""")
        assertEquals(1000, capturedMax)
    }

    @Test
    fun buy_unknownItem_returnsZero() {
        val lib = GameRuntimeLibrary(gameDatabase = stubDb(), mallManager = mallThatBuys(1))
        assertEquals("0", outputLib(lib, """print(to_string(buy(1, to_item("unknown item xyz"))));"""))
    }

    @Test
    fun buy_nullMallManager_returnsZero() {
        val lib = GameRuntimeLibrary(gameDatabase = stubDb(), mallManager = null)
        assertEquals("0", outputLib(lib, """print(to_string(buy(1, to_item("$TEST_ITEM"))));"""))
    }

    @Test
    fun retrieveItem_successReturnsTrue() {
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            retrieveItemService = retrieveAlwaysSucceeds()
        )
        assertEquals("true", outputLib(lib, """print(to_string(retrieve_item(3, to_item("$TEST_ITEM"))));"""))
    }

    @Test
    fun retrieveItem_withRetrieveFlag_alwaysAttempts() {
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            retrieveItemService = retrieveAlwaysSucceeds()
        )
        // retrieve = false: mobile ignores flag, still tries
        assertEquals("true", outputLib(lib, """print(to_string(retrieve_item(1, to_item("$TEST_ITEM"), false)));"""))
    }

    @Test
    fun retrieveItem_nullService_returnsFalse() {
        val lib = GameRuntimeLibrary(gameDatabase = stubDb(), retrieveItemService = null)
        assertEquals("false", outputLib(lib, """print(to_string(retrieve_item(1, to_item("$TEST_ITEM"))));"""))
    }

    @Test
    fun mallPrice_returnsLowestListedPrice() {
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            mallManager = mallThatBuys(0)   // cheapestPrice returns 500L for TEST_ITEM
        )
        assertEquals("500", outputLib(lib, """print(to_string(mall_price(to_item("$TEST_ITEM"))));"""))
    }

    @Test
    fun mallPrice_nullMallManager_returnsMinusOne() {
        val lib = GameRuntimeLibrary(gameDatabase = stubDb(), mallManager = null)
        assertEquals("-1", outputLib(lib, """print(to_string(mall_price(to_item("$TEST_ITEM"))));"""))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.ash.GameRuntimeLibraryMallTest" -q
```

Expected: FAIL with compilation errors (`mallManager` not a GRL constructor param, `buy`/`retrieve_item`/`mall_price` not registered)

- [ ] **Step 3: Add constructor params to GameRuntimeLibrary.kt**

File: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`

Add these two imports after the existing `import net.sourceforge.kolmafia.request.ClanStashRequest` line:

```kotlin
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.mall.MallManager
```

Add these two params to the class constructor, after `internal val clanStashRequest: ClanStashRequest? = null,`:

```kotlin
    internal val mallManager: MallManager? = null,
    internal val retrieveItemService: RetrieveItemService? = null,
```

Add `registerMallFunctions(scope)` call in `registerAll()`, after `registerPricingQueries(scope)`:

```kotlin
registerMallFunctions(scope)
```

- [ ] **Step 4: Create GameRuntimeLibrary.Mall.kt**

Create `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Mall.kt`:

```kotlin
package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerMallFunctions(scope: AshScope) {

    fun resolveItemId(itemName: String): Int? = gameDatabase?.item(itemName)?.id

    // buy(count: int, it: item) → int
    regFn(scope, "buy", AshType.INT,
        listOf("count" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(0L)
        val count = args[0].toLong().toInt()
        val purchased = kotlinx.coroutines.runBlocking {
            mallManager?.buy(itemId, count) ?: 0
        }
        AshValue.of(purchased.toLong())
    }

    // buy(count: int, it: item, maxPrice: int) → int
    regFn(scope, "buy", AshType.INT,
        listOf("count" to AshType.INT, "it" to AshType.ITEM, "maxPrice" to AshType.INT)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(0L)
        val count = args[0].toLong().toInt()
        val maxPrice = args[2].toLong().toInt()
        val purchased = kotlinx.coroutines.runBlocking {
            mallManager?.buy(itemId, count, maxPrice) ?: 0
        }
        AshValue.of(purchased.toLong())
    }

    // retrieve_item(count: int, it: item) → boolean
    regFn(scope, "retrieve_item", AshType.BOOLEAN,
        listOf("count" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val count = args[0].toLong().toInt()
        val retrieved = kotlinx.coroutines.runBlocking {
            retrieveItemService?.retrieve(itemId, count) ?: 0
        }
        AshValue.of(retrieved >= count)
    }

    // retrieve_item(count: int, it: item, retrieve: boolean) → boolean
    // Desktop: retrieve=false means "check only". Mobile: always attempts retrieval.
    regFn(scope, "retrieve_item", AshType.BOOLEAN,
        listOf("count" to AshType.INT, "it" to AshType.ITEM, "retrieve" to AshType.BOOLEAN)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val count = args[0].toLong().toInt()
        val retrieved = kotlinx.coroutines.runBlocking {
            retrieveItemService?.retrieve(itemId, count) ?: 0
        }
        AshValue.of(retrieved >= count)
    }
}
```

- [ ] **Step 5: Add `mall_price` to GameRuntimeLibrary.Pricing.kt**

File: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Pricing.kt`

The file currently registers `autosell_price` and `npc_price`. Add `mall_price` at the end of the `registerPricingQueries` function, before the closing `}`:

```kotlin
    // mall_price(it: item) → int — cheapest listed mall price; -1 if not found
    regFn(scope, "mall_price", AshType.INT,
        listOf("it" to AshType.ITEM)) { _, args ->
        val itemName = args[0].toString()
        val price = kotlinx.coroutines.runBlocking {
            mallManager?.cheapestPrice(itemName) ?: -1L
        }
        AshValue.of(price)
    }
```

- [ ] **Step 6: Run tests to verify they pass**

```
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.ash.GameRuntimeLibraryMallTest" -q
```

Expected: All 9 tests PASS

- [ ] **Step 7: Run full test suite to verify no regressions**

```
./gradlew :shared:jvmTest -q
```

Expected: All tests PASS

- [ ] **Step 8: Commit**

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Mall.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Pricing.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryMallTest.kt
git commit -m "feat: ASH buy()/retrieve_item()/mall_price() functions via MallManager + RetrieveItemService"
```

---

### Task 6: SharedModule DI wiring

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt`

Wire `NpcBuyRequest`, `MallManager`, and `RetrieveItemService` into Koin, and add the two new params to the `GameRuntimeLibrary` single block. No new tests needed — the Koin graph is validated at app startup.

- [ ] **Step 1: Add imports to SharedModule.kt**

File: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt`

Add these imports alongside the existing mall imports:

```kotlin
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.npc.NpcBuyRequest
```

- [ ] **Step 2: Register NpcBuyRequest, MallManager, RetrieveItemService**

In `SharedModule.kt`, add these three registrations after the existing `single { MallPriceManager() }` line:

```kotlin
    singleOf(::NpcBuyRequest)
    single { MallManager(get(), get(), get()) }
    single {
        RetrieveItemService(
            inventoryManager = get(),
            closetRequest    = get(),
            storageRequest   = get(),
            npcBuyRequest    = get(),
            mallManager      = get(),
            gameDatabase     = get()
        )
    }
```

- [ ] **Step 3: Add mallManager + retrieveItemService to the GameRuntimeLibrary single block**

In the `single { GameRuntimeLibrary(...) }` block (ending at line ~189), add after `clanStashRequest = get(),`:

```kotlin
            mallManager          = get(),
            retrieveItemService  = get(),
```

- [ ] **Step 4: Run the full test suite**

```
./gradlew :shared:jvmTest -q
```

Expected: All tests PASS

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt
git commit -m "feat: wire NpcBuyRequest, MallManager, RetrieveItemService into Koin DI"
```
