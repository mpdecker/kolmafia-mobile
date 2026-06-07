# Phase 11: Script Primitives + Quick Wins — Design Spec

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Deliver the highest-value remaining script compatibility improvements: `visit_url` + `cli_execute` dispatch (unblocking most community scripts), pricing functions, `my_familiar()` fix, BreakfastManager guild manual activation, and VillainLair + Rufus choice solvers.

**Architecture:** Extend `GameRuntimeLibrary` with one new `httpClient: HttpClient?` constructor param and two new extension files; extend `NpcStoreDatabase` with item-price parsing; add `RufusManager` and two choice handler files; fix `my_familiar()` in the existing base registration.

**Tech Stack:** Kotlin Multiplatform, Ktor `HttpClient`, Koin DI, existing `regFn()` extension pattern, existing `ChoiceHandler`/`ChoiceHandlerRegistry` pattern.

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
| `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/choice/handlers/VillainLairHandlersTest.kt` | Tests for VillainLair choice handler |
| `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/RufusManagerTest.kt` | Tests for `RufusManager` |
| `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/choice/handlers/RufusHandlersTest.kt` | Tests for Rufus choice handlers |

### Modified Files

| File | Change |
|------|--------|
| `ash/GameRuntimeLibrary.kt` | Add `httpClient: HttpClient?` param; add `registerWebRequests()` + `registerPricingQueries()` calls in `registerAll()`; fix `my_familiar()` at line 344; replace `cli_execute` echo stub with dispatch table |
| `data/NpcStoreDatabase.kt` | Extend parser to read item-price rows; add `_itemPrices` map; add `fun npcPrice(itemName: String): Int` |
| `data/NpcStoreData.kt` | Add `items: List<NpcStoreItem>` field |
| `data/GameDatabase.kt` | Add `fun npcPrice(itemName: String): Int` delegating to `NpcStoreDatabase` |
| `session/BreakfastManager.kt` | Add `useGuildManual(itemId: Int)` and call it after `detectGuildManual()`; add `UseItemRequest` constructor param if not already present |
| `adventure/choice/handlers/SolverHandlers.kt` | Remove TODO comments for 1260/1262/1498/1499; call `VillainLairHandlers.registerAll()` and `RufusHandlers.registerAll()` |
| `adventure/choice/ChoiceHandlerRegistry.kt` | Add `rufusManager: RufusManager` constructor param so `RufusHandlers` can access it |
| `di/SharedModule.kt` | Add `singleOf(::RufusManager)`; add `httpClient` to `GameRuntimeLibrary` binding |
| `preferences/Preferences.kt` | Add `GUILD_MANUAL_USED`, `RUFUS_QUEST_TYPE`, `RUFUS_QUEST_TARGET` constants |

---

## Component Designs

### 1. `visit_url` — `GameRuntimeLibrary.WebRequest.kt`

Two overloads registered by `internal fun GameRuntimeLibrary.registerWebRequests(scope: AshScope)`:

**Overload 1:** `visit_url(url: string) → string`
**Overload 2:** `visit_url(url: string, encoded: boolean) → string`

The `encoded` boolean controls URL construction:
- `encoded = false` (default): prepend `KOL_BASE_URL` → `"inventory.php?foo=bar"` becomes `"https://www.kingdomofloathing.com/inventory.php?foo=bar"`
- `encoded = true`: use the URL exactly as provided (for full external URLs)

```kotlin
internal fun GameRuntimeLibrary.registerWebRequests(scope: AshScope) {

    fun doVisit(url: String, encoded: Boolean): String {
        val client = httpClient ?: return ""
        val fullUrl = if (encoded) url
                      else "$KOL_BASE_URL/${url.trimStart('/')}"
        return runBlocking {
            try {
                val response = client.get(fullUrl)
                response.bodyAsText()
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

**Error contract (matches desktop):**
- Any HTTP response (4xx, 5xx): return the body string
- Network error (timeout, DNS failure, exception): return `""`
- `httpClient == null`: return `""`

### 2. `my_familiar()` fix — `GameRuntimeLibrary.kt` line 344

```kotlin
// Before (returns player name — bug):
register(scope, "my_familiar", AshType.FAMILIAR, emptyList()) { _, _ ->
    AshValue.familiar(character?.state?.value?.name ?: "none")
}

