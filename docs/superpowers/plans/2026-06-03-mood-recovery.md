# Mood/Recovery System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Auto-restore HP/MP when below configurable thresholds and auto-cast skills before each adventure turn to maintain mood-configured effects.

**Architecture:** `RecoveryManager` reads `RestoreDatabase` + `ItemDatabase` to find the best restore item or skill in inventory, then delegates execution to the existing `InventoryManager`/`SkillManager`. `MoodManager` holds an in-memory `Mood` (list of effect→skill triggers) and casts missing buffs before each adventure. Both are wired into `AdventureManager`'s turn loop as optional injected dependencies. No new HTTP calls — everything routes through existing managers.

**Tech Stack:** Kotlin Multiplatform, Koin DI, `data.RestoreDatabase` (already implemented + loaded), `data.ItemDatabase` (name→id lookup), `kotlinx.coroutines`, `kotlin.test`, `com.russhwolf.settings.MapSettings` for test preferences.

---

## File Map

| Status | File | Responsibility |
|--------|------|----------------|
| **Create** | `recovery/RecoveryManager.kt` | Orchestrates HP/MP recovery; preference thresholds; delegates to managers |
| **Create** | `mood/MoodTrigger.kt` | `(effectId, effectName, skillId, skillName, minimumTurns)` value object |
| **Create** | `mood/Mood.kt` | Named list of `MoodTrigger` entries |
| **Create** | `mood/MoodManager.kt` | Evaluates triggers; casts missing buffs; holds active mood |
| **Modify** | `adventure/AdventureManager.kt` | Add optional `recoveryManager` + `moodManager`; hook both into turn loop |
| **Modify** | `preferences/Preferences.kt` | Add recovery + mood preference key constants |
| **Modify** | `di/SharedModule.kt` | Register `RecoveryManager` + `MoodManager`; pass to `AdventureManager` |
| **Create** | `recovery/RecoveryManagerTest.kt` | Unit tests for threshold predicates + item/skill selection logic |
| **Create** | `mood/MoodManagerTest.kt` | Unit tests for trigger evaluation + execution |

All paths are under `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/` (source) and `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/` (tests).

---

## Key Types Reference

Before coding, internalize these types from existing files:

```kotlin
// character/CharacterState.kt
data class CharacterState(
    val currentHp: Int, val maxHp: Int,
    val currentMp: Int, val maxMp: Int,
    ...
)

// inventory/InventoryState.kt
data class InventoryState(
    val items: Map<Int, InventoryItem> = emptyMap(), ...
)
// InventoryItem(itemId: Int, name: String, count: Int, type: ItemType)

// skill/SkillState.kt — List<SkillData>
// SkillData(id: Int, name: String, type: SkillType, mpCost: Int, dailyLimit: Int, timesCast: Int)

// effect/EffectState.kt — List<EffectData>
// EffectData(id: Int, name: String, duration: Int)

// data/RestoreData.kt — hpMin/hpMax/mpMin/mpMax: Int (0 if expr like "[HP]")
// data/RestoreDatabase — hpRestores(), mpRestores(), skills()
// data/ItemDatabase — getByName(name: String): ItemData?  (ItemData.id: Int)
// data/RestoreType — ITEM, SKILL, LOC, UNKNOWN
```

---

## Task 1: Preference Key Constants

**Files:**
- Modify: `preferences/Preferences.kt`

- [ ] **Step 1: Add constants to the Keys companion object**

Open `preferences/Preferences.kt`. The existing companion is:
```kotlin
companion object Keys {
    const val LAST_USERNAME = "lastUsername"
}
```

