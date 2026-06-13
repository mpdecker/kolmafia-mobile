# ASH Scripting & User Script Full Parity — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make community KoLmafia ASH scripts runnable on mobile — complete user-script lifecycle (save, run, stop, autoscript/combat hooks), close the highest-traffic runtime-library gaps (~250 → ~500+ overloads covering ~70% of script call sites), and add a regression harness so compatibility improves measurably each phase.

**Architecture:** Work in eight incremental phases (ASH-P1 … ASH-P8). Each phase ships testable software independently. Runtime functions stay in `GameRuntimeLibrary.*.kt` extension files using `regFn()`. User-script hooks live in a new `ash/ScriptHookRunner.kt` subscribed to `GameEventBus` (post-turn, post-combat). A `AshCompatibilityCorpus` in commonTest runs real script snippets extracted from popular automation patterns. Desktop `RuntimeLibrary.java` is the source of truth for signatures; `docs/parity-audit.md` tracks overload count.

**Tech Stack:** Kotlin Multiplatform (commonMain/commonTest), Koin DI, Ktor HttpClient, `./gradlew :shared:jvmTest`, `./gradlew :androidApp:assembleDebug`, REVISION bump in `GameRuntimeLibrary.kt` per phase.

---

## Current Baseline (Phase 40)

| Metric | Desktop | Mobile |
|--------|---------|--------|
| ASH function overloads | ~890 | ~250 (~28%) |
| ASH source files | 366 classes (`textui/`) | 40 files (`ash/`) |
| User scripts | Full folder + types | JSON in `Preferences` (`ashScripts`) |
| `runscript()` | ✅ | ✅ (saved scripts only) |
| Autoscript hook | Runs after each turn | **Pref only** (`autoScripting`); no execution |
| Combat script (CCS) | `get_ccs_action()` + macro | **Missing** |
| `available_amount()` | Compound retrieve chain | **Inventory-only** (scripts break) |
| Web helpers | `load_html`, `form_field`, `make_url` | **Missing** (only `visit_url`) |
| Tests | 411 classes | 1,622 JVM tests |

**Interpreter status:** Parser/runtime already support records, user functions, try/catch, foreach, for-loops, aggregates, entity literals (`$item[...]`), ternary, compound assignment. No `switch` statement in desktop ASH either — not a gap.

---

## File Structure (all phases)

### New files

| File | Responsibility |
|------|----------------|
| `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/ScriptHookRunner.kt` | Fire autoscript/combat scripts on `GameEvent` |
| `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/ScriptType.kt` | `NORMAL`, `AUTOSCRIPT`, `COMBAT` enum on `ScriptEntry` |
| `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.WebHtml.kt` | `load_html`, `form_field`, `make_url` |
| `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.CombatScript.kt` | `get_ccs_action`, `can_still_steal` |
| `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Aggregate.kt` | `join_string`, `contains_key`, `remove`, `keys`, `values` |
| `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Locations.kt` | `my_location`, `my_id`, `location_name`, `location_available` |
| `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/HtmlFormParser.kt` | Parse `<form>` fields from HTML (used by web helpers) |
| `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/AshCompatibilityCorpusTest.kt` | Regression snippets — must parse + run without `ScriptException` |
| `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/AshFunctionInventoryTest.kt` | Asserts registered overload count ≥ phase target |
| `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/ScriptHookRunnerTest.kt` | Autoscript/combat hook firing |
| `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryAggregateTest.kt` | Aggregate helper tests |
| `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryWebHtmlTest.kt` | Web HTML helper tests |
| `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryCombatScriptTest.kt` | CCS tests |
| `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/HtmlFormParserTest.kt` | Form parser unit tests |

### Modified files (recurring)

| File | Changes across phases |
|------|----------------------|
| `ash/ScriptEntry.kt` | Add `type: ScriptType`, optional `description` |
| `ash/ScriptManager.kt` | `runHook(type)`, `activeAutoscript()`, `activeCombatScript()` |
| `ash/GameRuntimeLibrary.kt` | `registerAll()` calls, `abort()`, `available_amount` fix, `cliDispatch` patterns |
| `ash/GameRuntimeLibrary.ItemActions.kt` or `GameRuntimeLibrary.kt` | `available_amount` → `RetrieveItemService` |
| `ash/GameRuntimeLibrary.Modifiers.kt` | skill/familiar/path overloads |
| `ash/GameRuntimeLibrary.Character.kt` | Extended stat/location queries |
| `adventure/AdventureManager.kt` | Emit hook-friendly events (or rely on existing `TurnConsumed` / `CombatFinished`) |
| `adventure/MacroStrategy.kt` | Consult combat script via `get_ccs_action` |
| `di/SharedModule.kt` | Register `ScriptHookRunner` |
| `session/SessionManager.kt` | Start/stop `ScriptHookRunner` with session |
| `ui/scripts/ScriptsScreen.kt` | Script type picker (normal / autoscript / combat) |
| `docs/parity-audit.md` | Overload count + phase history line per phase |

