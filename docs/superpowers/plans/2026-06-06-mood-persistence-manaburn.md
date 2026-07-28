# Mood Persistence & ManaBurn Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the mood system by persisting the active mood across logins and adding ManaBurn (auto-cast excess MP into buffs post-turn), plus fix the `my_familiar()` ASH bug.

**Architecture:** Three independent improvements to the mood/ASH subsystem. Mood persistence adds `saveActiveMood()`/`loadActiveMood()` to `MoodManager` using a pipe-delimited preferences string, called on login from `SessionManager`. ManaBurn is a new `ManaBurnManager` class that picks the lowest-duration mood effect skill and casts it while MP is above a user-configured threshold; wired into `AdventureManager` as a post-recovery loop. The ASH fix is a single-line correction in `GameRuntimeLibrary`.

**Tech Stack:** Kotlin Multiplatform, Koin DI (`singleOf`), `com.russhwolf.settings.Settings` (preferences), Ktor MockEngine (tests), kotlin.test

---

## File Map

| File | Action | Purpose |
|------|--------|---------|
| `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/preferences/Preferences.kt` | Modify | Add new pref key constants; remove stale TODO comments |
| `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mood/MoodManager.kt` | Modify | Add `saveActiveMood()` / `loadActiveMood()` with serialization helpers |
| `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/SessionManager.kt` | Modify | Accept `MoodManager?` param; call `loadActiveMood()` on successful login |
| `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mood/ManaBurnManager.kt` | **Create** | New class: `shouldBurn()`, `pickSkillToBurn()`, `burnIfEnabled()` |
| `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureManager.kt` | Modify | Accept `ManaBurnManager?` param; add post-recovery mana burn loop |
| `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt` | Modify | Wire `ManaBurnManager`, update `SessionManager` and `AdventureManager` singletons |
| `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt` | Modify | Fix `my_familiar()` to read `familiarName` not `name` |
| `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryTest.kt` | Modify | Add `my_familiar` regression test |
| `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/mood/MoodManagerTest.kt` | Modify | Add round-trip persistence tests |
| `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/mood/ManaBurnManagerTest.kt` | **Create** | Full unit test suite for `ManaBurnManager` |

---

### Task 1: Fix `my_familiar()` ASH bug

**Background:** `my_familiar()` in `GameRuntimeLibrary.kt` (line 298–300) reads
`character?.state?.value?.name` — the player's name — instead of
`character?.state?.value?.familiarName`. Any ASH script calling `my_familiar()` gets the
wrong value.

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`
- Modify: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryTest.kt`

- [ ] **Step 1: Write the failing test**

Add this test to `GameRuntimeLibraryTest.kt`. It needs a character-backed library, so
add a helper alongside `run()`:

```kotlin
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter

// add to GameRuntimeLibraryTest class:

private fun runWithCharacter(character: KoLCharacter, src: String): String {
    val lib = GameRuntimeLibrary(character = character)
    val runtime = AshRuntime(lib)
    runtime.execute(AshParser().parse(src))
    return runtime.output.toString().trim()
}

@Test fun myFamiliar_returnsFamiliarName_notPlayerName() {
    val character = KoLCharacter()
    character.updateFromApiResponse(
        CharacterApiResponse(name = "PlayerName", familiarname = "Exotic Parrot")
    )
    val result = runWithCharacter(character, "print(to_string(my_familiar()));")
    assertEquals("Exotic Parrot", result)
}

@Test fun myFamiliar_noFamiliar_returnsNone() {
    val character = KoLCharacter()
    character.updateFromApiResponse(
        CharacterApiResponse(name = "PlayerName", familiarname = "")
    )
    val result = runWithCharacter(character, "print(to_string(my_familiar()));")
    assertEquals("none", result)
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```
.\gradlew :shared:allTests --tests "*.GameRuntimeLibraryTest.myFamiliar*"
```

Expected: FAIL — `myFamiliar_returnsFamiliarName_notPlayerName` gets "PlayerName" instead of
"Exotic Parrot".

- [ ] **Step 3: Apply the fix**

In `GameRuntimeLibrary.kt`, locate `registerCharacterQueries` (around line 298) and replace:

```kotlin
register(scope, "my_familiar", AshType.FAMILIAR, emptyList()) { _, _ ->
    AshValue.familiar(character?.state?.value?.name ?: "none")
}
```

with:

```kotlin
register(scope, "my_familiar", AshType.FAMILIAR, emptyList()) { _, _ ->
    val name = character?.state?.value?.familiarName?.takeIf { it.isNotBlank() } ?: "none"
    AshValue.familiar(name)
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```
.\gradlew :shared:allTests --tests "*.GameRuntimeLibraryTest.myFamiliar*"
```

Expected: PASS (both tests).

- [ ] **Step 5: Run full suite to confirm no regressions**

```
.\gradlew :shared:allTests
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt
git add shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryTest.kt
git commit -m "fix: my_familiar() returns familiarName not player name"
```

---

### Task 2: Mood Persistence — New Preference Keys

**Background:** `MoodManager` needs two new pref keys: the mood's name and a serialized
trigger list. Also clean up two stale TODO comments in `Preferences.kt` left over from
the Phase 6 recovery-loop work.

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/preferences/Preferences.kt`

- [ ] **Step 1: Add pref key constants and clean up stale TODOs**

In `Preferences.kt`, update the companion object:

```kotlin
companion object Keys {
    const val LAST_USERNAME = "lastUsername"

    // HP recovery
    const val AUTO_RECOVER_HP          = "autoRecoverHp"
    const val HP_RECOVERY_TARGET_PCT   = "hpRecoveryTargetPct"   // below → start recovering
    const val HP_RECOVERY_STOP_PCT     = "hpRecoveryStopPct"     // above → stop recovering

    // MP recovery
    const val AUTO_RECOVER_MP          = "autoRecoverMp"
    const val MP_RECOVERY_TARGET_PCT   = "mpRecoveryTargetPct"
    const val MP_RECOVERY_STOP_PCT     = "mpRecoveryStopPct"     // above → stop recovering

    // Mood
    const val AUTO_BUFF                = "autoBuff"
    const val ACTIVE_MOOD_NAME         = "activeMoodName"        // persisted active mood name
    const val ACTIVE_MOOD_TRIGGERS     = "activeMoodTriggers"    // serialized trigger list

    // ManaBurn
    const val MANA_BURN_ENABLED        = "manaBurnEnabled"       // default false
    const val MANA_BURN_BELOW_PCT      = "manaBurnBelowPct"      // burn while MP% >= this; default 90
}
```

(The TODO comments on `HP_RECOVERY_STOP_PCT` and `MP_RECOVERY_STOP_PCT` are removed — the
loop that uses them was implemented in Phase 6.)

- [ ] **Step 2: Run full test suite — expect no change**

```
.\gradlew :shared:allTests
```

Expected: BUILD SUCCESSFUL (constant-only change, no behavior change).

- [ ] **Step 3: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/preferences/Preferences.kt
git commit -m "feat: add mood persistence and ManaBurn preference key constants"
```

---

### Task 3: Mood Persistence — Serialize / Deserialize

**Background:** Add `saveActiveMood()` and `loadActiveMood()` to `MoodManager`. The
serialization format is:
- `ACTIVE_MOOD_NAME`: the mood's name string
- `ACTIVE_MOOD_TRIGGERS`: pipe-delimited records; each record is colon-delimited with
  `effectId:effectName:skillId:skillName:minimumTurns`.
  Split with `limit = 5` so that a colon inside `effectName` or `skillName` (which never
  appears in KoL but would be malformed) results in fewer-than-5 parts and is safely
  skipped. An empty/blank value means no active mood.

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mood/MoodManager.kt`
- Modify: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/mood/MoodManagerTest.kt`

- [ ] **Step 1: Write failing tests**

Add to `MoodManagerTest.kt` (after the existing `executeActiveMood_dailyLimitReached_skipsSkill` test):

```kotlin
// ── Persistence ───────────────────────────────────────────────────────────

@Test fun saveAndLoad_roundtrips_activeMood() {
    val settings = com.russhwolf.settings.MapSettings()
    val p = Preferences(settings)
    val manager = MoodManager(fakeCastSkillManager(mutableListOf()), p)

    val mood = Mood("combat", listOf(
        MoodTrigger(100, "Butt-Rock Hair", 4055, "Disco Nap", 3),
        MoodTrigger(200, "Strength of the Grizzly", 4095, "Musk of the Moose", 1),
    ))
    manager.activeMood = mood
    manager.saveActiveMood()

    manager.activeMood = null           // clear in-memory
    manager.loadActiveMood()

    assertEquals(mood, manager.activeMood)
}

@Test fun saveAndLoad_emptyTriggerList_roundtrips() {
    val settings = com.russhwolf.settings.MapSettings()
    val p = Preferences(settings)
    val manager = MoodManager(fakeCastSkillManager(mutableListOf()), p)

    manager.activeMood = Mood("empty", emptyList())
    manager.saveActiveMood()
    manager.activeMood = null
    manager.loadActiveMood()

    assertEquals(Mood("empty", emptyList()), manager.activeMood)
}

@Test fun loadActiveMood_noPrefsData_leavesNull() {
    val settings = com.russhwolf.settings.MapSettings()  // empty
    val p = Preferences(settings)
    val manager = MoodManager(fakeCastSkillManager(mutableListOf()), p)
    manager.activeMood = Mood("x", emptyList())

    manager.loadActiveMood()

    assertNull(manager.activeMood)
}

@Test fun saveActiveMood_nullMood_clearsPrefs() {
    val settings = com.russhwolf.settings.MapSettings()
    settings.putString(Preferences.ACTIVE_MOOD_NAME, "old")
    settings.putString(Preferences.ACTIVE_MOOD_TRIGGERS, "100:EffectName:200:SkillName:1")
    val p = Preferences(settings)
    val manager = MoodManager(fakeCastSkillManager(mutableListOf()), p)

    manager.activeMood = null
    manager.saveActiveMood()

    assertEquals("", p.getString(Preferences.ACTIVE_MOOD_NAME))
    assertEquals("", p.getString(Preferences.ACTIVE_MOOD_TRIGGERS))
}
```

Also add `import kotlin.test.assertNull` to the imports.

- [ ] **Step 2: Run tests to confirm they fail**

```
.\gradlew :shared:allTests --tests "*.MoodManagerTest.saveAndLoad*" --tests "*.MoodManagerTest.loadActiveMood*" --tests "*.MoodManagerTest.saveActiveMood*"
```

Expected: FAIL — `saveActiveMood` / `loadActiveMood` do not exist yet.

- [ ] **Step 3: Implement saveActiveMood / loadActiveMood in MoodManager**

Replace the entire `MoodManager.kt` with:

```kotlin
package net.sourceforge.kolmafia.mood

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState

class MoodManager(
    private val skillManager: SkillManager,
    private val preferences: Preferences,
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

    suspend fun executeActiveMood(
        effectState: EffectState,
        skillState: SkillState,
        charState: CharacterState,
    ) {
        val mood = activeMood ?: return
        if (!preferences.getBoolean(Preferences.AUTO_BUFF, true)) return
        for (trigger in missingTriggers(mood, effectState)) {
            val skill = skillState.skills.firstOrNull { it.id == trigger.skillId } ?: continue
            if (skill.mpCost > charState.currentMp) continue
            if (skill.dailyLimit > 0 && skill.timesCast >= skill.dailyLimit) continue
            skillManager.cast(skill)
        }
    }

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

    private fun serializeTriggers(triggers: List<MoodTrigger>): String =
        triggers.joinToString("|") { t ->
            "${t.effectId}:${t.effectName}:${t.skillId}:${t.skillName}:${t.minimumTurns}"
        }

    private fun parseTriggers(raw: String): List<MoodTrigger> {
        if (raw.isBlank()) return emptyList()
        return raw.split("|").mapNotNull { entry ->
            val parts = entry.split(":", limit = 5)
            if (parts.size < 5) return@mapNotNull null
            MoodTrigger(
                effectId     = parts[0].toIntOrNull() ?: return@mapNotNull null,
                effectName   = parts[1],
                skillId      = parts[2].toIntOrNull() ?: return@mapNotNull null,
                skillName    = parts[3],
                minimumTurns = parts[4].toIntOrNull() ?: 1,
            )
        }
    }
}
```

- [ ] **Step 4: Run persistence tests to confirm they pass**

```
.\gradlew :shared:allTests --tests "*.MoodManagerTest*"
```

Expected: all MoodManagerTest tests pass (new and existing).

- [ ] **Step 5: Run full suite**

```
.\gradlew :shared:allTests
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mood/MoodManager.kt
git add shared/src/commonTest/kotlin/net/sourceforge/kolmafia/mood/MoodManagerTest.kt
git commit -m "feat: mood persistence — saveActiveMood/loadActiveMood with trigger serialization"
```

---

### Task 4: Mood Persistence — Load on Login

**Background:** Wire `MoodManager.loadActiveMood()` into the login sequence so the active
mood is restored automatically when a session starts. `SessionManager` currently has no
reference to `MoodManager`, so we add it as an optional injected parameter.

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/SessionManager.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt`

- [ ] **Step 1: Add MoodManager to SessionManager**

In `SessionManager.kt`, add `moodManager` as the last optional parameter and call
`loadActiveMood()` in the login success block:

```kotlin
package net.sourceforge.kolmafia.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.sourceforge.kolmafia.ash.ScriptManager
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.character.DailyResourceTracker
import net.sourceforge.kolmafia.data.GameDatabase
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
                        dailyResourceTracker.syncDay(character.state.value.dayCount)
                        inventoryManager.initialize(appScope)
                        familiarManager.initialize(appScope)
                        skillManager.initialize(appScope)
                        effectManager.initialize(appScope)
                        scriptManager.initialize()
                        questLogRequest?.syncAll()
                        moodManager?.loadActiveMood()
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

- [ ] **Step 2: Update SharedModule to pass moodManager to SessionManager**

In `SharedModule.kt`, find the `single { SessionManager(...) }` block and add
`moodManager = get()` as the last named argument:

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
    )
}
```

- [ ] **Step 3: Run full suite to confirm no regressions**

```
.\gradlew :shared:allTests
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/SessionManager.kt
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt
git commit -m "feat: restore active mood from preferences on login"
```

---

### Task 5: ManaBurnManager — Core Logic

**Background:** `ManaBurnManager` burns excess MP after each adventure turn by casting the
skill from the active mood that will extend the effect with the fewest remaining turns.
The two static helpers (`shouldBurn`, `pickSkillToBurn`) are pure functions with no side
effects, making them easy to test in isolation.

**Pref semantics:**
- `MANA_BURN_ENABLED` — defaults `false`. Nothing happens unless explicitly enabled.
- `MANA_BURN_BELOW_PCT` — defaults `90`. Burn is triggered while `currentMp * 100 / maxMp >= belowPct`.
  I.e., only when MP is high (above 90% by default). Set lower to burn more aggressively.

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mood/ManaBurnManager.kt`
- Create: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/mood/ManaBurnManagerTest.kt`

- [ ] **Step 1: Write the failing tests first**

Create `ManaBurnManagerTest.kt`:

```kotlin
package net.sourceforge.kolmafia.mood

