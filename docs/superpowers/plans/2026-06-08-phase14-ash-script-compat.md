# Phase 14: ASH Script Compatibility Pack — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the highest-impact ASH script compatibility gaps that cause community scripts to crash or silently misbehave: `to_int()` entity overloads, `wait`/`waitq`, logging variants, `hermit()` count-first overload, live `under_standard()`, `can_adventure()`/`prepare_for_adventure()`, `adv1()`, and `cli_execute` cast/familiar expansion.

**Architecture:** All work is isolated to the ASH layer (`ash/` package) and two small model files. No new HTTP request classes, no DI changes. Each task adds `regFn` calls to an existing or new `GameRuntimeLibrary.*.kt` extension file and a matching test file. The one model change (`CharacterState.isUnderStandard` + `AscensionPath.STANDARD`) has zero downstream impact — it only adds a computed property to an existing immutable data class.

**Tech Stack:** Kotlin Multiplatform, KMP commonMain/commonTest, `./gradlew :shared:jvmTest` to verify after each task.

---

## Codebase Context

This is the **KoLmafia Mobile** project at `/c/Development/kolmafia-mobile/`. All source is in
`shared/src/commonMain/kotlin/net/sourceforge/kolmafia/` (abbreviated `…/kolmafia/` below).
All tests are in `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/`.

**How ASH functions are registered** (the pattern every task follows):
```kotlin
// In GameRuntimeLibrary.*.kt (extension file):
internal fun GameRuntimeLibrary.registerXxx(scope: AshScope) {
    regFn(scope, "function_name", AshType.RETURN_TYPE,
        listOf("param1" to AshType.PARAM1_TYPE, "param2" to AshType.PARAM2_TYPE)) { runtime, args ->
        // args[0] is param1, args[1] is param2, etc.
        // runtime.print(msg) for output
        AshValue.of(someResult)   // or AshValue.VOID, AshValue(AshType.TYPE, "str"), etc.
    }
}
```

**Extension files must be wired in `GameRuntimeLibrary.kt`:**
```kotlin
// In GameRuntimeLibrary.kt registerAll():
override fun registerAll(scope: AshScope) {
    super.registerAll(scope)
    // ... existing calls ...
    registerXxx(scope)   // add new call here
}
```

**Test helpers** (in `GameRuntimeLibraryTestHelpers.kt`):
```kotlin
fun runLib(lib: GameRuntimeLibrary, src: String): AshRuntime   // run ASH, return runtime
fun outputLib(lib: GameRuntimeLibrary, src: String): String    // run ASH, return printed output
fun prefs(): Preferences = Preferences(MapSettings())
```

**`GameRuntimeLibrary` constructor** takes all-nullable named params; `forTesting()` provides a zero-manager instance:
```kotlin
val lib = GameRuntimeLibrary.forTesting()               // no managers
val lib = GameRuntimeLibrary(character = KoLCharacter()) // with specific managers
val lib = GameRuntimeLibrary(preferences = prefs())      // with prefs
```

**AshValue constructors:**
```kotlin
AshValue.of(42L)                           // INT
AshValue.of(3.14)                          // FLOAT
AshValue.of(true)                          // BOOLEAN
AshValue.of("hello")                       // STRING
AshValue(AshType.ITEM, "item name")        // ITEM (string content is the entity name)
AshValue(AshType.SKILL, "skill name")      // SKILL
AshValue(AshType.EFFECT, "effect name")   // EFFECT
AshValue(AshType.FAMILIAR, "race name")   // FAMILIAR
AshValue(AshType.LOCATION, "loc name")    // LOCATION
AshValue(AshType.MONSTER, "monster name") // MONSTER
AshValue.VOID                              // VOID return
```

**Test command:** `./gradlew :shared:jvmTest 2>&1 | tail -6`

---

## File Structure

| File | Action | What changes |
|------|--------|-------------|
| `…/data/GameDatabase.kt` | Modify | Make `effect(name)`, `skill(name)`, `familiar(name)`, `monster(name)`, `zone(name)` `open` |
| `…/character/AscensionPath.kt` | Modify | Add `STANDARD("Standard")` enum entry |
| `…/character/CharacterState.kt` | Modify | Add `val isUnderStandard: Boolean` computed property |
| `…/ash/GameRuntimeLibrary.kt` | Modify | Add entity `to_int` overloads to `registerTypeConversions`; add `adv1` to `registerGameActions`; add cast/familiar `cliDispatch` patterns; call `registerTimingAndLogging` from `registerAll` |
| `…/ash/GameRuntimeLibrary.Character.kt` | Modify | Fix `under_standard()` stub → real value; add `can_adventure()`, `prepare_for_adventure()` |
| `…/ash/GameRuntimeLibrary.Hermit.kt` | Modify | Add `hermit(count: INT, item: ITEM)` count-first overload |
| `…/ash/GameRuntimeLibrary.Timing.kt` | **Create** | `wait`, `waitq`, `logprint`, `debugprint`, `traceprint` |
| `…/ash/GameRuntimeLibraryTest.kt` | Modify | Add entity `to_int` tests |
| `…/ash/GameRuntimeLibraryCharacterTest.kt` | Modify | Update `underStandard` test; add `can_adventure`, `prepare_for_adventure` tests |
| `…/ash/GameRuntimeLibraryCliTest.kt` | Modify | Add `cast` and `familiar` dispatch tests |
| `…/ash/GameRuntimeLibraryTimingTest.kt` | **Create** | Tests for wait/waitq/logprint/debugprint/traceprint |
| `…/ash/GameRuntimeLibraryHermitTest.kt` | **Create** | Tests for both hermit overloads |

