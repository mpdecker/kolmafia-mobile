# Goal Depth & Loop Reliability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the recovery stop-threshold correctness bug and implement complete goal-driven adventure loop stopping — item name goals, meat goals, and level goals.

**Architecture:** Four tasks in dependency order. T1 adds stop-threshold helpers to `RecoveryManager` and replaces the single recovery call in `AdventureManager` with a multi-iteration loop. T2 expands `GoalManager` with name-based item goals, meat goals, and level goals, and adds `StopReason.GoalMet`. T3 wires item-name goal checking into the adventure loop using a per-turn flag. T4 adds post-state-refresh numeric goal checks (meat, level). All changes are in the `shared` module; no DI wiring changes needed.

**Tech Stack:** Kotlin Multiplatform, `kotlin.test`, Ktor `MockEngine`, `MapSettings` (multiplatform-settings)

**Test command:** `./gradlew :shared:allTests` (run from `C:/Development/kolmafia-mobile`)

---

## File Map

| Status | File | What changes |
|--------|------|--------------|
| Modify | `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/recovery/RecoveryManager.kt` | Add `hpAboveStopThreshold`, `mpAboveStopThreshold` helpers; add `force: Boolean = false` to `recoverIfNeeded` |
| Modify | `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureManager.kt` | Replace single recovery call with loop; add `itemGoalMetThisTurn` flag; check goal flags in loop; make `goalManager` internal |
| Modify | `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/StopReason.kt` | Add `GoalMet(description: String)` |
| Modify | `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/GoalManager.kt` | Add name goals, meat goal, level goal; update `clearGoals` |
| Modify | `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/recovery/RecoveryManagerTest.kt` | Tests for stop-threshold helpers |
| Modify | `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/session/GoalManagerTest.kt` | Tests for all new GoalManager APIs |
| Modify | `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/AdventureManagerTest.kt` | Tests for item goal stop and numeric goal stop |

---

### Task T1: Recovery stop-threshold loop

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/recovery/RecoveryManager.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureManager.kt`
- Modify: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/recovery/RecoveryManagerTest.kt`

**Background:** `hpRecoveryStopPct` / `mpRecoveryStopPct` preference keys are defined in `Preferences.kt` but currently unused. Recovery fires once per turn-gap. A user at 5% HP using one potion may reach 70% but the stop threshold is 90% — they'd enter combat still low. The fix: add stop-threshold helpers to `RecoveryManager.companion` and replace the single call in `AdventureManager.runAdventures` with a loop that continues until the stop pct is met or no more items/skills are available (max 10 iterations to prevent infinite loops when recovery items exist but never push HP above the threshold).

The `force: Boolean = false` parameter on `recoverIfNeeded` is needed because, after the first recovery iteration, HP may be above the trigger threshold (`hpRecoveryTargetPct`) but below the stop threshold. Without `force = true`, `recoverIfNeeded` would return false (nothing healed) and break the loop prematurely.

- [ ] **Step 1: Write failing tests for the stop-threshold helpers**

Append to `RecoveryManagerTest.kt`:

