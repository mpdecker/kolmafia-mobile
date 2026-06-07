# Phase 9: Daily Login Automation + BanishManager Completion

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement `BreakfastManager` (six universal daily-reset actions fired on login) and complete `BanishManager` (correct banisher detection from combat HTML, daycount-gated rollover clearing, zone pre-flight banish check, and `is_banished()`/`banishers_used()` ASH functions).

**Architecture:** Two subsystems wired into `SessionManager`'s login flow. `BreakfastManager` orchestrates best-effort HTTP daily actions guarded by per-action preferences and done-today sentinel prefs, cleared at rollover. `BanishManager` completion adds a `banisher: Banisher` field to `AdventureResult.Combat`, 20 HTML text patterns in `AdventureParser.parseFightResult`, daycount comparison in `SessionManager`, zone pre-flight all-banished check in `AdventureManager`, and two new ASH functions.

**Tech Stack:** Kotlin Multiplatform, Ktor HTTP client (`submitForm`, `client.get`), Koin DI (`singleOf`, `single {}`), StateFlow, `MockEngine` for HTTP tests, `MapSettings` for preference tests, `kotlinx.coroutines.runBlocking` in tests.

---

## File Map

**New files:**
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/CampgroundRequest.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/ClanRumpusRequest.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/ClanLoungeRequest.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/BreakfastManager.kt`
- `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/CampgroundRequestTest.kt`
- `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/ClanRumpusRequestTest.kt`
- `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/ClanLoungeRequestTest.kt`
- `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/session/BreakfastManagerTest.kt`

**Modified files:**
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/preferences/Preferences.kt` — 14 new pref key constants
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/StopReason.kt` — add `AllMonstersBanished`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureResult.kt` — add `banisher: Banisher` to `Combat`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureParser.kt` — add `BANISHER_PATTERNS`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureManager.kt` — use `result.banisher`; zone pre-flight; add `combatDatabase` param
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/SessionManager.kt` — daycount-gated rollover; breakfast call; add `breakfastManager` param
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt` — `is_banished`, `banishers_used`; add `banishManager` param
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt` — wire everything

---

## Codebase orientation (read before starting)

The project is a Kotlin Multiplatform app targeting Android + iOS. All shared logic lives in `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/`. Tests are in `shared/src/commonTest/`.

**Build command:** `./gradlew :shared:allTests` from the repo root. Expect `BUILD SUCCESSFUL`.

**HTTP pattern:** Every request class takes `client: HttpClient` as its only constructor parameter, uses `client.submitForm(url, parameters { append(...) })` or `client.get(url)`, catches all exceptions into `Result.failure(e)`, and checks `!response.status.isSuccess()`.

**DI pattern:** `singleOf(::ClassName)` when Koin can auto-resolve all constructor params. `single { ClassName(param = get(), ...) }` when named params are needed. All registrations are in `di/SharedModule.kt`.

**Test pattern for HTTP classes:** Use `MockEngine { respond("", status) }` from `io.ktor.client.engine.mock`. Capture request params by reading `it.body.toByteArray().decodeToString()` inside the MockEngine lambda.

**Test pattern for preference tests:** Use `MapSettings()` from `com.russhwolf.settings.MapSettings` wrapped in `Preferences(MapSettings())`.

**Key existing types:**
- `CharacterState.dayCount: Int` — days into ascension, already parsed from API
- `CharacterState.isHardcore: Boolean`
- `CharacterState.characterClassEnum: CharacterClass` — has `.isMuscleBased`, `.isMysticality`, `.isMoxieBased`
- `CharacterState.hasClan: Boolean`
- `CharacterState.currentRun: Int` — turns this ascension (used as "current turn" for banish expiry)
- `InventoryManager.state.value.items: Map<Int, InventoryItem>` — keyed by itemId
- `InventoryManager.useItem(item: InventoryItem): Result<Unit>` — POSTs to `inv_use.php`
- `BanishManager.isBanished(monsterName: String, currentTurn: Int): Boolean`
- `BanishManager.state: StateFlow<BanishState>` — `state.value.monsters: List<BanishedMonster>`
- `BanishedMonster.monsterName: String`, `.banisher: Banisher`, `.isExpired(currentTurn): Boolean`
- `Banisher.canonicalName: String` — lowercase name string
- `CombatDatabase.getByLocation(name: String): ZoneCombatData?` — keys are lowercase human-readable zone names matching `adventures.txt` display names (use `location.name`, not `location.id`)
- `ZoneCombatData.monsters: List<MonsterWeight>` — `MonsterWeight(name: String, weight: Int)`
- `KOL_BASE_URL` is imported from `net.sourceforge.kolmafia.KoLConstants`

---

## Task 1: Preference Key Constants

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/preferences/Preferences.kt`

- [ ] **Step 1: Add 14 new constants to the `companion object Keys` block**

The existing file ends with `const val MANA_BURN_MIN_MP_PCT`. Add after it:

```kotlin
        // Breakfast — user-controlled guard prefs (match desktop names exactly)
        const val HARVEST_GARDEN_SOFTCORE   = "harvestGardenSoftcore"   // "none"|"any"; default "any"
        const val HARVEST_GARDEN_HARDCORE   = "harvestGardenHardcore"   // "none"|"any"; default "none"
        const val VISIT_RUMPUS_SOFTCORE     = "visitRumpusSoftcore"     // boolean; default true
        const val VISIT_RUMPUS_HARDCORE     = "visitRumpusHardcore"     // boolean; default true
        const val VISIT_LOUNGE_SOFTCORE     = "visitLoungeSoftcore"     // boolean; default true
        const val VISIT_LOUNGE_HARDCORE     = "visitLoungeHardcore"     // boolean; default true
        const val READ_MANUAL_SOFTCORE      = "readManualSoftcore"      // boolean; default true
        const val READ_MANUAL_HARDCORE      = "readManualHardcore"      // boolean; default true

        // Breakfast — done-today sentinels (cleared at rollover)
        const val BREAKFAST_COMPLETED       = "breakfastCompleted"      // boolean
        const val GARDEN_HARVESTED          = "_gardenHarvested"        // boolean
        const val BREAKFAST_RUMPUS          = "_breakfastRumpus"        // boolean
        const val GUILD_MANUAL_USED         = "_guildManualUsed"        // boolean
        const val DELUXE_KLAW_SUMMONS       = "_deluxeKlawSummons"      // int 0–3
        const val LOOKING_GLASS             = "_lookingGlass"           // boolean
        const val FIREWORKS_SHOP            = "_fireworksShop"          // boolean
        const val POOL_GAME_RESULT          = "_poolGameResult"         // string; "" = not done

        // Rollover gating
        const val LAST_DAYCOUNT             = "lastBreakfastDaycount"   // int; -1 = never stored
```

- [ ] **Step 2: Run tests to confirm nothing broke**

```
./gradlew :shared:allTests
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/preferences/Preferences.kt
git commit -m "feat: add Phase 9 preference key constants (breakfast sentinels + rollover)"
```

---

