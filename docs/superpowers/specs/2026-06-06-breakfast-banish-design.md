# Phase 9: Daily Login Automation + BanishManager Completion

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement `BreakfastManager` (six universal daily-reset actions fired on login) and complete `BanishManager` (correct banisher detection, rollover gating, zone pre-flight check, ASH functions).

**Architecture:** Two subsystems wired into `SessionManager`'s login flow. `BreakfastManager` orchestrates best-effort HTTP actions guarded by per-action preferences and done-today sentinel prefs. `BanishManager` completion adds a `banisher: Banisher` field to `AdventureResult.Combat`, per-banisher text patterns in `AdventureParser`, daycount-gated rollover clearing in `SessionManager`, zone pre-flight banish checking in `AdventureManager`, and `is_banished()`/`banishers_used()` ASH functions in `GameRuntimeLibrary`.

**Tech stack:** Kotlin Multiplatform, Ktor HTTP client, Koin DI, StateFlow, MockEngine tests, MapSettings for preference tests.

---

## Architecture Overview

```
SessionManager.login()
  ├── existing: loadActiveMood(), loadMoodLibrary(), banishManager.load()
  ├── NEW: daycount changed? → clearExpiredAndRollover() + clearBreakfastPrefs()
  └── NEW: breakfastManager.runBreakfast(charState, inventoryState)

AdventureManager.runAdventures()
  └── NEW: zone pre-flight → if all monsters banished → StopReason.AllMonstersBanished

AdventureParser.parseFightResult()
  └── existing BANISH_PATTERN hit → NEW BANISHER_PATTERNS scan → Combat(banisher=X)
```

---

## Section 1: Preference Key Constants

All new pref keys added to `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/preferences/Preferences.kt`.

### Breakfast guard prefs (match desktop names exactly)
These are *user-controlled* settings (not sentinel flags). Default: `"any"` for garden, `true` for lounge/rumpus/manual.

```kotlin
const val HARVEST_GARDEN_SOFTCORE   = "harvestGardenSoftcore"   // "none"|"any"|crop name; default "any"
const val HARVEST_GARDEN_HARDCORE   = "harvestGardenHardcore"   // same; default "none"
const val VISIT_RUMPUS_SOFTCORE     = "visitRumpusSoftcore"     // boolean; default true
const val VISIT_RUMPUS_HARDCORE     = "visitRumpusHardcore"     // boolean; default true
const val VISIT_LOUNGE_SOFTCORE     = "visitLoungeSoftcore"     // boolean; default true
const val VISIT_LOUNGE_HARDCORE     = "visitLoungeHardcore"     // boolean; default true
const val READ_MANUAL_SOFTCORE      = "readManualSoftcore"      // boolean; default true
const val READ_MANUAL_HARDCORE      = "readManualHardcore"      // boolean; default true
```

### Breakfast sentinel prefs (done-today flags, cleared at rollover)
```kotlin
const val BREAKFAST_COMPLETED       = "breakfastCompleted"      // boolean; false after rollover
const val GARDEN_HARVESTED          = "_gardenHarvested"        // boolean
const val BREAKFAST_RUMPUS          = "_breakfastRumpus"        // boolean
const val GUILD_MANUAL_USED         = "_guildManualUsed"        // boolean
const val DELUXE_KLAW_SUMMONS       = "_deluxeKlawSummons"      // int; 0–3
const val LOOKING_GLASS             = "_lookingGlass"           // boolean
const val FIREWORKS_SHOP            = "_fireworksShop"          // boolean
const val POOL_GAME_RESULT          = "_poolGameResult"         // string; "" = not done
```

### Daycount / rollover tracking
```kotlin
const val LAST_DAYCOUNT             = "lastBreakfastDaycount"   // int; -1 = never stored
```

---

## Section 2: HTTP Request Classes

### 2a: CampgroundRequest

**File:** `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/CampgroundRequest.kt`

```kotlin
class CampgroundRequest(private val client: HttpClient) {
    /** Harvests the garden. Returns Result<Unit>; success does not guarantee items were present. */
    open suspend fun harvestGarden(): Result<Unit> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/campground.php",
            formParameters = parameters {
                append("action", "garden")
            }
        )
        if (!response.status.isSuccess()) Result.failure(Exception("HTTP ${response.status.value}"))
        else Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
}
```

