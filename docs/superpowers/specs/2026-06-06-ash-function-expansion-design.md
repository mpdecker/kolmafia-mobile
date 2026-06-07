# Phase 10: ASH Function Expansion — Design Spec

_2026-06-06_

## Goal

Double ASH function coverage from ~54 to ~108 overloads (~13% of desktop's 835) by adding state-query functions across all missing domains and HTTP action functions for the core item-use/eat/drink/sell/storage loop. The result: the top community automation scripts become runnable on mobile for the first time.

## Architecture

`GameRuntimeLibrary.kt` becomes a coordinator — constructor + `registerAll()` only. Each domain is a Kotlin extension file (`internal fun GameRuntimeLibrary.register*()`) in the same package. Extension functions share access to the class's private members within the same module, so no visibility changes are needed.

Seven new HTTP request classes are added to `request/`, following the existing pattern (Ktor `HttpClient`, suspend functions, `Result<T>` return, error-string detection).

One `AdventureManager` change: write `Preferences.LAST_MONSTER` on each combat resolution so the new `last_monster()` ASH function can read it.

## File Structure

### `ash/` — coordinator + 11 domain extension files

```
ash/
  GameRuntimeLibrary.kt              ← constructor + registerAll() only (~60 lines after extraction)
  GameRuntimeLibrary.Character.kt    ← registerCharacterQueries()   — 9 new fns
  GameRuntimeLibrary.Familiar.kt     ← registerFamiliarQueries()    — 4 fns
  GameRuntimeLibrary.Equipment.kt    ← registerEquipmentQueries()   — 4 fns
  GameRuntimeLibrary.Modifiers.kt    ← registerModifierQueries()    — 6 overloads (3 fns × item+effect)
  GameRuntimeLibrary.Collections.kt  ← registerCollectionQueries()  — 5 fns
  GameRuntimeLibrary.DateTime.kt     ← registerDateTimeQueries()    — 5 fns
  GameRuntimeLibrary.Goals.kt        ← registerGoalQueries()        — 4 fns
  GameRuntimeLibrary.Mood.kt         ← registerMoodQueries()        — 2 fns
  GameRuntimeLibrary.Prefs.kt        ← registerPreferenceAccess()   — 2 fns
  GameRuntimeLibrary.Combat.kt       ← registerCombatStubs()        — 4 fns
  GameRuntimeLibrary.ItemActions.kt  ← registerItemActions()        — 9 fns (HTTP)
```

The existing `registerTypeConversions`, `registerStringUtils`, `registerMathUtils`,
`registerAggregateUtils`, `registerPrintUtils`, `registerItemQueries`, `registerSkillQueries`,
`registerEffectQueries`, `registerBanishQueries`, and `registerGameActions` **stay in
`GameRuntimeLibrary.kt`** as-is — no migration of existing code.

### `request/` — 7 new HTTP wrappers

```
request/
  UseItemRequest.kt       ← inv_use.php / multiuse.php
  EatFoodRequest.kt       ← inv_eat.php
  DrinkBoozeRequest.kt    ← inv_drink.php
  ChewRequest.kt          ← multiuse.php (spleen items)
  AutosellRequest.kt      ← sellstuff_multi.php
  ClosetRequest.kt        ← closet.php?action=put
  StorageRequest.kt       ← storage.php?action=pullitem
```

`use_familiar` adds `setFamiliar(name: String): Result<Unit>` to the existing `FamiliarManager`
(no new request file). Item ID resolution is shared across all action functions (see below).

## Constructor Changes

`GameRuntimeLibrary` gains 10 new optional params (all `= null` so `forTesting()` and all
existing test helpers keep compiling with zero changes):

```kotlin
class GameRuntimeLibrary(
    // existing (unchanged):
    private val character: KoLCharacter? = null,
    private val inventoryManager: InventoryManager? = null,
    private val skillManager: SkillManager? = null,
    private val effectManager: EffectManager? = null,
    private val adventureManager: AdventureManager? = null,
    private val banishManager: BanishManager? = null,
    // new:
    private val familiarManager: FamiliarManager? = null,
    private val goalManager: GoalManager? = null,
    private val moodManager: MoodManager? = null,
    private val preferences: Preferences? = null,
    private val gameDatabase: GameDatabase? = null,
    private val useItemRequest: UseItemRequest? = null,
    private val eatFoodRequest: EatFoodRequest? = null,
    private val drinkBoozeRequest: DrinkBoozeRequest? = null,
    private val chewRequest: ChewRequest? = null,
    private val autosellRequest: AutosellRequest? = null,
    private val closetRequest: ClosetRequest? = null,
    private val storageRequest: StorageRequest? = null,
)
```

## Function Inventory

### Character queries — `GameRuntimeLibrary.Character.kt`

| Function | Return type | Behavior |
|---|---|---|
| `my_class()` | `class` | `CharacterState.characterClassEnum.displayName` as CLASS value |
| `my_path()` | `path` | `CharacterState.path` as PATH value |
| `my_sign()` | `string` | `CharacterState.sign` (zodiac sign string) |
| `my_primestat()` | `stat` | Muscle-based → `"Muscle"`; Mysticality → `"Mysticality"`; else `"Moxie"` |
| `in_run()` | `boolean` | `!CharacterState.kingLiberated` |
| `under_standard()` | `boolean` | `CharacterState.underStandard` |
| `ascension_number()` | `int` | `CharacterState.ascensionNumber` |
| `can_interact()` | `boolean` | `!isHardcore && !inRonin` |
| `my_thrall()` | `string` | Stub: `""` — no thrall tracking; THRALL type not yet in AshType |

### Familiar queries — `GameRuntimeLibrary.Familiar.kt`

| Function | Signature | Behavior |
|---|---|---|
| `have_familiar` | `(familiar) → boolean` | `FamiliarManager.state.value.familiars.any { name matches }` |
| `my_familiar_weight` | `() → int` | `CharacterState.familiarWeight`; `0` if field absent |
| `to_familiar` | `(string) → familiar` | `AshValue.familiar(args[0].toString())` |
| `use_familiar` | `(familiar) → boolean` | `FamiliarManager.setFamiliar(name)` via `familiar.php?action=newfam`; `runBlocking` |

### Equipment queries — `GameRuntimeLibrary.Equipment.kt`

| Function | Signature | Behavior |
|---|---|---|
| `equipped_item` | `(slot) → item` | `CharacterState.equipment[slotName]` → item name; `"none"` if empty |
| `have_equipped` | `(item) → boolean` | `CharacterState.equipment.values.any { it == itemName }` |
| `to_slot` | `(string) → slot` | `AshValue(AshType.SLOT, args[0].toString())` |
| `slot_to_item` | `(slot) → item` | Alias for `equipped_item` |

### Modifier queries — `GameRuntimeLibrary.Modifiers.kt`

6 overloads (3 functions × item + effect):

| Function | Signature | Behavior |
|---|---|---|
| `numeric_modifier` | `(item, string) → float` | `ModifierParser` lookup for named modifier on item; `0.0` if not found |
| `numeric_modifier` | `(effect, string) → float` | Same, for effect entity |
| `boolean_modifier` | `(item, string) → boolean` | `numeric_modifier(...) > 0.0` |
| `boolean_modifier` | `(effect, string) → boolean` | Same, for effect entity |
| `string_modifier` | `(item, string) → string` | String-typed modifier (element, class restriction, etc.); `""` if not found |
| `string_modifier` | `(effect, string) → string` | Same, for effect entity |

### Collection accessors — `GameRuntimeLibrary.Collections.kt`

All return `int[item]` = `AggregateType(keyType=ITEM, dataType=INT)`.

| Function | Behavior |
|---|---|
| `get_inventory()` | `InventoryManager` state → live `AggregateValue`; key = item name, value = quantity |
| `get_closet()` | Stub: empty aggregate (no closet HTTP) |
| `get_storage()` | Stub: empty aggregate (no storage fetch HTTP) |
| `get_stash()` | Stub: empty aggregate |
| `get_display()` | Stub: empty aggregate |

### Date/time — `GameRuntimeLibrary.DateTime.kt`

| Function | Return | Behavior |
|---|---|---|
| `today_to_string()` | `string` | Current local date as `"YYYYMMDD"` via `kotlinx-datetime` |
| `now_to_string()` | `string` | `"YYYYMMDD HH:mm:ss"` local time |
| `gameday_to_string()` | `string` | Same as `today_to_string()` — KoL game day ≈ real day for script purposes |
| `rollover()` | `int` | Approximate seconds until next rollover (midnight UTC-6); derived from local clock: `secondsUntilMidnightUTC6()` |
| `moon_phase()` | `int` | Stub: `0` — requires server-side moon data |

### Goal management — `GameRuntimeLibrary.Goals.kt`

| Function | Signature | Behavior |
|---|---|---|
| `add_item_condition` | `(int, item) → void` | `GoalManager.addGoal(ItemNameGoal(itemName, qty))` |
| `remove_item_condition` | `(int, item) → void` | `GoalManager.removeGoal(itemName)` — adds `removeGoal()` to `GoalManager` |
| `goal_exists` | `(string) → boolean` | `"item"` → any ItemGoal; `"meat"` → MeatGoal; `"level"` → LevelGoal |
| `get_goals` | `() → string[int]` | Serialize each goal as a human-readable string; return as `AggregateValue(INT→STRING)` |

### Mood queries — `GameRuntimeLibrary.Mood.kt`

| Function | Return | Behavior |
|---|---|---|
| `get_moods()` | `string[int]` | `MoodManager.moodLibrary.keys` as indexed string aggregate |
| `mood_list()` | `string[int]` | Alias for `get_moods()` |

### Preference access — `GameRuntimeLibrary.Prefs.kt`

| Function | Signature | Behavior |
|---|---|---|
| `get_property` | `(string) → string` | `preferences.getString(key, "")` |
| `set_property` | `(string, string) → void` | `preferences.setString(key, value)` |

### Combat state — `GameRuntimeLibrary.Combat.kt`

| Function | Return | Behavior |
|---|---|---|
| `in_multi_fight()` | `boolean` | Stub: `false` |
| `fight_follows_choice()` | `boolean` | Stub: `false` |
| `last_monster()` | `monster` | Reads `Preferences.LAST_MONSTER` pref (written by `AdventureManager` on each combat) |
| `copiers_used(skill)` | `int` | Stub: `0` |

### Item actions — `GameRuntimeLibrary.ItemActions.kt`

All HTTP actions use `runBlocking` (same pattern as `adventure()`). All resolve item name →
numeric ID before issuing requests. Return `false` immediately if ID resolution fails or the
required request object is null.

| Function | Signature | HTTP endpoint |
|---|---|---|
| `use` | `(int, item) → boolean` | `UseItemRequest` → `inv_use.php` (qty=1) or `multiuse.php` (qty>1) |
| `eat` | `(int, item) → boolean` | `EatFoodRequest` → `inv_eat.php` |
| `drink` | `(int, item) → boolean` | `DrinkBoozeRequest` → `inv_drink.php` |
| `chew` | `(int, item) → boolean` | `ChewRequest` → `multiuse.php` |
| `autosell` | `(int, item) → boolean` | `AutosellRequest` → `sellstuff_multi.php` |
| `put_closet` | `(int, item) → boolean` | `ClosetRequest` → `closet.php?action=put` |
| `take_storage` | `(int, item) → boolean` | `StorageRequest` → `storage.php?action=pullitem` |
| `retrieve_item` | `(int, item) → boolean` | Inventory check → if short, `StorageRequest.pull()`; no post-pull verification |
| `use_familiar` | `(familiar) → boolean` | `FamiliarManager.setFamiliar()` → `familiar.php?action=newfam` |

**Item ID resolution** (shared across all action functions):

```kotlin
private fun resolveItemId(name: String): Int? =
    gameDatabase?.itemByName(name)?.id
        ?: inventoryManager?.state?.value?.items?.values
            ?.find { it.name.equals(name, ignoreCase = true) }?.itemId
```

Falls back to inventory state if `GameDatabase` doesn't have the item. Returns `null` → function
returns `false` with no HTTP call.

## HTTP Request Classes

All follow the existing pattern: `HttpClient` constructor param, suspend functions, `Result<T>`
return, non-2xx status → failure, known error strings in response HTML → failure.

### `UseItemRequest`
```kotlin
class UseItemRequest(private val client: HttpClient) {
    suspend fun use(itemId: Int, quantity: Int = 1): Result<Unit>
    // quantity == 1 → inv_use.php?whichitem={id}
    // quantity  > 1 → multiuse.php?action=useitem&whichitem={id}&quantity={qty}
    // fails on: "You don't have that item"
}
```

### `EatFoodRequest`
```kotlin
class EatFoodRequest(private val client: HttpClient) {
    suspend fun eat(itemId: Int, quantity: Int): Result<Unit>
    // inv_eat.php?whichitem={id}&quantity={qty}
    // fails on: "You can't eat anything", "too full", "You don't have that item"
}
```

### `DrinkBoozeRequest`
```kotlin
class DrinkBoozeRequest(private val client: HttpClient) {
    suspend fun drink(itemId: Int, quantity: Int): Result<Unit>
    // inv_drink.php?whichitem={id}&quantity={qty}
    // fails on: "You can't drink anything", "too drunk"
}
```

### `ChewRequest`
```kotlin
class ChewRequest(private val client: HttpClient) {
    suspend fun chew(itemId: Int, quantity: Int): Result<Unit>
    // multiuse.php?action=useitem&whichitem={id}&quantity={qty}
    // fails on: "too disgusting", spleen-full patterns
}
```

### `AutosellRequest`
```kotlin
class AutosellRequest(private val client: HttpClient) {
    suspend fun autosell(itemId: Int, quantity: Int): Result<Int>  // meat gained
    // sellstuff_multi.php?action=autosell&whichitem[]={id}&howmany={qty}
    // parses "You gain (\d+) Meat" from response; returns 0 if pattern absent but no error
}
```

### `ClosetRequest`
```kotlin
class ClosetRequest(private val client: HttpClient) {
    suspend fun put(itemId: Int, quantity: Int): Result<Unit>
    // closet.php?action=put&whichitem[]={id}&howmany={qty}
    // fails on: "You don't have that item"
}
```

### `StorageRequest`
```kotlin
class StorageRequest(private val client: HttpClient) {
    suspend fun pull(itemId: Int, quantity: Int): Result<Unit>
    // storage.php?action=pullitem&whichitem[]={id}&howmany={qty}
    // fails on: "You don't have storage access", Ronin/HC restriction strings
}
```

### `FamiliarManager.setFamiliar()` (new method, not a new class)
```kotlin
// familiar.php?action=newfam&whichfamiliar={id}
suspend fun setFamiliar(name: String): Result<Unit>
```
Resolves familiar name to ID from `FamiliarManager.state`. Returns failure if familiar not found.

## Other Code Changes

### `Preferences.kt` — one new constant
```kotlin
const val LAST_MONSTER = "_lastMonster"
```

### `AdventureManager.kt` — one new line in `resolveCombat()`
After the combat result is parsed:
```kotlin
preferences.setString(Preferences.LAST_MONSTER, result.monster)
```
No new dependency — `AdventureManager` already holds `preferences`.

### `GoalManager.kt` — one new method
```kotlin
fun removeGoal(itemName: String)
```
Removes the first `ItemGoal` or `ItemNameGoal` matching `itemName` (case-insensitive).

### `SharedModule.kt`
Seven new `singleOf(::*)` registrations for the new request classes. Updated `GameRuntimeLibrary`
single block wires all new params via `get()`.

## Testing

### New test files

A new `GameRuntimeLibraryTestHelpers.kt` (same test package) exposes top-level helper
functions — `runLib()`, `outputLib()`, `runWithCharacter()`, `prefs()` — so all domain test
files can call them without copying. Existing `GameRuntimeLibraryTest.kt` is updated to use
these shared helpers instead of its own private copies.

| Test file | Key cases |
|---|---|
| `GameRuntimeLibraryCharacterTest.kt` | `my_class` returns correct class name; `in_run` false when king liberated; `can_interact` false in HC |
| `GameRuntimeLibraryFamiliarTest.kt` | `have_familiar` true/false against FamiliarManager state; `to_familiar` round-trips name; `my_familiar_weight` reads CharacterState |
| `GameRuntimeLibraryEquipmentTest.kt` | `equipped_item` returns item name in slot; `"none"` for empty slot; `have_equipped` true/false |
| `GameRuntimeLibraryModifiersTest.kt` | `numeric_modifier` returns `0.0` for unknown item; returns parsed value for known item |
| `GameRuntimeLibraryCollectionsTest.kt` | `get_inventory` reflects InventoryManager state (count, item names); closet/storage/stash return empty aggregate with count 0 |
| `GameRuntimeLibraryDateTimeTest.kt` | `today_to_string` matches `^\d{8}$` pattern; `rollover` returns a timestamp > current time |
| `GameRuntimeLibraryGoalsTest.kt` | `add_item_condition` → `goal_exists("item")` true; `remove_item_condition` → false; `get_goals` count matches |
| `GameRuntimeLibraryMoodTest.kt` | `get_moods` returns mood library names; returns empty aggregate when library is empty |
| `GameRuntimeLibraryPrefsTest.kt` | `get_property` returns `""` for unknown key; `set_property` + `get_property` round-trips value |
| `GameRuntimeLibraryCombatTest.kt` | `in_multi_fight` false; `fight_follows_choice` false; `last_monster` reads `_lastMonster` pref |
| `GameRuntimeLibraryItemActionsTest.kt` | `use` sends correct URL params; `eat` returns false on fullness response; `retrieve_item` skips HTTP when already in inventory; null request → false |
| `UseItemRequestTest.kt` | Single-use routes to `inv_use.php`; multi-use routes to `multiuse.php`; failure on error HTML |
| `EatFoodRequestTest.kt` | Correct URL params; `Result.failure` on "too full" response |
| `DrinkBoozeRequestTest.kt` | Correct URL params; `Result.failure` on "too drunk" |
| `ChewRequestTest.kt` | Routes to `multiuse.php`; failure on spleen-full pattern |
| `AutosellRequestTest.kt` | Parses meat gained; returns 0 meat when pattern absent but no error |
| `ClosetRequestTest.kt` | `action=put` in URL; item and quantity params present |
| `StorageRequestTest.kt` | `action=pullitem` in URL; failure on Ronin/HC restriction string |

### Existing tests that need updates

- `GameRuntimeLibraryTest.kt` — helpers extracted to `GameRuntimeLibraryTestBase.kt`; existing
  tests updated to extend or import from base
- `AdventureManagerTest.kt` — one new test: after combat resolves, `LAST_MONSTER` pref equals
  the monster name from the combat result

## Scope Boundaries (Explicitly Out of Scope)

- `get_closet`, `get_storage`, `get_stash` — live HTTP fetch (stubs return empty this phase)
- `moon_phase` — server-side data (stub returns 0)
- `my_thrall` — no THRALL AshType yet (stub returns `""`)
- `in_multi_fight`, `fight_follows_choice`, `copiers_used` — stubs (no combat tracking)
- `buy`, `sell`, `create`, `craft` — economy actions deferred
- `visit_url` — web scripting escape hatch deferred
- `outfit`, `have_outfit` — outfit data queries deferred
- Full modifier overloads beyond item + effect (skill, familiar, zodiac, etc.)
- Response side-effect parsing for eat/drink/chew (HP/MP/fullness changes not parsed)