---

## Task T1: `to_int()` entity overloads

**Purpose:** `to_int($item[Seal Tooth])`, `to_int($skill[Steal Accordion])`, `to_int($effect[...])`  etc. are
extremely common in community scripts. All currently return 0. Each implementation is a 1-2 line database lookup.

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/GameDatabase.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`
- Modify: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryTest.kt`

- [ ] **Step 1: Make GameDatabase lookup methods `open` so tests can inject fakes**

In `…/data/GameDatabase.kt`, change the following method declarations from plain `fun` to `open fun`:

```kotlin
// Before (non-open):
fun effect(id: Int) = EffectDatabase.getById(id)
fun effect(name: String) = EffectDatabase.getByName(name)

fun skill(id: Int) = SkillDefinitionDatabase.getById(id)
fun skill(name: String) = SkillDefinitionDatabase.getByName(name)
fun familiar(id: Int) = FamiliarDefinitionDatabase.getById(id)
fun familiar(name: String) = FamiliarDefinitionDatabase.getByName(name)
fun zone(locationName: String) = AdventureDatabase.getByName(locationName)
fun monster(id: Int) = MonsterDatabase.getById(id)
fun monster(name: String) = MonsterDatabase.getByName(name)
```

```kotlin
// After (open):
open fun effect(id: Int) = EffectDatabase.getById(id)
open fun effect(name: String) = EffectDatabase.getByName(name)

open fun skill(id: Int) = SkillDefinitionDatabase.getById(id)
open fun skill(name: String) = SkillDefinitionDatabase.getByName(name)
open fun familiar(id: Int) = FamiliarDefinitionDatabase.getById(id)
open fun familiar(name: String) = FamiliarDefinitionDatabase.getByName(name)
open fun zone(locationName: String) = AdventureDatabase.getByName(locationName)
open fun monster(id: Int) = MonsterDatabase.getById(id)
open fun monster(name: String) = MonsterDatabase.getByName(name)
```

- [ ] **Step 2: Write the failing tests**

Add to `GameRuntimeLibraryTest.kt` (after the existing `to_int` tests):

```kotlin
// ── to_int entity overloads ──────────────────────────────────────────────────

private fun itemDb(itemId: Int): GameDatabase = object : GameDatabase() {
    override fun item(name: String) = net.sourceforge.kolmafia.data.ItemData(
        id = itemId, name = name, descId = "", image = "",
        primaryUse = net.sourceforge.kolmafia.data.ItemPrimaryUse.NONE,
        secondaryUses = emptySet(), access = emptySet(),
        autosellPrice = 0, plural = null
    )
}

private fun effectDb(effectId: Int): GameDatabase = object : GameDatabase() {
    override fun effect(name: String) = net.sourceforge.kolmafia.data.EffectData(
        id = effectId, name = name, image = "", descId = "",
        quality = net.sourceforge.kolmafia.data.EffectQuality.UNKNOWN,
        attributes = emptySet()
    )
}

private fun skillDb(skillId: Int): GameDatabase = object : GameDatabase() {
    override fun skill(name: String) = net.sourceforge.kolmafia.data.SkillDefinition(
        id = skillId, name = name, image = "", tags = emptySet(),
        mpCost = 0, duration = 0, isPermable = true,
        isPassive = false, isCombat = false, isNonCombat = false, isSong = false
    )
}

private fun familiarDb(familiarId: Int): GameDatabase = object : GameDatabase() {
    override fun familiar(name: String) = net.sourceforge.kolmafia.data.FamiliarDefinition(
        id = familiarId, name = name, image = "", types = emptySet(),
        larvaItem = "", hatchlingItem = "", arenaCombatMoves = 0,
        arenaStrength = 0, arenaOc = 0, arenaHs = 0, attributes = emptySet()
    )
}

private fun monsterDb(monsterId: Int): GameDatabase = object : GameDatabase() {
    override fun monster(name: String) = net.sourceforge.kolmafia.data.MonsterDefinition(
        name = name, id = monsterId, image = "", attack = 0, defense = 0, hp = 0,
        initiative = 0, meatDrop = 0, phylum = "dude",
        isBoss = false, isGhost = false, isLucky = false
    )
}

private fun zoneDb(snarfblat: Int): GameDatabase = object : GameDatabase() {
    override fun zone(locationName: String) = net.sourceforge.kolmafia.data.AdventureZone(
        zoneName = "Test", urlParams = "adventure=$snarfblat",
        locationName = locationName, environment = "indoor",
        diffLevel = "low", statRequirement = 0, goals = emptyList(),
        isOverdrunk = false, noWander = false
    )
}

@Test
fun to_int_fromItem_returnsItemId() {
    val lib = GameRuntimeLibrary(gameDatabase = itemDb(42))
    assertEquals("42", outputLib(lib, """print(to_string(to_int($item[Seal Tooth])));"""))
}

@Test
fun to_int_fromEffect_returnsEffectId() {
    val lib = GameRuntimeLibrary(gameDatabase = effectDb(55))
    assertEquals("55", outputLib(lib, """print(to_string(to_int($effect[Beaten Up])));"""))
}

@Test
fun to_int_fromSkill_returnsSkillId() {
    val lib = GameRuntimeLibrary(gameDatabase = skillDb(28))
    assertEquals("28", outputLib(lib, """print(to_string(to_int($skill[Empathy of the Newt])));"""))
}

@Test
fun to_int_fromFamiliar_returnsFamiliarId() {
    val lib = GameRuntimeLibrary(gameDatabase = familiarDb(7))
    assertEquals("7", outputLib(lib, """print(to_string(to_int($familiar[Mosquito])));"""))
}

@Test
fun to_int_fromMonster_returnsMonsterId() {
    val lib = GameRuntimeLibrary(gameDatabase = monsterDb(17))
    assertEquals("17", outputLib(lib, """print(to_string(to_int($monster[Knob Goblin])));"""))
}

@Test
fun to_int_fromLocation_returnsSnarfblat() {
    val lib = GameRuntimeLibrary(gameDatabase = zoneDb(88))
    assertEquals("88", outputLib(lib, """print(to_string(to_int($location[The Haunted Ballroom])));"""))
}

@Test
fun to_int_fromItem_returnsZeroWhenDbNull() {
    // gameDatabase not provided — fallback to 0
    val lib = GameRuntimeLibrary.forTesting()
    assertEquals("0", outputLib(lib, """print(to_string(to_int($item[unknown item])));"""))
}
```

