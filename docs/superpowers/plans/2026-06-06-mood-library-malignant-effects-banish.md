# Phase 8: Mood Library + Malignant Effect Clearing + BanishManager Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the mood system with auto-clearing of Beaten Up/poisons and a named mood library, then add monster banish tracking that persists across sessions.

**Architecture:** Three independent subsystems added in sequence: (1) `UneffectRequest` + `MalignantEffects` give `MoodManager` the ability to HTTP-remove bad effects; (2) `MoodManager` gains a `moodLibrary: Map<String, Mood>` persisted via preferences so users can save and switch named mood profiles; (3) `BanishManager` tracks banished monsters with turn-reset semantics, records new banishes from combat HTML detection, and clears on login.

**Tech Stack:** Kotlin Multiplatform, Ktor HTTP client (MockEngine for tests), Koin DI (`singleOf` / `single {}`), `StateFlow` for reactive state, `MapSettings` for in-memory preference testing.

**Prerequisites:** Phase 7 PR #7 merged to master before beginning. All code below references the merged state.

---

## File Structure

**New files:**
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/UneffectRequest.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mood/MalignantEffects.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/banish/Banisher.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/banish/BanishState.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/banish/BanishManager.kt`
- `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/UneffectRequestTest.kt`
- `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/banish/BanishManagerTest.kt`

**Modified files:**
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/preferences/Preferences.kt` — 3 new constants
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mood/MoodManager.kt` — `uneffectRequest` param, `removeMalignantEffects()`, mood library API
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureResult.kt` — `banished: Boolean = false` on `Combat`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureParser.kt` — banish text detection
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureManager.kt` — emit `MonsterBanished` + call `banishManager`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/event/GameEvent.kt` — `MonsterBanished` event
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/SessionManager.kt` — load mood library + banish on login
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt` — wire new classes
- `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/mood/MoodManagerTest.kt` — extend with library + malignant-effect tests
- `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/AdventureParserTest.kt` — banish detection tests (create file if absent)

---

### Task 1: UneffectRequest + MalignantEffects constants

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/UneffectRequest.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mood/MalignantEffects.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/UneffectRequestTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
// shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/UneffectRequestTest.kt
package net.sourceforge.kolmafia.request

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class UneffectRequestTest {

    @Test fun uneffect_success_returnsSuccess() {
        val client = HttpClient(MockEngine { respond("") })
        val req = UneffectRequest(client)
        val result = runBlocking { req.uneffect(42) }
        assertTrue(result.isSuccess)
    }

    @Test fun uneffect_networkError_returnsFailure() {
        val client = HttpClient(MockEngine { throw Exception("network error") })
        val req = UneffectRequest(client)
        val result = runBlocking { req.uneffect(42) }
        assertTrue(result.isFailure)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.request.UneffectRequestTest" --rerun-tasks
```

Expected: `FAILED` — `UneffectRequest not found`

- [ ] **Step 3: Create `UneffectRequest.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/UneffectRequest.kt
package net.sourceforge.kolmafia.request

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class UneffectRequest(private val client: HttpClient) {

    /** POSTs to uneffect.php to remove the effect with the given server ID. */
    suspend fun uneffect(effectId: Int): Result<Unit> = try {
        client.submitForm(
            url = "$KOL_BASE_URL/uneffect.php",
            formParameters = parameters {
                append("using", "Yep.")
                append("whicheffect", effectId.toString())
            }
        )
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

- [ ] **Step 4: Create `MalignantEffects.kt`**

These are the 9 effects the desktop auto-clears (`MoodManager.AUTO_CLEAR`).

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mood/MalignantEffects.kt
package net.sourceforge.kolmafia.mood

/**
 * Effect names auto-removed by [MoodManager.removeMalignantEffects] when
 * [net.sourceforge.kolmafia.preferences.Preferences.REMOVE_MALIGNANT_EFFECTS] is true.
 *
 * Matches desktop MoodManager.AUTO_CLEAR (EffectPool entries):
 *   BEATEN_UP, TETANUS, AMNESIA, CUNCTATITIS, and the five poison variants.
 */
object MalignantEffects {
    val NAMES: Set<String> = setOf(
        "Beaten Up",
        "Tetanus",
        "Amnesia",
        "Cunctatitis",
        "Hardly Poisoned at All",
        "A Little Bit Poisoned",
        "Somewhat Poisoned",
        "Really Quite Poisoned",
        "Majorly Poisoned",
    )
}
```

- [ ] **Step 5: Run tests to verify they pass**

```
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.request.UneffectRequestTest" --rerun-tasks
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/UneffectRequest.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mood/MalignantEffects.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/UneffectRequestTest.kt
git commit -m "feat: UneffectRequest HTTP wrapper + MalignantEffects constant set"
```

---

### Task 2: MoodManager.removeMalignantEffects()

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/preferences/Preferences.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mood/MoodManager.kt`
- Modify: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/mood/MoodManagerTest.kt`

- [ ] **Step 1: Add preference constant**

In `Preferences.kt`, add inside the `companion object Keys`:

```kotlin
// Malignant effect removal
const val REMOVE_MALIGNANT_EFFECTS = "removeMalignantEffects"  // default true
```

Add it after the `AUTO_BUFF` line:
```kotlin
const val AUTO_BUFF                = "autoBuff"
const val REMOVE_MALIGNANT_EFFECTS = "removeMalignantEffects"  // default true
const val ACTIVE_MOOD_NAME         = "activeMoodName"          // persisted active mood name
```

- [ ] **Step 2: Write failing tests**

Add to `MoodManagerTest.kt` (append after the last test):