**Test file:** `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/CampgroundRequestTest.kt`
- `harvestGarden_success_returnsSuccess`
- `harvestGarden_networkError_returnsFailure`
- `harvestGarden_serverError_returnsFailure`
- `harvestGarden_sendsCorrectAction`

### 2b: ClanRumpusRequest

**File:** `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/ClanRumpusRequest.kt`

```kotlin
class ClanRumpusRequest(private val client: HttpClient) {
    open suspend fun visit(): Result<Unit> = try {
        val response = client.get("$KOL_BASE_URL/clan_basement.php")
        if (!response.status.isSuccess()) Result.failure(Exception("HTTP ${response.status.value}"))
        else Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
}
```

**Test file:** `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/ClanRumpusRequestTest.kt`
- `visit_success_returnsSuccess`
- `visit_networkError_returnsFailure`

### 2c: ClanLoungeRequest

**File:** `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/ClanLoungeRequest.kt`

```kotlin
class ClanLoungeRequest(private val client: HttpClient) {

    /** Use the Deluxe Klaw machine once. Returns HTML body (caller checks sentinel). */
    open suspend fun useKlaw(): Result<String> = postAction("klaw")

    /** Visit the looking glass (free buff). */
    open suspend fun useLookingGlass(): Result<Unit> = postAction("lookingglass").map {}

    /** Visit the fireworks shop (free buff). */
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
        if (!response.status.isSuccess()) Result.failure(Exception("HTTP ${response.status.value}"))
        else Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    private suspend fun postAction(action: String): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/clan_viplounge.php",
            formParameters = parameters { append("action", action) }
        )
        if (!response.status.isSuccess()) Result.failure(Exception("HTTP ${response.status.value}"))
        else Result.success(response.bodyAsText())
    } catch (e: Exception) { Result.failure(e) }
}
```

**Test file:** `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/ClanLoungeRequestTest.kt`
- `useKlaw_success_returnsBody`
- `useKlaw_serverError_returnsFailure`
- `useLookingGlass_sendsCorrectAction`
- `playPoolGame_sendsCorrectFormParams`

---

## Section 3: BreakfastManager

**File:** `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/BreakfastManager.kt`

```kotlin
class BreakfastManager(
    private val campgroundRequest: CampgroundRequest,
    private val clanRumpusRequest: ClanRumpusRequest,
    private val clanLoungeRequest: ClanLoungeRequest,
    private val preferences: Preferences,
    private val inventoryManager: InventoryManager? = null,
) {
    companion object {
        const val POCKET_WISH_ITEM_ID = 8765
        val MUS_MANUAL_ID = 11
        val MYS_MANUAL_ID = 172
        val MOX_MANUAL_ID = 173
    }

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
        val crop = preferences.getString(Preferences.HARVEST_GARDEN_SOFTCORE.dropLast(8) + suffix)
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
        val hasKey = inventoryState.items.values.any { it.itemId == VIP_LOUNGE_KEY_ID }
        if (!hasKey) return

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

    private suspend fun readGuildManual(suffix: String, charState: CharacterState, inventoryState: InventoryState) {
        if (!preferences.getBoolean("readManual$suffix", true)) return
        if (preferences.getBoolean(Preferences.GUILD_MANUAL_USED, false)) return
        // Class-specific manual item ID
        val manualId = when {
            charState.characterClass.isMuscle()       -> MUS_MANUAL_ID
            charState.characterClass.isMysticality()  -> MYS_MANUAL_ID
            else                                       -> MOX_MANUAL_ID
        }
        val hasManual = inventoryState.items.values.any { it.itemId == manualId }
        if (!hasManual) return
        // Use item via InventoryManager (already in DI)
        inventoryManager?.use(manualId)
        preferences.setBoolean(Preferences.GUILD_MANUAL_USED, true)
    }

    private suspend fun makePocketWishes(inventoryState: InventoryState) {
        val hasWish = inventoryState.items.values.any { it.itemId == POCKET_WISH_ITEM_ID }
        if (!hasWish) return
        inventoryManager?.use(POCKET_WISH_ITEM_ID)
    }
}
```

**Note:** `CharacterClass.isMuscle()`, `isMysticality()` — `CharacterClass` enum already exists; add extension helpers if not present.

**Note:** `InventoryManager.use(itemId: Int)` — verify this method exists or add it as a thin wrapper around the existing `use` mechanism.

**Note:** `VIP_LOUNGE_KEY_ID = 5479` (desktop `ItemPool.VIP_LOUNGE_KEY`).

**Test file:** `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/session/BreakfastManagerTest.kt`

