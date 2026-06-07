# Phase 10: ASH Function Expansion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 54 new ASH function overloads across 11 domain extension files and 7 HTTP request classes, doubling coverage from ~54 to ~108 functions so core automation scripts run on mobile for the first time.

**Architecture:** `GameRuntimeLibrary.kt` becomes a coordinator (constructor + `registerAll()` only). Each domain lives in a `GameRuntimeLibrary.*.kt` extension file using `internal fun GameRuntimeLibrary.register*()`. A `regFn()` wrapper inside the class body bridges the `protected register()` method, allowing extension functions to call it. All constructor params become `internal val` so extension functions can access them. Seven new HTTP request classes follow the existing Ktor `submitForm` + `Result<T>` pattern. Branch from `feature/phase9-breakfast-banish`.

**Tech Stack:** Kotlin Multiplatform (KMP), Ktor HTTP client, `kotlinx-coroutines`, `kotlin.test` + `ktor-client-mock`, Koin DI.

---

## File Structure

### Modified
| File | Change |
|---|---|
| `shared/src/commonMain/.../ash/GameRuntimeLibrary.kt` | `internal val` all params, `regFn()` wrapper, 11 new `register*()` calls in `registerAll()` |
| `shared/src/commonMain/.../ash/Platform.kt` | Add `expect fun currentDateString()` + `expect fun currentDateTimeString()` |
| `shared/src/jvmMain/.../ash/Platform.kt` | `actual` impls via `java.text.SimpleDateFormat` |
| `shared/src/androidMain/.../ash/Platform.kt` | `actual` impls via `java.text.SimpleDateFormat` |
| `shared/src/iosMain/.../ash/Platform.kt` | `actual` impls via `NSDateFormatter` |
| `shared/src/commonMain/.../preferences/Preferences.kt` | Add `LAST_MONSTER = "_lastMonster"` |
| `shared/src/commonMain/.../adventure/AdventureManager.kt` | Write `LAST_MONSTER` pref after each combat |
| `shared/src/commonMain/.../session/GoalManager.kt` | Add `removeGoal()`, `hasItemGoals()`, `hasMeatGoalSet()`, `hasLevelGoalSet()`, `allGoalsAsStrings()` |
| `shared/src/commonMain/.../familiar/FamiliarManager.kt` | Add `setFamiliar(name: String)` |
| `shared/src/commonMain/.../di/SharedModule.kt` | 7 new `singleOf` + 12 new params in GameRuntimeLibrary block |
| `shared/src/commonTest/.../ash/GameRuntimeLibraryTest.kt` | Delegate to shared helpers |
| `shared/src/commonTest/.../adventure/AdventureManagerTest.kt` | Add `lastMonster_written_after_combat` |

### New source files
| File | Responsibility |
|---|---|
| `shared/src/commonMain/.../ash/GameRuntimeLibrary.Character.kt` | 9 character state queries |
| `shared/src/commonMain/.../ash/GameRuntimeLibrary.Familiar.kt` | 4 familiar queries + `use_familiar` |
| `shared/src/commonMain/.../ash/GameRuntimeLibrary.Equipment.kt` | 4 equipment queries |
| `shared/src/commonMain/.../ash/GameRuntimeLibrary.Modifiers.kt` | 6 modifier overloads (item + effect × 3 types) |
| `shared/src/commonMain/.../ash/GameRuntimeLibrary.Collections.kt` | `get_inventory` (live) + 4 empty stubs |
| `shared/src/commonMain/.../ash/GameRuntimeLibrary.DateTime.kt` | 5 date/time functions |
| `shared/src/commonMain/.../ash/GameRuntimeLibrary.Goals.kt` | 4 goal management functions |
| `shared/src/commonMain/.../ash/GameRuntimeLibrary.Mood.kt` | 2 mood library queries |
| `shared/src/commonMain/.../ash/GameRuntimeLibrary.Prefs.kt` | `get_property` / `set_property` |
| `shared/src/commonMain/.../ash/GameRuntimeLibrary.Combat.kt` | 4 combat state functions |
| `shared/src/commonMain/.../ash/GameRuntimeLibrary.ItemActions.kt` | 9 HTTP item action functions |
| `shared/src/commonMain/.../request/UseItemRequest.kt` | `inv_use.php` / `multiuse.php` |
| `shared/src/commonMain/.../request/EatFoodRequest.kt` | `inv_eat.php` |
| `shared/src/commonMain/.../request/DrinkBoozeRequest.kt` | `inv_drink.php` |
| `shared/src/commonMain/.../request/ChewRequest.kt` | `multiuse.php` (spleen) |
| `shared/src/commonMain/.../request/AutosellRequest.kt` | `sellstuff_multi.php` |
| `shared/src/commonMain/.../request/ClosetRequest.kt` | `closet.php?action=put` |
| `shared/src/commonMain/.../request/StorageRequest.kt` | `storage.php?action=pullitem` |

### New test files
| File | Key assertions |
|---|---|
| `shared/src/commonTest/.../ash/GameRuntimeLibraryTestHelpers.kt` | Shared `runLib`, `outputLib`, `prefs()` |
| `shared/src/commonTest/.../ash/GameRuntimeLibraryCharacterTest.kt` | `my_class`, `in_run`, `can_interact` |
| `shared/src/commonTest/.../ash/GameRuntimeLibraryFamiliarTest.kt` | `have_familiar`, `to_familiar`, `my_familiar_weight` |
| `shared/src/commonTest/.../ash/GameRuntimeLibraryEquipmentTest.kt` | `equipped_item`, `have_equipped`, `to_slot` |
| `shared/src/commonTest/.../ash/GameRuntimeLibraryModifiersTest.kt` | returns `0.0`/`false`/`""` for unknown |
| `shared/src/commonTest/.../ash/GameRuntimeLibraryCollectionsTest.kt` | `get_inventory` count; stubs = 0 |
| `shared/src/commonTest/.../ash/GameRuntimeLibraryDateTimeTest.kt` | `today_to_string` pattern; `rollover` ≥ 0 |
| `shared/src/commonTest/.../ash/GameRuntimeLibraryGoalsTest.kt` | add→exist; remove→gone; `get_goals` count |
| `shared/src/commonTest/.../ash/GameRuntimeLibraryMoodTest.kt` | `get_moods` names; empty library |
| `shared/src/commonTest/.../ash/GameRuntimeLibraryPrefsTest.kt` | set+get round-trip; unknown key = `""` |
| `shared/src/commonTest/.../ash/GameRuntimeLibraryCombatTest.kt` | stubs false; `last_monster` reads pref |
| `shared/src/commonTest/.../ash/GameRuntimeLibraryItemActionsTest.kt` | `use` params; null request → false |
| `shared/src/commonTest/.../request/UseItemRequestTest.kt` | single→`inv_use.php`; multi→`multiuse.php` |
| `shared/src/commonTest/.../request/EatFoodRequestTest.kt` | params; "too full" → failure |
| `shared/src/commonTest/.../request/DrinkBoozeRequestTest.kt` | params; "too drunk" → failure |
| `shared/src/commonTest/.../request/ChewRequestTest.kt` | `multiuse.php`; error → failure |
| `shared/src/commonTest/.../request/AutosellRequestTest.kt` | parses meat; 0 if no pattern |
| `shared/src/commonTest/.../request/ClosetRequestTest.kt` | `action=put`; item+qty present |
| `shared/src/commonTest/.../request/StorageRequestTest.kt` | `action=pullitem`; restriction → failure |

---


### Task T1: Infrastructure — `regFn` wrapper, visibility, test helpers

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryTestHelpers.kt`
- Modify: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryTest.kt`

- [ ] **Step 1: Create shared test helpers**

Create `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryTestHelpers.kt`:

```kotlin
package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences

fun runLib(lib: GameRuntimeLibrary, src: String): AshRuntime {
    val runtime = AshRuntime(lib)
    runtime.execute(AshParser().parse(src))
    return runtime
}

fun outputLib(lib: GameRuntimeLibrary, src: String): String =
    runLib(lib, src).output.toString().trim()

fun prefs(): Preferences = Preferences(MapSettings())
```

- [ ] **Step 2: Update `GameRuntimeLibraryTest.kt` to use shared helpers**

Replace the four private methods (`runWithCharacter`, `run`, `output`, `prefs`) at the top of `GameRuntimeLibraryTest.kt` with a single local wrapper so all `@Test` methods continue to compile unchanged:

```kotlin
package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.banish.Banisher
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryTest {

    // delegates to top-level helper — test bodies unchanged
    private fun output(src: String): String =
        outputLib(GameRuntimeLibrary.forTesting(), src)

    // All @Test methods follow here unchanged ...
```

- [ ] **Step 3: Run tests — should PASS**

```
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.ash.GameRuntimeLibraryTest"
```

Expected: BUILD SUCCESSFUL, all existing tests pass.

- [ ] **Step 4: Change `private val` → `internal val` and add new params + `regFn` wrapper**

In `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`, replace the class header and add `regFn()` after the companion object:

```kotlin
class GameRuntimeLibrary(
    // existing params — changed private → internal
    internal val character: KoLCharacter? = null,
    internal val inventoryManager: InventoryManager? = null,
    internal val skillManager: SkillManager? = null,
    internal val effectManager: EffectManager? = null,
    internal val adventureManager: AdventureManager? = null,
    internal val banishManager: BanishManager? = null,
    // new params — all nullable so forTesting() and existing tests still compile
    internal val familiarManager: net.sourceforge.kolmafia.familiar.FamiliarManager? = null,
    internal val goalManager: net.sourceforge.kolmafia.session.GoalManager? = null,
    internal val moodManager: net.sourceforge.kolmafia.mood.MoodManager? = null,
    internal val preferences: net.sourceforge.kolmafia.preferences.Preferences? = null,
    internal val gameDatabase: net.sourceforge.kolmafia.data.GameDatabase? = null,
    internal val useItemRequest: net.sourceforge.kolmafia.request.UseItemRequest? = null,
    internal val eatFoodRequest: net.sourceforge.kolmafia.request.EatFoodRequest? = null,
    internal val drinkBoozeRequest: net.sourceforge.kolmafia.request.DrinkBoozeRequest? = null,
    internal val chewRequest: net.sourceforge.kolmafia.request.ChewRequest? = null,
    internal val autosellRequest: net.sourceforge.kolmafia.request.AutosellRequest? = null,
    internal val closetRequest: net.sourceforge.kolmafia.request.ClosetRequest? = null,
    internal val storageRequest: net.sourceforge.kolmafia.request.StorageRequest? = null,
) : RuntimeLibrary() {

    companion object {
        fun forTesting() = GameRuntimeLibrary()
    }

    /** Bridges the protected [register] so extension functions in this module can call it. */
    internal fun regFn(
        scope: AshScope,
        name: String,
        returnType: AshType,
        params: List<Pair<String, AshType>>,
        impl: (AshRuntimeContext, List<AshValue>) -> AshValue
    ) = register(scope, name, returnType, params, impl)

    override fun registerAll(scope: AshScope) {
        super.registerAll(scope)
        registerTypeConversions(scope)
        registerStringUtils(scope)
        registerMathUtils(scope)
        registerAggregateUtils(scope)
        registerPrintUtils(scope)
        registerCharacterQueries(scope)      // existing
        registerItemQueries(scope)           // existing
        registerSkillQueries(scope)          // existing
        registerEffectQueries(scope)         // existing
        registerGameActions(scope)           // existing
        registerBanishQueries(scope)         // existing
        // new extension calls (added as tasks T4–T13 are implemented):
        // registerCharacterExtensions(scope)
        // registerFamiliarQueries(scope)
        // registerEquipmentQueries(scope)
        // registerModifierQueries(scope)
        // registerCollectionQueries(scope)
        // registerDateTimeQueries(scope)
        // registerGoalQueries(scope)
        // registerMoodQueries(scope)
        // registerPreferenceAccess(scope)
        // registerCombatStubs(scope)
        // registerItemActions(scope)
    }

    // ... all private register* methods remain unchanged below ...
```

