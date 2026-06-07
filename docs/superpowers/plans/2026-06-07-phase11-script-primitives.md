# Phase 11: Script Primitives + Quick Wins — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver `visit_url` + `cli_execute` dispatch (unblocking most community scripts), `autosell_price`/`npc_price` pricing functions, `my_familiar()` fix, BreakfastManager guild manual HTTP call, and VillainLair/Rufus choice solvers.

**Architecture:** Extend `GameRuntimeLibrary` with `httpClient: HttpClient?` constructor param and two new extension files (`WebRequest.kt`, `Pricing.kt`); extend `NpcStoreDatabase` to parse the full 5-column item-price rows and build an item→price index; add `RufusManager` and two choice handler files; fix the `my_familiar()` bug in base `registerCharacterQueries()`; replace the `cli_execute` echo stub with an extensible dispatch table.

**Tech Stack:** Kotlin Multiplatform, Ktor `HttpClient` + `HttpResponse.bodyAsText()`, Koin DI, existing `regFn()` extension pattern, existing `ChoiceHandler`/`ChoiceHandlerRegistry` pattern.

**Spec corrections applied in this plan (do not follow the spec document for these items):**
- `cli_execute` dispatch uses `moodManager.executeActiveMood(effectState=..., skillState=..., charState=...)` — the spec incorrectly says `executeMood(...)`
- `NpcStoreDatabase` parser rewrites the full 5-column row format (storeName/storeKey/itemName/price/rowId); sets `storeType = "NPC"` for all entries (file has no type column)
- `VillainLairHandlers` uses `ctx.preferences.getString(...)` (plural) — the spec incorrectly says `ctx.preference.getString(...)` (which is an `Int`)
- `ChoiceHandlerRegistry` does NOT receive a `rufusManager` constructor param — `RufusHandlers.registerAll(r, get())` is wired in `SharedModule.kt`'s `also` block instead

---

## File Structure

### New Files

| File | Responsibility |
|------|---------------|
| `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.WebRequest.kt` | `registerWebRequests(scope)` — `visit_url` × 2 overloads |
| `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Pricing.kt` | `registerPricingQueries(scope)` — `autosell_price`, `npc_price` |
| `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/RufusManager.kt` | Stores/loads Rufus quest state; `chooseQuestOption()`, `confirmChoice()` |
| `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/choice/handlers/VillainLairHandlers.kt` | Choice handlers for 1260/1262 |
| `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/choice/handlers/RufusHandlers.kt` | Choice handlers for 1498/1499 |
| `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/NpcStoreItem.kt` | `data class NpcStoreItem(val itemName: String, val price: Int)` |
| `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryWebRequestTest.kt` | Tests for `visit_url` |
| `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryPricingTest.kt` | Tests for `autosell_price`, `npc_price` |
| `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/choice/handlers/VillainLairHandlersTest.kt` | Tests for VillainLair choice handlers |
| `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/RufusManagerTest.kt` | Tests for `RufusManager` |
| `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/choice/handlers/RufusHandlersTest.kt` | Tests for Rufus choice handlers |

### Modified Files

| File | Change |
|------|--------|
| `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt` | Add `httpClient: HttpClient?` param; calls to `registerWebRequests()` + `registerPricingQueries()` in `registerAll()`; fix `my_familiar()` at line 344; replace `cli_execute` echo stub with dispatch table |
| `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/NpcStoreDatabase.kt` | Rewrite parser for 5-column item-price rows; add `_itemPrices` map; add `fun npcPrice(itemName: String): Int` |
| `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/NpcStoreData.kt` | Add `items: List<NpcStoreItem>` field |
| `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/GameDatabase.kt` | Add `open fun npcPrice(itemName: String): Int` |
| `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/BreakfastManager.kt` | Add `useItemRequest: UseItemRequest` constructor param; replace detection stub in `readGuildManual()` with actual HTTP call |
| `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/preferences/Preferences.kt` | Add `RUFUS_QUEST_TYPE`, `RUFUS_QUEST_TARGET` constants (note: `GUILD_MANUAL_USED` already exists in Phase 9 branch) |
| `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt` | Add `singleOf(::RufusManager)`; add `httpClient` to `GameRuntimeLibrary` binding; add `singleOf(::BreakfastManager)` and `SessionManager` breakfast wiring; add `VillainLairHandlers.registerAll(r)` + `RufusHandlers.registerAll(r, get())` to `ChoiceHandlerRegistry` `also` block |

---

## Prerequisites

### Task 0: Merge Phase 9 branch

Phase 9 (`feature/phase9-breakfast-banish`) contains `BreakfastManager.kt`, `CampgroundRequest.kt`, `ClanRumpusRequest.kt`, `ClanLoungeRequest.kt`, and 17 new `Preferences` constants. These files are required by Task 7 (guild manual). Merge the branch before starting Phase 11.

- [ ] **Step 1: Check Phase 9 branch status**

```bash
git log --oneline feature/phase9-breakfast-banish | head -5
git diff master..feature/phase9-breakfast-banish --stat
```

- [ ] **Step 2: Merge Phase 9 into master**

```bash
git checkout master
git merge feature/phase9-breakfast-banish --no-ff -m "feat: merge Phase 9 — breakfast manager + banish tracking"
```

If there are conflicts, resolve them (unlikely since Phase 9 was done in isolation).

- [ ] **Step 3: Verify build passes**

```bash
./gradlew :shared:jvmTest
```

Expected: all tests pass.

- [ ] **Step 4: Commit the merge**

The `--no-ff` flag already creates a merge commit. Push if needed:

```bash
git push origin master
```

---

## Task 1: Preference key constants

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/preferences/Preferences.kt`

`GUILD_MANUAL_USED = "_guildManualUsed"` is already present from Phase 9. Only the two Rufus keys are new.

- [ ] **Step 1: Write the failing test**

Open (or create) `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/preferences/PreferencesTest.kt` and add:

```kotlin
@Test
fun `rufus quest type pref key matches desktop`() {
    assertEquals("_rufusQuestType", Preferences.RUFUS_QUEST_TYPE)
}