Key tests:
- `runBreakfast_alreadyCompleted_doesNothing`
- `runBreakfast_harvestGarden_skippedWhenPrefNone`
- `runBreakfast_harvestGarden_callsRequestAndSentsSentinel`
- `runBreakfast_harvestGarden_networkFailure_continuesOtherActions`
- `runBreakfast_rumpus_skippedWhenPrefFalse`
- `runBreakfast_rumpus_callsVisit`
- `runBreakfast_vipLounge_skippedWithoutKey`
- `runBreakfast_klawLoopsUntilThree`
- `runBreakfast_klawStopsOnFailure`
- `runBreakfast_lookingGlass_skippedIfAlreadyDone`
- `runBreakfast_setsBreakfastCompletedOnSuccess`
- `clearBreakfastPrefs_resetsAllSentinels`

---

## Section 4: SessionManager Wiring

**File:** `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/SessionManager.kt`

Add `breakfastManager: BreakfastManager? = null` to constructor.

Login success block (complete updated order):

```kotlin
// 1. Daycount-gated rollover
val lastDay = preferences.getInt(Preferences.LAST_DAYCOUNT, -1)
val currentDay = charState.daycount
if (currentDay != lastDay) {
    banishManager?.clearExpiredAndRollover(charState.currentRun)
    breakfastManager?.clearBreakfastPrefs()
    preferences.setInt(Preferences.LAST_DAYCOUNT, currentDay)
}

// 2. Load persisted state
questLogRequest?.syncAll()
moodManager?.loadActiveMood()
moodManager?.loadMoodLibrary()
banishManager?.load()

// 3. Breakfast
breakfastManager?.runBreakfast(charState, inventoryState)

SessionState.LoggedIn
```

`SessionManager` needs `inventoryState` to pass to `runBreakfast`. Either inject `InventoryManager` into `SessionManager`, or fetch inventory state before calling `runBreakfast`. Prefer injecting `InventoryManager` (already a DI singleton).

---

## Section 5: CharacterApiResponse — daycount

**File:** `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/character/CharacterApiResponse.kt`

Add:
```kotlin
val daycount: Int = 0
```
Parsed from `/api.php?what=status&for=KoLmafia` JSON field `"daycount"`.

**File:** `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/character/CharacterState.kt`

Add:
```kotlin
val daycount: Int = 0
```
Populated via `character.updateFromApiResponse(response)` → `copy(daycount = response.daycount)`.

---

## Section 6: BanishManager — Banisher Detection

### AdventureResult.Combat — new field

**File:** `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureResult.kt`

```kotlin
data class Combat(
    val monster: String,
    val won: Boolean,
    val itemsGained: List<String> = emptyList(),
    val meatGained: Int = 0,
    val statsGained: Map<String, Int> = emptyMap(),
    val banished: Boolean = false,
    val banisher: Banisher = Banisher.UNKNOWN,   // NEW
) : AdventureResult()
```

### AdventureParser — BANISHER_PATTERNS

**File:** `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureParser.kt`

```kotlin
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

fun parseFightResult(html: String): AdventureResult.Combat {
    // ... existing parsing ...
    val banished = BANISH_PATTERN.containsMatchIn(html)
    val banisher = if (banished) {
        BANISHER_PATTERNS.firstOrNull { (text, _) -> html.contains(text) }?.second
            ?: Banisher.UNKNOWN
    } else Banisher.UNKNOWN
    return AdventureResult.Combat(
        monster = ..., won = ..., itemsGained = ..., meatGained = ...,
        statsGained = ..., banished = banished, banisher = banisher,
    )
}
```

**Test additions to `AdventureParserTest.kt`:**
- `parseFightResult_snokebomb_detectsBanisher`
- `parseFightResult_kgbDart_detectsBanisher`
- `parseFightResult_latteThrow_detectsBanisher`
- `parseFightResult_banishedNoPattern_returnsUnknown`

### AdventureManager — use result.banisher

**File:** `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureManager.kt`

In `resolveCombat()`, change:
```kotlin
banishManager?.banishMonster(
    monsterName = result.monster,
    banisher    = result.banisher,      // was Banisher.UNKNOWN
    currentTurn = character.state.value.currentRun,
)
```

Also update `MonsterBanished` event emission:
```kotlin
eventBus.emit(GameEvent.MonsterBanished(result.monster, result.banisher.canonicalName))
```

### AdventureManager — zone pre-flight banish check

