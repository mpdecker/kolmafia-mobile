# Phase 15: Inventory Ops & Consumption Completeness

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `eatsilent`/`drinksilent`/`overdrink` consumption variants, wire `get_closet`/`get_storage` to live API data, add display-case and clan-stash HTTP wrappers with their ASH functions, and expand `cli_execute` to handle `equip`/`unequip`/`sell`/`autosell` commands.

**Architecture:** All additions follow existing Phase 14 patterns: new or expanded `request/` classes for HTTP, `regFn()` entries in `GameRuntimeLibrary.ItemActions.kt` or `GameRuntimeLibrary.Collections.kt` for ASH bindings, new nullable constructor params on `GameRuntimeLibrary`, and `singleOf(::...)` entries in `SharedModule`. The `cliDispatch` ordered list in `GameRuntimeLibrary.kt` is extended with 4 new patterns; ordering matters (`equip slot item` before `equip item`). `InventoryManager` gains `open` on the methods used by cli dispatch so tests can override them.

**Tech Stack:** Kotlin Multiplatform (commonMain), Ktor client, MockEngine for tests, `./gradlew :shared:jvmTest`

---

## File Structure

**Create:**
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/DisplayCaseRequest.kt` — HTTP wrappers for `displaycollection.php` put/take
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/ClanStashRequest.kt` — HTTP wrappers for `clan_stash.php` put/take
- `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryConsumptionTest.kt` — tests for eatsilent/drinksilent/overdrink
- `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryDisplayStashTest.kt` — tests for put/take display + stash
- `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryCliEquipTest.kt` — tests for equip/unequip/sell/autosell cli dispatch

**Modify:**
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/ClosetRequest.kt` — add `open` + `open suspend fun fetchContents()`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/StorageRequest.kt` — add `open` + `open suspend fun fetchContents()`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Collections.kt` — wire live `get_closet` / `get_storage`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.ItemActions.kt` — add eatsilent/drinksilent/overdrink + put_display/take_display/put_stash/take_stash
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt` — add `displayCaseRequest`/`clanStashRequest` constructor params; add equip/unequip/sell/autosell to `cliDispatch`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/inventory/InventoryManager.kt` — make class `open`, `equipItem` and `unequipSlot` `open suspend fun`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt` — create and inject `DisplayCaseRequest` and `ClanStashRequest`
- `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryCollectionsTest.kt` — add live closet/storage tests

---

## Task T1: eatsilent / drinksilent / overdrink

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.ItemActions.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryConsumptionTest.kt`

**Background:** Desktop KoLmafia has `eatsilent(qty, item)` — eat without checking the fullness cap and without printing a fullness error. `drinksilent(qty, item)` skips the inebriety cap check. `overdrink(qty, item)` also skips the cap check and allows going over the inebriety limit. On mobile, the existing `EatFoodRequest.eat()` and `DrinkBoozeRequest.drink()` submit the HTTP request to KoL without any client-side cap enforcement (the server handles it), so all three silent/over variants can delegate directly to the same request methods. The only difference from `eat`/`drink` is that they don't refuse to fire when the character is already full — which mobile doesn't enforce anyway.

- [ ] **Step 1: Write the failing tests**

Create `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryConsumptionTest.kt`:

```kotlin
package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.request.DrinkBoozeRequest
import net.sourceforge.kolmafia.request.EatFoodRequest
import kotlin.test.Test
import kotlin.test.assertEquals

private fun okClient(): HttpClient = HttpClient(MockEngine {
    respond("ok", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html"))
})

private class StubDb : GameDatabase() {
    private val food = ItemData(1001, "magical sausage", "desc", "food.gif",
        ItemPrimaryUse.EAT, emptySet(), setOf('t', 'd'), 10, null)
    private val booze = ItemData(1002, "bottle of gin", "desc", "gin.gif",
        ItemPrimaryUse.DRINK, emptySet(), setOf('t', 'd'), 20, null)
    override fun item(name: String): ItemData? = when {
        name.equals("magical sausage", ignoreCase = true) -> food
        name.equals("bottle of gin", ignoreCase = true) -> booze
        else -> null
    }
    override fun item(id: Int): ItemData? = when (id) { 1001 -> food; 1002 -> booze; else -> null }
}

class GameRuntimeLibraryConsumptionTest {

    // ── eatsilent ──────────────────────────────────────────────────────────────

    @Test
    fun eatsilent_returnsTrueOnSuccess() {
        val lib = GameRuntimeLibrary(
            gameDatabase = StubDb(),
            eatFoodRequest = EatFoodRequest(okClient())
        )
        assertEquals("true",
            outputLib(lib, """print(to_string(eatsilent(1, to_item("magical sausage"))));"""))
    }

    @Test
    fun eatsilent_returnsFalseWithNullRequest() {
        val lib = GameRuntimeLibrary(gameDatabase = StubDb(), eatFoodRequest = null)
        assertEquals("false",
            outputLib(lib, """print(to_string(eatsilent(1, to_item("magical sausage"))));"""))
    }

    @Test
    fun eatsilent_returnsFalseForUnknownItem() {
        val lib = GameRuntimeLibrary(gameDatabase = StubDb(), eatFoodRequest = EatFoodRequest(okClient()))
        assertEquals("false",
            outputLib(lib, """print(to_string(eatsilent(1, to_item("mystery meat"))));"""))
    }

    // ── drinksilent ────────────────────────────────────────────────────────────

    @Test
    fun drinksilent_returnsTrueOnSuccess() {
        val lib = GameRuntimeLibrary(
            gameDatabase = StubDb(),
            drinkBoozeRequest = DrinkBoozeRequest(okClient())
        )
        assertEquals("true",
            outputLib(lib, """print(to_string(drinksilent(1, to_item("bottle of gin"))));"""))
    }

    @Test
    fun drinksilent_returnsFalseWithNullRequest() {
        val lib = GameRuntimeLibrary(gameDatabase = StubDb(), drinkBoozeRequest = null)
        assertEquals("false",
            outputLib(lib, """print(to_string(drinksilent(1, to_item("bottle of gin"))));"""))
    }

    @Test
    fun drinksilent_returnsFalseForUnknownItem() {
        val lib = GameRuntimeLibrary(gameDatabase = StubDb(), drinkBoozeRequest = DrinkBoozeRequest(okClient()))
        assertEquals("false",
            outputLib(lib, """print(to_string(drinksilent(1, to_item("mystery booze"))));"""))
    }

    // ── overdrink ──────────────────────────────────────────────────────────────

    @Test
    fun overdrink_returnsTrueOnSuccess() {
        val lib = GameRuntimeLibrary(
            gameDatabase = StubDb(),
            drinkBoozeRequest = DrinkBoozeRequest(okClient())
        )
        assertEquals("true",
            outputLib(lib, """print(to_string(overdrink(1, to_item("bottle of gin"))));"""))
    }

    @Test
    fun overdrink_returnsFalseWithNullRequest() {
        val lib = GameRuntimeLibrary(gameDatabase = StubDb(), drinkBoozeRequest = null)
        assertEquals("false",
            outputLib(lib, """print(to_string(overdrink(1, to_item("bottle of gin"))));"""))
    }

    @Test
    fun overdrink_returnsFalseForUnknownItem() {
        val lib = GameRuntimeLibrary(gameDatabase = StubDb(), drinkBoozeRequest = DrinkBoozeRequest(okClient()))
        assertEquals("false",
            outputLib(lib, """print(to_string(overdrink(1, to_item("mystery booze"))));"""))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```
