# KoLmafia Mobile vs Desktop — Parity Audit

_Generated: 2026-06-03 (updated 2026-06-03 after Phase 5a mood/recovery merge + Phase 5b quest tracking PR)_

## Scale Comparison

| Metric | Desktop (Java) | Mobile (Kotlin) | Coverage |
|--------|---------------|-----------------|----------|
| Source files | ~1,172 classes | ~202 files (commonMain) | ~17% |
| Lines of code | ~57,000 | ~20,300 | ~36% |
| Test files | 384 | 52 | ~14% |
| Build target | JVM 21 | Android + iOS | — |

The mobile app is a focused reimplementation, not a line-for-line port. The gap is wider than raw
file counts suggest because the desktop has massive complexity in its managers and parsers.

---

## Feature Parity Matrix

### Implemented in Mobile

| Feature | Desktop location | Mobile location | Notes |
|---------|-----------------|-----------------|-------|
| Login / session | `request/LoginRequest.java` | `request/LoginRequest.kt` | Parity solid |
| Character stats | `KoLCharacter.java` (6,201 lines) | `character/KoLCharacter.kt` | 70+ fields — see below |
| Adventure loop | `KoLAdventure.java` (4,608 lines) | `adventure/AdventureManager.kt` | N-turn loop; mood + recovery wired |
| Combat macros | `combat/Macrofier.java` | `adventure/MacroStrategy.kt` | Desktop language is richer |
| **Choice adventure handlers** | `session/ChoiceManager.java` | `adventure/choice/` (25 files) | **~80 choice IDs implemented** — see below |
| Inventory | `session/InventoryManager.java` | `inventory/InventoryManager.kt` | Basic use/equip; craft submits but unparsed |
| Familiar management | `FamiliarData.java` (65K) | `familiar/FamiliarManager.kt` | Switch/equip/hatch; no per-familiar logic |
| Skill casting | `session/SkillDatabase.java` | `skill/SkillManager.kt` | Daily limits tracked |
| Active effects | `session/EffectDatabase.java` | `effect/EffectManager.kt` | Display only |
| Chat (clan/PM) | `chat/ChatManager.java` | `chat/ChatManager.kt` | Polling parity |
| Coinmaster shops | `request/coinmaster/` (19 classes) | `shop/CoinmasterRegistry.kt` | 30+ coinmasters |
| Mall search/buy | `request/MallSearchRequest.java` | `mall/MallPriceManager.kt` | TTL cache; `mallSearch()` returns empty (HTML parser TODO) |
| ASH interpreter | `textui/` (366 classes) | `ash/` (14 files) | ~50 functions — see ASH section |
| **Data layer** | `data/*.txt` (51 files, 64K lines) | `composeResources/files/data/` | **50 files bundled; questslog.txt now parsed** |
| **Modifier system** | `Modifiers.java` + 16-class package | `modifiers/` (10 files) | Full passive prediction with class multipliers, path overrides |
| **BuffBot** | `BuffBotManager.java` | `buffbot/` (3 files) | Database + management |
| **Daily resources** | Scattered in `KoLCharacter.java` | `character/DailyResourceTracker.kt` | Item-use + free-fight counters |
| **Event bus** | Ad-hoc listeners | `event/GameEventBus.kt` | Pub/sub for state updates |
| **Location database** | `AdventureDatabase.java` | `location/LocationDatabase.kt` | Zone data from adventures.txt |
| **Goal manager** | `session/GoalManager.java` | `session/GoalManager.kt` | Item goals — **not checked mid-run** (see below) |
| **Quest database** | `persistence/QuestDatabase.java` | `quest/QuestDatabase.kt` + `data/QuestLogDatabase.kt` | 99 quests; text-based step detection (on branch) |
| **Ascension paths** | `AscensionClass.java` + managers | `adventure/AscensionPath.kt` | 37+ paths, consumption flags, avatars |
| **Stop conditions** | `session/` stop logic | `adventure/StopReason.kt` | NoAdventuresLeft, Death, Cancel, Network — no goal/item checks |
| **Mood system** | `moods/` (9 classes, ~3,539 lines) | `mood/` (3 files) | Core automation path; no persistence or named moods |
| **Recovery system** | `moods/RecoveryManager.java` | `recovery/RecoveryManager.kt` | HP/MP item + skill recovery; stop-threshold loop TODO |

---

### Not Implemented in Mobile

