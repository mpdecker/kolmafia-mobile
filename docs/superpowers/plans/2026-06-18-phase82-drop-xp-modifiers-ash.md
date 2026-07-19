# Phase 82: AshP40 Drop / XP / Initiative Modifiers

**Date:** 2026-06-18  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase82`

## Goal

Register and implement the stretch drop/XP/initiative ASH cluster deferred from Phase 81: `initiative_modifier`, `experience_bonus`, `meat_drop_modifier`, `item_drop_modifier`.

## Deliverables

| Area | Change |
|------|--------|
| Math | `CombatAdjustment` ASH-facing helpers matching desktop `KoLCharacter` (penalty `min(..., 0)`; item drop excludes GEARDROP; XP is prime-stat only) |
| ASH | AshP40 no-arg float wrappers |
| Tests | `CombatAdjustmentTest` formula cases, `GameRuntimeLibraryAshP40Test`, corpus |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- Adventure-queue-aware `appearance_rates(..., true)`
- `monster_attack` / `monster_defense` / `will_usually_miss` suite
- `numeric_modifier(monster, …)`, Hero of Half Shell
- `bastille.txt`, `nonfilling.txt`, Maximizer Evaluator
- Changing `CurrentModifiers.*Bonus()` semantics (maximizer may rely on GEARDROP)