- [ ] **Step 3: Run tests to confirm they fail**

```bash
cd /c/Development/kolmafia-mobile && ./gradlew :shared:jvmTest 2>&1 | tail -6
```

Expected: BUILD FAILED — `to_int` entity overloads not yet registered.

- [ ] **Step 4: Add the 6 entity `to_int` overloads to `registerTypeConversions` in `GameRuntimeLibrary.kt`**

Add these registrations at the end of the `registerTypeConversions` private function, after the existing `to_string` loop (around line 198):

```kotlin
// to_int for game entity types — returns the entity's numeric database ID
// Returns 0 when gameDatabase is null (test/no-db context) or entity unknown.
register(scope, "to_int", AshType.INT, listOf("it" to AshType.ITEM)) { _, args ->
    AshValue.of(gameDatabase?.item(args[0].toString())?.id?.toLong() ?: 0L)
}
register(scope, "to_int", AshType.INT, listOf("ef" to AshType.EFFECT)) { _, args ->
    AshValue.of(gameDatabase?.effect(args[0].toString())?.id?.toLong() ?: 0L)
}
register(scope, "to_int", AshType.INT, listOf("sk" to AshType.SKILL)) { _, args ->
    AshValue.of(gameDatabase?.skill(args[0].toString())?.id?.toLong() ?: 0L)
}
register(scope, "to_int", AshType.INT, listOf("fa" to AshType.FAMILIAR)) { _, args ->
    AshValue.of(gameDatabase?.familiar(args[0].toString())?.id?.toLong() ?: 0L)
}
register(scope, "to_int", AshType.INT, listOf("loc" to AshType.LOCATION)) { _, args ->
    AshValue.of(
        gameDatabase?.zone(args[0].toString())
            ?.snarfblat?.toIntOrNull()?.toLong() ?: 0L
    )
}
register(scope, "to_int", AshType.INT, listOf("mo" to AshType.MONSTER)) { _, args ->
    AshValue.of(gameDatabase?.monster(args[0].toString())?.id?.toLong() ?: 0L)
}
```

- [ ] **Step 5: Run tests to confirm they pass**

```bash
cd /c/Development/kolmafia-mobile && ./gradlew :shared:jvmTest 2>&1 | tail -6
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
cd /c/Development/kolmafia-mobile && git add \
  shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/GameDatabase.kt \
  shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt \
  shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryTest.kt
git commit -m "feat: to_int() entity overloads — item, effect, skill, familiar, location, monster"
```

---

## Task T2: `wait` / `waitq` + `logprint` / `debugprint` / `traceprint`

**Purpose:** `wait(secs)` and `waitq(secs)` appear in virtually every pacing script. `logprint`, `debugprint`, `traceprint` replace silent no-ops with actual output (mobile has one output channel; routing all three to `print` is the correct minimum).

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Timing.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryTimingTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `GameRuntimeLibraryTimingTest.kt`:

```kotlin
package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryTimingTest {

    private val lib get() = GameRuntimeLibrary.forTesting()

    // wait / waitq — exist, accept int, return void, don't throw
    @Test
    fun wait_zeroSecondsDoesNotThrow() {
        // wait(0) should complete immediately without error
        runLib(lib, "wait(0);")
    }

    @Test
    fun waitq_zeroSecondsDoesNotThrow() {
        runLib(lib, "waitq(0);")
    }

    @Test
    fun wait_isCallableFromAsh() {
        // confirm the function is registered (would throw ScriptException if missing)
        runLib(lib, "int n = 0; wait(n);")
    }

    // logprint / debugprint / traceprint — route output to print channel
    @Test
    fun logprint_outputsMessage() {
        assertEquals("hello log", outputLib(lib, """logprint("hello log");"""))
    }

    @Test
    fun debugprint_outputsMessage() {
        assertEquals("debug msg", outputLib(lib, """debugprint("debug msg");"""))
    }

    @Test
    fun traceprint_outputsMessage() {
        assertEquals("trace msg", outputLib(lib, """traceprint("trace msg");"""))
    }

    @Test
    fun logprint_returnsVoid() {
        // Should compile as a statement (VOID return type)
        runLib(lib, """logprint("test"); print("ok");""")
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd /c/Development/kolmafia-mobile && ./gradlew :shared:jvmTest 2>&1 | tail -6
```