| Feature | Desktop size/complexity | Priority |
|---------|------------------------|----------|
| **Goal depth — item goal stop in loop** | `GoalManager` + `StopReason` | **High** — item goals exist but the loop never checks them mid-run |
| **Numeric stop conditions** | Meat, HP, level goal checks in loop | **High** — loop has no "run until X meat" or "run until level N" |
| **Recovery stop-threshold loop** | `hpRecoveryStopPct` / `mpRecoveryStopPct` in `RecoveryManager` | **High** — keys defined; recovery fires once per gap, not until threshold |
| **Mood persistence** | `username_moods.txt` save/load in `MoodManager` | **High** — mood works in-session only; requires re-setup every login |
| **Multiple named moods** | `SortedListModel<Mood>` in desktop `MoodManager` | **Medium** — only one in-memory active mood supported |
| **ManaBurn** | `ManaBurn.java` + `ManaBurnManager.java` (~700 lines) | **Medium** — auto-cast excess MP into buffs before rollover |
| **Quest tracking depth** | Per-quest state machines in `QuestDatabase.java` (1,284 lines) | **Medium** — 99 quests by name; step detection text-based (no per-quest in-code logic) |
| **Banish tracking** | `BanishManager.java` (618 lines) | **Medium** — free-fight counter exists; no banish list |
| **Breakfast/daily automation** | `BreakfastManager.java` (1,000 lines) | **Medium** — `DailyResourceTracker` counts; no execution routine |
| **VillainLair + Rufus solvers** | 4 choice IDs (1260, 1262, 1498, 1499) | **Medium** — TODO stubs in `SolverHandlers.kt` |
| **KoLCharacter depth** | 200+ fields in desktop | **Medium** — 70+ fields now; quest flags, campground detail, storage absent |
| **Relay server** | `webui/` 20+ decorators, 15 JS/CSS files | **Medium** — intentionally skipped |
| **Maximizer** | `maximizer/` 12 classes | **Medium** — stat optimization engine |
| **Ascension mechanics depth** | `session/AscensionManager.java` | **Medium** — path enum complete; reset/completion/unlock tracking absent |
| **Session logging** | `RequestLogger.java` (1,322 lines) | **Medium** — events via GameEventBus only; nothing persisted |
| **Concoction/crafting logic** | `request/concoction/` 32 classes | **Medium** — `craft()` submits but returns placeholder item (id=-1) |
| **HP/MP recovery stop threshold** | `hpRecoveryStopPct`, `mpRecoveryStopPct` | **Low-Medium** — preference keys defined; multi-use recovery loop not implemented |
| **`AdventureManager` step count** | Zone turn count for choice context | **Low-Medium** — always 0; puzzle handlers that track zone turns broken |
| **`cli_execute()` dispatch** | Full CLI command parsing | **Low** — ASH stub echoes but does nothing |
| **`craft()` response parsing** | Parse `craft.php` response | **Low** — returns placeholder; `ItemCrafted` event has id=-1 |
| **`available_amount()` closet/storage** | Closet + storage awareness | **Low** — identical to `item_amount()` |
| **Plumber HP recovery** | `plumberHPRecovery()` path | **Low** |
| **Clan dungeons** | Scattered session managers | **Low** |
| **PvP** | `request/` PvP handlers | **Low** |

---

## Deep Gap Analysis

### Choice Adventure Handlers (Completed — 2026-06-03)

**Status: PRODUCTION-READY**

The full handler library has been implemented in `adventure/choice/` (25 files, ~17 handler test files).
Architecture: `ChoiceHandlerRegistry` dispatches by choice ID → `ChoiceHandler` fun interface
→ `ChoiceContext` (aggregates character/inventory/effects/skills/goals/quest/solver state).
Fallback chain: Handler → Preference → Manual (null).

**~80 active choice IDs covered across 11 handler group files:**

| File | Choice IDs |
|------|-----------|
| `InventoryHandlers.kt` | 5, 7, 127, 161, 191, 298, 305, 504, 553, 558, 692, 786, 791, 1091, 1489 |
| `ResponseTextHandlers.kt` | 155, 575, 678, 705, 808, 919, 923, 973, 975, 1026, 1222, 1461 |
| `StatHandlers.kt` | 89, 162, 184, 700, 1049, 1087 |
| `ComplexHandlers.kt` | 304, 309, 496, 502, 513, 514, 515, 549, 550, 551, 552 |
| `DreadsylvaniaHandlers.kt` | 721, 725, 729, 733, 737, 741, 745, 749, 753 |
| `HiddenCityHandlers.kt` | 780, 781, 783, 785, 787, 789 |
| `MiscHandlers.kt` | 182, 690, 691, 693, 879, 914, 988, 989 |
| `GoalHandlers.kt` | 26, 27 (uses GoalManager) |
| `QuestHandlers.kt` | 1060, 1061 (uses QuestDatabase) |
| `SkillUsesHandlers.kt` | 600, 601 (uses skillUses counter) |
| `SolverHandlers.kt` | 486, 535, 536, 546, 594, 665, 702, 890–903, 1260\*, 1262\*, 1498\*, 1499\* |