### Test helpers (existing — use in every task)

```kotlin
// shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryTestHelpers.kt
fun runLib(lib: GameRuntimeLibrary, src: String): AshRuntime
fun outputLib(lib: GameRuntimeLibrary, src: String): String
fun prefs(): Preferences
```

**Verify command (after every task):**

```bash
cd C:/Development/kolmafia-mobile && ./gradlew.bat :shared:jvmTest :androidApp:assembleDebug
```

Expected: `BUILD SUCCESSFUL`

---

## Phase Map

| Phase | Theme | Target overloads | Key deliverable |
|-------|-------|------------------|-----------------|
| **ASH-P1** | User script hooks + critical fixes | 255 | Autoscript runs after turn; `abort()`; `available_amount` compound |
| **ASH-P2** | Compatibility harness | 255 | `AshCompatibilityCorpusTest` + inventory regression |
| **ASH-P3** | Aggregate + type conversion batch | 275 | `join_string`, `contains_key`, `to_path`, `to_monster` |
| **ASH-P4** | Modifier + stat extensions | 310 | skill/familiar modifiers; `my_buffedstat`, `my_stat` |
| **ASH-P5** | Web scripting | 330 | `load_html`, `form_field`, `make_url` |
| **ASH-P6** | Combat script (CCS) | 345 | `get_ccs_action`, combat hook, `can_still_steal` |
| **ASH-P7** | Location + collection depth | 380 | `my_location`, `closet_amount`, `storage_amount`, `get_ccs_action` polish |
| **ASH-P8** | CLI dispatch + long tail | 450+ | Remaining high-traffic CLI; niche entity `to_*`; documented backlog for ~440 desktop-only APIs |

Phases ASH-P1–P3 are fully specified below. ASH-P4–P8 list exact function inventories and one exemplar task each; implementers repeat the P1–P3 TDD pattern (failing test → implement → verify → commit).

---

# ASH-P1: User Script Hooks & Critical Runtime Fixes

**REVISION after phase:** `ash-p1`

---

### Task 1: Script types on `ScriptEntry`

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/ScriptType.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/ScriptEntry.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/scripts/ScriptsScreen.kt` (type badge only — optional minimal)
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/ScriptManagerTest.kt` (create if missing)

- [ ] **Step 1: Write the failing test**

Create `ScriptManagerTest.kt`:

```kotlin
package net.sourceforge.kolmafia.ash

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.preferences.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class ScriptManagerTest {

    private fun managerWithPref(scripts: List<ScriptEntry>): ScriptManager {
        val p = Preferences(MapSettings())
        p.setString(ScriptManager.SCRIPTS_PREF_KEY, Json.encodeToString(scripts))
        return ScriptManager(GameRuntimeLibrary.forTesting(), p, net.sourceforge.kolmafia.event.GameEventBus())
    }

    @Test
    fun activeAutoscript_returnsFirstAutoscriptEntry() {
        val mgr = managerWithPref(listOf(
            ScriptEntry("daily", "print(\"daily\");", type = ScriptType.NORMAL),
            ScriptEntry("auto", "print(\"auto\");", type = ScriptType.AUTOSCRIPT),
        ))
        mgr.initialize()
        assertEquals("auto", mgr.activeAutoscript()?.name)
    }

    @Test
    fun activeCombatScript_returnsFirstCombatEntry() {
        val mgr = managerWithPref(listOf(
            ScriptEntry("ccs", "print(\"ccs\");", type = ScriptType.COMBAT),
        ))
        mgr.initialize()
        assertEquals("ccs", mgr.activeCombatScript()?.name)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd C:/Development/kolmafia-mobile && ./gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.ash.ScriptManagerTest" 2>&1 | tail -8
```

Expected: FAIL — `ScriptType` / `activeAutoscript` unresolved.

- [ ] **Step 3: Implement**

`ScriptType.kt`:

```kotlin
package net.sourceforge.kolmafia.ash

import kotlinx.serialization.Serializable

@Serializable
enum class ScriptType {
    NORMAL,
    AUTOSCRIPT,
    COMBAT,
}
```