./gradlew :shared:jvmTest --tests "*.GameRuntimeLibraryConsumptionTest"
```

Expected: FAIL — `eatsilent`, `drinksilent`, `overdrink` are not registered.

- [ ] **Step 3: Implement the three functions**

Append to `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.ItemActions.kt`, inside the `registerItemActions` function body, after the existing `take_storage` block and before the closing `}`:

```kotlin
    // 10. eatsilent(qty: int, it: item) → boolean
    // Same as eat() — mobile has no client-side fullness guard; server enforces cap.
    regFn(scope, "eatsilent", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = eatFoodRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.eat(itemId, qty) }.isSuccess)
    }

    // 11. drinksilent(qty: int, it: item) → boolean
    // Same as drink() — mobile has no client-side inebriety guard.
    regFn(scope, "drinksilent", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = drinkBoozeRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.drink(itemId, qty) }.isSuccess)
    }

    // 12. overdrink(qty: int, it: item) → boolean
    // Allows drinking past the inebriety limit — same HTTP call as drink().
    regFn(scope, "overdrink", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = drinkBoozeRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.drink(itemId, qty) }.isSuccess)
    }
```

- [ ] **Step 4: Run tests to verify they pass**

```
./gradlew :shared:jvmTest --tests "*.GameRuntimeLibraryConsumptionTest"
```

Expected: 9 tests PASS.

- [ ] **Step 5: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.ItemActions.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryConsumptionTest.kt
git commit -m "feat: add eatsilent/drinksilent/overdrink ASH functions (Phase 15 T1)"
```

---

## Task T2: get_closet / get_storage live

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/ClosetRequest.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/StorageRequest.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Collections.kt`
- Modify: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryCollectionsTest.kt`

**Background:** `get_closet()` and `get_storage()` currently return empty `AggregateValue`s (stubs). KoL's `api.php?what=closet` and `api.php?what=storage` return JSON `{"itemId": quantity, ...}` — the same format as `api.php?what=inventory` which `InventoryManager` already parses with `response.body<Map<String, Int>>()`. We add `fetchContents()` to both request classes (marked `open` so tests can override), then replace the stubs in `Collections.kt` with `runBlocking { closetRequest?.fetchContents() }` calls that convert IDs to names via `gameDatabase`.

- [ ] **Step 1: Write the failing tests**

Append to `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryCollectionsTest.kt`:

```kotlin
// Add these imports at the top of the file:
// import io.ktor.client.HttpClient
// import io.ktor.client.engine.mock.MockEngine
// import io.ktor.client.engine.mock.respond
// import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
// import io.ktor.http.HttpHeaders
// import io.ktor.http.HttpStatusCode
// import io.ktor.http.headersOf
// import io.ktor.serialization.kotlinx.json.json
// import kotlinx.serialization.json.Json
// import net.sourceforge.kolmafia.data.GameDatabase
// import net.sourceforge.kolmafia.data.ItemData
// import net.sourceforge.kolmafia.data.ItemPrimaryUse
// import net.sourceforge.kolmafia.request.ClosetRequest
// import net.sourceforge.kolmafia.request.StorageRequest

// Add these tests to GameRuntimeLibraryCollectionsTest class:

    @Test
    fun getCloset_returnsLiveItemsFromFetchContents() {
        // Override fetchContents() to return a known map without hitting the network
        val fakeCloset = object : ClosetRequest(
            HttpClient(MockEngine { respond("") })
        ) {
            override suspend fun fetchContents(): Map<Int, Int> = mapOf(42 to 3)
        }
        val db = object : GameDatabase() {
            override fun item(id: Int): ItemData? = if (id == 42)
                ItemData(42, "shiny item", "desc", "item.gif",
                    ItemPrimaryUse.NONE, emptySet(), setOf('t', 'd'), 0, null)
            else null
        }
        val lib = GameRuntimeLibrary(closetRequest = fakeCloset, gameDatabase = db)
        // get_closet() should return aggregate with 1 entry: "shiny item" → 3
        assertEquals("1", outputLib(lib, "print(to_string(count(get_closet())));"))
    }

    @Test
    fun getStorage_returnsLiveItemsFromFetchContents() {
        val fakeStorage = object : StorageRequest(
            HttpClient(MockEngine { respond("") })
        ) {
            override suspend fun fetchContents(): Map<Int, Int> = mapOf(99 to 7)
        }
        val db = object : GameDatabase() {
            override fun item(id: Int): ItemData? = if (id == 99)
                ItemData(99, "haggard item", "desc", "hag.gif",
                    ItemPrimaryUse.NONE, emptySet(), setOf('t', 'd'), 0, null)
            else null
        }
        val lib = GameRuntimeLibrary(storageRequest = fakeStorage, gameDatabase = db)
        assertEquals("1", outputLib(lib, "print(to_string(count(get_storage())));"))
    }

    @Test
    fun getCloset_returnsEmptyWhenRequestIsNull() {
        assertEquals("0",
            outputLib(GameRuntimeLibrary.forTesting(), "print(to_string(count(get_closet())));"))
    }

    @Test
    fun getStorage_returnsEmptyWhenRequestIsNull() {
        assertEquals("0",
            outputLib(GameRuntimeLibrary.forTesting(), "print(to_string(count(get_storage())));"))
    }
```

- [ ] **Step 2: Run tests to verify they fail**

```
./gradlew :shared:jvmTest --tests "*.GameRuntimeLibraryCollectionsTest"
```

Expected: `getCloset_returnsLiveItemsFromFetchContents` and `getStorage_returnsLiveItemsFromFetchContents` FAIL because `fetchContents()` doesn't exist yet; the existing 5 tests still pass.

- [ ] **Step 3: Make ClosetRequest open and add fetchContents()**

Replace the current content of `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/ClosetRequest.kt`:

```kotlin
package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.http.KOL_BASE_URL

open class ClosetRequest(private val client: HttpClient) {

    suspend fun putIn(itemId: Int, quantity: Int): Result<String> {
        return try {
            val response = client.get("$KOL_BASE_URL/closet.php") {
                parameter("action", "put")
                parameter("whichitem", itemId)
                parameter("qty", quantity)
                parameter("ajax", 1)
            }
            if (response.status.isSuccess()) {
                Result.success(response.bodyAsText())
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun takeOut(itemId: Int, quantity: Int): Result<String> {
        return try {
            val response = client.get("$KOL_BASE_URL/closet.php") {
                parameter("action", "take")
                parameter("whichitem", itemId)
                parameter("qty", quantity)
                parameter("ajax", 1)
            }
            if (response.status.isSuccess()) {
                Result.success(response.bodyAsText())
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches the closet contents from api.php?what=closet.
     * Returns a map of item ID → quantity. Open so tests can override.
     */
    open suspend fun fetchContents(): Map<Int, Int> {
        return try {
            val response = client.get("$KOL_BASE_URL/api.php") {
                parameter("what", "closet")
                parameter("for", "KoLmafia-Mobile")
            }
            if (!response.status.isSuccess()) return emptyMap()
            val rawMap: Map<String, Int> = response.body()
            rawMap.entries.mapNotNull { (k, v) -> k.toIntOrNull()?.to(v) }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
```

- [ ] **Step 4: Make StorageRequest open and add fetchContents()**

Replace the current content of `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/StorageRequest.kt`:

```kotlin
package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.http.KOL_BASE_URL

open class StorageRequest(private val client: HttpClient) {

    suspend fun withdraw(itemId: Int, quantity: Int): Result<String> {
        return try {
            val response = client.get("$KOL_BASE_URL/storage.php") {
                parameter("action", "take")
                parameter("whichitem", itemId)
                parameter("qty", quantity)
                parameter("ajax", 1)
            }
            if (response.status.isSuccess()) {
                Result.success(response.bodyAsText())
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches Hagnk's storage contents from api.php?what=storage.
     * Returns a map of item ID → quantity. Open so tests can override.
     */
    open suspend fun fetchContents(): Map<Int, Int> {
        return try {
            val response = client.get("$KOL_BASE_URL/api.php") {
                parameter("what", "storage")
                parameter("for", "KoLmafia-Mobile")
            }
            if (!response.status.isSuccess()) return emptyMap()
            val rawMap: Map<String, Int> = response.body()
            rawMap.entries.mapNotNull { (k, v) -> k.toIntOrNull()?.to(v) }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
```

- [ ] **Step 5: Wire live get_closet / get_storage in Collections.kt**

Replace the full content of `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Collections.kt`:

```kotlin
package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerCollectionQueries(scope: AshScope) {

    // int[item] — maps item names to quantities
    val itemIntType = AggregateType(AshType.ITEM, AshType.INT)

    // ── get_inventory() → int[item] ───────────────────────────────────────────
    regFn(scope, "get_inventory", itemIntType, emptyList()) { _, _ ->
        val result = AggregateValue(itemIntType)
        inventoryManager?.state?.value?.items?.values?.forEach { item ->
            result[AshValue.item(item.name)] = AshValue.of(item.quantity.toLong())
        }
        result
    }

    // ── Helper: convert fetchContents() Map<Int, Int> → AggregateValue ───────
    fun mapToAggregate(contents: Map<Int, Int>): AggregateValue {
        val result = AggregateValue(itemIntType)
        contents.forEach { (itemId, qty) ->
            val itemName = gameDatabase?.item(itemId)?.name ?: "Item #$itemId"
            result[AshValue.item(itemName)] = AshValue.of(qty.toLong())
        }
        return result
    }

    // ── get_closet() → int[item] (live — fetches from api.php?what=closet) ───
    regFn(scope, "get_closet", itemIntType, emptyList()) { _, _ ->
        val contents = kotlinx.coroutines.runBlocking {
            closetRequest?.fetchContents() ?: emptyMap()
        }
        mapToAggregate(contents)
    }

    // ── get_storage() → int[item] (live — fetches from api.php?what=storage) ─
    regFn(scope, "get_storage", itemIntType, emptyList()) { _, _ ->
        val contents = kotlinx.coroutines.runBlocking {
            storageRequest?.fetchContents() ?: emptyMap()
        }
        mapToAggregate(contents)
    }

    // ── get_stash() → int[item] (stub — clan stash not yet fetched) ──────────
    regFn(scope, "get_stash", itemIntType, emptyList()) { _, _ ->
        AggregateValue(itemIntType)
    }

    // ── get_display() → int[item] (stub — display case not yet fetched) ──────
    regFn(scope, "get_display", itemIntType, emptyList()) { _, _ ->
        AggregateValue(itemIntType)
    }
}
```

- [ ] **Step 6: Run all collection tests**

```
./gradlew :shared:jvmTest --tests "*.GameRuntimeLibraryCollectionsTest"
```

Expected: 9 tests PASS (5 original stubs + 4 new live/null tests).

- [ ] **Step 7: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/ClosetRequest.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/StorageRequest.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Collections.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryCollectionsTest.kt
git commit -m "feat: wire get_closet/get_storage to live api.php inventory (Phase 15 T2)"
```

---

## Task T3: DisplayCaseRequest + ClanStashRequest

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/DisplayCaseRequest.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/ClanStashRequest.kt`

**Background:** KoL's display collection is managed via `displaycollection.php` and clan stash via `clan_stash.php`. Parameter names follow the desktop KoLmafia `DisplayCaseManager.java` / `ClanStashRequest.java` patterns. Both classes are `open class` with `open suspend fun` methods so test overrides can intercept calls.

- [ ] **Step 1: Create DisplayCaseRequest.kt**

```kotlin
package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.http.KOL_BASE_URL

open class DisplayCaseRequest(private val client: HttpClient) {

    /** Move [quantity] of item [itemId] from backpack into the display case. */
    open suspend fun putIn(itemId: Int, quantity: Int): Result<String> {
        return try {
            val response = client.get("$KOL_BASE_URL/displaycollection.php") {
                parameter("action", "put")
                parameter("whichitem", itemId)
                parameter("howmany", quantity)
                parameter("ajax", 1)
            }
            if (response.status.isSuccess()) Result.success(response.bodyAsText())
            else Result.failure(Exception("HTTP ${response.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Move [quantity] of item [itemId] from the display case into the backpack. */
    open suspend fun takeOut(itemId: Int, quantity: Int): Result<String> {
        return try {
            val response = client.get("$KOL_BASE_URL/displaycollection.php") {
                parameter("action", "take")
                parameter("whichitem", itemId)
                parameter("howmany", quantity)
                parameter("ajax", 1)
            }
            if (response.status.isSuccess()) Result.success(response.bodyAsText())
            else Result.failure(Exception("HTTP ${response.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

- [ ] **Step 2: Create ClanStashRequest.kt**

```kotlin
package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.http.KOL_BASE_URL

open class ClanStashRequest(private val client: HttpClient) {