```kotlin
// ── removeMalignantEffects ────────────────────────────────────────────────

@Test fun removeMalignantEffects_noMalignantEffects_doesNotCallUneffect() {
    val uneffected = mutableListOf<Int>()
    val manager = managerWithUneffect(uneffected, prefs())
    runBlocking {
        manager.removeMalignantEffects(effectState(effect(10, 3)))
    }
    assertTrue(uneffected.isEmpty())
}

@Test fun removeMalignantEffects_beatenUpPresent_callsUneffect() {
    val uneffected = mutableListOf<Int>()
    val manager = managerWithUneffect(uneffected, prefs())
    val beatenUp = EffectData(id = 4, name = "Beaten Up", duration = 5)
    runBlocking {
        manager.removeMalignantEffects(effectState(beatenUp))
    }
    assertEquals(listOf(4), uneffected)
}

@Test fun removeMalignantEffects_disabledByPref_doesNotUneffect() {
    val uneffected = mutableListOf<Int>()
    val s = MapSettings()
    s.putBoolean(Preferences.REMOVE_MALIGNANT_EFFECTS, false)
    val p = Preferences(s)
    val manager = managerWithUneffect(uneffected, p)
    val beatenUp = EffectData(id = 4, name = "Beaten Up", duration = 5)
    runBlocking {
        manager.removeMalignantEffects(effectState(beatenUp))
    }
    assertTrue(uneffected.isEmpty())
}

@Test fun removeMalignantEffects_multipleMalignantEffects_uneffectsAll() {
    val uneffected = mutableListOf<Int>()
    val manager = managerWithUneffect(uneffected, prefs())
    val effects = effectState(
        EffectData(id = 4,  name = "Beaten Up",            duration = 3),
        EffectData(id = 37, name = "Hardly Poisoned at All", duration = 1),
    )
    runBlocking { manager.removeMalignantEffects(effects) }
    assertEquals(setOf(4, 37), uneffected.toSet())
}

@Test fun executeActiveMood_clearsBeatenUp_evenWhenNoActiveMood() {
    val uneffected = mutableListOf<Int>()
    val manager = managerWithUneffect(uneffected, prefs())
    manager.activeMood = null
    val beatenUp = EffectData(id = 4, name = "Beaten Up", duration = 2)
    runBlocking {
        manager.executeActiveMood(effectState(beatenUp), SkillState(), CharacterState())
    }
    assertEquals(listOf(4), uneffected)
}
```

Also add these two helpers inside the `MoodManagerTest` class:

```kotlin
private fun managerWithUneffect(
    uneffected: MutableList<Int>,
    prefs: Preferences,
): MoodManager {
    val fakeUneffect = object : net.sourceforge.kolmafia.request.UneffectRequest(
        io.ktor.client.HttpClient(MockEngine { respond("") })
    ) {
        override suspend fun uneffect(effectId: Int): Result<Unit> {
            uneffected.add(effectId)
            return Result.success(Unit)
        }
    }
    return MoodManager(
        skillManager   = fakeCastSkillManager(mutableListOf()),
        preferences    = prefs,
        uneffectRequest = fakeUneffect,
    )
}
```

- [ ] **Step 3: Run tests to verify they fail**

```
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.MoodManagerTest" --rerun-tasks
```

Expected: `FAILED` — several compilation errors about missing `managerWithUneffect` and `removeMalignantEffects`

- [ ] **Step 4: Implement in `MoodManager.kt`**

Add `uneffectRequest: UneffectRequest? = null` to the constructor and add the new method. Replace the whole file:

```kotlin
package net.sourceforge.kolmafia.mood

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.UneffectRequest
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState

class MoodManager(
    private val skillManager: SkillManager,
    private val preferences: Preferences,
    private val uneffectRequest: UneffectRequest? = null,
) {
    var activeMood: Mood? = null

    companion object {
        fun missingTriggers(mood: Mood, effectState: EffectState): List<MoodTrigger> =
            mood.triggers.filter { trigger ->
                val remaining = effectState.effects
                    .firstOrNull { it.id == trigger.effectId }
                    ?.duration ?: 0
                remaining < trigger.minimumTurns
            }
    }

    // ── Malignant effect removal ──────────────────────────────────────────────

    /**
     * Removes any effect from [MalignantEffects.NAMES] currently active on the character.
     * No-op when [Preferences.REMOVE_MALIGNANT_EFFECTS] is false or [uneffectRequest] is null.
     */
    suspend fun removeMalignantEffects(effectState: EffectState) {
        if (!preferences.getBoolean(Preferences.REMOVE_MALIGNANT_EFFECTS, true)) return
        val req = uneffectRequest ?: return
        for (effect in effectState.effects) {
            if (effect.name in MalignantEffects.NAMES) {
                req.uneffect(effect.id)
            }
        }
    }

    // ── Mood execution ────────────────────────────────────────────────────────

    suspend fun executeActiveMood(
        effectState: EffectState,
        skillState: SkillState,
        charState: CharacterState,
    ) {
        removeMalignantEffects(effectState)
        val mood = activeMood ?: return
        if (!preferences.getBoolean(Preferences.AUTO_BUFF, true)) return
        for (trigger in missingTriggers(mood, effectState)) {
            val skill = skillState.skills.firstOrNull { it.id == trigger.skillId } ?: continue
            if (skill.mpCost > charState.currentMp) continue
            if (skill.dailyLimit > 0 && skill.timesCast >= skill.dailyLimit) continue
            skillManager.cast(skill)
        }
    }

    // ── Active mood persistence ───────────────────────────────────────────────

    /** Writes the current [activeMood] to preferences. Call whenever the mood changes. */
    fun saveActiveMood() {
        val mood = activeMood
        if (mood == null) {
            preferences.setString(Preferences.ACTIVE_MOOD_NAME, "")
            preferences.setString(Preferences.ACTIVE_MOOD_TRIGGERS, "")
            return
        }
        preferences.setString(Preferences.ACTIVE_MOOD_NAME, mood.name)
        preferences.setString(Preferences.ACTIVE_MOOD_TRIGGERS, serializeTriggers(mood.triggers))
    }

    /** Restores [activeMood] from preferences. Call once after login. */
    fun loadActiveMood() {
        val name = preferences.getString(Preferences.ACTIVE_MOOD_NAME)
        if (name.isBlank()) {
            activeMood = null
            return
        }
        val raw = preferences.getString(Preferences.ACTIVE_MOOD_TRIGGERS)
        activeMood = Mood(name, parseTriggers(raw))
    }

    // ── Serialization helpers ─────────────────────────────────────────────────

    internal fun serializeTriggers(triggers: List<MoodTrigger>): String =
        triggers.joinToString("|") { t ->
            "${t.effectId}:${t.effectName}:${t.skillId}:${t.skillName}:${t.minimumTurns}"
        }

    internal fun parseTriggers(raw: String): List<MoodTrigger> {
        if (raw.isBlank()) return emptyList()
        return raw.split("|").mapNotNull { entry ->
            val parts = entry.split(":", limit = 5)
            if (parts.size < 5) return@mapNotNull null
            MoodTrigger(
                effectId     = parts[0].toIntOrNull() ?: return@mapNotNull null,
                effectName   = parts[1],
                skillId      = parts[2].toIntOrNull() ?: return@mapNotNull null,
                skillName    = parts[3],
                minimumTurns = parts[4].toIntOrNull() ?: return@mapNotNull null,
            )
        }
    }
}
```

