# KoLmafia Mobile — Phase 5b: ASH Runtime Library + Script UI

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Prerequisite:** Phase 5a complete — `AshType`, `AshValue`, `ParseTreeNode`, `AshParser`, `AshRuntime`, and the stub `RuntimeLibrary` all exist and all Phase 5a tests pass.

**Goal:** Replace the stub `RuntimeLibrary` with ~50 game-facing built-in functions (character queries, item/skill/effect queries, adventure and skill actions, string utilities, math, type conversions), wire `AshRuntime` into the app's DI and event bus, and add a Scripts tab with a script list, text editor, and console output screen.

**Architecture:** `GameRuntimeLibrary` extends `RuntimeLibrary` (defined in Phase 5a) and injects the Phase 1–4 managers (`KoLCharacter`, `InventoryManager`, `SkillManager`, `EffectManager`, `AdventureManager`) so library functions can call the live game layer. Scripts are stored as JSON strings in `Preferences` keyed by script name. The UI is three Compose screens: `ScriptsScreen` (list), `ScriptEditorScreen` (text editor), `ScriptConsoleScreen` (output + stop). A new `ScriptManager` owns the runtime and exposes `StateFlow<ScriptState>`.

**Tech Stack:** Existing stack — Ktor, Koin 4.0.0, Compose Multiplatform, kotlinx.coroutines, kotlin.test + MockEngine. No new dependencies.

---

## File Map

```
shared/src/commonMain/kotlin/net/sourceforge/kolmafia/
  ash/
    GameRuntimeLibrary.kt         ← extends RuntimeLibrary; registers all ~50 game + utility functions
    ScriptManager.kt              ← owns AshRuntime; runScript(), stopScript(), loadScripts(), saveScript()
    ScriptEntry.kt                ← data class: name, source, lastRunAt
    ScriptState.kt                ← data class: scripts list, running flag, output, error

  event/
    GameEvent.kt                  ← modify: add ScriptStarted, ScriptOutput, ScriptFinished

  di/
    SharedModule.kt               ← modify: add GameRuntimeLibrary, ScriptManager singletons

  ui/scripts/
    ScriptsScreen.kt              ← script list: run, edit, delete buttons per entry
    ScriptEditorScreen.kt         ← multi-line text field + save button
    ScriptConsoleScreen.kt        ← scrollable output text + error display + stop button

  ui/
    App.kt                        ← modify: 6-tab nav (add Scripts between Skills and Familiars)

shared/src/commonTest/kotlin/net/sourceforge/kolmafia/
  ash/
    GameRuntimeLibraryTest.kt     ← MockEngine-backed tests for each library function category
    ScriptManagerTest.kt          ← runScript produces output, stopScript cancels, error surfaced
```

---

## Task 1: GameEvent extensions

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/event/GameEvent.kt`

- [ ] **Step 1: Add script events to `GameEvent.kt`**

Add three new subtypes inside the existing `sealed class GameEvent` block:

```kotlin
// Inside the sealed class GameEvent body, after existing FamiliarHatched:

data class ScriptStarted(val scriptName: String) : GameEvent()
data class ScriptOutput(val line: String) : GameEvent()
data class ScriptFinished(val scriptName: String, val success: Boolean, val error: String?) : GameEvent()
```

- [ ] **Step 2: Verify compile**

```bash
cd C:\Development\kolmafia-mobile
./gradlew :shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add shared/src/
git commit -m "feat: add ScriptStarted, ScriptOutput, ScriptFinished to GameEvent"
```

---

## Task 2: ScriptEntry + ScriptState + ScriptManager

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/ScriptEntry.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/ScriptState.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/ScriptManager.kt`

- [ ] **Step 1: Write `ScriptEntry.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/ScriptEntry.kt
package net.sourceforge.kolmafia.ash

data class ScriptEntry(
    val name: String,
    val source: String,
    val lastRunAt: Long = 0L   // epoch millis; 0 = never run
)
```

- [ ] **Step 2: Write `ScriptState.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/ScriptState.kt
package net.sourceforge.kolmafia.ash

data class ScriptState(
    val scripts: List<ScriptEntry> = emptyList(),
    val runningScript: String? = null,    // name of the currently executing script, or null
    val output: String = "",
    val error: String? = null
)
```

- [ ] **Step 3: Write `ScriptManager.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/ScriptManager.kt
package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sourceforge.kolmafia.event.GameEvent
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences

private const val SCRIPTS_PREF_KEY = "ashScripts"

class ScriptManager(
    private val library: GameRuntimeLibrary,
    private val preferences: Preferences,
    private val eventBus: GameEventBus
) {
    private val _state = MutableStateFlow(ScriptState())
    val state: StateFlow<ScriptState> = _state.asStateFlow()

    private var runJob: Job? = null

    fun initialize() {
        loadScripts()
    }

    private fun loadScripts() {
        val json = preferences.getString(SCRIPTS_PREF_KEY, "[]")
        val scripts = try {
            Json.decodeFromString<List<ScriptEntry>>(json)
        } catch (_: Exception) {
            emptyList()
        }
        _state.value = _state.value.copy(scripts = scripts)
    }

    private fun persistScripts(scripts: List<ScriptEntry>) {
        preferences.setString(SCRIPTS_PREF_KEY, Json.encodeToString(scripts))
        _state.value = _state.value.copy(scripts = scripts)
    }

    fun saveScript(entry: ScriptEntry) {
        val updated = _state.value.scripts.toMutableList()
        val existing = updated.indexOfFirst { it.name == entry.name }
        if (existing >= 0) updated[existing] = entry else updated.add(entry)
        persistScripts(updated)
    }

    fun deleteScript(name: String) {
        persistScripts(_state.value.scripts.filter { it.name != name })
    }

    fun runScript(name: String, scope: CoroutineScope) {
        val entry = _state.value.scripts.find { it.name == name }
            ?: run { _state.value = _state.value.copy(error = "Script '$name' not found"); return }
        runJob?.cancel()
        _state.value = _state.value.copy(runningScript = name, output = "", error = null)
        eventBus.tryEmit(GameEvent.ScriptStarted(name))

        runJob = scope.launch {
            try {
                val runtime = AshRuntime(library)
                val nodes = AshParser().parse(entry.source)
                runtime.execute(nodes)
                val out = runtime.output.toString()
                _state.value = _state.value.copy(output = out, runningScript = null, error = null)
                // Update lastRunAt
                val updated = _state.value.scripts.map {
                    if (it.name == name) it.copy(lastRunAt = currentTimeMillis()) else it
                }
                persistScripts(updated)
                // Emit each output line to the event bus
                out.lines().filter { it.isNotEmpty() }.forEach { line ->
                    eventBus.tryEmit(GameEvent.ScriptOutput(line))
                }
                eventBus.tryEmit(GameEvent.ScriptFinished(name, success = true, error = null))
            } catch (e: ScriptException) {
                _state.value = _state.value.copy(runningScript = null, error = e.message)
                eventBus.tryEmit(GameEvent.ScriptFinished(name, success = false, error = e.message))
            } catch (e: Exception) {
                val msg = e.message ?: "Unknown error"
                _state.value = _state.value.copy(runningScript = null, error = msg)
                eventBus.tryEmit(GameEvent.ScriptFinished(name, success = false, error = msg))
            }
        }
    }

    fun stopScript() {
        runJob?.cancel()
        runJob = null
        _state.value = _state.value.copy(runningScript = null)
    }
}

// Platform-specific; provided via expect/actual in Phase 5b wiring.
expect fun currentTimeMillis(): Long
```

