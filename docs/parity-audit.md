# KoLmafia Mobile vs Desktop — Parity Audit

_Generated: 2026-06-03_

## Scale Comparison

| Metric | Desktop (Java) | Mobile (Kotlin) | Coverage |
|--------|---------------|-----------------|----------|
| Source files | ~1,172 classes | ~140 files | ~12% |
| Lines of code | ~57,000 | ~4,000 | ~7% |
| Test files | 384 | 28 | ~7% |
| Build target | JVM 21 | Android + iOS | — |

The mobile app is a focused reimplementation, not a line-for-line port. The gap is wider than raw
file counts suggest because the desktop has massive complexity in its managers and parsers.

---

## Feature Parity Matrix

### Implemented in Mobile

| Feature | Desktop location | Mobile location | Notes |
|---------|-----------------|-----------------|-------|
| Login / session | `request/LoginRequest.java` | `request/LoginRequest.kt` | Parity solid |
| Character stats | `KoLCharacter.java` (6,201 lines) | `character/KoLCharacter.kt` | See gap below |
| Adventure loop | `KoLAdventure.java` (4,608 lines) | `adventure/AdventureManager.kt` | N-turn loop works |
| Combat macros | `combat/Macrofier.java` | `adventure/MacroStrategy.kt` | Desktop language is richer |
| Choice adventures | `session/ChoiceManager.java` | `adventure/ChoiceRequest.kt` | Desktop has 1,000+ handlers |
| Inventory | `session/InventoryManager.java` | `inventory/InventoryManager.kt` | Basic use/equip; crafting partial |
| Familiar management | `FamiliarData.java` (65K) | `familiar/FamiliarManager.kt` | Desktop has per-familiar logic |
| Skill casting | `session/SkillDatabase.java` | `skill/SkillManager.kt` | Daily limits tracked |
| Active effects | `session/EffectDatabase.java` | `effect/EffectManager.kt` | Display only |
| Chat (clan/PM) | `chat/ChatManager.java` | `chat/ChatManager.kt` | Polling parity |
| Coinmaster shops | `request/coinmaster/` (19 classes) | `shop/CoinmasterRegistry.kt` | 30+ coinmasters |
| Mall search/buy | `request/MallSearchRequest.java` | `mall/MallPriceManager.kt` | TTL cache added |
| ASH interpreter | `textui/` (366 classes) | `ash/` (14 files) | See ASH section |

---

### Not Implemented in Mobile

| Feature | Desktop size/complexity | Priority |
|---------|------------------------|----------|
| **Data layer** | 51 `.txt` files, 64,700+ lines | **High** — blocks all offline reasoning |
| **KoLCharacter depth** | `KoLCharacter.java` 6,201 lines, 200+ fields | **High** — automation uses wrong values |
| **Modifier system** | `Modifiers.java` 1,724 lines + 16-class package | **High** — blocks equipment reasoning |
| **Mood/recovery** | `moods/` 9 classes | **High** — auto-healing is core automation |
| **Goal/stop conditions** | `session/GoalManager.java` | **High** — N-turn loops are dumb without it |
| **Quest tracking** | `persistence/QuestDatabase.java` + manager | **High** — many mechanics gated on quest state |
| **Choice adventure handlers** | 1,000+ individual handlers in ChoiceManager | **High** — automated runs stall on non-trivial choices |
| **Relay server** | `webui/` 20+ decorators, 15 JS/CSS files | **Medium** — powers browser questlines |
| **Maximizer** | `maximizer/` 12 classes | **Medium** — stat optimization engine |
| **Banish/free fight tracking** | `session/BanishManager.java` | **Medium** — critical resource management |
| **Breakfast/daily resources** | `session/BreakfastManager.java` | **Medium** — daily free resource claims |
| **Concoction/crafting DB** | `request/concoction/` 30 classes | **Medium** — crafting UI exists, no recipe data |
| **Ascension mechanics** | `session/AscensionManager.java` | **Medium** — path/class tracking partial |
| **Session logging** | `RequestLogger.java` | **Medium** — debugging/replay |
| **Clan dungeons** | Scattered session managers | **Low** |
| **PvP** | `request/` PvP handlers | **Low** |