@Test
fun `rufus quest target pref key matches desktop`() {
    assertEquals("_rufusQuestTarget", Preferences.RUFUS_QUEST_TARGET)
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
./gradlew :shared:jvmTest --tests "*.PreferencesTest"
```

Expected: FAIL (unresolved reference).

- [ ] **Step 3: Add constants to Preferences.kt**

In the `companion object Keys` block, after the ManaBurn constants, add:

```kotlin
        // Rufus / Shadow Rift
        const val RUFUS_QUEST_TYPE   = "_rufusQuestType"    // string: "entity"|"artifact"|"monument"
        const val RUFUS_QUEST_TARGET = "_rufusQuestTarget"  // string: target name after quest accepted
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
./gradlew :shared:jvmTest --tests "*.PreferencesTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/preferences/Preferences.kt
git add shared/src/commonTest/kotlin/net/sourceforge/kolmafia/preferences/PreferencesTest.kt
git commit -m "feat: add Rufus quest preference key constants"
```

---

## Task 2: Fix `my_familiar()` return value

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt` (line ~344)
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryFamiliarTest.kt` (or nearest existing familiar test file)

**Bug:** The current implementation returns `state.familiarName` (the player-assigned nickname), not the familiar's species/race. The ASH `my_familiar()` function must return the familiar's type ("Hovering Sombrero", "Rockin' Robin", etc.), sourced from `FamiliarManager.state.value.activeFamiliar.race`.

`FamiliarData` is: `data class FamiliarData(val id: Int, val name: String, val race: String, ...)`

- [ ] **Step 1: Write the failing test**

Find the test file that tests `registerCharacterQueries` (typically in `GameRuntimeLibraryTest.kt` or a separate familiar-queries test). Add:

```kotlin
@Test
fun `my_familiar returns familiar race not given name`() {
    val library = GameRuntimeLibrary(
        character = mockCharacterWithFamiliar("SomeName", "Hovering Sombrero"),
        familiarManager = mockFamiliarManager(race = "Hovering Sombrero"),
    )
    val scope = AshScope()
    library.registerAll(scope)
    val result = scope.evaluate("my_familiar()")
    assertEquals("Hovering Sombrero", result.toString())
}

@Test
fun `my_familiar returns none when no active familiar`() {
    val library = GameRuntimeLibrary(
        character = mockCharacter(),
        familiarManager = mockFamiliarManager(race = null),
    )
    val scope = AshScope()
    library.registerAll(scope)
    val result = scope.evaluate("my_familiar()")
    assertEquals("none", result.toString())
}
```

Helper approach — use existing test helpers from the test file. If `mockFamiliarManager` doesn't exist, create a simple inline stub:

```kotlin
private fun mockFamiliarManager(race: String?): FamiliarManager {
    val mgr = FamiliarManager()
    if (race != null) {
        val data = FamiliarData(id = 1, name = "Bob", race = race)
        mgr.testSetState(FamiliarState(activeFamiliar = data))
    }
    return mgr
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
./gradlew :shared:jvmTest --tests "*familiar*"
```

Expected: FAIL (test returns player name or wrong value).

- [ ] **Step 3: Fix the implementation**

In `GameRuntimeLibrary.kt` around line 344, replace:

```kotlin
        register(scope, "my_familiar", AshType.FAMILIAR, emptyList()) { _, _ ->
            val state = character?.state?.value
            val name = if (state?.hasFamiliar == true) state.familiarName else "none"
            AshValue.familiar(name)
        }
```

with:

```kotlin
        register(scope, "my_familiar", AshType.FAMILIAR, emptyList()) { _, _ ->
            AshValue.familiar(
                familiarManager?.state?.value?.activeFamiliar?.race
                    ?.takeIf { it.isNotBlank() } ?: "none"
            )
        }
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
./gradlew :shared:jvmTest --tests "*familiar*"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt
git add shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/  # relevant test file
git commit -m "fix: my_familiar() returns familiar race from FamiliarManager, not character name"
```

---

## Task 3: `NpcStoreItem` data class + `NpcStoreDatabase` item-price parser

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/NpcStoreItem.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/NpcStoreData.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/NpcStoreDatabase.kt`
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/data/NpcStoreDatabaseTest.kt`

**Critical context:** The actual `npcstores.txt` format is:
```
2
storeName\tstoreKey\titemName\tprice\tROWxxx
```
Example: `Shadowy Store\tguildstore1\tye olde golde frontes\t1500\tROW522`

Every data row has **5 columns**: storeName, storeKey, itemName, price, rowId. There are no separate "store header" rows. The file begins with a version number on its own line (which is an integer, no tab).

The **current parser is wrong**: it reads parts[0]=storeKey, parts[1]=storeName, parts[2]=storeType — all three column assignments are incorrect. The file has no storeType column. Since this file only lists NPC stores, set `storeType = "NPC"` for all entries.

- [ ] **Step 1: Create `NpcStoreItem.kt`**

```kotlin
package net.sourceforge.kolmafia.data

data class NpcStoreItem(val itemName: String, val price: Int)
```

- [ ] **Step 2: Write failing test for item-price lookup**

```kotlin
package net.sourceforge.kolmafia.data

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NpcStoreDatabaseTest {

    @Test
    fun `npcPrice returns correct price for known item`() = runBlocking {
        NpcStoreDatabase.resetForTest()
        NpcStoreDatabase.loadFromText("""
            2
            Shadowy Store	guildstore1	ye olde golde frontes	1500	ROW522
            Shadowy Store	guildstore1	bejeweled accordion strap	2000	ROW523
        """.trimIndent())
        assertEquals(1500, NpcStoreDatabase.npcPrice("ye olde golde frontes"))
        assertEquals(2000, NpcStoreDatabase.npcPrice("bejeweled accordion strap"))
    }

    @Test
    fun `npcPrice returns 0 for unknown item`() = runBlocking {
        NpcStoreDatabase.resetForTest()
        NpcStoreDatabase.loadFromText("2")
        assertEquals(0, NpcStoreDatabase.npcPrice("nonexistent item"))
    }

    @Test
    fun `npcPrice is case-insensitive`() = runBlocking {
        NpcStoreDatabase.resetForTest()
        NpcStoreDatabase.loadFromText("""
            2
            Shadowy Store	guildstore1	Fancy Hat	500	ROW1
        """.trimIndent())
        assertEquals(500, NpcStoreDatabase.npcPrice("FANCY HAT"))
        assertEquals(500, NpcStoreDatabase.npcPrice("fancy hat"))
    }

    @Test
    fun `stores are parsed with correct key and name`() = runBlocking {
        NpcStoreDatabase.resetForTest()
        NpcStoreDatabase.loadFromText("""
            2
            Shadowy Store	guildstore1	ye olde golde frontes	1500	ROW522
        """.trimIndent())
        val store = NpcStoreDatabase.getByKey("guildstore1")
        assertEquals("guildstore1", store?.storeKey)
        assertEquals("Shadowy Store", store?.storeName)
    }

    @Test
    fun `all stores loaded from npcstores txt are NPC stores`() = runBlocking {
        NpcStoreDatabase.resetForTest()
        NpcStoreDatabase.loadFromText("""
            2
            Shadowy Store	guildstore1	ye olde golde frontes	1500	ROW522
        """.trimIndent())
        assertTrue(NpcStoreDatabase.npcStores().isNotEmpty())
        assertTrue(NpcStoreDatabase.coinmasters().isEmpty())
    }
}
```

- [ ] **Step 3: Run to confirm failure**

```bash
./gradlew :shared:jvmTest --tests "*.NpcStoreDatabaseTest"
```

Expected: FAIL (missing `resetForTest`, `loadFromText`, `npcPrice`).

- [ ] **Step 4: Update `NpcStoreData.kt`**

Add the `items` field (default empty to avoid breaking existing code):

```kotlin
package net.sourceforge.kolmafia.data

data class NpcStoreData(
    val storeKey: String,
    val storeName: String,
    val storeType: String,          // "NPC", "COIN", or "NPCCOIN"
    val items: List<NpcStoreItem> = emptyList()
) {
    val isNpc get() = storeType.contains("NPC")
    val isCoinmaster get() = storeType.contains("COIN")
}
```

- [ ] **Step 5: Rewrite `NpcStoreDatabase.kt`**

```kotlin
package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
object NpcStoreDatabase {
    private val _byKey   = mutableMapOf<String, NpcStoreData>()
    private val _byName  = mutableMapOf<String, NpcStoreData>()
    private val _itemPrices = mutableMapOf<String, Int>()   // lowercase item name → price
    private var loaded = false

    val byKey:  Map<String, NpcStoreData> get() = _byKey
    val byName: Map<String, NpcStoreData> get() = _byName

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/npcstores.txt").decodeToString()
        loadFromText(text)
    }

    /** Visible for testing — parses raw text without touching bundled resources. */
    internal fun loadFromText(text: String) {
        _byKey.clear()
        _byName.clear()
        _itemPrices.clear()

        val storeItems = mutableMapOf<String, MutableList<NpcStoreItem>>() // storeKey → items

        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            // Skip version-number-only lines (no tab present)
            if (!line.contains('\t')) continue

            val parts = line.split('\t')
            if (parts.size < 4) continue

            // Actual column layout in npcstores.txt:
            // [0] storeName   [1] storeKey   [2] itemName   [3] price   [4] ROWxxx
            val storeName = parts[0].trim()
            val storeKey  = parts[1].trim()
            val itemName  = parts[2].trim()
            val price     = parts[3].trim().toIntOrNull() ?: continue

            // Build or update the store entry (all stores in this file are NPC stores)
            if (!_byKey.containsKey(storeKey.lowercase())) {
                val entry = NpcStoreData(
                    storeKey  = storeKey,
                    storeName = storeName,
                    storeType = "NPC",
                )
                _byKey[storeKey.lowercase()]   = entry
                _byName[storeName.lowercase()] = entry
            }

            // Accumulate items per store
            storeItems.getOrPut(storeKey.lowercase()) { mutableListOf() }
                .add(NpcStoreItem(itemName, price))

            // Global item→price index (first occurrence wins if duplicate)
            _itemPrices.putIfAbsent(itemName.lowercase(), price)
        }

        // Attach item lists to store entries
        storeItems.forEach { (key, items) ->
            _byKey[key]?.let { existing ->
                _byKey[key] = existing.copy(items = items)
                existing.storeName.lowercase().let { nameKey ->
                    _byName[nameKey] = _byKey[key]!!
                }
            }
        }

        loaded = true
    }

    /** Returns the NPC price for `itemName`, or 0 if not sold by any NPC store. */
    fun npcPrice(itemName: String): Int = _itemPrices[itemName.lowercase()] ?: 0

    fun getByKey(key: String): NpcStoreData? = _byKey[key.lowercase()]
    fun getByName(name: String): NpcStoreData? = _byName[name.lowercase()]
    fun all(): List<NpcStoreData> = _byKey.values.toList()
    fun npcStores(): List<NpcStoreData> = _byKey.values.filter { it.isNpc }
    fun coinmasters(): List<NpcStoreData> = _byKey.values.filter { it.isCoinmaster }

    /** Test helper — resets state so tests are independent. */
    internal fun resetForTest() {
        _byKey.clear()
        _byName.clear()
        _itemPrices.clear()
        loaded = false
    }
}
```

- [ ] **Step 6: Run tests to confirm they pass**

```bash
./gradlew :shared:jvmTest --tests "*.NpcStoreDatabaseTest"
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/NpcStoreItem.kt
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/NpcStoreData.kt
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/NpcStoreDatabase.kt
git add shared/src/commonTest/kotlin/net/sourceforge/kolmafia/data/NpcStoreDatabaseTest.kt
git commit -m "feat: fix NpcStoreDatabase 5-column parser + add item-price index + npcPrice()"
```

---

## Task 4: `GameDatabase.npcPrice()` + `GameRuntimeLibrary.Pricing.kt`

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/GameDatabase.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Pricing.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryPricingTest.kt`