Add `combatDatabase: CombatDatabase? = null` to `AdventureManager` constructor.

At the top of `runAdventures()`, before the turn loop:
```kotlin
val bm = banishManager
val zoneData = combatDatabase?.getByLocation(location.name)  // combats.txt uses human-readable zone names, matching AdventureLocation.name
if (bm != null && zoneData != null) {
    val currentTurn = character.state.value.currentRun
    val allBanished = zoneData.monsters
        .filter { it.weight > 0 }
        .all { bm.isBanished(it.name, currentTurn) }
    if (allBanished && zoneData.monsters.any { it.weight > 0 }) {
        _isRunning.value = false
        eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.AllMonstersBanished))
        return scope.launch {}   // no-op job
    }
}
```

### StopReason — new variant

**File:** `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/StopReason.kt`

```kotlin
object AllMonstersBanished : StopReason()
```

---

## Section 7: ASH Functions — is_banished, banishers_used

**File:** `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`

Add `banishManager: BanishManager? = null` to constructor. Add a new `registerBanishQueries(scope)` call in `registerAll()`.

```kotlin
private fun registerBanishQueries(scope: AshScope) {
    register(scope, "is_banished", AshType.BOOLEAN, listOf("monster" to AshType.MONSTER)) { _, args ->
        val name = args[0].toString()
        val currentTurn = character?.state?.value?.currentRun ?: 0
        AshValue.of(banishManager?.isBanished(name, currentTurn) ?: false)
    }

    // banishers_used() → string[monster] — monster name → banisher canonical name
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

**Test additions to `GameRuntimeLibraryTest.kt`:**
- `isBanished_monsterIsBanished_returnsTrue`
- `isBanished_monsterNotBanished_returnsFalse`
- `banishersUsed_returnsBanishedMonsters`
- `banishersUsed_noManager_returnsEmpty`

---

## Section 8: DI Wiring

**File:** `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt`

```kotlin
// New request singletons
singleOf(::CampgroundRequest)
singleOf(::ClanRumpusRequest)
singleOf(::ClanLoungeRequest)

// BreakfastManager
single {
    BreakfastManager(
        campgroundRequest  = get(),
        clanRumpusRequest  = get(),
        clanLoungeRequest  = get(),
        preferences        = get(),
        inventoryManager   = get(),
    )
}

// SessionManager — add breakfastManager + inventoryManager
single {
    SessionManager(
        ...,
        banishManager    = get(),
        breakfastManager = get(),
        inventoryManager = get(),
    )
}

// AdventureManager — add combatDatabase
single {
    AdventureManager(
        ...,
        banishManager    = get(),
        combatDatabase   = get(),
    )
}

// GameRuntimeLibrary — add banishManager
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

---

## Task Summary

| # | Task | Files | Scope |
|---|------|-------|-------|
| T1 | Preference key constants | `Preferences.kt` | 15 new constants |
| T2 | `CampgroundRequest` | `CampgroundRequest.kt` + test | Garden harvest HTTP |
| T3 | `ClanRumpusRequest` | `ClanRumpusRequest.kt` + test | Rumpus room HTTP |
| T4 | `ClanLoungeRequest` | `ClanLoungeRequest.kt` + test | Klaw + pool + looking glass + fireworks |
| T5 | `BreakfastManager` core | `BreakfastManager.kt` + test | Orchestrator + garden + rumpus + manual + wishes |
| T6 | `BreakfastManager` VIP Lounge + `clearBreakfastPrefs` | `BreakfastManager.kt` (modify) + test | VIP Lounge sub-actions wired in |
| T7 | `CharacterApiResponse` + `CharacterState` daycount | `CharacterApiResponse.kt`, `CharacterState.kt` + tests | Parse `daycount` field |
| T8 | `SessionManager` wiring | `SessionManager.kt` + test | Breakfast call + daycount-gated rollover clear |
| T9 | `AdventureResult.Combat` banisher field + `AdventureParser` patterns | `AdventureResult.kt`, `AdventureParser.kt` + test | 20 banisher text patterns |
| T10 | `AdventureManager` banisher + zone pre-flight | `AdventureManager.kt`, `StopReason.kt` + test | Use `result.banisher`; all-banished pre-check |
| T11 | ASH functions | `GameRuntimeLibrary.kt` + test | `is_banished()`, `banishers_used()` |
| T12 | DI wiring | `SharedModule.kt` | Wire all new classes |