Note: `serializeTriggers` and `parseTriggers` are now `internal` so Task 3 can reuse them for the mood library without duplication.

- [ ] **Step 5: Update `SharedModule.kt` to wire `UneffectRequest` into `MoodManager`**

Replace `singleOf(::MoodManager)` with an explicit block (needed because `uneffectRequest` is a new injectable dependency):

```kotlin
// Add before singleOf(::MoodManager):
singleOf(::UneffectRequest)
// Replace singleOf(::MoodManager) with:
single { MoodManager(skillManager = get(), preferences = get(), uneffectRequest = get()) }
```

Also add the import at the top of `SharedModule.kt`:
```kotlin
import net.sourceforge.kolmafia.request.UneffectRequest
```

- [ ] **Step 6: Run tests to verify they pass**

```
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.MoodManagerTest" --rerun-tasks
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/preferences/Preferences.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mood/MoodManager.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/mood/MoodManagerTest.kt
git commit -m "feat: MoodManager.removeMalignantEffects() — auto-clear Beaten Up and poisons each mood pass"
```

---

### Task 3: Mood library API (addMood, removeMood, setActiveMoodByName, save/load)

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/preferences/Preferences.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mood/MoodManager.kt`
- Modify: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/mood/MoodManagerTest.kt`

The mood library stores N named moods. Serialization:
- `MOOD_LIBRARY_NAMES` = `|`-delimited list of names (mood names must not contain `|`)
- Per-mood triggers stored under the dynamic key `"moodTriggers_${name}"` using the same pipe-delimited format as `ACTIVE_MOOD_TRIGGERS`

- [ ] **Step 1: Add preference constant**

In `Preferences.kt` companion object, add after `ACTIVE_MOOD_TRIGGERS`:

```kotlin
const val MOOD_LIBRARY_NAMES       = "moodLibraryNames"        // |-separated saved mood names
// Per-mood data stored under "moodTriggers_${name}" (not a constant — dynamic key)
```

- [ ] **Step 2: Write failing tests**

Append to `MoodManagerTest.kt`:

```kotlin
// ── Mood library ──────────────────────────────────────────────────────────

@Test fun addMoodToLibrary_addsEntry() {
    val manager = MoodManager(fakeCastSkillManager(mutableListOf()), prefs())
    val mood = Mood("farming", listOf(trigger(10, 200)))
    manager.addMoodToLibrary(mood)
    assertEquals(mood, manager.moodLibrary["farming"])
}

@Test fun addMoodToLibrary_upsertsByName() {
    val manager = MoodManager(fakeCastSkillManager(mutableListOf()), prefs())
    manager.addMoodToLibrary(Mood("farming", listOf(trigger(10, 200))))
    val updated = Mood("farming", listOf(trigger(20, 300)))
    manager.addMoodToLibrary(updated)
    assertEquals(updated, manager.moodLibrary["farming"])
    assertEquals(1, manager.moodLibrary.size)
}

@Test fun removeMoodFromLibrary_removesEntry() {
    val manager = MoodManager(fakeCastSkillManager(mutableListOf()), prefs())
    manager.addMoodToLibrary(Mood("farming", listOf(trigger(10, 200))))
    manager.removeMoodFromLibrary("farming")
    assertTrue(manager.moodLibrary.isEmpty())
}

@Test fun removeMoodFromLibrary_unknownName_doesNotCrash() {
    val manager = MoodManager(fakeCastSkillManager(mutableListOf()), prefs())
    manager.removeMoodFromLibrary("nonexistent")  // should not throw
    assertTrue(manager.moodLibrary.isEmpty())
}

@Test fun setActiveMoodByName_knownName_setsActiveMoodAndReturnsTrue() {
    val s = MapSettings()
    val p = Preferences(s)
    val manager = MoodManager(fakeCastSkillManager(mutableListOf()), p)
    val mood = Mood("combat", listOf(trigger(10, 200)))
    manager.addMoodToLibrary(mood)
    val result = manager.setActiveMoodByName("combat")
    assertTrue(result)
    assertEquals(mood, manager.activeMood)
    // Also persists via saveActiveMood()
    assertEquals("combat", s.getString(Preferences.ACTIVE_MOOD_NAME, ""))
}

@Test fun setActiveMoodByName_unknownName_returnsFalse() {
    val manager = MoodManager(fakeCastSkillManager(mutableListOf()), prefs())
    assertFalse(manager.setActiveMoodByName("doesNotExist"))
    assertNull(manager.activeMood)
}

@Test fun saveMoodLibrary_and_loadMoodLibrary_roundtrip() {
    val s = MapSettings()
    val p = Preferences(s)
    val manager = MoodManager(fakeCastSkillManager(mutableListOf()), p)
    manager.addMoodToLibrary(Mood("farming", listOf(trigger(10, 200, minTurns = 5))))
    manager.addMoodToLibrary(Mood("leveling", listOf(trigger(20, 300))))
    manager.saveMoodLibrary()

    // Fresh manager loads from same settings
    val manager2 = MoodManager(fakeCastSkillManager(mutableListOf()), p)
    manager2.loadMoodLibrary()
    assertEquals(2, manager2.moodLibrary.size)
    assertEquals(listOf(trigger(10, 200, minTurns = 5)), manager2.moodLibrary["farming"]?.triggers)
    assertEquals(listOf(trigger(20, 300)), manager2.moodLibrary["leveling"]?.triggers)
}

@Test fun loadMoodLibrary_emptyPrefs_setsEmptyMap() {
    val manager = MoodManager(fakeCastSkillManager(mutableListOf()), prefs())
    manager.loadMoodLibrary()
    assertTrue(manager.moodLibrary.isEmpty())
}
```