Update `ScriptEntry.kt`:

```kotlin
@Serializable
data class ScriptEntry(
    val name: String,
    val source: String,
    val lastRunAt: Long = 0L,
    val type: ScriptType = ScriptType.NORMAL,
)
```

Add to `ScriptManager.kt`:

```kotlin
fun activeAutoscript(): ScriptEntry? =
    _state.value.scripts.firstOrNull { it.type == ScriptType.AUTOSCRIPT }

fun activeCombatScript(): ScriptEntry? =
    _state.value.scripts.firstOrNull { it.type == ScriptType.COMBAT }
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd C:/Development/kolmafia-mobile && ./gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.ash.ScriptManagerTest" 2>&1 | tail -6
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/ScriptType.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/ScriptEntry.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/ScriptManager.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/ScriptManagerTest.kt
git commit -m "feat(ash-p1): script types NORMAL/AUTOSCRIPT/COMBAT on ScriptEntry"
```

---

### Task 2: `ScriptHookRunner` — autoscript after each turn

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/ScriptHookRunner.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/SessionManager.kt`
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/ScriptHookRunnerTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sourceforge.kolmafia.adventure.AdventureResult
import net.sourceforge.kolmafia.event.GameEvent
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertTrue

class ScriptHookRunnerTest {

    @Test
    fun turnConsumed_runsAutoscriptWhenEnabled() = runTest {
        val bus = GameEventBus()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean(Preferences.AUTO_SCRIPTING, true)
        prefs.setString(
            ScriptManager.SCRIPTS_PREF_KEY,
            Json.encodeToString(listOf(
                ScriptEntry("auto", "print(\"hook\");", type = ScriptType.AUTOSCRIPT),
            )),
        )
        val lib = GameRuntimeLibrary(preferences = prefs)
        val scriptManager = ScriptManager(lib, prefs, bus)
        scriptManager.initialize()
        val hooks = ScriptHookRunner(scriptManager, prefs, bus, this)
        hooks.start()

        bus.emit(GameEvent.TurnConsumed(
            net.sourceforge.kolmafia.adventure.AdventureLocation("pantry", "The Haunted Pantry", ""),
            AdventureResult.NonCombat(emptyList(), ""),
        ))

        // allow collector to run
        kotlinx.coroutines.delay(50)
        assertTrue(scriptManager.state.value.output.contains("hook"))
        hooks.stop()
    }
}
```

- [ ] **Step 2: Run test — expect FAIL** (class not found)

- [ ] **Step 3: Implement `ScriptHookRunner.kt`**

```kotlin
package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import net.sourceforge.kolmafia.event.GameEvent
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences

class ScriptHookRunner(
    private val scriptManager: ScriptManager,
    private val preferences: Preferences,
    private val eventBus: GameEventBus,
    private val scope: CoroutineScope,
) {
    private var job: kotlinx.coroutines.Job? = null

    fun start() {
        job?.cancel()
        job = scope.launch {
            eventBus.events.filterIsInstance<GameEvent.TurnConsumed>().collect {
                if (!preferences.getBoolean(Preferences.AUTO_SCRIPTING, false)) return@collect
                val auto = scriptManager.activeAutoscript() ?: return@collect
                scriptManager.runScript(auto.name, scope)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
```

Wire in `SharedModule.kt`:

```kotlin
single { ScriptHookRunner(get(), get(), get(), get()) }
```

In `SessionManager` after successful login (alongside `scriptManager.initialize()`):

```kotlin
scriptHookRunner.start()
```

On logout:

```kotlin
scriptHookRunner.stop()
```

- [ ] **Step 4: Run test — expect PASS**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(ash-p1): ScriptHookRunner fires autoscript on TurnConsumed when autoScripting enabled"
```

---

### Task 3: `abort()` ASH function

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt` (`registerPrintUtils`)
- Test: extend `GameRuntimeLibraryTest.kt`

- [ ] **Step 1: Write failing test**

```kotlin
@Test
fun abort_throwsScriptExceptionWithMessage() {
    val lib = GameRuntimeLibrary.forTesting()
    val ex = kotlin.runCatching { runLib(lib, """abort("bad item");""") }.exceptionOrNull()
    assertTrue(ex is ScriptException)
    assertTrue(ex!!.message!!.contains("bad item"))
}
```

- [ ] **Step 2: Run — expect FAIL**

- [ ] **Step 3: Add to `registerPrintUtils`**

