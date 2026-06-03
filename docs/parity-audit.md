# KoLmafia Mobile vs Desktop — Parity Audit

_Generated: 2026-06-03 (updated 2026-06-03 after choice-handler implementation complete)_

## Scale Comparison

| Metric | Desktop (Java) | Mobile (Kotlin) | Coverage |
|--------|---------------|-----------------|----------|
| Source files | ~1,172 classes | ~261 files | ~22% |
| Lines of code | ~57,000 | ~15,000+ | ~26% |
| Test files | 384 | 47 | ~12% |
| Build target | JVM 21 | Android + iOS | — |

The mobile app is a focused reimplementation, not a line-for-line port. The gap is wider than raw
file counts suggest because the desktop has massive complexity in its managers and parsers.

---

## Feature Parity Matrix

### Implemented in Mobile

| Feature | Desktop location | Mobile location | Notes |
|---------|-----------------|-----------------|-------|
| Login / session | `request/LoginRequest.java` | `request/LoginRequest.kt` | Parity solid |
| Character stats | `KoLCharacter.java` (6,201 lines) | `character/KoLCharacter.kt` | 50+ fields — see below |
| Adventure loop | `KoLAdventure.java` (4,608 lines) | `adventure/AdventureManager.kt` | N-turn loop works |
| Combat macros | `combat/Macrofier.java` | `adventure/MacroStrategy.kt` | Desktop language is richer |
| **Choice adventure handlers** | `session/ChoiceManager.java` | `adventure/choice/` (25 files) | **~80 choice IDs implemented** — see below |
| Inventory | `session/InventoryManager.java` | `inventory/InventoryManager.kt` | Basic use/equip; crafting partial |
| Familiar management | `FamiliarData.java` (65K) | `familiar/FamiliarManager.kt` | Desktop has per-familiar logic |
| Skill casting | `session/SkillDatabase.java` | `skill/SkillManager.kt` | Daily limits tracked |
| Active effects | `session/EffectDatabase.java` | `effect/EffectManager.kt` | Display only |
| Chat (clan/PM) | `chat/ChatManager.java` | `chat/ChatManager.kt` | Polling parity |
| Coinmaster shops | `request/coinmaster/` (19 classes) | `shop/CoinmasterRegistry.kt` | 30+ coinmasters |
| Mall search/buy | `request/MallSearchRequest.java` | `mall/MallPriceManager.kt` | TTL cache added |
| ASH interpreter | `textui/` (366 classes) | `ash/` (14 files) | See ASH section |
| **Data layer** | `data/*.txt` (51 files, 64K lines) | `composeResources/files/data/` | **50 files bundled** |
| **Modifier system** | `Modifiers.java` + 16-class package | `modifiers/` (10 files) | Parser + evaluator present |
| **BuffBot** | `BuffBotManager.java` | `buffbot/` (3 files) | Database + management |
| **Daily resources** | Scattered in `KoLCharacter.java` | `character/DailyResourceTracker.kt` | Item-use + free-fight counters |
| **Event bus** | Ad-hoc listeners | `event/GameEventBus.kt` | Pub/sub for state updates |
| **Location database** | `AdventureDatabase.java` | `location/LocationDatabase.kt` | Zone data from adventures.txt |
| **Goal manager** | `session/GoalManager.java` | `session/GoalManager.kt` | Item goals only — see below |
| **Quest database** | `persistence/QuestDatabase.java` | `quest/QuestDatabase.kt` | MEATSMITH + ARMORER only — see below |
| **Ascension paths** | `AscensionClass.java` + managers | `adventure/AscensionPath.kt` | 37+ paths, consumption flags, avatars |
| **Stop conditions** | `session/` stop logic | `adventure/StopReason.kt` | NoAdventuresLeft, Death, Cancel, Network |

---

### Not Implemented in Mobile

| Feature | Desktop size/complexity | Priority |
|---------|------------------------|----------|
| **Mood/recovery** | `moods/` 9 classes | **High** — auto-healing is core automation; loop stops when HP drops |
| **Goal depth** | `GoalManager` meat/HP/MP stop conditions | **High** — item goals exist; numeric stop conditions do not |
| **Quest tracking depth** | `persistence/QuestDatabase.java` + manager (200+ quests) | **High** — only MEATSMITH/ARMORER ported; most quest gates unimplemented |
| **Banish tracking** | `session/BanishManager.java` | **Medium** — free-fight counter infrastructure exists; no banish list |
| **Breakfast/daily automation** | `session/BreakfastManager.java` | **Medium** — `DailyResourceTracker` counts; no execution routine |
| **KoLCharacter depth** | 200+ fields in desktop | **Medium** — 50+ fields now; per-skill daily cast counters partial |
| **Relay server** | `webui/` 20+ decorators, 15 JS/CSS files | **Medium** — powers browser questlines; intentionally skipped |
| **Maximizer** | `maximizer/` 12 classes | **Medium** — stat optimization engine |
| **Ascension mechanics depth** | `session/AscensionManager.java` | **Medium** — path enum complete; reset/completion/unlock tracking absent |
| **Session logging** | `RequestLogger.java` | **Medium** — events via GameEventBus only; nothing persisted |
| **Concoction/crafting logic** | `request/concoction/` 30 classes | **Medium** — concoctions.txt bundled; no recipe execution or ingredient checks |
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

