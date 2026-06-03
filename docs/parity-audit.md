# KoLmafia Mobile vs Desktop — Parity Audit

_Generated: 2026-06-03 (updated 2026-06-03 after codebase re-inventory)_

## Scale Comparison

| Metric | Desktop (Java) | Mobile (Kotlin) | Coverage |
|--------|---------------|-----------------|----------|
| Source files | ~1,172 classes | ~208 files | ~18% |
| Lines of code | ~57,000 | ~11,150 | ~20% |
| Test files | 384 | 30 | ~8% |
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
| Choice adventures | `session/ChoiceManager.java` | `adventure/ChoiceRequest.kt` | Generic submit only; handlers in progress |
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
| **Daily resources** | Scattered in `KoLCharacter.java` | `character/DailyResourceTracker.kt` | Dedicated tracker |
| **Event bus** | Ad-hoc listeners | `event/GameEventBus.kt` | Pub/sub for state updates |
| **Location database** | `AdventureDatabase.java` | `location/LocationDatabase.kt` | Zone data from adventures.txt |

---

### Not Implemented in Mobile

| Feature | Desktop size/complexity | Priority |
|---------|------------------------|----------|
| **Choice adventure handlers** | 1,000+ individual handlers in ChoiceManager | **High** — automated runs stall on non-trivial choices |
| **Mood/recovery** | `moods/` 9 classes | **High** — auto-healing is core automation |
| **Goal/stop conditions** | `session/GoalManager.java` | **High** — N-turn loops are dumb without it |
| **Quest tracking** | `persistence/QuestDatabase.java` + manager | **High** — many mechanics gated on quest state |
| **Banish/free fight tracking** | `session/BanishManager.java` | **Medium** — critical resource management |
| **Breakfast/daily resources** | `session/BreakfastManager.java` | **Medium** — daily free resource claims |
| **KoLCharacter depth** | 200+ fields in desktop | **Medium** — 50+ fields now; gaps in daily per-skill counters |
| **Relay server** | `webui/` 20+ decorators, 15 JS/CSS files | **Medium** — powers browser questlines; intentionally skipped |
| **Maximizer** | `maximizer/` 12 classes | **Medium** — stat optimization engine |
| **Ascension mechanics** | `session/AscensionManager.java` | **Medium** — path/class tracking partial |
| **Session logging** | `RequestLogger.java` | **Medium** — debugging/replay |
| **Concoction/crafting logic** | `request/concoction/` 30 classes | **Medium** — concoctions.txt bundled; no recipe execution |
| **Clan dungeons** | Scattered session managers | **Low** |
| **PvP** | `request/` PvP handlers | **Low** |

---

## Deep Gap Analysis

### KoLCharacter (Substantially Addressed)

Desktop [`KoLCharacter.java`](../../../kolmafia/src/net/sourceforge/kolmafia/KoLCharacter.java)
is 6,201 lines tracking everything: all stat subpoints, adventures remaining, inebriety,
fullness, spleen, PvP fights, muscle/mys/mox experience, 12+ equipment slots, intrinsic effects,
path-specific state, Ronin/HC counters, daily per-skill counters, etc.

Mobile `character/KoLCharacter.kt` now tracks **50+ fields** including: all six stat subpoints,
buffed stats, HP/MP with base values, fullness/inebriety/spleen with caps, PvP, Ronin/HC,
all class-specific resources (fury, soulsauce, plumber resources, PP, robot resources,
minstrel level), equipment across 10 slots, enthroned/bjorned familiars, moon state,
campground state, and social flags.

Remaining gaps: per-skill daily cast counters (partially in `DailyResourceTracker`), path-specific
state beyond the basics, some rarely-used stat fields.

### Choice Adventures (Active Gap — In Progress)

Desktop `ChoiceManager` has handlers for ~100 choice IDs with actual automation logic
(in `specialChoiceDecision1` / `specialChoiceDecision2`), plus metadata for 200+ additional
choices in `ChoiceAdventures.java`. Mobile has a generic 22-line `ChoiceRequest` that submits
a choice but applies no intelligence. Automated runs stall on any non-trivial choice
(Spooky Forest, Haunted Bathroom, quest gates, etc.).

**Active work:** Choice adventure handler library is being ported. See
`docs/superpowers/plans/2026-06-03-choice-adventure-handlers.md`.

### Data Files (Addressed)

Desktop ships 51 data files (`adventures.txt`, `equipment.txt`, `modifiers.txt`,
`familiars.txt`, etc. — 64,700+ lines). Mobile now bundles **50 `.txt` data files** under
`shared/src/commonMain/composeResources/files/data/`, including all of: `items.txt`,
`equipment.txt`, `statuseffects.txt`, `familiars.txt`, `modifiers.txt`, `classskills.txt`,
`concoctions.txt`, `monsters.txt`, `adventures.txt`, `encounters.txt`, `coinmasters.txt`,
and 39 others. Kotlin parser classes exist for the high-priority files.

### ASH Interpreter (Impressive but Incomplete)

Desktop `textui/` is 366 classes (~21.8K SLOC) vs. mobile's 14-file interpreter. Specific gaps:

- No file I/O (`file_to_array`, `array_to_file`) — used heavily by community scripts
- No `load_html` / HTTP passthrough
- No `xpath` / HTML parsing functions used by relay scripts
- Game runtime library has ~30 functions; desktop has hundreds
- No interactive CLI (`cli_execute`)

### Modifier System (Present, Depth TBD)

Mobile has a 10-file `modifiers/` package: `ModifierParser.kt`, `ModifierExpression.kt`,
`ModifierValues.kt`, `CurrentModifiers.kt`, and type classes for bitmap/boolean/double/
string/derived modifiers. `modifiers.txt` is bundled. The parser and expression evaluator
exist and are tested.

Desktop's `Modifiers.java` is 1,724 lines plus a 16-class package; mobile coverage is
solid for basic modifier lookup and arithmetic but likely incomplete for edge cases
(outfit set bonuses, path-specific overrides, time-varying modifiers).

---

## Architecture Comparison

| Aspect | Desktop | Mobile | Assessment |
|--------|---------|--------|------------|
| HTTP layer | Raw Java HTTP + custom threading | Ktor coroutines | Mobile is cleaner |
| State management | Static singletons | StateFlow + Koin DI | Mobile is cleaner |
| UI | Swing (aging) | Compose Multiplatform | Mobile is modern |
| Concurrency | Manual threading | Coroutines | Mobile is cleaner |
| Data | 51 `.txt` files parsed at startup | 50 bundled `.txt` files | Parity |
| Testing | 384 test classes, many integration | 30 unit test files | Desktop wins |
| Scripting | Full ASH + CLI | Partial ASH | Desktop wins |
| Events | Ad-hoc listeners | GameEventBus pub/sub | Mobile is cleaner |

---

## Top Priorities

1. **Choice adventure handlers** — Port the handler library so automated runs can navigate
   non-trivial choices (Spooky Forest, quest gates, dungeon branches). _(In progress.)_

2. **Mood/recovery** — Auto-healing and buff maintenance are what make KoLmafia automation
   genuinely useful. Without it, automation stops when HP drops.

3. **Goal/stop conditions** — `GoalManager` lets automation stop when a goal is met (item
   acquired, level reached, etc.). Without it N-turn loops are dumb.

4. **Quest tracking** — `QuestDatabase` + manager needed to gate automation correctly on
   quest state (data files `questscouncil.txt` and `questslog.txt` are already bundled).

5. **KoLCharacter — per-skill daily counters** — Remaining gap in the character model;
   needed for skill-spam automation to respect daily limits.