- [ ] **Step 1: Write failing tests for pricing ASH functions**

```kotlin
package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryPricingTest {

    /** Stub database that returns controlled item data without reading from files. */
    private class StubDatabase(
        private val itemsByName: Map<String, ItemData> = emptyMap(),
        private val npcPrices: Map<String, Int> = emptyMap(),
    ) : GameDatabase() {
        override fun item(name: String): ItemData? = itemsByName[name.lowercase()]
        override fun npcPrice(itemName: String): Int = npcPrices[itemName.lowercase()] ?: 0
    }

    private fun makeItem(name: String, autosell: Int): ItemData =
        ItemData(id = 1, name = name, autosellPrice = autosell,
                 /* fill remaining fields with safe defaults */ plural = "$name s",
                 image = "", descId = "", itemType = "",
                 consumed = "", accessGroup = "", tradeable = true, discardable = true,
                 quest = false, gift = false, candy = 0, segment = "", fullness = 0,
                 inebriety = 0, spleenHit = 0, quality = "", adventureRange = "")

    @Test
    fun `autosell_price returns correct price for known item`() {
        val item = makeItem("bottle of beer", autosell = 47)
        val db = StubDatabase(itemsByName = mapOf("bottle of beer" to item))
        val lib = GameRuntimeLibrary(gameDatabase = db)
        val scope = AshScope()
        lib.registerAll(scope)
        val result = scope.evaluate("""autosell_price(${'$'}item[bottle of beer])""")
        assertEquals(47L, result.toLong())
    }

    @Test
    fun `autosell_price returns 0 for unknown item`() {
        val lib = GameRuntimeLibrary(gameDatabase = StubDatabase())
        val scope = AshScope()
        lib.registerAll(scope)
        val result = scope.evaluate("""autosell_price(${'$'}item[none])""")
        assertEquals(0L, result.toLong())
    }

    @Test
    fun `npc_price returns correct price for NPC-sold item`() {
        val db = StubDatabase(npcPrices = mapOf("reagent potion" to 150))
        val lib = GameRuntimeLibrary(gameDatabase = db)
        val scope = AshScope()
        lib.registerAll(scope)
        val result = scope.evaluate("""npc_price(${'$'}item[reagent potion])""")
        assertEquals(150L, result.toLong())
    }

    @Test
    fun `npc_price returns 0 for item not sold by NPC`() {
        val lib = GameRuntimeLibrary(gameDatabase = StubDatabase())
        val scope = AshScope()
        lib.registerAll(scope)
        val result = scope.evaluate("""npc_price(${'$'}item[none])""")
        assertEquals(0L, result.toLong())
    }
}
```

> **Note on `makeItem`:** Check the actual `ItemData` constructor signature in `ItemData.kt` and supply the required fields. The example above is illustrative — adjust to match the real constructor.

- [ ] **Step 2: Run to confirm failure**

```bash
./gradlew :shared:jvmTest --tests "*.GameRuntimeLibraryPricingTest"
```

