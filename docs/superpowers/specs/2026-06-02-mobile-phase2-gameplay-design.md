# KoLmafia Mobile — Phase 2: Core Gameplay Design

**Date:** 2026-06-02
**Status:** Approved
**Scope:** Adventure loop (full automation), inventory management (full CRUD + crafting + mall), familiar management (full terrarium + hatchery + familiar-specific actions), and UI for all three

---

## Overview

Phase 2 builds on the Phase 1 foundation (auth, character state, HTTP client, Koin DI, Compose UI) to deliver full core gameplay: the player can run N turns unattended in any zone, manage their inventory and equipment, and manage their familiars — all from the mobile app.

**Deliverable:** Can adventure with full automation, view and manage inventory (including crafting and mall), manage familiars including hatchery, and track turns on-device.

---

## Architecture

### Pattern: Event Bus with Domain Managers

Each of the three new domains (adventure, inventory, familiar) has a `Manager` class that:
- Owns its domain state as a `MutableStateFlow`
- Handles all HTTP operations for its domain
- Publishes `GameEvent`s to a shared `GameEventBus` after successful operations
- Subscribes to relevant events from other managers via `GameEventBus`

Managers never read each other's state directly. All cross-domain coordination happens through events. This matches KoLmafia's existing listener/registry architecture and gives Phase 5's ASH engine a clean subscription surface.

### New Module Layout (`shared/commonMain`)

```
event/
  GameEvent.kt              ← sealed class hierarchy
  GameEventBus.kt           ← MutableSharedFlow<GameEvent>, singleton

adventure/
  AdventureLocation.kt      ← data class: id, name, zone, urlParam
  AdventureResult.kt        ← sealed: Combat, NonCombat, Choice
  CombatState.kt            ← ongoing fight state: monster, round, hp, mp
  MacroStrategy.kt          ← macro text storage: global default + per-zone overrides
  AdventureParser.kt        ← parses adventure.php / fight.php HTML → AdventureResult
  AdventureRequest.kt       ← HTTP POST adventure.php
  FightRequest.kt           ← HTTP POST fight.php (macro delivery)
  ChoiceRequest.kt          ← HTTP POST choice.php
  AdventureManager.kt       ← orchestrates N-turn loop; publishes to GameEventBus

inventory/
  InventoryItem.kt          ← data class: itemId, name, quantity, type
  InventoryState.kt         ← data class: items, fullness, inebriety, spleen, meat
  UseItemRequest.kt         ← HTTP POST inv_use.php
  EquipRequest.kt           ← HTTP POST inv_equip.php
  UnequipRequest.kt         ← HTTP POST inv_equip.php?action=unequip
  DiscardRequest.kt         ← HTTP POST multiuse.php?action=trash
  CraftRequest.kt           ← HTTP POST craft.php (combine/cook/cocktail/smith)
  MallSearchRequest.kt      ← HTTP POST mallsearch.php
  MallBuyRequest.kt         ← HTTP POST mallstore.php
  InventoryManager.kt       ← owns InventoryState; all item operations; subscribes to events

familiar/
  FamiliarData.kt           ← data class: id, name, race, weight, exp, kills, equipment, modifiers
  FamiliarState.kt          ← data class: activeFamiliar, ownedFamiliars
  FamiliarAction.kt         ← sealed class for familiar-specific actions
  FamiliarRequest.kt        ← HTTP POST familiar.php (switch/putback)
  FamiliarEquipRequest.kt   ← HTTP POST inv_equip.php?slot=familiarequip
  HatcheryRequest.kt        ← HTTP POST hatchery.php
  FamiliarActionRequest.kt  ← HTTP for familiar-specific actions (varies by familiar)
  FamiliarManager.kt        ← owns FamiliarState; all familiar operations

ui/adventure/
  AdventureScreen.kt        ← zone picker, N-turn runner, live result feed
  MacroEditorScreen.kt      ← macro text editor (global + per-zone tabs)
  CombatResultCard.kt       ← composable for a single combat result

ui/inventory/
  InventoryScreen.kt        ← tabbed item list (All/Equipment/Food/Drink/Spleen/Usable)
  ItemDetailSheet.kt        ← bottom sheet: description + contextual actions
  CraftingScreen.kt         ← four-tab crafting interface

ui/familiar/
  FamiliarScreen.kt         ← active familiar card + terrarium grid
  FamiliarDetailSheet.kt    ← familiar stats, switch, equip, familiar-specific action
  HatcheryScreen.kt         ← available eggs list with Hatch button
```