Expected: BUILD FAILED — `wait`, `waitq`, `logprint`, etc. not registered.

- [ ] **Step 3: Create `GameRuntimeLibrary.Timing.kt`**

```kotlin
package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

internal fun GameRuntimeLibrary.registerTimingAndLogging(scope: AshScope) {

    // wait(secs: int) — pauses the script for [secs] seconds.
    // Uses runBlocking + coroutine delay so it yields to the coroutine scheduler
    // rather than blocking a platform thread.
    regFn(scope, "wait", AshType.VOID, listOf("secs" to AshType.INT)) { _, args ->
        val ms = args[0].toLong() * 1000L
        if (ms > 0L) runBlocking { delay(ms) }
        AshValue.VOID
    }

    // waitq(secs: int) — same as wait; desktop distinction is logging verbosity only.
    regFn(scope, "waitq", AshType.VOID, listOf("secs" to AshType.INT)) { _, args ->
        val ms = args[0].toLong() * 1000L
        if (ms > 0L) runBlocking { delay(ms) }
        AshValue.VOID
    }

    // logprint / debugprint / traceprint — desktop routes these to different log channels.
    // Mobile has one output channel; all three route to the same print handler.
    regFn(scope, "logprint", AshType.VOID, listOf("msg" to AshType.STRING)) { runtime, args ->
        runtime.print(args[0].toString())
        AshValue.VOID
    }
    regFn(scope, "debugprint", AshType.VOID, listOf("msg" to AshType.STRING)) { runtime, args ->
        runtime.print(args[0].toString())
        AshValue.VOID
    }
    regFn(scope, "traceprint", AshType.VOID, listOf("msg" to AshType.STRING)) { runtime, args ->
        runtime.print(args[0].toString())
        AshValue.VOID
    }
}
```

- [ ] **Step 4: Wire into `registerAll` in `GameRuntimeLibrary.kt`**

Add `registerTimingAndLogging(scope)` at the end of the `override fun registerAll(scope: AshScope)` block (after `registerHermit(scope)`):

```kotlin
override fun registerAll(scope: AshScope) {
    super.registerAll(scope)
    // ... existing calls (don't remove them) ...
    registerHermit(scope)
    registerTimingAndLogging(scope)  // ← add this line
}
```

- [ ] **Step 5: Run tests to confirm they pass**

```bash
cd /c/Development/kolmafia-mobile && ./gradlew :shared:jvmTest 2>&1 | tail -6
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
cd /c/Development/kolmafia-mobile && git add \
  shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Timing.kt \
  shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt \
  shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryTimingTest.kt
git commit -m "feat: wait/waitq + logprint/debugprint/traceprint ASH functions"
```

---

## Task T3: `hermit()` count-first overload