Expected: FAIL (unresolved `autosell_price`, `npc_price`).

- [ ] **Step 3: Add `npcPrice()` to `GameDatabase.kt`**

After the existing `fun packageItem(...)` accessor, add:

```kotlin
    open fun npcPrice(itemName: String): Int = NpcStoreDatabase.npcPrice(itemName)
```

- [ ] **Step 4: Create `GameRuntimeLibrary.Pricing.kt`**

```kotlin
package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerPricingQueries(scope: AshScope) {

    regFn(scope, "autosell_price", AshType.INT,
        listOf("it" to AshType.ITEM)) { _, args ->
        val price = gameDatabase?.item(args[0].toString())?.autosellPrice ?: 0
        AshValue.of(price.toLong())
    }

    regFn(scope, "npc_price", AshType.INT,
        listOf("it" to AshType.ITEM)) { _, args ->
        val price = gameDatabase?.npcPrice(args[0].toString()) ?: 0
        AshValue.of(price.toLong())
    }
}
```

- [ ] **Step 5: Register in `GameRuntimeLibrary.kt`**

In `registerAll()`, after `registerItemActions(scope)`, add:

```kotlin
        registerPricingQueries(scope)
```

- [ ] **Step 6: Run tests to confirm they pass**

```bash
./gradlew :shared:jvmTest --tests "*.GameRuntimeLibraryPricingTest"
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/GameDatabase.kt
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Pricing.kt
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt
git add shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryPricingTest.kt
git commit -m "feat: add autosell_price and npc_price ASH functions"
```

---

## Task 5: `GameRuntimeLibrary.kt` — `httpClient` param + `cli_execute` dispatch

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryCliTest.kt`

**Context:** The `cli_execute` registration is at line ~482 in `registerGameActions()`. The dispatch table replaces the echo stub. The `MoodManager.executeActiveMood(effectState, skillState, charState)` signature requires importing `EffectState` and `SkillState`. The `httpClient` param is only used by `GameRuntimeLibrary.WebRequest.kt` (added in Task 6), but it must be declared here so both tasks compile.

The correct `executeActiveMood` call (matching `AdventureManager.kt` exactly):
```kotlin
mood.executeActiveMood(
    effectState = effectManager?.state?.value ?: EffectState(),
    skillState  = skillManager?.state?.value  ?: SkillState(),
    charState   = character?.state?.value     ?: CharacterState(),
)
```

- [ ] **Step 1: Write failing tests for `cli_execute` dispatch**

Create `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryCliTest.kt`:

```kotlin
package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.mood.MoodManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillManager
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryCliTest {

    private fun makePrefs() = Preferences(MapSettings())

    @Test
    fun `cli_execute set command writes preference`() {
        val prefs = makePrefs()
        val lib = GameRuntimeLibrary(preferences = prefs)
        val scope = AshScope()
        lib.registerAll(scope)
        scope.evaluate("""cli_execute("set myPref=hello")""")
        assertEquals("hello", prefs.getString("myPref", ""))
    }

    @Test
    fun `cli_execute get command prints preference value`() {
        val prefs = makePrefs()
        prefs.setString("myKey", "myVal")
        val printed = mutableListOf<String>()
        val lib = GameRuntimeLibrary(preferences = prefs)
        val scope = AshScope()
        lib.registerAll(scope)
        val runtime = object : AshRuntime(scope) {
            override fun print(msg: String) { printed += msg }
        }
        runtime.execute("""cli_execute("get myKey")""")
        assertTrue(printed.any { it.contains("myVal") })
    }

    @Test
    fun `cli_execute unknown command echoes with cli prefix`() {
        val printed = mutableListOf<String>()
        val lib = GameRuntimeLibrary()
        val scope = AshScope()
        lib.registerAll(scope)
        val runtime = object : AshRuntime(scope) {
            override fun print(msg: String) { printed += msg }
        }
        runtime.execute("""cli_execute("frob the widget")""")
        assertTrue(printed.any { it.contains("frob the widget") })
    }

    @Test
    fun `cli_execute returns true`() {
        val lib = GameRuntimeLibrary()
        val scope = AshScope()
        lib.registerAll(scope)
        val result = scope.evaluate("""cli_execute("anything")""")
        assertTrue(result.toBoolean())
    }
}
```

> **Note on `AshRuntime` instantiation:** Check the actual test infrastructure in the project. If a `scope.evaluate(...)` helper already captures print output, prefer that. Use whatever pattern the existing `GameRuntimeLibrary*Test.kt` files use for capturing runtime output.

- [ ] **Step 2: Run to confirm failure**

```bash
./gradlew :shared:jvmTest --tests "*.GameRuntimeLibraryCliTest"
```

Expected: FAIL (tests reference dispatch behavior not yet implemented).

- [ ] **Step 3: Add `httpClient` import + constructor param**

In `GameRuntimeLibrary.kt` imports section, add:

```kotlin
import io.ktor.client.HttpClient
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.skill.SkillState
```

In the constructor (after `storageRequest`), add:

```kotlin
    internal val httpClient: HttpClient? = null,
```

- [ ] **Step 4: Replace the `cli_execute` echo stub with dispatch table**

Add as a private property of `GameRuntimeLibrary` (above `registerAll`):

```kotlin
    private val cliDispatch: List<Pair<Regex, (MatchResult, AshRuntime) -> Unit>> = listOf(

        // "mood execute" — run missing triggers for active mood
        Regex("^mood\\s+execute$", RegexOption.IGNORE_CASE) to { _, _ ->
            moodManager?.let { mood ->
                kotlinx.coroutines.runBlocking {
                    mood.executeActiveMood(
                        effectState = effectManager?.state?.value ?: EffectState(),
                        skillState  = skillManager?.state?.value  ?: SkillState(),
                        charState   = character?.state?.value     ?: CharacterState(),
                    )
                }
            }
        },

        // "mood <name>" — set active mood by name, then execute
        Regex("^mood\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val name = m.groupValues[1].trim()
            moodManager?.setActiveMoodByName(name)
            moodManager?.let { mood ->
                kotlinx.coroutines.runBlocking {
                    mood.executeActiveMood(
                        effectState = effectManager?.state?.value ?: EffectState(),
                        skillState  = skillManager?.state?.value  ?: SkillState(),
                        charState   = character?.state?.value     ?: CharacterState(),
                    )
                }
            }
        },

        // "set key=value" — write a preference string
        Regex("^set\\s+(.+?)\\s*=\\s*(.*)$") to { m, _ ->
            preferences?.setString(m.groupValues[1].trim(), m.groupValues[2])
        },

        // "get key" — read and print a preference string
        Regex("^get\\s+(.+)$") to { m, rt ->
            val value = preferences?.getString(m.groupValues[1].trim(), "") ?: ""
            rt.print(value)
        },
    )