    /** Contribute [quantity] of item [itemId] to the clan stash. */
    open suspend fun putIn(itemId: Int, quantity: Int): Result<String> {
        return try {
            val response = client.get("$KOL_BASE_URL/clan_stash.php") {
                parameter("action", "contribute")
                parameter("whichitem", itemId)
                parameter("qty", quantity)
                parameter("ajax", 1)
            }
            if (response.status.isSuccess()) Result.success(response.bodyAsText())
            else Result.failure(Exception("HTTP ${response.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Take [quantity] of item [itemId] from the clan stash into the backpack. */
    open suspend fun takeOut(itemId: Int, quantity: Int): Result<String> {
        return try {
            val response = client.get("$KOL_BASE_URL/clan_stash.php") {
                parameter("action", "take")
                parameter("whichitem", itemId)
                parameter("qty", quantity)
                parameter("ajax", 1)
            }
            if (response.status.isSuccess()) Result.success(response.bodyAsText())
            else Result.failure(Exception("HTTP ${response.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

- [ ] **Step 3: Verify compilation**

```
./gradlew :shared:compileKotlinJvm
```

Expected: BUILD SUCCESSFUL — both new files compile.

- [ ] **Step 4: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/DisplayCaseRequest.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/ClanStashRequest.kt
git commit -m "feat: add DisplayCaseRequest and ClanStashRequest HTTP wrappers (Phase 15 T3)"
```

---

## Task T4: put_display / take_display / put_stash / take_stash + DI wiring

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.ItemActions.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryDisplayStashTest.kt`

**Background:** Follows the same dependency-injection pattern as `hermitRequest` / `closetRequest`. Add nullable `displayCaseRequest` and `clanStashRequest` params to the `GameRuntimeLibrary` constructor (default `null` so `forTesting()` still compiles), add 4 `regFn` entries in `ItemActions.kt`, wire in `SharedModule`.

- [ ] **Step 1: Write the failing tests**

Create `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryDisplayStashTest.kt`:

```kotlin
package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.request.ClanStashRequest
import net.sourceforge.kolmafia.request.DisplayCaseRequest
import kotlin.test.Test
import kotlin.test.assertEquals

private fun okClient(): HttpClient = HttpClient(MockEngine {
    respond("ok", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html"))
})

private class DisplayStashDb : GameDatabase() {
    private val gem = ItemData(55, "brilliant gem", "desc", "gem.gif",
        ItemPrimaryUse.NONE, emptySet(), setOf('t', 'd'), 0, null)
    override fun item(name: String): ItemData? =
        if (name.equals("brilliant gem", ignoreCase = true)) gem else null
    override fun item(id: Int): ItemData? = if (id == 55) gem else null
}

class GameRuntimeLibraryDisplayStashTest {

    // ── put_display ────────────────────────────────────────────────────────────

    @Test
    fun putDisplay_returnsTrueOnSuccess() {
        val lib = GameRuntimeLibrary(
            gameDatabase = DisplayStashDb(),
            displayCaseRequest = DisplayCaseRequest(okClient())
        )
        assertEquals("true",
            outputLib(lib, """print(to_string(put_display(1, to_item("brilliant gem"))));"""))
    }

    @Test
    fun putDisplay_returnsFalseWithNullRequest() {
        val lib = GameRuntimeLibrary(gameDatabase = DisplayStashDb(), displayCaseRequest = null)
        assertEquals("false",
            outputLib(lib, """print(to_string(put_display(1, to_item("brilliant gem"))));"""))
    }

    @Test
    fun putDisplay_returnsFalseForUnknownItem() {
        val lib = GameRuntimeLibrary(
            gameDatabase = DisplayStashDb(),
            displayCaseRequest = DisplayCaseRequest(okClient())
        )
        assertEquals("false",
            outputLib(lib, """print(to_string(put_display(1, to_item("no such item"))));"""))
    }

    // ── take_display ───────────────────────────────────────────────────────────

    @Test
    fun takeDisplay_returnsTrueOnSuccess() {
        val lib = GameRuntimeLibrary(
            gameDatabase = DisplayStashDb(),
            displayCaseRequest = DisplayCaseRequest(okClient())
        )
        assertEquals("true",
            outputLib(lib, """print(to_string(take_display(2, to_item("brilliant gem"))));"""))
    }

    @Test
    fun takeDisplay_returnsFalseWithNullRequest() {
        val lib = GameRuntimeLibrary(gameDatabase = DisplayStashDb(), displayCaseRequest = null)
        assertEquals("false",
            outputLib(lib, """print(to_string(take_display(1, to_item("brilliant gem"))));"""))
    }

    // ── put_stash ──────────────────────────────────────────────────────────────

    @Test
    fun putStash_returnsTrueOnSuccess() {
        val lib = GameRuntimeLibrary(
            gameDatabase = DisplayStashDb(),
            clanStashRequest = ClanStashRequest(okClient())
        )
        assertEquals("true",
            outputLib(lib, """print(to_string(put_stash(1, to_item("brilliant gem"))));"""))
    }

    @Test
    fun putStash_returnsFalseWithNullRequest() {
        val lib = GameRuntimeLibrary(gameDatabase = DisplayStashDb(), clanStashRequest = null)
        assertEquals("false",
            outputLib(lib, """print(to_string(put_stash(1, to_item("brilliant gem"))));"""))
    }

    // ── take_stash ─────────────────────────────────────────────────────────────

    @Test
    fun takeStash_returnsTrueOnSuccess() {
        val lib = GameRuntimeLibrary(
            gameDatabase = DisplayStashDb(),
            clanStashRequest = ClanStashRequest(okClient())
        )
        assertEquals("true",
            outputLib(lib, """print(to_string(take_stash(1, to_item("brilliant gem"))));"""))
    }

    @Test
    fun takeStash_returnsFalseWithNullRequest() {
        val lib = GameRuntimeLibrary(gameDatabase = DisplayStashDb(), clanStashRequest = null)
        assertEquals("false",
            outputLib(lib, """print(to_string(take_stash(1, to_item("brilliant gem"))));"""))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```
./gradlew :shared:jvmTest --tests "*.GameRuntimeLibraryDisplayStashTest"
```

Expected: FAIL — `displayCaseRequest`/`clanStashRequest` params don't exist yet and functions aren't registered.

- [ ] **Step 3: Add constructor params to GameRuntimeLibrary**

In `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`, add two new import lines after the existing `import net.sourceforge.kolmafia.request.HermitRequest` line:

```kotlin
import net.sourceforge.kolmafia.request.DisplayCaseRequest
import net.sourceforge.kolmafia.request.ClanStashRequest
```

Then add two nullable constructor params after `internal val hermitRequest: HermitRequest? = null,`:

```kotlin
    internal val displayCaseRequest: DisplayCaseRequest? = null,
    internal val clanStashRequest: ClanStashRequest? = null,
```

The updated parameter block looks like:

```kotlin
class GameRuntimeLibrary(
    internal val character: KoLCharacter? = null,
    internal val inventoryManager: InventoryManager? = null,
    internal val skillManager: SkillManager? = null,
    internal val effectManager: EffectManager? = null,
    internal val adventureManager: AdventureManager? = null,
    internal val familiarManager: FamiliarManager? = null,
    internal val goalManager: GoalManager? = null,
    internal val moodManager: MoodManager? = null,
    internal val preferences: Preferences? = null,
    internal val gameDatabase: GameDatabase? = null,
    internal val useItemRequest: UseItemRequest? = null,
    internal val eatFoodRequest: EatFoodRequest? = null,
    internal val drinkBoozeRequest: DrinkBoozeRequest? = null,
    internal val chewRequest: ChewRequest? = null,
    internal val autosellRequest: AutosellRequest? = null,
    internal val closetRequest: ClosetRequest? = null,
    internal val storageRequest: StorageRequest? = null,
    internal val banishManager: BanishManager? = null,
    internal val httpClient: HttpClient? = null,
    internal val hermitRequest: HermitRequest? = null,
    internal val displayCaseRequest: DisplayCaseRequest? = null,
    internal val clanStashRequest: ClanStashRequest? = null,
) : RuntimeLibrary() {
```

- [ ] **Step 4: Add put_display / take_display / put_stash / take_stash to ItemActions.kt**

Append to `GameRuntimeLibrary.ItemActions.kt` inside `registerItemActions`, after the `overdrink` block from T1:

```kotlin
    // 13. put_display(qty: int, it: item) → boolean — move item from backpack to display case
    regFn(scope, "put_display", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = displayCaseRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.putIn(itemId, qty) }.isSuccess)
    }

    // 14. take_display(qty: int, it: item) → boolean — move item from display case to backpack
    regFn(scope, "take_display", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = displayCaseRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.takeOut(itemId, qty) }.isSuccess)
    }

    // 15. put_stash(qty: int, it: item) → boolean — contribute item to clan stash
    regFn(scope, "put_stash", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = clanStashRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.putIn(itemId, qty) }.isSuccess)
    }

    // 16. take_stash(qty: int, it: item) → boolean — take item from clan stash
    regFn(scope, "take_stash", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = clanStashRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.takeOut(itemId, qty) }.isSuccess)
    }
```

- [ ] **Step 5: Wire into SharedModule.kt**

In `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt`:

Add two import lines after the existing request imports:
```kotlin
import net.sourceforge.kolmafia.request.DisplayCaseRequest
import net.sourceforge.kolmafia.request.ClanStashRequest
```

Add two `singleOf` entries after `singleOf(::StorageRequest)`:
```kotlin
    singleOf(::DisplayCaseRequest)
    singleOf(::ClanStashRequest)
```

Wire the new params into the `GameRuntimeLibrary` single block. The existing block ends with `hermitRequest = get(),`. Add after it:
```kotlin
            displayCaseRequest = get(),
            clanStashRequest   = get(),
```

So the full `GameRuntimeLibrary` single block becomes:
```kotlin
    single {
        GameRuntimeLibrary(
            character         = get(),
            inventoryManager  = get(),
            skillManager      = get(),
            effectManager     = get(),
            adventureManager  = get(),
            familiarManager   = get(),
            goalManager       = get(),
            moodManager       = get(),
            preferences       = get(),
            gameDatabase      = get(),
            useItemRequest    = get(),
            eatFoodRequest    = get(),
            drinkBoozeRequest = get(),
            chewRequest       = get(),
            autosellRequest   = get(),
            closetRequest     = get(),
            storageRequest    = get(),
            banishManager     = get(),
            httpClient        = get(),
            hermitRequest     = get(),
            displayCaseRequest = get(),
            clanStashRequest   = get(),
        )
    }
```

- [ ] **Step 6: Run all tests**

```
./gradlew :shared:jvmTest --tests "*.GameRuntimeLibraryDisplayStashTest"
```

Expected: 10 tests PASS.

- [ ] **Step 7: Run full test suite to check for regressions**

```
./gradlew :shared:jvmTest
```

Expected: All tests PASS.

- [ ] **Step 8: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.ItemActions.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryDisplayStashTest.kt
git commit -m "feat: add put_display/take_display/put_stash/take_stash ASH functions (Phase 15 T4)"
```

---

## Task T5: cli_execute equip / unequip / sell / autosell

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/inventory/InventoryManager.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/AutosellRequest.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryCliEquipTest.kt`

**Background:** `InventoryManager.equipItem()` / `unequipSlot()` and `AutosellRequest.autosell()` are not currently `open`, so tests can't override them. Following the `FamiliarManager` pattern (made `open class` + `open suspend fun setFamiliar()` in Phase 14 T6), mark both classes `open` and add `open` to the specific methods used by cli dispatch. Then add 4 patterns to `cliDispatch`:

1. `equip <slot> <item>` — slot + item name, calls `inventoryManager.equipItem()`
2. `equip <item>` — no slot prefix, auto-detect slot by passing `"default"` to server
3. `unequip <slot>` — remove equipped item from a slot
4. `sell N <item>` or `autosell N <item>` — autosell N copies

**Ordering in cliDispatch is critical.** The 3-word form `equip slot item` must be inserted **before** the 2-word form `equip item` so `firstOrNull` matches the more-specific regex first. New entries go at the end of the `cliDispatch` list (after the `familiar` pattern), since none of the new patterns conflict with existing ones.

- [ ] **Step 1: Write the failing tests**

Create `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryCliEquipTest.kt`:

```kotlin
package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.request.AutosellRequest
import kotlin.test.Test
import kotlin.test.assertEquals

// ── Fake InventoryManager ────────────────────────────────────────────────────

private class FakeInventoryManager(
    initialItems: Map<Int, InventoryItem> = emptyMap(),
    val equipCalls: MutableList<Pair<String, String>> = mutableListOf(),
    val unequipCalls: MutableList<String> = mutableListOf(),
) : InventoryManager(
    HttpClient(MockEngine { respond("") }),
    GameEventBus()
) {
    init {
        // Pre-populate state directly via the mutable backing field exposed by open access
        // (We override state-reading in equip/unequip instead of touching state internals.)
    }

    private val _items: Map<Int, InventoryItem> = initialItems

    // Return fake state so item lookup by name works in cliDispatch
    override val state get() = kotlinx.coroutines.flow.MutableStateFlow(
        InventoryState(items = _items)
    )

    override suspend fun equipItem(item: InventoryItem, slot: String): Result<Unit> {
        equipCalls.add(item.name to slot)
        return Result.success(Unit)
    }

    override suspend fun unequipSlot(slot: String): Result<Unit> {
        unequipCalls.add(slot)
        return Result.success(Unit)
    }
}

// ── Fake AutosellRequest ─────────────────────────────────────────────────────

private class FakeAutosellRequest(
    val sellCalls: MutableList<Pair<Int, Int>> = mutableListOf()
) : AutosellRequest(HttpClient(MockEngine { respond("") })) {
    override suspend fun autosell(itemId: Int, quantity: Int): Result<String> {
        sellCalls.add(itemId to quantity)
        return Result.success("ok")
    }
}

// ── Stub GameDatabase ─────────────────────────────────────────────────────────

private class EquipDb : GameDatabase() {
    private val sword = ItemData(10, "rusty sword", "desc", "sword.gif",
        ItemPrimaryUse.WEAPON, emptySet(), setOf('t', 'd'), 5, null)
    override fun item(name: String): ItemData? =
        if (name.equals("rusty sword", ignoreCase = true)) sword else null
    override fun item(id: Int): ItemData? = if (id == 10) sword else null
}

// ── Tests ─────────────────────────────────────────────────────────────────────

class GameRuntimeLibraryCliEquipTest {

    private fun swordInBackpack() = mapOf(
        10 to InventoryItem(10, "rusty sword", 1, ItemType.WEAPON)
    )

    // ── equip slot item ────────────────────────────────────────────────────────

    @Test
    fun cliExecute_equipWithSlot_callsEquipItem() {
        val equipCalls = mutableListOf<Pair<String, String>>()
        val invMgr = FakeInventoryManager(swordInBackpack(), equipCalls)
        val lib = GameRuntimeLibrary(inventoryManager = invMgr)
        runLib(lib, """cli_execute("equip weapon rusty sword");""")
        assertEquals(listOf("rusty sword" to "weapon"), equipCalls)
    }

    @Test
    fun cliExecute_equipWithSlotUnknownSlot_echoesCommand() {
        val invMgr = FakeInventoryManager(swordInBackpack())
        val lib = GameRuntimeLibrary(inventoryManager = invMgr)
        val out = outputLib(lib, """cli_execute("equip badslot rusty sword");""")
        assertEquals("[cli] equip: unknown slot badslot", out)
    }

    // ── equip item (no slot) ───────────────────────────────────────────────────

    @Test
    fun cliExecute_equipNoSlot_callsEquipItemWithDefault() {
        val equipCalls = mutableListOf<Pair<String, String>>()
        val invMgr = FakeInventoryManager(swordInBackpack(), equipCalls)
        val lib = GameRuntimeLibrary(inventoryManager = invMgr)
        runLib(lib, """cli_execute("equip rusty sword");""")
        assertEquals(listOf("rusty sword" to "default"), equipCalls)
    }

    @Test
    fun cliExecute_equipNoSlot_silentNoOpForUnknownItem() {
        val equipCalls = mutableListOf<Pair<String, String>>()
        val invMgr = FakeInventoryManager(emptyMap(), equipCalls)
        val lib = GameRuntimeLibrary(inventoryManager = invMgr)
        val out = outputLib(lib, """cli_execute("equip mystery item");""")
        assertEquals("", out)
        assertEquals(emptyList<Pair<String, String>>(), equipCalls)
    }

    // ── unequip ────────────────────────────────────────────────────────────────

    @Test
    fun cliExecute_unequip_callsUnequipSlot() {
        val unequipCalls = mutableListOf<String>()
        val invMgr = FakeInventoryManager(unequipCalls = unequipCalls)
        val lib = GameRuntimeLibrary(inventoryManager = invMgr)
        runLib(lib, """cli_execute("unequip weapon");""")
        assertEquals(listOf("weapon"), unequipCalls)
    }

    // ── sell / autosell ────────────────────────────────────────────────────────

    @Test
    fun cliExecute_sell_callsAutosell() {
        val sellCalls = mutableListOf<Pair<Int, Int>>()
        val fakeAutosell = FakeAutosellRequest(sellCalls)
        val lib = GameRuntimeLibrary(gameDatabase = EquipDb(), autosellRequest = fakeAutosell)
        runLib(lib, """cli_execute("sell 5 rusty sword");""")
        assertEquals(listOf(10 to 5), sellCalls)
    }

    @Test
    fun cliExecute_autosell_callsAutosell() {
        val sellCalls = mutableListOf<Pair<Int, Int>>()
        val fakeAutosell = FakeAutosellRequest(sellCalls)
        val lib = GameRuntimeLibrary(gameDatabase = EquipDb(), autosellRequest = fakeAutosell)
        runLib(lib, """cli_execute("autosell 3 rusty sword");""")
        assertEquals(listOf(10 to 3), sellCalls)
    }

    @Test
    fun cliExecute_sell_silentNoOpForUnknownItem() {
        val sellCalls = mutableListOf<Pair<Int, Int>>()
        val fakeAutosell = FakeAutosellRequest(sellCalls)
        val lib = GameRuntimeLibrary(gameDatabase = EquipDb(), autosellRequest = fakeAutosell)
        val out = outputLib(lib, """cli_execute("sell 1 no such thing");""")
        assertEquals("", out)
        assertEquals(emptyList<Pair<Int, Int>>(), sellCalls)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```
./gradlew :shared:jvmTest --tests "*.GameRuntimeLibraryCliEquipTest"
```

Expected: Compilation fails — `InventoryManager.state` is not `open`, `equipItem` and `unequipSlot` are not `open`, and the new cliDispatch patterns don't exist yet.

- [ ] **Step 3: Make InventoryManager open with open methods**

In `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/inventory/InventoryManager.kt`, make four changes:

1. Change `class InventoryManager(` → `open class InventoryManager(`

2. Change `val state: StateFlow<InventoryState>` to `open val state: StateFlow<InventoryState>`:

   Find this line:
   ```kotlin
   val state: StateFlow<InventoryState> = _state.asStateFlow()
   ```
   Replace with:
   ```kotlin
   open val state: StateFlow<InventoryState> = _state.asStateFlow()
   ```

3. Change `suspend fun equipItem(item: InventoryItem, slot: String)` → `open suspend fun equipItem(item: InventoryItem, slot: String)`

4. Change `suspend fun unequipSlot(slot: String)` → `open suspend fun unequipSlot(slot: String)`

- [ ] **Step 3b: Make AutosellRequest open with open autosell()**

In `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/AutosellRequest.kt`, make two changes:

1. Change `class AutosellRequest(` → `open class AutosellRequest(`

2. Change `suspend fun autosell(` → `open suspend fun autosell(`

The full updated file:

```kotlin
package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.http.KOL_BASE_URL

open class AutosellRequest(private val client: HttpClient) {
    open suspend fun autosell(itemId: Int, quantity: Int): Result<String> {
        return try {
            val response = client.get("$KOL_BASE_URL/sellstuff_ugly.php") {
                parameter("action", "sell")
                parameter("whichitem", itemId)
                parameter("quantity", quantity)
                parameter("ajax", 1)
            }
            if (response.status.isSuccess()) {
                Result.success(response.bodyAsText())
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

- [ ] **Step 4: Add EquipmentSlot import to GameRuntimeLibrary.kt**

Add this import near the top of `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`, after the existing imports:

```kotlin
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.inventory.InventoryItem
```

- [ ] **Step 5: Add equip/unequip/sell/autosell patterns to cliDispatch**

In `GameRuntimeLibrary.kt`, append 4 new entries to the `cliDispatch` list, **inside** the `listOf(...)` block after the existing `familiar` pattern (before the closing `)`):

```kotlin
        // IMPORTANT: "equip <slot> <item>" must come before "equip <item>" below.
        // firstOrNull matches the first regex that fits — the 3-word form is more specific.

        // "equip <slot> <item-name>" — equip into named slot (slot = apiKey or displayName)
        Regex("^equip\\s+(\\S+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            val slotName = m.groupValues[1].trim()
            val itemName = m.groupValues[2].trim()
            val slot = EquipmentSlot.entries.find { s ->
                s.displayName.equals(slotName, ignoreCase = true)
                    || s.apiKey.equals(slotName, ignoreCase = true)
            }
            if (slot == null) {
                rt.print("[cli] equip: unknown slot $slotName")
                return@to
            }
            val item = inventoryManager?.state?.value?.items?.values
                ?.find { it.name.equals(itemName, ignoreCase = true) }
            if (item == null) {
                return@to   // not in backpack — silent no-op
            }
            kotlinx.coroutines.runBlocking { inventoryManager!!.equipItem(item, slot.apiKey) }
        },

        // "equip <item-name>" — equip with server-auto-detected slot ("default")
        Regex("^equip\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val itemName = m.groupValues[1].trim()
            val item = inventoryManager?.state?.value?.items?.values
                ?.find { it.name.equals(itemName, ignoreCase = true) }
            if (item != null) {
                kotlinx.coroutines.runBlocking { inventoryManager!!.equipItem(item, "default") }
            }
            // Not in backpack → silent no-op
        },

        // "unequip <slot>" — remove equipped item from a slot
        Regex("^unequip\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val slotName = m.groupValues[1].trim()
            kotlinx.coroutines.runBlocking { inventoryManager?.unequipSlot(slotName) }
        },

        // "sell N <item>" or "autosell N <item>" — autosell N copies via AutosellRequest
        Regex("^(?:sell|autosell)\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemName = m.groupValues[2].trim()
            val itemId = gameDatabase?.item(itemName)?.id ?: return@to
            kotlinx.coroutines.runBlocking { autosellRequest?.autosell(itemId, qty) }
        },
```

- [ ] **Step 6: Run equip/unequip/sell tests**

```
./gradlew :shared:jvmTest --tests "*.GameRuntimeLibraryCliEquipTest"
```

Expected: All 9 tests PASS.

- [ ] **Step 7: Run full test suite to verify no regressions**

```
./gradlew :shared:jvmTest
```

Expected: All tests PASS.

- [ ] **Step 8: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/inventory/InventoryManager.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/AutosellRequest.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryCliEquipTest.kt
git commit -m "feat: add cli_execute equip/unequip/sell/autosell dispatch (Phase 15 T5)"
```