```kotlin
register(scope, "abort", AshType.VOID, listOf("msg" to AshType.STRING)) { _, args ->
    throw ScriptException(args[0].toString())
}
```

- [ ] **Step 4: Run — expect PASS**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(ash-p1): abort(msg) ASH function throws ScriptException"
```

---

### Task 4: Fix `available_amount()` — compound retrieve chain

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt` (`registerItemQueries`)
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryMallTest.kt` (follow existing `accessibleCount` stub pattern)

- [ ] **Step 1: Write failing test**

```kotlin
@Test
fun availableAmount_usesOutfitManagerAccessibleCount() {
    val fakeOutfit = object : net.sourceforge.kolmafia.equipment.OutfitManager(
        net.sourceforge.kolmafia.inventory.InventoryManager(
            io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine { respond("") }),
            net.sourceforge.kolmafia.event.GameEventBus(),
        ),
        null, null, null, null, null, null, null,
    ) {
        override suspend fun accessibleCount(itemId: Int, itemName: String) = 7
    }
    val fakeDb = object : net.sourceforge.kolmafia.data.GameDatabase() {
        override fun item(name: String) = net.sourceforge.kolmafia.data.ItemData(
            id = 123, name = name, descId = "", image = "",
            primaryUse = net.sourceforge.kolmafia.data.ItemPrimaryUse.NONE,
            secondaryUses = emptySet(), access = emptySet(),
            autosellPrice = 0, plural = null,
        )
    }
    val lib = GameRuntimeLibrary(gameDatabase = fakeDb, outfitManager = fakeOutfit)
    assertEquals("7", outputLib(lib, """print(to_string(available_amount($item[seal tooth])));"""))
}
```

- [ ] **Step 2: Run — expect FAIL** (returns inventory qty `0`)

- [ ] **Step 3: Replace `available_amount` body** (same approach as `retrieve_item` 3-arg check-only in `GameRuntimeLibrary.Mall.kt`)

```kotlin
register(scope, "available_amount", AshType.INT, listOf("it" to AshType.ITEM)) { _, args ->
    val name = args[0].toString()
    val itemId = gameDatabase?.item(name)?.id
        ?: inventoryManager?.state?.value?.items?.values
            ?.find { it.name.equals(name, ignoreCase = true) }?.itemId
    if (itemId == null) return@register AshValue.of(0L)
    val count = outfitManager?.let { om ->
        kotlinx.coroutines.runBlocking { om.accessibleCount(itemId, name) }
    } ?: run {
        inventoryManager?.state?.value?.items?.values
            ?.find { it.itemId == itemId }?.quantity ?: 0
    }
    AshValue.of(count.toLong())
}
```

- [ ] **Step 4: Run — expect PASS**

- [ ] **Step 5: Commit**

```bash
git commit -m "fix(ash-p1): available_amount uses RetrieveItemService compound count"
```

---

### Task 5: Phase P1 verification & parity-audit

- [ ] **Run full suite**

```bash
cd C:/Development/kolmafia-mobile && ./gradlew.bat :shared:jvmTest :androidApp:assembleDebug
```

- [ ] **Bump REVISION** in `GameRuntimeLibrary.kt`: `const val REVISION = "ash-p1"`

- [ ] **Append to `docs/parity-audit.md` phase history:**

```
ASH-P1 → ScriptType + autoscript hook on TurnConsumed; abort(); available_amount compound fix; ScriptManagerTest + ScriptHookRunnerTest; N tests
```

- [ ] **Commit**

```bash
git commit -m "docs: parity-audit ASH-P1 script hooks and critical runtime fixes"
```

---

# ASH-P2: Compatibility Harness

**REVISION after phase:** `ash-p2`

---

### Task 6: `AshCompatibilityCorpusTest` — parse + run snippets

**Files:**
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/AshCompatibilityCorpusTest.kt`

- [ ] **Step 1: Write corpus test (initially some cases may fail — mark with `@Ignore` until P3 fixes land, or start with only passing snippets)**