Also update the existing `trigger()` helper to accept `minTurns`:

```kotlin
private fun trigger(effectId: Int, skillId: Int, minTurns: Int = 1) =
    MoodTrigger(effectId, "Effect $effectId", skillId, "Skill $skillId", minTurns)
```

And add the missing import to the test file:
```kotlin
import kotlin.test.assertFalse
import kotlin.test.assertNull
```

- [ ] **Step 3: Run tests to verify they fail**

```
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.MoodManagerTest" --rerun-tasks
```

Expected: `FAILED` — `moodLibrary`, `addMoodToLibrary`, etc. not found

- [ ] **Step 4: Implement mood library in `MoodManager.kt`**

Add after the `loadActiveMood()` function and before the serialization helpers:

```kotlin
// ── Mood library ──────────────────────────────────────────────────────────

var moodLibrary: Map<String, Mood> = emptyMap()
    private set

/** Adds or replaces the mood in the library by [Mood.name]. */
fun addMoodToLibrary(mood: Mood) {
    moodLibrary = moodLibrary + (mood.name to mood)
}

/** Removes the mood with the given [name] from the library. No-op if absent. */
fun removeMoodFromLibrary(name: String) {
    moodLibrary = moodLibrary - name
}

/**
 * Sets [activeMood] to the library entry named [name] and persists via [saveActiveMood].
 * Returns true on success, false if [name] is not in the library.
 */
fun setActiveMoodByName(name: String): Boolean {
    val mood = moodLibrary[name] ?: return false
    activeMood = mood
    saveActiveMood()
    return true
}

/** Persists the current [moodLibrary] to preferences. */
fun saveMoodLibrary() {
    val names = moodLibrary.keys.joinToString("|")
    preferences.setString(Preferences.MOOD_LIBRARY_NAMES, names)
    for ((name, mood) in moodLibrary) {
        preferences.setString("moodTriggers_$name", serializeTriggers(mood.triggers))
    }
}

/** Restores [moodLibrary] from preferences. Call once after login. */
fun loadMoodLibrary() {
    val namesRaw = preferences.getString(Preferences.MOOD_LIBRARY_NAMES)
    if (namesRaw.isBlank()) { moodLibrary = emptyMap(); return }
    val names = namesRaw.split("|").filter { it.isNotBlank() }
    moodLibrary = names.associate { name ->
        val raw = preferences.getString("moodTriggers_$name")
        name to Mood(name, parseTriggers(raw))
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

```
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.MoodManagerTest" --rerun-tasks
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/preferences/Preferences.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mood/MoodManager.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/mood/MoodManagerTest.kt
git commit -m "feat: mood library — addMoodToLibrary, removeMoodFromLibrary, setActiveMoodByName, save/load"
```

---

### Task 4: SessionManager login wiring for mood library

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/SessionManager.kt`

- [ ] **Step 1: Add `loadMoodLibrary()` call in login**

In `SessionManager.kt`, find the login success block. It currently ends with:

```kotlin
questLogRequest?.syncAll()
moodManager?.loadActiveMood()
SessionState.LoggedIn
```

Change it to:

```kotlin
questLogRequest?.syncAll()
moodManager?.loadActiveMood()
moodManager?.loadMoodLibrary()
SessionState.LoggedIn
```

- [ ] **Step 2: Run the full test suite**

```
./gradlew :shared:jvmTest --rerun-tasks
```

Expected: `BUILD SUCCESSFUL` (all existing tests still pass)

- [ ] **Step 3: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/SessionManager.kt
git commit -m "feat: restore mood library from preferences on login"
```

---

### Task 5: Banisher enum + BanishState data model

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/banish/Banisher.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/banish/BanishState.kt`

- [ ] **Step 1: Write failing tests**

Create a new test file:

```kotlin
// shared/src/commonTest/kotlin/net/sourceforge/kolmafia/banish/BanishManagerTest.kt
package net.sourceforge.kolmafia.banish

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class BanishManagerTest {

    // ── Banisher.fromName ────────────────────────────────────────────────────

    @Test fun banisher_fromName_knownBanisher_returnsIt() {
        assertEquals(Banisher.SNOKEBOMB, Banisher.fromName("snokebomb"))
    }

    @Test fun banisher_fromName_caseInsensitive() {
        assertEquals(Banisher.SABER_FORCE, Banisher.fromName("Saber Force"))
    }

    @Test fun banisher_fromName_unknownName_returnsUnknown() {
        assertEquals(Banisher.UNKNOWN, Banisher.fromName("some imaginary banisher"))
    }

    // ── BanishedMonster.isExpired ────────────────────────────────────────────

    @Test fun isExpired_turnBased_withinTurns_returnsFalse() {
        val b = BanishedMonster("Foo", Banisher.SNOKEBOMB, turnBanished = 100)
        // SNOKEBOMB has 30 turns; at turn 129 it's still active
        assertFalse(b.isExpired(currentTurn = 129))
    }

    @Test fun isExpired_turnBased_atExpiryTurn_returnsTrue() {
        val b = BanishedMonster("Foo", Banisher.SNOKEBOMB, turnBanished = 100)
        // 100 + 30 = 130; at turn 130 it expires
        assertTrue(b.isExpired(currentTurn = 130))
    }

    @Test fun isExpired_rolloverBanish_neverExpiresWithinRun() {
        val b = BanishedMonster("Foo", Banisher.BEANCANNON, turnBanished = 1)
        // BEANCANNON is ROLLOVER; never expires mid-run
        assertFalse(b.isExpired(currentTurn = 999))
    }

    @Test fun isExpired_neverReset_alwaysFalse() {
        val b = BanishedMonster("Foo", Banisher.ICE_HOUSE, turnBanished = 0)
        assertFalse(b.isExpired(currentTurn = 9999))
    }

    // BanishManager tests continue in Task 6
}
```

- [ ] **Step 2: Run tests to verify they fail**

```
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.banish.BanishManagerTest" --rerun-tasks
```

Expected: `FAILED` — classes not found

- [ ] **Step 3: Create `Banisher.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/banish/Banisher.kt
package net.sourceforge.kolmafia.banish

/**
 * How a banish is reset. Matches `BanishManager.Reset` in the desktop codebase.
 *
 * - [ROLLOVER]      — cleared every daily rollover (and on login, treated as rollover)
 * - [TURNS]         — cleared when the banish's turn count expires
 * - [TURN_ROLLOVER] — cleared when turns expire OR on rollover, whichever comes first
 * - [AVATAR]        — cleared on avatar/ascension change; treated as ROLLOVER here
 * - [NEVER]         — never expires (Ice House)
 */
enum class ResetType { ROLLOVER, TURNS, TURN_ROLLOVER, AVATAR, NEVER }

/**
 * The 20 most common banishers. Unknown banishers map to [UNKNOWN] which is treated
 * as ROLLOVER so it's safely cleared on next login.
 *
 * Sourced from desktop `BanishManager.Banisher` enum.
 */
enum class Banisher(
    val canonicalName: String,
    val turns: Int,            // -1 means rollover/avatar-only reset
    val resetType: ResetType,
    val isTurnFree: Boolean,
) {
    ANCHOR_BOMB("anchor bomb", 30, ResetType.TURN_ROLLOVER, true),
    BANISHING_SHOUT("banishing shout", -1, ResetType.AVATAR, false),
    BEANCANNON("beancannon", -1, ResetType.ROLLOVER, false),
    BOWL_A_CURVEBALL("Bowl a Curveball", -1, ResetType.ROLLOVER, true),
    BREATHE_OUT("breathe out", 20, ResetType.TURN_ROLLOVER, true),
    CHATTERBOXING("chatterboxing", 20, ResetType.TURN_ROLLOVER, true),
    DIVINE_CHAMPAGNE_POPPER("divine champagne popper", 5, ResetType.TURNS, true),
    FEEL_HATRED("Feel Hatred", 50, ResetType.TURN_ROLLOVER, true),
    ICE_HOUSE("ice house", -1, ResetType.NEVER, false),
    KGB_TRANQUILIZER_DART("KGB tranquilizer dart", 20, ResetType.TURN_ROLLOVER, true),
    LOUDER_THAN_BOMB("louder than bomb", 20, ResetType.TURN_ROLLOVER, true),
    MAFIA_MIDDLEFINGER_RING("mafia middle finger ring", 60, ResetType.TURN_ROLLOVER, true),
    PANTSGIVING("pantsgiving", 30, ResetType.TURN_ROLLOVER, false),
    REFLEX_HAMMER("Reflex Hammer", 30, ResetType.TURN_ROLLOVER, true),
    SABER_FORCE("Saber Force", 30, ResetType.TURN_ROLLOVER, true),
    SNOKEBOMB("snokebomb", 30, ResetType.TURN_ROLLOVER, true),
    SPRING_LOADED_FRONT_BUMPER("Spring-Loaded Front Bumper", 30, ResetType.TURN_ROLLOVER, true),
    STUFFED_YAM_STINKBOMB("stuffed yam stinkbomb", 15, ResetType.TURN_ROLLOVER, true),
    SYSTEM_SWEEP("System Sweep", -1, ResetType.ROLLOVER, false),
    THROW_LATTE_ON_OPPONENT("Throw Latte on Opponent", 30, ResetType.TURN_ROLLOVER, true),
    UNKNOWN("unknown", -1, ResetType.ROLLOVER, false);

    companion object {
        fun fromName(name: String): Banisher =
            entries.firstOrNull { it.canonicalName.equals(name, ignoreCase = true) } ?: UNKNOWN
    }
}
```

- [ ] **Step 4: Create `BanishState.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/banish/BanishState.kt
package net.sourceforge.kolmafia.banish

/**
 * A single banished monster entry.
 *
 * @param monsterName  The name of the banished monster (case-insensitive for lookups).
 * @param banisher     Which banisher was used; [Banisher.UNKNOWN] for unrecognised banishers.
 * @param turnBanished The value of [CharacterState.currentRun] at the time of banishment.
 */
data class BanishedMonster(
    val monsterName: String,
    val banisher: Banisher,
    val turnBanished: Int,
) {
    /**
     * Returns true if the banish has expired based on turn count.
     * ROLLOVER, AVATAR, and NEVER banishes never expire during a run — only [clearExpiredAndRollover]
     * removes them at login.
     */
    fun isExpired(currentTurn: Int): Boolean = when (banisher.resetType) {
        ResetType.TURNS, ResetType.TURN_ROLLOVER ->
            banisher.turns > 0 && currentTurn >= turnBanished + banisher.turns
        ResetType.ROLLOVER, ResetType.AVATAR, ResetType.NEVER -> false
    }
}

data class BanishState(val monsters: List<BanishedMonster> = emptyList())
```