import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState
import net.sourceforge.kolmafia.skill.SkillType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ManaBurnManagerTest {

    // ── shouldBurn ────────────────────────────────────────────────────────────

    @Test fun shouldBurn_disabled_returnsFalse() {
        val prefs = prefs(enabled = false)
        assertFalse(ManaBurnManager.shouldBurn(CharacterState(currentMp = 100, maxMp = 100), prefs))
    }

    @Test fun shouldBurn_enabledAboveThreshold_returnsTrue() {
        val prefs = prefs(enabled = true, belowPct = 90)
        assertTrue(ManaBurnManager.shouldBurn(CharacterState(currentMp = 95, maxMp = 100), prefs))
    }

    @Test fun shouldBurn_enabledAtThreshold_returnsTrue() {
        val prefs = prefs(enabled = true, belowPct = 90)
        assertTrue(ManaBurnManager.shouldBurn(CharacterState(currentMp = 90, maxMp = 100), prefs))
    }

    @Test fun shouldBurn_enabledBelowThreshold_returnsFalse() {
        val prefs = prefs(enabled = true, belowPct = 90)
        assertFalse(ManaBurnManager.shouldBurn(CharacterState(currentMp = 89, maxMp = 100), prefs))
    }

    @Test fun shouldBurn_zeroMaxMp_returnsFalse() {
        val prefs = prefs(enabled = true)
        assertFalse(ManaBurnManager.shouldBurn(CharacterState(currentMp = 0, maxMp = 0), prefs))
    }

    // ── pickSkillToBurn ───────────────────────────────────────────────────────

    @Test fun pickSkillToBurn_noMood_returnsNull() {
        assertNull(
            ManaBurnManager.pickSkillToBurn(null, EffectState(), SkillState(), CharacterState())
        )
    }

    @Test fun pickSkillToBurn_emptyMood_returnsNull() {
        assertNull(
            ManaBurnManager.pickSkillToBurn(
                Mood("x", emptyList()), EffectState(), SkillState(), CharacterState()
            )
        )
    }

    @Test fun pickSkillToBurn_returnsSkillForLowestDurationEffect() {
        val mood = Mood("test", listOf(
            MoodTrigger(effectId = 10, effectName = "E10", skillId = 100, skillName = "S100"),
            MoodTrigger(effectId = 20, effectName = "E20", skillId = 200, skillName = "S200"),
        ))
        val effectState = EffectState(effects = listOf(
            EffectData(id = 10, name = "E10", duration = 5),
            EffectData(id = 20, name = "E20", duration = 1),   // shorter → burn first
        ))
        val skillState = SkillState(skills = listOf(
            skill(100, mpCost = 10),
            skill(200, mpCost = 10),
        ))
        val picked = ManaBurnManager.pickSkillToBurn(
            mood, effectState, skillState, CharacterState(currentMp = 50)
        )
        assertEquals(200, picked?.id)
    }

    @Test fun pickSkillToBurn_effectAbsent_treatedAsZeroDuration() {
        // An effect not currently active has 0 duration — should be picked first
        val mood = Mood("test", listOf(
            MoodTrigger(effectId = 10, effectName = "E10", skillId = 100, skillName = "S100"), // absent
            MoodTrigger(effectId = 20, effectName = "E20", skillId = 200, skillName = "S200"), // 5 turns
        ))
        val effectState = EffectState(effects = listOf(
            EffectData(id = 20, name = "E20", duration = 5),
        ))
        val skillState = SkillState(skills = listOf(skill(100, mpCost = 10), skill(200, mpCost = 10)))
        val picked = ManaBurnManager.pickSkillToBurn(
            mood, effectState, skillState, CharacterState(currentMp = 50)
        )
        assertEquals(100, picked?.id)
    }

    @Test fun pickSkillToBurn_insufficientMp_skipsSkill() {
        val mood = Mood("test", listOf(
            MoodTrigger(effectId = 10, effectName = "E10", skillId = 100, skillName = "S100"),
        ))
        val skillState = SkillState(skills = listOf(skill(100, mpCost = 100)))
        assertNull(
            ManaBurnManager.pickSkillToBurn(
                mood, EffectState(), skillState, CharacterState(currentMp = 50)
            )
        )
    }

    @Test fun pickSkillToBurn_zeroMpCostSkill_skipped() {
        // Skills with zero MP cost do nothing for ManaBurn purposes
        val mood = Mood("test", listOf(
            MoodTrigger(effectId = 10, effectName = "E10", skillId = 100, skillName = "S100"),
        ))
        val skillState = SkillState(skills = listOf(skill(100, mpCost = 0)))
        assertNull(
            ManaBurnManager.pickSkillToBurn(
                mood, EffectState(), skillState, CharacterState(currentMp = 50)
            )
        )
    }

    @Test fun pickSkillToBurn_dailyLimitReached_skipsSkill() {
        val mood = Mood("test", listOf(
            MoodTrigger(effectId = 10, effectName = "E10", skillId = 100, skillName = "S100"),
        ))
        val skillState = SkillState(skills = listOf(skill(100, mpCost = 10, dailyLimit = 1, timesCast = 1)))
        assertNull(
            ManaBurnManager.pickSkillToBurn(
                mood, EffectState(), skillState, CharacterState(currentMp = 50)
            )
        )
    }

    // ── burnIfEnabled (integration) ───────────────────────────────────────────

    @Test fun burnIfEnabled_disabled_doesNotCast() {
        val cast = mutableListOf<Int>()
        val manager = ManaBurnManager(fakeCastSkillManager(cast), prefs(enabled = false))
        val mood = Mood("test", listOf(
            MoodTrigger(effectId = 10, effectName = "E10", skillId = 100, skillName = "S100")
        ))
        val skillState = SkillState(skills = listOf(skill(100, mpCost = 10)))
        runBlocking {
            manager.burnIfEnabled(mood, EffectState(), skillState, CharacterState(currentMp = 100, maxMp = 100))
        }
        assertTrue(cast.isEmpty())
    }

    @Test fun burnIfEnabled_noEligibleSkill_returnsFalse() {
        val cast = mutableListOf<Int>()
        val manager = ManaBurnManager(fakeCastSkillManager(cast), prefs(enabled = true))
        runBlocking {
            val burned = manager.burnIfEnabled(
                mood = null, effectState = EffectState(),
                skillState = SkillState(), charState = CharacterState(currentMp = 100, maxMp = 100)
            )
            assertFalse(burned)
        }
        assertTrue(cast.isEmpty())
    }

    @Test fun burnIfEnabled_castsLowestDurationSkill() {
        val cast = mutableListOf<Int>()
        val manager = ManaBurnManager(fakeCastSkillManager(cast), prefs(enabled = true, belowPct = 90))
        val mood = Mood("test", listOf(
            MoodTrigger(effectId = 10, effectName = "E10", skillId = 100, skillName = "S100"),
        ))
        val skillState = SkillState(skills = listOf(skill(100, mpCost = 10)))
        runBlocking {
            val burned = manager.burnIfEnabled(
                mood, EffectState(), skillState, CharacterState(currentMp = 95, maxMp = 100)
            )
            assertTrue(burned)
        }
        assertEquals(listOf(100), cast)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun prefs(enabled: Boolean, belowPct: Int = 90): Preferences {
        val s = MapSettings()
        s.putBoolean(Preferences.MANA_BURN_ENABLED, enabled)
        s.putInt(Preferences.MANA_BURN_BELOW_PCT, belowPct)
        return Preferences(s)
    }

    private fun skill(
        id: Int,
        mpCost: Int,
        dailyLimit: Int = 0,
        timesCast: Int = 0,
    ) = SkillData(
        id = id, name = "Skill $id",
        type = SkillType.PASSIVE,
        mpCost = mpCost, dailyLimit = dailyLimit, timesCast = timesCast,
    )

    private fun fakeCastSkillManager(cast: MutableList<Int>): SkillManager {
        val fakeClient = io.ktor.client.HttpClient(MockEngine { _ -> respond("") })
        val fakeRequest = SkillCastRequest(fakeClient)
        val fakeEventBus = GameEventBus()
        return object : SkillManager(fakeClient, fakeRequest, fakeEventBus) {
            override suspend fun cast(skill: SkillData, quantity: Int): Result<Unit> {
                cast.add(skill.id)
                return Result.success(Unit)
            }
        }
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```
.\gradlew :shared:allTests --tests "*.ManaBurnManagerTest*"
```

Expected: FAIL — `ManaBurnManager` does not exist yet.

- [ ] **Step 3: Implement ManaBurnManager**

Create `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mood/ManaBurnManager.kt`:

```kotlin
package net.sourceforge.kolmafia.mood

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState

class ManaBurnManager(
    private val skillManager: SkillManager,
    private val preferences: Preferences,
) {
    companion object {
        /**
         * Returns true when mana burn should fire: enabled preference is set AND
         * current MP is at or above [MANA_BURN_BELOW_PCT] percent of max MP.
         */
        fun shouldBurn(charState: CharacterState, prefs: Preferences): Boolean {
            if (!prefs.getBoolean(Preferences.MANA_BURN_ENABLED, false)) return false
            if (charState.maxMp <= 0) return false
            val belowPct = prefs.getInt(Preferences.MANA_BURN_BELOW_PCT, 90)
            return charState.currentMp * 100 / charState.maxMp >= belowPct
        }

        /**
         * From the active [mood]'s trigger list, returns the [SkillData] whose effect has
         * the fewest remaining turns (i.e., the effect most in need of extension), subject
         * to: the skill must cost MP > 0, must be castable at current MP, and must not be
         * at its daily limit. Returns null if no mood or no eligible skill.
         */
        fun pickSkillToBurn(
            mood: Mood?,
            effectState: EffectState,
            skillState: SkillState,
            charState: CharacterState,
        ): SkillData? {
            if (mood == null) return null
            return mood.triggers
                .sortedBy { trigger ->
                    effectState.effects.firstOrNull { it.id == trigger.effectId }?.duration ?: 0
                }
                .firstNotNullOfOrNull { trigger ->
                    skillState.skills.firstOrNull { skill ->
                        skill.id == trigger.skillId
                            && skill.mpCost > 0
                            && skill.mpCost <= charState.currentMp
                            && (skill.dailyLimit == 0 || skill.timesCast < skill.dailyLimit)
                    }
                }
        }
    }

    /**
     * If mana burn is enabled and MP is above the threshold, casts one skill
     * (the one extending the shortest-duration active effect).
     * Returns true if a skill was cast, false otherwise.
     */
    suspend fun burnIfEnabled(
        mood: Mood?,
        effectState: EffectState,
        skillState: SkillState,
        charState: CharacterState,
    ): Boolean {
        if (!shouldBurn(charState, preferences)) return false
        val skill = pickSkillToBurn(mood, effectState, skillState, charState) ?: return false
        skillManager.cast(skill)
        return true
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```
.\gradlew :shared:allTests --tests "*.ManaBurnManagerTest*"
```

Expected: all 12 tests pass.

- [ ] **Step 5: Run full suite**

```
.\gradlew :shared:allTests
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mood/ManaBurnManager.kt
git add shared/src/commonTest/kotlin/net/sourceforge/kolmafia/mood/ManaBurnManagerTest.kt
git commit -m "feat: ManaBurnManager — shouldBurn, pickSkillToBurn, burnIfEnabled"
```

---

### Task 6: Wire ManaBurn into AdventureManager

**Background:** `AdventureManager` runs a post-recovery loop per turn. ManaBurn should run
after that loop: cast one skill per iteration, fetch updated state, and repeat until MP
drops below the threshold or no eligible skill remains. Cap at 10 iterations to prevent
runaway loops.

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureManager.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt`

- [ ] **Step 1: Add ManaBurnManager parameter to AdventureManager**

In `AdventureManager.kt`, add to the constructor (after `moodManager`):

```kotlin
private val manaBurnManager: ManaBurnManager? = null,
```

Also add the import:
```kotlin
import net.sourceforge.kolmafia.mood.ManaBurnManager
```

- [ ] **Step 2: Add ManaBurn loop after recovery in runAdventures**

In `runAdventures`, immediately after the closing `}` of the recovery `if (rm != null)` block
(and before `checkQuestAdvancement`), add:

```kotlin
// ManaBurn: cast lowest-duration effect skill while MP is above burn threshold
val mbm = manaBurnManager
if (mbm != null) {
    var burnIter = 0
    while (burnIter < 10) {
        val burned = mbm.burnIfEnabled(
            mood        = moodManager?.activeMood,
            effectState = effects?.state?.value ?: EffectState(),
            skillState  = skills?.state?.value ?: SkillState(),
            charState   = character.state.value,
        )
        burnIter++
        if (!burned) break
        characterRequest.fetchCharacterState().onSuccess { character.updateFromApiResponse(it) }
    }
}
```

After this addition the relevant section of `runAdventures` should look like:

```kotlin
                    // Recovery loop: repeat until stop threshold met or no recovery available (max 10 iterations)
                    val rm = recoveryManager
                    if (rm != null) {
                        var iter = 0
                        while (iter < 10) {
                            val force = iter > 0
                            val healed = rm.recoverIfNeeded(
                                charState  = character.state.value,
                                invState   = inventory?.state?.value ?: InventoryState(),
                                skillState = skills?.state?.value ?: SkillState(),
                                force      = force,
                            )
                            iter++
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

                    // ManaBurn: cast lowest-duration effect skill while MP is above burn threshold
                    val mbm = manaBurnManager
                    if (mbm != null) {
                        var burnIter = 0
                        while (burnIter < 10) {
                            val burned = mbm.burnIfEnabled(
                                mood        = moodManager?.activeMood,
                                effectState = effects?.state?.value ?: EffectState(),
                                skillState  = skills?.state?.value ?: SkillState(),
                                charState   = character.state.value,
                            )
                            burnIter++
                            if (!burned) break
                            characterRequest.fetchCharacterState().onSuccess { character.updateFromApiResponse(it) }
                        }
                    }

                    checkQuestAdvancement(lastTurnResponseText)
```

- [ ] **Step 3: Update SharedModule**

In `SharedModule.kt`:

1. Add after `singleOf(::MoodManager)`:
```kotlin
singleOf(::ManaBurnManager)
```

2. Add `manaBurnManager = get()` to the `AdventureManager` single block, after `moodManager = get()`:
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
    )
}
```

Also add the import at the top of SharedModule.kt:
```kotlin
import net.sourceforge.kolmafia.mood.ManaBurnManager
```

- [ ] **Step 4: Run full test suite**

```
.\gradlew :shared:allTests
```

Expected: BUILD SUCCESSFUL. All existing AdventureManager tests still pass (ManaBurn
is opt-in via `MANA_BURN_ENABLED` which defaults to `false`, so no existing test behavior
changes).

- [ ] **Step 5: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureManager.kt
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt
git commit -m "feat: wire ManaBurn post-recovery loop into adventure turn"
```

---

## Self-Review

**Spec coverage check:**

| Requirement | Task |
|-------------|------|
| my_familiar() returns familiarName | T1 |
| Mood persists name to prefs | T3 |
| Mood persists trigger list (serialized) | T3 |
| Mood loads on login | T4 |
| Null mood clears prefs on save | T3 |
| ManaBurnManager.shouldBurn respects pref flag | T5 |
| ManaBurnManager.shouldBurn checks MP threshold | T5 |
| ManaBurnManager picks lowest-duration effect | T5 |
| ManaBurnManager skips zero-MP-cost skills | T5 |
| ManaBurnManager skips daily-limit-reached skills | T5 |
| ManaBurnManager wired into post-recovery loop | T6 |
| ManaBurn loop capped at 10 iterations | T6 |
| ACTIVE_MOOD_NAME / ACTIVE_MOOD_TRIGGERS / MANA_BURN_ENABLED / MANA_BURN_BELOW_PCT pref keys | T2 |
| Stale TODO comments in Preferences.kt removed | T2 |
| SharedModule DI wiring | T4 + T6 |

All requirements covered. No placeholders. No TODOs.

**Placeholder scan:** clean.

**Type consistency check:**
- `ManaBurnManager.burnIfEnabled(mood, effectState, skillState, charState)` — matches call site in AdventureManager T6 and test in T5. ✓
- `MoodManager.saveActiveMood()` / `loadActiveMood()` — called in SessionManager T4 and tested in T3. ✓
- `Preferences.ACTIVE_MOOD_NAME` etc. — defined in T2, used in T3 and T4. ✓
- `Preferences.MANA_BURN_ENABLED` etc. — defined in T2, used in T5 and T6. ✓