`App.kt` gains a `BottomNavBar` with four destinations: Character · Adventure · Inventory · Familiar.

---

## Adventure Loop

### State Machine

```
idle
  → POST adventure.php (snarfblat=<zone_id>)
  → AdventureParser determines result type:
      Combat     → POST fight.php (action=macro&macrotext=<strategy>)
                   → parse fight result
                   → repeat until fight ends
                   → emit CombatFinished
      NonCombat  → emit NonCombatEncountered
      Choice     → POST choice.php (whichchoice=<id>&option=<n>)
                   → emit ChoiceResolved
  → re-fetch api.php?what=status (updates KoLCharacter HP/MP/turns)
  → emit TurnConsumed
  → loop if turns_remaining > 0
```

### KoL Endpoints

| Step | Endpoint |
|---|---|
| Start adventure | `POST adventure.php` — `snarfblat=<zone_id>` |
| Fight (macro) | `POST fight.php` — `action=macro&macrotext=<text>` |
| Choice | `POST choice.php` — `whichchoice=<id>&option=<n>` |
| Status sync | `GET api.php?what=status` |

### MacroStrategy

Combat macros are stored in `Preferences` and sent verbatim to `fight.php` — the KoL server executes them, so no local interpretation is needed. Lookup order:

1. Per-zone override: `Preferences.getString("combatMacro_<zone_id>")`
2. Global default: `Preferences.getString("combatMacroDefault")`
3. Bundled safe default: `"attack; if (hpbelow 30) use healing potion; attack"`

### Loop Control

`AdventureManager.runAdventures(location: AdventureLocation, turns: Int)` is a `suspend` function running in a `CoroutineScope`. The loop is cancelled and `AdventureLoopStopped(reason)` is emitted on:
- User taps Stop
- `KoLCharacter.adventuresLeft == 0`
- `KoLCharacter.currentHp == 0` (character death)
- KoL server rejects the macro (parse error in fight response)
- Unexpected HTTP response (non-2xx, unexpected redirect)

---

## Inventory Management

### Fetch

`InventoryManager` fetches inventory on login and after any turn that emits `ItemObtained` or `ItemConsumed`:
- `GET api.php?what=inventory` → JSON object: `{itemId: quantity, …}`
- `GET api.php?what=equipment` → currently equipped items

Both are merged into `InventoryState`.

### Item Operations

| Operation | Endpoint |
|---|---|
| Use consumable | `POST inv_use.php?which=3&whichitem=<id>` |
| Equip item | `POST inv_equip.php?which=2&whichitem=<id>&slot=<slot>` |
| Unequip slot | `POST inv_equip.php?action=unequip&type=<slot>` |
| Discard | `POST multiuse.php?action=trash&whichitem=<id>&quantity=<n>` |
| Craft (combine) | `POST craft.php?action=combine&mode=combine&item1=<id>&item2=<id>` |
| Craft (cook) | `POST craft.php?action=cook` + ingredient params |
| Craft (cocktail) | `POST craft.php?action=cocktail` + ingredient params |
| Craft (smith) | `POST craft.php?action=smith` + ingredient params |
| Mall search | `POST mallsearch.php?searching=Yep&phrasetype=exact&pudnuggler=<query>` |
| Mall buy | `POST mallstore.php?whichstore=<storeid>&buying=<id>&quantity=<n>` |

After each mutating operation, `InventoryManager` re-fetches `api.php?what=inventory` to sync state, then emits the relevant `GameEvent` (`ItemConsumed`, `ItemEquipped`, `ItemDiscarded`, `ItemCrafted`, `MallPurchase`).

**Item types** are inferred from the API response category field: `food`, `drink`, `spleen`, `weapon`, `offhand`, `hat`, `shirt`, `pants`, `accessory`, `familiar`, `usable`, `multiusable`, `reusable`, `other`. Item type determines which action buttons appear in `ItemDetailSheet`.

---

## Familiar Management

### Fetch

`FamiliarManager` fetches the full terrarium on login via `GET api.php?what=familiars`, which returns a JSON array of all owned familiars. The active familiar is identified by the `active` field.