```

Replace the existing `cli_execute` registration block (lines ~482–487) with:

```kotlin
        register(scope, "cli_execute", AshType.BOOLEAN, listOf("cmd" to AshType.STRING)) { runtime, args ->
            val cmd = args[0].toString()
            val matched = cliDispatch.firstOrNull { (regex, _) -> regex.matches(cmd) }
            if (matched != null) {
                matched.second(matched.first.find(cmd)!!, runtime)
            } else {
                runtime.print("[cli] $cmd")   // unknown command: echo fallback
            }
            AshValue.of(true)
        }
```

- [ ] **Step 5: Run tests to confirm they pass**

```bash
./gradlew :shared:jvmTest --tests "*.GameRuntimeLibraryCliTest"
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt
git add shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryCliTest.kt
git commit -m "feat: add httpClient param to GameRuntimeLibrary + cli_execute dispatch table"
```

---

## Task 6: `GameRuntimeLibrary.WebRequest.kt` — `visit_url`

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.WebRequest.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryWebRequestTest.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt` (add `registerWebRequests` call)

**`KOL_BASE_URL`** is defined in `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/http/KoLHttpClient.kt` as `const val KOL_BASE_URL = "https://www.kingdomofloathing.com"`.

**Error contract:** Any HTTP response (including 4xx/5xx) → return body. Network error (exception) → return `""`. Null httpClient → return `""`.

**`encoded` param:** `false` → prepend `KOL_BASE_URL/`; `true` → use URL as-is.

- [ ] **Step 1: Write failing tests**

```kotlin
package net.sourceforge.kolmafia.ash

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryWebRequestTest {

    private fun mockClient(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = "ok",
        throwOnRequest: Boolean = false,
    ): HttpClient = HttpClient(MockEngine { _ ->
        if (throwOnRequest) throw RuntimeException("network error")
        respond(
            content = ByteReadChannel(body),
            status = status,
            headers = headersOf(HttpHeaders.ContentType, "text/html"),
        )
    })

    private fun library(client: HttpClient?) = GameRuntimeLibrary(httpClient = client)

    @Test
    fun `visit_url returns response body on 200`() {
        val lib = library(mockClient(body = "<html>hello</html>"))
        val scope = AshScope()
        lib.registerAll(scope)
        val result = scope.evaluate("""visit_url("inventory.php?action=get")""")
        assertEquals("<html>hello</html>", result.toString())
    }

    @Test
    fun `visit_url encoded=false prepends KOL_BASE_URL`() {
        var capturedUrl = ""
        val client = HttpClient(MockEngine { req ->
            capturedUrl = req.url.toString()
            respond(content = ByteReadChannel(""), status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html"))
        })
        val lib = library(client)
        val scope = AshScope()
        lib.registerAll(scope)
        scope.evaluate("""visit_url("inventory.php", false)""")
        assertTrue(capturedUrl.startsWith("https://www.kingdomofloathing.com"))
        assertTrue(capturedUrl.contains("inventory.php"))
    }

    @Test
    fun `visit_url encoded=true uses URL as-is`() {
        var capturedUrl = ""
        val client = HttpClient(MockEngine { req ->
            capturedUrl = req.url.toString()
            respond(content = ByteReadChannel(""), status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html"))
        })
        val lib = library(client)
        val scope = AshScope()
        lib.registerAll(scope)
        scope.evaluate("""visit_url("https://api.example.com/data", true)""")
        assertTrue(capturedUrl.startsWith("https://api.example.com"))
    }

    @Test
    fun `visit_url returns body on 404`() {
        val lib = library(mockClient(status = HttpStatusCode.NotFound, body = "not found"))
        val scope = AshScope()
        lib.registerAll(scope)
        val result = scope.evaluate("""visit_url("missing.php")""")
        assertEquals("not found", result.toString())
    }

    @Test
    fun `visit_url returns empty string on network error`() {
        val lib = library(mockClient(throwOnRequest = true))
        val scope = AshScope()
        lib.registerAll(scope)
        val result = scope.evaluate("""visit_url("inventory.php")""")
        assertEquals("", result.toString())
    }

    @Test
    fun `visit_url returns empty string when httpClient is null`() {
        val lib = library(null)
        val scope = AshScope()
        lib.registerAll(scope)
        val result = scope.evaluate("""visit_url("inventory.php")""")
        assertEquals("", result.toString())
    }
}
```

> **Ktor mock dependency check:** Verify `io.ktor:ktor-client-mock` is in the test dependencies in `shared/build.gradle.kts`. If it's already used by existing request tests, it's already present. If not, add:
> ```kotlin
> commonTestImplementation("io.ktor:ktor-client-mock:$ktorVersion")
> ```

- [ ] **Step 2: Run to confirm failure**

```bash
./gradlew :shared:jvmTest --tests "*.GameRuntimeLibraryWebRequestTest"
```

Expected: FAIL.

- [ ] **Step 3: Create `GameRuntimeLibrary.WebRequest.kt`**

```kotlin
package net.sourceforge.kolmafia.ash

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.http.KOL_BASE_URL

internal fun GameRuntimeLibrary.registerWebRequests(scope: AshScope) {

    fun doVisit(url: String, encoded: Boolean): String {
        val client = httpClient ?: return ""
        val fullUrl = if (encoded) url
                      else "$KOL_BASE_URL/${url.trimStart('/')}"
        return runBlocking {
            try {
                val response = client.get(fullUrl)
                response.body<String>()
            } catch (e: Exception) {
                ""   // network failure → empty string (matches desktop contract)
            }
        }
    }

    regFn(scope, "visit_url", AshType.STRING,
        listOf("url" to AshType.STRING)) { _, args ->
        AshValue.of(doVisit(args[0].toString(), encoded = false))
    }

    regFn(scope, "visit_url", AshType.STRING,
        listOf("url" to AshType.STRING, "encoded" to AshType.BOOLEAN)) { _, args ->
        AshValue.of(doVisit(args[0].toString(), args[1].toBoolean()))
    }
}
```

- [ ] **Step 4: Register in `GameRuntimeLibrary.kt`**

In `registerAll()`, after `registerPricingQueries(scope)`, add:

```kotlin
        registerWebRequests(scope)
```

- [ ] **Step 5: Run tests to confirm they pass**

```bash
./gradlew :shared:jvmTest --tests "*.GameRuntimeLibraryWebRequestTest"
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.WebRequest.kt
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt
git add shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryWebRequestTest.kt
git commit -m "feat: add visit_url ASH function via GameRuntimeLibrary.WebRequest"
```

---

## Task 7: BreakfastManager guild manual HTTP call

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/BreakfastManager.kt`
- Modify: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/session/BreakfastManagerTest.kt`

**Context:** After merging Phase 9 (Task 0), `BreakfastManager.kt` exists. It has a `readGuildManual()` private method with a detection stub. The stub comment says "HTTP use is not yet implemented." The method:
1. Determines `manualId` (11/172/173) from char class
2. Checks inventory contains `manualId`
3. **Stub:** Does nothing further — no HTTP call yet

The fix: add `useItemRequest: UseItemRequest` constructor param, then replace the stub with `useItemRequest.use(manualId, 1)`.

`GUILD_MANUAL_USED = "_guildManualUsed"` already exists in `Preferences.kt` (added in Phase 9).