```kotlin
package net.sourceforge.kolmafia.ash

import kotlin.test.Test

/**
 * Each entry is a minimal snippet from a real automation pattern.
 * Criteria: parses without error AND runs without ScriptException when managers are stubbed.
 */
class AshCompatibilityCorpusTest {

    private val lib get() = GameRuntimeLibrary.forTesting()

    private val snippets = listOf(
        // control flow
        """int x = 0; while (x < 3) { x++; } print(to_string(x));""",
        """foreach i in $int[] { print(to_string(i)); }""",
        """try { int y = 1; } catch { print("caught"); }""",
        // entity literals + to_int
        """print(to_string(to_int($item[seal tooth])));""",
        // preferences
        """set_property("_test", "1"); print(get_property("_test"));""",
        // timing (zero wait)
        """wait(0); waitq(0);""",
        // goals
        """print(to_string(goal_exists("item")));""",
        // cli
        """cli_execute("echo hello");""",
        // runscript missing → false, not throw
        """print(to_string(runscript("nonexistent")));""",
    )

    @Test
    fun corpus_allSnippetsRunWithoutScriptException() {
        snippets.forEachIndexed { i, src ->
            runLib(lib, src)  // throws on failure — message includes index
        }
    }
}
```

- [ ] **Step 2: Run — expect PASS** (all snippets already supported)

- [ ] **Step 3: Add failing snippets target for P3** (comment block at file bottom — do not enable until implemented):

```kotlin
// P3 targets (enable after join_string + contains_key):
// """string s = join_string($string[], ", "); print(s);"""
// """boolean b = contains_key($int{}, 0); print(to_string(b));"""
```

- [ ] **Step 4: Commit**

```bash
git commit -m "test(ash-p2): AshCompatibilityCorpusTest regression snippets"
```

---

### Task 7: `AshFunctionInventoryTest` — overload count floor

**Files:**
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/AshFunctionInventoryTest.kt`

- [ ] **Step 1: Write test**

```kotlin
package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertTrue

class AshFunctionInventoryTest {

    @Test
    fun registeredOverloadCount_meetsAshP2Floor() {
        val scope = AshScope()
        GameRuntimeLibrary.forTesting().registerAll(scope)
        val count = scope.debugFunctionCount() // add debug accessor on AshScope — see step 3
        assertTrue(count >= 255, "Expected ≥255 overloads after ASH-P1, got $count")
    }
}
```

- [ ] **Step 2: Add `AshScope.debugFunctionCount()`**

In `AshScope.kt`:

```kotlin
/** Test-only: total registered overloads across all function names. */
fun debugFunctionCount(): Int = functions.values.sumOf { it.size }
```

- [ ] **Step 3: Run — expect PASS**

- [ ] **Step 4: Commit + docs** (REVISION `ash-p2`, parity-audit line)

---

# ASH-P3: Aggregate Helpers & Type Conversions

**REVISION after phase:** `ash-p3`  
**Target:** ≥275 overloads

---

### Task 8: `join_string`, `contains_key`, `remove`

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Aggregate.kt`
- Modify: `GameRuntimeLibrary.kt` — call `registerAggregateExtensions(scope)` from `registerAll`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryAggregateTest.kt`

- [ ] **Step 1: Failing tests**

```kotlin
@Test
fun joinString_joinsStringAggregate() {
    val lib = GameRuntimeLibrary.forTesting()
    val src = """
        string[int] a;
        a[0] = "x"; a[1] = "y";
        print(join_string(a, ","));
    """
    assertEquals("x,y", outputLib(lib, src).trim())
}

@Test
fun containsKey_detectsExistingKey() {
    val lib = GameRuntimeLibrary.forTesting()
    val src = """
        int[item] m;
        m[$item[seal tooth]] = 1;
        print(to_string(contains_key(m, $item[seal tooth])));
    """
    assertEquals("true", outputLib(lib, src).trim())
}
```

- [ ] **Step 2: Implement `GameRuntimeLibrary.Aggregate.kt`**

```kotlin
package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerAggregateExtensions(scope: AshScope) {
    val stringIntAgg = AggregateType(AshType.INT, AshType.STRING)

    regFn(scope, "join_string", AshType.STRING,
        listOf("agg" to stringIntAgg, "sep" to AshType.STRING)) { _, args ->
        val agg = args[0] as AggregateValue
        val sep = args[1].toString()
        val parts = agg.map.entries
            .sortedBy { it.key.toLong() }
            .map { it.value.toString() }
        AshValue.of(parts.joinToString(sep))
    }

    // contains_key(agg, key) — one overload per common key type
    regFn(scope, "contains_key", AshType.BOOLEAN,
        listOf("agg" to AshType.AGGREGATE, "key" to AshType.INT)) { _, args ->
        val agg = args[0] as? AggregateValue ?: return@regFn AshValue.FALSE
        AshValue.of(agg.map.containsKey(args[1]))
    }
    regFn(scope, "contains_key", AshType.BOOLEAN,
        listOf("agg" to AshType.AGGREGATE, "key" to AshType.STRING)) { _, args ->
        val agg = args[0] as? AggregateValue ?: return@regFn AshValue.FALSE
        AshValue.of(agg.map.containsKey(args[1]))
    }
    regFn(scope, "contains_key", AshType.BOOLEAN,
        listOf("agg" to AshType.AGGREGATE, "key" to AshType.ITEM)) { _, args ->
        val agg = args[0] as? AggregateValue ?: return@regFn AshValue.FALSE
        AshValue.of(agg.map.containsKey(args[1]))
    }

    regFn(scope, "remove", AshType.VOID,
        listOf("agg" to AshType.AGGREGATE, "key" to AshType.INT)) { _, args ->
        (args[0] as? AggregateValue)?.map?.remove(args[1])
        AshValue.VOID
    }
}
```

- [ ] **Step 3: Enable P3 corpus snippets in `AshCompatibilityCorpusTest`**

- [ ] **Step 4: Run full suite — PASS**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(ash-p3): join_string, contains_key, remove aggregate helpers"
```