### FamiliarData

```kotlin
data class FamiliarData(
    val id: Int,
    val name: String,
    val race: String,
    val weight: Int,
    val experience: Int,
    val kills: Int,
    val equipment: InventoryItem?,
    val modifiers: Map<String, String>
)
```

No Swing dependencies. All rendering is delegated to the UI layer.

### Familiar Operations

| Operation | Endpoint |
|---|---|
| Switch active | `POST familiar.php?action=newfam&whichfam=<id>` |
| Put in terrarium | `POST familiar.php?action=putback` |
| Equip familiar item | `POST inv_equip.php?which=2&whichitem=<id>&slot=familiarequip` |
| Hatch egg | `POST hatchery.php?action=hatch&whichitem=<id>` |
| Familiar-specific action | varies per familiar type |

**Familiar-specific actions** are modelled as a `FamiliarAction` sealed class. Only the most commonly used familiar-specific actions are implemented in Phase 2 (e.g. Pocket Professor lecture selection, Shortest Wig assignment). Uncommon familiars display a "use KoL web interface" prompt that opens a browser to the relevant page.

After any operation, `FamiliarManager` re-fetches `api.php?what=familiars` and emits `FamiliarSwitched` or `FamiliarEquipped`.

---

## GameEvent Sealed Class Hierarchy

```kotlin
sealed class GameEvent {
    // Adventure
    data class TurnConsumed(val location: AdventureLocation, val result: AdventureResult) : GameEvent()
    data class CombatFinished(val won: Boolean, val monster: String) : GameEvent()
    data class ChoiceResolved(val choiceId: Int, val option: Int) : GameEvent()
    data class AdventureLoopStopped(val reason: StopReason) : GameEvent()

    // Inventory
    data class ItemObtained(val item: InventoryItem) : GameEvent()
    data class ItemConsumed(val itemId: Int, val quantity: Int) : GameEvent()
    data class ItemEquipped(val item: InventoryItem, val slot: String) : GameEvent()
    data class ItemDiscarded(val itemId: Int, val quantity: Int) : GameEvent()
    data class ItemCrafted(val resultItem: InventoryItem) : GameEvent()
    data class MallPurchase(val item: InventoryItem, val meatSpent: Int) : GameEvent()

    // Familiar
    data class FamiliarSwitched(val familiar: FamiliarData) : GameEvent()
    data class FamiliarEquipped(val familiar: FamiliarData, val item: InventoryItem) : GameEvent()
    data class FamiliarHatched(val familiar: FamiliarData) : GameEvent()
}
```

---

## Error Handling

- All operations return `Result<T>` — no exceptions cross subsystem boundaries.
- **Adventure loop interruption:** loop stops and emits `AdventureLoopStopped(reason)` on user cancellation, zero adventures, character death, macro rejection, or unexpected HTTP response. UI shows reason and offers Resume or Edit Macro.
- **Stale state:** after a network error mid-operation, the manager sets its state to `Stale` and shows a refresh prompt rather than displaying potentially incorrect data.
- **Mall errors:** `MallBuyRequest` returns typed `MallError` subtypes (`SoldOut`, `InsufficientMeat`, `StoreClosed`) so the UI can display specific messages.

---

## Testing

- **`AdventureParser`** — pure unit tests against raw HTML fixture strings recording actual KoL response samples. No HTTP.
- **`AdventureManager`** — `MockEngine` replaying recorded response sequences per scenario: combat win, combat loss, non-combat, choice branch A, choice branch B, loop stop conditions. Verifies correct turn count, event emissions, loop termination.
- **`MacroStrategy` lookup** — pure unit test: per-zone override → global default → bundled safe default.
- **`InventoryManager`** and **`FamiliarManager`** — `MockEngine` per operation. Verifies state transitions and correct `GameEvent` emissions after each operation.
- **`GameEventBus`** — verifies events emitted by one manager are received by a subscriber within the same test coroutine scope.
- Existing Phase 1 tests remain green throughout.

---

## Out of Scope for Phase 2

- Full familiar-specific action coverage for all familiars (uncommon ones defer to KoL web)
- Mall store management (listing your own store items, setting prices)
- Clan-related inventory/familiar operations
- Item autosell and autobuy rules (defer to Phase 4)
- ASH-based combat scripts (Phase 5)