- [ ] **Step 1: Write failing tests**

Add to `BreakfastManagerTest.kt`:

```kotlin
@Test
fun `readGuildManual calls useItemRequest when manual is in inventory`() = runBlocking {
    val prefs = Preferences(MapSettings())
    val useReq = mockUseItemRequest(succeedFor = MUS_MANUAL_ID)
    val manager = BreakfastManager(
        campgroundRequest  = mockCampgroundRequest(),
        clanRumpusRequest  = mockClanRumpusRequest(),
        clanLoungeRequest  = mockClanLoungeRequest(),
        preferences        = prefs,
        useItemRequest     = useReq,
    )
    val charState = muscleCharState()
    val invState  = InventoryState(items = mapOf(MUS_MANUAL_ID to InventoryItem(MUS_MANUAL_ID, "Guild Manual", 1)))
    manager.runBreakfast(charState, invState)
    assertTrue(useReq.wasCalledWith(MUS_MANUAL_ID))
    assertTrue(prefs.getBoolean(Preferences.GUILD_MANUAL_USED, false))
}

@Test
fun `readGuildManual sets GUILD_MANUAL_USED on success`() = runBlocking {
    val prefs = Preferences(MapSettings())
    val useReq = mockUseItemRequest(succeedFor = MUS_MANUAL_ID)
    val manager = BreakfastManager(
        campgroundRequest  = mockCampgroundRequest(),
        clanRumpusRequest  = mockClanRumpusRequest(),
        clanLoungeRequest  = mockClanLoungeRequest(),
        preferences        = prefs,
        useItemRequest     = useReq,
    )
    manager.runBreakfast(muscleCharState(), inventoryWith(MUS_MANUAL_ID))
    assertTrue(prefs.getBoolean(Preferences.GUILD_MANUAL_USED, false))
}

@Test
fun `readGuildManual skips when GUILD_MANUAL_USED is already true`() = runBlocking {
    val prefs = Preferences(MapSettings())
    prefs.setBoolean(Preferences.GUILD_MANUAL_USED, true)
    val useReq = trackingUseItemRequest()
    val manager = BreakfastManager(
        campgroundRequest  = mockCampgroundRequest(),
        clanRumpusRequest  = mockClanRumpusRequest(),
        clanLoungeRequest  = mockClanLoungeRequest(),
        preferences        = prefs,
        useItemRequest     = useReq,
    )
    manager.runBreakfast(muscleCharState(), inventoryWith(MUS_MANUAL_ID))
    assertFalse(useReq.wasCalled())
}
```

> **Note on test helpers:** `mockUseItemRequest`, `muscleCharState`, `inventoryWith`, etc. — use whatever pattern the existing `BreakfastManagerTest.kt` uses for mocking. The test should adapt to the existing mock/stub infrastructure in the Phase 9 test file.

- [ ] **Step 2: Run to confirm failure**

```bash
./gradlew :shared:jvmTest --tests "*.BreakfastManagerTest"
```

Expected: FAIL (constructor missing `useItemRequest`).

- [ ] **Step 3: Add `useItemRequest` param and `useGuildManual()` to `BreakfastManager.kt`**

Update the constructor to add:
```kotlin
    private val useItemRequest: UseItemRequest,
```

After the existing imports, add:
```kotlin
import net.sourceforge.kolmafia.request.UseItemRequest
```

Add the new private method:
```kotlin
    private suspend fun useGuildManual(manualId: Int) {
        if (preferences.getBoolean(Preferences.GUILD_MANUAL_USED, false)) return
        val result = useItemRequest.use(manualId, 1)
        if (result.isSuccess) {
            preferences.setBoolean(Preferences.GUILD_MANUAL_USED, true)
        }
    }
```

Replace the stub comment in `readGuildManual()`:
```kotlin
        if (!inventoryState.items.containsKey(manualId)) return
        // Guild manual item is in inventory but HTTP use is not yet implemented.
        // Sentinel will be set once the actual inv_use.php call is implemented.
        // For now, this is a detection-only stub.
```
→
```kotlin
        if (!inventoryState.items.containsKey(manualId)) return
        useGuildManual(manualId)
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
./gradlew :shared:jvmTest --tests "*.BreakfastManagerTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/BreakfastManager.kt
git add shared/src/commonTest/kotlin/net/sourceforge/kolmafia/session/BreakfastManagerTest.kt
git commit -m "feat: BreakfastManager guild manual — call UseItemRequest.use() after detection"
```

---

## Task 8: VillainLairHandlers

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/choice/handlers/VillainLairHandlers.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/choice/handlers/VillainLairHandlersTest.kt`

**Context:** Choice 1260 and 1262 are the "Villain Lair" doors. The handler reads `_villainLairColor` from preferences via `ctx.preferences.getString(...)`. **Important:** `ChoiceContext.preferences` is the `Preferences` object (plural), NOT `ChoiceContext.preference` (which is an `Int` representing the user's saved preference for this choice).

The handler scans option numbers 1–6 to find the one whose surrounding HTML mentions the correct color.

- [ ] **Step 1: Write failing tests**

```kotlin
package net.sourceforge.kolmafia.adventure.choice.handlers

import net.sourceforge.kolmafia.adventure.choice.ChoiceContext
import net.sourceforge.kolmafia.adventure.choice.ChoiceHandlerRegistry
import net.sourceforge.kolmafia.preferences.Preferences
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VillainLairHandlersTest {

    private fun context(color: String, html: String): ChoiceContext {
        val prefs = Preferences(MapSettings())
        prefs.setString("_villainLairColor", color)
        return minimalChoiceContext(
            choiceId = 1260,
            responseText = html,
            preferences = prefs,
        )
    }

    @Test
    fun `returns option 1 when color matches option 1 HTML`() {
        val html = buildHtml(1 to "red door", 2 to "blue door", 3 to "green door")
        assertEquals(1, VillainLairHandlers.handlers[1260]!!.decide(context("red", html)))
    }

    @Test
    fun `returns option 2 when color matches option 2 HTML`() {
        val html = buildHtml(1 to "red door", 2 to "blue door", 3 to "green door")
        assertEquals(2, VillainLairHandlers.handlers[1260]!!.decide(context("blue", html)))
    }

    @Test
    fun `returns null when _villainLairColor is empty`() {
        val html = buildHtml(1 to "red door")
        assertNull(VillainLairHandlers.handlers[1260]!!.decide(context("", html)))
    }

    @Test
    fun `returns null when color not found in any option HTML`() {
        val html = buildHtml(1 to "red door", 2 to "blue door")
        assertNull(VillainLairHandlers.handlers[1260]!!.decide(context("purple", html)))
    }

    @Test
    fun `handler 1262 uses same logic as 1260`() {
        val html = buildHtml(1 to "red door", 2 to "green door")
        val prefs = Preferences(MapSettings())
        prefs.setString("_villainLairColor", "green")
        val ctx = minimalChoiceContext(choiceId = 1262, responseText = html, preferences = prefs)
        assertEquals(2, VillainLairHandlers.handlers[1262]!!.decide(ctx))
    }

    @Test
    fun `registerAll registers both choices`() {
        val registry = ChoiceHandlerRegistry()
        VillainLairHandlers.registerAll(registry)
        assertNotNull(registry.handlerFor(1260))
        assertNotNull(registry.handlerFor(1262))
    }

    // Builds fake HTML with option markers matching the ChoiceHandler scan pattern
    private fun buildHtml(vararg options: Pair<Int, String>): String =
        options.joinToString("") { (num, text) ->
            """<input name=whichchoice value=$num> $text """
        }
}
```

> **`minimalChoiceContext`:** Check existing handler test files for a factory helper. If none exists, create a minimal stub. All `ChoiceContext` fields you don't need can use safe defaults. See `ChoiceContext` constructor at `adventure/choice/ChoiceContext.kt` for required fields.
>
> **`registry.handlerFor(id)`:** Check if `ChoiceHandlerRegistry` exposes a lookup method or if you need to dispatch a test context and check the result.

- [ ] **Step 2: Run to confirm failure**

```bash
./gradlew :shared:jvmTest --tests "*.VillainLairHandlersTest"
```

Expected: FAIL.

- [ ] **Step 3: Create `VillainLairHandlers.kt`**

```kotlin
package net.sourceforge.kolmafia.adventure.choice.handlers