Note: the commented-out lines will be uncommented one by one as each extension task is completed.

- [ ] **Step 5: Run all ASH tests — should PASS**

```
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.ash.*"
```

Expected: BUILD SUCCESSFUL. The new params all default to null; existing behaviour unchanged.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryTestHelpers.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryTest.kt
git commit -m "refactor: GameRuntimeLibrary infra — regFn wrapper, internal visibility, shared test helpers"
```

---


### Task T2: `Preferences.LAST_MONSTER` + `AdventureManager` combat tracking

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/preferences/Preferences.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureManager.kt`
- Modify: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/AdventureManagerTest.kt`

- [ ] **Step 1: Write failing test in `AdventureManagerTest.kt`**

Add this test inside `AdventureManagerTest` (not the companion object), after the existing tests:

```kotlin
@Test
fun lastMonster_written_after_combat() = runTest {
    val prefs = Preferences(MapSettings())
    val engine = MockEngine { request ->
        when {
            request.url.encodedPath.contains("adventure.php") ->
                respond(COMBAT_HTML, HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "text/html"))
            request.url.encodedPath.contains("fight.php") ->
                respond(COMBAT_WIN_HTML, HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "text/html"))
            request.url.encodedPath.contains("api.php") ->
                respond(STATUS_JSON_ADVENTURES_LEFT, HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"))
            else -> respond("", HttpStatusCode.NotFound)
        }
    }
    val client = HttpClient(engine) {
        install(HttpCookies)
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }
    val manager = AdventureManager(
        AdventureRequest(client),
        FightRequest(client),
        ChoiceRequest(client),
        CharacterRequest(client),
        KoLCharacter(),
        prefs,
        GameEventBus()
    )
    val job = manager.runAdventures(testLocation, 1, this)
    job.join()
    assertEquals("bunny", prefs.getString(Preferences.LAST_MONSTER, ""))
}
```

`Preferences.LAST_MONSTER` does not exist yet, so this will NOT compile. That's expected.

- [ ] **Step 2: Add `LAST_MONSTER` constant to `Preferences.kt`**

In `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/preferences/Preferences.kt`, add after `LAST_DAYCOUNT`:

```kotlin
        const val LAST_DAYCOUNT             = "lastBreakfastDaycount"   // int; -1 = never stored
        const val LAST_MONSTER              = "_lastMonster"             // string; last monster fought
```

- [ ] **Step 3: Run test — should now COMPILE but FAIL** (pref not written yet)

```
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.adventure.AdventureManagerTest.lastMonster_written_after_combat"
```

Expected: compiles, test FAILs (pref value is `""`, expected `"bunny"`).

- [ ] **Step 4: Write `LAST_MONSTER` in `AdventureManager.resolveCombat()`**

In `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureManager.kt`, in the `resolveCombat()` method, add one line immediately after the `CombatFinished` event emit:

```kotlin
    private suspend fun resolveCombat(location: AdventureLocation): AdventureResult.Combat? {
        val macro = MacroStrategy.forLocation(location.id, preferences)
        val fightHtml = fightRequest.fight(macro).getOrElse {
            eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.NetworkError(it)))
            return null
        }
        val result = AdventureParser.parseFightResult(fightHtml)
        eventBus.emit(GameEvent.CombatFinished(result.won, result.monster))
        preferences.setString(Preferences.LAST_MONSTER, result.monster)   // <-- ADD THIS LINE
        emitItemEvents(result.itemsGained)
        // ... rest unchanged
```

- [ ] **Step 5: Run test — should PASS**

```
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.adventure.AdventureManagerTest.lastMonster_written_after_combat"
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Run full test suite**

```
./gradlew shared:jvmTest
```

Expected: BUILD SUCCESSFUL. No regressions.

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/preferences/Preferences.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureManager.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/AdventureManagerTest.kt
git commit -m "feat: track last monster in _lastMonster pref after each combat"
```

---

### Task T3: `GoalManager` helpers + `FamiliarManager.setFamiliar()`

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/GoalManager.kt`
- Modify: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/session/GoalManagerTest.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/familiar/FamiliarManager.kt`
- Modify: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/familiar/FamiliarManagerTest.kt`

- [ ] **Step 1: Write failing tests for new GoalManager methods**

In the GoalManager test file, add:

```kotlin
@Test
fun hasItemGoals_trueAfterAddingByName() {
    val gm = GoalManager()
    assertFalse(gm.hasItemGoals())
    gm.addItemGoalByName("seal tooth")
    assertTrue(gm.hasItemGoals())
}

@Test
fun removeGoal_removesNameGoal() {
    val gm = GoalManager()
    gm.addItemGoalByName("seal tooth")
    gm.removeGoal("seal tooth")
    assertFalse(gm.hasItemGoals())
}

@Test
fun hasMeatGoalSet_trueAfterSetMeatGoal() {
    val gm = GoalManager()
    assertFalse(gm.hasMeatGoalSet())
    gm.setMeatGoal(5000)
    assertTrue(gm.hasMeatGoalSet())
}

@Test
fun allGoalsAsStrings_listsAllGoals() {
    val gm = GoalManager()
    gm.addItemGoalByName("seal tooth")
    gm.setMeatGoal(1000)
    gm.setLevelGoal(10)
    val goals = gm.allGoalsAsStrings()
    assertTrue(goals.any { it.contains("seal tooth") })
    assertTrue(goals.any { it.contains("meat") })
    assertTrue(goals.any { it.contains("level") })
}
```

- [ ] **Step 2: Run tests — should FAIL** (methods don't exist)

```
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.session.GoalManagerTest*"
```

Expected: compilation error — `hasItemGoals`, `removeGoal`, `hasMeatGoalSet`, `allGoalsAsStrings` not found.

- [ ] **Step 3: Add methods to `GoalManager.kt`**

In `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/GoalManager.kt`, append before the closing `}`:

```kotlin
    // ── Phase 10 ASH helpers ──────────────────────────────────────────────────

    /** True if any item goal (by ID or name) is active. */
    fun hasItemGoals(): Boolean = _itemGoalIds.isNotEmpty() || _itemGoalNames.isNotEmpty()

    /** True if a meat goal has been set (regardless of current meat). */
    fun hasMeatGoalSet(): Boolean = meatGoal != null

    /** True if a level goal has been set (regardless of current level). */
    fun hasLevelGoalSet(): Boolean = levelGoal != null

    /** Remove the first item goal matching [itemName] (case-insensitive). */
    fun removeGoal(itemName: String) { removeItemGoalByName(itemName) }

    /** Serialize all active goals as human-readable strings. */
    fun allGoalsAsStrings(): List<String> = buildList {
        _itemGoalIds.forEach  { add("item id:$it") }
        _itemGoalNames.forEach { add("item name:$it") }
        meatGoal?.let  { add("meat:$it") }
        levelGoal?.let { add("level:$it") }
    }
```

- [ ] **Step 4: Run GoalManager tests — should PASS**

```
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.session.GoalManagerTest*"
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Write failing test for `FamiliarManager.setFamiliar()`**

In the FamiliarManager test file, add:

```kotlin
@Test
fun setFamiliar_byName_switchesToCorrectFamiliar() = runTest {
    val bodies = mutableListOf<String>()
    val engine = MockEngine { request ->
        bodies += request.url.fullPath
        respond("", HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "text/html"))
    }
    val client = HttpClient(engine)
    val bus = GameEventBus()
    val mgr = FamiliarManager(client, bus)
    // seed state with a known familiar
    mgr.testSetState(FamiliarState(
        activeFamiliar = null,
        ownedFamiliars = listOf(FamiliarData(id = 3, name = "Biscuit", race = "Angry Goat", weight = 10))
    ))
    val result = mgr.setFamiliar("Angry Goat")
    assertTrue(result.isSuccess)
    assertTrue(bodies.any { it.contains("whichfamiliar=3") }, "Expected whichfamiliar=3 in: $bodies")
}

@Test
fun setFamiliar_notFound_returnsFailure() = runTest {
    val client = HttpClient(MockEngine { respond("") })
    val mgr = FamiliarManager(client, GameEventBus())
    mgr.testSetState(FamiliarState())
    val result = mgr.setFamiliar("No Such Familiar")
    assertTrue(result.isFailure)
}
```

Note: `testSetState` and `FamiliarData` need to be importable. `testSetState` is a test-only helper you'll add to `FamiliarManager` (or its test companion). The simplest approach: expose an `internal fun testSetState(state: FamiliarState)` on `FamiliarManager` for tests.

- [ ] **Step 6: Add `setFamiliar()` + `testSetState()` to `FamiliarManager.kt`**

In `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/familiar/FamiliarManager.kt`, add:

```kotlin
    /** Sets the active familiar by species name (race). Returns failure if not owned. */
    suspend fun setFamiliar(name: String): Result<Unit> {
        val familiar = state.value.ownedFamiliars
            .find { it.race.equals(name, ignoreCase = true) }
            ?: return Result.failure(Exception("Familiar not owned: $name"))
        return switchFamiliar(familiar)
    }

    /** Test hook — sets internal state without going through the network. */
    internal fun testSetState(state: FamiliarState) { _state.value = state }
```

- [ ] **Step 7: Run FamiliarManager tests — should PASS**

```
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.familiar.FamiliarManagerTest*"
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Run full suite**