// After (returns active familiar race or "none"):
register(scope, "my_familiar", AshType.FAMILIAR, emptyList()) { _, _ ->
    AshValue.familiar(
        familiarManager?.state?.value?.activeFamiliar?.race
            ?.takeIf { it.isNotBlank() } ?: "none"
    )
}
```

### 3. `cli_execute` dispatch table — `GameRuntimeLibrary.kt`

Replace the echo stub with an extensible dispatch table. Add as a private property of `GameRuntimeLibrary`:

```kotlin
private val cliDispatch: List<Pair<Regex, (MatchResult, AshRuntime) -> Unit>> = listOf(
    Regex("^mood\\s+execute$", IGNORE_CASE) to { _, _ ->
        moodManager?.let { mood ->
            runBlocking { mood.executeMood(character, skillManager, effectManager) }
        }
    },
    Regex("^mood\\s+(.+)$", IGNORE_CASE) to { m, _ ->
        val name = m.groupValues[1].trim()
        moodManager?.setActiveMoodByName(name)
        moodManager?.let { mood ->
            runBlocking { mood.executeMood(character, skillManager, effectManager) }
        }
    },
    Regex("^set\\s+(.+?)\\s*=\\s*(.*)$") to { m, _ ->
        preferences?.setString(m.groupValues[1].trim(), m.groupValues[2])
    },
    Regex("^get\\s+(.+)$") to { m, rt ->
        val value = preferences?.getString(m.groupValues[1].trim(), "") ?: ""
        rt.print(value)
    },
)
```

Updated `cli_execute` registration in `registerGameActions()`:

```kotlin
register(scope, "cli_execute", AshType.BOOLEAN, listOf("cmd" to AshType.STRING)) { runtime, args ->
    val cmd = args[0].toString()
    val entry = cliDispatch.firstOrNull { (regex, _) -> regex.matches(cmd) }
    if (entry != null) {
        entry.second(entry.first.find(cmd)!!, runtime)
    } else {
        runtime.print("[cli] $cmd")   // unknown command: echo fallback
    }
    AshValue.of(true)
}
```

**Supported commands:**
- `mood execute` — runs missing triggers from the active mood
- `mood <name>` — sets active mood by name, then executes it
- `set key=value` — writes a preference string
- `get key` — reads a preference string and prints to script output

Adding a new command in future is a one-liner in `cliDispatch`.

> **Implementer note:** Check `AdventureManager.kt` for the exact `moodManager.executeMood(...)` call signature used before each adventure turn — the `cli_execute` dispatch should call it identically.

### 4. Pricing functions — `GameRuntimeLibrary.Pricing.kt`

```kotlin
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

### 5. `NpcStoreDatabase` extension

**New `data/NpcStoreItem.kt`:**
```kotlin
package net.sourceforge.kolmafia.data
data class NpcStoreItem(val itemName: String, val price: Int)
```

**`NpcStoreData.kt`** — add items list:
```kotlin
data class NpcStoreData(
    val storeKey: String,
    val storeName: String,
    val storeType: String,
    val items: List<NpcStoreItem> = emptyList()
) { ... }
```

**`NpcStoreDatabase.kt`** — extend parser and add lookup:
```kotlin
private val _itemPrices = mutableMapOf<String, Int>()

// In parse() — after reading store header, read subsequent item rows until next header
// Item row format (inspect bundled npcstores.txt to confirm exact column layout):
//   itemName TAB storeKey TAB price  (or similar)

fun npcPrice(itemName: String): Int = _itemPrices[itemName.lowercase()] ?: 0
```

**`GameDatabase.kt`** — delegate:
```kotlin
open fun npcPrice(itemName: String): Int = NpcStoreDatabase.npcPrice(itemName)
```

> **Implementer note:** Inspect `shared/src/commonMain/composeResources/files/data/npcstores.txt` before implementing the parser. Column layout may differ from the desktop version. Fallback to 0 on any parse error.