\* Marked TODO: VillainLair (1260/1262) and Rufus (1498/1499) solvers not yet implemented.

Desktop `ChoiceManager` covers ~1,000+ cases; mobile covers the ~80 that need automation
logic. The long tail of trivial or preference-driven choices falls through to the
preference/manual fallback correctly.

### Mood / Recovery (Phase 5a — Merged 2026-06-03)

**Status: CORE WORKING; persistence and stop-threshold missing.**

`RecoveryManager.kt` (148 lines) picks the best available HP/MP restore item or skill from
`RestoreDatabase`, uses it once per turn-gap, and re-fetches character state on success.
`MoodManager.kt` (39 lines) evaluates `missingTriggers()` against active effects before each
turn and casts missing buffs via `SkillManager`.

**What's working:** Single active mood in memory; recovery item/skill selection with daily-limit
awareness; HP/MP threshold predicates (`needsHpRecovery`, `needsMpRecovery`); preference keys
`AUTO_RECOVER_HP`, `HP_RECOVERY_TARGET_PCT`, `AUTO_RECOVER_MP`, `MP_RECOVERY_TARGET_PCT`,
`AUTO_BUFF` honored.

**Known gaps:**
- **Stop-threshold loop not implemented** — `HP_RECOVERY_STOP_PCT` / `MP_RECOVERY_STOP_PCT`
  keys are defined but unused. Recovery calls the best-available item/skill once and stops.
  A user at 5% HP may still be below 50% after one large potion because the loop does not
  continue until the stop threshold is reached.
- **No mood persistence** — `activeMood: Mood?` is in-memory. Users must re-configure every
  login. Desktop serializes named moods to `username_moods.txt`.
- **No multiple named moods** — Desktop supports a library of named moods (default, execute,
  autofill, extends). Mobile has one in-memory active mood.
- **No ManaBurn** — Desktop `ManaBurnManager` (~700 lines) casts excess MP into buffs before
  rollover. Not implemented.

### KoLCharacter (Substantially Addressed)

Desktop [`KoLCharacter.java`](../../../kolmafia/src/net/sourceforge/kolmafia/KoLCharacter.java)
is 6,201 lines. Mobile `character/KoLCharacter.kt` wraps a `StateFlow<CharacterState>` with
partial-update methods (`updateHpMp`, `updateMeat`, `updateAdventuresLeft`, `updateEquipment`,
`updateClassResource`, `setIntrinsics`, etc.).

`CharacterState.kt` tracks: all six stat subpoints, buffed stats, HP/MP with base values,
fullness/inebriety/spleen with caps, PvP, Ronin/HC, king-liberated, all class-specific
resources (fury, soulsauce, discoMomentum, audience, absorbs, thunder/rain/lightning, PP,
You Robot energy/scraps, wildfire water, minstrel level), equipment across 10+ slots,
enthroned/bjorned familiars, moon state, campground state, and social flags.
Computed properties cover path restrictions (`canEat`/`canDrink`/`canChew`),
`isFistcore`/`isAxecore`, `isLowHp`/`isLowMp`, rollover countdown, and more.

`DailyResourceTracker.kt` handles per-day item-use counts and free-fight sources that
the API doesn't report directly.

**Remaining gaps:** per-quest flags, telescope monster data, detailed campground state
(garden type/yield, mushroom plot), storage/closet item counts beyond meat totals.

### Goal Manager (Partial — Loop Integration Missing)

`session/GoalManager.kt` tracks item goals (mutable `Set<Int>` of item IDs) with
`addItemGoal`, `removeItemGoal`, `hasItemGoal`, `clearGoals`, `itemGoalIds`.
Used by choice handlers (cases 26, 27, 182, 879) and `StopReason` logic.

**Critical gap: goal is never checked mid-run.** `AdventureManager.runAdventures()` does not
query `goalManager.hasItemGoal()` after items are obtained. The loop runs all N turns even if
the item goal is met on turn 1. A new `StopReason.GoalMet` entry and post-turn item tracking
are needed.

**Also missing:** Numeric stop conditions — meat goals, HP/MP thresholds, level goals.

### Quest Database (Substantially Addressed — on branch)

`quest/Quest.kt` now enumerates **99 quests** across all desktop categories. `QuestDatabase.kt`
has step-ordering (`stepOrdinal`), `isQuestLaterThan`/`isQuestFinished`, `validateStep`, and
`setProgressByPrefKey`. `QuestLogDatabase.kt` parses `questslog.txt` for text-based step
detection. `QuestLogRequest.kt` syncs all three `questlog.php` pages on login and on quest
advancement signals.

**Remaining gaps:** Per-quest in-code state machine logic (e.g., CYRPT step detection from
campground state rather than questlog text). Quests advanced via NPC visits (not adventure
responses) are only captured at next login sync. The handful of quests with special-cased
step detection (Telegram, Party Fair, Doctor Bag, PirateRealm) fall back to "started".