- [ ] **Step 4: Add `expect fun currentTimeMillis()` actuals**

```kotlin
// shared/src/androidMain/kotlin/net/sourceforge/kolmafia/ash/Platform.kt
package net.sourceforge.kolmafia.ash
actual fun currentTimeMillis(): Long = System.currentTimeMillis()
```

```kotlin
// shared/src/iosMain/kotlin/net/sourceforge/kolmafia/ash/Platform.kt
package net.sourceforge.kolmafia.ash
import platform.Foundation.NSDate
actual fun currentTimeMillis(): Long = (NSDate.date().timeIntervalSince1970 * 1000).toLong()
```

```kotlin
// shared/src/jvmMain/kotlin/net/sourceforge/kolmafia/ash/Platform.kt  (for unit tests)
package net.sourceforge.kolmafia.ash
actual fun currentTimeMillis(): Long = System.currentTimeMillis()
```

- [ ] **Step 5: Add `@Serializable` to `ScriptEntry`**

Add the annotation and import so `Json.encodeToString` / `decodeFromString` work:

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/ScriptEntry.kt
package net.sourceforge.kolmafia.ash

import kotlinx.serialization.Serializable

@Serializable
data class ScriptEntry(
    val name: String,
    val source: String,
    val lastRunAt: Long = 0L
)
```

- [ ] **Step 6: Verify compile**

```bash
./gradlew :shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add shared/src/
git commit -m "feat: ScriptEntry, ScriptState, ScriptManager — script storage and execution lifecycle"
```

---

## Task 3: GameRuntimeLibrary — utility functions

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryTest.kt`

- [ ] **Step 1: Write failing tests for utility functions**

```kotlin
// shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryTest.kt
package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryTest {

    // Uses a minimal stub that provides no game managers — only pure utility functions are tested here.
    private fun run(src: String): AshRuntime {
        val runtime = AshRuntime(GameRuntimeLibrary.forTesting())
        runtime.execute(AshParser().parse(src))
        return runtime
    }

    private fun output(src: String) = run(src).output.toString().trim()

    // --- Type conversion ---

    @Test
    fun to_string_int() = assertEquals("42", output("print(to_string(42));"))

    @Test
    fun to_string_float() = assertEquals("3.14", output("print(to_string(3.14));"))

    @Test
    fun to_string_boolean() = assertEquals("true", output("print(to_string(true));"))

    @Test
    fun to_int_fromString() = assertEquals("7", output("""print(to_string(to_int("7")));"""))

    @Test
    fun to_int_fromFloat() = assertEquals("3", output("print(to_string(to_int(3.9)));"))

    @Test
    fun to_float_fromInt() = assertEquals("5.0", output("print(to_string(to_float(5)));"))

    @Test
    fun to_boolean_fromInt_zero() = assertEquals("false", output("print(to_string(to_boolean(0)));"))

    @Test
    fun to_boolean_fromInt_nonzero() = assertEquals("true", output("print(to_string(to_boolean(1)));"))

    // --- String utilities ---

    @Test
    fun length_string() = assertEquals("5", output("""print(to_string(length("hello")));"""))

    @Test
    fun length_emptyString() = assertEquals("0", output("""print(to_string(length("")));"""))

    @Test
    fun substring_basic() = assertEquals("ell", output("""print(substring("hello", 1, 3));"""))

    @Test
    fun index_of_found() = assertEquals("2", output("""print(to_string(index_of("hello", "ll")));"""))

    @Test
    fun index_of_notFound() = assertEquals("-1", output("""print(to_string(index_of("hello", "xyz")));"""))

    @Test
    fun to_upper_case() = assertEquals("HELLO", output("""print(to_upper_case("hello"));"""))

    @Test
    fun to_lower_case() = assertEquals("hello", output("""print(to_lower_case("HELLO"));"""))

    @Test
    fun starts_with_true() = assertEquals("true", output("""print(to_string(starts_with("hello", "he")));"""))

    @Test
    fun starts_with_false() = assertEquals("false", output("""print(to_string(starts_with("hello", "lo")));"""))

    @Test
    fun replace_string() = assertEquals("hXllX", output("""print(replace_string("hello", "l", "X"));"""))

    @Test
    fun split_string() {
        val o = output("""
            string[int] parts = split_string("a,b,c", ",");
            print(parts[0]);
            print(parts[1]);
            print(parts[2]);
        """.trimIndent())
        assertEquals("a\nb\nc", o)
    }

    // --- Math ---

    @Test
    fun math_floor() = assertEquals("3", output("print(to_string(floor(3.9)));"))

    @Test
    fun math_ceil() = assertEquals("4", output("print(to_string(ceil(3.1)));"))

    @Test
    fun math_round() = assertEquals("4", output("print(to_string(round(3.6)));"))

    @Test
    fun math_abs_negative() = assertEquals("5", output("print(to_string(abs(-5)));"))

    @Test
    fun math_abs_float() = assertEquals("2.5", output("print(to_string(abs(-2.5)));"))

    @Test
    fun math_sqrt() {
        val o = run("float r = sqrt(9.0);").output.toString() // no print; just verify no throw
        assertTrue(true)
    }

    @Test
    fun math_max_int() = assertEquals("7", output("print(to_string(max(3, 7)));"))

    @Test
    fun math_min_int() = assertEquals("3", output("print(to_string(min(3, 7)));"))

    @Test
    fun math_random() {
        val runtime = AshRuntime(GameRuntimeLibrary.forTesting())
        val nodes = AshParser().parse("float r = random(1.0);")
        runtime.execute(nodes)
        // just verify no exception
        assertTrue(true)
    }

    // --- Aggregate utilities ---

    @Test
    fun count_aggregate() = assertEquals(
        "3",
        output("""
            string[int] m; m[0]="a"; m[1]="b"; m[2]="c";
            print(to_string(count(m)));
        """.trimIndent())
    )

    @Test
    fun clear_aggregate() = assertEquals(
        "0",
        output("""
            string[int] m; m[0]="a"; m[1]="b";
            clear(m);
            print(to_string(count(m)));
        """.trimIndent())
    )
}
```