```
./gradlew shared:jvmTest
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 9: Commit**

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/GoalManager.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/familiar/FamiliarManager.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/session/GoalManagerTest.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/familiar/FamiliarManagerTest.kt
git commit -m "feat: GoalManager helper predicates + FamiliarManager.setFamiliar()"
```

---


### Task T4: Character extension — `GameRuntimeLibrary.Character.kt`

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Character.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryCharacterTest.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt` (uncomment `registerCharacterExtensions`)

- [ ] **Step 1: Write failing tests**

Create `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryCharacterTest.kt`:

```kotlin
package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryCharacterTest {

    private fun libWith(block: CharacterApiResponse.() -> Unit): GameRuntimeLibrary {
        val char = KoLCharacter()
        char.update(CharacterApiResponse().apply(block))
        return GameRuntimeLibrary(character = char)
    }

    @Test
    fun myClass_sealClubber() {
        val lib = libWith { characterClass = 1 }
        assertEquals("Seal Clubber", outputLib(lib, "print(my_class());"))
    }

    @Test
    fun inRun_falseWhenKingLiberated() {
        val lib = libWith { kingLiberated = true }
        assertEquals("false", outputLib(lib, "print(to_string(in_run()));"))
    }

    @Test
    fun inRun_trueWhenActive() {
        val lib = libWith { kingLiberated = false }
        assertEquals("true", outputLib(lib, "print(to_string(in_run()));"))
    }

    @Test
    fun canInteract_falseInHardcore() {
        val lib = libWith { isHardcore = true }
        assertEquals("false", outputLib(lib, "print(to_string(can_interact()));"))
    }

    @Test
    fun mySign_returnsZodiacSign() {
        val lib = libWith { zodiacSign = "Opossum" }
        assertEquals("Opossum", outputLib(lib, "print(my_sign());"))
    }

    @Test
    fun underStandard_alwaysFalse() {
        assertEquals("false",
            outputLib(GameRuntimeLibrary.forTesting(), "print(to_string(under_standard()));"))
    }

    @Test
    fun ascensionNumber_returns42() {
        val lib = libWith { ascensionNumber = 42 }
        assertEquals("42", outputLib(lib, "print(to_string(ascension_number()));"))
    }

    @Test
    fun myThrall_emptyString() {
        assertEquals("", outputLib(GameRuntimeLibrary.forTesting(), "print(my_thrall());"))
    }
}
```

- [ ] **Step 2: Run tests — should FAIL** (functions not registered)

```
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.ash.GameRuntimeLibraryCharacterTest"
```

Expected: FAIL — `my_class`, `in_run`, etc. not found as library functions.

- [ ] **Step 3: Create `GameRuntimeLibrary.Character.kt`**

```kotlin
package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerCharacterExtensions(scope: AshScope) {

    regFn(scope, "my_class", AshType.CLASS, emptyList()) { _, _ ->
        AshValue(AshType.CLASS,
            character?.state?.value?.characterClassEnum?.displayName ?: "")
    }

    regFn(scope, "my_path", AshType.PATH, emptyList()) { _, _ ->
        AshValue(AshType.PATH,
            character?.state?.value?.ascensionPath?.apiName ?: "None")
    }

    regFn(scope, "my_sign", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(character?.state?.value?.zodiacSign ?: "")
    }

    regFn(scope, "my_primestat", AshType.STAT, emptyList()) { _, _ ->
        AshValue(AshType.STAT,
            character?.state?.value?.characterClassEnum?.primeStatName ?: "Muscle")
    }

    regFn(scope, "in_run", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(!(character?.state?.value?.kingLiberated ?: true))
    }

    // Stub: CharacterState has no underStandard field
    regFn(scope, "under_standard", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(false)
    }

    regFn(scope, "ascension_number", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((character?.state?.value?.ascensionNumber ?: 0).toLong())
    }

    regFn(scope, "can_interact", AshType.BOOLEAN, emptyList()) { _, _ ->
        val cs = character?.state?.value
        AshValue.of(cs != null && !cs.isHardcore && !cs.isInRonin)
    }

    // Stub: no THRALL AshType yet
    regFn(scope, "my_thrall", AshType.STRING, emptyList()) { _, _ ->
        AshValue.EMPTY_STRING
    }
}
```

- [ ] **Step 4: Uncomment `registerCharacterExtensions(scope)` in `GameRuntimeLibrary.kt`**

In `registerAll()`, replace:
```kotlin
        // registerCharacterExtensions(scope)
```
with:
```kotlin
        registerCharacterExtensions(scope)
```

- [ ] **Step 5: Run character tests — should PASS**

```
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.ash.GameRuntimeLibraryCharacterTest"
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Run full suite and commit**

```
./gradlew shared:jvmTest
```

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Character.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryCharacterTest.kt
git commit -m "feat: ASH character extension — my_class, in_run, can_interact, my_path, my_sign, ascension_number"
```

---

### Task T5: Familiar extension — `GameRuntimeLibrary.Familiar.kt`

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Familiar.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryFamiliarTest.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`

- [ ] **Step 1: Write failing tests**

Create `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryFamiliarTest.kt`:

```kotlin
package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.familiar.FamiliarState
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryFamiliarTest {

    private val goat = FamiliarData(id = 7, name = "Biscuit", race = "Angry Goat", weight = 12)

    private fun libWithGoat(): GameRuntimeLibrary {
        val fm = FamiliarManager(HttpClient(MockEngine { respond("") }), GameEventBus())
        fm.testSetState(FamiliarState(ownedFamiliars = listOf(goat)))
        val char = KoLCharacter()
        char.update(CharacterApiResponse().apply { familiarWeight = 12 })
        return GameRuntimeLibrary(character = char, familiarManager = fm)
    }

    @Test
    fun haveFamiliar_trueWhenOwned() {
        assertEquals("true",
            outputLib(libWithGoat(), "print(to_string(have_familiar(\$familiar[Angry Goat])));"))
    }

    @Test
    fun haveFamiliar_falseWhenNotOwned() {
        val fm = FamiliarManager(HttpClient(MockEngine { respond("") }), GameEventBus())
        val lib = GameRuntimeLibrary(familiarManager = fm)
        assertEquals("false",
            outputLib(lib, "print(to_string(have_familiar(\$familiar[Purse Rat])));"))
    }

    @Test
    fun toFamiliar_roundTripsName() {
        assertEquals("Angry Goat",
            outputLib(GameRuntimeLibrary.forTesting(), "print(to_familiar(\"Angry Goat\"));"))
    }

    @Test
    fun myFamiliarWeight_returnsFromCharacterState() {
        assertEquals("12",
            outputLib(libWithGoat(), "print(to_string(my_familiar_weight()));"))
    }
}
```

- [ ] **Step 2: Run tests — should FAIL**

```
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.ash.GameRuntimeLibraryFamiliarTest"
```

- [ ] **Step 3: Create `GameRuntimeLibrary.Familiar.kt`**

```kotlin
package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerFamiliarQueries(scope: AshScope) {

    regFn(scope, "have_familiar", AshType.BOOLEAN,
        listOf("fam" to AshType.FAMILIAR)) { _, args ->
        val name = args[0].toString()
        val has = familiarManager?.state?.value?.ownedFamiliars
            ?.any { it.race.equals(name, ignoreCase = true) } ?: false
        AshValue.of(has)
    }

    regFn(scope, "my_familiar_weight", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((character?.state?.value?.familiarWeight ?: 0).toLong())
    }

    regFn(scope, "to_familiar", AshType.FAMILIAR,
        listOf("name" to AshType.STRING)) { _, args ->
        AshValue.familiar(args[0].toString())
    }

    // use_familiar(familiar) -> boolean  (familiar.php?action=newfam)
    regFn(scope, "use_familiar", AshType.BOOLEAN,
        listOf("fam" to AshType.FAMILIAR)) { _, args ->
        val fm = familiarManager ?: return@regFn AshValue.of(false)
        val success = kotlinx.coroutines.runBlocking {
            fm.setFamiliar(args[0].toString())
        }.isSuccess
        AshValue.of(success)
    }
}
```

- [ ] **Step 4: Uncomment `registerFamiliarQueries(scope)` in `GameRuntimeLibrary.kt`**

- [ ] **Step 5: Run familiar tests — should PASS**

```
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.ash.GameRuntimeLibraryFamiliarTest"
```

- [ ] **Step 6: Run full suite and commit**

```
./gradlew shared:jvmTest
```

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Familiar.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryFamiliarTest.kt
git commit -m "feat: ASH familiar extension — have_familiar, my_familiar_weight, to_familiar, use_familiar"
```

---

### Task T6: Equipment extension — `GameRuntimeLibrary.Equipment.kt`

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Equipment.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryEquipmentTest.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`

- [ ] **Step 1: Write failing tests**

Create `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryEquipmentTest.kt`:

```kotlin
package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryEquipmentTest {

    private fun libWithHat(itemName: String): GameRuntimeLibrary {
        val char = KoLCharacter()
        char.update(CharacterApiResponse().apply {
            equipment = mapOf(EquipmentSlot.HAT to itemName)
        })
        return GameRuntimeLibrary(character = char)
    }

    @Test
    fun equippedItem_returnsItemName() {
        val lib = libWithHat("spooky scarecrow")
        assertEquals("spooky scarecrow",
            outputLib(lib, "print(equipped_item(\$slot[Hat]));"))
    }

    @Test
    fun equippedItem_noneForEmptySlot() {
        assertEquals("none",
            outputLib(GameRuntimeLibrary.forTesting(), "print(equipped_item(\$slot[Hat]));"))
    }

    @Test
    fun haveEquipped_trueWhenWearing() {
        val lib = libWithHat("spooky scarecrow")
        assertEquals("true",
            outputLib(lib, "print(to_string(have_equipped(\$item[spooky scarecrow])));"))
    }

    @Test
    fun haveEquipped_falseWhenNotWearing() {
        assertEquals("false",
            outputLib(GameRuntimeLibrary.forTesting(),
                "print(to_string(have_equipped(\$item[spooky scarecrow])));"))
    }

    @Test
    fun toSlot_roundTripsName() {
        assertEquals("Hat",
            outputLib(GameRuntimeLibrary.forTesting(), "print(to_slot(\"Hat\"));"))
    }

    @Test
    fun slotToItem_aliasForEquippedItem() {
        val lib = libWithHat("spooky scarecrow")
        assertEquals("spooky scarecrow",
            outputLib(lib, "print(slot_to_item(\$slot[Hat]));"))
    }
}
```

- [ ] **Step 2: Run tests — should FAIL**

```
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.ash.GameRuntimeLibraryEquipmentTest"
```

- [ ] **Step 3: Create `GameRuntimeLibrary.Equipment.kt`**