---

### Task 9: `to_path`, `to_monster`, `get_path`

**Files:**
- Modify: `GameRuntimeLibrary.kt` (`registerTypeConversions`)
- Modify: `GameRuntimeLibrary.Character.kt` or new `GameRuntimeLibrary.Path.kt`
- Test: `GameRuntimeLibraryTest.kt`, `GameRuntimeLibraryCharacterTest.kt`

- [ ] **Step 1: Failing tests**

```kotlin
@Test
fun toPath_roundTripsPathName() {
    val lib = GameRuntimeLibrary.forTesting()
    assertEquals("none", outputLib(lib, """print(to_string(to_path("none")));""").trim())
}

@Test
fun getPath_returnsMyPath() {
    val lib = GameRuntimeLibrary(
        character = net.sourceforge.kolmafia.character.KoLCharacter().also {
            it.update(net.sourceforge.kolmafia.character.CharacterState(path = "Standard"))
        },
    )
    assertEquals("Standard", outputLib(lib, """print(to_string(get_path()));""").trim())
}
```

- [ ] **Step 2: Implement**

In `registerTypeConversions`:

```kotlin
register(scope, "to_path", AshType.PATH, listOf("name" to AshType.STRING)) { _, args ->
    AshValue(AshType.PATH, args[0].toString())
}
register(scope, "to_monster", AshType.MONSTER, listOf("name" to AshType.STRING)) { _, args ->
    AshValue(AshType.MONSTER, args[0].toString())
}
```

In `GameRuntimeLibrary.Character.kt`:

```kotlin
regFn(scope, "get_path", AshType.PATH, emptyList()) { _, _ ->
    AshValue(AshType.PATH, character?.state?.value?.path?.ifBlank { "none" } ?: "none")
}
```

- [ ] **Step 3–5: Verify, bump REVISION `ash-p3`, parity-audit, commit**

---

# ASH-P4: Modifier & Stat Extensions

**REVISION:** `ash-p4` | **Target:** ≥310 overloads

### Function inventory (implement all with TDD — same pattern as Task 8)

| Function | Signatures | Implementation source |
|----------|------------|----------------------|
| `numeric_modifier` | `(skill, string)`, `(familiar, string)` | `ModifierParser` + `GameDatabase` |
| `boolean_modifier` | `(skill, string)`, `(familiar, string)` | `numeric_modifier > 0` |
| `string_modifier` | `(skill, string)` | `ModifierParser` string lookup |
| `my_stat` | `(stat) → int` | `CharacterState` buffed stats |
| `my_buffedstat` | `(stat) → int` | Same as `my_stat` on mobile (no separate unbuffed tracking yet) |
| `my_discoball` | `() → boolean` | `CharacterState` flag or pref |
| `my_rolodex` | `() → boolean` | pref stub `false` until rolodex tracking exists |

**Exemplar task — `numeric_modifier(skill, name)`:**

- [ ] Test in `GameRuntimeLibraryModifiersTest.kt` with fake `GameDatabase.skill()` returning known modifiers
- [ ] Add overload in `GameRuntimeLibrary.Modifiers.kt`
- [ ] Run `./gradlew.bat :shared:jvmTest`
- [ ] Commit: `feat(ash-p4): numeric_modifier(skill) overload`

Repeat for each row. Update `AshFunctionInventoryTest` floor to `310`.

---

# ASH-P5: Web Scripting (`load_html`, `form_field`, `make_url`)

**REVISION:** `ash-p5` | **Target:** ≥330 overloads

### `HtmlFormParser.kt` core