### 6. BreakfastManager guild manual activation

Add to `Preferences.kt`:
```kotlin
const val GUILD_MANUAL_USED = "guildManualUsed"   // boolean
```

In `BreakfastManager.kt`, verify `useItemRequest: UseItemRequest` is a constructor param (add if absent — already in Koin graph). Add:

```kotlin
private suspend fun useGuildManual(itemId: Int) {
    if (preferences.getBoolean(Preferences.GUILD_MANUAL_USED, false)) return
    val result = useItemRequest.use(itemId, 1)
    if (result.isSuccess) {
        preferences.setBoolean(Preferences.GUILD_MANUAL_USED, true)
    }
}
```

In `runBreakfast()`, after the existing `detectGuildManual()?.let { ... }` detection stub, replace the stub with:
```kotlin
detectGuildManual()?.let { itemId -> useGuildManual(itemId) }
```

`GUILD_MANUAL_USED` is cleared during daycount rollover alongside the other breakfast boolean prefs.

### 7. VillainLair solver — `VillainLairHandlers.kt`

Choices 1260 and 1262 require picking the door whose color matches `_villainLairColor` preference.

```kotlin
package net.sourceforge.kolmafia.adventure.choice.handlers

import net.sourceforge.kolmafia.adventure.choice.ChoiceHandler
import net.sourceforge.kolmafia.adventure.choice.ChoiceHandlerRegistry

object VillainLairHandlers {

    private fun colorHandler(): ChoiceHandler = handler@{ ctx ->
        val color = ctx.preference.getString("_villainLairColor", "").lowercase()
        if (color.isEmpty()) return@handler null
        // Scan up to 6 options; find the one whose HTML fragment mentions the color
        (1..6).firstOrNull { option ->
            val marker = "name=whichchoice value=$option"
            val fragment = ctx.responseText
                .substringAfter(marker, "")
                .substringBefore("name=whichchoice", "")
            fragment.contains(color, ignoreCase = true)
        }
    }

    val handlers: Map<Int, ChoiceHandler> = mapOf(
        1260 to colorHandler(),
        1262 to colorHandler()
    )

    fun registerAll(registry: ChoiceHandlerRegistry) =
        handlers.forEach { (id, h) -> registry.register(id, h) }
}
```

Returns `null` when `_villainLairColor` is empty → falls through to preference/manual. HTML parsing approach follows the existing `ResponseTextHandlers` pattern.

`VillainLairHandlers.registerAll(registry)` called from `ChoiceHandlerRegistry` init.

### 8. Rufus solver — `RufusManager.kt` + `RufusHandlers.kt`

**`RufusManager.kt`:**

```kotlin
package net.sourceforge.kolmafia.adventure

import net.sourceforge.kolmafia.preferences.Preferences

class RufusManager(private val preferences: Preferences) {

    companion object {
        const val RUFUS_QUEST_TYPE   = "_rufusQuestType"    // "entity" | "artifact" | "monument"
        const val RUFUS_QUEST_TARGET = "_rufusQuestTarget"  // target name (set after choice)
    }

    val questType: String
        get() = preferences.getString(RUFUS_QUEST_TYPE, "entity").lowercase()

    // Choice 1498: find which option number in the HTML matches the desired quest type.
    // Desktop option order is: 1=entity, 2=artifact, 3=monument (confirm by inspecting live HTML).
    fun chooseQuestOption(responseText: String): Int? {
        val desired = questType
        val optionLabels = listOf("entity" to 1, "artifact" to 2, "monument" to 3)
        return optionLabels
            .firstOrNull { (label, _) ->
                label == desired && responseText.contains(label, ignoreCase = true)
            }
            ?.second
    }

    // Choice 1499: always confirm (option 1).
    fun confirmChoice(): Int = 1

    // Store the target name after quest is accepted (extracted from post-choice response).
    fun recordQuestTarget(target: String) {
        preferences.setString(RUFUS_QUEST_TARGET, target)
    }
}
```

**`RufusHandlers.kt`:**

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

**`ChoiceHandlerRegistry`** receives `rufusManager: RufusManager` as a new constructor param. `RufusHandlers.registerAll(this, rufusManager)` called in `init {}` alongside existing groups.