```kotlin
package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.character.EquipmentSlot

internal fun GameRuntimeLibrary.registerEquipmentQueries(scope: AshScope) {

    fun resolveSlot(slotName: String): String? {
        val slot = EquipmentSlot.entries.find { s ->
            s.displayName.equals(slotName, ignoreCase = true)
                || s.apiKey.equals(slotName, ignoreCase = true)
        }
        val itemName = slot?.let { character?.state?.value?.equipment?.get(it) }
        return if (itemName.isNullOrBlank()) null else itemName
    }

    regFn(scope, "equipped_item", AshType.ITEM,
        listOf("slot" to AshType.SLOT)) { _, args ->
        AshValue.item(resolveSlot(args[0].toString()) ?: "none")
    }

    regFn(scope, "have_equipped", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM)) { _, args ->
        val name = args[0].toString()
        val has = character?.state?.value?.equipment?.values
            ?.any { it.equals(name, ignoreCase = true) } ?: false
        AshValue.of(has)
    }

    regFn(scope, "to_slot", AshType.SLOT,
        listOf("name" to AshType.STRING)) { _, args ->
        AshValue(AshType.SLOT, args[0].toString())
    }

    // alias for equipped_item
    regFn(scope, "slot_to_item", AshType.ITEM,
        listOf("slot" to AshType.SLOT)) { _, args ->
        AshValue.item(resolveSlot(args[0].toString()) ?: "none")
    }
}
```

- [ ] **Step 4: Uncomment `registerEquipmentQueries(scope)` in `GameRuntimeLibrary.kt`**

- [ ] **Step 5: Run equipment tests — should PASS**

```
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.ash.GameRuntimeLibraryEquipmentTest"
```

- [ ] **Step 6: Run full suite and commit**

```
./gradlew shared:jvmTest
```

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Equipment.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryEquipmentTest.kt
git commit -m "feat: ASH equipment extension — equipped_item, have_equipped, to_slot, slot_to_item"
```

---


### Task T7: Platform date functions + DateTime extension

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/Platform.kt`
- Modify: `shared/src/jvmMain/kotlin/net/sourceforge/kolmafia/ash/Platform.kt`
- Modify: `shared/src/androidMain/kotlin/net/sourceforge/kolmafia/ash/Platform.kt`
- Modify: `shared/src/iosMain/kotlin/net/sourceforge/kolmafia/ash/Platform.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.DateTime.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryDateTimeTest.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`

- [ ] **Step 1: Write failing tests**

Create `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryDateTimeTest.kt`:

```kotlin
package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryDateTimeTest {

    private val lib = GameRuntimeLibrary.forTesting()

    @Test
    fun todayToString_matchesDatePattern() {
        val result = outputLib(lib, "print(today_to_string());")
        assertTrue(result.matches(Regex("""\d{8}""")),
            "Expected YYYYMMDD but got: $result")
    }

    @Test
    fun nowToString_matchesDateTimePattern() {
        val result = outputLib(lib, "print(now_to_string());")
        assertTrue(result.matches(Regex("""\d{8} \d{2}:\d{2}:\d{2}""")),
            "Expected YYYYMMDD HH:mm:ss but got: $result")
    }

    @Test
    fun gamedayToString_matchesDatePattern() {
        val result = outputLib(lib, "print(gameday_to_string());")
        assertTrue(result.matches(Regex("""\d{8}""")),
            "Expected YYYYMMDD but got: $result")
    }

    @Test
    fun rollover_nonnegativeWithNoCharacter() {
        val result = outputLib(lib, "print(to_string(rollover()));").toIntOrNull() ?: -1
        assertTrue(result >= 0, "rollover() should be >= 0 but got $result")
    }

    @Test
    fun moonPhase_returnsInt() {
        val result = outputLib(lib, "print(to_string(moon_phase()));").toIntOrNull()
        assertTrue(result != null, "moon_phase() should return parseable int")
    }
}
```

- [ ] **Step 2: Run tests — should FAIL** (functions not registered)

```
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.ash.GameRuntimeLibraryDateTimeTest"
```

- [ ] **Step 3: Add `expect` functions to commonMain `Platform.kt`**

Add to the end of `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/Platform.kt`:

```kotlin
package net.sourceforge.kolmafia.ash

expect fun currentTimeMillis(): Long      // existing
expect fun currentDateString(): String    // new: "YYYYMMDD" in local time
expect fun currentDateTimeString(): String // new: "YYYYMMDD HH:mm:ss" in local time
```

- [ ] **Step 4: Add `actual` implementations for JVM**

Full content of `shared/src/jvmMain/kotlin/net/sourceforge/kolmafia/ash/Platform.kt`:

```kotlin
package net.sourceforge.kolmafia.ash

import java.text.SimpleDateFormat
import java.util.Date

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
actual fun currentDateString(): String = SimpleDateFormat("yyyyMMdd").format(Date())
actual fun currentDateTimeString(): String = SimpleDateFormat("yyyyMMdd HH:mm:ss").format(Date())
```

- [ ] **Step 5: Add `actual` implementations for Android**

Full content of `shared/src/androidMain/kotlin/net/sourceforge/kolmafia/ash/Platform.kt`:

```kotlin
package net.sourceforge.kolmafia.ash

import java.text.SimpleDateFormat
import java.util.Date

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
actual fun currentDateString(): String = SimpleDateFormat("yyyyMMdd").format(Date())
actual fun currentDateTimeString(): String = SimpleDateFormat("yyyyMMdd HH:mm:ss").format(Date())
```

- [ ] **Step 6: Add `actual` implementations for iOS**

Full content of `shared/src/iosMain/kotlin/net/sourceforge/kolmafia/ash/Platform.kt`:

```kotlin
package net.sourceforge.kolmafia.ash

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter

actual fun currentTimeMillis(): Long =
    (NSDate.date().timeIntervalSince1970 * 1000).toLong()

actual fun currentDateString(): String {
    val fmt = NSDateFormatter()
    fmt.dateFormat = "yyyyMMdd"
    return fmt.stringFromDate(NSDate.date())
}

actual fun currentDateTimeString(): String {
    val fmt = NSDateFormatter()
    fmt.dateFormat = "yyyyMMdd HH:mm:ss"
    return fmt.stringFromDate(NSDate.date())
}
```

- [ ] **Step 7: Create `GameRuntimeLibrary.DateTime.kt`**

```kotlin
package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerDateTimeQueries(scope: AshScope) {

    // today_to_string() -> string  "YYYYMMDD"
    regFn(scope, "today_to_string", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(currentDateString())
    }

    // now_to_string() -> string  "YYYYMMDD HH:mm:ss"
    regFn(scope, "now_to_string", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(currentDateTimeString())
    }

    // gameday_to_string() -> string  (KoL game day == local calendar day for scripts)
    regFn(scope, "gameday_to_string", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(currentDateString())
    }

    // rollover() -> int  (seconds until next rollover; uses CharacterState.secondsUntilRollover)
    regFn(scope, "rollover", AshType.INT, emptyList()) { _, _ ->
        val secs = character?.state?.value?.secondsUntilRollover ?: 0L
        AshValue.of(secs.coerceAtLeast(0L))
    }

    // moon_phase() -> int  (real data from CharacterState; 0 until CharacterRequest refreshes)
    regFn(scope, "moon_phase", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((character?.state?.value?.moonPhase ?: 0).toLong())
    }
}
```

- [ ] **Step 8: Uncomment `registerDateTimeQueries(scope)` in `GameRuntimeLibrary.kt`**

- [ ] **Step 9: Run DateTime tests — should PASS**

```
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.ash.GameRuntimeLibraryDateTimeTest"
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 10: Run full suite and commit**

```
./gradlew shared:jvmTest
```

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/Platform.kt \
        shared/src/jvmMain/kotlin/net/sourceforge/kolmafia/ash/Platform.kt \
        shared/src/androidMain/kotlin/net/sourceforge/kolmafia/ash/Platform.kt \
        shared/src/iosMain/kotlin/net/sourceforge/kolmafia/ash/Platform.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.DateTime.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryDateTimeTest.kt
git commit -m "feat: ASH datetime extension — today_to_string, now_to_string, rollover, moon_phase"
```

---

### Task T8: Modifier extension — `GameRuntimeLibrary.Modifiers.kt`

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Modifiers.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryModifiersTest.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`

- [ ] **Step 1: Write failing tests**

Create `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryModifiersTest.kt`:

```kotlin
package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryModifiersTest {

    private val lib = GameRuntimeLibrary.forTesting()

    @Test
    fun numericModifier_item_unknownReturnsZero() {
        assertEquals("0.0",
            outputLib(lib, "print(to_string(numeric_modifier(\$item[seal tooth], \"Muscle\")));"))
    }

    @Test
    fun numericModifier_effect_unknownReturnsZero() {
        assertEquals("0.0",
            outputLib(lib, "print(to_string(numeric_modifier(\$effect[Saucestorm], \"Spell Damage\")));"))
    }

    @Test
    fun booleanModifier_item_unknownReturnsFalse() {
        assertEquals("false",
            outputLib(lib, "print(to_string(boolean_modifier(\$item[seal tooth], \"Softcore Only\")));"))
    }

    @Test
    fun stringModifier_item_unknownReturnsEmpty() {
        assertEquals("",
            outputLib(lib, "print(string_modifier(\$item[seal tooth], \"Class\"));"))
    }

    @Test
    fun stringModifier_effect_unknownReturnsEmpty() {
        assertEquals("",
            outputLib(lib, "print(string_modifier(\$effect[Saucestorm], \"Class\"));"))
    }
}
```

- [ ] **Step 2: Run tests — should FAIL**

```
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.ash.GameRuntimeLibraryModifiersTest"
```

- [ ] **Step 3: Create `GameRuntimeLibrary.Modifiers.kt`**

```kotlin
package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.BooleanModifier
import net.sourceforge.kolmafia.modifiers.StringModifier

