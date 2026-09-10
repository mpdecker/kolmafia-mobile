# ASH 100% Sprint — XLVI Combat+Adventure (6611–6670)

> **For agentic workers:** Use existing `GameRuntimeLibrary.*.kt` deepen patterns from XLIII–XLV. Do NOT bump `REVISION` (parent wraps to `phase6670`). Exclude `git_*`/`svn_*`, `user_*`, Relay/JS, full xpath.

**Goal:** Close ~25–35 combat/adventure shallow ASH edges toward honest 100% live (~883 excl. non-goals).

**Desktop:** `C:\Development\kolmafia\kolmafia\src\net\sourceforge\kolmafia\textui\RuntimeLibrary.java`  
**Mobile tip:** `phase6610` → wrap `phase6670`

## Tracks (parallel; no REVISION bump)

### Track A — Combat prediction (6611–6630)
Deepen vs desktop FightRequest formulas:
- `expected_damage` / `elemental_resistance` residual beyond tracker/Hero/shield
- `will_usually_miss` / `will_usually_dodge` hit/dodge polish
- `item_drops` / `item_drops_array` / `meat_drop` / `jump_chance` conditional/OCRS residual
- Marker: `GameRuntimeLibrary.Phase6611.kt`
- Tests: `GameRuntimeLibraryPhase6630Test.kt` + `corpus_behavioralDeepenXlviTracksA_live`

### Track B — Rates + fight actions (6631–6650)
- `appearance_rates` / `get_location_monsters` conditional weighting leftovers
- `combat_skill_available` fight-dropdown depth
- `attack` / `steal` / `twiddle` / `runaway` BUFFER offline vs live path
- Marker: `GameRuntimeLibrary.Phase6631.kt`
- Tests: `GameRuntimeLibraryPhase6650Test.kt` + `corpus_behavioralDeepenXlviTracksB_live`

### Track C — Adventure / choice / CCS (6651–6670)
- `can_adventure` / `pre_validate_adventure` / `prepare_for_adventure` AdventurePrep/Failures residual
- `adventure` / `adv1` filter arity residual
- `run_choice` spoiler/multi-arg beyond `-1`; `run_combat` CCS/filter residual
- `get_ccs_action` / `set_ccs` / `write_ccs` CustomCombatLookup reload edges
- `available_choice_options` / `can_walk_from_choice` / `tavern` residual
- `get_auto_attack` / `set_auto_attack` account sync if thin
- Marker: `GameRuntimeLibrary.Phase6651.kt`
- Tests: `GameRuntimeLibraryPhase6670Test.kt` + `corpus_behavioralDeepenXlviTracksC_live`

## Parent wrap
- Bump `"phase6610"` → `"phase6670"` (asserts + REVISION)
- Update `docs/parity-audit.md` Top Priorities + Phase History + `AGENTS.md`
- Focused gradle: Phase6630/6650/6670 + Xlvi corpus tracks

## Non-goals this mega
HTTP residual; XLVII economy/character; inventing non-desktop names.