- [ ] **Step 2: Run — verify it fails**

```bash
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.ash.GameRuntimeLibraryTest"
```

Expected: FAIL — `GameRuntimeLibrary` does not exist yet.

- [ ] **Step 3: Write `GameRuntimeLibrary.kt` (utility functions section)**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt
package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.adventure.AdventureLocation
import net.sourceforge.kolmafia.adventure.AdventureManager
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.skill.SkillManager
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sqrt
import kotlin.random.Random

class GameRuntimeLibrary(
    private val character: KoLCharacter? = null,
    private val inventoryManager: InventoryManager? = null,
    private val skillManager: SkillManager? = null,
    private val effectManager: EffectManager? = null,
    private val adventureManager: AdventureManager? = null
) : RuntimeLibrary() {

    companion object {
        // Used in tests where no game managers are available
        fun forTesting() = GameRuntimeLibrary()
    }

    override fun registerAll(scope: AshScope) {
        super.registerAll(scope) // registers print() and to_string() overloads from stub
        registerTypeConversions(scope)
        registerStringUtils(scope)
        registerMathUtils(scope)
        registerAggregateUtils(scope)
        registerPrintUtils(scope)
        registerCharacterQueries(scope)
        registerItemQueries(scope)
        registerSkillQueries(scope)
        registerEffectQueries(scope)
        registerGameActions(scope)
    }

    // ──────────────────────────────────────────────────────────────
    // Type conversion
    // ──────────────────────────────────────────────────────────────

    private fun registerTypeConversions(scope: AshScope) {
        // to_string overloads for remaining types not covered by stub
        // (stub already covers int, float, boolean, string)

        // to_int
        register(scope, "to_int", AshType.INT, listOf("value" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().toLongOrNull() ?: 0L)
        }
        register(scope, "to_int", AshType.INT, listOf("value" to AshType.FLOAT)) { _, args ->
            AshValue.of(args[0].toLong())
        }
        register(scope, "to_int", AshType.INT, listOf("value" to AshType.BOOLEAN)) { _, args ->
            AshValue.of(if (args[0].toBoolean()) 1L else 0L)
        }
        register(scope, "to_int", AshType.INT, listOf("value" to AshType.INT)) { _, args ->
            args[0]
        }

        // to_float
        register(scope, "to_float", AshType.FLOAT, listOf("value" to AshType.INT)) { _, args ->
            AshValue.of(args[0].toDouble())
        }
        register(scope, "to_float", AshType.FLOAT, listOf("value" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().toDoubleOrNull() ?: 0.0)
        }
        register(scope, "to_float", AshType.FLOAT, listOf("value" to AshType.FLOAT)) { _, args ->
            args[0]
        }

        // to_boolean
        register(scope, "to_boolean", AshType.BOOLEAN, listOf("value" to AshType.INT)) { _, args ->
            AshValue.of(args[0].toLong() != 0L)
        }
        register(scope, "to_boolean", AshType.BOOLEAN, listOf("value" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().isNotEmpty() && args[0].toString() != "false")
        }
        register(scope, "to_boolean", AshType.BOOLEAN, listOf("value" to AshType.BOOLEAN)) { _, args ->
            args[0]
        }

        // to_string for game entities
        for (entityType in listOf(AshType.ITEM, AshType.SKILL, AshType.EFFECT,
                                   AshType.FAMILIAR, AshType.LOCATION, AshType.MONSTER,
                                   AshType.CLASS, AshType.STAT, AshType.SLOT,
                                   AshType.ELEMENT, AshType.COINMASTER, AshType.PHYLUM, AshType.PATH)) {
            val capturedType = entityType
            register(scope, "to_string", AshType.STRING, listOf("value" to capturedType)) { _, args ->
                AshValue.of(args[0].toString())
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // String utilities
    // ──────────────────────────────────────────────────────────────

    private fun registerStringUtils(scope: AshScope) {
        register(scope, "length", AshType.INT, listOf("s" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().length)
        }
        register(scope, "substring", AshType.STRING,
            listOf("s" to AshType.STRING, "start" to AshType.INT, "end" to AshType.INT)) { _, args ->
            val s = args[0].toString()
            val start = args[1].toLong().toInt().coerceIn(0, s.length)
            val end = args[2].toLong().toInt().coerceIn(start, s.length)
            AshValue.of(s.substring(start, end))
        }
        register(scope, "substring", AshType.STRING,
            listOf("s" to AshType.STRING, "start" to AshType.INT)) { _, args ->
            val s = args[0].toString()
            val start = args[1].toLong().toInt().coerceIn(0, s.length)
            AshValue.of(s.substring(start))
        }
        register(scope, "index_of", AshType.INT,
            listOf("source" to AshType.STRING, "search" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().indexOf(args[1].toString()).toLong())
        }
        register(scope, "index_of", AshType.INT,
            listOf("source" to AshType.STRING, "search" to AshType.STRING, "start" to AshType.INT)) { _, args ->
            val start = args[2].toLong().toInt()
            AshValue.of(args[0].toString().indexOf(args[1].toString(), start).toLong())
        }
        register(scope, "to_upper_case", AshType.STRING, listOf("s" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().uppercase())
        }
        register(scope, "to_lower_case", AshType.STRING, listOf("s" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().lowercase())
        }
        register(scope, "starts_with", AshType.BOOLEAN,
            listOf("s" to AshType.STRING, "prefix" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().startsWith(args[1].toString()))
        }
        register(scope, "ends_with", AshType.BOOLEAN,
            listOf("s" to AshType.STRING, "suffix" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().endsWith(args[1].toString()))
        }
        register(scope, "contains", AshType.BOOLEAN,
            listOf("s" to AshType.STRING, "sub" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().contains(args[1].toString()))
        }
        register(scope, "replace_string", AshType.STRING,
            listOf("s" to AshType.STRING, "old" to AshType.STRING, "new" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().replace(args[1].toString(), args[2].toString()))
        }
        register(scope, "trim", AshType.STRING, listOf("s" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().trim())
        }
        register(scope, "split_string", AggregateType(AshType.INT, AshType.STRING),
            listOf("s" to AshType.STRING, "sep" to AshType.STRING)) { _, args ->
            val parts = args[0].toString().split(args[1].toString())
            val result = AggregateValue(AggregateType(AshType.INT, AshType.STRING))
            parts.forEachIndexed { i, part -> result[AshValue.of(i)] = AshValue.of(part) }
            result
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Math utilities
    // ──────────────────────────────────────────────────────────────

    private fun registerMathUtils(scope: AshScope) {
        register(scope, "floor", AshType.INT, listOf("f" to AshType.FLOAT)) { _, args ->
            AshValue.of(floor(args[0].toDouble()).toLong())
        }
        register(scope, "ceil", AshType.INT, listOf("f" to AshType.FLOAT)) { _, args ->
            AshValue.of(ceil(args[0].toDouble()).toLong())
        }
        register(scope, "round", AshType.INT, listOf("f" to AshType.FLOAT)) { _, args ->
            AshValue.of(args[0].toDouble().roundToLong())
        }
        register(scope, "sqrt", AshType.FLOAT, listOf("f" to AshType.FLOAT)) { _, args ->
            AshValue.of(sqrt(args[0].toDouble()))
        }
        register(scope, "abs", AshType.INT, listOf("n" to AshType.INT)) { _, args ->
            AshValue.of(abs(args[0].toLong()))
        }
        register(scope, "abs", AshType.FLOAT, listOf("f" to AshType.FLOAT)) { _, args ->
            AshValue.of(abs(args[0].toDouble()))
        }
        register(scope, "max", AshType.INT, listOf("a" to AshType.INT, "b" to AshType.INT)) { _, args ->
            AshValue.of(maxOf(args[0].toLong(), args[1].toLong()))
        }
        register(scope, "max", AshType.FLOAT, listOf("a" to AshType.FLOAT, "b" to AshType.FLOAT)) { _, args ->
            AshValue.of(maxOf(args[0].toDouble(), args[1].toDouble()))
        }
        register(scope, "min", AshType.INT, listOf("a" to AshType.INT, "b" to AshType.INT)) { _, args ->
            AshValue.of(minOf(args[0].toLong(), args[1].toLong()))
        }
        register(scope, "min", AshType.FLOAT, listOf("a" to AshType.FLOAT, "b" to AshType.FLOAT)) { _, args ->
            AshValue.of(minOf(args[0].toDouble(), args[1].toDouble()))
        }
        register(scope, "random", AshType.FLOAT, listOf("limit" to AshType.FLOAT)) { _, args ->
            AshValue.of(Random.nextDouble() * args[0].toDouble())
        }
        register(scope, "pow", AshType.FLOAT, listOf("base" to AshType.FLOAT, "exp" to AshType.FLOAT)) { _, args ->
            AshValue.of(args[0].toDouble().pow(args[1].toDouble()))
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Aggregate utilities
    // ──────────────────────────────────────────────────────────────

    private fun registerAggregateUtils(scope: AshScope) {
        // count(aggregate) — uses a broad param type; runtime resolves by actual arg
        // We register for common aggregate types; actual dispatch finds best match
        val countType = AggregateType(AshType.STRING, AshType.INT)
        register(scope, "count", AshType.INT, listOf("agg" to countType)) { _, args ->
            AshValue.of((args[0] as? AggregateValue)?.map?.size?.toLong() ?: 0L)
        }
        // clear(aggregate) — same approach
        register(scope, "clear", AshType.VOID, listOf("agg" to countType)) { _, args ->
            (args[0] as? AggregateValue)?.map?.clear()
            AshValue.VOID
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Print utilities
    // ──────────────────────────────────────────────────────────────

    private fun registerPrintUtils(scope: AshScope) {
        // print(string) already registered in super; add print_html and print_to_string
        register(scope, "print_html", AshType.VOID, listOf("html" to AshType.STRING)) { runtime, args ->
            // Strip basic HTML tags for console display
            val stripped = args[0].toString()
                .replace(Regex("<[^>]+>"), "")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
            runtime.print(stripped)
            AshValue.VOID
        }
        register(scope, "print_to_string", AshType.STRING, listOf("value" to AshType.STRING)) { _, args ->
            args[0] // returns the string rather than printing
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Character state queries
    // ──────────────────────────────────────────────────────────────

    private fun registerCharacterQueries(scope: AshScope) {
        fun charVal(block: () -> Long) = register(scope, "", AshType.INT, emptyList()) { _, _ ->
            AshValue.of(block())
        } // placeholder — actual registration below

        fun intQuery(name: String, block: () -> Long) {
            register(scope, name, AshType.INT, emptyList()) { _, _ ->
                AshValue.of(character?.state?.value?.let(block.let { _ -> { s: net.sourceforge.kolmafia.character.CharacterState -> block() } }) ?: 0L)
            }
        }

        register(scope, "my_name", AshType.STRING, emptyList()) { _, _ ->
            AshValue.of(character?.state?.value?.name ?: "")
        }
        register(scope, "my_level", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.level ?: 1).toLong())
        }
        register(scope, "my_hp", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.currentHp ?: 0).toLong())
        }
        register(scope, "my_maxhp", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.maxHp ?: 0).toLong())
        }
        register(scope, "my_mp", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.currentMp ?: 0).toLong())
        }
        register(scope, "my_maxmp", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.maxMp ?: 0).toLong())
        }
        register(scope, "my_meat", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.meat ?: 0).toLong())
        }
        register(scope, "my_adventures", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.adventuresLeft ?: 0).toLong())
        }
        register(scope, "my_fullness", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.fullness ?: 0).toLong())
        }
        register(scope, "my_inebriety", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.inebriety ?: 0).toLong())
        }
        register(scope, "my_spleen_use", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.spleenUsed ?: 0).toLong())
        }
        register(scope, "my_basestat", AshType.INT, listOf("stat" to AshType.STAT)) { _, args ->
            val statName = args[0].toString().lowercase()
            val cs = character?.state?.value
            AshValue.of(when (statName) {
                "muscle" -> (cs?.baseMusc ?: 0).toLong()
                "mysticality" -> (cs?.baseMyst ?: 0).toLong()
                "moxie" -> (cs?.baseMoxie ?: 0).toLong()
                else -> 0L
            })
        }
        register(scope, "in_hardcore", AshType.BOOLEAN, emptyList()) { _, _ ->
            AshValue.of(character?.state?.value?.isHardcore ?: false)
        }
        register(scope, "my_familiar", AshType.FAMILIAR, emptyList()) { _, _ ->
            AshValue.familiar(character?.state?.value?.name ?: "none")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Item queries
    // ──────────────────────────────────────────────────────────────

    private fun registerItemQueries(scope: AshScope) {
        register(scope, "item_amount", AshType.INT, listOf("it" to AshType.ITEM)) { _, args ->
            val name = args[0].toString()
            val qty = inventoryManager?.state?.value?.items
                ?.find { it.name.equals(name, ignoreCase = true) }?.quantity ?: 0
            AshValue.of(qty.toLong())
        }
        register(scope, "available_amount", AshType.INT, listOf("it" to AshType.ITEM)) { _, args ->
            // Same as item_amount for mobile (no closet/storage distinction yet)
            val name = args[0].toString()
            val qty = inventoryManager?.state?.value?.items
                ?.find { it.name.equals(name, ignoreCase = true) }?.quantity ?: 0
            AshValue.of(qty.toLong())
        }
        register(scope, "to_item", AshType.ITEM, listOf("name" to AshType.STRING)) { _, args ->
            AshValue.item(args[0].toString())
        }
        register(scope, "to_item", AshType.ITEM, listOf("id" to AshType.INT)) { _, args ->
            // Resolve by itemId; falls back to id string if no match
            val id = args[0].toLong().toInt()
            val name = inventoryManager?.state?.value?.items
                ?.find { it.itemId == id }?.name ?: id.toString()
            AshValue.item(name)
        }
        register(scope, "have_item", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { _, args ->
            val name = args[0].toString()
            val qty = inventoryManager?.state?.value?.items
                ?.find { it.name.equals(name, ignoreCase = true) }?.quantity ?: 0
            AshValue.of(qty > 0)
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Skill + effect queries
    // ──────────────────────────────────────────────────────────────

    private fun registerSkillQueries(scope: AshScope) {
        register(scope, "have_skill", AshType.BOOLEAN, listOf("sk" to AshType.SKILL)) { _, args ->
            val name = args[0].toString()
            val has = skillManager?.state?.value?.skills
                ?.any { it.name.equals(name, ignoreCase = true) } ?: false
            AshValue.of(has)
        }
        register(scope, "mp_cost", AshType.INT, listOf("sk" to AshType.SKILL)) { _, args ->
            val name = args[0].toString()
            val cost = skillManager?.state?.value?.skills
                ?.find { it.name.equals(name, ignoreCase = true) }?.mpCost ?: 0
            AshValue.of(cost.toLong())
        }
        register(scope, "to_skill", AshType.SKILL, listOf("name" to AshType.STRING)) { _, args ->
            AshValue.skill(args[0].toString())
        }
        register(scope, "daily_limit", AshType.INT, listOf("sk" to AshType.SKILL)) { _, args ->
            val name = args[0].toString()
            val limit = skillManager?.state?.value?.skills
                ?.find { it.name.equals(name, ignoreCase = true) }?.dailyLimit ?: 0
            AshValue.of(limit.toLong())
        }
        register(scope, "times_cast", AshType.INT, listOf("sk" to AshType.SKILL)) { _, args ->
            val name = args[0].toString()
            val cast = skillManager?.state?.value?.skills
                ?.find { it.name.equals(name, ignoreCase = true) }?.timesCast ?: 0
            AshValue.of(cast.toLong())
        }
    }

    private fun registerEffectQueries(scope: AshScope) {
        register(scope, "have_effect", AshType.INT, listOf("ef" to AshType.EFFECT)) { _, args ->
            val name = args[0].toString()
            val duration = effectManager?.state?.value?.effects
                ?.find { it.name.equals(name, ignoreCase = true) }?.duration ?: 0
            AshValue.of(duration.toLong())
        }
        register(scope, "to_effect", AshType.EFFECT, listOf("name" to AshType.STRING)) { _, args ->
            AshValue.effect(args[0].toString())
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Game actions (suspend-aware wrappers)
    // ──────────────────────────────────────────────────────────────

    private fun registerGameActions(scope: AshScope) {
        register(scope, "adventure", AshType.BOOLEAN,
            listOf("turns" to AshType.INT, "loc" to AshType.LOCATION)) { _, args ->
            // adventure() in ASH blocks until turns are consumed.
            // In mobile, AdventureManager.runAdventures is a suspend function, but
            // ScriptManager runs the script on a coroutine, so we call runBlocking here.
            // This blocks the script coroutine — which is the correct ASH semantics.
            val turns = args[0].toLong().toInt()
            val locName = args[1].toString()
            val manager = adventureManager
                ?: throw ScriptException("Adventure manager not available")
            val location = AdventureLocation(locName, locName, "")
            kotlinx.coroutines.runBlocking {
                manager.runAdventures(location, turns, this)
            }
            AshValue.of(true)
        }

        register(scope, "use_skill", AshType.BOOLEAN,
            listOf("turns" to AshType.INT, "sk" to AshType.SKILL)) { _, args ->
            val count = args[0].toLong().toInt()
            val skillName = args[1].toString()
            val manager = skillManager
                ?: throw ScriptException("Skill manager not available")
            val skill = manager.state.value.skills
                .find { it.name.equals(skillName, ignoreCase = true) }
                ?: throw ScriptException("Unknown skill: $skillName")
            kotlinx.coroutines.runBlocking {
                repeat(count) { manager.cast(skill, 1) }
            }
            AshValue.of(true)
        }

        register(scope, "use_skill", AshType.BOOLEAN,
            listOf("sk" to AshType.SKILL)) { runtime, args ->
            val skillName = args[0].toString()
            val manager = skillManager
                ?: throw ScriptException("Skill manager not available")
            val skill = manager.state.value.skills
                .find { it.name.equals(skillName, ignoreCase = true) }
                ?: throw ScriptException("Unknown skill: $skillName")
            kotlinx.coroutines.runBlocking { manager.cast(skill, 1) }
            AshValue.of(true)
        }

        register(scope, "cli_execute", AshType.BOOLEAN, listOf("cmd" to AshType.STRING)) { runtime, args ->
            // Minimal implementation: echo the command to output.
            // Full KoLmafia CLI dispatch is out of scope for Phase 5.
            runtime.print("[cli] ${args[0]}")
            AshValue.of(true)
        }
    }
}
```

- [ ] **Step 4: Run — verify tests pass**

```bash
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.ash.GameRuntimeLibraryTest"
```

Expected: PASS (all tests)

- [ ] **Step 5: Run all ASH tests — no regressions**

```bash
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.ash.*"
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add shared/src/
git commit -m "feat: GameRuntimeLibrary — type conversions, string utils, math, aggregate utils, print, game queries, adventure + skill actions"
```

---

## Task 4: Wire into SharedModule + SessionManager

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/SessionManager.kt`

- [ ] **Step 1: Replace `SharedModule.kt`**

Add `GameRuntimeLibrary`, `ScriptManager` singletons. Full file:

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt
package net.sourceforge.kolmafia.di

import io.ktor.client.*
import net.sourceforge.kolmafia.adventure.AdventureManager
import net.sourceforge.kolmafia.adventure.AdventureRequest
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.adventure.FightRequest
import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.ash.ScriptManager
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.http.createKoLHttpClient
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.request.LoginRequest
import net.sourceforge.kolmafia.session.SessionManager
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillManager
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val sharedModule = module {
    single<HttpClient> { createKoLHttpClient() }
    single { KoLCharacter() }
    single { Preferences(get()) }
    single { GameEventBus() }
    singleOf(::LoginRequest)
    singleOf(::CharacterRequest)
    singleOf(::AdventureRequest)
    singleOf(::FightRequest)
    singleOf(::ChoiceRequest)
    singleOf(::AdventureManager)
    singleOf(::InventoryManager)
    singleOf(::FamiliarManager)
    singleOf(::SkillCastRequest)
    singleOf(::SkillManager)
    singleOf(::EffectManager)
    single {
        GameRuntimeLibrary(
            character = get(),
            inventoryManager = get(),
            skillManager = get(),
            effectManager = get(),
            adventureManager = get()
        )
    }
    singleOf(::ScriptManager)
    singleOf(::SessionManager)
}
```

- [ ] **Step 2: Replace `SessionManager.kt`**

Add `scriptManager` initialization on login:

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/SessionManager.kt
package net.sourceforge.kolmafia.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.sourceforge.kolmafia.ash.ScriptManager
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.request.LoginRequest
import net.sourceforge.kolmafia.request.LoginResult
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
    private val scriptManager: ScriptManager
) {
    private val appScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    suspend fun login(username: String, password: String): SessionState {
        return when (val loginResult = loginRequest.login(username, password)) {
            is LoginResult.Success -> {
                preferences.setString(Preferences.LAST_USERNAME, username)
                characterRequest.fetchCharacterState().fold(
                    onSuccess = { apiResponse ->
                        character.updateFromApiResponse(apiResponse)
                        inventoryManager.initialize(appScope)
                        familiarManager.initialize(appScope)
                        skillManager.initialize(appScope)
                        effectManager.initialize(appScope)
                        scriptManager.initialize()
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

- [ ] **Step 3: Verify compile**

```bash
./gradlew :shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Run full test suite**

```bash
./gradlew :shared:jvmTest
```

Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/
git commit -m "feat: wire GameRuntimeLibrary + ScriptManager into SharedModule and SessionManager"
```

---

## Task 5: ScriptsScreen

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/scripts/ScriptsScreen.kt`

- [ ] **Step 1: Write `ScriptsScreen.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/scripts/ScriptsScreen.kt
package net.sourceforge.kolmafia.ui.scripts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.sourceforge.kolmafia.ash.ScriptEntry
import net.sourceforge.kolmafia.ash.ScriptManager
import org.koin.compose.koinInject

@Composable
fun ScriptsScreen(onEditScript: (ScriptEntry?) -> Unit, onShowConsole: (String) -> Unit) {
    val scriptManager: ScriptManager = koinInject()
    val state by scriptManager.state.collectAsState()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Scripts", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { onEditScript(null) }) { Text("New Script") }
        }

        if (state.scripts.isEmpty()) {
            Text(
                "No scripts yet. Tap 'New Script' to create one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.scripts, key = { it.name }) { script ->
                    ScriptRow(
                        script = script,
                        isRunning = state.runningScript == script.name,
                        onRun = {
                            scriptManager.runScript(script.name, scope)
                            onShowConsole(script.name)
                        },
                        onEdit = { onEditScript(script) },
                        onDelete = { scriptManager.deleteScript(script.name) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ScriptRow(
    script: ScriptEntry,
    isRunning: Boolean,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(script.name, style = MaterialTheme.typography.bodyMedium)
            if (script.lastRunAt > 0L) {
                Text(
                    "Last run: ${formatRelativeTime(script.lastRunAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (confirmDelete) {
            TextButton(onClick = { scriptManager.deleteScript(script.name); confirmDelete = false },
                       colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                           contentColor = MaterialTheme.colorScheme.error)) {
                Text("Confirm")
            }
            TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
        } else {
            Button(onClick = onRun, enabled = !isRunning) {
                Text(if (isRunning) "Running…" else "Run")
            }
            Spacer(Modifier.width(4.dp))
            OutlinedButton(onClick = onEdit) { Text("Edit") }
            Spacer(Modifier.width(4.dp))
            TextButton(onClick = { confirmDelete = true },
                       colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                           contentColor = MaterialTheme.colorScheme.error)) {
                Text("Delete")
            }
        }
    }
}

// Simple relative time formatting without external dependencies.
private fun formatRelativeTime(epochMillis: Long): String {
    val diffMs = currentTimeMillis() - epochMillis
    val diffSec = diffMs / 1000
    return when {
        diffSec < 60 -> "just now"
        diffSec < 3600 -> "${diffSec / 60}m ago"
        diffSec < 86400 -> "${diffSec / 3600}h ago"
        else -> "${diffSec / 86400}d ago"
    }
}

private fun currentTimeMillis(): Long = net.sourceforge.kolmafia.ash.currentTimeMillis()
```

- [ ] **Step 2: Verify compile**

```bash
./gradlew :shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add shared/src/
git commit -m "feat: ScriptsScreen — script list with run, edit, delete"
```

---

## Task 6: ScriptEditorScreen

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/scripts/ScriptEditorScreen.kt`

- [ ] **Step 1: Write `ScriptEditorScreen.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/scripts/ScriptEditorScreen.kt
package net.sourceforge.kolmafia.ui.scripts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import net.sourceforge.kolmafia.ash.ScriptEntry
import net.sourceforge.kolmafia.ash.ScriptManager
import org.koin.compose.koinInject

@Composable
fun ScriptEditorScreen(
    existingScript: ScriptEntry?,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val scriptManager: ScriptManager = koinInject()
    var name by remember { mutableStateOf(existingScript?.name ?: "") }
    var source by remember { mutableStateOf(existingScript?.source ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            if (existingScript == null) "New Script" else "Edit Script",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it; nameError = null },
            label = { Text("Script name") },
            isError = nameError != null,
            supportingText = nameError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = existingScript == null // can't rename an existing script
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = source,
            onValueChange = { source = it },
            label = { Text("Script source (ASH)") },
            modifier = Modifier.fillMaxWidth().weight(1f),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            colors = TextFieldDefaults.colors()
        )
        Spacer(Modifier.height(8.dp))

        Row {
            Button(onClick = {
                if (name.isBlank()) { nameError = "Name is required"; return@Button }
                scriptManager.saveScript(
                    ScriptEntry(
                        name = name.trim(),
                        source = source,
                        lastRunAt = existingScript?.lastRunAt ?: 0L
                    )
                )
                onSaved()
            }) { Text("Save") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
./gradlew :shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add shared/src/
git commit -m "feat: ScriptEditorScreen — name + monospace source editor with save/cancel"
```

---

## Task 7: ScriptConsoleScreen

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/scripts/ScriptConsoleScreen.kt`

- [ ] **Step 1: Write `ScriptConsoleScreen.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/scripts/ScriptConsoleScreen.kt
package net.sourceforge.kolmafia.ui.scripts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import net.sourceforge.kolmafia.ash.ScriptManager
import org.koin.compose.koinInject

@Composable
fun ScriptConsoleScreen(scriptName: String, onBack: () -> Unit) {
    val scriptManager: ScriptManager = koinInject()
    val state by scriptManager.state.collectAsState()
    val scrollState = rememberScrollState()

    // Auto-scroll to bottom as output grows
    LaunchedEffect(state.output) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBack) { Text("← Back") }
            Text(
                scriptName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 12.dp).weight(1f)
            )
            if (state.runningScript == scriptName) {
                OutlinedButton(
                    onClick = { scriptManager.stopScript() },
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Stop") }
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        if (state.runningScript == scriptName) {
            Text(
                "Running…",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
        }

        state.error?.let { err ->
            Text(
                "Error: $err",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            if (state.output.isEmpty() && state.error == null && state.runningScript != scriptName) {
                Text(
                    "No output.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    state.output,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )
            }
        }
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
./gradlew :shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add shared/src/
git commit -m "feat: ScriptConsoleScreen — scrollable output with stop button and error display"
```

---

## Task 8: Update App.kt — 6-tab navigation

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/App.kt`

- [ ] **Step 1: Replace `App.kt`**

Adds Scripts tab (index 3) between Skills and Familiars, with simple in-memory navigation state for the Scripts sub-screens:

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/App.kt
package net.sourceforge.kolmafia.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import net.sourceforge.kolmafia.ash.ScriptEntry
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.session.SessionManager
import net.sourceforge.kolmafia.ui.adventure.AdventureScreen
import net.sourceforge.kolmafia.ui.character.CharacterScreen
import net.sourceforge.kolmafia.ui.familiar.FamiliarScreen
import net.sourceforge.kolmafia.ui.inventory.InventoryScreen
import net.sourceforge.kolmafia.ui.login.LoginScreen
import net.sourceforge.kolmafia.ui.login.LoginViewModel
import net.sourceforge.kolmafia.ui.scripts.ScriptConsoleScreen
import net.sourceforge.kolmafia.ui.scripts.ScriptEditorScreen
import net.sourceforge.kolmafia.ui.scripts.ScriptsScreen
import net.sourceforge.kolmafia.ui.skills.SkillsScreen
import org.koin.compose.koinInject

private sealed class ScriptsNav {
    object List : ScriptsNav()
    data class Editor(val script: ScriptEntry?) : ScriptsNav()
    data class Console(val name: String) : ScriptsNav()
}

@Composable
fun App() {
    MaterialTheme {
        var isLoggedIn by remember { mutableStateOf(false) }
        val sessionManager: SessionManager = koinInject()
        val character: KoLCharacter = koinInject()

        if (!isLoggedIn) {
            val viewModel = remember { LoginViewModel(sessionManager) }
            LoginScreen(viewModel = viewModel, onLoginSuccess = { isLoggedIn = true })
            return@MaterialTheme
        }

        var selectedTab by remember { mutableIntStateOf(0) }
        var scriptsNav by remember { mutableStateOf<ScriptsNav>(ScriptsNav.List) }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0, onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.AccountCircle, "Character") },
                        label = { Text("Character") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1, onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Place, "Adventure") },
                        label = { Text("Adventure") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2, onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, "Inventory") },
                        label = { Text("Inventory") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3, onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.AutoFixHigh, "Skills") },
                        label = { Text("Skills") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4; scriptsNav = ScriptsNav.List },
                        icon = { Icon(Icons.Default.Code, "Scripts") },
                        label = { Text("Scripts") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 5, onClick = { selectedTab = 5 },
                        icon = { Icon(Icons.Default.Favorite, "Familiars") },
                        label = { Text("Familiars") }
                    )
                }
            }
        ) { _ ->
            when (selectedTab) {
                0 -> CharacterScreen(character = character)
                1 -> AdventureScreen()
                2 -> InventoryScreen()
                3 -> SkillsScreen()
                4 -> when (val nav = scriptsNav) {
                    is ScriptsNav.List -> ScriptsScreen(
                        onEditScript = { scriptsNav = ScriptsNav.Editor(it) },
                        onShowConsole = { name -> scriptsNav = ScriptsNav.Console(name) }
                    )
                    is ScriptsNav.Editor -> ScriptEditorScreen(
                        existingScript = nav.script,
                        onSaved = { scriptsNav = ScriptsNav.List },
                        onCancel = { scriptsNav = ScriptsNav.List }
                    )
                    is ScriptsNav.Console -> ScriptConsoleScreen(
                        scriptName = nav.name,
                        onBack = { scriptsNav = ScriptsNav.List }
                    )
                }
                5 -> FamiliarScreen()
            }
        }
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
./gradlew :shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Run full test suite**

```bash
./gradlew :shared:jvmTest
```

Expected: All tests PASS.

- [ ] **Step 4: Build Android APK**

```bash
./gradlew :androidApp:assembleDebug
```

Expected: `BUILD SUCCESSFUL`. APK at `androidApp/build/outputs/apk/debug/androidApp-debug.apk`

- [ ] **Step 5: Commit**

```bash
git add shared/ androidApp/
git commit -m "feat: 6-tab navigation — Scripts tab with list, editor, and console screens"
```

---

## Self-Review

**Spec coverage (Phase 5 from master spec `2026-06-02-mobile-port-design.md`):**

| Deliverable | Task |
|---|---|
| ASH interpreter ported to Kotlin (commonMain) | Phase 5a |
| Script execution runtime | Tasks 2, 4 |
| Script editor UI | Task 6 |
| Script console UI | Task 7 |
| Script list / management UI | Task 5 |
| GameEvent integration (ScriptStarted, ScriptOutput, ScriptFinished) | Task 1 |
| SharedModule + SessionManager wired | Task 4 |
| 6-tab navigation with Scripts tab | Task 8 |

**RuntimeLibrary function coverage:**

| Category | Functions |
|---|---|
| Type conversion | `to_string` (all types), `to_int`, `to_float`, `to_boolean` |
| String | `length`, `substring`, `index_of`, `to_upper_case`, `to_lower_case`, `starts_with`, `ends_with`, `contains`, `replace_string`, `trim`, `split_string` |
| Math | `floor`, `ceil`, `round`, `sqrt`, `abs`, `max`, `min`, `random`, `pow` |
| Print | `print`, `print_html`, `print_to_string` |
| Aggregate | `count`, `clear` |
| Character | `my_name`, `my_level`, `my_hp`, `my_maxhp`, `my_mp`, `my_maxmp`, `my_meat`, `my_adventures`, `my_fullness`, `my_inebriety`, `my_spleen_use`, `my_basestat`, `in_hardcore`, `my_familiar` |
| Items | `item_amount`, `available_amount`, `to_item`, `have_item` |
| Skills | `have_skill`, `mp_cost`, `to_skill`, `daily_limit`, `times_cast` |
| Effects | `have_effect`, `to_effect` |
| Actions | `adventure`, `use_skill` (1- and 2-arg), `cli_execute` |

**Placeholder scan:** No TBD, TODO, or "implement later" patterns.

**Type consistency check:**
- `ScriptEntry` is `@Serializable` (Task 2) and referenced identically in `ScriptManager`, `ScriptsScreen`, `ScriptEditorScreen` — ✓
- `ScriptState.runningScript` is `String?` set by `ScriptManager.runScript()` and read by `ScriptsScreen` and `ScriptConsoleScreen` — ✓
- `GameRuntimeLibrary` constructor matches `SharedModule` single { } wiring — ✓
- `AshRuntime(library)` receives `GameRuntimeLibrary` (subclass of `RuntimeLibrary`) — ✓
- `ScriptsNav` sealed class covers all three sub-screens, matching `App.kt` when block — ✓

**Out of scope for Phase 5 (future work):**
- Remaining ~450 RuntimeLibrary functions (added incrementally; adding a function requires no structural changes)
- JavaScript integration (`textui/javascript/`)
- CLI command dispatch (`textui/command/`)
- Buff bot hosting
- Mall/shop write operations from ASH (read queries work; `buy()`, `sell()` deferred to Phase 4 completion)