---

## Deep Gap Analysis

### KoLCharacter (Critical)

Desktop [`KoLCharacter.java`](../../../kolmafia/src/net/sourceforge/kolmafia/KoLCharacter.java)
is 6,201 lines tracking everything: all stat subpoints, adventures remaining, inebriety,
fullness, spleen, PvP fights, muscle/mys/mox experience, 12+ equipment slots, intrinsic effects,
path-specific state, Ronin/HC counters, daily per-skill counters, etc.

Mobile originally tracked ~15 fields. This means automation code silently uses wrong values.

**Addressed in this branch:** expanded to ~40 fields covering all daily counters, equipment
slots, resource caps, and buffed stats.

### Choice Adventures (Critical for Automation)

Desktop `ChoiceManager` has handlers for 1,000+ individual choice adventures — each KoL choice
adventure has unique logic. Mobile has a generic `ChoiceRequest` that can submit a choice, but
without per-choice handling logic, automated runs stall on any non-trivial choice (Spooky
Forest, Haunted Bathroom, etc.).

### Data Files (Structural Gap → Partially Addressed)

Desktop ships 51 data files (`adventures.txt`, `equipment.txt`, `modifiers.txt`,
`familiars.txt`, etc. — 64,700+ lines). Mobile originally had no data layer.

**Addressed in this branch:** bundled `items.txt`, `statuseffects.txt`, and `equipment.txt`
as compose resources with Kotlin parser classes (`ItemDatabase`, `EffectDatabase`,
`EquipmentDatabase`). Remaining high-value files: `modifiers.txt`, `familiars.txt`,
`classskills.txt`, `concoctions.txt`.

### ASH Interpreter (Impressive but Incomplete)

Desktop `textui/` is 366 classes (~21.8K SLOC) vs. mobile's 14-file interpreter. Specific gaps:

- No file I/O (`file_to_array`, `array_to_file`) — used heavily by community scripts
- No `load_html` / HTTP passthrough
- No `xpath` / HTML parsing functions used by relay scripts
- Game runtime library has ~30 functions; desktop has hundreds
- No interactive CLI (`cli_execute`)

### Modifier / Equipment System (Automation Blocker)

Without `Modifiers.java` and the modifier database, there's no way to know what stat bonuses
an item provides. This blocks:

- The Maximizer (equipment optimization)
- Mood system (knowing if an effect provides enough +combat)
- Any combat strategy that adapts to player stats

---

## Architecture Comparison

| Aspect | Desktop | Mobile | Assessment |
|--------|---------|--------|------------|
| HTTP layer | Raw Java HTTP + custom threading | Ktor coroutines | Mobile is cleaner |
| State management | Static singletons | StateFlow + Koin DI | Mobile is cleaner |
| UI | Swing (aging) | Compose Multiplatform | Mobile is modern |
| Concurrency | Manual threading | Coroutines | Mobile is cleaner |
| Data | 51 `.txt` files parsed at startup | 3 bundled (partial) | Desktop wins |
| Testing | 384 test classes, many integration | 28 unit test files | Desktop wins |
| Scripting | Full ASH + CLI | Partial ASH | Desktop wins |

---

## Top Priorities

1. **Data layer** — Bundle and parse core data files (items, effects, equipment). Enables
   offline item reasoning, type classification, autosell prices. _(Partially implemented.)_

2. **KoLCharacter depth** — Track all daily counters, equipment slots, resource limits.
   Without this, any automation that reads character state may silently use wrong values.
   _(Implemented.)_

3. **Modifier system** — A stripped-down `ModifierDatabase` is needed for any meaningful
   equipment advice or combat stat calculation.

4. **Choice adventure handling** — Even a small set of handlers (Spooky Forest, key dungeons)
   would unblock most automated runs.

5. **Mood/recovery** — Auto-healing and buff maintenance are what make KoLmafia automation
   genuinely useful.

6. **Goal/stop conditions** — `GoalManager` lets automation stop when a goal is met (item
   acquired, level reached, etc.). Without it N-turn loops are dumb.
