# Phase 16: Mall Buy & Retrieve Item — Design Spec

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement `buy()` and `retrieve_item()` ASH functions with a full compound retrieval chain (inventory → closet → Hagnk's → NPC → mall), backed by an HTML-parsing `MallSearchRequest` and multi-seller `MallManager`.

**Architecture:** New `mall/` package containing `MallOffer` data class, `MallSearchRequest` (HTML scraper), `MallBuyRequest` (HTTP buyer), and `MallManager` (search→sort→iterate orchestrator). New `mall/RetrieveItemService` owns the full five-step chain. `NpcBuyRequest` added to `request/`. `NpcStoreDatabase` gains a secondary item-ID index. ASH wired via new `GameRuntimeLibrary.Mall.kt` extension. `mall_price()` stub promoted to live.

**Tech Stack:** Ktor `HttpClient`, Koin DI, `runBlocking` coroutine bridge (consistent with all other request classes), regex-based HTML parsing (consistent with `QuestLogDatabase`, `AdventureParser`).

---

## File Map

| Action | File |
|--------|------|
| Create | `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mall/MallOffer.kt` |
| Create | `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mall/MallSearchRequest.kt` |
| Create | `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mall/MallBuyRequest.kt` |
| Create | `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mall/MallManager.kt` |
| Create | `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mall/RetrieveItemService.kt` |
| Create | `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/NpcBuyRequest.kt` |
| Modify | `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/NpcStoreDatabase.kt` |
| Modify | `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/GameDatabase.kt` |
| Create | `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Mall.kt` |
| Modify | `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Pricing.kt` |
| Modify | `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt` |
| Modify | `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt` |
| Create | `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/mall/MallSearchRequestTest.kt` |
| Create | `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/mall/MallBuyRequestTest.kt` |
| Create | `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/mall/MallManagerTest.kt` |
| Create | `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/NpcBuyRequestTest.kt` |
| Create | `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/mall/RetrieveItemServiceTest.kt` |
| Create | `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryMallTest.kt` |

---

## Data Model

### `MallOffer`
```kotlin
package net.sourceforge.kolmafia.mall

data class MallOffer(
    val storeId: Int,
    val price: Int,
    val quantity: Int,
    val isLimited: Boolean
)
```

---

## HTTP Layer

### `MallSearchRequest`

```kotlin
open class MallSearchRequest(private val client: HttpClient) {
    open suspend fun search(itemName: String): List<MallOffer> {
        val html = client.get("mall_search.php") {
            parameter("searchstring", itemName)
            parameter("category", "allitems")
            parameter("num", "50")
        }.bodyAsText()
        return parseOffers(html)
    }

    internal fun parseOffers(html: String): List<MallOffer> {
        // Regex extracts each seller row from the mall search results table.
        // KoL mall HTML format (as of 2026):
        //   href="mallstore.php?whichstore=STOREID&..."  — storeId
        //   "N,NNN Meat" text node                        — price (strip commas)
        //   integer quantity cell                         — quantity
        //   "limited" text presence                       — isLimited
        val offers = mutableListOf<MallOffer>()
        val rowRegex = Regex(
            """whichstore=(\d+)[^"]*"[^>]*>.*?</a>.*?([\d,]+)\s*[Mm]eat.*?</td>\s*<td[^>]*>\s*(\d+)""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )
        for (match in rowRegex.findAll(html)) {
            val storeId = match.groupValues[1].toIntOrNull() ?: continue
            val price   = match.groupValues[2].replace(",", "").toIntOrNull() ?: continue
            val qty     = match.groupValues[3].toIntOrNull() ?: continue
            // "limited" appears in the row when the store has a per-day purchase limit
            val isLimited = match.value.contains("limited", ignoreCase = true)
            offers += MallOffer(storeId, price, qty, isLimited)
        }
        return offers
    }
}
```

**Note:** `parseOffers` is `internal` (not `private`) so tests can call it directly with fixture HTML without needing an `HttpClient`.

### `MallBuyRequest`

```kotlin
open class MallBuyRequest(private val client: HttpClient) {
    open suspend fun buy(storeId: Int, itemId: Int, quantity: Int): Result<Int> =
        runCatching {
            val body = client.submitForm(
                "mallstore.php",
                formParameters = parameters {
                    append("whichstore", storeId.toString())
                    append("buying",     "1")
                    append("whichitem",  itemId.toString())
                    append("quantity",   quantity.toString())
                    append("ajax",       "1")
                }
            ).bodyAsText()
            // KoL returns the number of items purchased in the response body.
            // Successful buy: body contains "You acquire" or a quantity integer.
            // On full success assume `quantity` items purchased; on detectable
            // failure (body contains "you can't afford" / "That store is empty")
            // return 0.
            when {
                body.contains("can't afford", ignoreCase = true) -> 0
                body.contains("empty",        ignoreCase = true) -> 0
                body.contains("You acquire",  ignoreCase = true) -> quantity
                else -> quantity  // treat unknown success as full purchase
            }
        }
}
```

### `NpcBuyRequest`

```kotlin
open class NpcBuyRequest(private val client: HttpClient) {
    open suspend fun buy(storeId: Int, itemId: Int, quantity: Int): Result<Int> =
        runCatching {
            val body = client.submitForm(
                "store.php",
                formParameters = parameters {
                    append("whichstore", storeId.toString())
                    append("buying",     "1")
                    append("whichitem",  itemId.toString())
                    append("quantity",   quantity.toString())
                }
            ).bodyAsText()
            when {
                body.contains("don't have enough Meat", ignoreCase = true) -> 0
                body.contains("You acquire",            ignoreCase = true) -> quantity
                else -> quantity
            }
        }
}
```

---

## `NpcStoreDatabase` Extension

`NpcStoreDatabase` currently indexes `NpcStoreItem` by item name. Add a secondary index by item ID built at load time:

```kotlin
// Inside NpcStoreDatabase.load():
private val byItemId: MutableMap<Int, NpcStoreItem> = mutableMapOf()

// After parsing each row:
byItemId[item.itemId] = item

// New accessor:
fun byItemId(itemId: Int): NpcStoreItem? = byItemId[itemId]
```

`NpcStoreItem` must expose `itemId: Int` and `storeId: Int`. Verify these fields exist when implementing (they should from the Phase 11 5-column parser; add if absent).

`GameDatabase` gains:
```kotlin
fun npcStoreFor(itemId: Int): NpcStoreItem? = npcStoreDatabase?.byItemId(itemId)
```

---

## `MallManager`

```kotlin
open class MallManager(
    val searchRequest: MallSearchRequest,
    val buyRequest: MallBuyRequest,
    private val gameDatabase: GameDatabase?
) {
    open suspend fun buy(itemId: Int, count: Int, maxPrice: Int = Int.MAX_VALUE): Int {
        val itemName = gameDatabase?.item(itemId)?.name ?: return 0
        val offers = searchRequest.search(itemName)
            .filter { it.price <= maxPrice && it.quantity > 0 }
            .sortedBy { it.price }
        var remaining = count
        for (offer in offers) {
            if (remaining <= 0) break
            val qty = minOf(remaining, offer.quantity)
            val purchased = buyRequest.buy(offer.storeId, itemId, qty).getOrDefault(0)
            remaining -= purchased
        }
        return count - remaining
    }
}
```

`searchRequest` and `buyRequest` are `internal val` (not private) so test subclasses can access them and `FakeMallManager` overrides are clean.

---

## `RetrieveItemService`

```kotlin
open class RetrieveItemService(
    private val inventoryManager: InventoryManager?,
    private val closetRequest: ClosetRequest?,
    private val storageRequest: StorageRequest?,
    private val npcBuyRequest: NpcBuyRequest?,
    private val mallManager: MallManager?,
    private val gameDatabase: GameDatabase?
) {
    open suspend fun retrieve(itemId: Int, qty: Int): Int {
        var remaining = qty - inventoryCount(itemId)
        if (remaining <= 0) return qty

        // Step 1: closet pull — on success assume full qty moved (KoL response body not parsed)
        if (remaining > 0 && closetRequest != null) {
            val result = closetRequest.takeOut(itemId, remaining)
            if (result.isSuccess) remaining = 0
        }

        // Step 2: Hagnk's storage pull — same assumption
        if (remaining > 0 && storageRequest != null) {
            val result = storageRequest.withdraw(itemId, remaining)
            if (result.isSuccess) remaining = 0
        }

        // Step 3: NPC buy (if item is in NpcStoreDatabase)
        if (remaining > 0 && npcBuyRequest != null) {
            val npc = gameDatabase?.npcStoreFor(itemId)
            if (npc != null) {
                val purchased = npcBuyRequest.buy(npc.storeId, itemId, remaining).getOrDefault(0)
                remaining -= purchased
            }
        }

        // Step 4: Mall buy (no price ceiling for retrieve_item)
        if (remaining > 0 && mallManager != null) {
            remaining -= mallManager.buy(itemId, remaining)
        }

        return qty - remaining
    }

    private fun inventoryCount(itemId: Int): Int =
        inventoryManager?.state?.value?.items?.values
            ?.find { it.id == itemId }?.quantity ?: 0
}
```

**Re-checking inventory after closet/storage pulls:** `ClosetRequest.takeOut()` and `StorageRequest.withdraw()` return `Result<String>` (raw response body). Rather than parsing quantities from responses (fragile), `retrieve()` re-reads `inventoryManager.state` after each successful pull to compute the new `remaining`. This is safe because `InventoryManager` state updates on successful HTTP actions.

---

## ASH Layer — `GameRuntimeLibrary.Mall.kt`

```kotlin
package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking

internal fun GameRuntimeLibrary.registerMallFunctions(scope: AshScope) {

    fun resolveItemId(name: String): Int? = gameDatabase?.item(name)?.id

    // buy(count, item) → int
    regFn(scope, "buy", AshType.INT,
        listOf("count" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(0L)
        val count  = args[0].toLong().toInt()
        AshValue.of(runBlocking { mallManager?.buy(itemId, count) ?: 0 }.toLong())
    }

    // buy(count, item, maxPrice) → int
    regFn(scope, "buy", AshType.INT,
        listOf("count" to AshType.INT, "it" to AshType.ITEM, "price" to AshType.INT)) { _, args ->
        val itemId   = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(0L)
        val count    = args[0].toLong().toInt()
        val maxPrice = args[2].toLong().toInt()
        AshValue.of(runBlocking { mallManager?.buy(itemId, count, maxPrice) ?: 0 }.toLong())
    }

    // retrieve_item(count, item) → boolean
    regFn(scope, "retrieve_item", AshType.BOOLEAN,
        listOf("count" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val count  = args[0].toLong().toInt()
        val got    = runBlocking { retrieveItemService?.retrieve(itemId, count) ?: 0 }
        AshValue.of(got >= count)
    }

    // retrieve_item(count, item, retrieve) → boolean
    // retrieve=false: check-only mode — stub returning false (no inventory scan implemented)
    regFn(scope, "retrieve_item", AshType.BOOLEAN,
        listOf("count" to AshType.INT, "it" to AshType.ITEM, "retrieve" to AshType.BOOLEAN)) { _, args ->
        if (!args[2].toBoolean()) return@regFn AshValue.of(false)
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val count  = args[0].toLong().toInt()
        val got    = runBlocking { retrieveItemService?.retrieve(itemId, count) ?: 0 }
        AshValue.of(got >= count)
    }
}
```

`GameRuntimeLibrary.kt` constructor gains:
```kotlin
internal val mallManager: MallManager? = null,
internal val retrieveItemService: RetrieveItemService? = null,
```

`registerAll()` gains a call to `registerMallFunctions(scope)`.

### `mall_price()` — promoted from stub to live in `GameRuntimeLibrary.Pricing.kt`

```kotlin
regFn(scope, "mall_price", AshType.INT, listOf("it" to AshType.ITEM)) { _, args ->
    val itemName = args[0].toString()
    val offers   = runBlocking { mallManager?.searchRequest?.search(itemName) ?: emptyList() }
    // Return cheapest non-limited price; 0 if not found in mall
    AshValue.of(offers.filter { !it.isLimited }.minOfOrNull { it.price }?.toLong() ?: 0L)
}
```

---

## DI Wiring — `SharedModule.kt`

```kotlin
singleOf(::MallSearchRequest)
singleOf(::MallBuyRequest)
singleOf(::MallManager)
singleOf(::NpcBuyRequest)
singleOf(::RetrieveItemService)
```

`GameRuntimeLibrary` single block:
```kotlin
single {
    GameRuntimeLibrary(
        // ... existing params ...
        mallManager          = get(),
        retrieveItemService  = get(),
    )
}
```

---

## Testing Approach

### `MallSearchRequestTest`

Tests `parseOffers(html)` directly (no HTTP needed).

```kotlin
@Test
fun parseOffers_returnsOffersFromHtml() {
    val html = """
        <a href="mallstore.php?whichstore=12345&searching=1">Bob's Shop</a>
        500 Meat</td><td class="small">10
        <a href="mallstore.php?whichstore=99999&searching=1">Alice's Mall</a>
        1,200 Meat</td><td class="small">3
    """.trimIndent()
    val offers = MallSearchRequest(mockClient()).parseOffers(html)
    assertEquals(2, offers.size)
    assertEquals(MallOffer(12345, 500, 10, false), offers[0])
    assertEquals(MallOffer(99999, 1200, 3, false), offers[1])
}

@Test
fun parseOffers_marksLimitedOffers() { /* row contains "limited" */ }

@Test
fun parseOffers_returnsEmptyOnNoResults() { /* empty HTML */ }

@Test
fun parseOffers_skipsMalformedRows() { /* row missing price cell */ }
```

### `MallBuyRequestTest`

Fake `HttpClient` returning canned response bodies. Assert `Result<Int>` values for: success body ("You acquire"), "can't afford", "empty" response, unknown body (treated as success).

### `MallManagerTest`

`FakeMallSearchRequest` (subclass with stored `_offers`) + `FakeMall BuyRequest` (subclass recording calls, returning configured results):

```kotlin
@Test
fun buy_iteratesMultipleSellers() {
    // Seller A: price=500, qty=3 → buy 3
    // Seller B: price=600, qty=5 → buy 2 (only 2 remaining)
    // Request: buy 5 items
    assertEquals(5, manager.buy(itemId=1, count=5))
    assertEquals(listOf(Pair(111, 3), Pair(222, 2)), fakeBuy.calls)
}

@Test
fun buy_respectsPriceCeiling() { /* maxPrice=550 → only Seller A */ }

@Test
fun buy_returnsPartialWhenSellersExhausted() { /* only 3 available, request 5 → returns 3 */ }

@Test
fun buy_returnsZeroWhenNoOffers() { /* empty search result */ }

@Test
fun buy_returnsZeroWhenManagerIsNull() { /* mallManager = null in GRL */ }
```

### `NpcBuyRequestTest`

Fake client. Assert correct URL + form params, `Result<Int>` for success and "don't have enough Meat" failure.

### `RetrieveItemServiceTest`

All managers faked via subclass override. Key cases:

```kotlin
@Test fun retrieve_alreadyInInventory_noHttpCalls()
@Test fun retrieve_closetSatisfies_noStorageOrMall()
@Test fun retrieve_storageSatisfies_noMall()
@Test fun retrieve_npcSatisfies_noMall()
@Test fun retrieve_mallSatisfies_fullChain()
@Test fun retrieve_partialFromEachSource_accumulates()
@Test fun retrieve_nullClosetRequest_skipsToStorage()
@Test fun retrieve_nullMallManager_returnsBestEffort()
@Test fun retrieve_returnsQtyNotBoolean()
```

### `GameRuntimeLibraryMallTest`

`FakeMallManager` (subclass, stores `buyCalls: List<Triple<Int,Int,Int>>`, returns configured `buyResult`).  
`FakeRetrieveItemService` (subclass, returns configured `retrieveResult`).

```kotlin
@Test fun buy_noPrice_callsMallManager()
@Test fun buy_withPrice_passesMaxPrice()
@Test fun buy_returnsActualCount()
@Test fun retrieveItem_returnsTrueWhenFullyRetrieved()
@Test fun retrieveItem_returnsFalseOnPartial()
@Test fun retrieveItem_withRetrieveFalse_alwaysFalse()
@Test fun mallPrice_returnsCheapestNonLimitedOffer()
@Test fun mallPrice_returnsZeroWhenNoOffers()
```

---

## Known Limitations

- **`retrieve_item(count, item, retrieve=false)`** — check-only mode returns `false` (stub). Desktop behavior scans all sources without purchasing. Not implemented in Phase 16.
- **`historical_price(item)`** — remains a stub (requires `api.php?what=prices` integration, out of scope).
- **Crafting step in retrieve chain** — desktop tries crafting before NPC. Mobile skips crafting (not implemented). Chain goes inventory → closet → storage → NPC → mall.
- **`MallBuyRequest` quantity parsing** — KoL's `mallstore.php?ajax=1` response varies by server version. The "You acquire" heuristic covers the common case; edge cases (purchasing items that go directly to closet/storage) may undercount. Treat as best-effort for Phase 16.
- **`ClosetRequest`/`StorageRequest` all-or-nothing assumption** — `RetrieveItemService` treats a successful `takeOut`/`withdraw` as "all requested qty was moved." If KoL returns success but moved fewer items (e.g., closet had 3 but we requested 5), `remaining` is set to 0 prematurely and the chain stops short. Acceptable for Phase 16 since KoL typically errors when the closet/storage lacks sufficient quantity.