import net.sourceforge.kolmafia.adventure.choice.ChoiceHandler
import net.sourceforge.kolmafia.adventure.choice.ChoiceHandlerRegistry

object VillainLairHandlers {

    private fun colorHandler(): ChoiceHandler = handler@{ ctx ->
        val color = ctx.preferences.getString("_villainLairColor", "").lowercase()
        if (color.isEmpty()) return@handler null
        // Scan options 1–6; find the one whose surrounding HTML fragment mentions the color
        (1..6).firstOrNull { option ->
            val marker  = "name=whichchoice value=$option"
            val fragment = ctx.responseText
                .substringAfter(marker, "")
                .substringBefore("name=whichchoice", "")
            fragment.contains(color, ignoreCase = true)
        }
    }

    val handlers: Map<Int, ChoiceHandler> = mapOf(
        1260 to colorHandler(),
        1262 to colorHandler(),
    )

    fun registerAll(registry: ChoiceHandlerRegistry) =
        handlers.forEach { (id, h) -> registry.register(id, h) }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
./gradlew :shared:jvmTest --tests "*.VillainLairHandlersTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/choice/handlers/VillainLairHandlers.kt
git add shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/choice/handlers/VillainLairHandlersTest.kt
git commit -m "feat: VillainLairHandlers — choices 1260/1262 color-matching solver"
```

---

## Task 9: RufusManager

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/RufusManager.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/RufusManagerTest.kt`

**Context:** Choices 1498 and 1499 are the Shadow Rift / Rufus quest choices. Choice 1498 lets you pick the quest type (entity/artifact/monument). The option-to-type mapping (1=entity, 2=artifact, 3=monument) matches KoLmafia desktop's ChoiceManager. Choice 1499 is the confirmation — always option 1.

`RUFUS_QUEST_TYPE` and `RUFUS_QUEST_TARGET` were added to `Preferences.kt` in Task 1.

- [ ] **Step 1: Write failing tests**

```kotlin
package net.sourceforge.kolmafia.adventure

import net.sourceforge.kolmafia.preferences.Preferences
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RufusManagerTest {

    private fun manager(questType: String): RufusManager {
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.RUFUS_QUEST_TYPE, questType)
        return RufusManager(prefs)
    }

    @Test
    fun `chooseQuestOption returns 1 for entity quest when HTML contains entity`() {
        val html = "<option value=1>Seek entity</option><option value=2>Seek artifact</option>"
        assertEquals(1, manager("entity").chooseQuestOption(html))
    }

    @Test
    fun `chooseQuestOption returns 2 for artifact quest when HTML contains artifact`() {
        val html = "<option value=1>Seek entity</option><option value=2>Find artifact</option>"
        assertEquals(2, manager("artifact").chooseQuestOption(html))
    }

    @Test
    fun `chooseQuestOption returns 3 for monument quest when HTML contains monument`() {
        val html = "<option value=3>Build monument</option>"
        assertEquals(3, manager("monument").chooseQuestOption(html))
    }

    @Test
    fun `chooseQuestOption returns null when type not found in HTML`() {
        val html = "<option value=1>Seek entity</option>"
        assertNull(manager("artifact").chooseQuestOption(html))
    }

    @Test
    fun `confirmChoice always returns 1`() {
        assertEquals(1, manager("entity").confirmChoice())
        assertEquals(1, manager("artifact").confirmChoice())
        assertEquals(1, manager("monument").confirmChoice())
    }

    @Test
    fun `recordQuestTarget persists target in preferences`() {
        val prefs = Preferences(MapSettings())
        val mgr = RufusManager(prefs)
        mgr.recordQuestTarget("Shadow Guy")
        assertEquals("Shadow Guy", prefs.getString(Preferences.RUFUS_QUEST_TARGET, ""))
    }

    @Test
    fun `questType defaults to entity when pref not set`() {
        val prefs = Preferences(MapSettings())
        val mgr = RufusManager(prefs)
        assertEquals("entity", mgr.questType)
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
./gradlew :shared:jvmTest --tests "*.RufusManagerTest"
```

Expected: FAIL.

- [ ] **Step 3: Create `RufusManager.kt`**

```kotlin
package net.sourceforge.kolmafia.adventure

import net.sourceforge.kolmafia.preferences.Preferences

class RufusManager(private val preferences: Preferences) {

    val questType: String
        get() = preferences.getString(Preferences.RUFUS_QUEST_TYPE, "entity").lowercase()

    /**
     * Choice 1498: maps the desired quest type to an option number.
     * Desktop option order: 1=entity, 2=artifact, 3=monument.
     * Returns null if the desired type's label is not found in [responseText].
     */
    fun chooseQuestOption(responseText: String): Int? {
        val desired = questType
        val optionLabels = listOf(
            "entity"   to 1,
            "artifact" to 2,
            "monument" to 3,
        )
        return optionLabels
            .firstOrNull { (label, _) ->
                label == desired && responseText.contains(label, ignoreCase = true)
            }
            ?.second
    }

    /** Choice 1499: always confirm (option 1). */
    fun confirmChoice(): Int = 1

    /** Store the target name after quest is accepted (extracted from post-choice response). */
    fun recordQuestTarget(target: String) {
        preferences.setString(Preferences.RUFUS_QUEST_TARGET, target)
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
./gradlew :shared:jvmTest --tests "*.RufusManagerTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/RufusManager.kt
git add shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/RufusManagerTest.kt
git commit -m "feat: RufusManager — quest type preferences + choice option mapping"
```

---

## Task 10: RufusHandlers

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/choice/handlers/RufusHandlers.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/choice/handlers/RufusHandlersTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
package net.sourceforge.kolmafia.adventure.choice.handlers

import net.sourceforge.kolmafia.adventure.RufusManager
import net.sourceforge.kolmafia.adventure.choice.ChoiceHandlerRegistry
import net.sourceforge.kolmafia.preferences.Preferences
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RufusHandlersTest {

    private fun registryWithRufus(questType: String): Pair<ChoiceHandlerRegistry, RufusManager> {
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.RUFUS_QUEST_TYPE, questType)
        val rufus = RufusManager(prefs)
        val registry = ChoiceHandlerRegistry()
        RufusHandlers.registerAll(registry, rufus)
        return registry to rufus
    }