```kotlin
// ── Stop-threshold helpers ───────────────────────────────────────────────

@Test fun hpAboveStop_belowStopPct_returnsFalse() {
    val p = prefs(Preferences.AUTO_RECOVER_HP to true, Preferences.HP_RECOVERY_STOP_PCT to 90)
    assertFalse(RecoveryManager.hpAboveStopThreshold(state(70, 100), p))
}

@Test fun hpAboveStop_atStopPct_returnsTrue() {
    val p = prefs(Preferences.AUTO_RECOVER_HP to true, Preferences.HP_RECOVERY_STOP_PCT to 90)
    assertTrue(RecoveryManager.hpAboveStopThreshold(state(90, 100), p))
}

@Test fun hpAboveStop_aboveStopPct_returnsTrue() {
    val p = prefs(Preferences.AUTO_RECOVER_HP to true, Preferences.HP_RECOVERY_STOP_PCT to 90)
    assertTrue(RecoveryManager.hpAboveStopThreshold(state(95, 100), p))
}

@Test fun hpAboveStop_zeroMaxHp_returnsTrue() {
    val p = prefs(Preferences.AUTO_RECOVER_HP to true, Preferences.HP_RECOVERY_STOP_PCT to 90)
    assertTrue(RecoveryManager.hpAboveStopThreshold(state(0, 0), p))
}

@Test fun hpAboveStop_defaultStopPctIs90() {
    val p = prefs(Preferences.AUTO_RECOVER_HP to true)
    assertFalse(RecoveryManager.hpAboveStopThreshold(state(89, 100), p))
    assertTrue(RecoveryManager.hpAboveStopThreshold(state(90, 100), p))
}

@Test fun mpAboveStop_belowStopPct_returnsFalse() {
    val p = prefs(Preferences.AUTO_RECOVER_MP to true, Preferences.MP_RECOVERY_STOP_PCT to 80)
    assertFalse(RecoveryManager.mpAboveStopThreshold(state(0, 0, 50, 100), p))
}

@Test fun mpAboveStop_atStopPct_returnsTrue() {
    val p = prefs(Preferences.AUTO_RECOVER_MP to true, Preferences.MP_RECOVERY_STOP_PCT to 80)
    assertTrue(RecoveryManager.mpAboveStopThreshold(state(0, 0, 80, 100), p))
}

@Test fun mpAboveStop_zeroMaxMp_returnsTrue() {
    val p = prefs(Preferences.AUTO_RECOVER_MP to true, Preferences.MP_RECOVERY_STOP_PCT to 80)
    assertTrue(RecoveryManager.mpAboveStopThreshold(state(0, 0, 0, 0), p))
}
```

- [ ] **Step 2: Run tests to verify they fail**

```
./gradlew :shared:allTests --tests "net.sourceforge.kolmafia.recovery.RecoveryManagerTest"
```

Expected: compilation failure — `hpAboveStopThreshold` and `mpAboveStopThreshold` do not exist yet.

- [ ] **Step 3: Add helpers and `force` param to RecoveryManager**

In `RecoveryManager.kt`, add two methods to the `companion object` (after `needsMpRecovery`, before `isFullRestore`):

```kotlin
fun hpAboveStopThreshold(state: CharacterState, prefs: Preferences): Boolean {
    if (state.maxHp <= 0) return true
    val stopPct = prefs.getInt(Preferences.HP_RECOVERY_STOP_PCT, 90)
    return state.currentHp * 100 / state.maxHp >= stopPct
}

fun mpAboveStopThreshold(state: CharacterState, prefs: Preferences): Boolean {
    if (state.maxMp <= 0) return true
    val stopPct = prefs.getInt(Preferences.MP_RECOVERY_STOP_PCT, 90)
    return state.currentMp * 100 / state.maxMp >= stopPct
}
```

Change `recoverIfNeeded` signature to add `force`:

```kotlin
suspend fun recoverIfNeeded(
    charState: CharacterState,
    invState: InventoryState,
    skillState: SkillState,
    force: Boolean = false,
): Boolean {
    var recovered = false
    if (force || needsHpRecovery(charState, preferences)) {
        recovered = recoverHp(charState, invState, skillState) || recovered
    }
    if (force || needsMpRecovery(charState, preferences)) {
        recovered = recoverMp(charState, invState, skillState) || recovered
    }
    return recovered
}
```

- [ ] **Step 4: Replace single recovery call in AdventureManager with a loop**