internal fun GameRuntimeLibrary.registerModifierQueries(scope: AshScope) {

    // numeric_modifier(item, string) -> float
    regFn(scope, "numeric_modifier", AshType.FLOAT,
        listOf("it" to AshType.ITEM, "modifier" to AshType.STRING)) { _, args ->
        val mods = gameDatabase?.itemModifier(args[0].toString())
        val dm = DoubleModifier.byTag(args[1].toString())
        AshValue.of(dm?.let { mods?.get(it) } ?: 0.0)
    }

    // numeric_modifier(effect, string) -> float
    regFn(scope, "numeric_modifier", AshType.FLOAT,
        listOf("ef" to AshType.EFFECT, "modifier" to AshType.STRING)) { _, args ->
        val mods = gameDatabase?.effectModifier(args[0].toString())
        val dm = DoubleModifier.byTag(args[1].toString())
        AshValue.of(dm?.let { mods?.get(it) } ?: 0.0)
    }

    // boolean_modifier(item, string) -> boolean
    regFn(scope, "boolean_modifier", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM, "modifier" to AshType.STRING)) { _, args ->
        val mods = gameDatabase?.itemModifier(args[0].toString())
        val bm = BooleanModifier.byTag(args[1].toString())
        AshValue.of(bm?.let { mods?.get(it) } ?: false)
    }

    // boolean_modifier(effect, string) -> boolean
    regFn(scope, "boolean_modifier", AshType.BOOLEAN,
        listOf("ef" to AshType.EFFECT, "modifier" to AshType.STRING)) { _, args ->
        val mods = gameDatabase?.effectModifier(args[0].toString())
        val bm = BooleanModifier.byTag(args[1].toString())
        AshValue.of(bm?.let { mods?.get(it) } ?: false)
    }

    // string_modifier(item, string) -> string
    regFn(scope, "string_modifier", AshType.STRING,
        listOf("it" to AshType.ITEM, "modifier" to AshType.STRING)) { _, args ->
        val mods = gameDatabase?.itemModifier(args[0].toString())
        val sm = StringModifier.byTag(args[1].toString())
        AshValue.of(sm?.let { mods?.get(it) } ?: "")
    }

    // string_modifier(effect, string) -> string
    regFn(scope, "string_modifier", AshType.STRING,
        listOf("ef" to AshType.EFFECT, "modifier" to AshType.STRING)) { _, args ->
        val mods = gameDatabase?.effectModifier(args[0].toString())
        val sm = StringModifier.byTag(args[1].toString())
        AshValue.of(sm?.let { mods?.get(it) } ?: "")
    }
}
```

- [ ] **Step 4: Uncomment `registerModifierQueries(scope)` in `GameRuntimeLibrary.kt`**

- [ ] **Step 5: Run modifier tests — should PASS**

```
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.ash.GameRuntimeLibraryModifiersTest"
```

Expected: BUILD SUCCESSFUL. (All unknowns return 0.0/false/"" when `gameDatabase` is null.)

- [ ] **Step 6: Run full suite and commit**

```
./gradlew shared:jvmTest
```

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Modifiers.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryModifiersTest.kt
git commit -m "feat: ASH modifier extension — numeric_modifier, boolean_modifier, string_modifier (item+effect)"
```

---

### Task T9: Collections extension — `GameRuntimeLibrary.Collections.kt`

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Collections.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryCollectionsTest.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`

- [ ] **Step 1: Write failing tests**

Create `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryCollectionsTest.kt`:

```kotlin
package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryCollectionsTest {

    private fun libWithInventory(vararg items: InventoryItem): GameRuntimeLibrary {
        val im = InventoryManager(HttpClient(MockEngine { respond("") }), GameEventBus())
        im.testSetState(InventoryState(items = items.associateBy { it.itemId }))
        return GameRuntimeLibrary(inventoryManager = im)
    }

    @Test
    fun getInventory_emptyWhenNoInventoryManager() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("0",
            outputLib(lib, "print(to_string(count(get_inventory())));"))
    }

    @Test
    fun getInventory_countsItems() {
        val tooth = InventoryItem(2, "seal tooth", 3, ItemType.USABLE)
        val lib = libWithInventory(tooth)
        assertEquals("1",   // one distinct item
            outputLib(lib, "print(to_string(count(get_inventory())));"))
    }

    @Test
    fun getInventory_returnsQuantity() {
        val tooth = InventoryItem(2, "seal tooth", 3, ItemType.USABLE)
        val lib = libWithInventory(tooth)
        assertEquals("3",
            outputLib(lib, "print(to_string(get_inventory()[\$item[seal tooth]]));"))
    }

    @Test
    fun getCloset_returnsEmptyAggregate() {
        assertEquals("0",
            outputLib(GameRuntimeLibrary.forTesting(),
                "print(to_string(count(get_closet())));"))
    }

    @Test
    fun getStorage_returnsEmptyAggregate() {
        assertEquals("0",
            outputLib(GameRuntimeLibrary.forTesting(),
                "print(to_string(count(get_storage())));"))
    }
}
```

Note: `InventoryManager.testSetState()` may not exist yet — add it if absent, following the same pattern as `FamiliarManager.testSetState()`. Add `internal fun testSetState(state: InventoryState) { _state.value = state }` to `InventoryManager`.

- [ ] **Step 2: Add `testSetState` to `InventoryManager` if missing**

In `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/inventory/InventoryManager.kt`, add:

```kotlin
    internal fun testSetState(state: InventoryState) { _state.value = state }
```

- [ ] **Step 3: Run tests — should FAIL** (functions not registered)

```
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.ash.GameRuntimeLibraryCollectionsTest"
```

- [ ] **Step 4: Create `GameRuntimeLibrary.Collections.kt`**

```kotlin
package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerCollectionQueries(scope: AshScope) {

    val itemIntType = AggregateType(AshType.ITEM, AshType.INT)

    // get_inventory() -> int[item]  (live from InventoryManager)
    regFn(scope, "get_inventory", itemIntType, emptyList()) { _, _ ->
        val result = AggregateValue(itemIntType)
        inventoryManager?.state?.value?.items?.values?.forEach { item ->
            result[AshValue.item(item.name)] = AshValue.of(item.quantity.toLong())
        }
        result
    }

    // get_closet() -> int[item]  (stub: no closet fetch yet)
    regFn(scope, "get_closet", itemIntType, emptyList()) { _, _ ->
        AggregateValue(itemIntType)
    }

    // get_storage() -> int[item]  (stub: no storage fetch yet)
    regFn(scope, "get_storage", itemIntType, emptyList()) { _, _ ->
        AggregateValue(itemIntType)
    }

    // get_stash() -> int[item]  (stub)
    regFn(scope, "get_stash", itemIntType, emptyList()) { _, _ ->
        AggregateValue(itemIntType)
    }

    // get_display() -> int[item]  (stub)
    regFn(scope, "get_display", itemIntType, emptyList()) { _, _ ->
        AggregateValue(itemIntType)
    }
}
```

- [ ] **Step 5: Uncomment `registerCollectionQueries(scope)` in `GameRuntimeLibrary.kt`**

- [ ] **Step 6: Run collection tests — should PASS**

```
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.ash.GameRuntimeLibraryCollectionsTest"
```

- [ ] **Step 7: Run full suite and commit**

```
./gradlew shared:jvmTest
```

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Collections.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/inventory/InventoryManager.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryCollectionsTest.kt
git commit -m "feat: ASH collections extension — get_inventory (live), get_closet/storage/stash/display (stubs)"
```

---