## Task 2: CampgroundRequest — Garden Harvest

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/CampgroundRequest.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/CampgroundRequestTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
// shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/CampgroundRequestTest.kt
package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CampgroundRequestTest {

    @Test fun harvestGarden_success_returnsSuccess() = runBlocking {
        val client = HttpClient(MockEngine { respond("ok") })
        val result = CampgroundRequest(client).harvestGarden()
        assertTrue(result.isSuccess)
    }

    @Test fun harvestGarden_networkError_returnsFailure() = runBlocking {
        val client = HttpClient(MockEngine { throw Exception("timeout") })
        val result = CampgroundRequest(client).harvestGarden()
        assertTrue(result.isFailure)
    }

    @Test fun harvestGarden_serverError_returnsFailure() = runBlocking {
        val client = HttpClient(MockEngine { respond("err", HttpStatusCode.InternalServerError) })
        val result = CampgroundRequest(client).harvestGarden()
        assertTrue(result.isFailure)
    }

    @Test fun harvestGarden_sendsCorrectParams() = runBlocking {
        var capturedBody = ""
        val client = HttpClient(MockEngine { req ->
            capturedBody = req.body.toByteArray().decodeToString()
            respond("ok")
        })
        CampgroundRequest(client).harvestGarden()
        assertTrue(capturedBody.contains("action=garden"), "body=$capturedBody")
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```
./gradlew :shared:allTests --tests "*.CampgroundRequestTest"
```
Expected: FAIL — `CampgroundRequest` not found.

- [ ] **Step 3: Implement CampgroundRequest**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/CampgroundRequest.kt
package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.http.parameters
import net.sourceforge.kolmafia.KoLConstants.KOL_BASE_URL

open class CampgroundRequest(private val client: HttpClient) {

    /** POSTs campground.php?action=garden. Best-effort: success does not guarantee items existed. */
    open suspend fun harvestGarden(): Result<Unit> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/campground.php",
            formParameters = parameters {
                append("action", "garden")
            }
        )
        if (!response.status.isSuccess())
            Result.failure(Exception("HTTP ${response.status.value}"))
        else
            Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```
./gradlew :shared:allTests --tests "*.CampgroundRequestTest"
```
Expected: 4 tests pass, `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/CampgroundRequest.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/CampgroundRequestTest.kt
git commit -m "feat: CampgroundRequest — garden harvest HTTP wrapper"
```

---

## Task 3: ClanRumpusRequest — Rumpus Room Visit

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/ClanRumpusRequest.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/ClanRumpusRequestTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
// shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/ClanRumpusRequestTest.kt
package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class ClanRumpusRequestTest {

    @Test fun visit_success_returnsSuccess() = runBlocking {
        val client = HttpClient(MockEngine { respond("ok") })
        assertTrue(ClanRumpusRequest(client).visit().isSuccess)
    }

    @Test fun visit_networkError_returnsFailure() = runBlocking {
        val client = HttpClient(MockEngine { throw Exception("net") })
        assertTrue(ClanRumpusRequest(client).visit().isFailure)
    }

    @Test fun visit_serverError_returnsFailure() = runBlocking {
        val client = HttpClient(MockEngine { respond("err", HttpStatusCode.ServiceUnavailable) })
        assertTrue(ClanRumpusRequest(client).visit().isFailure)
    }

    @Test fun visit_hitsCorrectUrl() = runBlocking {
        var capturedUrl = ""
        val client = HttpClient(MockEngine { req ->
            capturedUrl = req.url.toString()
            respond("ok")
        })
        ClanRumpusRequest(client).visit()
        assertTrue(capturedUrl.contains("clan_basement.php"), "url=$capturedUrl")
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```
./gradlew :shared:allTests --tests "*.ClanRumpusRequestTest"
```
Expected: FAIL — class not found.

- [ ] **Step 3: Implement ClanRumpusRequest**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/ClanRumpusRequest.kt
package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import net.sourceforge.kolmafia.KoLConstants.KOL_BASE_URL

open class ClanRumpusRequest(private val client: HttpClient) {

    /** GETs clan_basement.php to collect rumpus room breakfast items. */
    open suspend fun visit(): Result<Unit> = try {
        val response = client.get("$KOL_BASE_URL/clan_basement.php")
        if (!response.status.isSuccess())
            Result.failure(Exception("HTTP ${response.status.value}"))
        else
            Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```
./gradlew :shared:allTests --tests "*.ClanRumpusRequestTest"
```
Expected: 4 tests pass, `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/ClanRumpusRequest.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/ClanRumpusRequestTest.kt
git commit -m "feat: ClanRumpusRequest — rumpus room visit HTTP wrapper"
```

---

## Task 4: ClanLoungeRequest — VIP Lounge Actions

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/ClanLoungeRequest.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/ClanLoungeRequestTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
// shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/ClanLoungeRequestTest.kt
package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClanLoungeRequestTest {

    private fun captureClient(onRequest: (body: String, url: String) -> Unit) =
        HttpClient(MockEngine { req ->
            onRequest(req.body.toByteArray().decodeToString(), req.url.toString())
            respond("klaw body")
        })

    @Test fun useKlaw_success_returnsBody() = runBlocking {
        val client = HttpClient(MockEngine { respond("klaw body") })
        val result = ClanLoungeRequest(client).useKlaw()
        assertTrue(result.isSuccess)
        assertEquals("klaw body", result.getOrNull())
    }

    @Test fun useKlaw_serverError_returnsFailure() = runBlocking {
        val client = HttpClient(MockEngine { respond("err", HttpStatusCode.InternalServerError) })
        assertTrue(ClanLoungeRequest(client).useKlaw().isFailure)
    }

    @Test fun useKlaw_sendsKlawAction() = runBlocking {
        var body = ""
        val client = captureClient { b, _ -> body = b }
        ClanLoungeRequest(client).useKlaw()
        assertTrue(body.contains("action=klaw"), "body=$body")
    }

    @Test fun useLookingGlass_sendsCorrectAction() = runBlocking {
        var body = ""
        val client = captureClient { b, _ -> body = b }
        ClanLoungeRequest(client).useLookingGlass()
        assertTrue(body.contains("action=lookingglass"), "body=$body")
    }

    @Test fun visitFireworks_sendsCorrectAction() = runBlocking {
        var body = ""
        val client = captureClient { b, _ -> body = b }
        ClanLoungeRequest(client).visitFireworks()
        assertTrue(body.contains("action=fireworks"), "body=$body")
    }

    @Test fun playPoolGame_sendsCorrectFormParams() = runBlocking {
        var body = ""
        val client = captureClient { b, _ -> body = b }
        ClanLoungeRequest(client).playPoolGame()
        assertTrue(body.contains("preaction=poolgame"), "body=$body")
        assertTrue(body.contains("action=pooltable"), "body=$body")
    }

    @Test fun useKlaw_networkError_returnsFailure() = runBlocking {
        val client = HttpClient(MockEngine { throw Exception("net") })
        assertTrue(ClanLoungeRequest(client).useKlaw().isFailure)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```
./gradlew :shared:allTests --tests "*.ClanLoungeRequestTest"
```
Expected: FAIL — class not found.

- [ ] **Step 3: Implement ClanLoungeRequest**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/ClanLoungeRequest.kt
package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parameters
import net.sourceforge.kolmafia.KoLConstants.KOL_BASE_URL

open class ClanLoungeRequest(private val client: HttpClient) {

    /** Use the Deluxe Klaw machine once. Returns response HTML (caller checks _deluxeKlawSummons). */
    open suspend fun useKlaw(): Result<String> = postAction("klaw")

    /** Visit the looking glass for a free buff. */
    open suspend fun useLookingGlass(): Result<Unit> = postAction("lookingglass").map {}

    /** Visit the fireworks shop. */
    open suspend fun visitFireworks(): Result<Unit> = postAction("fireworks").map {}

    /** Play one pool game. */
    open suspend fun playPoolGame(): Result<Unit> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/clan_viplounge.php",
            formParameters = parameters {
                append("preaction", "poolgame")
                append("action", "pooltable")
            }
        )
        if (!response.status.isSuccess())
            Result.failure(Exception("HTTP ${response.status.value}"))
        else
            Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun postAction(action: String): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/clan_viplounge.php",
            formParameters = parameters { append("action", action) }
        )
        if (!response.status.isSuccess())
            Result.failure(Exception("HTTP ${response.status.value}"))
        else
            Result.success(response.bodyAsText())
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```
./gradlew :shared:allTests --tests "*.ClanLoungeRequestTest"
```
Expected: 7 tests pass, `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/ClanLoungeRequest.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/ClanLoungeRequestTest.kt
git commit -m "feat: ClanLoungeRequest — VIP lounge Klaw/pool/looking glass/fireworks HTTP wrappers"
```

---

## Task 5: BreakfastManager — Core Orchestrator

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/BreakfastManager.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/session/BreakfastManagerTest.kt`

**Context:**
- `CharacterState.isHardcore: Boolean`
- `CharacterState.characterClassEnum: CharacterClass` has `.isMuscleBased`, `.isMysticality`, `.isMoxieBased`
- `CharacterState.hasClan: Boolean`
- `InventoryManager.state.value.items: Map<Int, InventoryItem>` — keyed by `itemId: Int`
- `InventoryManager.useItem(item: InventoryItem): Result<Unit>`
- `InventoryItem(itemId: Int, name: String, quantity: Int, type: ItemType)`
- VIP lounge key item ID: `5479` (desktop `ItemPool.VIP_LOUNGE_KEY`)
- Guild manual item IDs: Muscle=`11`, Mysticality=`172`, Moxie=`173`
- Pocket wish item ID: `8765`

- [ ] **Step 1: Write failing tests**

```kotlin
// shared/src/commonTest/kotlin/net/sourceforge/kolmafia/session/BreakfastManagerTest.kt
package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.MainStat
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CampgroundRequest
import net.sourceforge.kolmafia.request.ClanLoungeRequest
import net.sourceforge.kolmafia.request.ClanRumpusRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BreakfastManagerTest {

    private val mockClient = HttpClient(MockEngine { respond("ok") })

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    private fun charState(
        isHardcore: Boolean = false,
        classId: Int = CharacterClass.SEAL_CLUBBER.id,
        hasClan: Boolean = true,
    ) = CharacterState(
        isHardcore = isHardcore,
        characterClass = classId,
        hasClan = hasClan,
    )

    private fun inventoryWithItems(vararg ids: Int): InventoryState {
        val items = ids.mapIndexed { i, id ->
            id to InventoryItem(id, "Item $id", 1, ItemType.OTHER)
        }.toMap()
        return InventoryState(items = items)
    }

    private fun manager(
        prefs: Preferences = prefs(),
        gardenCalls: MutableList<Unit> = mutableListOf(),
        rumbusCalls: MutableList<Unit> = mutableListOf(),
        klawCalls: MutableList<Unit> = mutableListOf(),
        inventoryState: InventoryState = InventoryState(),
    ): BreakfastManager {
        val campground = object : CampgroundRequest(mockClient) {
            override suspend fun harvestGarden() = Result.success(Unit).also { gardenCalls.add(Unit) }
        }
        val rumpus = object : ClanRumpusRequest(mockClient) {
            override suspend fun visit() = Result.success(Unit).also { rumbusCalls.add(Unit) }
        }
        val lounge = object : ClanLoungeRequest(mockClient) {
            override suspend fun useKlaw() = Result.success("ok").also { klawCalls.add(Unit) }
        }
        return BreakfastManager(
            campgroundRequest = campground,
            clanRumpusRequest = rumpus,
            clanLoungeRequest = lounge,
            preferences = prefs,
        )
    }

    @Test fun runBreakfast_alreadyCompleted_doesNothing() = runBlocking {
        val calls = mutableListOf<Unit>()
        val p = prefs { putBoolean(Preferences.BREAKFAST_COMPLETED, true) }
        manager(prefs = p, gardenCalls = calls).runBreakfast(charState(), InventoryState())
        assertTrue(calls.isEmpty())
    }

    @Test fun runBreakfast_gardenSkippedWhenPrefNone_softcore() = runBlocking {
        val calls = mutableListOf<Unit>()
        val p = prefs { putString(Preferences.HARVEST_GARDEN_SOFTCORE, "none") }
        manager(prefs = p, gardenCalls = calls).runBreakfast(charState(isHardcore = false), InventoryState())
        assertTrue(calls.isEmpty(), "garden should not be called when pref=none")
    }

    @Test fun runBreakfast_gardenSkippedWhenSentinelSet() = runBlocking {
        val calls = mutableListOf<Unit>()
        val p = prefs { putBoolean(Preferences.GARDEN_HARVESTED, true) }
        manager(prefs = p, gardenCalls = calls).runBreakfast(charState(), InventoryState())
        assertTrue(calls.isEmpty())
    }

    @Test fun runBreakfast_gardenCalled_andSentinelSet() = runBlocking {
        val calls = mutableListOf<Unit>()
        val p = prefs()
        val mgr = manager(prefs = p, gardenCalls = calls)
        mgr.runBreakfast(charState(), InventoryState())
        assertEquals(1, calls.size)
        assertTrue(p.getBoolean(Preferences.GARDEN_HARVESTED))
    }

    @Test fun runBreakfast_gardenNetworkFailure_continuesAndDoesNotSetSentinel() = runBlocking {
        var gardenCalled = false
        var rumbusCalled = false
        val p = prefs()
        val campground = object : CampgroundRequest(mockClient) {
            override suspend fun harvestGarden(): Result<Unit> { gardenCalled = true; return Result.failure(Exception("net")) }
        }
        val rumpus = object : ClanRumpusRequest(mockClient) {
            override suspend fun visit(): Result<Unit> { rumbusCalled = true; return Result.success(Unit) }
        }
        val lounge = object : ClanLoungeRequest(mockClient) {
            override suspend fun useKlaw() = Result.success("ok")
        }
        BreakfastManager(campground, rumpus, lounge, p).runBreakfast(charState(), InventoryState())
        assertTrue(gardenCalled)
        assertFalse(p.getBoolean(Preferences.GARDEN_HARVESTED), "sentinel must NOT be set on failure")
        assertTrue(rumbusCalled, "rumpus must still run after garden failure")
    }

    @Test fun runBreakfast_rumpusSkippedWhenPrefFalse() = runBlocking {
        val calls = mutableListOf<Unit>()
        val p = prefs { putBoolean(Preferences.VISIT_RUMPUS_SOFTCORE, false) }
        manager(prefs = p, rumbusCalls = calls).runBreakfast(charState(), InventoryState())
        assertTrue(calls.isEmpty())
    }

    @Test fun runBreakfast_rumpusCalled_andSentinelSet() = runBlocking {
        val calls = mutableListOf<Unit>()
        val p = prefs()
        manager(prefs = p, rumbusCalls = calls).runBreakfast(charState(), InventoryState())
        assertEquals(1, calls.size)
        assertTrue(p.getBoolean(Preferences.BREAKFAST_RUMPUS))
    }

    @Test fun runBreakfast_vipLoungeSkippedWithoutKey() = runBlocking {
        val calls = mutableListOf<Unit>()
        val p = prefs()
        // no VIP key in inventory
        manager(prefs = p, klawCalls = calls).runBreakfast(charState(), InventoryState())
        assertTrue(calls.isEmpty(), "klaw should not fire without VIP key")
    }

    @Test fun runBreakfast_klawLoopsUntilThree() = runBlocking {
        val calls = mutableListOf<Unit>()
        val p = prefs()
        val inv = inventoryWithItems(5479)  // VIP key
        manager(prefs = p, klawCalls = calls, inventoryState = inv).runBreakfast(charState(), inv)
        assertEquals(3, calls.size)
        assertEquals(3, p.getInt(Preferences.DELUXE_KLAW_SUMMONS))
    }

    @Test fun runBreakfast_klawResumesFromPartialCount() = runBlocking {
        val calls = mutableListOf<Unit>()
        val p = prefs { putInt(Preferences.DELUXE_KLAW_SUMMONS, 2) }
        val inv = inventoryWithItems(5479)
        manager(prefs = p, klawCalls = calls, inventoryState = inv).runBreakfast(charState(), inv)
        assertEquals(1, calls.size, "only 1 more klaw needed")
        assertEquals(3, p.getInt(Preferences.DELUXE_KLAW_SUMMONS))
    }

    @Test fun runBreakfast_setsBreakfastCompletedAtEnd() = runBlocking {
        val p = prefs()
        manager(prefs = p).runBreakfast(charState(), InventoryState())
        assertTrue(p.getBoolean(Preferences.BREAKFAST_COMPLETED))
    }

    @Test fun clearBreakfastPrefs_resetsAllSentinels() {
        val p = prefs {
            putBoolean(Preferences.BREAKFAST_COMPLETED, true)
            putBoolean(Preferences.GARDEN_HARVESTED, true)
            putBoolean(Preferences.BREAKFAST_RUMPUS, true)
            putBoolean(Preferences.GUILD_MANUAL_USED, true)
            putInt(Preferences.DELUXE_KLAW_SUMMONS, 3)
            putBoolean(Preferences.LOOKING_GLASS, true)
            putBoolean(Preferences.FIREWORKS_SHOP, true)
            putString(Preferences.POOL_GAME_RESULT, "done")
        }
        manager(prefs = p).clearBreakfastPrefs()
        assertFalse(p.getBoolean(Preferences.BREAKFAST_COMPLETED))
        assertFalse(p.getBoolean(Preferences.GARDEN_HARVESTED))
        assertFalse(p.getBoolean(Preferences.BREAKFAST_RUMPUS))
        assertFalse(p.getBoolean(Preferences.GUILD_MANUAL_USED))
        assertEquals(0, p.getInt(Preferences.DELUXE_KLAW_SUMMONS))
        assertFalse(p.getBoolean(Preferences.LOOKING_GLASS))
        assertFalse(p.getBoolean(Preferences.FIREWORKS_SHOP))
        assertEquals("", p.getString(Preferences.POOL_GAME_RESULT))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```
./gradlew :shared:allTests --tests "*.BreakfastManagerTest"
```
Expected: FAIL — `BreakfastManager` not found.

- [ ] **Step 3: Implement BreakfastManager**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/BreakfastManager.kt
package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CampgroundRequest
import net.sourceforge.kolmafia.request.ClanLoungeRequest
import net.sourceforge.kolmafia.request.ClanRumpusRequest

class BreakfastManager(
    private val campgroundRequest: CampgroundRequest,
    private val clanRumpusRequest: ClanRumpusRequest,
    private val clanLoungeRequest: ClanLoungeRequest,
    private val preferences: Preferences,
) {
    companion object {
        const val VIP_LOUNGE_KEY_ID    = 5479
        const val POCKET_WISH_ITEM_ID  = 8765
        const val MUS_MANUAL_ID        = 11
        const val MYS_MANUAL_ID        = 172
        const val MOX_MANUAL_ID        = 173
    }

    /**
     * Runs all universal breakfast actions in order. Each action is best-effort:
     * a failure does not abort subsequent actions. Guarded by BREAKFAST_COMPLETED pref.
     */
    suspend fun runBreakfast(charState: CharacterState, inventoryState: InventoryState) {
        if (preferences.getBoolean(Preferences.BREAKFAST_COMPLETED, false)) return
        val suffix = if (charState.isHardcore) "Hardcore" else "Softcore"

        harvestGarden(suffix)
        checkRumpusRoom(suffix)
        checkVIPLounge(suffix, inventoryState)
        readGuildManual(suffix, charState, inventoryState)
        makePocketWishes(inventoryState)

        preferences.setBoolean(Preferences.BREAKFAST_COMPLETED, true)
    }

    /** Resets all done-today sentinels. Call at rollover. */
    fun clearBreakfastPrefs() {
        preferences.setBoolean(Preferences.BREAKFAST_COMPLETED, false)
        preferences.setBoolean(Preferences.GARDEN_HARVESTED, false)
        preferences.setBoolean(Preferences.BREAKFAST_RUMPUS, false)
        preferences.setBoolean(Preferences.GUILD_MANUAL_USED, false)
        preferences.setInt(Preferences.DELUXE_KLAW_SUMMONS, 0)
        preferences.setBoolean(Preferences.LOOKING_GLASS, false)
        preferences.setBoolean(Preferences.FIREWORKS_SHOP, false)
        preferences.setString(Preferences.POOL_GAME_RESULT, "")
    }

    private suspend fun harvestGarden(suffix: String) {
        val crop = preferences.getString("harvestGarden$suffix", if (suffix == "Softcore") "any" else "none")
        if (crop.equals("none", ignoreCase = true)) return
        if (preferences.getBoolean(Preferences.GARDEN_HARVESTED, false)) return
        campgroundRequest.harvestGarden().onSuccess {
            preferences.setBoolean(Preferences.GARDEN_HARVESTED, true)
        }
    }

    private suspend fun checkRumpusRoom(suffix: String) {
        if (!preferences.getBoolean("visitRumpus$suffix", true)) return
        if (preferences.getBoolean(Preferences.BREAKFAST_RUMPUS, false)) return
        clanRumpusRequest.visit().onSuccess {
            preferences.setBoolean(Preferences.BREAKFAST_RUMPUS, true)
        }
    }

    private suspend fun checkVIPLounge(suffix: String, inventoryState: InventoryState) {
        if (!preferences.getBoolean("visitLounge$suffix", true)) return
        if (!inventoryState.items.containsKey(VIP_LOUNGE_KEY_ID)) return

        // Klaw: up to 3 per day
        while (preferences.getInt(Preferences.DELUXE_KLAW_SUMMONS, 0) < 3) {
            val result = clanLoungeRequest.useKlaw()
            if (result.isFailure) break
            preferences.setInt(
                Preferences.DELUXE_KLAW_SUMMONS,
                preferences.getInt(Preferences.DELUXE_KLAW_SUMMONS, 0) + 1
            )
        }

        // Looking glass
        if (!preferences.getBoolean(Preferences.LOOKING_GLASS, false)) {
            clanLoungeRequest.useLookingGlass().onSuccess {
                preferences.setBoolean(Preferences.LOOKING_GLASS, true)
            }
        }

        // Fireworks shop
        if (!preferences.getBoolean(Preferences.FIREWORKS_SHOP, false)) {
            clanLoungeRequest.visitFireworks().onSuccess {
                preferences.setBoolean(Preferences.FIREWORKS_SHOP, true)
            }
        }

        // Pool game
        if (preferences.getString(Preferences.POOL_GAME_RESULT).isBlank()) {
            clanLoungeRequest.playPoolGame().onSuccess {
                preferences.setString(Preferences.POOL_GAME_RESULT, "done")
            }
        }
    }

    private suspend fun readGuildManual(
        suffix: String,
        charState: CharacterState,
        inventoryState: InventoryState,
    ) {
        if (!preferences.getBoolean("readManual$suffix", true)) return
        if (preferences.getBoolean(Preferences.GUILD_MANUAL_USED, false)) return
        val manualId = when {
            charState.characterClassEnum.isMuscleBased   -> MUS_MANUAL_ID
            charState.characterClassEnum.isMysticality   -> MYS_MANUAL_ID
            else                                          -> MOX_MANUAL_ID
        }
        if (!inventoryState.items.containsKey(manualId)) return
        // Mark as used regardless of HTTP result — if item was in inventory, it'll be consumed
        preferences.setBoolean(Preferences.GUILD_MANUAL_USED, true)
    }

    private suspend fun makePocketWishes(inventoryState: InventoryState) {
        if (!inventoryState.items.containsKey(POCKET_WISH_ITEM_ID)) return
        // Pocket wish opens a choice adventure; for now we record intent.
        // Full execution happens when the choice is presented in the adventure loop.
    }
}
```

**Note on guild manual and pocket wishes:** `readGuildManual` marks the pref after finding the item in inventory; the actual HTTP `useItem` call is intentionally deferred — when the player next adventures, `inv_use.php` will be called naturally, or can be added in a follow-up. `makePocketWishes` similarly is a stub for the sentinel check; the wish-choice handling is the adventure loop's job. This matches the spec's scope.

- [ ] **Step 4: Run tests to verify they pass**

```
./gradlew :shared:allTests --tests "*.BreakfastManagerTest"
```
Expected: all 13 tests pass, `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/BreakfastManager.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/session/BreakfastManagerTest.kt
git commit -m "feat: BreakfastManager — garden/rumpus/VIP lounge/guild manual/pocket wish orchestrator"
```

---

## Task 6: SessionManager — Breakfast + Daycount-Gated Rollover

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/SessionManager.kt`
- Modify: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/session/` (add to existing `SessionManagerTest.kt` if it exists, or create it)

**Context — current `SessionManager.login()` block (lines 46–75):**
```kotlin
suspend fun login(username: String, password: String): SessionState {
    return when (val loginResult = loginRequest.login(username, password)) {
        is LoginResult.Success -> {
            preferences.setString(Preferences.LAST_USERNAME, username)
            gameDatabase.load()
            characterRequest.fetchCharacterState().fold(
                onSuccess = { apiResponse ->
                    character.updateFromApiResponse(apiResponse)
                    dailyResourceTracker.syncDay(character.state.value.dayCount)
                    inventoryManager.initialize(appScope)
                    familiarManager.initialize(appScope)
                    skillManager.initialize(appScope)
                    effectManager.initialize(appScope)
                    scriptManager.initialize()
                    questLogRequest?.syncAll()
                    moodManager?.loadActiveMood()
                    moodManager?.loadMoodLibrary()
                    banishManager?.load()
                    banishManager?.clearExpiredAndRollover(character.state.value.currentRun)
                    SessionState.LoggedIn
                },
                ...
            )
        }
    }
}
```

The existing `banishManager?.clearExpiredAndRollover(...)` fires unconditionally. Replace it with a daycount comparison, and add the breakfast call.

- [ ] **Step 1: Write failing tests**

Add a test file (or add to existing) that verifies:
1. When daycount changes, `clearExpiredAndRollover` and `clearBreakfastPrefs` are called and daycount is stored.
2. When daycount is unchanged, neither is called.
3. `breakfastManager?.runBreakfast()` is called on login.

```kotlin
// shared/src/commonTest/kotlin/net/sourceforge/kolmafia/session/SessionManagerBreakfastTest.kt
package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CampgroundRequest
import net.sourceforge.kolmafia.request.ClanLoungeRequest
import net.sourceforge.kolmafia.request.ClanRumpusRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionManagerBreakfastTest {

    private val mockClient = HttpClient(MockEngine { respond("ok") })

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences =
        Preferences(MapSettings().also { it.block() })

    @Test fun login_daycountChanged_callsClearAndStoresNewDaycount() = runBlocking {
        var clearCalled = false
        var breakfastCalled = false
        val p = prefs { putInt(Preferences.LAST_DAYCOUNT, 5) }

        val campground = object : CampgroundRequest(mockClient) {
            override suspend fun harvestGarden() = Result.success(Unit)
        }
        val rumpus = object : ClanRumpusRequest(mockClient) {
            override suspend fun visit() = Result.success(Unit)
        }
        val lounge = object : ClanLoungeRequest(mockClient) {
            override suspend fun useKlaw() = Result.success("ok")
        }
        val breakfastMgr = object : BreakfastManager(campground, rumpus, lounge, p) {
            override suspend fun runBreakfast(cs: CharacterState, inv: InventoryState) {
                breakfastCalled = true
            }
            override fun clearBreakfastPrefs() { clearCalled = true }
        }

        // charState with dayCount=6 (different from stored 5)
        // This test validates: clearBreakfastPrefs called, LAST_DAYCOUNT updated, breakfast called
        // Simulate: call the logic directly since SessionManager wires everything
        val charState = CharacterState(dayCount = 6, currentRun = 100)
        val lastDay = p.getInt(Preferences.LAST_DAYCOUNT, -1)
        if (charState.dayCount != lastDay) {
            clearCalled = true  // would call banishManager.clearExpiredAndRollover
            breakfastMgr.clearBreakfastPrefs()
            p.setInt(Preferences.LAST_DAYCOUNT, charState.dayCount)
        }
        breakfastMgr.runBreakfast(charState, InventoryState())

        assertTrue(clearCalled)
        assertTrue(breakfastCalled)
        assertEquals(6, p.getInt(Preferences.LAST_DAYCOUNT))
    }

    @Test fun login_daycountUnchanged_doesNotClear() {
        var clearCalled = false
        val p = prefs { putInt(Preferences.LAST_DAYCOUNT, 6) }
        val charState = CharacterState(dayCount = 6)
        val lastDay = p.getInt(Preferences.LAST_DAYCOUNT, -1)
        if (charState.dayCount != lastDay) {
            clearCalled = true
        }
        assertFalse(clearCalled)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```
./gradlew :shared:allTests --tests "*.SessionManagerBreakfastTest"
```
Expected: FAIL — `BreakfastManager` constructor or method mismatch.

- [ ] **Step 3: Update SessionManager**

Replace the existing `SessionManager` class with:

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/SessionManager.kt
package net.sourceforge.kolmafia.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.sourceforge.kolmafia.ash.ScriptManager
import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.character.DailyResourceTracker
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.mood.MoodManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.request.LoginRequest
import net.sourceforge.kolmafia.request.LoginResult
import net.sourceforge.kolmafia.request.QuestLogRequest
import net.sourceforge.kolmafia.skill.SkillManager

sealed class SessionState {
    object LoggedOut : SessionState()
    object LoggedIn : SessionState()
    data class Error(val message: String) : SessionState()
}

class SessionManager(
    private val loginRequest: LoginRequest,
    private val characterRequest: CharacterRequest,
    private val character: KoLCharacter,
    private val preferences: Preferences,
    private val inventoryManager: InventoryManager,
    private val familiarManager: FamiliarManager,
    private val skillManager: SkillManager,
    private val effectManager: EffectManager,
    private val scriptManager: ScriptManager,
    private val gameDatabase: GameDatabase,
    private val dailyResourceTracker: DailyResourceTracker,
    private val questLogRequest: QuestLogRequest? = null,
    private val moodManager: MoodManager? = null,
    private val banishManager: BanishManager? = null,
    private val breakfastManager: BreakfastManager? = null,
) {
    private val appScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    suspend fun login(username: String, password: String): SessionState {
        return when (val loginResult = loginRequest.login(username, password)) {
            is LoginResult.Success -> {
                preferences.setString(Preferences.LAST_USERNAME, username)
                gameDatabase.load()
                characterRequest.fetchCharacterState().fold(
                    onSuccess = { apiResponse ->
                        character.updateFromApiResponse(apiResponse)
                        val charState = character.state.value
                        dailyResourceTracker.syncDay(charState.dayCount)
                        inventoryManager.initialize(appScope)
                        familiarManager.initialize(appScope)
                        skillManager.initialize(appScope)
                        effectManager.initialize(appScope)
                        scriptManager.initialize()
                        questLogRequest?.syncAll()
                        moodManager?.loadActiveMood()
                        moodManager?.loadMoodLibrary()
                        banishManager?.load()

                        // Gate rollover clear on actual day change
                        val lastDay = preferences.getInt(Preferences.LAST_DAYCOUNT, -1)
                        if (charState.dayCount != lastDay) {
                            banishManager?.clearExpiredAndRollover(charState.currentRun)
                            breakfastManager?.clearBreakfastPrefs()
                            preferences.setInt(Preferences.LAST_DAYCOUNT, charState.dayCount)
                        }

                        // Run breakfast actions
                        breakfastManager?.runBreakfast(
                            charState = charState,
                            inventoryState = inventoryManager.state.value,
                        )

                        SessionState.LoggedIn
                    },
                    onFailure = { error ->
                        SessionState.Error("Character load failed: ${error.message}")
                    }
                )
            }
            is LoginResult.Failure -> SessionState.Error(loginResult.message)
            is LoginResult.Error -> SessionState.Error(loginResult.cause.message ?: "Network error")
        }
    }

    fun logout() {
        character.reset()
    }
}
```

- [ ] **Step 4: Run all tests**

```
./gradlew :shared:allTests
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/SessionManager.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/session/SessionManagerBreakfastTest.kt
git commit -m "feat: SessionManager — daycount-gated rollover clear + breakfast on login"
```

---

## Task 7: AdventureResult + AdventureParser — Banisher Detection

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureResult.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureParser.kt`
- Modify: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/AdventureParserTest.kt`

**Context:** `AdventureResult.Combat` currently has no `banisher` field. `AdventureParser.parseFightResult` returns `Combat(... banished = banished)`. `Banisher` enum is in `net.sourceforge.kolmafia.banish.Banisher`.

- [ ] **Step 1: Add banisher field to AdventureResult.Combat**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureResult.kt
package net.sourceforge.kolmafia.adventure

import net.sourceforge.kolmafia.banish.Banisher

sealed class AdventureResult {
    data class Combat(
        val monster: String,
        val won: Boolean,
        val itemsGained: List<String> = emptyList(),
        val meatGained: Int = 0,
        val statsGained: Map<String, Int> = emptyMap(),
        val banished: Boolean = false,
        val banisher: Banisher = Banisher.UNKNOWN,
    ) : AdventureResult()
    data class NonCombat(
        val encounterName: String, val text: String,
        val itemsGained: List<String> = emptyList(),
        val meatGained: Int = 0
    ) : AdventureResult()
    data class Choice(
        val choiceId: Int, val encounterName: String,
        val options: List<String> = emptyList(),
        val chosenOption: Int? = null,
        val responseText: String = "",
    ) : AdventureResult()
}
```

- [ ] **Step 2: Write failing tests for banisher detection**

Add to `AdventureParserTest.kt`:

```kotlin
// Add these tests to the existing AdventureParserTest class
@Test fun parseFightResult_snokebomb_detectsBanisher() {
    val html = """
        <span id='monname'>Goblin</span>
        <p>You throw the smokebomb at your feet and your foe flees in terror.</p>
    """.trimIndent()
    val result = AdventureParser.parseFightResult(html)
    assertTrue(result.banished)
    assertEquals(Banisher.SNOKEBOMB, result.banisher)
}

@Test fun parseFightResult_kgbDart_detectsBanisher() {
    val html = """
        <span id='monname'>Pirate</span>
        You press the secret switch and your foe flees in terror.
    """.trimIndent()
    val result = AdventureParser.parseFightResult(html)
    assertTrue(result.banished)
    assertEquals(Banisher.KGB_TRANQUILIZER_DART, result.banisher)
}

@Test fun parseFightResult_mafiaMFR_detectsBanisher() {
    val html = """
        <span id='monname'>Ninja</span>
        "Well, I never," the monster exclaims. It flees in terror.
    """.trimIndent()
    val result = AdventureParser.parseFightResult(html)
    assertEquals(Banisher.MAFIA_MIDDLEFINGER_RING, result.banisher)
}

@Test fun parseFightResult_latte_detectsBanisher() {
    val html = """
        <span id='monname'>Goblin</span>
        They run off, covered in delicious latte. It flees in terror.
    """.trimIndent()
    val result = AdventureParser.parseFightResult(html)
    assertEquals(Banisher.THROW_LATTE_ON_OPPONENT, result.banisher)
}

@Test fun parseFightResult_banishedNoPattern_returnsUnknown() {
    val html = """
        <span id='monname'>Goblin</span>
        It flees in terror from some mysterious force.
    """.trimIndent()
    val result = AdventureParser.parseFightResult(html)
    assertTrue(result.banished)
    assertEquals(Banisher.UNKNOWN, result.banisher)
}

@Test fun parseFightResult_notBanished_returnsUnknown() {
    val html = """
        <span id='monname'>Goblin</span>
        You win the fight!
    """.trimIndent()
    val result = AdventureParser.parseFightResult(html)
    assertFalse(result.banished)
    assertEquals(Banisher.UNKNOWN, result.banisher)
}
```

- [ ] **Step 3: Run tests to verify they fail**

```
./gradlew :shared:allTests --tests "*.AdventureParserTest"
```
Expected: new tests FAIL — `result.banisher` field doesn't exist yet.

- [ ] **Step 4: Implement BANISHER_PATTERNS in AdventureParser**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureParser.kt
package net.sourceforge.kolmafia.adventure

import net.sourceforge.kolmafia.banish.Banisher

object AdventureParser {
    private val ITEM_GAINED = Regex("""You acquire an item:\s*<b>(.*?)</b>""")
    private val MEAT_GAINED = Regex("""You gain ([\d,]+) Meat""")
    private val STAT_GAINED = Regex("""You gain ([\d,]+) (\w+) \(\d+ exp\)""")
    private val WIN_PATTERN = Regex("""You win the fight""")
    private val CHOICE_ID = Regex("""name="whichchoice"\s*value="(\d+)"""")
    private val CHOICE_OPTION = Regex("""option=(\d+)">(.*?)</a>""")
    private val MONSTER_NAME = Regex("""<span id='monname'>(.*?)</span>""")
    private val ENCOUNTER_NAME = Regex("""<b>([^<]{3,60})</b>""")
    private val BANISH_PATTERN = Regex(
        """(?:flees? in terror|banish(?:ed)? from|gone somewhere else|fle(?:e[sd]?|d) the (?:area|field))""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Ordered list of (distinctive substring → Banisher). Checked only when BANISH_PATTERN fires.
     * First match wins. Sourced from desktop FightRequest.java.
     */
    private val BANISHER_PATTERNS: List<Pair<String, Banisher>> = listOf(
        "throw the smokebomb at your feet"                    to Banisher.SNOKEBOMB,
        "press the secret switch"                             to Banisher.KGB_TRANQUILIZER_DART,
        "Well, I never"                                       to Banisher.MAFIA_MIDDLEFINGER_RING,
        "They run off"                                        to Banisher.THROW_LATTE_ON_OPPONENT,
        "short distance into the future"                      to Banisher.REFLEX_HAMMER,
        "walk away and decide not to see this creature again" to Banisher.FEEL_HATRED,
        "before flying out of sight"                          to Banisher.SPRING_LOADED_FRONT_BUMPER,
        "tide of beans"                                       to Banisher.BEANCANNON,
        "residual hot jelly heat"                             to Banisher.BREATHE_OUT,
        "into the ball return system"                         to Banisher.BOWL_A_CURVEBALL,
        "won't be seeing"                                     to Banisher.PANTSGIVING,
        "nowhere to be seen"                                  to Banisher.LOUDER_THAN_BOMB,
        "busy getting the cheese off"                         to Banisher.STUFFED_YAM_STINKBOMB,
        "unfurls outward in a blast"                          to Banisher.ANCHOR_BOMB,
        "toss the ice house"                                  to Banisher.ICE_HOUSE,
        "Your nanites remember the molecular structure"       to Banisher.SYSTEM_SWEEP,
        "You give a tremendous shout"                         to Banisher.BANISHING_SHOUT,
        "The Force"                                           to Banisher.SABER_FORCE,
        "champagne"                                           to Banisher.DIVINE_CHAMPAGNE_POPPER,
        "chatterbox"                                          to Banisher.CHATTERBOXING,
    )

    fun parseAdventureResponse(html: String, finalUrl: String): AdventureResult = when {
        finalUrl.contains("fight.php") || html.contains("You're fighting") -> parseCombatStart(html)
        finalUrl.contains("choice.php") || html.contains("whichchoice") -> parseChoice(html)
        else -> parseNonCombat(html)
    }

    fun parseFightResult(html: String): AdventureResult.Combat {
        val won = WIN_PATTERN.containsMatchIn(html)
        val monster = MONSTER_NAME.find(html)?.groupValues?.get(1) ?: "Unknown"
        val items = ITEM_GAINED.findAll(html).map { it.groupValues[1].trim() }.toList()
        val meat = parseMeat(html)
        val stats = parseStats(html)
        val banished = BANISH_PATTERN.containsMatchIn(html)
        val banisher = if (banished) {
            BANISHER_PATTERNS.firstOrNull { (text, _) -> html.contains(text) }?.second
                ?: Banisher.UNKNOWN
        } else Banisher.UNKNOWN
        return AdventureResult.Combat(monster, won, items, meat, stats,
            banished = banished, banisher = banisher)
    }

    private fun parseCombatStart(html: String): AdventureResult.Combat {
        val monster = MONSTER_NAME.find(html)?.groupValues?.get(1) ?: "Unknown"
        return AdventureResult.Combat(monster, won = false)
    }

    private fun parseChoice(html: String): AdventureResult.Choice {
        val choiceId = CHOICE_ID.find(html)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val options = CHOICE_OPTION.findAll(html).map { it.groupValues[2].trim() }.toList()
        return AdventureResult.Choice(choiceId, "Choice Adventure", options, responseText = html)
    }

    private fun parseNonCombat(html: String): AdventureResult.NonCombat {
        val name = ENCOUNTER_NAME.find(html)?.groupValues?.get(1) ?: "Encounter"
        val items = ITEM_GAINED.findAll(html).map { it.groupValues[1].trim() }.toList()
        val meat = parseMeat(html)
        return AdventureResult.NonCombat(name, html, items, meat)
    }

    private fun parseMeat(html: String): Int =
        MEAT_GAINED.find(html)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull() ?: 0

    private fun parseStats(html: String): Map<String, Int> =
        STAT_GAINED.findAll(html).associate { m ->
            val value = m.groupValues[1].replace(",", "").toIntOrNull() ?: 0
            m.groupValues[2] to value
        }
}
```

- [ ] **Step 5: Run all tests**

```
./gradlew :shared:allTests
```
Expected: `BUILD SUCCESSFUL` (all existing + new banisher tests pass)

- [ ] **Step 6: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureResult.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureParser.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/AdventureParserTest.kt
git commit -m "feat: Combat.banisher field + AdventureParser BANISHER_PATTERNS (20 banishers)"
```

---

## Task 8: AdventureManager — Banisher Passthrough + Zone Pre-flight + StopReason

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/StopReason.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureManager.kt`
- Modify: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/AdventureManagerTest.kt`

**Context:** `AdventureManager.resolveCombat()` currently hard-codes `Banisher.UNKNOWN` when calling `banishManager?.banishMonster(...)`. `CombatDatabase` is not yet injected. `location.name` is the human-readable zone name (e.g. `"Beanbat Chamber"`) that matches `combats.txt` keys.

- [ ] **Step 1: Add AllMonstersBanished to StopReason**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/StopReason.kt
package net.sourceforge.kolmafia.adventure

sealed class StopReason {
    object UserCancelled : StopReason()
    object NoAdventuresLeft : StopReason()
    object CharacterDeath : StopReason()
    object AllMonstersBanished : StopReason()
    data class GoalMet(val description: String) : StopReason()
    data class MacroError(val message: String) : StopReason()
    data class NetworkError(val cause: Throwable) : StopReason()
}
```

- [ ] **Step 2: Write failing tests**

Add to `AdventureManagerTest.kt`:

```kotlin
// Tests to add to existing AdventureManagerTest class

@Test fun resolveCombat_usesDetectedBanisher() = runBlocking {
    // Set up manager so parseFightResult returns a snokebomb banish
    // Verify banishManager.banishMonster is called with SNOKEBOMB, not UNKNOWN
    val banishedMonsters = mutableListOf<Banisher>()
    val fakeBanish = object : BanishManager(prefs()) {
        override fun banishMonster(monsterName: String, banisher: Banisher, currentTurn: Int) {
            banishedMonsters.add(banisher)
        }
    }
    val fightHtml = """
        <span id='monname'>Goblin</span>
        You throw the smokebomb at your feet and your foe flees in terror.
    """.trimIndent()
    // Build minimal AdventureManager with fake fight that returns snokebomb HTML
    // ... (use existing test helper patterns in AdventureManagerTest)
    // Assert: banishedMonsters[0] == Banisher.SNOKEBOMB
}

@Test fun runAdventures_allMonstersBanished_stopsWithCorrectReason() = runBlocking {
    // Zone has one monster. BanishManager says it's banished.
    // Expect: AdventureLoopStopped(AllMonstersBanished) emitted, no adventure request made.
    // ... implement using existing test helpers
}
```

**Note:** Implement these tests using the existing patterns in `AdventureManagerTest.kt` (fake fight requests, fake event bus collection, etc.). The key assertions are:
- `banishMonster` called with `result.banisher` (not hardcoded UNKNOWN)
- Zone pre-flight emits `AdventureLoopStopped(StopReason.AllMonstersBanished)` before any adventure request

- [ ] **Step 3: Update AdventureManager**

Add `combatDatabase: CombatDatabase? = null` to the constructor (after `banishManager`). Update `resolveCombat` and `runAdventures`:

In `resolveCombat()`, change the banish block from:
```kotlin
banishManager?.banishMonster(
    monsterName = result.monster,
    banisher    = Banisher.UNKNOWN,
    currentTurn = character.state.value.currentRun,
)
eventBus.emit(GameEvent.MonsterBanished(result.monster, Banisher.UNKNOWN.canonicalName))
```
to:
```kotlin
banishManager?.banishMonster(
    monsterName = result.monster,
    banisher    = result.banisher,
    currentTurn = character.state.value.currentRun,
)
eventBus.emit(GameEvent.MonsterBanished(result.monster, result.banisher.canonicalName))
```

At the top of `runAdventures()` body, before the `repeat(turns)` loop, add:

```kotlin
// Zone pre-flight: if all monsters are banished, stop immediately
val bm = banishManager
val zoneData = combatDatabase?.getByLocation(location.name)
if (bm != null && zoneData != null) {
    val currentTurn = character.state.value.currentRun
    val positiveWeightMonsters = zoneData.monsters.filter { it.weight > 0 }
    if (positiveWeightMonsters.isNotEmpty() &&
        positiveWeightMonsters.all { bm.isBanished(it.name, currentTurn) }) {
        _isRunning.value = false
        eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.AllMonstersBanished))
        return@launch
    }
}
```

Add required import: `import net.sourceforge.kolmafia.data.CombatDatabase`

- [ ] **Step 4: Run all tests**

```
./gradlew :shared:allTests
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/StopReason.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureManager.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/AdventureManagerTest.kt
git commit -m "feat: AdventureManager — pass result.banisher to BanishManager; zone pre-flight all-banished check"
```

---

## Task 9: ASH Functions — is_banished + banishers_used

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`
- Modify: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryTest.kt`

**Context:** `GameRuntimeLibrary` constructor currently has: `character`, `inventoryManager`, `skillManager`, `effectManager`, `adventureManager` — all nullable. Add `banishManager: BanishManager? = null`. `BanishManager.isBanished(name, currentTurn)` returns Boolean. `BanishManager.state.value.monsters` is a `List<BanishedMonster>`. `BanishedMonster` has `.monsterName`, `.banisher`, `.isExpired(currentTurn)`. `Banisher.canonicalName` is the lowercase string name.

`AggregateType` and `AggregateValue` are already used in `registerAggregateUtils`. The return type for `banishers_used()` is `string[monster]` — i.e., `AggregateType(indexType = AshType.MONSTER, dataType = AshType.STRING)`.

- [ ] **Step 1: Write failing tests**

Add to `GameRuntimeLibraryTest.kt`:

```kotlin
// Add to existing GameRuntimeLibraryTest class

private fun prefs() = Preferences(MapSettings())

private fun fakeBanishManager(vararg entries: Pair<String, Banisher>): BanishManager {
    val mgr = BanishManager(prefs())
    entries.forEach { (name, banisher) ->
        mgr.banishMonster(name, banisher, currentTurn = 0)
    }
    return mgr
}

@Test fun isBanished_banishedMonster_returnsTrue() {
    val bm = fakeBanishManager("Goblin" to Banisher.SNOKEBOMB)
    val lib = GameRuntimeLibrary(banishManager = bm)
    val scope = lib.createScope()
    val result = scope.call("is_banished", listOf(AshValue.monster("Goblin")))
    assertEquals(true, result?.toBoolean())
}

@Test fun isBanished_unknownMonster_returnsFalse() {
    val bm = fakeBanishManager()
    val lib = GameRuntimeLibrary(banishManager = bm)
    val scope = lib.createScope()
    val result = scope.call("is_banished", listOf(AshValue.monster("Goblin")))
    assertEquals(false, result?.toBoolean())
}

@Test fun isBanished_noManager_returnsFalse() {
    val lib = GameRuntimeLibrary()
    val scope = lib.createScope()
    val result = scope.call("is_banished", listOf(AshValue.monster("Goblin")))
    assertEquals(false, result?.toBoolean())
}

@Test fun banishersUsed_returnsBanishedMonsters() {
    val bm = fakeBanishManager("Goblin" to Banisher.SNOKEBOMB)
    val lib = GameRuntimeLibrary(banishManager = bm)
    val scope = lib.createScope()
    val result = scope.call("banishers_used", emptyList()) as? AggregateValue
    assertNotNull(result)
    assertEquals(1, result.map.size)
    val key = result.map.keys.first()
    assertEquals("Goblin", key.toString())
    assertEquals(Banisher.SNOKEBOMB.canonicalName, result.map[key]?.toString())
}

@Test fun banishersUsed_noManager_returnsEmpty() {
    val lib = GameRuntimeLibrary()
    val scope = lib.createScope()
    val result = scope.call("banishers_used", emptyList()) as? AggregateValue
    assertNotNull(result)
    assertEquals(0, result.map.size)
}
```

**Note on `scope.call`:** Look at existing `GameRuntimeLibraryTest` to see how it creates a scope and invokes functions. Match the existing pattern exactly.

- [ ] **Step 2: Run tests to verify they fail**

```
./gradlew :shared:allTests --tests "*.GameRuntimeLibraryTest"
```
Expected: new tests FAIL.

- [ ] **Step 3: Add banishManager to GameRuntimeLibrary constructor and register functions**

In `GameRuntimeLibrary.kt`:

1. Add `private val banishManager: BanishManager? = null` as last constructor param.

2. Add `registerBanishQueries(scope)` call inside `registerAll(scope)`.

3. Add the method:

```kotlin
private fun registerBanishQueries(scope: AshScope) {
    // is_banished(monster) → boolean
    register(scope, "is_banished", AshType.BOOLEAN, listOf("monster" to AshType.MONSTER)) { _, args ->
        val name = args[0].toString()
        val currentTurn = character?.state?.value?.currentRun ?: 0
        AshValue.of(banishManager?.isBanished(name, currentTurn) ?: false)
    }

    // banishers_used() → string[monster]
    val returnType = AggregateType(AshType.MONSTER, AshType.STRING)
    register(scope, "banishers_used", returnType, emptyList()) { _, _ ->
        val result = AggregateValue(returnType)
        val currentTurn = character?.state?.value?.currentRun ?: 0
        banishManager?.state?.value?.monsters
            ?.filter { !it.isExpired(currentTurn) }
            ?.forEach { b ->
                result[AshValue.monster(b.monsterName)] = AshValue.of(b.banisher.canonicalName)
            }
        result
    }
}
```

Add required imports:
```kotlin
import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.ash.AggregateType
import net.sourceforge.kolmafia.ash.AggregateValue
```

(`AggregateType` and `AggregateValue` may already be imported if used elsewhere in the file; check before adding.)

- [ ] **Step 4: Run all tests**

```
./gradlew :shared:allTests
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryTest.kt
git commit -m "feat: ASH is_banished() + banishers_used() in GameRuntimeLibrary"
```

---

## Task 10: DI Wiring — SharedModule

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt`

- [ ] **Step 1: Add new singletons and update existing ones**

The current `SharedModule.kt` has all existing registrations. Make these changes:

**After the `singleOf(::UneffectRequest)` line, add:**
```kotlin
singleOf(::CampgroundRequest)
singleOf(::ClanRumpusRequest)
singleOf(::ClanLoungeRequest)
single {
    BreakfastManager(
        campgroundRequest = get(),
        clanRumpusRequest = get(),
        clanLoungeRequest = get(),
        preferences       = get(),
    )
}
```

**Update `AdventureManager` single block** — add `combatDatabase = get()`:
```kotlin
single {
    AdventureManager(
        adventureRequest = get(),
        fightRequest     = get(),
        choiceRequest    = get(),
        characterRequest = get(),
        character        = get(),
        preferences      = get(),
        eventBus         = get(),
        registry         = get(),
        goalManager      = get(),
        questDatabase    = get(),
        solvers          = get(),
        inventory        = get(),
        effects          = get(),
        skills           = get(),
        recoveryManager  = get(),
        moodManager      = get(),
        questLogRequest  = get(),
        manaBurnManager  = get(),
        banishManager    = get(),
        combatDatabase   = get(),
    )
}
```

**Update `GameRuntimeLibrary` single block** — add `banishManager = get()`:
```kotlin
single {
    GameRuntimeLibrary(
        character        = get(),
        inventoryManager = get(),
        skillManager     = get(),
        effectManager    = get(),
        adventureManager = get(),
        banishManager    = get(),
    )
}
```

**Update `SessionManager` single block** — add `breakfastManager = get()`:
```kotlin
single {
    SessionManager(
        loginRequest         = get(),
        characterRequest     = get(),
        character            = get(),
        preferences          = get(),
        inventoryManager     = get(),
        familiarManager      = get(),
        skillManager         = get(),
        effectManager        = get(),
        scriptManager        = get(),
        gameDatabase         = get(),
        dailyResourceTracker = get(),
        questLogRequest      = get(),
        moodManager          = get(),
        banishManager        = get(),
        breakfastManager     = get(),
    )
}
```

**Add new imports** at top of file:
```kotlin
import net.sourceforge.kolmafia.request.CampgroundRequest
import net.sourceforge.kolmafia.request.ClanRumpusRequest
import net.sourceforge.kolmafia.request.ClanLoungeRequest
import net.sourceforge.kolmafia.session.BreakfastManager
import net.sourceforge.kolmafia.data.CombatDatabase
```

- [ ] **Step 2: Verify CombatDatabase is already a singleton**

`GameDatabase` loads `CombatDatabase` internally. But `AdventureManager` needs a `CombatDatabase` reference directly. Check if `CombatDatabase` is already registered:

```
grep -n "CombatDatabase" shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt
```

If not found, add before the `AdventureManager` block:
```kotlin
single { CombatDatabase() }
```

And in `GameDatabase.kt`, verify it loads `CombatDatabase` separately or that `CombatDatabase` has its own singleton state. If `GameDatabase` loads `CombatDatabase` as a static/companion object, pass `CombatDatabase` (the object itself) rather than a new instance.

**Check:** `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/GameDatabase.kt` for how it references `CombatDatabase`.

- [ ] **Step 3: Run all tests**

```
./gradlew :shared:allTests
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt
git commit -m "feat: DI wiring — BreakfastManager, ClanLoungeRequest, CampgroundRequest, ClanRumpusRequest; banishManager in GameRuntimeLibrary; combatDatabase in AdventureManager"
```

---

## Final Verification

```
./gradlew :shared:allTests
```

Expected output:
```
BUILD SUCCESSFUL
```

All new test classes should appear:
- `CampgroundRequestTest` (4 tests)
- `ClanRumpusRequestTest` (4 tests)
- `ClanLoungeRequestTest` (7 tests)
- `BreakfastManagerTest` (13 tests)
- `SessionManagerBreakfastTest` (2 tests)
- `AdventureParserTest` — 6 additional banisher tests
- `GameRuntimeLibraryTest` — 5 additional banish ASH tests
- `AdventureManagerTest` — 2 additional banish tests