In `AdventureManager.kt`, find this block in `runAdventures` (it's after `checkQuestAdvancement` in the prior code but before `TurnConsumed`):

```kotlin
val healed = recoveryManager?.recoverIfNeeded(
    charState  = character.state.value,
    invState   = inventory?.state?.value ?: InventoryState(),
    skillState = skills?.state?.value ?: SkillState(),
)
if (healed == true) {
    characterRequest.fetchCharacterState().onSuccess { character.updateFromApiResponse(it) }
}
```

Replace it with:

```kotlin
// Recovery loop: repeat until stop threshold met or no recovery available (max 10 iterations)
val rm = recoveryManager
if (rm != null) {
    var firstIter = true
    var iter = 0
    while (iter++ < 10) {
        val healed = rm.recoverIfNeeded(
            charState  = character.state.value,
            invState   = inventory?.state?.value ?: InventoryState(),
            skillState = skills?.state?.value ?: SkillState(),
            force      = !firstIter,  // after first recovery, bypass trigger-threshold check
        )
        firstIter = false
        if (!healed) break
        characterRequest.fetchCharacterState().onSuccess { character.updateFromApiResponse(it) }
        val s = character.state.value
        val hpDone = !preferences.getBoolean(Preferences.AUTO_RECOVER_HP, true) ||
                     RecoveryManager.hpAboveStopThreshold(s, preferences)
        val mpDone = !preferences.getBoolean(Preferences.AUTO_RECOVER_MP, false) ||
                     RecoveryManager.mpAboveStopThreshold(s, preferences)
        if (hpDone && mpDone) break
    }
}
```

Note: the `characterRequest.fetchCharacterState()` call that was previously outside the recovery block is now inside the loop. Remove any duplicate outside fetch if one was already there.

- [ ] **Step 5: Run all tests**

```
./gradlew :shared:allTests
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/recovery/RecoveryManager.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureManager.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/recovery/RecoveryManagerTest.kt
git commit -m "feat: recovery loops until stop-threshold; add hpAboveStopThreshold/mpAboveStopThreshold"
```

---

### Task T2: GoalManager expansion + StopReason.GoalMet

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/GoalManager.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/StopReason.kt`
- Modify: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/session/GoalManagerTest.kt`

**Background:** `GoalManager` only tracks item goals by integer ID. Adventure HTML parsing yields item names (not IDs — the parser outputs `itemsGained: List<String>`), so name-based goals are needed. Meat and level goals complete the "run until X" contract. `StopReason.GoalMet` is added here so T3/T4 can use it.

Note: the private field currently named `itemGoals` is renamed to `_itemGoalIds` in the new implementation. The public API (`addItemGoal`, `removeItemGoal`, `hasItemGoal`, `itemGoalIds()`) is unchanged.

- [ ] **Step 1: Write failing tests for new GoalManager APIs**

Append to `GoalManagerTest.kt`:

```kotlin
// ── Name-based item goals ──────────────────────────────────────────────

@Test fun hasItemGoalByName_afterAdd_returnsTrue() {
    val gm = GoalManager()
    gm.addItemGoalByName("Knob Goblin Lasso")
    assertTrue(gm.hasItemGoalByName("Knob Goblin Lasso"))
}

@Test fun hasItemGoalByName_caseInsensitive() {
    val gm = GoalManager()
    gm.addItemGoalByName("Knob Goblin Lasso")
    assertTrue(gm.hasItemGoalByName("knob goblin lasso"))
    assertTrue(gm.hasItemGoalByName("KNOB GOBLIN LASSO"))
}

@Test fun hasItemGoalByName_leadingTrailingWhitespace_matches() {
    val gm = GoalManager()
    gm.addItemGoalByName("  rat whisker  ")
    assertTrue(gm.hasItemGoalByName("rat whisker"))
}

@Test fun removeItemGoalByName_removesIt() {
    val gm = GoalManager()
    gm.addItemGoalByName("Meat Vortex")
    gm.removeItemGoalByName("Meat Vortex")
    assertFalse(gm.hasItemGoalByName("Meat Vortex"))
}

@Test fun hasItemGoalByName_notAdded_returnsFalse() {
    assertFalse(GoalManager().hasItemGoalByName("nothing"))
}

// ── Meat goal ──────────────────────────────────────────────────────────

@Test fun hasMeatGoal_atGoal_returnsTrue() {
    val gm = GoalManager()
    gm.setMeatGoal(10_000)
    assertTrue(gm.hasMeatGoal(10_000))
}

@Test fun hasMeatGoal_aboveGoal_returnsTrue() {
    val gm = GoalManager()
    gm.setMeatGoal(10_000)
    assertTrue(gm.hasMeatGoal(15_000))
}

@Test fun hasMeatGoal_belowGoal_returnsFalse() {
    val gm = GoalManager()
    gm.setMeatGoal(10_000)
    assertFalse(gm.hasMeatGoal(9_999))
}

@Test fun hasMeatGoal_noGoalSet_returnsFalse() {
    assertFalse(GoalManager().hasMeatGoal(99_999))
}

@Test fun clearMeatGoal_clearsIt() {
    val gm = GoalManager()
    gm.setMeatGoal(5_000)
    gm.clearMeatGoal()
    assertFalse(gm.hasMeatGoal(99_999))
}

// ── Level goal ────────────────────────────────────────────────────────

@Test fun hasLevelGoal_atGoal_returnsTrue() {
    val gm = GoalManager()
    gm.setLevelGoal(10)
    assertTrue(gm.hasLevelGoal(10))
}

@Test fun hasLevelGoal_aboveGoal_returnsTrue() {
    val gm = GoalManager()
    gm.setLevelGoal(10)
    assertTrue(gm.hasLevelGoal(11))
}

@Test fun hasLevelGoal_belowGoal_returnsFalse() {
    val gm = GoalManager()
    gm.setLevelGoal(10)
    assertFalse(gm.hasLevelGoal(9))
}

@Test fun hasLevelGoal_noGoalSet_returnsFalse() {
    assertFalse(GoalManager().hasLevelGoal(99))
}

@Test fun clearLevelGoal_clearsIt() {
    val gm = GoalManager()
    gm.setLevelGoal(10)
    gm.clearLevelGoal()
    assertFalse(gm.hasLevelGoal(99))
}

// ── clearGoals clears everything ─────────────────────────────────────

@Test fun clearGoals_clearsNameGoalsAndNumericGoals() {
    val gm = GoalManager()
    gm.addItemGoal(1)
    gm.addItemGoalByName("Rat Whisker")
    gm.setMeatGoal(5_000)
    gm.setLevelGoal(10)
    gm.clearGoals()
    assertFalse(gm.hasItemGoal(1))
    assertFalse(gm.hasItemGoalByName("Rat Whisker"))
    assertFalse(gm.hasMeatGoal(99_999))
    assertFalse(gm.hasLevelGoal(99))
}
```

- [ ] **Step 2: Run tests to verify they fail**

```
./gradlew :shared:allTests --tests "net.sourceforge.kolmafia.session.GoalManagerTest"
```

Expected: compilation failure — `addItemGoalByName`, `setMeatGoal`, `setLevelGoal`, etc. do not exist.

- [ ] **Step 3: Implement the expanded GoalManager**

Replace the entire content of `GoalManager.kt`:

```kotlin
package net.sourceforge.kolmafia.session

/**
 * Tracks acquisition goals for the current automation session.
 * Supports item goals by ID, item goals by name (case-insensitive), meat goals, and level goals.
 */
class GoalManager {
    private val _itemGoalIds   = mutableSetOf<Int>()
    private val _itemGoalNames = mutableSetOf<String>()  // stored lowercase+trimmed
    private var meatGoal: Int?  = null
    private var levelGoal: Int? = null

    // ── ID-based item goals ───────────────────────────────────────────────────

    fun addItemGoal(itemId: Int)    { _itemGoalIds.add(itemId) }
    fun removeItemGoal(itemId: Int) { _itemGoalIds.remove(itemId) }
    fun hasItemGoal(itemId: Int): Boolean = _itemGoalIds.contains(itemId)
    fun itemGoalIds(): Set<Int> = _itemGoalIds.toSet()

    // ── Name-based item goals (case-insensitive) ──────────────────────────────

    fun addItemGoalByName(name: String)    { _itemGoalNames.add(name.lowercase().trim()) }
    fun removeItemGoalByName(name: String) { _itemGoalNames.remove(name.lowercase().trim()) }
    fun hasItemGoalByName(name: String): Boolean = _itemGoalNames.contains(name.lowercase().trim())

    // ── Meat goal ─────────────────────────────────────────────────────────────

    fun setMeatGoal(meat: Int)  { meatGoal = meat }
    fun clearMeatGoal()         { meatGoal = null }
    /** Returns true when [currentMeat] meets or exceeds the configured meat goal. */
    fun hasMeatGoal(currentMeat: Int): Boolean = meatGoal?.let { currentMeat >= it } ?: false

    // ── Level goal ────────────────────────────────────────────────────────────

    fun setLevelGoal(level: Int)  { levelGoal = level }
    fun clearLevelGoal()          { levelGoal = null }
    /** Returns true when [currentLevel] meets or exceeds the configured level goal. */
    fun hasLevelGoal(currentLevel: Int): Boolean = levelGoal?.let { currentLevel >= it } ?: false

    // ── Clear all ─────────────────────────────────────────────────────────────

    fun clearGoals() {
        _itemGoalIds.clear()
        _itemGoalNames.clear()
        meatGoal = null
        levelGoal = null
    }
}
```

- [ ] **Step 4: Add `StopReason.GoalMet` to StopReason.kt**

Replace the file content:

```kotlin
package net.sourceforge.kolmafia.adventure

sealed class StopReason {
    object UserCancelled : StopReason()
    object NoAdventuresLeft : StopReason()
    object CharacterDeath : StopReason()
    data class GoalMet(val description: String) : StopReason()
    data class MacroError(val message: String) : StopReason()
    data class NetworkError(val cause: Throwable) : StopReason()
}
```

- [ ] **Step 5: Run all tests**

```
./gradlew :shared:allTests
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/GoalManager.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/StopReason.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/session/GoalManagerTest.kt
git commit -m "feat: GoalManager name/meat/level goals; StopReason.GoalMet"
```

---

### Task T3: Item name goal check in adventure loop

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureManager.kt`
- Modify: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/AdventureManagerTest.kt`

**Background:** `AdventureParser` already parses item names from both non-combat and combat HTML using `ITEM_GAINED = Regex("""You acquire an item:\s*<b>(.*?)</b>""")`. Both `AdventureResult.NonCombat.itemsGained` and `AdventureResult.Combat.itemsGained` are `List<String>` of item names, already trimmed. All flow through `emitItemEvents`.

This task: make `goalManager` accessible to tests (`internal`), add a per-turn `itemGoalMetThisTurn: Boolean` flag that `emitItemEvents` sets when a name goal matches, and check it in `runAdventures` immediately after `doOneTurn`.

- [ ] **Step 1: Write failing tests**

Add to `AdventureManagerTest.kt`:

```kotlin
// Add this constant to the companion object:
const val NON_COMBAT_WITH_ITEM_HTML = """<html><body><b>A Spooky Treehouse</b>
<p>You acquire an item: <b>rat whisker</b></p>
<p>You gain 10 Meat.</p></body></html>"""
```

Add these test methods to the class body:

```kotlin
@Test
fun runAdventures_stopsWithGoalMet_whenItemGoalSatisfied() = runTest {
    val (manager, bus, received) = makeManager(adventureHtml = NON_COMBAT_WITH_ITEM_HTML)
    val collectJob = launch { bus.events.collect { received.add(it) } }

    manager.goalManager.addItemGoalByName("rat whisker")
    manager.runAdventures(testLocation, 10, CoroutineScope(Dispatchers.Default)).join()

    collectJob.cancel()
    val stopped = received.filterIsInstance<GameEvent.AdventureLoopStopped>()
    assertEquals(1, stopped.size)
    assertIs<StopReason.GoalMet>(stopped.first().reason)
    // Stopped after first turn; TurnConsumed was emitted before GoalMet
    assertEquals(1, received.filterIsInstance<GameEvent.TurnConsumed>().size)
}

@Test
fun runAdventures_doesNotStopForGoal_whenNoItemGoalSet() = runTest {
    val (manager, bus, received) = makeManager(adventureHtml = NON_COMBAT_WITH_ITEM_HTML)
    val collectJob = launch { bus.events.collect { received.add(it) } }

    // No goal — runs 3 turns (adventures never run out in this mock)
    manager.runAdventures(testLocation, 3, CoroutineScope(Dispatchers.Default)).join()

    collectJob.cancel()
    assertFalse(
        received.filterIsInstance<GameEvent.AdventureLoopStopped>()
            .any { it.reason is StopReason.GoalMet }
    )
    assertEquals(3, received.filterIsInstance<GameEvent.TurnConsumed>().size)
}

@Test
fun runAdventures_doesNotStop_whenItemNameDoesNotMatchGoal() = runTest {
    val (manager, bus, received) = makeManager(adventureHtml = NON_COMBAT_WITH_ITEM_HTML)
    val collectJob = launch { bus.events.collect { received.add(it) } }

    manager.goalManager.addItemGoalByName("completely different item")
    manager.runAdventures(testLocation, 2, CoroutineScope(Dispatchers.Default)).join()

    collectJob.cancel()
    assertFalse(
        received.filterIsInstance<GameEvent.AdventureLoopStopped>()
            .any { it.reason is StopReason.GoalMet }
    )
}
```

- [ ] **Step 2: Run tests to verify they fail**

```
./gradlew :shared:allTests --tests "net.sourceforge.kolmafia.adventure.AdventureManagerTest.runAdventures_stopsWithGoalMet_whenItemGoalSatisfied"
```

Expected: compilation error — `manager.goalManager` is not accessible (it's `private`).

- [ ] **Step 3: Make goalManager internal and add itemGoalMetThisTurn flag**

In `AdventureManager.kt`:

**Change this line** (in the constructor parameters):
```kotlin
private val goalManager: GoalManager = GoalManager(),
```
**To:**
```kotlin
internal val goalManager: GoalManager = GoalManager(),
```

**Add this member variable** right after `private var lastTurnResponseText: String = ""`:
```kotlin
private var itemGoalMetThisTurn = false
```

**Change `emitItemEvents`** to detect goal matches:
```kotlin
private suspend fun emitItemEvents(items: List<String>) {
    items.forEach { name ->
        eventBus.emit(GameEvent.ItemObtained(InventoryItem(-1, name, 1, ItemType.OTHER)))
        if (goalManager.hasItemGoalByName(name)) itemGoalMetThisTurn = true
    }
}
```

**In `runAdventures`**, at the top of the `repeat(turns)` block, right after `if (!isActive) return@launch`, add the flag reset:
```kotlin
itemGoalMetThisTurn = false
```

**After `val result = doOneTurn(location) ?: return@launch`**, add the goal-met check:
```kotlin
// Emit turn consumed, then stop if an item goal was satisfied this turn
if (itemGoalMetThisTurn) {
    eventBus.emit(GameEvent.TurnConsumed(location, result))
    eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.GoalMet("item goal met")))
    return@launch
}
```

This block goes BEFORE the `characterRequest.fetchCharacterState()` call — no need to fetch state if we're stopping.

- [ ] **Step 4: Run all tests**

```
./gradlew :shared:allTests
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureManager.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/AdventureManagerTest.kt
git commit -m "feat: stop adventure loop when item name goal satisfied"
```

---

### Task T4: Numeric goal checks (meat and level) in adventure loop

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureManager.kt`
- Modify: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/AdventureManagerTest.kt`

**Background:** After each `fetchCharacterState`, the loop has an up-to-date `meat` (from `CharacterState.meat`) and `level` (from `CharacterState.level`). This task adds checks for meat and level goals immediately after the state refresh, before the recovery loop. The mock engine in tests returns a constant `api.php` response, so `STATUS_JSON_HIGH_MEAT` and `STATUS_JSON_HIGH_LEVEL` constants drive the character state seen inside the loop.

- [ ] **Step 1: Write failing tests**

Add two constants to `AdventureManagerTest`'s companion object:

```kotlin
const val STATUS_JSON_HIGH_MEAT = """{"name":"Player","playerid":"1","level":"5","class":"1","hp":"50","hpmax":"100","mp":"30","mpmax":"50","meat":"50000","adventures":"40","fullness":"0","drunk":"0","spleen":"0"}"""
const val STATUS_JSON_HIGH_LEVEL = """{"name":"Player","playerid":"1","level":"15","class":"1","hp":"50","hpmax":"100","mp":"30","mpmax":"50","meat":"1000","adventures":"40","fullness":"0","drunk":"0","spleen":"0"}"""
```

Add test methods:

```kotlin
@Test
fun runAdventures_stopsWithGoalMet_whenMeatGoalReached() = runTest {
    val (manager, bus, received) = makeManager(statusJson = STATUS_JSON_HIGH_MEAT)
    val collectJob = launch { bus.events.collect { received.add(it) } }

    manager.goalManager.setMeatGoal(40_000)  // 50_000 >= 40_000 → stop
    manager.runAdventures(testLocation, 5, CoroutineScope(Dispatchers.Default)).join()

    collectJob.cancel()
    val stopped = received.filterIsInstance<GameEvent.AdventureLoopStopped>()
    assertEquals(1, stopped.size)
    val reason = stopped.first().reason
    assertIs<StopReason.GoalMet>(reason)
    assertTrue((reason as StopReason.GoalMet).description.contains("meat", ignoreCase = true))
    assertEquals(1, received.filterIsInstance<GameEvent.TurnConsumed>().size)
}

@Test
fun runAdventures_stopsWithGoalMet_whenLevelGoalReached() = runTest {
    val (manager, bus, received) = makeManager(statusJson = STATUS_JSON_HIGH_LEVEL)
    val collectJob = launch { bus.events.collect { received.add(it) } }

    manager.goalManager.setLevelGoal(12)  // level 15 >= 12 → stop
    manager.runAdventures(testLocation, 5, CoroutineScope(Dispatchers.Default)).join()

    collectJob.cancel()
    val stopped = received.filterIsInstance<GameEvent.AdventureLoopStopped>()
    assertEquals(1, stopped.size)
    assertIs<StopReason.GoalMet>(stopped.first().reason)
    assertEquals(1, received.filterIsInstance<GameEvent.TurnConsumed>().size)
}

@Test
fun runAdventures_doesNotStop_whenMeatBelowGoal() = runTest {
    // STATUS_JSON_ADVENTURES_LEFT has meat=1000
    val (manager, bus, received) = makeManager()
    val collectJob = launch { bus.events.collect { received.add(it) } }

    manager.goalManager.setMeatGoal(999_999)  // far above meat=1000
    manager.runAdventures(testLocation, 2, CoroutineScope(Dispatchers.Default)).join()

    collectJob.cancel()
    assertFalse(
        received.filterIsInstance<GameEvent.AdventureLoopStopped>()
            .any { it.reason is StopReason.GoalMet }
    )
}

@Test
fun runAdventures_doesNotStop_whenLevelBelowGoal() = runTest {
    // STATUS_JSON_ADVENTURES_LEFT has level=5
    val (manager, bus, received) = makeManager()
    val collectJob = launch { bus.events.collect { received.add(it) } }

    manager.goalManager.setLevelGoal(20)  // far above level=5
    manager.runAdventures(testLocation, 2, CoroutineScope(Dispatchers.Default)).join()

    collectJob.cancel()
    assertFalse(
        received.filterIsInstance<GameEvent.AdventureLoopStopped>()
            .any { it.reason is StopReason.GoalMet }
    )
}
```

- [ ] **Step 2: Run tests to verify they fail**

```
./gradlew :shared:allTests --tests "net.sourceforge.kolmafia.adventure.AdventureManagerTest.runAdventures_stopsWithGoalMet_whenMeatGoalReached"
```

Expected: FAIL — `hasMeatGoal` exists but the loop has no goal check yet.

- [ ] **Step 3: Add numeric goal checks to runAdventures**

In `AdventureManager.kt`, find the `characterRequest.fetchCharacterState()` call that occurs right after the item-goal check (from T3). After this state refresh, add the numeric goal checks. The block looks like:

```kotlin
characterRequest.fetchCharacterState().onSuccess { character.updateFromApiResponse(it) }
```

After that line, add:

```kotlin
// Numeric goal checks (meat, level) — evaluated on up-to-date character state
val charAfterTurn = character.state.value
if (goalManager.hasMeatGoal(charAfterTurn.meat)) {
    eventBus.emit(GameEvent.TurnConsumed(location, result))
    eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.GoalMet("meat goal met: ${charAfterTurn.meat}")))
    return@launch
}
if (goalManager.hasLevelGoal(charAfterTurn.level)) {
    eventBus.emit(GameEvent.TurnConsumed(location, result))
    eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.GoalMet("level goal met: ${charAfterTurn.level}")))
    return@launch
}
```

These checks go BEFORE the recovery loop — no point recovering if we're stopping.

The final ordering inside the `repeat` block should be:
1. `itemGoalMetThisTurn = false` — reset flag
2. mood execution
3. `doOneTurn` — adventure + fight/choice resolution + item events
4. item goal check — stop if `itemGoalMetThisTurn`
5. `fetchCharacterState` — refresh state
6. meat goal check — stop if `hasMeatGoal(meat)`
7. level goal check — stop if `hasLevelGoal(level)`
8. recovery loop (T1)
9. `checkQuestAdvancement`
10. `eventBus.emit(GameEvent.TurnConsumed(...))`
11. `adventuresLeft / currentHp` stop checks

- [ ] **Step 4: Run all tests**

```
./gradlew :shared:allTests
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureManager.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/AdventureManagerTest.kt
git commit -m "feat: stop adventure loop when meat or level goal reached"
```

---

## Self-Review Checklist

After completing all tasks, verify:

1. `./gradlew :shared:allTests` passes with BUILD SUCCESSFUL
2. `RecoveryManager` companion object has `hpAboveStopThreshold` and `mpAboveStopThreshold`
3. `recoverIfNeeded` has `force: Boolean = false` parameter
4. `AdventureManager.runAdventures` recovery section is a `while (iter++ < 10)` loop with `force = !firstIter`
5. `GoalManager` has `addItemGoalByName`, `hasItemGoalByName`, `setMeatGoal`, `hasMeatGoal`, `setLevelGoal`, `hasLevelGoal`
6. `StopReason.GoalMet(val description: String)` exists
7. `AdventureManager.goalManager` is `internal` (not `private`)
8. `itemGoalMetThisTurn` is reset at the top of each `repeat` iteration
9. `TurnConsumed` is emitted before `AdventureLoopStopped(GoalMet)` in all stop paths
10. Numeric goal checks come before the recovery loop (no recovery if we're stopping)