### Task T10: Goals, Mood, Prefs, Combat extensions

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Goals.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Mood.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Prefs.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Combat.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryGoalsTest.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryMoodTest.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryPrefsTest.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryCombatTest.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`

- [ ] **Step 1: Create `GameRuntimeLibrary.Goals.kt`**

```kotlin
package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerGoalQueries(scope: AshScope) {

    val intStringType = AggregateType(AshType.INT, AshType.STRING)

    // add_item_condition(int qty, item it) -> void
    // qty is ignored — GoalManager tracks presence, not quantity
    regFn(scope, "add_item_condition", AshType.VOID,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        goalManager?.addItemGoalByName(args[1].toString())
        AshValue.VOID
    }

    // remove_item_condition(int qty, item it) -> void
    regFn(scope, "remove_item_condition", AshType.VOID,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        goalManager?.removeGoal(args[1].toString())
        AshValue.VOID
    }

    // goal_exists(string type) -> boolean
    // type: "item", "meat", "level"
    regFn(scope, "goal_exists", AshType.BOOLEAN,
        listOf("type" to AshType.STRING)) { _, args ->
        val gm = goalManager ?: return@regFn AshValue.of(false)
        val exists = when (args[0].toString().lowercase()) {
            "item"  -> gm.hasItemGoals()
            "meat"  -> gm.hasMeatGoalSet()
            "level" -> gm.hasLevelGoalSet()
            else    -> false
        }
        AshValue.of(exists)
    }

    // get_goals() -> string[int]
    regFn(scope, "get_goals", intStringType, emptyList()) { _, _ ->
        val result = AggregateValue(intStringType)
        goalManager?.allGoalsAsStrings()?.forEachIndexed { i, s ->
            result[AshValue.of(i)] = AshValue.of(s)
        }
        result
    }
}
```

- [ ] **Step 2: Create `GameRuntimeLibrary.Mood.kt`**

```kotlin
package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerMoodQueries(scope: AshScope) {

    val intStringType = AggregateType(AshType.INT, AshType.STRING)

    // get_moods() -> string[int]
    regFn(scope, "get_moods", intStringType, emptyList()) { _, _ ->
        val result = AggregateValue(intStringType)
        moodManager?.moodLibrary?.keys?.forEachIndexed { i, name ->
            result[AshValue.of(i)] = AshValue.of(name)
        }
        result
    }

    // mood_list() -> string[int]  (alias for get_moods)
    regFn(scope, "mood_list", intStringType, emptyList()) { _, _ ->
        val result = AggregateValue(intStringType)
        moodManager?.moodLibrary?.keys?.forEachIndexed { i, name ->
            result[AshValue.of(i)] = AshValue.of(name)
        }
        result
    }
}
```

- [ ] **Step 3: Create `GameRuntimeLibrary.Prefs.kt`**

```kotlin
package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerPreferenceAccess(scope: AshScope) {

    // get_property(string key) -> string
    regFn(scope, "get_property", AshType.STRING,
        listOf("key" to AshType.STRING)) { _, args ->
        AshValue.of(preferences?.getString(args[0].toString(), "") ?: "")
    }

    // set_property(string key, string value) -> void
    regFn(scope, "set_property", AshType.VOID,
        listOf("key" to AshType.STRING, "value" to AshType.STRING)) { _, args ->
        preferences?.setString(args[0].toString(), args[1].toString())
        AshValue.VOID
    }
}
```

- [ ] **Step 4: Create `GameRuntimeLibrary.Combat.kt`**

```kotlin
package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerCombatStubs(scope: AshScope) {

    // in_multi_fight() -> boolean  (stub)
    regFn(scope, "in_multi_fight", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(false)
    }

    // fight_follows_choice() -> boolean  (stub)
    regFn(scope, "fight_follows_choice", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(false)
    }

    // last_monster() -> monster  (reads _lastMonster pref written by AdventureManager)
    regFn(scope, "last_monster", AshType.MONSTER, emptyList()) { _, _ ->
        val name = preferences?.getString(
            net.sourceforge.kolmafia.preferences.Preferences.LAST_MONSTER, "") ?: ""
        AshValue(AshType.MONSTER, name)
    }

    // copiers_used(skill) -> int  (stub)
    regFn(scope, "copiers_used", AshType.INT,
        listOf("sk" to AshType.SKILL)) { _, _ ->
        AshValue.of(0L)
    }
}
```

- [ ] **Step 5: Write tests for all four domains**

Create `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryGoalsTest.kt`:

```kotlin
package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.session.GoalManager
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryGoalsTest {

    private fun libWithGoals(): GameRuntimeLibrary {
        val gm = GoalManager()
        return GameRuntimeLibrary(goalManager = gm)
    }

    @Test
    fun addItemCondition_thenGoalExistsItem() {
        val gm = GoalManager()
        val lib = GameRuntimeLibrary(goalManager = gm)
        outputLib(lib, "add_item_condition(1, \$item[seal tooth]);")
        assertEquals("true", outputLib(lib, "print(to_string(goal_exists(\"item\")));"))
    }

    @Test
    fun removeItemCondition_thenGoalExistsFalse() {
        val gm = GoalManager()
        val lib = GameRuntimeLibrary(goalManager = gm)
        outputLib(lib, "add_item_condition(1, \$item[seal tooth]);")
        outputLib(lib, "remove_item_condition(1, \$item[seal tooth]);")
        assertEquals("false", outputLib(lib, "print(to_string(goal_exists(\"item\")));"))
    }

    @Test
    fun getGoals_countMatchesAdded() {
        val gm = GoalManager()
        gm.addItemGoalByName("seal tooth")
        gm.addItemGoalByName("spooky sprocket")
        val lib = GameRuntimeLibrary(goalManager = gm)
        assertEquals("2", outputLib(lib, "print(to_string(count(get_goals())));"))
    }
}
```

Create `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryMoodTest.kt`:

```kotlin
package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.mood.MoodManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.request.UneffectRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryMoodTest {

    private fun makeMoodManager(vararg moodNames: String): MoodManager {
        val prefs = Preferences(MapSettings())
        val client = HttpClient(MockEngine { respond("") })
        val bus = GameEventBus()
        val sm = SkillManager(client, bus)
        val ur = UneffectRequest(client)
        val mm = MoodManager(skillManager = sm, preferences = prefs, uneffectRequest = ur)
        moodNames.forEach { name ->
            mm.addMood(net.sourceforge.kolmafia.mood.Mood(name, emptyList()))
        }
        return mm
    }

    @Test
    fun getMoods_returnsMoodNames() {
        val mm = makeMoodManager("buff", "heal")
        val lib = GameRuntimeLibrary(moodManager = mm)
        assertEquals("2", outputLib(lib, "print(to_string(count(get_moods())));"))
    }

    @Test
    fun getMoods_emptyWhenNoLibrary() {
        val mm = makeMoodManager()
        val lib = GameRuntimeLibrary(moodManager = mm)
        assertEquals("0", outputLib(lib, "print(to_string(count(get_moods())));"))
    }
}
```

Create `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryPrefsTest.kt`:

```kotlin
package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryPrefsTest {

    @Test
    fun getProperty_unknownKeyReturnsEmpty() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("", outputLib(lib, "print(get_property(\"noSuchKey\"));"))
    }

    @Test
    fun setAndGetProperty_roundTrips() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        outputLib(lib, "set_property(\"myKey\", \"myValue\");")
        assertEquals("myValue", outputLib(lib, "print(get_property(\"myKey\"));"))
    }
}
```

Create `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryCombatTest.kt`:

```kotlin
package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryCombatTest {

    @Test
    fun inMultiFight_alwaysFalse() {
        assertEquals("false",
            outputLib(GameRuntimeLibrary.forTesting(), "print(to_string(in_multi_fight()));"))
    }

    @Test
    fun fightFollowsChoice_alwaysFalse() {
        assertEquals("false",
            outputLib(GameRuntimeLibrary.forTesting(), "print(to_string(fight_follows_choice()));"))
    }

    @Test
    fun lastMonster_readsFromPref() {
        val p = prefs()
        p.setString(Preferences.LAST_MONSTER, "bunny")
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("bunny", outputLib(lib, "print(last_monster());"))
    }

    @Test
    fun copiersUsed_returnsZero() {
        assertEquals("0",
            outputLib(GameRuntimeLibrary.forTesting(),
                "print(to_string(copiers_used(\$skill[Accordion Thief])));"))
    }
}
```

- [ ] **Step 6: Run tests — should FAIL** (functions not registered)

```
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.ash.GameRuntimeLibraryGoalsTest"
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.ash.GameRuntimeLibraryPrefsTest"
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.ash.GameRuntimeLibraryCombatTest"
```

- [ ] **Step 7: Uncomment all four register calls in `GameRuntimeLibrary.kt`**

```kotlin
        registerGoalQueries(scope)
        registerMoodQueries(scope)
        registerPreferenceAccess(scope)
        registerCombatStubs(scope)
```

- [ ] **Step 8: Run all four test classes — should PASS**

```
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.ash.GameRuntimeLibraryGoalsTest"
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.ash.GameRuntimeLibraryMoodTest"
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.ash.GameRuntimeLibraryPrefsTest"
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.ash.GameRuntimeLibraryCombatTest"
```

- [ ] **Step 9: Run full suite and commit**

```
./gradlew shared:jvmTest
```

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Goals.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Mood.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Prefs.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Combat.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryGoalsTest.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryMoodTest.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryPrefsTest.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryCombatTest.kt
git commit -m "feat: ASH goal/mood/prefs/combat extensions — add_item_condition, goal_exists, get_goals, get_moods, get/set_property, last_monster, combat stubs"
```

---


### Task T11: HTTP request classes — use/eat/drink/chew

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/UseItemRequest.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/EatFoodRequest.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/DrinkBoozeRequest.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/ChewRequest.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/UseItemRequestTest.kt`

- [ ] **Step 1: Write failing tests for `UseItemRequest`**

Create `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/UseItemRequestTest.kt`:

```kotlin
package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.HttpHeaders
import kotlin.test.Test
import kotlin.test.assertTrue

class UseItemRequestTest {

    @Test
    fun useItem_sendsCorrectItemId() {
        val capturedPaths = mutableListOf<String>()
        val engine = MockEngine { request ->
            capturedPaths += request.url.fullPath
            respond("<html>You use the item.</html>", HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "text/html"))
        }
        val client = HttpClient(engine)
        val req = UseItemRequest(client)
        kotlinx.coroutines.runBlocking { req.use(itemId = 2, quantity = 1) }
        assertTrue(capturedPaths.any { it.contains("itemId=2") || it.contains("whichitem=2") },
            "Expected item ID in request path but got: $capturedPaths")
    }

    @Test
    fun useItem_returnsSuccessOnOkResponse() {
        val engine = MockEngine { respond("<html>You use the item.</html>", HttpStatusCode.OK,
            headersOf(HttpHeaders.ContentType, "text/html")) }
        val client = HttpClient(engine)
        val req = UseItemRequest(client)
        val result = kotlinx.coroutines.runBlocking { req.use(itemId = 2, quantity = 1) }
        assertTrue(result.isSuccess)
    }

    @Test
    fun useItem_returnsFailureOnNetworkError() {
        val engine = MockEngine { throw Exception("Network error") }
        val client = HttpClient(engine)
        val req = UseItemRequest(client)
        val result = kotlinx.coroutines.runBlocking { req.use(itemId = 2, quantity = 1) }
        assertTrue(result.isFailure)
    }
}
```

- [ ] **Step 2: Run tests — should FAIL** (`UseItemRequest` not found)

```
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.request.UseItemRequestTest"
```

- [ ] **Step 3: Create `UseItemRequest.kt`**

```kotlin
package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

class UseItemRequest(private val client: HttpClient) {
    /**
     * Uses an item via inv_use.php.
     * @param itemId  KoL item ID
     * @param quantity  number to use (default 1)
     * @return Success with response HTML, or Failure
     */
    suspend fun use(itemId: Int, quantity: Int = 1): Result<String> = runCatching {
        client.get("https://www.kingdomofloathing.com/inv_use.php") {
            parameter("which", 3)
            parameter("whichitem", itemId)
            parameter("ajax", 1)
            if (quantity > 1) parameter("quantity", quantity)
        }.bodyAsText()
    }
}
```

- [ ] **Step 4: Create `EatFoodRequest.kt`**

```kotlin
package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

class EatFoodRequest(private val client: HttpClient) {
    /**
     * Eats food via inv_eat.php.
     * @param itemId  KoL item ID of food
     * @param quantity  number to eat (default 1)
     * @return Success with response HTML, or Failure
     */
    suspend fun eat(itemId: Int, quantity: Int = 1): Result<String> = runCatching {
        client.get("https://www.kingdomofloathing.com/inv_eat.php") {
            parameter("which", 1)
            parameter("whichitem", itemId)
            parameter("ajax", 1)
            if (quantity > 1) parameter("quantity", quantity)
        }.bodyAsText()
    }
}
```

- [ ] **Step 5: Create `DrinkBoozeRequest.kt`**

```kotlin
package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

class DrinkBoozeRequest(private val client: HttpClient) {
    /**
     * Drinks booze via inv_booze.php.
     * @param itemId  KoL item ID of booze
     * @param quantity  number to drink (default 1)
     * @return Success with response HTML, or Failure
     */
    suspend fun drink(itemId: Int, quantity: Int = 1): Result<String> = runCatching {
        client.get("https://www.kingdomofloathing.com/inv_booze.php") {
            parameter("which", 1)
            parameter("whichitem", itemId)
            parameter("ajax", 1)
            if (quantity > 1) parameter("quantity", quantity)
        }.bodyAsText()
    }
}
```

- [ ] **Step 6: Create `ChewRequest.kt`**

```kotlin
package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

class ChewRequest(private val client: HttpClient) {
    /**
     * Chews a spleen item via inv_spleen.php.
     * @param itemId  KoL item ID of spleen item
     * @param quantity  number to chew (default 1)
     * @return Success with response HTML, or Failure
     */
    suspend fun chew(itemId: Int, quantity: Int = 1): Result<String> = runCatching {
        client.get("https://www.kingdomofloathing.com/inv_spleen.php") {
            parameter("which", 1)
            parameter("whichitem", itemId)
            parameter("ajax", 1)
            if (quantity > 1) parameter("quantity", quantity)
        }.bodyAsText()
    }
}
```

- [ ] **Step 7: Run UseItemRequest tests — should PASS**

```
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.request.UseItemRequestTest"
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Run full suite and commit**

