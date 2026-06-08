# KoLmafia Mobile vs Desktop — Parity Audit

_Generated: 2026-06-03 (updated 2026-06-07 after Phase 12: Choice Solvers + ASH Quick Wins + Banisher Expansion — PR #13)_

## Scale Comparison

| Metric | Desktop (Java) | Mobile (Kotlin) | Coverage |
|--------|---------------|-----------------|----------|
| Source files | ~1,172 classes | ~245 files (commonMain) | ~21% |
| Lines of code | ~57,000 | ~13,500 (commonMain, est.) | ~24% |
| Test files | 411 | 92 | ~22% |
| ASH function overloads | ~890 | ~82 registered | ~9% |
| Banisher enum entries | 70 (69 named + UNKNOWN) | 70 (69 named + UNKNOWN) | **100%** |
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
| **Choice solvers** *(PR #13)* | `ArcadeGameSolver`, `GameproSolver`, etc. | `adventure/choice/solvers/` (12 files) | **All 6 implemented** — LightsOut, SafetyShelter, LostKey, ArcadeGame, Gamepro, VampOut |
| **resolveChoice() loop** *(PR #13)* | `ChoiceManager.java` multi-step chain | `AdventureManager.resolveChoice()` | While-loop up to 20 steps; `StopReason.MacroError` on cap |
| Inventory | `session/InventoryManager.java` | `inventory/InventoryManager.kt` | Basic use/equip; craft submits but unparsed |
| Familiar management | `FamiliarData.java` (65K) | `familiar/FamiliarManager.kt` | Switch/equip/hatch; no per-familiar logic |
| Skill casting | `session/SkillDatabase.java` | `skill/SkillManager.kt` | Daily limits tracked |
| Active effects | `session/EffectDatabase.java` | `effect/EffectManager.kt` | Display only |
| Chat (clan/PM) | `chat/ChatManager.java` | `chat/ChatManager.kt` | Polling parity |
| Coinmaster shops | `request/coinmaster/` (19 classes) | `shop/CoinmasterRegistry.kt` | 30+ coinmasters |
| Mall search/buy | `request/MallSearchRequest.java` | `mall/MallPriceManager.kt` | TTL cache; `mallSearch()` returns empty (HTML parser TODO) |
| ASH interpreter | `textui/` (366 classes) | `ash/` (26 files) | **~82 function overloads** — see ASH section |
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
| **Auto-clear malignant effects** *(PR #8)* | `MoodManager.removeMalignantEffects()` | `mood/MalignantEffects.kt` + `MoodManager.removeMalignantEffects()` | 9 effect names; fires every mood pass via `UneffectRequest`; best-effort |
| **UneffectRequest** *(PR #8)* | `UseSkillRequest` + CLI `uneffect` | `request/UneffectRequest.kt` | HTTP wrapper for `uneffect.php`; Result-typed, status-validated |
| **Mood library** *(PR #8)* | `MoodManager._moods` + `username_moods.txt` | `MoodManager.moodLibrary` + Preferences | Named mood persistence; add/remove/setActive/save/load; restored on login |
| **BanishManager** *(PR #8 + PR #10 + PR #13)* | `session/BanishManager.java` (618 lines) | `banish/` (3 files) | **70 enum entries (parity)**; 37 BANISHER_PATTERNS; zone pre-flight wired; daycount-gated rollover |
| **MonsterBanished event** *(PR #8)* | `BanisherUsed` KoLmafia event | `event/GameEvent.MonsterBanished` | Emitted from `AdventureManager`; includes banisher identity |
| **BreakfastManager** *(PR #10)* | `BreakfastManager.java` (1,000 lines) | `session/BreakfastManager.kt` | Garden harvest, rumpus, VIP lounge (Klaw ×3, pool, looking glass, fireworks), guild manual detection |
| **CampgroundRequest** *(PR #10)* | `request/CampgroundRequest.java` | `request/CampgroundRequest.kt` | `harvestGarden()` HTTP wrapper |
| **ClanRumpusRequest** *(PR #10)* | `request/ClanRumpusRequest.java` | `request/ClanRumpusRequest.kt` | `visit()` HTTP wrapper |
| **ClanLoungeRequest** *(PR #10)* | `request/ClanLoungeRequest.java` | `request/ClanLoungeRequest.kt` | `useKlaw()`, `useLookingGlass()`, `visitFireworks()`, `playPoolGame()` |
| **CombatDatabase / ZoneLookup** *(PR #10)* | `AdventureDatabase.java` monster weights | `data/ZoneLookup.kt` + `data/CombatDatabase.kt` | Zone→monster list; used by banish zone pre-flight |
| **`is_banished()` + `banishers_used()`** *(PR #10)* | `RuntimeLibrary.java` banish queries | `ash/GameRuntimeLibrary.kt` | `is_banished(monster)`, `is_banished(string)`, `banishers_used() → string[monster]` |
| **ASH character/familiar/equipment** *(PR #11)* | `RuntimeLibrary.java` | `GameRuntimeLibrary.Character/Familiar/Equipment.kt` | `my_class`, `my_path`, `my_sign`, `my_primestat`, `in_run`, `ascension_number`, `have_familiar`, `use_familiar`, `equipped_item`, `to_slot`, `slot_to_item` |
| **ASH date/time** *(PR #11)* | `RuntimeLibrary.java` | `GameRuntimeLibrary.DateTime.kt` | `today_to_string`, `now_to_string`, `gameday_to_string`, `rollover`, `moon_phase`; platform expect/actual |
| **ASH modifier extensions** *(PR #11)* | `RuntimeLibrary.java` | `GameRuntimeLibrary.Modifiers.kt` | `numeric_modifier` (×3), `boolean_modifier` (×2), `string_modifier` (×1) |
| **ASH collection extensions** *(PR #11)* | `RuntimeLibrary.java` | `GameRuntimeLibrary.Collections.kt` | `get_inventory` (live); `get_closet`, `get_storage`, `get_stash`, `get_display` (stubs) |
| **ASH item-action extensions** *(PR #11)* | `RuntimeLibrary.java` | `GameRuntimeLibrary.ItemActions.kt` | `use`, `eat`, `drink`, `chew`, `autosell`, `put_closet`, `take_closet`, `take_storage`; `put_shop` stub |
| **HTTP item request classes** *(PR #11)* | Various `request/` classes | `request/UseItemRequest`, `EatFoodRequest`, `DrinkBoozeRequest`, `ChewRequest`, `AutosellRequest`, `ClosetRequest`, `StorageRequest` | 7 suspend request classes |
| **`visit_url` ASH function** *(PR #13)* | `RuntimeLibrary.java` `visit_url` | `GameRuntimeLibrary.WebRequest.kt` | 4 overloads: GET (×2), POST string (×2); HttpClient wired via DI |
| **`hermit()` ASH function** *(PR #13)* | `RuntimeLibrary.java` `hermit` | `GameRuntimeLibrary.Hermit.kt` + `request/HermitRequest.kt` | `hermit(item, count) → int`; item-first to match desktop API |
| **`get_moods()` / `mood_list()`** *(PR #13)* | `RuntimeLibrary.java` mood queries | `GameRuntimeLibrary.Mood.kt` | Now live — reads `moodManager.moodLibrary.keys.sorted()` |
| **`cli_execute` partial dispatch** *(PR #11)* | `KoLmafiaCLI.java` (100+ commands) | `GameRuntimeLibrary.kt` cliDispatch | Handles: `mood execute`, `mood <name>`, `set key=val`, `get key`; all others echo |
| **ASH pricing functions** *(PR #11)* | `RuntimeLibrary.java` pricing | `GameRuntimeLibrary.Pricing.kt` | `npc_price`, `autosell_price` (GameDatabase lookups); `mall_price`/`historical_price` stubs |

---

### Not Implemented in Mobile

| Feature | Desktop size/complexity | Priority |
|---------|------------------------|----------|
| **`cli_execute` full dispatch** | `KoLmafiaCLI.java` ~100+ commands | **High** — only 4 patterns handled (`mood execute`, `mood <name>`, `set key=val`, `get key`); scripts calling `maximize`, `cast`, `familiar`, etc. fall through to echo |
| **`hermit()` count-first overload** | `RuntimeLibrary.java` — 2 overloads | **High** — desktop has both `hermit(item, count)` and `hermit(count, item)`; mobile only has item-first; scripts using count-first form fail silently |
| **`under_standard()` real value** | `KoLCharacter.isUnderStandard()` | **High** — always returns `false`; `CharacterState` has no Standard flag; scripts gating on Standard mode behavior are broken |
| **BreakfastManager completion** | 20 actions in desktop | **High** — guild manual HTTP use (~15 lines with existing UseItemRequest); hermit clovers (HermitRequest ready); hardwood, Mr Store, boxing daydream, toy uses, batteries, skill books, pocket wish choice handling |
| **`get_closet()` / `get_storage()`** | `RuntimeLibrary.java` — live inventory | **High** — both return empty stubs; `ClosetRequest` and `StorageRequest` exist but don't populate collections; scripts using closet/Hagnk's content are blind |
| **`to_int()` entity overloads** | `RuntimeLibrary.java` — 12+ overloads | **High** — mobile only converts string/float/boolean/int; missing `to_int(item)`, `to_int(familiar)`, `to_int(skill)`, `to_int(effect)`, `to_int(location)`, `to_int(monster)`; common in scripts |
| **`can_adventure()` / `prepare_for_adventure()`** | `RuntimeLibrary.java` adventure guards | **High** — not implemented; scripts call these before spending turns |
| **`buy()` / `retrieve_item()`** | `MallSearchRequest` / compound | **Medium** — `buy` needs MallSearchRequest HTML parser; `retrieve_item` is compound (closet/storage/buy); blocks many automation scripts |
| **`adv1(location, adventuresUsed)`** | `RuntimeLibrary.java` | **Medium** — single-adventure variant used in fine-grained scripts; mobile only has `adventure(turns, loc)` |
| **`create()` / `craft()` response parsing** | `concoction/` 32 classes | **Medium** — submits but returns placeholder item (id=-1); `ItemCrafted` event has id=-1 |
| **`put_shop()` / `take_shop()` / `reprice_shop()`** | `MallRequest.java` | **Medium** — all stub/absent; shop management blocked |
| **`eatsilent()` / `drinksilent()` / `overdrink()`** | `RuntimeLibrary.java` consumption variants | **Medium** — mobile has eat/drink but not silent variants or overdrinking |
| **`copiers_used(skill)`** | `RuntimeLibrary.java` copy tracking | **Medium** — always returns 0; Yellow Ray / other copy counts not tracked |
| **`put_display()` / `take_display()` / `put_stash()` / `take_stash()`** | `RuntimeLibrary.java` | **Medium** — missing; `get_stash()` and `get_display()` are also empty stubs |
| **ASH outfit/equipment (remaining)** | `outfit`, `have_outfit`, `retrieve_outfit` | **Medium** — requires outfit tracking in InventoryManager |
| **Quest tracking depth** | Per-quest state machines in `QuestDatabase.java` | **Medium** — 99 quests by name; step detection text-based; NPC-visit advances missed until next login |
| **KoLCharacter depth** | 200+ fields in desktop | **Medium** — 70+ fields; per-quest flags, detailed campground state, storage/closet item counts beyond meat totals absent |
| **Relay server** | `webui/` 20+ decorators, 15 JS/CSS files | **Medium** — intentionally skipped |
| **Maximizer** | `maximizer/` 12 classes (~5,877 lines) | **Medium** — stat optimization engine; very high complexity |
| **Session logging** | `RequestLogger.java` (1,322 lines) | **Medium** — events via GameEventBus only; nothing persisted |
| **`logprint` / `debugprint` / `traceprint`** | `RuntimeLibrary.java` logging variants | **Medium** — mobile only has `print`; scripts that use `logprint` silently drop output |
| **`wait(delay)` / `waitq(delay)`** | `RuntimeLibrary.java` sleep/yield | **Medium** — not present; timed scripts can't yield |
| **ManaBurn sophistication gap** | Desktop burns any castable buff, summons, per-skill priority | **Medium** — mobile covers mood-trigger effects only; `allowNonMoodBurning`, `manaBurnSummonThreshold`, per-skill priority absent |
| **Mood inheritance** | `Mood.java` `extends` keyword | **Medium** — mobile `Mood` is flat; no parent concept; the "default" base mood pattern used by most power users doesn't work |
| **AT song limit management** | MoodManager auto-evicts lowest-priority AT song | **Medium** — no song tracking in mobile |
| **GoalManager special stops** | `GOAL_CHOICE`, `GOAL_AUTOSTOP`, `GOAL_FACTOID`, etc. | **Low-Medium** — mobile only has item/meat/level/banished goals |
| **`my_thrall()`** | `RuntimeLibrary.java` thrall queries | **Low-Medium** — returns empty string; no THRALL AshType defined |
| **`in_multi_fight()` / `fight_follows_choice()`** | Combat state queries | **Low-Medium** — always false; by design on mobile |
| **Pre-adventure zone checks** | `KoLAdventure.java` outfit/familiar/limit-mode checks | **Low-Medium** — banish pre-flight is wired; outfit/familiar/limit-mode validation absent |
| **BANISHER_PATTERNS coverage gaps** | `FightRequest.java` ~55 banish triggers | **Low-Medium** — 20 of 69 named banishers have no pattern; these are triggered via non-fight paths (choice adventures, specific items); will record as UNKNOWN banisher safely cleared on rollover |
| **`to_int()` / entity conversions (remaining)** | `to_thrall`, `to_servant`, `to_vykea`, `to_bounty`, `to_modifier`, `to_path` | **Low** — niche entity types |
| **`user_confirm()` / `user_prompt()`** | Interactive prompts | **Low** — not applicable to headless mobile |
| **`batch_open()` / `batch_close()`** | Batching | **Low** |
| **`dump()`** | Aggregate pretty-print | **Low** |
| **`is_dark_mode()` / `is_headless()`** | Environment queries | **Low** |
| **`session_logs(days)`** | Session log reading | **Low** |
| **`holiday()`** | KoL holiday string | **Low** |
| **Mall HTML parser** | `mall_price()` / `historical_price()` live data | **Low** — `MallSearchRequest` exists; HTML parser not implemented; `mall_price` returns 0 |
| **Ascension mechanics depth** | `session/AscensionManager.java` | **Low** — path enum complete; reset/completion/unlock tracking absent |
| **Concoction/crafting logic** | `request/concoction/` 32 classes | **Low** — `craft()` submits but returns placeholder |
| **Clan dungeons** | Scattered session managers | **Low** |
| **PvP** | `request/` PvP handlers | **Low** |

---

## Deep Gap Analysis

### Choice Adventure Handlers + Solvers (Phase 12 Complete)

**Status: PRODUCTION-READY including all 6 solver implementations**

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
| `SolverHandlers.kt` | 486, 535, 536, 546, 594, 665, 702, 890–903, 1260, 1262, 1498, 1499 |

Desktop `ChoiceManager` covers ~1,000+ cases; mobile covers the ~80 that need automation
logic. The long tail of trivial or preference-driven choices falls through to the
preference/manual fallback correctly.

**All 6 choice solver implementations shipped in PR #13:**

| Solver | Choice IDs | Implementation |
|--------|-----------|----------------|
| `LightsOutSolverImpl` | 890–903 | Reads `lightsOutAutomation` pref (0=off/1=chase/2=power); room 890-896 keyword→option; 897-903 multi-step HTML branch |
| `SafetyShelterSolverImpl` | 535–536 | Ronald scripts `["11211","1122","12211","12221","1321","1322"]`; Grimace scripts `["1121","1122","1211","12121","13211","12221"]`; pref 1-6 selects index |
| `LostKeySolverImpl` | 546 | 3 scripts indexed by pref (glasses/comb/pill bottle) |
| `ArcadeGameSolverImpl` | 486, 594 | 120-char FistScript + "Finish from Memory" text match via `ChoiceUtilities.parseChoices()` |
| `GameproSolverImpl` | 665 | Reads `choiceAdventure665` pref, splits on comma, returns stepCount-th int |
| `VampOutSolverImpl` | 702 | 13 goal scripts (goalIdx 0-12); dynamic option math for Vlad/Isabella/Masquerade availability; `Preferences.INTERVIEW_*` tracking; `ChoiceUtilities.parseChoices()` for HTML parsing |

`resolveChoice()` now loops up to 20 steps (was single-shot), enabling multi-step solvers to complete their full script sequences.

**Known VampOut edge case:** When the solver's target location (e.g., Vlad for goalIdx 0-2) has already been visited this run, `vladChoice` evaluates to `0` rather than `null`. Option `0` is not a valid KoL selection. Tracked as follow-up background task.

### Mood / Recovery (Phase 5a + Phase 6 + Phase 7 + Phase 8)

**Status: CORE WORKING + STOP-THRESHOLD LOOP + MANABURN + PERSISTENCE + NAMED LIBRARY + MALIGNANT CLEARING (PR #8).
Mood inheritance, AT song management, and full ManaBurn breadth absent.**

`RecoveryManager.kt` picks the best available HP/MP restore item or skill from
`RestoreDatabase`. The adventure loop in `AdventureManager` calls `recoverIfNeeded` in an
up-to-10-iteration while-loop. After recovery, `ManaBurnManager.burnIfEnabled` fires, casting
mood-trigger skills until MP falls below `MANA_BURN_MIN_MP_PCT` (default 90%).

**What's working:**
- Single active mood with full trigger evaluation
- Mood library — named mood profiles with `addMoodToLibrary`, `removeMoodFromLibrary`, `setActiveMoodByName`
- Mood library + active mood persistence across login
- Malignant effect auto-clearing — 9 effect names (Beaten Up + poisons); best-effort
- ManaBurn post-turn loop (capped at 10 iterations; mood-trigger effects only)
- Recovery item/skill selection with daily-limit awareness

**Known gaps:**
- **No mood inheritance** — Desktop `Mood.java` parses `"moodA extends default"` parent merge.
- **No AT song management** — Desktop auto-evicts lowest-priority AT song when slot is full.
- **Mobile ManaBurn is narrower** — Desktop burns *any* castable buff; `allowNonMoodBurning`, `allowSummonBurning`, per-skill priority prefs absent.

### Goal Manager + Adventure Loop Stop (Phase 6 — Merged 2026-06-06)

**Status: CORE COMPLETE. Six special stop-types from desktop `GoalManager` absent.**

Mobile `GoalManager.kt` covers: item goals by ID, item goals by name, meat goal, level goal.

Desktop additionally has: `GOAL_CHOICE`, `GOAL_AUTOSTOP`, `GOAL_FACTOID`, `GOAL_FLOUNDRY`, `GOAL_LEPRECONDO`, `GOAL_SUBSTATS`.

### KoLCharacter (Substantially Addressed)

Desktop [`KoLCharacter.java`](../../../kolmafia/src/net/sourceforge/kolmafia/KoLCharacter.java)
is 6,201 lines. Mobile `character/KoLCharacter.kt` wraps a `StateFlow<CharacterState>` with
partial-update methods.

**Remaining gaps:** Standard flag (`under_standard()` always returns `false`), per-quest flags, telescope monster data, detailed campground state (garden type/yield, mushroom plot), storage/closet item counts, Ed servant data, pasta thrall data, VYKEA companion data, ascension modifiers.

### Banish Tracking (PR #8 + PR #10 + PR #13)

**Status: ENUM PARITY ACHIEVED (70/70 entries). 37 detection patterns. Zone pre-flight wired.**

`banish/` package (3 files):
- `Banisher.kt` — **70 entries** (69 named + UNKNOWN); exact canonical name parity with desktop; `ResetType` enum (ROLLOVER/TURNS/TURN_ROLLOVER/AVATAR/NEVER)
- `BanishState.kt` — `BanishedMonster` data with `isExpired()` logic
- `BanishManager.kt` — StateFlow-backed; all CRUD + persistence + `clearExpiredAndRollover()`

**BANISHER_PATTERNS in `AdventureParser.kt`: 37 patterns** (covers the most common fight-sourced banishers). Approximately 20 of the 69 named banishers have no pattern because they're triggered via non-fight paths (choice adventures, NPC requests, specific item use) — these safely record as `UNKNOWN` and clear on rollover.

**Remaining gaps:**
1. **~20 banishers lack fight-HTML patterns** — lower-frequency items/skills. Enum entries exist; will record as UNKNOWN. Zone pre-flight routing still works (routing uses BanishManager state, not pattern detection).
2. **No phylum banishing** — Desktop tracks Breathitin / Out of the Frying Pan by phylum. Mobile has no phylum concept.
3. **No queue model** — Desktop allows multiple simultaneous banishes of the same monster; mobile replaces the entry.

### Quest Database (Substantially Addressed)

`quest/Quest.kt` enumerates **99 quests** across all desktop categories. `QuestDatabase.kt`
has step-ordering, `isQuestLaterThan`/`isQuestFinished`, `validateStep`, `setProgressByPrefKey`.
`QuestLogDatabase.kt` parses `questslog.txt` for text-based step detection.

**Remaining gaps:** Per-quest in-code state machine logic. Quests advanced via NPC visits captured only at next login sync. Handful of quests with special-cased step detection (Telegram, Party Fair, Doctor Bag, PirateRealm) fall back to "started".

### Data Files (Addressed)

Desktop ships 51 data files (64,700+ lines). Mobile bundles **50 `.txt` files**.

### ASH Interpreter (~82 Function Overloads — 9% Gap)

Desktop `textui/RuntimeLibrary.java` registers **~890 `LibraryFunction` instances**. Mobile
`ash/GameRuntimeLibrary.kt` + 13 extension files register approximately **82 distinct function
overloads** — roughly 9% coverage.

**Architecture:** `GameRuntimeLibrary.kt` core registrations plus 13 extension files (`GameRuntimeLibrary.*.kt`) via the `regFn()` bridge.

**What's implemented (after Phase 12 / PR #13):**

| Category | Functions |
|----------|-----------|
| Type conversions | `to_int` (×4 — string/float/bool/int only), `to_float` (×2), `to_string` (×5), `to_boolean`, `to_item`, `to_skill`, `to_effect`, `to_slot`, `to_familiar`, `to_location` (stub), `to_monster` |
| String utils | `contains`, `length`, `substring`, `index_of`, `replace`, `split`, `join`, `to_upper_case`, `to_lower_case` |
| Math utils | `min`, `max`, `floor`, `ceil`, `round`, `abs`, `sqrt`, `random` |
| Aggregate ops | `count`, `contains_key`, `remove`, `to_string` (aggregate) |
| Print | `print`, `print_html`, `abort` |
| Character | `my_name`, `my_level`, `my_hp`, `my_maxhp`, `my_mp`, `my_maxmp`, `my_meat`, `my_adventures`, `my_fullness`, `my_inebriety`, `my_spleen_use`, `my_basestat`, `in_hardcore`, `my_familiar`, `my_class`, `my_path`, `my_sign`, `my_primestat`, `my_thrall` (stub), `in_run`, `under_standard` (⚠️ always false), `ascension_number`, `can_interact` |
| Familiar | `have_familiar`, `use_familiar`, `my_familiar_weight`, `to_familiar` |
| Item | `item_amount`, `available_amount`, `to_item`, `have_item` |
| Equipment | `equipped_item`, `have_equipped`, `to_slot`, `slot_to_item` |
| Skill | `have_skill`, `mp_cost`, `to_skill`, `daily_limit`, `times_cast` |
| Effect | `have_effect`, `to_effect` |
| Modifier | `numeric_modifier` (item×2, effect×1), `boolean_modifier` (item×1, effect×1), `string_modifier` (item×1) |
| Collections | `get_inventory` (live), `get_closet`, `get_storage`, `get_stash`, `get_display` (stubs) |
| Date/time | `today_to_string`, `now_to_string`, `gameday_to_string`, `rollover`, `moon_phase` |
| Goals | `add_item_condition`, `remove_item_condition`, `goal_exists`, `get_goals` |
| Mood | `get_moods`, `mood_list` (live — reads `moodLibrary.keys.sorted()`) |
| Preferences | `get_property`, `set_property` |
| Combat | `last_monster` (live via `_lastMonster` pref), `in_multi_fight` (stub), `fight_follows_choice` (stub), `copiers_used` (stub) |
| Item actions | `use`, `eat`, `drink`, `chew`, `autosell`, `put_closet`, `take_closet`, `take_storage` (all live); `put_shop` (stub) |
| Banish | `is_banished` (×2), `banishers_used`, `to_monster` |
| Web | `visit_url` (×4: GET×2, POST×2) |
| Economy | `hermit(item, count)` (item-first overload only); `npc_price`, `autosell_price` (GameDatabase lookups) |
| Game actions | `adventure`, `use_skill` (×2), `cli_execute` (partial — 4 patterns dispatched) |

**Still missing by category:**

| Category | Key absent functions |
|----------|---------------------|
| Type conversions | `to_int(item/familiar/skill/effect/location/monster)`, `to_modifier`, `to_path`, `to_thrall`, `to_vykea`, `to_servant`, `to_bounty` |
| Web scripting | `load_html`, `write`, `writeln`, `form_field`, `make_url` |
| Economy | `buy` (×3), `sell`, `retrieve_item`, `retrieve_price`, `overdrink`, `eatsilent`, `drinksilent`, `hermit(count, item)` (count-first overload) |
| Collections | `put_display`, `take_display`, `put_stash`, `take_stash`, `empty_closet` |
| Shop management | `reprice_shop`, `take_shop` |
| Pricing | `mall_price`, `historical_price` |
| Adventure prep | `can_adventure`, `prepare_for_adventure`, `adv1`, `set_location` |
| Equipment | `outfit`, `have_outfit`, `retrieve_outfit` |
| CLI | `cli_execute` full dispatch (~96 unhandled patterns), `cli_execute_output` |
| Logging | `logprint`, `debugprint`, `traceprint`, `dump` |
| Timing | `wait`, `waitq` |
| Environment | `is_dark_mode`, `is_headless`, `get_revision`, `get_version`, `get_path` |
| Combat | `copiers_used` real tracking, `can_still_steal`, `get_ccs_action` |
| Coinmaster | `is_accessible`, `inaccessible_reason`, `visit` (coinmaster variants) |

### Breakfast / Daily Automation (PR #10 — Partial)

Desktop `session/BreakfastManager.java` automates **20 actions**. Mobile covers a subset:

**Mobile status:** Garden harvest, clan rumpus, VIP lounge core (Klaw×3, Looking Glass, Fireworks, Pool), guild manual detection.

**Remaining gaps (20 desktop actions → mobile covers ~5):**
- Guild manual HTTP use — `UseItemRequest` ready; ~15 lines
- Hermit clovers — `HermitRequest` ready (PR #13); ~15 lines
- Pocket wish choice handling — requires adventure loop integration
- Hardwood planks, Mr Store monthly credits, April shower globs, spinning wheel, boxing daydream, toy uses (35 types), anticheese, sea jelly, batteries, replica books, Book of Every Skill, handheld radios

### Modifier System (Present, Substantial)

Mobile has a 10-file `modifiers/` package covering the full passive prediction algorithm.

**Remaining gaps:** Conditional outfit set bonuses (half-set bonuses), modifier lookup by item ID (name-only currently), Synergy modifier type.

---

## Architecture Comparison

| Aspect | Desktop | Mobile | Assessment |
|--------|---------|--------|------------|
| HTTP layer | Raw Java HTTP + custom threading | Ktor coroutines | Mobile is cleaner |
| State management | Static singletons | StateFlow + Koin DI | Mobile is cleaner |
| UI | Swing (aging) | Compose Multiplatform | Mobile is modern |
| Concurrency | Manual threading | Coroutines | Mobile is cleaner |
| Data | 51 `.txt` files parsed at startup | 50 bundled `.txt` files | Parity |
| Testing | 411 test classes, many integration | 92 unit test files | Desktop wins |
| Scripting | Full ASH + CLI (890 functions) | Partial ASH (~82 functions, 9%) | Desktop wins; closing gap |
| Events | Ad-hoc listeners | GameEventBus pub/sub | Mobile is cleaner |
| Choice automation | ~1,000 handler cases | ~80 active IDs; all 6 solvers implemented | Good coverage of common paths |
| Recovery/mood | 9 classes, full persistence + mood library | 6 files, named library + malignant clearing | Closing gap — inheritance/AT songs remain |
| ManaBurn | Full — any buff, summons, per-skill priority | Partial — mood-trigger effects only | Desktop wins on coverage |
| Banish tracking | 70 banishers, queue model, phylum, full routing | **70 banishers (parity)**, 37 patterns, zone pre-flight, daycount-gated | Enum parity; queue/phylum remain |
| Breakfast / daily actions | ~20 actions, outfit checkpointing | Garden, rumpus, VIP lounge core, guild manual detection | Core in; ~15 actions remain |
| Item HTTP actions | Broad coverage | 7 request classes + HermitRequest | Core consumption + selling + hermit covered |

---

## Top Priorities

1. **`under_standard()` real value** — Always returns `false`. Add `isUnderStandard` to `CharacterState`
   and populate it from the character API response. One field + one parser update. High community impact
   since many path-gating scripts call this.

2. **`to_int()` entity overloads** — `to_int($item[name])`, `to_int($familiar[name])`, etc. return 0.
   These are extremely common in community scripts. Desktop registers 12+ overloads. Each is a simple
   `GameDatabase.item(name).id` lookup. Quick wins, high value.

3. **BreakfastManager completion** — `HermitRequest` (PR #13) and `UseItemRequest` (PR #11) are both ready:
   - Guild manual HTTP use: `UseItemRequest(manualItemId, 1)` — ~15 lines
   - Hermit clovers: `hermitRequest.trade(CLOVER_ITEM_ID, count)` — ~15 lines
   - Outfit checkpoint for breakfast safety

4. **`hermit()` count-first overload** — Add `hermit(count: INT, item: ITEM) → INT` to match desktop.
   One additional `regFn` call referencing the same `hermitRequest.trade()`.

5. **`get_closet()` / `get_storage()` live data** — These return empty stubs. Wire
   `ClosetRequest` and `StorageRequest` to populate the aggregate (HTTP fetch + parse inventory HTML).
   Many scripts check closet/Hagnk's before deciding to buy.

6. **`cli_execute` expansion** — Add `cast`, `familiar`, `maximize` (echo with note), `equip`,
   `unequip`, `outfit`, `buy`, `sell` dispatch patterns to the `cliDispatch` table in `GameRuntimeLibrary.kt`.
   Each is a few lines; collectively unblocks a large portion of community scripts.

7. **Mood system refinements** — In priority order:
   (a) AT song slot tracking + auto-evict (unblocks AT class players)
   (b) Mood inheritance (`extends` keyword) — enables "default" base mood pattern
   (c) `removeMalignantEffects` default alignment

8. **BANISHER_PATTERNS gap fill** — The 20 banishers with enum entries but no fight-HTML patterns
   are incremental. Each is a one-liner in `BANISHER_PATTERNS`. High-value ones that fire through
   fight HTML: `BATTER_UP`, `HUMAN_MUSK`, `MARK_YOUR_TERRITORY`, `BALEFUL_HOWL`.

9. **VampOut option-0 edge case** — When the solver's target location (Vlad/Isabella/Masquerade)
   is already visited, `vladChoice`/`isabellaChoice`/`masqueradeChoice` evaluates to `0` (invalid
   KoL option). Should return `null` to fall through to preference fallback. Tracked as background task.

10. **`visit_url` POST body handling** — Current `doPost` re-splits and re-encodes the `post_data`
    string which double-encodes any pre-encoded values. Scripts passing pre-encoded POST bodies
    (e.g., names with spaces) get corrupted requests. Tracked as background task.