### Data Files (Addressed)

Desktop ships 51 data files (64,700+ lines). Mobile bundles **50 `.txt` files** under
`shared/src/commonMain/composeResources/files/data/`. Kotlin parser classes exist for the
high-priority files, including `questslog.txt` (now parsed by `QuestLogDatabase`).

### ASH Interpreter (~50 Functions)

Desktop `textui/` is 366 classes (~21.8K SLOC) vs. mobile's 14-file interpreter with ~50
registered functions covering: type conversions, string ops, math, aggregate ops, print
variants, character queries (name/level/HP/MP/meat/adventures/fullness/inebriety/spleen/
basestat/in_hardcore/my_familiar), item queries (item_amount, available_amount, to_item,
have_item), skill queries (have_skill, mp_cost, to_skill, daily_limit, times_cast), effect
queries (have_effect, to_effect), and game actions (adventure, use_skill, cli_execute stub).

**Gaps:** `file_to_array`/`array_to_file` (heavily used by community scripts), `load_html`/
`visit_url`, `xpath`, `maximize`, `get_property`/`set_property`, `my_class`/`my_path`,
`is_npc_item`, `mall_price`, `autosell_price`, `cli_execute` (stub only), `available_amount`
ignores closet/storage, and hundreds of game-entity lookup functions.

### Modifier System (Present, Substantial)

Mobile has a 10-file `modifiers/` package. `CurrentModifiers` aggregates all 7 source types
(items, effects, passive skills, zodiac sign, challenge path, familiar, outfits) and
implements `predict()` following the desktop algorithm with class multipliers, equalization,
stat limits, HP/MP class multipliers, buffed-stat floor, and path-specific HP/MP overrides
(Vampyre, Zootomist, You Robot, Grey You).

**Remaining gaps:** conditional outfit set bonuses (half-set bonuses), modifier lookup by
item ID (currently name-only), Synergy modifier type.

---

## Architecture Comparison

| Aspect | Desktop | Mobile | Assessment |
|--------|---------|--------|------------|
| HTTP layer | Raw Java HTTP + custom threading | Ktor coroutines | Mobile is cleaner |
| State management | Static singletons | StateFlow + Koin DI | Mobile is cleaner |
| UI | Swing (aging) | Compose Multiplatform | Mobile is modern |
| Concurrency | Manual threading | Coroutines | Mobile is cleaner |
| Data | 51 `.txt` files parsed at startup | 50 bundled `.txt` files | Parity |
| Testing | 384 test classes, many integration | 52 unit test files | Desktop wins |
| Scripting | Full ASH + CLI | Partial ASH (~50 functions) | Desktop wins |
| Events | Ad-hoc listeners | GameEventBus pub/sub | Mobile is cleaner |
| Choice automation | ~1,000 handler cases | ~80 active IDs covered | Good coverage of common paths |
| Recovery/mood | 9 classes, full persistence | 4 files, in-memory only | Desktop wins on persistence |

---

## Top Priorities

1. **Recovery stop-threshold loop** — `hpRecoveryStopPct` / `mpRecoveryStopPct` are defined
   but the recovery loop fires once and stops. A user at 5% HP after one potion may still be
   at 30%. Change `recoverHp`/`recoverMp` to loop until the stop threshold is met. Low effort,
   high impact — this is a correctness bug in existing recovery.

2. **GoalManager item-goal stop in adventure loop** — Item goals exist but the loop runs
   all N turns regardless. Add `StopReason.GoalMet`, emit `ItemObtained` events from combat
   result parsing, and check `goalManager.hasItemGoal()` after each turn. The "run until I
   get item X" use case is core automation.

3. **Numeric stop conditions** — Add meat/HP/MP/level goals to `GoalManager` and check them
   in the adventure loop. Completes the "run until X" contract.

4. **Mood persistence** — Serialize the active `Mood` to preferences. Eliminates re-setup
   every login. Medium effort with high UX value.

5. **VillainLair + Rufus solvers** — Four TODO stubs in `SolverHandlers.kt` (choice IDs
   1260, 1262, 1498, 1499). Filling these completes the choice handler coverage for
   late-game automation paths.

6. **Banish tracking** — `BanishManager` equivalent to avoid re-encountering banished monsters
   and to correctly count available free fights. The `DailyResourceTracker` counter exists
   but nothing writes to it from combat results.

7. **Breakfast/daily automation** — `BreakfastManager` equivalent to claim free daily
   resources (free fights, semirares, etc.) at session start.

8. **ManaBurn** — Cast excess MP into buffs pre-rollover. High value for mysticality classes;
   moderate implementation effort (~100–200 lines plus test coverage).