- [ ] **Step 5: Run tests to verify they pass**

```
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.banish.BanishManagerTest" --rerun-tasks
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/banish/Banisher.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/banish/BanishState.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/banish/BanishManagerTest.kt
git commit -m "feat: Banisher enum + BanishState data model"
```

---

### Task 6: BanishManager core logic + persistence

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/banish/BanishManager.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/preferences/Preferences.kt`
- Modify: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/banish/BanishManagerTest.kt`

- [ ] **Step 1: Add preference constant**

In `Preferences.kt` companion object, add after `MOOD_LIBRARY_NAMES`:

```kotlin
// Banish tracking
const val BANISHED_MONSTERS = "banishedMonsters"   // serialized banish list (same key as desktop)
```

- [ ] **Step 2: Write failing tests**

Append to `BanishManagerTest.kt`:

```kotlin
// ── BanishManager ────────────────────────────────────────────────────────

@Test fun banishMonster_recordsEntry() {
    val manager = BanishManager(prefs())
    manager.banishMonster("Ninja Snowman", Banisher.SNOKEBOMB, currentTurn = 50)
    assertTrue(manager.isBanished("Ninja Snowman", currentTurn = 60))
}

@Test fun banishMonster_caseInsensitiveLookup() {
    val manager = BanishManager(prefs())
    manager.banishMonster("ninja snowman", Banisher.SNOKEBOMB, currentTurn = 50)
    assertTrue(manager.isBanished("Ninja Snowman", currentTurn = 60))
}

@Test fun banishMonster_duplicateMonster_replacesExisting() {
    val manager = BanishManager(prefs())
    manager.banishMonster("Ninja Snowman", Banisher.SNOKEBOMB, currentTurn = 50)
    manager.banishMonster("Ninja Snowman", Banisher.SABER_FORCE, currentTurn = 80)
    // Only one entry; uses newer banish
    assertEquals(1, manager.state.value.monsters.size)
    assertEquals(Banisher.SABER_FORCE, manager.state.value.monsters.first().banisher)
}

@Test fun isBanished_unknownMonster_returnsFalse() {
    val manager = BanishManager(prefs())
    assertFalse(manager.isBanished("Random Monster", currentTurn = 1))
}

@Test fun isBanished_turnBanish_withinTurns_returnsTrue() {
    val manager = BanishManager(prefs())
    manager.banishMonster("Foo", Banisher.SNOKEBOMB, currentTurn = 100)
    assertTrue(manager.isBanished("Foo", currentTurn = 129))   // 100 + 30 - 1 = 129
}

@Test fun isBanished_turnBanish_expired_returnsFalse() {
    val manager = BanishManager(prefs())
    manager.banishMonster("Foo", Banisher.SNOKEBOMB, currentTurn = 100)
    assertFalse(manager.isBanished("Foo", currentTurn = 130))  // 100 + 30 = expired
}

@Test fun clearExpiredAndRollover_removesRolloverBanishes() {
    val manager = BanishManager(prefs())
    manager.banishMonster("Foo", Banisher.BEANCANNON, currentTurn = 1)   // ROLLOVER
    manager.clearExpiredAndRollover(currentTurn = 2)
    assertFalse(manager.isBanished("Foo", currentTurn = 2))
}

@Test fun clearExpiredAndRollover_removesAvatarBanishes() {
    val manager = BanishManager(prefs())
    manager.banishMonster("Foo", Banisher.BANISHING_SHOUT, currentTurn = 1)  // AVATAR
    manager.clearExpiredAndRollover(currentTurn = 2)
    assertFalse(manager.isBanished("Foo", currentTurn = 2))
}

@Test fun clearExpiredAndRollover_removesTurnRolloverBanishes() {
    val manager = BanishManager(prefs())
    manager.banishMonster("Foo", Banisher.SNOKEBOMB, currentTurn = 1)  // TURN_ROLLOVER
    // Regardless of turn count, TURN_ROLLOVER banishes clear on rollover/login
    manager.clearExpiredAndRollover(currentTurn = 5)
    assertFalse(manager.isBanished("Foo", currentTurn = 5))
}

@Test fun clearExpiredAndRollover_keepsNeverBanish() {
    val manager = BanishManager(prefs())
    manager.banishMonster("Foo", Banisher.ICE_HOUSE, currentTurn = 1)   // NEVER
    manager.clearExpiredAndRollover(currentTurn = 9999)
    assertTrue(manager.isBanished("Foo", currentTurn = 9999))
}

@Test fun save_and_load_roundtrip() {
    val s = com.russhwolf.settings.MapSettings()
    val p = net.sourceforge.kolmafia.preferences.Preferences(s)
    val manager = BanishManager(p)
    manager.banishMonster("Ninja Snowman", Banisher.SNOKEBOMB, currentTurn = 100)
    manager.banishMonster("Ice Cream Sandwich", Banisher.SABER_FORCE, currentTurn = 200)
    manager.save()

    val manager2 = BanishManager(p)
    manager2.load()
    assertTrue(manager2.isBanished("Ninja Snowman", currentTurn = 110))
    assertTrue(manager2.isBanished("Ice Cream Sandwich", currentTurn = 210))
}

@Test fun load_emptyPrefs_setsEmptyState() {
    val manager = BanishManager(prefs())
    manager.load()
    assertTrue(manager.state.value.monsters.isEmpty())
}

@Test fun load_malformedEntry_skipsIt() {
    val s = com.russhwolf.settings.MapSettings()
    s.putString(net.sourceforge.kolmafia.preferences.Preferences.BANISHED_MONSTERS, "bad|Ninja Snowman:snokebomb:100")
    val p = net.sourceforge.kolmafia.preferences.Preferences(s)
    val manager = BanishManager(p)
    manager.load()
    // "bad" entry has only 1 field; should be skipped; valid entry should load
    assertEquals(1, manager.state.value.monsters.size)
}

// ── Test helper ───────────────────────────────────────────────────────────

private fun prefs(): net.sourceforge.kolmafia.preferences.Preferences =
    net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
```