> **Implementer note:** Inspect live Shadow Rift choice HTML (or KoLmafia source `ChoiceManager.java` around choice 1498) to confirm option-number-to-quest-type mapping before finalizing `optionLabels`. The fallback is `null` → preference/manual if the response HTML doesn't match any label.

### 9. DI wiring — `SharedModule.kt`

```kotlin
// New singleton
singleOf(::RufusManager)

// GameRuntimeLibrary binding — add httpClient param
single {
    GameRuntimeLibrary(
        character         = get(),
        inventoryManager  = get(),
        skillManager      = get(),
        effectManager     = get(),
        adventureManager  = get(),
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
        httpClient        = get(),   // ← new
    )
}

// ChoiceHandlerRegistry binding — add rufusManager param
single {
    ChoiceHandlerRegistry(
        // ... existing params ...
        rufusManager = get(),        // ← new
    )
}
```

`HttpClient` is already a named singleton in Koin from the existing login/request infrastructure.

---

## Preference Key Constants

Add to `Preferences.kt` companion:

```kotlin
// Breakfast
const val GUILD_MANUAL_USED  = "guildManualUsed"    // boolean; cleared on daycount change

// Rufus / Shadow Rift
const val RUFUS_QUEST_TYPE   = "_rufusQuestType"     // string: "entity"|"artifact"|"monument"
const val RUFUS_QUEST_TARGET = "_rufusQuestTarget"   // string: target name
```

`GUILD_MANUAL_USED` added to the list of breakfast prefs cleared in `SessionManager` on daycount rollover.

---

## Testing Strategy

| Component | Key test cases |
|-----------|---------------|
| `visit_url` | GET returns body; non-200 returns body (not empty); network error returns `""`; encoded=false prepends KOL_BASE_URL; encoded=true uses URL as-is; null httpClient returns `""` |
| `my_familiar()` | Returns familiar race when active; returns `"none"` when no familiar; no longer returns player name |
| `cli_execute` | `mood execute` calls executeMood; `mood name` sets + executes; `set k=v` writes pref; `get k` reads and prints pref; unknown command echoes |
| `autosell_price` | Returns correct price from GameDatabase; returns 0 for unknown item |
| `npc_price` | Returns correct price for NPC-sold item; returns 0 for non-NPC item; returns 0 for unknown item |
| `NpcStoreDatabase` | Parses item rows correctly; `npcPrice()` returns 0 for missing item |
| `BreakfastManager` guild manual | Calls UseItemRequest when manual detected; sets GUILD_MANUAL_USED pref on success; skips if pref already true |
| `VillainLairHandlers` | Returns correct option when color found in HTML; returns null when `_villainLairColor` empty |
| `RufusManager` | `chooseQuestOption` returns matching option for each quest type; returns null when type not in HTML |
| `RufusHandlers` | Choice 1498 delegates to `chooseQuestOption`; choice 1499 returns 1 |

---

## Implementation Order (suggested task sequence)

1. Preference key constants (`GUILD_MANUAL_USED`, `RUFUS_QUEST_TYPE`, `RUFUS_QUEST_TARGET`)
2. `my_familiar()` fix + test
3. `NpcStoreItem` data class + `NpcStoreDatabase` item-price parser + test
4. `GameDatabase.npcPrice()` delegate
5. `GameRuntimeLibrary.Pricing.kt` (`autosell_price`, `npc_price`) + test
6. `GameRuntimeLibrary.kt` — add `httpClient` param + `cli_execute` dispatch table + `registerPricingQueries`/`registerWebRequests` calls
7. `GameRuntimeLibrary.WebRequest.kt` (`visit_url` × 2) + test
8. BreakfastManager guild manual (`useGuildManual`) + test
9. `VillainLairHandlers.kt` + register + test
10. `RufusManager.kt` + test
11. `RufusHandlers.kt` + register in `ChoiceHandlerRegistry` + test
12. DI wiring — `SharedModule.kt` (`RufusManager`, `httpClient` in GameRuntimeLibrary, `rufusManager` in ChoiceHandlerRegistry)