Supporting systems ported alongside: `GoalManager`, `QuestDatabase`, `AscensionPath`,
`OutfitPool`, `ItemPool`, `EffectPool`, six injectable mini-game solver interfaces
(`SafetyShelterSolver`, `VampOutSolver`, `ArcadeGameSolver`, `LostKeySolver`,
`GameproSolver`, `LightsOutSolver`) with NoOp defaults.

Desktop `ChoiceManager` covers ~1,000+ cases; mobile covers the ~80 that need automation
logic. The long tail of trivial or preference-driven choices falls through to the
preference/manual fallback correctly.

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

Remaining gaps: per-skill daily cast counters (partially in `DailyResourceTracker`),
path-specific state beyond the basics, some rarely-used stat fields.

### Goal Manager (Partial)

`session/GoalManager.kt` tracks item goals (mutable Set<Int> of item IDs) with
`addItemGoal`, `removeItemGoal`, `hasItemGoal`, `clearGoals`, `itemGoalIds`.
Used by choice handlers (cases 26, 27, 182, 879) and `StopReason` logic.

**Missing:** Numeric stop conditions — meat goals, HP/MP thresholds, level goals, and
the general-purpose `GoalManager.isGoalMet()` that the desktop uses to halt the
adventure loop mid-run.

### Quest Database (Skeleton Only)

`quest/Quest.kt` enumerates MEATSMITH and ARMORER. `quest/QuestDatabase.kt` is
preferences-backed with progress step ordering ("unstarted" < "started" < "step1"..."stepN"
< "finished") and `isQuestLaterThan` / `isQuestFinished` APIs.

Desktop has 200+ tracked quests. Mobile has 2. The questscouncil.txt and questslog.txt
data files are bundled — the data exists but the enum and update parsing are not written.

### Data Files (Addressed)

Desktop ships 51 data files (64,700+ lines). Mobile bundles **50 `.txt` files** under
`shared/src/commonMain/composeResources/files/data/`. Kotlin parser classes exist for the
high-priority files.

### ASH Interpreter (Impressive but Incomplete)

Desktop `textui/` is 366 classes (~21.8K SLOC) vs. mobile's 14-file interpreter. Gaps:

- No file I/O (`file_to_array`, `array_to_file`) — used heavily by community scripts
- No `load_html` / HTTP passthrough
- No `xpath` / HTML parsing functions used by relay scripts
- Game runtime library has ~30 functions; desktop has hundreds
- No interactive CLI (`cli_execute`)

### Modifier System (Present, Depth TBD)

Mobile has a 10-file `modifiers/` package with parser, expression evaluator, and type classes.
`modifiers.txt` is bundled and tested. Coverage is solid for basic lookup and arithmetic
but likely incomplete for outfit set bonuses, path-specific overrides, and time-varying
modifiers. Desktop's `Modifiers.java` is 1,724 lines plus a 16-class package.

---

## Architecture Comparison

| Aspect | Desktop | Mobile | Assessment |
|--------|---------|--------|------------|
| HTTP layer | Raw Java HTTP + custom threading | Ktor coroutines | Mobile is cleaner |
| State management | Static singletons | StateFlow + Koin DI | Mobile is cleaner |
| UI | Swing (aging) | Compose Multiplatform | Mobile is modern |
| Concurrency | Manual threading | Coroutines | Mobile is cleaner |
| Data | 51 `.txt` files parsed at startup | 50 bundled `.txt` files | Parity |
| Testing | 384 test classes, many integration | 47 unit test files | Desktop wins |
| Scripting | Full ASH + CLI | Partial ASH | Desktop wins |
| Events | Ad-hoc listeners | GameEventBus pub/sub | Mobile is cleaner |
| Choice automation | ~1,000 handler cases | ~80 active IDs covered | Good coverage of common paths |

---

## Top Priorities

1. **Mood/recovery** — Auto-healing and buff maintenance are what make KoLmafia automation
   genuinely useful. Without it, the adventure loop halts whenever HP drops. `moods/` has
   9 desktop classes; mobile has nothing. This is the single highest-leverage missing feature.

2. **Goal depth — numeric stop conditions** — Item goals exist via `GoalManager`. Adding
   meat/HP/MP/level stop conditions completes the "run until X" contract. The infrastructure
   is there; the evaluator and adventure-loop integration are not.

3. **Quest tracking expansion** — Only MEATSMITH/ARMORER are tracked. Expanding to the
   ~20–30 most automation-relevant quests (main council quests, tavern, wizard, etc.) is
   needed to correctly gate choice handlers and automation decisions. questscouncil.txt
   is already bundled.

4. **Banish tracking** — `BanishManager` needed to avoid re-encountering banished monsters
   and to correctly count available free fights. The `DailyResourceTracker` counter exists
   but nothing writes to it from combat results.

5. **Breakfast/daily automation** — `BreakfastManager` equivalent to claim free daily
   resources (free fights, semirares, etc.) at session start. `DailyResourceTracker`
   can count them once the execution logic exists.

6. **VillainLair + Rufus solvers** — Two TODO stubs in `SolverHandlers.kt` (choice IDs
   1260, 1262, 1498, 1499). Filling these completes the choice handler coverage for
   late-game automation paths.
