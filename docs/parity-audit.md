# KoLmafia Mobile vs Desktop — Parity Audit

_Generated: 2026-06-03 (updated 2026-06-08 after Phase 14: ASH Script Compatibility Pack)_

## Scale Comparison

| Metric | Desktop (Java) | Mobile (Kotlin) | Coverage |
|--------|---------------|-----------------|----------|
| Source files | ~1,172 classes | ~247 files (commonMain) | ~21% |
| Lines of code | ~57,000 | ~14,657 (commonMain) | ~26% |
| Test files | 411 | 95 | ~23% |
| Tests | ~1,800+ | 1,207 | ~67% (of covered scope) |
| ASH function overloads | ~890 | ~98 registered | ~11% |
| Banisher enum entries | 70 (69 named + UNKNOWN) | 70 (69 named + UNKNOWN) | **100%** |
| BreakfastManager actions | 22 (20 universal + 2 niche) | 20 | **~91%** |
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
| **BreakfastManager** *(PR #10 + Phase 13)* | `BreakfastManager.java` (1,000 lines) | `session/BreakfastManager.kt` (363 lines) | **20/22 desktop actions** — see breakdown below; missing only `checkJackass` (arcade game) and `collectSeaJelly` (jellyfish familiar) |
| **CampgroundRequest** *(PR #10 + Phase 13)* | `request/CampgroundRequest.java` | `request/CampgroundRequest.kt` | `harvestGarden()` + `useSpinningWheel()` HTTP wrappers |
| **ClanRumpusRequest** *(PR #10)* | `request/ClanRumpusRequest.java` | `request/ClanRumpusRequest.kt` | `visit()` HTTP wrapper |
| **ClanLoungeRequest** *(PR #10)* | `request/ClanLoungeRequest.java` | `request/ClanLoungeRequest.kt` | `useKlaw()`, `useLookingGlass()`, `visitFireworks()`, `playPoolGame()` |
| **HermitRequest** *(PR #13)* | `request/HermitRequest.java` | `request/HermitRequest.kt` | `trade(itemId, quantity)` POST wrapper; open class for test override |
| **BreakfastItemIds** *(Phase 13)* | Scattered `ItemPool.*` constants | `session/BreakfastItemIds.kt` | 34-toy map + item ID constants for all breakfast items |
| **AT song slot management** *(Phase 13)* | `MoodManager` auto-evict + AT song detection | `MoodManager.isAtSong()` + `executeActiveMood()` eviction | `atSongLimit` on `CharacterState` (3 for AT, 0 others); evicts lowest-priority song when slot full; double-eviction guard via `locallyEvicted` set |
| **`to_int()` entity overloads** *(Phase 14)* | `RuntimeLibrary.java` — `to_int` ×15 | `GameRuntimeLibrary.kt registerTypeConversions` | 6 new overloads: item, effect, skill, familiar, location (snarfblat), monster; all return 0 when db null or entity unknown |
| **`to_location(string)`** *(Phase 14)* | `RuntimeLibrary.java` `to_location` | `GameRuntimeLibrary.kt registerTypeConversions` | Constructs LOCATION value from string name; moved from stub to live |
| **`wait` / `waitq` + logging variants** *(Phase 14)* | `RuntimeLibrary.java` timing + log channels | `GameRuntimeLibrary.Timing.kt` | `wait(secs)`, `waitq(secs)` via `runBlocking { delay(ms) }`; `logprint`, `debugprint`, `traceprint` all route to `runtime.print()` |
| **`hermit()` count-first overload** *(Phase 14)* | `RuntimeLibrary.java` — 2 hermit overloads | `GameRuntimeLibrary.Hermit.kt` | `hermit(count: INT, item: ITEM) → INT` added alongside existing `hermit(item, count)` |
| **`under_standard()` real value** *(Phase 14)* | `KoLCharacter.isUnderStandard()` | `AscensionPath.STANDARD` + `CharacterState.isUnderStandard` | `AscensionPath.STANDARD("Standard")` enum entry added; `isUnderStandard` computed property; function reads live value instead of hard-coded `false` |
| **`can_adventure()` / `prepare_for_adventure()`** *(Phase 14)* | `RuntimeLibrary.java` adventure guards | `GameRuntimeLibrary.Character.kt` | `can_adventure(loc)` returns `adventuresLeft > 0`; `prepare_for_adventure()` is no-op returning `true` |
| **`adv1(location, adventuresUsed)`** *(Phase 14)* | `RuntimeLibrary.java` single-adventure form | `GameRuntimeLibrary.kt registerGameActions` | Runs 1 adventure via `adventureManager.runAdventures()`; returns false when manager null |
| **`cli_execute` cast + familiar** *(Phase 14)* | `KoLmafiaCLI.java` cast/familiar commands | `GameRuntimeLibrary.kt cliDispatch` | `cast N skill-name` and `cast skill-name` wire to `SkillManager.cast()`; `familiar name` wires to `FamiliarManager.setFamiliar()`; `FamiliarManager` made `open class` |
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
| **`get_closet()` / `get_storage()`** | `RuntimeLibrary.java` — live inventory | **High** — both return empty stubs; `ClosetRequest` and `StorageRequest` exist but don't populate collections; scripts using closet/Hagnk's content are blind |
| **`buy()` / `retrieve_item()`** | `MallSearchRequest` / compound | **High** — `buy` needs MallSearchRequest HTML parser; `retrieve_item` is compound (closet/storage/buy); blocks many automation scripts |
| **`cli_execute` remaining dispatch** | `KoLmafiaCLI.java` ~100+ commands | **High** — Phase 14 added `cast N/1` + `familiar`; still missing `equip`, `unequip`, `outfit`, `sell/autosell`, `buy`; scripts calling these fall through to echo |
| **`eatsilent()` / `drinksilent()` / `overdrink()`** | `RuntimeLibrary.java` consumption variants | **Medium** — mobile has eat/drink but not silent variants or overdrinking; `EatFoodRequest`/`DrinkBoozeRequest` already exist |
| **`create()` / `craft()` response parsing** | `concoction/` 32 classes | **Medium** — submits but returns placeholder item (id=-1); `ItemCrafted` event has id=-1 |
| **`put_shop()` / `take_shop()` / `reprice_shop()`** | `MallRequest.java` | **Medium** — all stub/absent; shop management blocked |
| **`copiers_used(skill)`** | `RuntimeLibrary.java` copy tracking | **Medium** — always returns 0; Yellow Ray / other copy counts not tracked |
| **`put_display()` / `take_display()` / `put_stash()` / `take_stash()`** | `RuntimeLibrary.java` | **Medium** — missing; `get_stash()` and `get_display()` are also empty stubs |
| **ASH outfit/equipment (remaining)** | `outfit`, `have_outfit`, `retrieve_outfit` | **Medium** — requires outfit tracking in InventoryManager |
| **Quest tracking depth** | Per-quest state machines in `QuestDatabase.java` | **Medium** — 99 quests by name; step detection text-based; NPC-visit advances missed until next login |
| **KoLCharacter depth** | 200+ fields in desktop | **Medium** — 70+ fields; per-quest flags, detailed campground state, storage/closet item counts beyond meat totals absent |
| **Relay server** | `webui/` 20+ decorators, 15 JS/CSS files | **Medium** — intentionally skipped |
| **Maximizer** | `maximizer/` 12 classes (~5,877 lines) | **Medium** — stat optimization engine; very high complexity |
| **Session logging** | `RequestLogger.java` (1,322 lines) | **Medium** — events via GameEventBus only; nothing persisted |
| **ManaBurn sophistication gap** | Desktop burns any castable buff, summons, per-skill priority | **Medium** — mobile covers mood-trigger effects only; `allowNonMoodBurning`, `manaBurnSummonThreshold`, per-skill priority absent |
| **Mood inheritance** | `Mood.java` `extends` keyword | **Medium** — mobile `Mood` is flat; no parent concept; the "default" base mood pattern used by most power users doesn't work |
| **GoalManager special stops** | `GOAL_CHOICE`, `GOAL_AUTOSTOP`, `GOAL_FACTOID`, etc. | **Low-Medium** — mobile only has item/meat/level/banished goals |
| **`my_thrall()`** | `RuntimeLibrary.java` thrall queries | **Low-Medium** — returns empty string; no THRALL AshType defined |
| **`in_multi_fight()` / `fight_follows_choice()`** | Combat state queries | **Low-Medium** — always false; by design on mobile |
| **Pre-adventure zone checks** | `KoLAdventure.java` outfit/familiar/limit-mode checks | **Low-Medium** — banish pre-flight is wired; outfit/familiar/limit-mode validation absent |
| **BANISHER_PATTERNS coverage gaps** | `FightRequest.java` ~55 banish triggers | **Low-Medium** — 20 of 69 named banishers have no pattern; these are triggered via non-fight paths (choice adventures, specific items); will record as UNKNOWN banisher safely cleared on rollover |
| **BreakfastManager remaining gaps** | `checkJackass`, `collectSeaJelly` | **Low** — `checkJackass` requires `ArcadeRequest.checkJackassPlumber()` (Arcade-specific path, niche); `collectSeaJelly` requires Space Jellyfish familiar + Sea quest started; neither blocks typical automation |
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

### Mood / Recovery (Phase 5a + Phase 6 + Phase 7 + Phase 8 + Phase 13)

**Status: CORE WORKING + STOP-THRESHOLD LOOP + MANABURN + PERSISTENCE + NAMED LIBRARY + MALIGNANT CLEARING + **AT SONG SLOT MANAGEMENT** (Phase 13).
Mood inheritance and full ManaBurn breadth absent.**

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
- **AT song slot management** — `CharacterState.atSongLimit` (3 for Accordion Thief, 0 others); `MoodManager.isAtSong()` queries `EffectDatabase` "song" attribute; `executeActiveMood()` evicts lowest-priority active song before overcasting; `locallyEvicted` set prevents double-eviction across multiple triggers in one pass

**Known gaps:**
- **No mood inheritance** — Desktop `Mood.java` parses `"moodA extends default"` parent merge.
- **Mobile ManaBurn is narrower** — Desktop burns *any* castable buff; `allowNonMoodBurning`, `allowSummonBurning`, per-skill priority prefs absent.

### Breakfast / Daily Automation (PR #10 + Phase 13 — Near Parity)

**Status: 20/22 desktop actions implemented. Only two niche actions remain.**

Desktop `session/BreakfastManager.java` automates **22 distinct actions** (called in `getBreakfast`):

| Action | Mobile | Notes |
|--------|--------|-------|
| `checkRumpusRoom` | ✅ | |
| `checkVIPLounge` | ✅ | Klaw×3, Looking Glass, Fireworks, Pool |
| `getHermitClovers` | ✅ *(Phase 13)* | Counts all 3 worthless item types, trades up to 5 |
| `harvestGarden` | ✅ | |
| `collectHardwood` | ✅ *(Phase 13)* | Visits `woods.php` |
| `collect2002MrStoreCredits` | ✅ *(Phase 13)* | Visits catalog item or replica catalog |
| `collectAprilShowerGlobs` | ✅ *(Phase 13)* | |
| `useSpinningWheel` | ✅ *(Phase 13)* | `campground.php?action=spinningwheel` |
| `visitBigIsland` | ✅ *(Phase 13)* | |
| `visitVolcanoIsland` | ✅ *(Phase 13)* | |
| `checkJackass` | ❌ | Requires `ArcadeRequest.checkJackassPlumber()` — Arcade-specific; niche |
| `makePocketWishes` | ✅ *(Phase 13)* | Genie bottle + replica bottle; choice handling |
| `haveBoxingDaydream` | ✅ *(Phase 13)* | |
| `useToys` | ✅ *(Phase 13)* | 34 toys; per-toy sentinel (`_toyUsed_$toyId`) |
| `collectAnticheese` | ✅ *(Phase 13)* | 5-day cooldown guard (`lastAnticheeseDay + 5`) |
| `visitServerRoom` | ✅ *(Phase 13)* | |
| `collectSeaJelly` | ❌ | Requires Space Jellyfish familiar + Sea quest started; niche |
| `harvestBatteries` | ✅ *(Phase 13)* | |
| `useBookOfEverySkill` | ✅ *(Phase 13)* | |
| `useReplicaBooks` | ✅ *(Phase 13)* | Snowcone tome, Resolution libram, Smith's tome |
| `makeHandheldRadios` | ✅ *(Phase 13)* | Allied Radio Backpack |
| *(Guild manual)* | ✅ *(PR #11/12)* | `readGuildManual` + `useGuildManual` |

The two missing actions (`checkJackass`, `collectSeaJelly`) require niche game state (Jackass Plumber arcade token, Space Jellyfish familiar + Sea quest) that very few players encounter in typical automation.

### Goal Manager + Adventure Loop Stop (Phase 6 — Merged 2026-06-06)

**Status: CORE COMPLETE. Six special stop-types from desktop `GoalManager` absent.**

Mobile `GoalManager.kt` covers: item goals by ID, item goals by name, meat goal, level goal.

Desktop additionally has: `GOAL_CHOICE`, `GOAL_AUTOSTOP`, `GOAL_FACTOID`, `GOAL_FLOUNDRY`, `GOAL_LEPRECONDO`, `GOAL_SUBSTATS`.

### KoLCharacter (Substantially Addressed)

Desktop [`KoLCharacter.java`](../../../kolmafia/src/net/sourceforge/kolmafia/KoLCharacter.java)
is 6,201 lines. Mobile `character/KoLCharacter.kt` wraps a `StateFlow<CharacterState>` with
partial-update methods.

**Added in Phase 13:** `atSongLimit: Int` computed property (3 for `CharacterClass.ACCORDION_THIEF`, 0 for all others).

**Added in Phase 14:** `AscensionPath.STANDARD("Standard")` enum entry; `CharacterState.isUnderStandard` computed property; `under_standard()` now reads live value.

**Remaining gaps:** Per-quest flags, telescope monster data, detailed campground state (garden type/yield, mushroom plot), storage/closet item counts, Ed servant data, pasta thrall data, VYKEA companion data, ascension modifiers.

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

### ASH Interpreter (~98 Function Overloads — 11% Gap)

Desktop `textui/RuntimeLibrary.java` registers **~890 `LibraryFunction` instances**. Mobile
`ash/GameRuntimeLibrary.kt` + 14 extension files register approximately **98 distinct function
overloads** — roughly 11% coverage.

**Architecture:** `GameRuntimeLibrary.kt` core registrations plus 14 extension files (`GameRuntimeLibrary.*.kt`) via the `regFn()` bridge. Added in Phase 14: `GameRuntimeLibrary.Timing.kt` for timing/logging functions.

**What's implemented (after Phase 14):**

| Category | Functions |
|----------|-----------|
| Type conversions | `to_int` (×10 — string/float/bool/int + item/effect/skill/familiar/location/monster), `to_float` (×2), `to_string` (×5), `to_boolean`, `to_item`, `to_skill`, `to_effect`, `to_slot`, `to_familiar`, `to_location` (live), `to_monster` |
| String utils | `contains`, `length`, `substring`, `index_of`, `replace`, `split`, `join`, `to_upper_case`, `to_lower_case` |
| Math utils | `min`, `max`, `floor`, `ceil`, `round`, `abs`, `sqrt`, `random` |
| Aggregate ops | `count`, `contains_key`, `remove`, `to_string` (aggregate) |
| Print | `print`, `print_html`, `abort`, `logprint`, `debugprint`, `traceprint` |
| Character | `my_name`, `my_level`, `my_hp`, `my_maxhp`, `my_mp`, `my_maxmp`, `my_meat`, `my_adventures`, `my_fullness`, `my_inebriety`, `my_spleen_use`, `my_basestat`, `in_hardcore`, `my_familiar`, `my_class`, `my_path`, `my_sign`, `my_primestat`, `my_thrall` (stub), `in_run`, `under_standard` (live — reads `CharacterState.isUnderStandard`), `ascension_number`, `can_interact`, `can_adventure`, `prepare_for_adventure` |
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
| Economy | `hermit(item, count)` and `hermit(count, item)` (both overloads); `npc_price`, `autosell_price` (GameDatabase lookups) |
| Game actions | `adventure`, `use_skill` (×2), `adv1` (single-adventure form), `cli_execute` (partial — 6 patterns dispatched: mood/set/get/cast×2/familiar) |
| Timing | `wait`, `waitq` |

**Still missing by category:**

| Category | Key absent functions |
|----------|---------------------|
| Type conversions | `to_modifier`, `to_path`, `to_thrall`, `to_vykea`, `to_servant`, `to_bounty` (niche entity types) |
| Web scripting | `load_html`, `write`, `writeln`, `form_field`, `make_url` |
| Economy | `buy` (×3), `sell`, `retrieve_item`, `retrieve_price`, `overdrink`, `eatsilent`, `drinksilent` |
| Collections | `put_display`, `take_display`, `put_stash`, `take_stash`, `empty_closet` |
| Shop management | `reprice_shop`, `take_shop` |
| Pricing | `mall_price`, `historical_price` |
| Adventure prep | `set_location` |
| Equipment | `outfit`, `have_outfit`, `retrieve_outfit` |
| CLI | `cli_execute` remaining dispatch (`equip`, `unequip`, `outfit`, `sell/autosell`, `buy`; ~90 unhandled patterns), `cli_execute_output` |
| Logging | `dump` |
| Environment | `is_dark_mode`, `is_headless`, `get_revision`, `get_version`, `get_path` |
| Combat | `copiers_used` real tracking, `can_still_steal`, `get_ccs_action` |
| Coinmaster | `is_accessible`, `inaccessible_reason`, `visit` (coinmaster variants) |

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
| Testing | 411 test classes, many integration | 93 unit test files, 1,176 tests | Desktop wins on volume; mobile wins on isolation |
| Scripting | Full ASH + CLI (890 functions) | Partial ASH (~98 functions, 11%) | Desktop wins; closing gap |
| Events | Ad-hoc listeners | GameEventBus pub/sub | Mobile is cleaner |
| Choice automation | ~1,000 handler cases | ~80 active IDs; all 6 solvers implemented | Good coverage of common paths |
| Recovery/mood | 9 classes, full persistence + mood library | 6 files, named library + malignant clearing + AT song eviction | Closing gap — inheritance remains |
| ManaBurn | Full — any buff, summons, per-skill priority | Partial — mood-trigger effects only | Desktop wins on coverage |
| Banish tracking | 70 banishers, queue model, phylum, full routing | **70 banishers (parity)**, 37 patterns, zone pre-flight, daycount-gated | Enum parity; queue/phylum remain |
| Breakfast / daily actions | ~22 actions, outfit checkpointing | **20/22 actions** — all core items; missing only Jackass Plumber + Sea Jelly | **Near parity** — niche items remain |
| Item HTTP actions | Broad coverage | 7 request classes + HermitRequest | Core consumption + selling + hermit covered |

---

## Top Priorities

_Phase 14 completed items 1–7 from the previous list. Updated priorities:_

1. **`get_closet()` / `get_storage()` live data** — Both return empty stubs. Wire
   `ClosetRequest` and `StorageRequest` to populate the aggregate via HTTP fetch + inventory HTML parse.
   Many scripts check closet/Hagnk's contents before deciding to buy or retrieve items; blind returns
   silently break these checks.

2. **`buy()` / `retrieve_item()`** — `buy` needs a `MallSearchRequest` HTML parser to identify the
   cheapest seller and submit the purchase. `retrieve_item` is a compound operation (check closet →
   check storage → mall buy). Both are blocked on the HTML parser. High impact: these underpin most
   end-to-end automation scripts.

3. **`cli_execute` remaining dispatch** — Phase 14 added `cast N/1` and `familiar`. Still missing:
   `equip <slot> <item>`, `unequip <slot>`, `outfit <name>`, `sell <n> <item>`, `autosell <n> <item>`,
   `buy <n> <item>`. Add patterns to `cliDispatch` in `GameRuntimeLibrary.kt`; wire to existing
   `InventoryManager`/`MallPriceManager` methods where available.

4. **`eatsilent()` / `drinksilent()` / `overdrink()`** — Mobile has `eat`/`drink` but not the silent
   or overdrink variants. `EatFoodRequest` and `DrinkBoozeRequest` already exist; silent variants skip
   the fullness/inebriety capacity check. `overdrink` calls `DrinkBoozeRequest` ignoring the limit.
   3 additional `regFn` calls wrapping existing request classes.

5. **Mood inheritance** — `Mood.java` parses `"moodA extends default"` parent merge. Mobile `Mood` is
   flat. Add an optional `parent: String?` field to `Mood`, and in `MoodManager.executeActiveMood()`
   merge parent triggers before evaluating child triggers. Enables the "default" base mood pattern
   used by almost every power user.

6. **`put_display()` / `take_display()` / `put_stash()` / `take_stash()`** — Display case and clan
   stash operations. `get_stash()` and `get_display()` are already stubs; add the corresponding
   mutating calls. Requires HTTP wrappers for `displaycollection.php` and `clan_stash.php`.

7. **Outfit / equipment query functions** — `outfit(name)`, `have_outfit(name)`,
   `retrieve_outfit(name)` require tracking outfit state in `InventoryManager`. Outfit data is
   available via the character API; needs parsing + a new `OutfitManager` or extension of equipment
   state.

8. **BANISHER_PATTERNS gap fill** — 20 banishers have enum entries but no fight-HTML pattern. Each is
   a one-liner in `BANISHER_PATTERNS`. High-value ones that fire through fight HTML: `BATTER_UP`,
   `HUMAN_MUSK`, `MARK_YOUR_TERRITORY`, `BALEFUL_HOWL`.

9. **VampOut option-0 edge case** — When the solver's target location (Vlad/Isabella/Masquerade) is
   already visited, `vladChoice` evaluates to `0` (invalid KoL option). Should return `null` to fall
   through to preference fallback. Tracked as background task.

10. **`visit_url` POST body double-encoding** — Current `doPost` re-splits and re-encodes the
    `post_data` string, double-encoding any pre-encoded values. Scripts passing pre-encoded POST bodies
    (e.g., names with spaces) get corrupted requests. Tracked as background task.