Replace it with:
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
    const val MP_RECOVERY_STOP_PCT     = "mpRecoveryStopPct"

    // Mood
    const val AUTO_BUFF                = "autoBuff"
}
```

- [ ] **Step 2: Verify it compiles**

```
gradlew.bat :shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/preferences/Preferences.kt
git commit -m "feat: add recovery and mood preference key constants"
```

---

## Task 2: RecoveryManager — Threshold Predicates

**Files:**
- Create: `recovery/RecoveryManager.kt`
- Create: `recovery/RecoveryManagerTest.kt`

- [ ] **Step 1: Write failing tests for threshold predicates**

Create `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/recovery/RecoveryManagerTest.kt`:

```kotlin
package net.sourceforge.kolmafia.recovery

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecoveryManagerTest {

    private fun prefs(vararg pairs: Pair<String, Any>): Preferences {
        val settings = MapSettings()
        pairs.forEach { (k, v) ->
            when (v) {
                is Boolean -> settings.putBoolean(k, v)
                is Int     -> settings.putInt(k, v)
                is String  -> settings.putString(k, v)
            }
        }
        return Preferences(settings)
    }

    private fun state(hp: Int, maxHp: Int, mp: Int = 0, maxMp: Int = 0) =
        CharacterState(currentHp = hp, maxHp = maxHp, currentMp = mp, maxMp = maxMp)

    // ── HP needs-recovery ────────────────────────────────────────────────────

    @Test fun hpRecovery_disabledByPref_returnsFalse() {
        val p = prefs(Preferences.AUTO_RECOVER_HP to false)
        assertFalse(RecoveryManager.needsHpRecovery(state(10, 100), p))
    }

    @Test fun hpRecovery_aboveTarget_returnsFalse() {
        val p = prefs(Preferences.AUTO_RECOVER_HP to true, Preferences.HP_RECOVERY_TARGET_PCT to 50)
        assertFalse(RecoveryManager.needsHpRecovery(state(60, 100), p))
    }

    @Test fun hpRecovery_atTarget_returnsFalse() {
        val p = prefs(Preferences.AUTO_RECOVER_HP to true, Preferences.HP_RECOVERY_TARGET_PCT to 50)
        assertFalse(RecoveryManager.needsHpRecovery(state(50, 100), p))
    }

    @Test fun hpRecovery_belowTarget_returnsTrue() {
        val p = prefs(Preferences.AUTO_RECOVER_HP to true, Preferences.HP_RECOVERY_TARGET_PCT to 50)
        assertTrue(RecoveryManager.needsHpRecovery(state(49, 100), p))
    }

    @Test fun hpRecovery_defaultTargetIs50Pct() {
        val p = prefs(Preferences.AUTO_RECOVER_HP to true)
        assertTrue(RecoveryManager.needsHpRecovery(state(40, 100), p))
        assertFalse(RecoveryManager.needsHpRecovery(state(60, 100), p))
    }

    @Test fun hpRecovery_zeroMaxHp_doesNotCrash() {
        val p = prefs(Preferences.AUTO_RECOVER_HP to true, Preferences.HP_RECOVERY_TARGET_PCT to 50)
        assertFalse(RecoveryManager.needsHpRecovery(state(0, 0), p))
    }

    // ── MP needs-recovery ────────────────────────────────────────────────────

    @Test fun mpRecovery_disabledByPref_returnsFalse() {
        val p = prefs(Preferences.AUTO_RECOVER_MP to false)
        assertFalse(RecoveryManager.needsMpRecovery(state(0, 0, 10, 100), p))
    }

    @Test fun mpRecovery_aboveTarget_returnsFalse() {
        val p = prefs(Preferences.AUTO_RECOVER_MP to true, Preferences.MP_RECOVERY_TARGET_PCT to 30)
        assertFalse(RecoveryManager.needsMpRecovery(state(0, 0, 40, 100), p))
    }

    @Test fun mpRecovery_belowTarget_returnsTrue() {
        val p = prefs(Preferences.AUTO_RECOVER_MP to true, Preferences.MP_RECOVERY_TARGET_PCT to 30)
        assertTrue(RecoveryManager.needsMpRecovery(state(0, 0, 20, 100), p))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```
gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.recovery.RecoveryManagerTest" 2>&1 | Select-String -Pattern "FAIL|error|not found" | Select-Object -First 10
```

Expected: compilation failure — `RecoveryManager` does not exist.

- [ ] **Step 3: Create RecoveryManager with threshold predicates**

Create `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/recovery/RecoveryManager.kt`:

```kotlin
package net.sourceforge.kolmafia.recovery

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.RestoreData
import net.sourceforge.kolmafia.data.RestoreDatabase
import net.sourceforge.kolmafia.data.RestoreType
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState

class RecoveryManager(
    private val inventoryManager: InventoryManager,
    private val skillManager: SkillManager,
    private val preferences: Preferences,
) {
    companion object {
        fun needsHpRecovery(state: CharacterState, prefs: Preferences): Boolean {
            if (!prefs.getBoolean(Preferences.AUTO_RECOVER_HP, true)) return false
            val targetPct = prefs.getInt(Preferences.HP_RECOVERY_TARGET_PCT, 50)
            val maxHp = state.maxHp.coerceAtLeast(1)
            val ratioPct = state.currentHp * 100 / maxHp
            return ratioPct < targetPct
        }

        fun needsMpRecovery(state: CharacterState, prefs: Preferences): Boolean {
            if (!prefs.getBoolean(Preferences.AUTO_RECOVER_MP, false)) return false
            val targetPct = prefs.getInt(Preferences.MP_RECOVERY_TARGET_PCT, 50)
            val maxMp = state.maxMp.coerceAtLeast(1)
            val ratioPct = state.currentMp * 100 / maxMp
            return ratioPct < targetPct
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```
gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.recovery.RecoveryManagerTest"
```

Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 5: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/recovery/RecoveryManager.kt
git add shared/src/commonTest/kotlin/net/sourceforge/kolmafia/recovery/RecoveryManagerTest.kt
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/preferences/Preferences.kt
git commit -m "feat: RecoveryManager threshold predicates + preference constants"
```

---

## Task 3: RecoveryManager — Item and Skill Selection Logic

**Files:**
- Modify: `recovery/RecoveryManager.kt`
- Modify: `recovery/RecoveryManagerTest.kt`

The selection logic must be testable without loading compose resources. Extract it as `internal` companion functions that accept explicit `List<RestoreData>` and `Map<Int, InventoryItem>` inputs.

- [ ] **Step 1: Add selection tests**

Append to `RecoveryManagerTest.kt`:

```kotlin
    // ── HP item selection ────────────────────────────────────────────────────

    @Test fun pickHpItem_noItems_returnsNull() {
        val result = RecoveryManager.pickHpItem(
            restores = listOf(restore("aspirin", hpMax = 101)),
            items = emptyMap(),
            nameToId = { 1 },
        )
        assertNull(result)
    }

    @Test fun pickHpItem_itemInInventory_returnsIt() {
        val aspirin = invItem(id = 99, count = 5)
        val result = RecoveryManager.pickHpItem(
            restores = listOf(restore("aspirin", hpMax = 101)),
            items = mapOf(99 to aspirin),
            nameToId = { name -> if (name == "aspirin") 99 else null },
        )
        assertNotNull(result)
        assertEquals(99, result.itemId)
    }

    @Test fun pickHpItem_noCount_returnsNull() {
        val result = RecoveryManager.pickHpItem(
            restores = listOf(restore("aspirin", hpMax = 101)),
            items = mapOf(99 to invItem(id = 99, count = 0)),
            nameToId = { 99 },
        )
        assertNull(result)
    }

    @Test fun pickHpItem_prefersFullRestoreOverPartial() {
        val partial = invItem(id = 1, count = 3)
        val full = invItem(id = 2, count = 1)
        // "[HP]" expression → hpMax == 0 via toIntOrNull; flag via hpMaxExpr
        val result = RecoveryManager.pickHpItem(
            restores = listOf(
                restore("partial", hpMax = 101),
                restore("full-restore", hpMaxExpr = "[HP]"),
            ),
            items = mapOf(1 to partial, 2 to full),
            nameToId = { name -> if (name == "partial") 1 else 2 },
        )
        assertNotNull(result)
        assertEquals(2, result.itemId)
    }

    // ── MP item selection ────────────────────────────────────────────────────

    @Test fun pickMpItem_itemPresent_returnsIt() {
        val soda = invItem(id = 50, count = 2)
        val result = RecoveryManager.pickMpItem(
            restores = listOf(mpRestore("blue pixel potion", mpMax = 80)),
            items = mapOf(50 to soda),
            nameToId = { 50 },
        )
        assertNotNull(result)
        assertEquals(50, result.itemId)
    }

    // ── HP skill selection ───────────────────────────────────────────────────

    @Test fun pickHpSkill_sufficientMp_returnsSkill() {
        val result = RecoveryManager.pickHpSkill(
            restores = listOf(skillRestore("Cannelloni Cocoon", hpMaxExpr = "[HP]")),
            skills = listOf(skill(id = 3012, name = "Cannelloni Cocoon", mpCost = 40)),
            currentMp = 50,
        )
        assertNotNull(result)
        assertEquals(3012, result.id)
    }

    @Test fun pickHpSkill_insufficientMp_returnsNull() {
        val result = RecoveryManager.pickHpSkill(
            restores = listOf(skillRestore("Cannelloni Cocoon", hpMaxExpr = "[HP]")),
            skills = listOf(skill(id = 3012, name = "Cannelloni Cocoon", mpCost = 40)),
            currentMp = 10,
        )
        assertNull(result)
    }

    @Test fun pickHpSkill_dailyLimitReached_returnsNull() {
        val result = RecoveryManager.pickHpSkill(
            restores = listOf(skillRestore("Cannelloni Cocoon", hpMaxExpr = "[HP]")),
            skills = listOf(skill(id = 3012, name = "Cannelloni Cocoon", mpCost = 40, dailyLimit = 1, timesCast = 1)),
            currentMp = 100,
        )
        assertNull(result)
    }
```

Also add helpers at the bottom of the test class (before the closing `}`):

```kotlin
    // ── Test helpers ─────────────────────────────────────────────────────────

    private fun restore(
        name: String,
        hpMin: Int = 0,
        hpMax: Int = 0,
        hpMinExpr: String = hpMin.toString(),
        hpMaxExpr: String = hpMax.toString(),
    ) = net.sourceforge.kolmafia.data.RestoreData(
        name = name,
        type = net.sourceforge.kolmafia.data.RestoreType.ITEM,
        hpMinExpr = hpMinExpr,
        hpMaxExpr = hpMaxExpr,
        mpMinExpr = "0",
        mpMaxExpr = "0",
        advCost = 0,
        usesLeftExpr = "",
        notes = "",
    )

    private fun mpRestore(name: String, mpMin: Int = 0, mpMax: Int = 0) =
        net.sourceforge.kolmafia.data.RestoreData(
            name = name,
            type = net.sourceforge.kolmafia.data.RestoreType.ITEM,
            hpMinExpr = "0", hpMaxExpr = "0",
            mpMinExpr = mpMin.toString(), mpMaxExpr = mpMax.toString(),
            advCost = 0, usesLeftExpr = "", notes = "",
        )

    private fun skillRestore(
        name: String,
        hpMin: Int = 0,
        hpMaxExpr: String = "0",
    ) = net.sourceforge.kolmafia.data.RestoreData(
        name = name,
        type = net.sourceforge.kolmafia.data.RestoreType.SKILL,
        hpMinExpr = hpMin.toString(),
        hpMaxExpr = hpMaxExpr,
        mpMinExpr = "0", mpMaxExpr = "0",
        advCost = 0, usesLeftExpr = "", notes = "",
    )

    private fun invItem(id: Int, count: Int) =
        net.sourceforge.kolmafia.inventory.InventoryItem(
            itemId = id, name = "Item #$id", count = count,
            type = net.sourceforge.kolmafia.inventory.ItemType.OTHER,
        )

    private fun skill(
        id: Int,
        name: String,
        mpCost: Int = 0,
        dailyLimit: Int = 0,
        timesCast: Int = 0,
    ) = net.sourceforge.kolmafia.skill.SkillData(
        id = id, name = name,
        type = net.sourceforge.kolmafia.skill.SkillType.PASSIVE,
        mpCost = mpCost, dailyLimit = dailyLimit, timesCast = timesCast,
    )
```

Also add `import kotlin.test.assertNotNull` and `import kotlin.test.assertNull` to imports.

- [ ] **Step 2: Run tests to verify they fail**

```
gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.recovery.RecoveryManagerTest"
```

Expected: compile failure — `pickHpItem`, `pickMpItem`, `pickHpSkill` not defined.

- [ ] **Step 3: Implement selection functions in RecoveryManager companion**

Add to the `companion object` inside `RecoveryManager`:

```kotlin
        internal fun isFullRestore(restoreData: RestoreData): Boolean =
            restoreData.hpMaxExpr.contains("[") || restoreData.mpMaxExpr.contains("[")

        internal fun pickHpItem(
            restores: List<RestoreData>,
            items: Map<Int, InventoryItem>,
            nameToId: (String) -> Int?,
        ): InventoryItem? = restores
            .filter { it.type == RestoreType.ITEM && it.restoresHp }
            .sortedByDescending { if (isFullRestore(it)) Int.MAX_VALUE else it.hpMax }
            .firstNotNullOfOrNull { restore ->
                val id = nameToId(restore.name) ?: return@firstNotNullOfOrNull null
                items[id]?.takeIf { it.count > 0 }
            }

        internal fun pickMpItem(
            restores: List<RestoreData>,
            items: Map<Int, InventoryItem>,
            nameToId: (String) -> Int?,
        ): InventoryItem? = restores
            .filter { it.type == RestoreType.ITEM && it.restoresMp }
            .sortedByDescending { if (isFullRestore(it)) Int.MAX_VALUE else it.mpMax }
            .firstNotNullOfOrNull { restore ->
                val id = nameToId(restore.name) ?: return@firstNotNullOfOrNull null
                items[id]?.takeIf { it.count > 0 }
            }

        internal fun pickHpSkill(
            restores: List<RestoreData>,
            skills: List<net.sourceforge.kolmafia.skill.SkillData>,
            currentMp: Int,
        ): net.sourceforge.kolmafia.skill.SkillData? = restores
            .filter { it.type == RestoreType.SKILL && it.restoresHp }
            .sortedByDescending { if (isFullRestore(it)) Int.MAX_VALUE else it.hpMax }
            .firstNotNullOfOrNull { restore ->
                skills.firstOrNull { skill ->
                    skill.name.equals(restore.name, ignoreCase = true)
                        && skill.mpCost <= currentMp
                        && (skill.dailyLimit == 0 || skill.timesCast < skill.dailyLimit)
                }
            }

        internal fun pickMpSkill(
            restores: List<RestoreData>,
            skills: List<net.sourceforge.kolmafia.skill.SkillData>,
            currentMp: Int,
        ): net.sourceforge.kolmafia.skill.SkillData? = restores
            .filter { it.type == RestoreType.SKILL && it.restoresMp }
            .sortedByDescending { if (isFullRestore(it)) Int.MAX_VALUE else it.mpMax }
            .firstNotNullOfOrNull { restore ->
                skills.firstOrNull { skill ->
                    skill.name.equals(restore.name, ignoreCase = true)
                        && skill.mpCost <= currentMp
                        && (skill.dailyLimit == 0 || skill.timesCast < skill.dailyLimit)
                }
            }
```

Add required imports to `RecoveryManager.kt`:
```kotlin
import net.sourceforge.kolmafia.data.RestoreData
import net.sourceforge.kolmafia.data.RestoreType
```

- [ ] **Step 4: Run tests to verify they pass**

```
gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.recovery.RecoveryManagerTest"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/recovery/RecoveryManager.kt
git add shared/src/commonTest/kotlin/net/sourceforge/kolmafia/recovery/RecoveryManagerTest.kt
git commit -m "feat: RecoveryManager item/skill selection logic with tests"
```

---

## Task 4: RecoveryManager — Execute Recovery

**Files:**
- Modify: `recovery/RecoveryManager.kt`

Wire the selection logic to actual `InventoryManager.useItem()` and `SkillManager.cast()` calls.

- [ ] **Step 1: Add recoverIfNeeded to RecoveryManager (instance method)**

Add after the `companion object` closing brace in `RecoveryManager.kt`:

```kotlin
    suspend fun recoverIfNeeded(
        charState: CharacterState,
        invState: InventoryState,
        skillState: SkillState,
    ): Boolean {
        var recovered = false
        if (needsHpRecovery(charState, preferences)) {
            recovered = recoverHp(charState, invState, skillState) || recovered
        }
        if (needsMpRecovery(charState, preferences)) {
            recovered = recoverMp(charState, invState, skillState) || recovered
        }
        return recovered
    }

    private suspend fun recoverHp(
        charState: CharacterState,
        invState: InventoryState,
        skillState: SkillState,
    ): Boolean {
        val nameToId: (String) -> Int? = { name -> ItemDatabase.getByName(name)?.id }
        val item = pickHpItem(RestoreDatabase.hpRestores(), invState.items, nameToId)
        if (item != null) {
            inventoryManager.useItem(item)
            return true
        }
        val skill = pickHpSkill(RestoreDatabase.hpRestores(), skillState.skills, charState.currentMp)
        if (skill != null) {
            skillManager.cast(skill)
            return true
        }
        return false
    }

    private suspend fun recoverMp(
        charState: CharacterState,
        invState: InventoryState,
        skillState: SkillState,
    ): Boolean {
        val nameToId: (String) -> Int? = { name -> ItemDatabase.getByName(name)?.id }
        val item = pickMpItem(RestoreDatabase.mpRestores(), invState.items, nameToId)
        if (item != null) {
            inventoryManager.useItem(item)
            return true
        }
        val skill = pickMpSkill(RestoreDatabase.mpRestores(), skillState.skills, charState.currentMp)
        if (skill != null) {
            skillManager.cast(skill)
            return true
        }
        return false
    }
```

Note: `recoverHp` and `recoverMp` each make one use/cast attempt. The adventure loop calls `recoverIfNeeded` between turns, which is sufficient — multi-item recovery loops are YAGNI.

- [ ] **Step 2: Compile check**

```
gradlew.bat :shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/recovery/RecoveryManager.kt
git commit -m "feat: RecoveryManager execute recovery (item then skill fallback)"
```

---

## Task 5: MoodTrigger and Mood Data Classes

**Files:**
- Create: `mood/MoodTrigger.kt`
- Create: `mood/Mood.kt`

Pure data classes — no tests needed.

- [ ] **Step 1: Create MoodTrigger**

Create `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mood/MoodTrigger.kt`:

```kotlin
package net.sourceforge.kolmafia.mood

/**
 * Instructs the mood system to cast [skillId] whenever [effectId] has fewer
 * than [minimumTurns] remaining.  [effectName] and [skillName] are display-only
 * labels used in UI and logging.
 */
data class MoodTrigger(
    val effectId: Int,
    val effectName: String,
    val skillId: Int,
    val skillName: String,
    val minimumTurns: Int = 1,
)
```

- [ ] **Step 2: Create Mood**

Create `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mood/Mood.kt`:

```kotlin
package net.sourceforge.kolmafia.mood

/** A named set of [MoodTrigger] entries that the mood system evaluates each turn. */
data class Mood(
    val name: String,
    val triggers: List<MoodTrigger>,
) {
    companion object {
        val EMPTY = Mood(name = "default", triggers = emptyList())
    }
}
```

- [ ] **Step 3: Compile check**

```
gradlew.bat :shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mood/MoodTrigger.kt
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mood/Mood.kt
git commit -m "feat: MoodTrigger and Mood data classes"
```

---

## Task 6: MoodManager — Missing Trigger Evaluation

**Files:**
- Create: `mood/MoodManager.kt`
- Create: `mood/MoodManagerTest.kt`

- [ ] **Step 1: Write failing tests for trigger evaluation**

Create `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/mood/MoodManagerTest.kt`:

```kotlin
package net.sourceforge.kolmafia.mood

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MoodManagerTest {

    private fun prefs(autoBuffEnabled: Boolean = true): Preferences {
        val s = MapSettings()
        s.putBoolean(Preferences.AUTO_BUFF, autoBuffEnabled)
        return Preferences(s)
    }

    private fun trigger(effectId: Int, skillId: Int, minTurns: Int = 1) =
        MoodTrigger(effectId, "Effect $effectId", skillId, "Skill $skillId", minTurns)

    private fun effect(id: Int, duration: Int) =
        EffectData(id = id, name = "Effect $id", duration = duration)

    private fun effectState(vararg effects: EffectData) =
        EffectState(effects = effects.toList())

    // ── missingTriggers ──────────────────────────────────────────────────────

    @Test fun missingTriggers_effectPresent_returnsEmpty() {
        val mood = Mood("test", listOf(trigger(effectId = 10, skillId = 200, minTurns = 1)))
        val effects = effectState(effect(10, duration = 3))
        val missing = MoodManager.missingTriggers(mood, effects)
        assertTrue(missing.isEmpty())
    }

    @Test fun missingTriggers_effectAbsent_returnsTrigger() {
        val t = trigger(effectId = 10, skillId = 200)
        val mood = Mood("test", listOf(t))
        val missing = MoodManager.missingTriggers(mood, effectState())
        assertEquals(listOf(t), missing)
    }

    @Test fun missingTriggers_effectBelowMinTurns_returnsTrigger() {
        val t = trigger(effectId = 10, skillId = 200, minTurns = 5)
        val mood = Mood("test", listOf(t))
        val effects = effectState(effect(10, duration = 3))
        val missing = MoodManager.missingTriggers(mood, effects)
        assertEquals(listOf(t), missing)
    }

    @Test fun missingTriggers_exactlyAtMinTurns_returnsEmpty() {
        val t = trigger(effectId = 10, skillId = 200, minTurns = 3)
        val mood = Mood("test", listOf(t))
        val effects = effectState(effect(10, duration = 3))
        assertTrue(MoodManager.missingTriggers(mood, effects).isEmpty())
    }

    @Test fun missingTriggers_multipleTriggers_returnsMissingOnly() {
        val t1 = trigger(effectId = 10, skillId = 200)
        val t2 = trigger(effectId = 20, skillId = 300)
        val mood = Mood("test", listOf(t1, t2))
        val effects = effectState(effect(10, duration = 5))  // only t1 present
        assertEquals(listOf(t2), MoodManager.missingTriggers(mood, effects))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```
gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.MoodManagerTest"
```

Expected: compile failure — `MoodManager` does not exist.

- [ ] **Step 3: Implement MoodManager with missingTriggers**

Create `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mood/MoodManager.kt`:

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
}
```

- [ ] **Step 4: Run tests to verify they pass**

```
gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.MoodManagerTest"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mood/MoodManager.kt
git add shared/src/commonTest/kotlin/net/sourceforge/kolmafia/mood/MoodManagerTest.kt
git commit -m "feat: MoodManager with missingTriggers evaluation"
```

---

## Task 7: MoodManager — Execute Mood (Cast Missing Buffs)

**Files:**
- Modify: `mood/MoodManager.kt`
- Modify: `mood/MoodManagerTest.kt`

- [ ] **Step 1: Add execution tests**

Append to `MoodManagerTest.kt` (inside the class, before closing `}`):

```kotlin
    // ── executeActiveMood (captures cast calls) ──────────────────────────────

    @Test fun executeActiveMood_noActiveMood_doesNothing() {
        val cast = mutableListOf<Int>()
        val manager = MoodManager(fakeCastSkillManager(cast), prefs())
        manager.activeMood = null
        // No exception, cast list stays empty
        runBlocking {
            manager.executeActiveMood(effectState(), SkillState(), CharacterState())
        }
        assertTrue(cast.isEmpty())
    }

    @Test fun executeActiveMood_autoBuff_disabled_doesNothing() {
        val cast = mutableListOf<Int>()
        val manager = MoodManager(fakeCastSkillManager(cast), prefs(autoBuffEnabled = false))
        manager.activeMood = Mood("test", listOf(trigger(effectId = 10, skillId = 200)))
        runBlocking {
            manager.executeActiveMood(effectState(), SkillState(), CharacterState())
        }
        assertTrue(cast.isEmpty())
    }

    @Test fun executeActiveMood_missingEffect_castsSkill() {
        val cast = mutableListOf<Int>()
        val manager = MoodManager(fakeCastSkillManager(cast), prefs())
        manager.activeMood = Mood("test", listOf(trigger(effectId = 10, skillId = 200)))
        val skills = SkillState(skills = listOf(skillData(id = 200, mpCost = 10)))
        runBlocking {
            manager.executeActiveMood(effectState(), skills, CharacterState(currentMp = 50, maxMp = 100))
        }
        assertEquals(listOf(200), cast)
    }

    @Test fun executeActiveMood_effectPresent_doesNotCast() {
        val cast = mutableListOf<Int>()
        val manager = MoodManager(fakeCastSkillManager(cast), prefs())
        manager.activeMood = Mood("test", listOf(trigger(effectId = 10, skillId = 200)))
        val skills = SkillState(skills = listOf(skillData(id = 200, mpCost = 10)))
        runBlocking {
            manager.executeActiveMood(effectState(effect(10, 3)), skills, CharacterState(currentMp = 50, maxMp = 100))
        }
        assertTrue(cast.isEmpty())
    }

    @Test fun executeActiveMood_insufficientMp_skipsSkill() {
        val cast = mutableListOf<Int>()
        val manager = MoodManager(fakeCastSkillManager(cast), prefs())
        manager.activeMood = Mood("test", listOf(trigger(effectId = 10, skillId = 200)))
        val skills = SkillState(skills = listOf(skillData(id = 200, mpCost = 50)))
        runBlocking {
            manager.executeActiveMood(effectState(), skills, CharacterState(currentMp = 10, maxMp = 100))
        }
        assertTrue(cast.isEmpty())
    }

    @Test fun executeActiveMood_skillNotKnown_skipsIt() {
        val cast = mutableListOf<Int>()
        val manager = MoodManager(fakeCastSkillManager(cast), prefs())
        manager.activeMood = Mood("test", listOf(trigger(effectId = 10, skillId = 200)))
        runBlocking {
            manager.executeActiveMood(effectState(), SkillState(skills = emptyList()), CharacterState(currentMp = 50, maxMp = 100))
        }
        assertTrue(cast.isEmpty())
    }

    @Test fun executeActiveMood_dailyLimitReached_skipsSkill() {
        val cast = mutableListOf<Int>()
        val manager = MoodManager(fakeCastSkillManager(cast), prefs())
        manager.activeMood = Mood("test", listOf(trigger(effectId = 10, skillId = 200)))
        val skills = SkillState(skills = listOf(skillData(id = 200, mpCost = 10, dailyLimit = 1, timesCast = 1)))
        runBlocking {
            manager.executeActiveMood(effectState(), skills, CharacterState(currentMp = 50, maxMp = 100))
        }
        assertTrue(cast.isEmpty())
    }
```

Add helpers at the bottom of the class (before closing `}`):

```kotlin
    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun skillData(
        id: Int,
        mpCost: Int = 0,
        dailyLimit: Int = 0,
        timesCast: Int = 0,
    ) = net.sourceforge.kolmafia.skill.SkillData(
        id = id, name = "Skill $id",
        type = net.sourceforge.kolmafia.skill.SkillType.PASSIVE,
        mpCost = mpCost, dailyLimit = dailyLimit, timesCast = timesCast,
    )

    /** Returns a fake SkillManager that records which skill IDs were cast. */
    private fun fakeCastSkillManager(cast: MutableList<Int>): SkillManager {
        val fakeClient = io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine { _ ->
            io.ktor.client.engine.mock.respond("")
        })
        val fakeRequest = net.sourceforge.kolmafia.skill.SkillCastRequest(fakeClient)
        val fakeEventBus = net.sourceforge.kolmafia.event.GameEventBus()
        return object : SkillManager(fakeClient, fakeRequest, fakeEventBus) {
            override suspend fun cast(
                skill: net.sourceforge.kolmafia.skill.SkillData,
                quantity: Int,
            ): Result<Unit> {
                cast.add(skill.id)
                return Result.success(Unit)
            }
        }
    }
```

Add imports to `MoodManagerTest.kt`:
```kotlin
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.skill.SkillState
```

- [ ] **Step 2: Run tests to verify they fail**

```
gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.MoodManagerTest"
```

Expected: compile failure — `executeActiveMood` not defined; `SkillManager.cast` not open.

- [ ] **Step 3: Make SkillManager.cast open**

In `skill/SkillManager.kt`, add `open` to the cast declaration:
```kotlin
open suspend fun cast(skill: SkillData, quantity: Int = 1): Result<Unit> {
```

- [ ] **Step 4: Add executeActiveMood to MoodManager**

Add the following method to `MoodManager.kt` (instance method, after companion object):

```kotlin
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
```

Add missing imports to `MoodManager.kt`:
```kotlin
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.skill.SkillState
```

- [ ] **Step 5: Run tests to verify they pass**

```
gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.MoodManagerTest"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/mood/MoodManager.kt
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/skill/SkillManager.kt
git add shared/src/commonTest/kotlin/net/sourceforge/kolmafia/mood/MoodManagerTest.kt
git commit -m "feat: MoodManager.executeActiveMood — cast missing buffs"
```

---

## Task 8: AdventureManager — Wire Recovery and Mood Into Turn Loop

**Files:**
- Modify: `adventure/AdventureManager.kt`

The adventure loop already lives in `runAdventures()`. Add two optional constructor parameters and hook them into the loop body.

- [ ] **Step 1: Add optional parameters to constructor**

In `AdventureManager.kt`, find the constructor signature. After `skills: SkillManager? = null,`, add:

```kotlin
    private val recoveryManager: RecoveryManager? = null,
    private val moodManager: MoodManager? = null,
```

Add imports:
```kotlin
import net.sourceforge.kolmafia.mood.MoodManager
import net.sourceforge.kolmafia.recovery.RecoveryManager
```

- [ ] **Step 2: Add recovery call after each turn**

Inside `runAdventures()`, find the block that runs after `doOneTurn(location)`:

```kotlin
val result = doOneTurn(location) ?: return@launch
characterRequest.fetchCharacterState().onSuccess { character.updateFromApiResponse(it) }
```

After the `characterRequest` line (so we have fresh state), add:

```kotlin
                    // Recover HP/MP between turns using fresh character state
                    recoveryManager?.recoverIfNeeded(
                        charState = character.state.value,
                        invState  = inventory?.state?.value ?: InventoryState(),
                        skillState = skills?.state?.value ?: SkillState(),
                    )
```

- [ ] **Step 3: Add mood call before each turn**

In `runAdventures()`, immediately **before** the `doOneTurn(location)` call, add:

```kotlin
                    // Re-buff before this adventure turn
                    moodManager?.executeActiveMood(
                        effectState = effects?.state?.value ?: EffectState(),
                        skillState  = skills?.state?.value ?: SkillState(),
                        charState   = character.state.value,
                    )
```

Add any missing imports — `EffectState`, `SkillState`, `InventoryState` should already be imported, but verify.

- [ ] **Step 4: Compile check**

```
gradlew.bat :shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Run full test suite to catch regressions**

```
gradlew.bat :shared:jvmTest
```

Expected: `BUILD SUCCESSFUL` — all pre-existing tests still pass.

- [ ] **Step 6: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureManager.kt
git commit -m "feat: wire RecoveryManager and MoodManager into AdventureManager turn loop"
```

---

## Task 9: DI Wiring — SharedModule and AdventureManager Constructor

**Files:**
- Modify: `di/SharedModule.kt`

- [ ] **Step 1: Register RecoveryManager and MoodManager as singletons**

In `SharedModule.kt`, after `singleOf(::SkillManager)`, add:

```kotlin
    singleOf(::RecoveryManager)
    singleOf(::MoodManager)
```

Add imports:
```kotlin
import net.sourceforge.kolmafia.mood.MoodManager
import net.sourceforge.kolmafia.recovery.RecoveryManager
```

- [ ] **Step 2: Pass them to AdventureManager**

Find the `single { AdventureManager(...) }` block. After `skills = get(),`, add:

```kotlin
            recoveryManager = get(),
            moodManager     = get(),
```

- [ ] **Step 3: Compile check**

```
gradlew.bat :shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Run full test suite**

```
gradlew.bat :shared:jvmTest
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt
git commit -m "feat: register RecoveryManager and MoodManager in Koin shared module"
```

---

## Self-Review Checklist

Run this after Task 9 completes. These are checks, not new tasks.

**Spec coverage:**

| Requirement | Task | Status |
|-------------|------|--------|
| Auto-restore HP below threshold | Tasks 2–4 | ✅ |
| Auto-restore MP below threshold | Task 4 | ✅ |
| Configurable target/stop percentages | Task 1 | ✅ |
| Item-first, skill-fallback recovery | Task 4 | ✅ |
| Full-restore expressions prioritized | Task 3 | ✅ |
| Daily limit respected for heal skills | Task 3 | ✅ |
| MP cost respected for heal skills | Task 3 | ✅ |
| Mood trigger data model | Task 5 | ✅ |
| Missing trigger detection | Task 6 | ✅ |
| Cast missing buffs before adventure | Tasks 7–8 | ✅ |
| MP/daily limit respected for mood | Task 7 | ✅ |
| Recovery disabled by preference | Task 2 | ✅ |
| Mood disabled by preference | Task 7 | ✅ |
| DI wiring | Task 9 | ✅ |

**Known TODOs (intentionally deferred):**
- Persistent mood storage (preferences serialization) — in-memory `activeMood` is sufficient for initial automation; add in follow-up
- Multi-use recovery loop (use item, re-check, use again) — YAGNI; single recovery per turn is the common case
- Location-based restores (`RestoreType.LOC`) — `recoverHp`/`recoverMp` skip these; the logic is the same but needs adventure requests
- `AUTO_RECOVER_MP` defaults to `false` — MP recovery is less critical than HP; flip when the system is validated in use