**Purpose:** Desktop registers both `hermit(item, count)` and `hermit(count, item)`. Community scripts use both forms. Mobile only has item-first.

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Hermit.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryHermitTest.kt`

Current `GameRuntimeLibrary.Hermit.kt` content (for reference):

```kotlin
package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerHermit(scope: AshScope) {
    fun resolveItemId(itemName: String): Int? = gameDatabase?.item(itemName)?.id

    // hermit(it: item, n: int) → int   [item-first; matches desktop API]
    regFn(scope, "hermit", AshType.INT,
        listOf("it" to AshType.ITEM, "n" to AshType.INT)) { _, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(0L)
        val count  = args[1].toLong().toInt()
        if (count <= 0) return@regFn AshValue.of(0L)
        val req = hermitRequest ?: return@regFn AshValue.of(0L)
        val success = kotlinx.coroutines.runBlocking { req.trade(itemId, count) }.isSuccess
        AshValue.of(if (success) count.toLong() else 0L)
    }
}
```

- [ ] **Step 1: Write the failing test**

Create `GameRuntimeLibraryHermitTest.kt`:

```kotlin
package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.request.HermitRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryHermitTest {

    private val cloverItemId = 24

    private fun makeLib(tradeResult: Result<String>): Pair<GameRuntimeLibrary, MutableList<Pair<Int, Int>>> {
        val calls = mutableListOf<Pair<Int, Int>>()
        val fakeHermit = object : HermitRequest(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        ) {
            override suspend fun trade(itemId: Int, quantity: Int): Result<String> {
                calls.add(itemId to quantity)
                return tradeResult
            }
        }
        val fakeDb = object : GameDatabase() {
            override fun item(name: String) = ItemData(
                id = cloverItemId, name = name, descId = "", image = "",
                primaryUse = ItemPrimaryUse.NONE, secondaryUses = emptySet(),
                access = emptySet(), autosellPrice = 0, plural = null
            )
        }
        return GameRuntimeLibrary(hermitRequest = fakeHermit, gameDatabase = fakeDb) to calls
    }

    // hermit(item, count) — item-first (existing overload, regression test)
    @Test
    fun hermit_itemFirst_callsTradeAndReturnsCount() {
        val (lib, calls) = makeLib(Result.success("ok"))
        val out = outputLib(lib, """print(to_string(hermit($item[ten-leaf clover], 3)));""")
        assertEquals("3", out)
        assertEquals(listOf(cloverItemId to 3), calls)
    }

    // hermit(count, item) — count-first (new overload)
    @Test
    fun hermit_countFirst_callsTradeAndReturnsCount() {
        val (lib, calls) = makeLib(Result.success("ok"))
        val out = outputLib(lib, """print(to_string(hermit(2, $item[ten-leaf clover])));""")
        assertEquals("2", out)
        assertEquals(listOf(cloverItemId to 2), calls)
    }

    @Test
    fun hermit_countFirst_returnsZeroOnFailure() {
        val (lib, calls) = makeLib(Result.failure(Exception("network")))
        val out = outputLib(lib, """print(to_string(hermit(1, $item[ten-leaf clover])));""")
        assertEquals("0", out)
    }

    @Test
    fun hermit_countFirst_returnsZeroWhenCountIsZero() {
        val (lib, calls) = makeLib(Result.success("ok"))
        val out = outputLib(lib, """print(to_string(hermit(0, $item[ten-leaf clover])));""")
        assertEquals("0", out)
        assertEquals(emptyList<Pair<Int, Int>>(), calls, "Should not call trade when count is 0")
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd /c/Development/kolmafia-mobile && ./gradlew :shared:jvmTest 2>&1 | tail -6
```

Expected: BUILD FAILED — count-first overload not registered.

- [ ] **Step 3: Add count-first overload to `GameRuntimeLibrary.Hermit.kt`**

Add the second `regFn` call after the existing one:

```kotlin
package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerHermit(scope: AshScope) {

    fun resolveItemId(itemName: String): Int? = gameDatabase?.item(itemName)?.id

    // hermit(it: item, n: int) → int   [item-first; matches desktop hermit(item, count)]
    regFn(scope, "hermit", AshType.INT,
        listOf("it" to AshType.ITEM, "n" to AshType.INT)) { _, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(0L)
        val count  = args[1].toLong().toInt()
        if (count <= 0) return@regFn AshValue.of(0L)
        val req = hermitRequest ?: return@regFn AshValue.of(0L)
        val success = kotlinx.coroutines.runBlocking { req.trade(itemId, count) }.isSuccess
        AshValue.of(if (success) count.toLong() else 0L)
    }

    // hermit(n: int, it: item) → int   [count-first; matches desktop hermit(count, item)]
    regFn(scope, "hermit", AshType.INT,
        listOf("n" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val count  = args[0].toLong().toInt()
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(0L)
        if (count <= 0) return@regFn AshValue.of(0L)
        val req = hermitRequest ?: return@regFn AshValue.of(0L)
        val success = kotlinx.coroutines.runBlocking { req.trade(itemId, count) }.isSuccess
        AshValue.of(if (success) count.toLong() else 0L)
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
cd /c/Development/kolmafia-mobile && ./gradlew :shared:jvmTest 2>&1 | tail -6
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
cd /c/Development/kolmafia-mobile && git add \
  shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Hermit.kt \
  shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryHermitTest.kt
git commit -m "feat: hermit(count, item) count-first overload — parity with desktop ASH API"
```

---

## Task T4: `under_standard()` real value

**Purpose:** `under_standard()` always returns `false`. Standard season is a common path-gate for many automation scripts. The KoL API returns `path = "Standard"` for players in a Standard run; `AscensionPath.STANDARD` does not currently exist in the enum.

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/character/AscensionPath.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/character/CharacterState.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Character.kt`
- Modify: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryCharacterTest.kt`

- [ ] **Step 1: Write the failing tests**

Add to `GameRuntimeLibraryCharacterTest.kt` (before the class's closing brace):

```kotlin
@Test
fun underStandard_trueWhenPathIsStandard() {
    val lib = libWith { copy(path = "Standard") }
    assertEquals("true", outputLib(lib, "print(to_string(under_standard()));"))
}

@Test
fun underStandard_falseWhenPathIsNone() {
    val lib = libWith { copy(path = "None") }
    assertEquals("false", outputLib(lib, "print(to_string(under_standard()));"))
}

@Test
fun underStandard_falseWhenPathIsHardcore() {
    val lib = libWith { copy(path = "Hardcore") }
    assertEquals("false", outputLib(lib, "print(to_string(under_standard()));"))
}
```

The existing `underStandard_alwaysFalse` test should be **removed** (it will conflict with the new behavior). Delete these lines:

```kotlin
// DELETE this test:
@Test
fun underStandard_alwaysFalse() {
    assertEquals("false",
        outputLib(GameRuntimeLibrary.forTesting(), "print(to_string(under_standard()));"))
}
```

- [ ] **Step 2: Run tests to confirm the new tests fail**

```bash
cd /c/Development/kolmafia-mobile && ./gradlew :shared:jvmTest 2>&1 | tail -6
```

Expected: BUILD FAILED — `underStandard_trueWhenPathIsStandard` fails (returns "false").

- [ ] **Step 3: Add `STANDARD` to `AscensionPath.kt`**

Add the entry after `SURPRISING_FIST` and before `UNKNOWN`:

```kotlin
    // ── Standard season ─────────────────────────────────────────────────────────
    STANDARD("Standard"),

    UNKNOWN("Unknown");
```

- [ ] **Step 4: Add `isUnderStandard` to `CharacterState.kt`**

In the `// ── Computed: restriction / mode flags ──` section (around line 187–189), add after `val isInLimitMode`:

```kotlin
val isInLimitMode: Boolean get() = limitMode.isNotBlank()
val isUnderStandard: Boolean get() = ascensionPath == AscensionPath.STANDARD
```

- [ ] **Step 5: Update `under_standard()` in `GameRuntimeLibrary.Character.kt`**

Replace the stub:

```kotlin
// Before:
// Stub: CharacterState has no underStandard field
regFn(scope, "under_standard", AshType.BOOLEAN, emptyList()) { _, _ ->
    AshValue.of(false)
}
```

```kotlin
// After:
regFn(scope, "under_standard", AshType.BOOLEAN, emptyList()) { _, _ ->
    AshValue.of(character?.state?.value?.isUnderStandard ?: false)
}
```

- [ ] **Step 6: Run tests to confirm they pass**

```bash
cd /c/Development/kolmafia-mobile && ./gradlew :shared:jvmTest 2>&1 | tail -6
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
cd /c/Development/kolmafia-mobile && git add \
  shared/src/commonMain/kotlin/net/sourceforge/kolmafia/character/AscensionPath.kt \
  shared/src/commonMain/kotlin/net/sourceforge/kolmafia/character/CharacterState.kt \
  shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Character.kt \
  shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryCharacterTest.kt
git commit -m "feat: under_standard() reads AscensionPath.STANDARD — adds STANDARD path + CharacterState.isUnderStandard"
```

---

## Task T5: `can_adventure()` / `prepare_for_adventure()` / `adv1()`

**Purpose:** `can_adventure(location)` is called by scripts before spending turns. `prepare_for_adventure()` is a no-op that scripts call to restore outfit/mp before a zone. `adv1(location, turns)` is the single-adventure form used by fine-grained scripts. All three are currently missing.

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Character.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`
- Modify: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryCharacterTest.kt`

- [ ] **Step 1: Write the failing tests**

Add to `GameRuntimeLibraryCharacterTest.kt`:

```kotlin
@Test
fun canAdventure_trueWhenAdventuresLeft() {
    val lib = libWith { copy(adventures = "5") }
    assertEquals("true", outputLib(lib, """print(to_string(can_adventure($location[The Haunted Pantry])));"""))
}

@Test
fun canAdventure_falseWhenNoAdventuresLeft() {
    val lib = libWith { copy(adventures = "0") }
    assertEquals("false", outputLib(lib, """print(to_string(can_adventure($location[The Haunted Pantry])));"""))
}

@Test
fun prepareForAdventure_returnsTrue() {
    // No game state needed — always returns true (no-op)
    assertEquals("true", outputLib(GameRuntimeLibrary.forTesting(), "print(to_string(prepare_for_adventure()));"))
}
```

For `adv1`, testing would require a full `AdventureManager` mock (complex). Instead, verify it is registered without crashing when the manager is null — the null manager causes it to return false:

```kotlin
@Test
fun adv1_returnsFalseWhenNoAdventureManager() {
    // adventureManager is null → can't run → returns false without crashing
    val lib = GameRuntimeLibrary.forTesting()
    assertEquals("false", outputLib(lib, """print(to_string(adv1($location[The Haunted Pantry], 1)));"""))
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd /c/Development/kolmafia-mobile && ./gradlew :shared:jvmTest 2>&1 | tail -6
```

Expected: BUILD FAILED — `can_adventure`, `prepare_for_adventure`, `adv1` not registered.

- [ ] **Step 3: Add `can_adventure()` and `prepare_for_adventure()` to `GameRuntimeLibrary.Character.kt`**

Append to the `registerCharacterExtensions` function (before the closing `}`):

```kotlin
// can_adventure(location) → boolean
// Returns true if the character has adventures remaining. Does not check zone access,
// equipment requirements, or limit-mode restrictions (those are pre-flight concerns not
// modelled in mobile's CharacterState).
regFn(scope, "can_adventure", AshType.BOOLEAN,
    listOf("loc" to AshType.LOCATION)) { _, _ ->
    AshValue.of((character?.state?.value?.adventuresLeft ?: 0) > 0)
}

// prepare_for_adventure() → boolean
// On desktop this restores outfit/HP/MP before a zone. Mobile no-ops it (recovery
// is handled by the adventure loop's RecoveryManager pass). Returns true.
regFn(scope, "prepare_for_adventure", AshType.BOOLEAN, emptyList()) { _, _ ->
    AshValue.of(true)
}
```

- [ ] **Step 4: Add `adv1()` to `registerGameActions` in `GameRuntimeLibrary.kt`**

In `registerGameActions`, add after the existing `use_skill` registrations (before the closing `}`):

```kotlin
// adv1(loc: location, adventuresUsed: int) → boolean
// Runs a single adventure at [loc]. [adventuresUsed] indicates how many turns this
// adventure costs (typically 1) — tracked for scripts that manage turn budgets, but
// not currently consumed from CharacterState (the server updates that on response).
// Returns true on success, false if no AdventureManager is available.
register(scope, "adv1", AshType.BOOLEAN,
    listOf("loc" to AshType.LOCATION, "adventuresUsed" to AshType.INT)) { _, args ->
    val locName = args[0].toString()
    val manager = adventureManager ?: return@register AshValue.of(false)
    val location = net.sourceforge.kolmafia.adventure.AdventureLocation(locName, locName, "")
    kotlinx.coroutines.runBlocking {
        manager.runAdventures(location, 1, this).join()
    }
    AshValue.of(true)
}
```

- [ ] **Step 5: Run tests to confirm they pass**

```bash
cd /c/Development/kolmafia-mobile && ./gradlew :shared:jvmTest 2>&1 | tail -6
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
cd /c/Development/kolmafia-mobile && git add \
  shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Character.kt \
  shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt \
  shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryCharacterTest.kt
git commit -m "feat: can_adventure(), prepare_for_adventure(), adv1() ASH functions"
```

---

## Task T6: `cli_execute` expansion — cast and familiar

**Purpose:** `cli_execute("cast N skill")` and `cli_execute("familiar name")` appear in many scripts.
Currently these fall through to the echo fallback (`[cli] cast ...`). Wire them to `SkillManager.cast`
and `FamiliarManager.setFamiliar`.

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`
- Modify: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryCliTest.kt`

**Background on the `cliDispatch` list** (in `GameRuntimeLibrary.kt`):
```kotlin
private val cliDispatch: List<Pair<Regex, (MatchResult, AshRuntimeContext) -> Unit>> = listOf(
    Regex("^mood\\s+execute$", RegexOption.IGNORE_CASE) to { _, _ -> ... },
    Regex("^mood\\s+(.+)$",    RegexOption.IGNORE_CASE) to { m, _ -> ... },
    Regex("^set\\s+(.+?)\\s*=\\s*(.*)$")               to { m, _ -> ... },
    Regex("^get\\s+(.+)$")                              to { m, rt -> ... },
    // ← add new entries here
)
```

New patterns to add:
- `cast N skill-name` — cast skill N times
- `cast skill-name` — cast skill once
- `familiar familiar-name` — switch familiar

- [ ] **Step 1: Write the failing tests**

Add to `GameRuntimeLibraryCliTest.kt`:

```kotlin
@Test
fun cliExecute_castWithCount_callsSkillManager() {
    val castCalls = mutableListOf<Pair<String, Int>>()   // skill name → count
    val fakeSkillMgr = object : net.sourceforge.kolmafia.skill.SkillManager(
        net.sourceforge.kolmafia.event.GameEventBus()
    ) {
        override suspend fun cast(
            skill: net.sourceforge.kolmafia.skill.SkillData,
            quantity: Int
        ): Result<Unit> {
            castCalls.add(skill.name to quantity)
            return Result.success(Unit)
        }
    }
    // Give the skill manager a "Leash of Linguini" skill with id=6003
    fakeSkillMgr.update(net.sourceforge.kolmafia.skill.SkillState(
        skills = listOf(net.sourceforge.kolmafia.skill.SkillData(
            id = 6003, name = "Leash of Linguini", mpCost = 1,
            dailyLimit = 0, timesCast = 0
        ))
    ))
    val lib = GameRuntimeLibrary(skillManager = fakeSkillMgr)
    runLib(lib, """cli_execute("cast 3 Leash of Linguini");""")
    assertEquals(listOf("Leash of Linguini" to 3), castCalls)
}

@Test
fun cliExecute_castSingleNoCount_callsSkillManagerOnce() {
    val castCalls = mutableListOf<String>()
    val fakeSkillMgr = object : net.sourceforge.kolmafia.skill.SkillManager(
        net.sourceforge.kolmafia.event.GameEventBus()
    ) {
        override suspend fun cast(
            skill: net.sourceforge.kolmafia.skill.SkillData,
            quantity: Int
        ): Result<Unit> {
            repeat(quantity) { castCalls.add(skill.name) }
            return Result.success(Unit)
        }
    }
    fakeSkillMgr.update(net.sourceforge.kolmafia.skill.SkillState(
        skills = listOf(net.sourceforge.kolmafia.skill.SkillData(
            id = 6003, name = "Leash of Linguini", mpCost = 1,
            dailyLimit = 0, timesCast = 0
        ))
    ))
    val lib = GameRuntimeLibrary(skillManager = fakeSkillMgr)
    runLib(lib, """cli_execute("cast Leash of Linguini");""")
    assertEquals(listOf("Leash of Linguini"), castCalls)
}

@Test
fun cliExecute_familiar_callsFamiliarManager() {
    val switchCalls = mutableListOf<String>()
    val fakeFamiliarMgr = object : net.sourceforge.kolmafia.familiar.FamiliarManager(
        net.sourceforge.kolmafia.event.GameEventBus(),
        null, null
    ) {
        override suspend fun setFamiliar(name: String): Result<Unit> {
            switchCalls.add(name)
            return Result.success(Unit)
        }
    }
    val lib = GameRuntimeLibrary(familiarManager = fakeFamiliarMgr)
    runLib(lib, """cli_execute("familiar Mosquito");""")
    assertEquals(listOf("Mosquito"), switchCalls)
}

@Test
fun cliExecute_castUnknownSkill_echoesFallback() {
    // Skill not in skill manager → falls through to echo
    val lib = GameRuntimeLibrary()
    val out = outputLib(lib, """cli_execute("cast 1 Nonexistent Skill");""")
    assertEquals("[cli] cast 1 Nonexistent Skill", out)
}
```

**Note on constructors:** Read `SkillManager` and `FamiliarManager` constructors from the source to confirm exact parameters before writing the test. The constructors above are approximate; the subagent must read the actual source and adjust.

- [ ] **Step 2: Read SkillManager and FamiliarManager constructors**

Read these files to confirm exact constructor signatures:
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/skill/SkillManager.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/familiar/FamiliarManager.kt`

Adjust the test stubs above to match actual constructors.

- [ ] **Step 3: Run tests to confirm they fail**

```bash
cd /c/Development/kolmafia-mobile && ./gradlew :shared:jvmTest 2>&1 | tail -6
```

Expected: BUILD FAILED — `cast` and `familiar` dispatch patterns not in cliDispatch.

- [ ] **Step 4: Add cast and familiar dispatch patterns to `cliDispatch` in `GameRuntimeLibrary.kt`**

Find the `cliDispatch` list and add three new patterns after the existing `get` pattern (before the closing `)`):

```kotlin
// "cast N skill-name" — cast a skill N times
Regex("^cast\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
    val count = m.groupValues[1].toIntOrNull() ?: 1
    val skillName = m.groupValues[2].trim()
    val skill = skillManager?.state?.value?.skills
        ?.find { it.name.equals(skillName, ignoreCase = true) }
    if (skill != null) {
        kotlinx.coroutines.runBlocking {
            skillManager!!.cast(skill, count)
        }
    }
},

// "cast skill-name" — cast a skill once (no count prefix)
Regex("^cast\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
    val skillName = m.groupValues[1].trim()
    val skill = skillManager?.state?.value?.skills
        ?.find { it.name.equals(skillName, ignoreCase = true) }
    if (skill != null) {
        kotlinx.coroutines.runBlocking {
            skillManager!!.cast(skill, 1)
        }
    } else {
        rt.print("[cli] cast $skillName")  // unknown skill → echo
    }
},

// "familiar name" — switch to a familiar by name
Regex("^familiar\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
    val name = m.groupValues[1].trim()
    kotlinx.coroutines.runBlocking {
        familiarManager?.setFamiliar(name)
    }
},
```

**Important:** The `cast N skill` pattern must come BEFORE the `cast skill` pattern in the list, because `firstOrNull` matches the first regex that matches. If `cast skill` comes first, `cast 3 Leash of Linguini` would be captured by it.

- [ ] **Step 5: Run tests to confirm they pass**

```bash
cd /c/Development/kolmafia-mobile && ./gradlew :shared:jvmTest 2>&1 | tail -6
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
cd /c/Development/kolmafia-mobile && git add \
  shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt \
  shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryCliTest.kt
git commit -m "feat: cli_execute expansion — cast N/1, familiar dispatch patterns"
```

---

## Final Verification

- [ ] **Run full test suite**

```bash
cd /c/Development/kolmafia-mobile && ./gradlew :shared:jvmTest 2>&1 | tail -6
```

Expected: `BUILD SUCCESSFUL` with all tests passing.

- [ ] **Verify all 6 tasks are committed**

```bash
cd /c/Development/kolmafia-mobile && git log --oneline -8
```

Expected output (exact messages may vary):
```
feat: cli_execute expansion — cast N/1, familiar dispatch patterns
feat: can_adventure(), prepare_for_adventure(), adv1() ASH functions
feat: under_standard() reads AscensionPath.STANDARD — adds STANDARD path + CharacterState.isUnderStandard
feat: hermit(count, item) count-first overload — parity with desktop ASH API
feat: wait/waitq + logprint/debugprint/traceprint ASH functions
feat: to_int() entity overloads — item, effect, skill, familiar, location, monster
```

---

## Self-Review Notes

**Spec coverage:**
- ✅ `to_int()` entity overloads (T1) — item/effect/skill/familiar/location/monster
- ✅ `wait()` / `waitq()` (T2)
- ✅ `logprint` / `debugprint` / `traceprint` (T2)
- ✅ `hermit()` count-first (T3)
- ✅ `under_standard()` real value (T4)
- ✅ `can_adventure()` / `prepare_for_adventure()` (T5)
- ✅ `adv1()` (T5)
- ✅ `cli_execute` cast + familiar (T6)

**Type consistency check:**
- `SkillData` is used in T6 tests — this is the runtime type in `SkillState.skills`. Read `SkillData.kt` to confirm field names (`id`, `name`, `mpCost`, `dailyLimit`, `timesCast`) match usage in the test stubs.
- `FamiliarManager.setFamiliar(name: String)` — confirmed `open suspend fun setFamiliar(name: String): Result<Unit>` at line 108 in `FamiliarManager.kt`.
- `SkillManager.cast(skill, quantity)` — confirmed `open suspend fun cast(skill: SkillData, quantity: Int = 1)` at line 67 in `SkillManager.kt`.
- `AdventureZone.snarfblat: String?` — `get() = urlParams.substringAfter("adventure=", "").takeIf { it.isNotBlank() }`. The `zoneDb` fake in T1 sets `urlParams = "adventure=$snarfblat"` so `snarfblat.toIntOrNull()` works.

**Placeholder scan:** No TBD, TODO, or "similar to" references found. All code blocks are complete.