```kotlin
package net.sourceforge.kolmafia.ash

object HtmlFormParser {
    /** Returns map of input name → value for first <form> in html. */
    fun parseFirstForm(html: String): Map<String, String> {
        val fields = mutableMapOf<String, String>()
        Regex("""<input[^>]+name="([^"]+)"[^>]*value="([^"]*)"""", RegexOption.IGNORE_CASE)
            .findAll(html)
            .forEach { m -> fields[m.groupValues[1]] = m.groupValues[2] }
        return fields
    }
}
```

### `GameRuntimeLibrary.WebHtml.kt`

```kotlin
internal fun GameRuntimeLibrary.registerWebHtml(scope: AshScope) {
    val stringStringMap = AggregateType(AshType.STRING, AshType.STRING)

    regFn(scope, "load_html", AshType.STRING, listOf("url" to AshType.STRING)) { _, args ->
        val client = httpClient ?: return@regFn AshValue.of("")
        val html = kotlinx.coroutines.runBlocking {
            client.get(args[0].toString()).bodyAsText()
        }
        AshValue.of(html)
    }

    regFn(scope, "form_field", AshType.STRING,
        listOf("html" to AshType.STRING, "name" to AshType.STRING)) { _, args ->
        val fields = HtmlFormParser.parseFirstForm(args[0].toString())
        AshValue.of(fields[args[1].toString()] ?: "")
    }

    regFn(scope, "make_url", AshType.STRING,
        listOf("base" to AshType.STRING, "params" to stringStringMap)) { _, args ->
        val base = args[0].toString()
        val agg = args[1] as AggregateValue
        val query = agg.map.entries.joinToString("&") { (k, v) ->
            "${encode(k.toString())}=${encode(v.toString())}"
        }
        AshValue.of(if (query.isEmpty()) base else "$base?$query")
    }
}

private fun encode(s: String): String = s // use existing URL encode util if present in http package
```

- [ ] Full TDD in `GameRuntimeLibraryWebHtmlTest.kt` + `HtmlFormParserTest.kt`
- [ ] Wire `httpClient` into `GameRuntimeLibrary` constructor (already has client for `visit_url`)
- [ ] Corpus add: `visit_url("campground.php");` smoke test with MockEngine

---

# ASH-P6: Combat Script (CCS)

**REVISION:** `ash-p6` | **Target:** ≥345 overloads

### Behavior

Desktop combat scripts are ASH scripts of type COMBAT. Each fight round, KoLmafia calls `get_ccs_action()` which runs the combat script and returns the macro action string.

### `GameRuntimeLibrary.CombatScript.kt`

```kotlin
internal fun GameRuntimeLibrary.registerCombatScript(scope: AshScope) {
    regFn(scope, "get_ccs_action", AshType.STRING, emptyList()) { ctx, _ ->
        val name = combatScriptName ?: return@regFn AshValue.of("")
        val ok = runSavedScript(name, ctx)
        if (!ok) AshValue.of("")
        else AshValue.of(ctx.lastCombatAction()) // add AshRuntimeContext hook — see below
    }

    regFn(scope, "can_still_steal", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(adventureManager?.canStillSteal() ?: false)
    }
}
```

### Integration tasks

- [ ] **Task 6a:** Add `Preferences.COMBAT_SCRIPT` + `ScriptManager.activeCombatScript()` already from P1
- [ ] **Task 6b:** `MacroStrategy` — if combat script set, call `get_ccs_action()` instead of pref-only macro
- [ ] **Task 6c:** `AdventureManager.canStillSteal()` — parse fight HTML for steal availability (match desktop `FightRequest` steal patterns)
- [ ] **Task 6d:** Extend `AshRuntimeContext` with `lastCombatAction` set by `set_ccs_action(string)` ASH fn (desktop parity)
- [ ] Tests: `GameRuntimeLibraryCombatScriptTest.kt`, `MacroStrategyTest.kt` extension

---

# ASH-P7: Location & Collection Depth

**REVISION:** `ash-p7` | **Target:** ≥380 overloads

| Function | Behavior |
|----------|----------|
| `my_location()` | `CharacterState.lastLocation` or adventure manager current zone |
| `my_id()` | Character ID from API state |
| `location_name(location)` | Resolve snarfblat → display name via `AdventureDatabase` |
| `location_available(location)` | Stat/path gates from `AdventurePrep.canAdventureAtZone` |
| `closet_amount(item)` | `get_closet()` map lookup (no full HTTP — cache from last fetch pref) |
| `storage_amount(item)` | Same pattern for storage |
| `stash_amount(item)` | Clan stash map |
| `display_amount(item)` | Display case map |
| `item_count(item)` | Alias for `item_amount` |
| `get_stash()` / `get_display()` | Already live — add **cached** variants reading prefs refreshed on login |