```
./gradlew shared:jvmTest
```

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/UseItemRequest.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/EatFoodRequest.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/DrinkBoozeRequest.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/ChewRequest.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/UseItemRequestTest.kt
git commit -m "feat: HTTP request classes — UseItemRequest, EatFoodRequest, DrinkBoozeRequest, ChewRequest"
```

---

### Task T12: HTTP request classes — autosell/closet/storage

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/AutosellRequest.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/ClosetRequest.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/StorageRequest.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/AutosellRequestTest.kt`

- [ ] **Step 1: Write failing tests for `AutosellRequest`**

Create `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/AutosellRequestTest.kt`:

```kotlin
package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.HttpHeaders
import kotlin.test.Test
import kotlin.test.assertTrue

class AutosellRequestTest {

    @Test
    fun autosell_sendsItemIdAndQuantity() {
        val capturedPaths = mutableListOf<String>()
        val engine = MockEngine { request ->
            capturedPaths += request.url.fullPath
            respond("<html>You sell the item.</html>", HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "text/html"))
        }
        val client = HttpClient(engine)
        val req = AutosellRequest(client)
        kotlinx.coroutines.runBlocking { req.autosell(itemId = 2, quantity = 5) }
        assertTrue(capturedPaths.any { it.contains("whichitem=2") },
            "Expected item ID in request but got: $capturedPaths")
    }

    @Test
    fun autosell_returnsSuccessOnOkResponse() {
        val engine = MockEngine { respond("<html>Sold!</html>", HttpStatusCode.OK,
            headersOf(HttpHeaders.ContentType, "text/html")) }
        val result = kotlinx.coroutines.runBlocking {
            AutosellRequest(HttpClient(engine)).autosell(itemId = 2, quantity = 1)
        }
        assertTrue(result.isSuccess)
    }

    @Test
    fun autosell_returnsFailureOnNetworkError() {
        val engine = MockEngine { throw Exception("Network failure") }
        val result = kotlinx.coroutines.runBlocking {
            AutosellRequest(HttpClient(engine)).autosell(itemId = 2, quantity = 1)
        }
        assertTrue(result.isFailure)
    }
}
```

- [ ] **Step 2: Run tests — should FAIL** (`AutosellRequest` not found)

```
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.request.AutosellRequestTest"
```

- [ ] **Step 3: Create `AutosellRequest.kt`**

```kotlin
package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

class AutosellRequest(private val client: HttpClient) {
    /**
     * Autosells items via sellstuff_ugly.php.
     * @param itemId   KoL item ID
     * @param quantity number to sell
     * @return Success with response HTML, or Failure
     */
    suspend fun autosell(itemId: Int, quantity: Int): Result<String> = runCatching {
        client.get("https://www.kingdomofloathing.com/sellstuff_ugly.php") {
            parameter("action", "sell")
            parameter("whichitem", itemId)
            parameter("quantity", quantity)
            parameter("ajax", 1)
        }.bodyAsText()
    }
}
```

- [ ] **Step 4: Create `ClosetRequest.kt`**

```kotlin
package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

class ClosetRequest(private val client: HttpClient) {

    /**
     * Puts items into the closet via closet.php.
     * @param itemId   KoL item ID
     * @param quantity number to put in
     * @return Success with response HTML, or Failure
     */
    suspend fun putIn(itemId: Int, quantity: Int): Result<String> = runCatching {
        client.get("https://www.kingdomofloathing.com/closet.php") {
            parameter("action", "put")
            parameter("whichitem", itemId)
            parameter("qty", quantity)
            parameter("ajax", 1)
        }.bodyAsText()
    }

    /**
     * Takes items from the closet via closet.php.
     * @param itemId   KoL item ID
     * @param quantity number to take out
     * @return Success with response HTML, or Failure
     */
    suspend fun takeOut(itemId: Int, quantity: Int): Result<String> = runCatching {
        client.get("https://www.kingdomofloathing.com/closet.php") {
            parameter("action", "take")
            parameter("whichitem", itemId)
            parameter("qty", quantity)
            parameter("ajax", 1)
        }.bodyAsText()
    }
}
```

- [ ] **Step 5: Create `StorageRequest.kt`**

```kotlin
package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

class StorageRequest(private val client: HttpClient) {

    /**
     * Withdraws items from Hagnk's storage via storage.php.
     * @param itemId   KoL item ID
     * @param quantity number to withdraw
     * @return Success with response HTML, or Failure
     */
    suspend fun withdraw(itemId: Int, quantity: Int): Result<String> = runCatching {
        client.get("https://www.kingdomofloathing.com/storage.php") {
            parameter("action", "take")
            parameter("whichitem", itemId)
            parameter("qty", quantity)
            parameter("ajax", 1)
        }.bodyAsText()
    }
}
```

- [ ] **Step 6: Run AutosellRequest tests — should PASS**

```
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.request.AutosellRequestTest"
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Run full suite and commit**

```
./gradlew shared:jvmTest
```

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/AutosellRequest.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/ClosetRequest.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/StorageRequest.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/AutosellRequestTest.kt
git commit -m "feat: HTTP request classes — AutosellRequest, ClosetRequest, StorageRequest"
```

---

### Task T13: Item-action extension — `GameRuntimeLibrary.ItemActions.kt`

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.ItemActions.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryItemActionsTest.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`

- [ ] **Step 1: Write failing tests**

Create `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryItemActionsTest.kt`:

```kotlin
package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.HttpHeaders
import net.sourceforge.kolmafia.request.UseItemRequest
import net.sourceforge.kolmafia.request.EatFoodRequest
import net.sourceforge.kolmafia.request.DrinkBoozeRequest
import net.sourceforge.kolmafia.request.ChewRequest
import net.sourceforge.kolmafia.request.AutosellRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.StorageRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryItemActionsTest {

    private fun successClient() = HttpClient(MockEngine {
        respond("<html>success</html>", HttpStatusCode.OK,
            headersOf(HttpHeaders.ContentType, "text/html"))
    })

    private fun failingClient() = HttpClient(MockEngine { throw Exception("Network error") })

    private fun libWithSuccess() = GameRuntimeLibrary(
        useItemRequest     = UseItemRequest(successClient()),
        eatFoodRequest     = EatFoodRequest(successClient()),
        drinkBoozeRequest  = DrinkBoozeRequest(successClient()),
        chewRequest        = ChewRequest(successClient()),
        autosellRequest    = AutosellRequest(successClient()),
        closetRequest      = ClosetRequest(successClient()),
        storageRequest     = StorageRequest(successClient()),
    )

    private fun libWithFailure() = GameRuntimeLibrary(
        useItemRequest     = UseItemRequest(failingClient()),
        eatFoodRequest     = EatFoodRequest(failingClient()),
        drinkBoozeRequest  = DrinkBoozeRequest(failingClient()),
        chewRequest        = ChewRequest(failingClient()),
        autosellRequest    = AutosellRequest(failingClient()),
        closetRequest      = ClosetRequest(failingClient()),
        storageRequest     = StorageRequest(failingClient()),
    )

    @Test
    fun useItem_returnsOneOnSuccess() {
        assertEquals("1",
            outputLib(libWithSuccess(), "print(to_string(use(1, \$item[seal tooth])));"))
    }

    @Test
    fun useItem_returnsZeroOnFailure() {
        assertEquals("0",
            outputLib(libWithFailure(), "print(to_string(use(1, \$item[seal tooth])));"))
    }

    @Test
    fun eatItem_returnsOneOnSuccess() {
        assertEquals("1",
            outputLib(libWithSuccess(), "print(to_string(eat(1, \$item[seal tooth])));"))
    }

    @Test
    fun drinkItem_returnsOneOnSuccess() {
        assertEquals("1",
            outputLib(libWithSuccess(), "print(to_string(drink(1, \$item[seal tooth])));"))
    }

    @Test
    fun chewItem_returnsOneOnSuccess() {
        assertEquals("1",
            outputLib(libWithSuccess(), "print(to_string(chew(1, \$item[seal tooth])));"))
    }

    @Test
    fun autosellItem_returnsOneOnSuccess() {
        assertEquals("1",
            outputLib(libWithSuccess(), "print(to_string(autosell(1, \$item[seal tooth])));"))
    }

    @Test
    fun putCloset_returnsOneOnSuccess() {
        assertEquals("1",
            outputLib(libWithSuccess(), "print(to_string(put_closet(1, \$item[seal tooth])));"))
    }

    @Test
    fun takeCloset_returnsOneOnSuccess() {
        assertEquals("1",
            outputLib(libWithSuccess(), "print(to_string(take_closet(1, \$item[seal tooth])));"))
    }

    @Test
    fun takeStorage_returnsOneOnSuccess() {
        assertEquals("1",
            outputLib(libWithSuccess(), "print(to_string(take_storage(1, \$item[seal tooth])));"))
    }

    @Test
    fun useItem_returnsZeroWhenNoRequest() {
        // no request classes wired → graceful degradation
        assertEquals("0",
            outputLib(GameRuntimeLibrary.forTesting(), "print(to_string(use(1, \$item[seal tooth])));"))
    }
}
```

- [ ] **Step 2: Run tests — should FAIL** (functions not registered)

```
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.ash.GameRuntimeLibraryItemActionsTest"
```

- [ ] **Step 3: Create `GameRuntimeLibrary.ItemActions.kt`**

```kotlin
package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.GameDatabase

