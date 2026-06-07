# KoLmafia Mobile vs Desktop — Parity Audit

_Generated: 2026-06-03 (updated 2026-06-06 after Phase 9: Breakfast Automation + BanishManager Completion — PR #10)_

## Scale Comparison

| Metric | Desktop (Java) | Mobile (Kotlin) | Coverage |
|--------|---------------|-----------------|----------|
| Source files | ~1,172 classes | ~213 files (commonMain) | ~18% |
| Lines of code | ~57,000 | ~13,500 (commonMain) | ~24% |
| Test files | 384 | 60 | ~16% |
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
| Adventure loop | `KoLAdventure.java` (4,608 lines) | `adventure/AdventureManager.kt` | N-turn loop; mood + recovery + ManaBurn + goal + banish checks wired |
| Combat macros | `combat/Macrofier.java` | `adventure/MacroStrategy.kt` | Desktop language is richer |
| **Choice adventure handlers** | `session/ChoiceManager.java` | `adventure/choice/` (25 files) | **~80 choice IDs implemented** — see below |
| Inventory | `session/InventoryManager.java` | `inventory/InventoryManager.kt` | Basic use/equip; craft submits but unparsed |
| Familiar management | `FamiliarData.java` (65K) | `familiar/FamiliarManager.kt` | Switch/equip/hatch; no per-familiar logic |
| Skill casting | `session/SkillDatabase.java` | `skill/SkillManager.kt` | Daily limits tracked |
| Active effects | `session/EffectDatabase.java` | `effect/EffectManager.kt` | Display only |
| Chat (clan/PM) | `chat/ChatManager.java` | `chat/ChatManager.kt` | Polling parity |
| Coinmaster shops | `request/coinmaster/` (19 classes) | `shop/CoinmasterRegistry.kt` | 30+ coinmasters |
| Mall search/buy | `request/MallSearchRequest.java` | `mall/MallPriceManager.kt` | TTL cache; `mallSearch()` returns empty (HTML parser TODO) |
| ASH interpreter | `textui/` (366 classes) | `ash/` (14 files) | ~54 functions — see ASH section |
| **Data layer** | `data/*.txt` (51 files, 64K lines) | `composeResources/files/data/` | **50 files bundled; questslog.txt now parsed** |
| **Modifier system** | `Modifiers.java` + 16-class package | `modifiers/` (10 files) | Full passive prediction with class multipliers, path overrides |
| **BuffBot** | `BuffBotManager.java` | `buffbot/` (3 files) | Database + management |
| **Daily resources** | Scattered in `KoLCharacter.java` | `character/DailyResourceTracker.kt` | Item-use + free-fight counters |
| **Event bus** | Ad-hoc listeners | `event/GameEventBus.kt` | Pub/sub for state updates |
| **Location database** | `AdventureDatabase.java` | `location/LocationDatabase.kt` | Zone data from adventures.txt |
| **Goal manager** | `session/GoalManager.java` | `session/GoalManager.kt` | Item (by ID + name), meat, level goals; checked mid-run ✅ |
| **Quest database** | `persistence/QuestDatabase.java` | `quest/QuestDatabase.kt` + `data/QuestLogDatabase.kt` | 99 quests; text-based step detection |
| **Ascension paths** | `AscensionClass.java` + managers | `adventure/AscensionPath.kt` | 37+ paths, consumption flags, avatars |
| **Stop conditions** | `session/` stop logic | `adventure/StopReason.kt` | NoAdventuresLeft, Death, Cancel, Network, GoalMet, **AllMonstersBanished** ✅ |
| **Mood system** | `moods/` (9 classes, ~3,539 lines) | `mood/` (5 files — Phase 7+8) | Core automation path; named mood library + persistence in PR #8 |
| **Recovery system** | `moods/RecoveryManager.java` | `recovery/RecoveryManager.kt` | HP/MP item + skill recovery; stop-threshold loop ✅ |
| **ManaBurn** *(PR #7)* | `moods/ManaBurnManager.java` | `mood/ManaBurnManager.kt` | Post-turn MP burn into lowest-duration mood effect; enabled via pref |
| **Mood persistence** *(PR #7)* | `username_moods.txt` | `MoodManager` + Preferences | Single active mood saved/loaded across login; pipe-delimited pref format |
| **`my_familiar()` fix** *(PR #7)* | `RuntimeLibrary.java` | `ash/GameRuntimeLibrary.kt` | Fixed: now uses `hasFamiliar` (familiarId > 0) not player name |
| **Auto-clear malignant effects** *(PR #8)* | `MoodManager.removeMalignantEffects()` | `mood/MalignantEffects.kt` + `MoodManager.removeMalignantEffects()` | 9 effect names (Beaten Up + 5 poisons + 3 others); fires every mood pass via `UneffectRequest`; best-effort (continues on network failure) |
| **UneffectRequest** *(PR #8)* | `UseSkillRequest` + CLI `uneffect` | `request/UneffectRequest.kt` | HTTP wrapper for `uneffect.php`; Result-typed, status-validated |
| **Mood library** *(PR #8)* | `MoodManager._moods` SortedListModel + `username_moods.txt` | `MoodManager.moodLibrary` + Preferences | Named mood persistence; `addMoodToLibrary`, `removeMoodFromLibrary`, `setActiveMoodByName`, `saveMoodLibrary`, `loadMoodLibrary`; restored on login. Orphaned `moodTriggers_$name` keys cleaned on removal |
| **BanishManager** *(PR #8 + PR #10)* | `session/BanishManager.java` (618 lines, 55+ banishers) | `banish/` (3 files: Banisher, BanishState, BanishManager) | 20 named banishers; identity detected from combat HTML (20 patterns); zone pre-flight routing wired; daycount-gated rollover; `getActiveBanishes()` for ASH; `isBanished` drives `AllMonstersBanished` stop |
| **MonsterBanished event** *(PR #8)* | `BanisherUsed` KoLmafia event | `event/GameEvent.MonsterBanished` | Emitted from `AdventureManager` when combat banish detected; now includes banisher identity |
| **BreakfastManager** *(PR #10)* | `BreakfastManager.java` (1,000 lines, 20+ actions) | `session/BreakfastManager.kt` | Garden harvest, clan rumpus, VIP lounge (Klaw ×3, pool, looking glass, fireworks), guild manual detection; daycount-gated via `LAST_DAYCOUNT`; idempotent via per-action boolean prefs |
| **CampgroundRequest** *(PR #10)* | `request/CampgroundRequest.java` | `request/CampgroundRequest.kt` | `harvestGarden()` HTTP wrapper |
| **ClanRumpusRequest** *(PR #10)* | `request/ClanRumpusRequest.java` | `request/ClanRumpusRequest.kt` | `visit()` HTTP wrapper |
| **ClanLoungeRequest** *(PR #10)* | `request/ClanLoungeRequest.java` | `request/ClanLoungeRequest.kt` | `useKlaw()`, `useLookingGlass()`, `visitFireworks()`, `playPoolGame()` |
| **CombatDatabase / ZoneLookup** *(PR #10)* | `AdventureDatabase.java` monster weights | `data/ZoneLookup.kt` + `data/CombatDatabase.kt` | Zone→monster list lookup used by banish zone pre-flight |
| **`is_banished()` + `banishers_used()`** *(PR #10)* | `RuntimeLibrary.java` banish queries | `ash/GameRuntimeLibrary.kt` `registerBanishQueries()` | `is_banished(monster)`, `is_banished(string)`, `banishers_used() → string[monster]`, `to_monster(string)` |

---

### Not Implemented in Mobile

| Feature | Desktop size/complexity | Priority |
|---------|------------------------|----------|
| **BreakfastManager completion** | Guild manual HTTP use (detection-only stub); pocket wishes (choice handling deferred); hermit clovers, hardwood, Mr Store, boxing daydream, toy uses, batteries, skill books, outfit checkpoint | **High** — core is in (PR #10), gaps are the long tail |
| **BanishManager coverage** | Desktop has 55+ banishers; mobile has 20 from combat HTML patterns. Also: no phylum banishing (Breathitin, Out of the Frying Pan); no queue model | **Medium** — routing is wired; adding banishers is incremental work |
| **VillainLair + Rufus solvers** | 4 choice IDs (1260, 1262, 1498, 1499) | **Medium** — TODO stubs; VillainLair is string matching (~50 lines); Rufus needs `RufusManager` (~200 lines) |
| **Quest tracking depth** | Per-quest state machines in `QuestDatabase.java` (1,284 lines) | **Medium** — 99 quests by name; step detection text-based; NPC-visit advances missed until next login |
| **KoLCharacter depth** | 200+ fields in desktop | **Medium** — 70+ fields; quest flags, campground detail, storage absent |
| **Relay server** | `webui/` 20+ decorators, 15 JS/CSS files | **Medium** — intentionally skipped |
| **Maximizer** | `maximizer/` 12 classes (~5,877 lines) | **Medium** — stat optimization engine; very high complexity |
| **Ascension mechanics depth** | `session/AscensionManager.java` | **Medium** — path enum complete; reset/completion/unlock tracking absent |
| **Session logging** | `RequestLogger.java` (1,322 lines) | **Medium** — events via GameEventBus only; nothing persisted |
| **Concoction/crafting logic** | `request/concoction/` 32 classes | **Medium** — `craft()` submits but returns placeholder item (id=-1) |
| **ManaBurn sophistication gap** | Desktop burns *any* castable buff, not just mood effects; respects `allowNonMoodBurning`, `allowSummonBurning`, `manaBurnSummonThreshold`, per-skill priority prefs, `lastChanceBurn` | **Medium** — mobile ManaBurn covers the core case (mood effects); advanced pref-driven burning absent |
| **Mood inheritance** | `Mood.java` parses `"moodA extends default"` and comma-list multi-mood names | **Medium** — mobile `Mood` is a flat data class; no parent concept |
| **AT song limit management** | MoodManager auto-`uneffect`s lowest-priority AT song when slot is full | **Medium** — no song tracking in mobile |
| **GoalManager special stops** | `GOAL_CHOICE`, `GOAL_AUTOSTOP`, `GOAL_FACTOID`, `GOAL_FLOUNDRY`, `GOAL_LEPRECONDO`, `GOAL_SUBSTATS` | **Low-Medium** — mobile only has item/meat/level/banished goals |
| **`AdventureManager` step count** | Zone turn count for choice context | **Low-Medium** — always 0; puzzle handlers that track zone turns broken |
| **Pre-adventure zone checks** | `KoLAdventure.java` validates limit mode, outfit requirements, familiar requirements, zone-specific pre-flight (Tavern, Pyramid, Bat Hole, etc.) | **Low-Medium** — mobile sends raw adventure request with no zone pre-checks (banish pre-flight is now wired, but outfit/familiar/limit-mode checks absent) |
| **Mood auto-fill** | `minimalSet()` and `maximalSet()` in `MoodManager` | **Low** — no way to populate a mood from currently-active effects or available skills |
| **`cli_execute()` real dispatch** | Full CLI command parsing | **Low** — ASH stub echoes but does nothing; `mood execute`, `set key=value`, `get key` patterns call-sites fail silently |
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

**Choice solver stubs:** All 6 solver interfaces (`ArcadeGameSolver`, `GameproSolver`,
`LightsOutSolver`, `LostKeySolver`, `SafetyShelterSolver`, `VampOutSolver`) are `NoOp`
implementations. Without real implementations, encounters with choice IDs 486, 535, 536, 546,
594, 665, 890–903 fall through to `preferences.getInt("choiceAdventure$choiceId", 0)`,
defaulting to option 1. This causes unpredictable behavior in Dreadsylvania, Safety Shelter,
and the Lights Out puzzle chain.

### Mood / Recovery (Phase 5a + Phase 6 + Phase 7 + Phase 8)

**Status: CORE WORKING + STOP-THRESHOLD LOOP + MANABURN + PERSISTENCE + NAMED LIBRARY + MALIGNANT CLEARING (PR #8).
Mood inheritance, AT song management, and full ManaBurn breadth absent.**

`RecoveryManager.kt` picks the best available HP/MP restore item or skill from
`RestoreDatabase`. The adventure loop in `AdventureManager` calls `recoverIfNeeded` in an
up-to-10-iteration while-loop, checking `hpAboveStopThreshold`/`mpAboveStopThreshold` after
each recovery action and breaking when both are met. After recovery, `ManaBurnManager.burnIfEnabled`
fires in its own up-to-10-iteration loop, casting the skill from the active mood trigger list
whose corresponding effect has the fewest remaining turns, until MP falls below `MANA_BURN_MIN_MP_PCT`
(default 90%) or no eligible skill remains. `MoodManager.kt` evaluates `missingTriggers()` against
active effects before each turn and casts missing buffs via `SkillManager`.

**What's working (after Phase 8 merges):**
- Single active mood with full trigger evaluation
- Mood library — named mood profiles with `addMoodToLibrary`, `removeMoodFromLibrary`, `setActiveMoodByName`
- Mood library + active mood persistence across login (save/load via `MOOD_LIBRARY_NAMES` + dynamic `moodTriggers_$name` keys)
- Malignant effect auto-clearing — 9 effect names (Beaten Up, Tetanus, Amnesia, Cunctatitis, all 5 poison variants) uneffected before every mood pass via `UneffectRequest`; best-effort (network failures don't abort mood pass)
- ManaBurn post-turn loop (enabled/disabled pref; threshold pref; capped at 10 iterations)
- Recovery item/skill selection with daily-limit awareness
- HP/MP threshold predicates (`needsHpRecovery`, `needsMpRecovery`)
- Preference keys `AUTO_RECOVER_HP`, `HP_RECOVERY_TARGET_PCT`, `HP_RECOVERY_STOP_PCT`,
  `AUTO_RECOVER_MP`, `MP_RECOVERY_TARGET_PCT`, `MP_RECOVERY_STOP_PCT`, `AUTO_BUFF` honored

**Known gaps:**
- **No mood inheritance** — Desktop `Mood.java` parses `"moodA extends default"` and
  comma-list names to merge parent trigger lists at evaluation time.
- **No AT song management** — Desktop auto-evicts the lowest-priority Accordion Thief song
  when the song slot is full before casting a new one.
- **Mobile ManaBurn is narrower** — Desktop `ManaBurnManager.java` burns *any* castable buff
  (not just mood-trigger effects), respects `allowNonMoodBurning`, `allowSummonBurning`,
  `manaBurnSummonThreshold`, per-skill priority prefs, and `lastChanceBurn`/`lastChanceThreshold`.
  Mobile only burns the shortest-duration mood-trigger effect.
- **`removeMalignantEffects` defaults to `true`** — Desktop `defaults.txt` has `removeMalignantEffects false`.
  Mobile's default (true) means it will attempt to auto-remove effects out of the box. This may be
  the desired mobile behavior (more beginner-friendly) but diverges from desktop; confirm intent.

### Goal Manager + Adventure Loop Stop (Phase 6 — Merged 2026-06-06)

**Status: CORE COMPLETE. Six special stop-types from desktop `GoalManager` absent.**

Mobile `GoalManager.kt` covers: item goals by ID, item goals by name, meat goal, level goal.

Desktop `GoalManager.java` additionally has: `GOAL_CHOICE` (stop at next choice),
`GOAL_AUTOSTOP` (stop on specific text), `GOAL_FACTOID` (stop on factoid acquisition),
`GOAL_FLOUNDRY` (stop at Clan Floundry), `GOAL_LEPRECONDO` (stop at Leprecondo), and
`GOAL_SUBSTATS` (stop when substat gain ≥ target). These are low-priority for general
automation but are used by some community scripts.

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

**Remaining gaps:** per-quest flags, telescope monster data, detailed campground state
(garden type/yield, mushroom plot), storage/closet item counts beyond meat totals.

### Banish Tracking (PR #8 foundation + PR #10 completion)

Desktop `session/BanishManager.java` (618 lines) tracks which monsters have been banished,
by which banisher, with what turn-reset or rollover-reset semantics. 55+ named banishers,
per-banisher turn-counts, rollover/turn/avatar reset types, phylum banishing, and a queue model.
State is serialized to the `banishedMonsters` preference string. Exposes `isBanished(monster)`
for the combat advisor and `BanisherUsed` events for daily tracking.

**Mobile status (PR #8 + PR #10): Substantially complete — routing wired, identity detected, daycount gated.**

`banish/` package (3 files):
- `Banisher.kt` — 20 named banishers with `canonicalName`, `turns`, `ResetType` (`ROLLOVER/TURNS/TURN_ROLLOVER/AVATAR/NEVER`), `isTurnFree`
- `BanishState.kt` — `BanishedMonster` (name, banisher, turnBanished) with `isExpired()` logic
- `BanishManager.kt` — StateFlow-backed; `banishMonster()`, `isBanished()`, `getActiveBanishes()`, `clearExpiredAndRollover()`, `save()`, `load()` via Preferences

**What PR #10 added:**
- `AdventureParser` detects banisher identity from combat HTML via 20 `BANISHER_PATTERNS` (text substring → Banisher); `AdventureResult.Combat` carries `banisher` field
- `AdventureManager` zone pre-flight: before each turn, if all positive-weight monsters in zone are banished, emits `StopReason.AllMonstersBanished` and halts
- `SessionManager` now daycount-gates rollover: `clearExpiredAndRollover` only fires when `dayCount != LAST_DAYCOUNT` — same-day app restarts no longer wipe valid TURN_ROLLOVER banishes
- `getActiveBanishes(currentTurn)` method enables the `banishers_used()` ASH function

**Remaining gaps:**
1. **35 missing banishers** — Desktop has 55+; mobile covers 20 from combat HTML patterns. The 35 missing are lower-frequency items/skills (e.g., Feel Hatred, Monkey Slap, Show your Work, etc.)
2. **No phylum banishing** — Desktop tracks Breathitin / Out of the Frying Pan etc. by phylum, not monster name. Mobile has no phylum concept in BanishManager.
3. **No queue model** — Desktop allows multiple simultaneous banishes of the same monster (e.g., snokebomb + ice house); mobile replaces the entry. Edge case only.

### Quest Database (Substantially Addressed)

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

### ASH Interpreter (~54 Functions — 93% Gap)

Desktop `textui/RuntimeLibrary.java` registers **835 `LibraryFunction` instances**. Mobile
`ash/GameRuntimeLibrary.kt` registers approximately **54 distinct function overloads** — roughly
6% coverage. The gap is the single largest remaining feature deficit.

**What's working (after Phase 9):** type conversions, string ops, math, aggregate ops, print variants,
character queries (my_name, my_level, my_hp, my_maxhp, my_mp, my_maxmp, my_meat, my_adventures,
my_fullness, my_inebriety, my_spleen_use, my_basestat, in_hardcore, my_familiar ✅ fixed),
item queries (item_amount, available_amount, to_item, have_item), skill queries (have_skill,
mp_cost, to_skill, daily_limit, times_cast), effect queries (have_effect, to_effect), game
actions (adventure, use_skill, cli_execute stub), **banish queries (to_monster, is_banished ×2 overloads, banishers_used) ✅ new in PR #10**.

**Missing by category:**

| Category | Key absent functions |
|----------|---------------------|
| Web scripting | `visit_url`, `load_html`, `form_field`, `write`, `writeln` |
| Collection accessors | `get_inventory`, `get_closet`, `get_storage`, `get_display`, `get_stash` |
| Economy actions | `buy`, `sell`, `create`, `craft`, `eat`, `drink`, `chew`, `overdrink`, `use` |
| Inventory movement | `retrieve_item`, `autosell`, `hermit`, `put_closet`, `take_storage` |
| Pricing | `mall_price`, `historical_price`, `npc_price`, `autosell_price` |
| Adventure prep | `can_adventure`, `prepare_for_adventure`, `set_location` |
| Goal management | `add_item_condition`, `remove_item_condition`, `goal_exists`, `get_goals` |
| Mood integration | `get_moods`, `mood_list` |
| Date/time | `today_to_string`, `now_to_string`, `rollover`, `moon_phase`, `gameday_to_string` |
| Character queries | `my_class`, `my_path`, `my_sign`, `my_thrall`, `my_primestat`, `in_run`, `under_standard`, `ascension_number`, `can_interact` |
| Familiar queries | `my_familiar_weight`, `familiar_weight`, `have_familiar`, `to_familiar`, `use_familiar` |
| Equipment queries | `equipped_item`, `have_equipped`, `outfit`, `have_outfit`, `retrieve_outfit` |
| Modifier queries | `numeric_modifier`, `boolean_modifier`, `string_modifier` |
| Combat helpers | `in_multi_fight`, `fight_follows_choice`, `last_monster`, `copiers_used` |
| Type conversions | `to_location`, `to_familiar`, `to_modifier`, `to_path`, `to_vykea` |
| CLI | `cli_execute` (stub only — echoes, no real dispatch) |

### Breakfast / Daily Automation (PR #10 — Partial)

Desktop `session/BreakfastManager.java` (1,000 lines) automates ~20 one-time-per-day actions:
clan rumpus room, VIP lounge, guild manual, hermit clovers, garden harvest, hardwood
collection, Mr Store credits, pocket wishes, boxing daydream, toy uses, batteries, skill books,
and more. Each action checks whether it has already been done today (via preferences), issues
the appropriate HTTP requests, and handles outfit checkpointing.

**Mobile status (PR #10): Core actions implemented; long tail absent.**

`session/BreakfastManager.kt` implements:
- **Garden harvest** — reads `harvestGarden{Softcore,Hardcore}` pref (crop name, default "none"); calls `CampgroundRequest.harvestGarden()`; guarded by `GARDEN_HARVESTED` bool
- **Clan rumpus room** — reads `visitRumpus{Softcore,Hardcore}` pref (default true); calls `ClanRumpusRequest.visit()`; guarded by `BREAKFAST_RUMPUS` bool
- **VIP lounge** — gated on VIP Lounge Key in inventory; runs Deluxe Klaw up to 3 times (guarded by `DELUXE_KLAW_SUMMONS` int), Looking Glass (bool), Fireworks Shop (bool), Pool Table (int ≥ 1)
- **Guild manual detection** — checks character class → manual item ID → presence in inventory; HTTP use request not yet implemented (detection-only stub)
- **Pocket wishes** — detects pocket wish in inventory; choice handling deferred to adventure loop (stub)
- **Daycount gating** — `SessionManager` stores `LAST_DAYCOUNT`; rollover + breakfast prefs cleared only when `dayCount` changes

**Remaining gaps:**
- Guild manual HTTP use (`inv_use.php` call) — detection is present, activation is not
- Pocket wish choice handling — requires adventure loop integration
- Hermit clovers, hardwood planks, Mr Store monthly credits
- Boxing daydream, toy uses, batteries, ancient saucehelm, skill books
- Outfit checkpoint (save/restore equipped outfit around breakfast HTTP requests)

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
| Testing | 384 test classes, many integration | 60 unit test files | Desktop wins |
| Scripting | Full ASH + CLI (835 functions) | Partial ASH (~54 functions) | Desktop wins heavily |
| Events | Ad-hoc listeners | GameEventBus pub/sub | Mobile is cleaner |
| Choice automation | ~1,000 handler cases | ~80 active IDs covered | Good coverage of common paths |
| Recovery/mood | 9 classes, full persistence + mood library | 6 files, named library + malignant clearing | Closing gap — inheritance/AT songs remain |
| ManaBurn | Full — any buff, summons, per-skill priority | Partial — mood-trigger effects only | Desktop wins on coverage |
| Banish tracking | 55+ banishers, full routing integration, phylum | 20 banishers, zone pre-flight wired, identity detected, daycount-gated rollover | Substantially closed; 35 banishers + phylum remain |
| Breakfast / daily actions | ~20 actions, outfit checkpointing | Garden, rumpus, VIP lounge core, guild manual detection | Core in; long tail absent |

---

## Top Priorities

1. **ASH function batch — core scripting primitives** — At ~54/835 (6%) coverage, the ASH gap
   is the single largest feature deficit. No real community KoLmafia script is portable to mobile
   until this improves. Highest-value batch:
   - **Character queries:** `my_class`, `my_path`, `in_run`, `ascension_number`, `my_primestat`, `my_sign`
   - **Collection accessor:** `get_inventory` (returns `int[item]` map; unlocks most inventory-checking scripts)
   - **Modifier queries:** `numeric_modifier`, `boolean_modifier`, `string_modifier` (enables gear planning scripts)
   - **Type conversions:** `to_location`, `to_familiar` (needed by nearly every automation script)
   - **Goal management:** `add_item_condition`, `goal_exists`, `get_goals` (scripts that set their own stop conditions)
   - **Date/time:** `today_to_string`, `now_to_string`, `rollover` (daily automation scripts need these)
   - This batch alone would make the top ~20 community scripts runnable.

2. **VillainLair + Rufus solvers** — Complete the four TODO stubs in `SolverHandlers.kt`:
   - Choices 1260/1262: pure string matching against `_villainLairColor` pref and response
     text (~50 lines). No HTTP side effects.
   - Choices 1498/1499: requires a `RufusManager` to parse phone call HTML and store quest
     type/target in preferences (~200 lines total).

3. **Six choice solver implementations** — All 6 solver stubs (`ArcadeGameSolver`, `GameproSolver`,
   `LightsOutSolver`, `LostKeySolver`, `SafetyShelterSolver`, `VampOutSolver`) are `NoOp`.
   Without real implementations, choice IDs 486, 535, 536, 546, 594, 665, 890–903 fall
   through to option 1, breaking Dreadsylvania, Safety Shelter, and the Lights Out chain.
   `LightsOut` and `SafetyShelter` are the highest-value starting points.

4. **BreakfastManager completion** — The high-value core actions are in (PR #10). Remaining:
   - Guild manual activation (`inv_use.php` call; detection already works)
   - Pocket wish choice handling (requires adventure loop integration)
   - Hermit clovers, hardwood, Mr Store monthly credits
   - Outfit checkpoint (save/restore around HTTP requests)

5. **BanishManager — missing banishers** — Adding the 35 missing banishers is incremental
   (each is a one-liner in `Banisher.kt` + corresponding HTML pattern in `BANISHER_PATTERNS`).
   High-value gaps: Feel Hatred (25 turns), Monkey Slap (30 turns), Show your Work (30 turns),
   Spring-Loaded Front Bumper (30 turns).

6. **`cli_execute` real dispatch** — Currently prints and returns true. Minimal real dispatch
   should handle: `mood execute`, `mood [name]`, `set key=value`, `get key` — the four
   most-called patterns in community scripts. Directly unblocks mood-using scripts.

7. **Mood system refinements** — Three remaining gaps in approximate value order:
   (a) AT song slot tracking + auto-evict lowest-priority song when full (unblocks AT class players);
   (b) `removeMalignantEffects` default alignment with desktop (currently `true`, desktop is `false` — resolve intentional divergence);
   (c) mood inheritance (`extends` keyword parsing in mood names) — enables the "default" base mood pattern used by most power users.