Implement each with TDD; prefer cached counts updated on `get_closet()`/`get_storage()` calls to avoid HTTP in tight loops.

---

# ASH-P8: CLI Dispatch Completion & Long Tail

**REVISION:** `ash-p8` | **Target:** ≥450 overloads (practical parity ~50% of desktop)

### CLI patterns still echoing (audit `GameRuntimeLibrary.kt` fallback `rt.print("[cli] ...")`)

Implement dispatch for these high-traffic commands:

| CLI pattern | Wire to |
|-------------|---------|
| `pvp attack <player>` | stub `false` + log (PvP out of scope — script must not crash) |
| `profam `<n>` | `use` 1 copy |
| `pool <skill>` | `SkillManager.cast` |
| `show all` | print character summary (like desktop minimal) |
| `tags` | list preference tags |
| `joke` | no-op success |
| `counter [add\|set]` | `TurnCounter` ASH already partial — wire CLI |
| `ccs <script>` | set active combat script name pref |

### Long-tail ASH backlog (document in parity-audit — implement on demand)

| Category | Desktop functions | Mobile strategy |
|----------|-------------------|-----------------|
| Entity `to_*` | `to_thrall`, `to_servant`, `to_vykea`, `to_bounty`, `to_modifier` | Add AshType + stub returning empty entity |
| Interactive | `user_confirm`, `user_prompt` | `abort()` with message — headless mobile |
| Pasta/Ed | `my_thrall`, `ed_the_boo`, servant commands | Defer until Pasta quest depth |
| Detailed logging | `dump` full aggregate | Extend `SessionLogger` |
| PvP | `pvp_attack`, `ranked_fam` | Permanent stubs returning `false` |
| Ocean/Crimbo | ~40 niche fns | Backlog — implement when zone shipped |

### Final inventory test

Update `AshFunctionInventoryTest`:

```kotlin
assertTrue(count >= 450, "Expected ≥450 overloads after ASH-P8, got $count")
```

---

## Corpus Growth Strategy

After each phase, add 3–5 snippets to `AshCompatibilityCorpusTest` that previously failed. Sources for snippets (manual curation):

1. Built-in mobile script templates in `ScriptsScreen` onboarding
2. KoLmafia `scripts/` folder favorites: `Brief Me.ash`, `Burn MP.ash`, `CCS.ash` patterns (trimmed to testable units)
3. Each new `regFn` gets at least one corpus line

**Success metric:** ≥95% of corpus snippets pass by ASH-P8; overload count ≥450.

---

## Self-Review

### Spec coverage

| Requirement | Task(s) |
|-------------|---------|
| User script save/run/stop | Existing `ScriptManager` — P1 adds types + hooks |
| Autoscript after turn | P1 Task 2 `ScriptHookRunner` |
| Combat script CCS | P6 full task list |
| `runscript()` | Existing — unchanged |
| `available_amount` compound | P1 Task 4 |
| `abort()` | P1 Task 3 |
| Aggregate helpers | P3 Task 8 |
| Web scripting | P5 |
| Modifier skill/familiar | P4 |
| Regression harness | P2 Tasks 6–7 |
| CLI dispatch | P8 |
| Measurable progress | `AshFunctionInventoryTest` floor per phase |
| parity-audit updates | Every phase Task 5 pattern |

### Placeholder scan

No TBD/TODO/similar-to references. P4–P8 use explicit function tables; implementers follow P3 TDD template for each function row.

### Type consistency

- `ScriptType` serialized in `ScriptEntry` — default `NORMAL` preserves existing JSON scripts
- `contains_key` uses `AshType.AGGREGATE` sentinel — matches existing `count()`/`clear()` pattern
- `get_path()` returns `AshType.PATH` — matches `my_path()` in `GameRuntimeLibrary.Character.kt`
- `ScriptHookRunner` uses existing `Preferences.AUTO_SCRIPTING` constant

---

## Execution Handoff

**Plan complete and saved to `docs/superpowers/plans/2026-06-11-ash-script-full-parity.md`.**

**Two execution options:**

**1. Subagent-Driven (recommended)** — dispatch a fresh subagent per task (P1 Task 1 → review → Task 2 …), fast iteration

**2. Inline Execution** — run tasks in this session using executing-plans, batch checkpoints after each phase (P1 complete → verify → P2 …)

**Which approach?**