    @Test
    fun `choice 1498 delegates to chooseQuestOption`() {
        val (registry, _) = registryWithRufus("artifact")
        val html = "<option value=1>Seek entity</option><option value=2>Find artifact</option>"
        val ctx = minimalChoiceContext(choiceId = 1498, responseText = html)
        val result = registry.dispatch(ctx)
        assertEquals(2, result)
    }

    @Test
    fun `choice 1498 returns null when quest type not in HTML`() {
        val (registry, _) = registryWithRufus("monument")
        val html = "<option value=1>Seek entity</option>"
        val ctx = minimalChoiceContext(choiceId = 1498, responseText = html)
        assertNull(registry.dispatch(ctx))
    }

    @Test
    fun `choice 1499 always returns 1`() {
        val (registry, _) = registryWithRufus("entity")
        val ctx = minimalChoiceContext(choiceId = 1499, responseText = "")
        assertEquals(1, registry.dispatch(ctx))
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
./gradlew :shared:jvmTest --tests "*.RufusHandlersTest"
```

Expected: FAIL.

- [ ] **Step 3: Create `RufusHandlers.kt`**

```kotlin
package net.sourceforge.kolmafia.adventure.choice.handlers

import net.sourceforge.kolmafia.adventure.RufusManager
import net.sourceforge.kolmafia.adventure.choice.ChoiceHandlerRegistry

object RufusHandlers {

    fun registerAll(registry: ChoiceHandlerRegistry, rufusManager: RufusManager) {
        registry.register(1498) { ctx ->
            rufusManager.chooseQuestOption(ctx.responseText)
        }
        registry.register(1499) { _ ->
            rufusManager.confirmChoice()
        }
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
./gradlew :shared:jvmTest --tests "*.RufusHandlersTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/choice/handlers/RufusHandlers.kt
git add shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/choice/handlers/RufusHandlersTest.kt
git commit -m "feat: RufusHandlers — choices 1498/1499 Shadow Rift quest solver"
```

---

## Task 11: DI wiring — `SharedModule.kt`

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt`

This task wires all Phase 11 new singletons into Koin and updates the `ChoiceHandlerRegistry` `also` block. **No new tests** — the DI wiring is tested transitively by the full test suite.

**Changes needed:**
1. Add `singleOf(::RufusManager)` — constructor takes `Preferences`, already in graph
2. Add `singleOf(::BreakfastManager)` — constructor takes `CampgroundRequest`, `ClanRumpusRequest`, `ClanLoungeRequest`, `Preferences`, `UseItemRequest`; all are in the graph after Phase 9 merge
3. Update `GameRuntimeLibrary` binding to add `httpClient = get()`
4. Update `ChoiceHandlerRegistry` `also` block to add `VillainLairHandlers.registerAll(r)` and `RufusHandlers.registerAll(r, get())`
5. Wire `BreakfastManager` into `SessionManager` so it can be called during login
6. Add missing imports

- [ ] **Step 1: Add new imports to `SharedModule.kt`**

Add to the imports:
```kotlin
import net.sourceforge.kolmafia.adventure.RufusManager
import net.sourceforge.kolmafia.adventure.choice.handlers.VillainLairHandlers
import net.sourceforge.kolmafia.adventure.choice.handlers.RufusHandlers
import net.sourceforge.kolmafia.session.BreakfastManager
```

- [ ] **Step 2: Add `singleOf(::RufusManager)` before the `ChoiceHandlerRegistry` block**

After `singleOf(::BanishManager)`, add:
```kotlin
    singleOf(::RufusManager)
```

- [ ] **Step 3: Update `ChoiceHandlerRegistry` `also` block**

In the existing block, after `SolverHandlers.registerAll(r)`, add:
```kotlin
            VillainLairHandlers.registerAll(r)
            RufusHandlers.registerAll(r, get())
```

- [ ] **Step 4: Update `GameRuntimeLibrary` binding to add `httpClient`**

In the `single { GameRuntimeLibrary(...) }` block, after `storageRequest = get(),`, add:
```kotlin
            httpClient        = get(),
```

- [ ] **Step 5: Add `singleOf(::BreakfastManager)` after the other request singles**

After `singleOf(::StorageRequest)`, add:
```kotlin
    singleOf(::BreakfastManager)
```

- [ ] **Step 6: Run the full test suite to confirm wiring is correct**

```bash
./gradlew :shared:jvmTest
```

Expected: all tests pass. If you get a Koin injection error, check that all constructor params of `BreakfastManager` and `RufusManager` are bound in the module. If `SessionManager` needs `BreakfastManager`, add it to the `SessionManager` constructor params and binding.

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt
git commit -m "feat: wire Phase 11 singletons — RufusManager, BreakfastManager, httpClient, VillainLair/RufusHandlers"
```

---

## Final Verification

- [ ] **Run full test suite**

```bash
./gradlew :shared:jvmTest
```

Expected: all tests pass. Note the count — Phase 11 should add ~25–35 new passing tests.

- [ ] **Build for all targets**

```bash
./gradlew :shared:build
```

Expected: no compile errors on any target.

- [ ] **Commit any remaining changes**

If any files were updated during verification but not yet committed, commit them now.

---

## Self-Review Checklist

**Spec coverage:**
- ✅ `visit_url` (1 overload + `encoded` overload) — Task 6
- ✅ `my_familiar()` fix — Task 2
- ✅ `cli_execute` dispatch table (mood execute/name, set, get) — Task 5
- ✅ `autosell_price` — Task 4
- ✅ `npc_price` — Task 4
- ✅ `NpcStoreDatabase` item-price parser — Task 3
- ✅ `GameDatabase.npcPrice()` — Task 4
- ✅ `BreakfastManager` guild manual HTTP call — Task 7
- ✅ `VillainLairHandlers` (1260/1262) — Task 8
- ✅ `RufusManager` — Task 9
- ✅ `RufusHandlers` (1498/1499) — Task 10
- ✅ Preference key constants — Task 1
- ✅ DI wiring — Task 11
- ✅ Phase 9 merge prerequisite — Task 0

**Corrections vs spec:**
1. `cli_execute` uses `executeActiveMood(effectState=..., skillState=..., charState=...)` ✅
2. `VillainLairHandlers` uses `ctx.preferences.getString(...)` (not `ctx.preference`) ✅
3. `ChoiceHandlerRegistry` gets no new constructor param; `RufusHandlers.registerAll(r, get())` called in SharedModule `also` block ✅
4. `NpcStoreDatabase` parser fixed to 5-column row format with `storeType = "NPC"` hardcoded ✅