- [ ] **Step 3: Run tests to verify they fail**

```
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.banish.BanishManagerTest" --rerun-tasks
```

Expected: `FAILED` — `BanishManager` not found

- [ ] **Step 4: Create `BanishManager.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/banish/BanishManager.kt
package net.sourceforge.kolmafia.banish

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Tracks banished monsters across adventure turns.
 *
 * Serialization format for [Preferences.BANISHED_MONSTERS]:
 *   Records separated by `|`; each record is `monsterName:banisherName:turnBanished`
 *   using `split(":", limit = 3)` so monster names containing `:` are safe.
 *
 * Usage:
 *   - Call [load] once after login.
 *   - Call [banishMonster] when a combat banish is detected.
 *   - Call [isBanished] before adventuring to skip banished monsters.
 *   - Call [clearExpiredAndRollover] at login to remove stale rollover banishes.
 */
class BanishManager(private val preferences: Preferences) {
    private val _state = MutableStateFlow(BanishState())
    val state: StateFlow<BanishState> = _state.asStateFlow()

    /**
     * Records a banish, replacing any existing entry for the same monster.
     * Persists to preferences immediately.
     */
    fun banishMonster(monsterName: String, banisher: Banisher, currentTurn: Int) {
        val existing = _state.value.monsters.toMutableList()
        existing.removeIf { it.monsterName.equals(monsterName, ignoreCase = true) }
        existing.add(BanishedMonster(monsterName, banisher, currentTurn))
        _state.value = BanishState(existing)
        save()
    }

    /** Returns true if [monsterName] has an active (non-expired) banish at [currentTurn]. */
    fun isBanished(monsterName: String, currentTurn: Int): Boolean =
        _state.value.monsters.any { b ->
            b.monsterName.equals(monsterName, ignoreCase = true) && !b.isExpired(currentTurn)
        }

    /**
     * Removes all [ResetType.ROLLOVER], [ResetType.AVATAR], and [ResetType.TURN_ROLLOVER] banishes
     * (they reset every rollover), and also removes expired [ResetType.TURNS] banishes.
     * [ResetType.NEVER] banishes (Ice House) are kept.
     * Call this at login after loading state.
     */
    fun clearExpiredAndRollover(currentTurn: Int) {
        _state.value = _state.value.copy(
            monsters = _state.value.monsters.filter { b ->
                when (b.banisher.resetType) {
                    ResetType.ROLLOVER, ResetType.AVATAR, ResetType.TURN_ROLLOVER -> false
                    ResetType.TURNS -> !b.isExpired(currentTurn)
                    ResetType.NEVER -> true
                }
            }
        )
        save()
    }

    /** Serializes [state] to [Preferences.BANISHED_MONSTERS]. */
    fun save() {
        val serialized = _state.value.monsters.joinToString("|") { b ->
            "${b.monsterName}:${b.banisher.canonicalName}:${b.turnBanished}"
        }
        preferences.setString(Preferences.BANISHED_MONSTERS, serialized)
    }

    /** Restores [state] from [Preferences.BANISHED_MONSTERS]. Call once after login. */
    fun load() {
        val raw = preferences.getString(Preferences.BANISHED_MONSTERS)
        if (raw.isBlank()) { _state.value = BanishState(); return }
        val monsters = raw.split("|").mapNotNull { entry ->
            val parts = entry.split(":", limit = 3)
            if (parts.size < 3) return@mapNotNull null
            val banisher = Banisher.fromName(parts[1])
            val turn = parts[2].toIntOrNull() ?: return@mapNotNull null
            BanishedMonster(monsterName = parts[0], banisher = banisher, turnBanished = turn)
        }
        _state.value = BanishState(monsters)
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

```
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.banish.BanishManagerTest" --rerun-tasks
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/preferences/Preferences.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/banish/BanishManager.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/banish/BanishManagerTest.kt
git commit -m "feat: BanishManager — track banished monsters with turn/rollover reset semantics"
```

---

### Task 7: DI + GameEvent + AdventureParser banish detection + AdventureManager + SessionManager wiring

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/event/GameEvent.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureResult.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureParser.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureManager.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/SessionManager.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt`
- Modify: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/AdventureParserTest.kt` (create if absent)

- [ ] **Step 1: Add `GameEvent.MonsterBanished`**

In `GameEvent.kt`, add after `data class CombatFinished(...)`:

```kotlin
data class MonsterBanished(val monsterName: String, val banisherName: String) : GameEvent()
```

- [ ] **Step 2: Add `banished` flag to `AdventureResult.Combat`**

In `AdventureResult.kt`, change the `Combat` data class to:

```kotlin
data class Combat(
    val monster: String, val won: Boolean,
    val itemsGained: List<String> = emptyList(),
    val meatGained: Int = 0,
    val statsGained: Map<String, Int> = emptyMap(),
    val banished: Boolean = false,
) : AdventureResult()
```

- [ ] **Step 3: Write failing banish detection tests**

Create `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/AdventureParserTest.kt` (or append to it if it already exists):

```kotlin
package net.sourceforge.kolmafia.adventure

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class AdventureParserTest {

    @Test fun parseFightResult_winWithoutBanish_banishedFalse() {
        val html = """
            <span id='monname'>Ninja Snowman</span>
            You win the fight!
            You gain 100 Meat
        """.trimIndent()
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.won)
        assertFalse(result.banished)
    }

    @Test fun parseFightResult_fleeInTerror_banishedTrue() {
        val html = """
            <span id='monname'>Ninja Snowman</span>
            The monster flees in terror!
        """.trimIndent()
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
        assertEquals("Ninja Snowman", result.monster)
    }

    @Test fun parseFightResult_banishedFromAdventures_banishedTrue() {
        val html = """
            <span id='monname'>Ninja Snowman</span>
            The Ninja Snowman is banished from your adventures for the rest of today.
        """.trimIndent()
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
    }

    @Test fun parseFightResult_goneSomewhere_banishedTrue() {
        val html = """
            <span id='monname'>Ninja Snowman</span>
            It has gone somewhere else to live.
        """.trimIndent()
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
    }

    @Test fun parseFightResult_fleeField_banishedTrue() {
        val html = """
            <span id='monname'>Spooky Vampire</span>
            It flees the field.
        """.trimIndent()
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
    }
}
```

- [ ] **Step 4: Run tests to verify they fail**

```
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.adventure.AdventureParserTest" --rerun-tasks
```

Expected: `FAILED` — `AdventureResult.Combat` missing `banished` field or banish detection not implemented

- [ ] **Step 5: Add banish detection to `AdventureParser.kt`**

Add the regex constant and update `parseFightResult`:

```kotlin
// Add with the other private regex constants at the top of the object:
private val BANISH_PATTERN = Regex(
    """(?:flees? in terror|banished? from|gone somewhere else|flees? the (?:area|field)|flee[sd]? the (?:area|field))""",
    RegexOption.IGNORE_CASE
)
```

Update `parseFightResult`:

```kotlin
fun parseFightResult(html: String): AdventureResult.Combat {
    val won = WIN_PATTERN.containsMatchIn(html)
    val monster = MONSTER_NAME.find(html)?.groupValues?.get(1) ?: "Unknown"
    val items = ITEM_GAINED.findAll(html).map { it.groupValues[1].trim() }.toList()
    val meat = parseMeat(html)
    val stats = parseStats(html)
    val banished = BANISH_PATTERN.containsMatchIn(html)
    return AdventureResult.Combat(monster, won, items, meat, stats, banished = banished)
}
```

- [ ] **Step 6: Run tests to verify they pass**

```
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.adventure.AdventureParserTest" --rerun-tasks
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Wire `BanishManager` into `AdventureManager`**