internal fun GameRuntimeLibrary.registerItemActions(scope: AshScope) {

    /**
     * Resolves an item name to its KoL numeric ID.
     * Returns null if the item cannot be found in the database.
     */
    fun resolveItemId(itemName: String): Int? =
        gameDatabase?.item(itemName)?.id

    // use(int qty, item it) -> boolean
    regFn(scope, "use", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val qty = args[0].toLong().toInt()
        val id = resolveItemId(args[1].toString())
        val req = useItemRequest
        if (req == null || id == null) return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.use(id, qty) }.isSuccess)
    }

    // eat(int qty, item it) -> boolean
    regFn(scope, "eat", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val qty = args[0].toLong().toInt()
        val id = resolveItemId(args[1].toString())
        val req = eatFoodRequest
        if (req == null || id == null) return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.eat(id, qty) }.isSuccess)
    }

    // drink(int qty, item it) -> boolean
    regFn(scope, "drink", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val qty = args[0].toLong().toInt()
        val id = resolveItemId(args[1].toString())
        val req = drinkBoozeRequest
        if (req == null || id == null) return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.drink(id, qty) }.isSuccess)
    }

    // chew(int qty, item it) -> boolean
    regFn(scope, "chew", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val qty = args[0].toLong().toInt()
        val id = resolveItemId(args[1].toString())
        val req = chewRequest
        if (req == null || id == null) return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.chew(id, qty) }.isSuccess)
    }

    // autosell(int qty, item it) -> boolean
    regFn(scope, "autosell", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val qty = args[0].toLong().toInt()
        val id = resolveItemId(args[1].toString())
        val req = autosellRequest
        if (req == null || id == null) return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.autosell(id, qty) }.isSuccess)
    }

    // put_closet(int qty, item it) -> boolean
    regFn(scope, "put_closet", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val qty = args[0].toLong().toInt()
        val id = resolveItemId(args[1].toString())
        val req = closetRequest
        if (req == null || id == null) return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.putIn(id, qty) }.isSuccess)
    }

    // take_closet(int qty, item it) -> boolean
    regFn(scope, "take_closet", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val qty = args[0].toLong().toInt()
        val id = resolveItemId(args[1].toString())
        val req = closetRequest
        if (req == null || id == null) return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.takeOut(id, qty) }.isSuccess)
    }

    // put_shop(int qty, int price, item it) -> boolean  (stub: mall shop not implemented)
    regFn(scope, "put_shop", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "price" to AshType.INT, "it" to AshType.ITEM)) { _, _ ->
        AshValue.of(false)
    }

    // take_storage(int qty, item it) -> boolean
    regFn(scope, "take_storage", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val qty = args[0].toLong().toInt()
        val id = resolveItemId(args[1].toString())
        val req = storageRequest
        if (req == null || id == null) return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.withdraw(id, qty) }.isSuccess)
    }
}
```

- [ ] **Step 4: Uncomment `registerItemActions(scope)` in `GameRuntimeLibrary.kt`**

- [ ] **Step 5: Run item-action tests — should PASS**

```
./gradlew shared:jvmTest --tests "net.sourceforge.kolmafia.ash.GameRuntimeLibraryItemActionsTest"
```

Expected: BUILD SUCCESSFUL. (Note: `resolveItemId` returns null for unknown items when `gameDatabase == null`, so failure tests also pass via graceful degradation.)

- [ ] **Step 6: Run full suite and commit**

```
./gradlew shared:jvmTest
```

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.ItemActions.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryItemActionsTest.kt
git commit -m "feat: ASH item-action extension — use, eat, drink, chew, autosell, put_closet, take_closet, take_storage"
```

---

### Task T14: DI wiring — `SharedModule.kt`

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt`

- [ ] **Step 1: No test needed** — this is pure wiring; the full integration tests in T4–T13 already exercise the wired paths. Verify by inspecting `SharedModule.kt` after the edit.

- [ ] **Step 2: Open `SharedModule.kt` and locate the `GameRuntimeLibrary` binding**

The file uses Koin. The existing `GameRuntimeLibrary` binding looks like:

```kotlin
singleOf(::GameRuntimeLibrary) {
    // existing params ...
}
```

Or it may be a `single { GameRuntimeLibrary(...) }` block. Find whichever form is present.

- [ ] **Step 3: Add 7 new `singleOf` declarations for the request classes**

Add these declarations in the appropriate section (near other `Request` singleOf calls):

```kotlin
singleOf(::UseItemRequest)
singleOf(::EatFoodRequest)
singleOf(::DrinkBoozeRequest)
singleOf(::ChewRequest)
singleOf(::AutosellRequest)
singleOf(::ClosetRequest)
singleOf(::StorageRequest)
```

- [ ] **Step 4: Expand the `GameRuntimeLibrary` binding with 12 new constructor params**

Replace the existing `GameRuntimeLibrary` binding with one that includes all new params. The full binding (old + new) must cover these parameters, injected by name/type:

```kotlin
single {
    GameRuntimeLibrary(
        // --- existing params ---
        character         = get(),
        inventoryManager  = get(),
        skillManager      = get(),
        effectManager     = get(),
        adventureManager  = get(),
        banishManager     = get(),
        // --- new params (Phase 10) ---
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
    )
}
```

Note: if any of `familiarManager`, `goalManager`, `moodManager`, `gameDatabase` are not currently bound in `SharedModule.kt`, add their `singleOf` declarations too — check the file before editing.

- [ ] **Step 5: Verify the app builds**

```
./gradlew shared:compileKotlinJvm
```

Expected: BUILD SUCCESSFUL. If there are unresolved Koin bindings, add the missing `singleOf(::ClassName)` declarations.

- [ ] **Step 6: Run full suite**

```
./gradlew shared:jvmTest
```

Expected: BUILD SUCCESSFUL, all prior tests still pass.

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt
git commit -m "feat: wire Phase 10 request classes + new GameRuntimeLibrary params in SharedModule"
```

---


---

## Self-Review

### 1. Spec Coverage Check

| Spec Section | Covered by | Status |
|---|---|---|
| `regFn` wrapper + visibility | T1 | ✅ |
| `GameRuntimeLibraryTestHelpers.kt` | T1 | ✅ |
| `Preferences.LAST_MONSTER` | T2 | ✅ |
| `AdventureManager` writes pref | T2 | ✅ |
| `GoalManager` predicates | T3 | ✅ |
| `FamiliarManager.setFamiliar()` | T3 | ✅ |
| `GameRuntimeLibrary.Character.kt` | T4 | ✅ |
| `GameRuntimeLibrary.Familiar.kt` | T5 | ✅ |
| `GameRuntimeLibrary.Equipment.kt` | T6 | ✅ |
| Platform date expect/actual | T7 | ✅ |
| `GameRuntimeLibrary.DateTime.kt` | T7 | ✅ |
| `GameRuntimeLibrary.Modifiers.kt` | T8 | ✅ |
| `GameRuntimeLibrary.Collections.kt` | T9 | ✅ |
| `GameRuntimeLibrary.Goals.kt` | T10 | ✅ |
| `GameRuntimeLibrary.Mood.kt` | T10 | ✅ |
| `GameRuntimeLibrary.Prefs.kt` | T10 | ✅ |
| `GameRuntimeLibrary.Combat.kt` | T10 | ✅ |
| `UseItemRequest` | T11 | ✅ |
| `EatFoodRequest` | T11 | ✅ |
| `DrinkBoozeRequest` | T11 | ✅ |
| `ChewRequest` | T11 | ✅ |
| `AutosellRequest` | T12 | ✅ |
| `ClosetRequest` | T12 | ✅ |
| `StorageRequest` | T12 | ✅ |
| `GameRuntimeLibrary.ItemActions.kt` | T13 | ✅ |
| `SharedModule.kt` DI wiring | T14 | ✅ |

**Gaps:** None. All 11 extension files, all 7 HTTP request classes, all infrastructure changes covered.

### 2. Placeholder Scan

Searched plan for: "TBD", "TODO", "implement later", "fill in details", "similar to Task", "add appropriate", "handle edge cases", "write tests for the above"

- `put_shop` is stubbed in T13 — this is intentional (mall shop not in scope per spec), not a placeholder
- `mood_list` in T10 is marked as alias — intentional duplication, not deferred work
- `get_stash` / `get_display` are stubs in T9 — intentional per spec ("stub")
- `under_standard` is stub in T4 — intentional ("CharacterState has no underStandard field")
- `in_multi_fight` / `fight_follows_choice` are stubs in T10 — intentional

**Result:** No unintentional placeholders.

### 3. Type Consistency Check

| Name | Defined in | Used in | Match? |
|---|---|---|---|
| `regFn(scope, name, returnType, params, impl)` | T1 | T4–T13 | ✅ |
| `AshValue.of(Boolean/Long/Double/String)` | T1 (referenced) | T4–T13 | ✅ |
| `AshValue.item(name)` | T1 (referenced) | T4, T6, T9, T13 | ✅ |
| `AshValue.familiar(name)` | T1 (referenced) | T5 | ✅ |
| `AshValue.VOID` | T1 (referenced) | T10 | ✅ |
| `AshValue.EMPTY_STRING` | T1 (referenced) | T4 | ✅ |
| `AggregateValue(AggregateType(key, data))` | T9 | T9, T10 | ✅ |
| `FamiliarManager.setFamiliar(name)` | T3 | T5 | ✅ |
| `FamiliarManager.testSetState(state)` | T3 | T5 test | ✅ |
| `InventoryManager.testSetState(state)` | T9 | T9 test | ✅ |
| `GoalManager.hasItemGoals()` | T3 | T10 | ✅ |
| `GoalManager.hasMeatGoalSet()` | T3 | T10 | ✅ |
| `GoalManager.hasLevelGoalSet()` | T3 | T10 | ✅ |
| `GoalManager.removeGoal(name)` | T3 | T10 | ✅ |
| `GoalManager.allGoalsAsStrings()` | T3 | T10 | ✅ |
| `UseItemRequest.use(itemId, quantity)` | T11 | T13 | ✅ |
| `EatFoodRequest.eat(itemId, quantity)` | T11 | T13 | ✅ |
| `DrinkBoozeRequest.drink(itemId, quantity)` | T11 | T13 | ✅ |
| `ChewRequest.chew(itemId, quantity)` | T11 | T13 | ✅ |
| `AutosellRequest.autosell(itemId, quantity)` | T12 | T13 | ✅ |
| `ClosetRequest.putIn(itemId, quantity)` | T12 | T13 | ✅ |
| `ClosetRequest.takeOut(itemId, quantity)` | T12 | T13 | ✅ |
| `StorageRequest.withdraw(itemId, quantity)` | T12 | T13 | ✅ |
| `currentDateString()` | T7 | T7 | ✅ |
| `currentDateTimeString()` | T7 | T7 | ✅ |
| `Preferences.LAST_MONSTER` | T2 | T10 (combat) | ✅ |
| `gameDatabase?.item(name)?.id` | T13 design | T13 | ✅ |
| `character?.state?.value?.secondsUntilRollover` | T7 | T7 | ✅ |
| `character?.state?.value?.moonPhase` | T7 | T7 | ✅ |

**Result:** All type references consistent across tasks.

---

## Cleanup

After the full plan has been executed and all tasks committed, delete the helper scripts:

```bash
rm docs/superpowers/plans/append_t1.py \
   docs/superpowers/plans/append_t2_t3.py \
   docs/superpowers/plans/append_t4_t5_t6.py \
   docs/superpowers/plans/append_t7_t10.py \
   docs/superpowers/plans/append_t11_t14.py \
   docs/superpowers/plans/append_review.py
```

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-06-07-ash-function-expansion.md`. Two execution options:

**1. Subagent-Driven (recommended)** — dispatch a fresh subagent per task with spec compliance + code quality review between each task, fast iteration without context pollution.

**2. Inline Execution** — execute tasks in this session using `superpowers:executing-plans`, with checkpoints between task groups.

Which approach?