In `AdventureManager.kt`:

Add import:
```kotlin
import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.banish.Banisher
```

Add `banishManager: BanishManager? = null` as the last constructor parameter:

```kotlin
class AdventureManager(
    // ... existing params ...
    private val manaBurnManager: ManaBurnManager? = null,
    private val banishManager: BanishManager? = null,
)
```

In `resolveCombat()`, after `eventBus.emit(GameEvent.CombatFinished(...))` and the `emitItemEvents(result.itemsGained)` call, add:

```kotlin
if (result.banished) {
    eventBus.emit(GameEvent.MonsterBanished(result.monster, Banisher.UNKNOWN.canonicalName))
    banishManager?.banishMonster(
        monsterName  = result.monster,
        banisher     = Banisher.UNKNOWN,
        currentTurn  = character.state.value.currentRun,
    )
}
```

The full updated `resolveCombat` function:

```kotlin
private suspend fun resolveCombat(location: AdventureLocation): AdventureResult.Combat? {
    val macro = MacroStrategy.forLocation(location.id, preferences)
    val fightHtml = fightRequest.fight(macro).getOrElse {
        eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.NetworkError(it)))
        return null
    }
    val result = AdventureParser.parseFightResult(fightHtml)
    eventBus.emit(GameEvent.CombatFinished(result.won, result.monster))
    emitItemEvents(result.itemsGained)
    if (result.banished) {
        eventBus.emit(GameEvent.MonsterBanished(result.monster, Banisher.UNKNOWN.canonicalName))
        banishManager?.banishMonster(
            monsterName = result.monster,
            banisher    = Banisher.UNKNOWN,
            currentTurn = character.state.value.currentRun,
        )
    }
    if (!result.won) {
        eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.CharacterDeath))
        return null
    }
    return result
}
```

- [ ] **Step 8: Wire `BanishManager` into `SessionManager`**

In `SessionManager.kt`:

Add import:
```kotlin
import net.sourceforge.kolmafia.banish.BanishManager
```

Add `banishManager: BanishManager? = null` as the last constructor parameter.

In the login success block, add after `moodManager?.loadMoodLibrary()`:

```kotlin
banishManager?.load()
banishManager?.clearExpiredAndRollover(character.state.value.currentRun)
SessionState.LoggedIn
```

Full updated login block end:

```kotlin
questLogRequest?.syncAll()
moodManager?.loadActiveMood()
moodManager?.loadMoodLibrary()
banishManager?.load()
banishManager?.clearExpiredAndRollover(character.state.value.currentRun)
SessionState.LoggedIn
```

- [ ] **Step 9: Register `BanishManager` in `SharedModule.kt`**

Add import:
```kotlin
import net.sourceforge.kolmafia.banish.BanishManager
```

Add after `singleOf(::ManaBurnManager)`:
```kotlin
singleOf(::BanishManager)
```

Change `AdventureManager` single block to add `banishManager = get()`:
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
    )
}
```

Change `SessionManager` single block to add `banishManager = get()`:
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
    )
}
```

- [ ] **Step 10: Run the full test suite**

```
./gradlew :shared:jvmTest --rerun-tasks
```

Expected: `BUILD SUCCESSFUL` (all tests pass)

- [ ] **Step 11: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/event/GameEvent.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureResult.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureParser.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureManager.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/SessionManager.kt \
        shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt \
        shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/AdventureParserTest.kt
git commit -m "feat: BanishManager DI wiring — banish detection in combat + session load/clear on login"
```
