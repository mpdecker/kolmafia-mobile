# Phase 81: AshP39 Combat Adjustment Library

**Date:** 2026-06-18  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase81`

## Goal

Register and implement the high-traffic combat-adjustment ASH cluster deferred from Phase 80: `expected_damage`, `monster_level_adjustment`, `elemental_resistance`, and related DA/DR/ML/mana/weight wrappers on live `CurrentModifiers`.

## Deliverables

| Area | Change |
|------|--------|
| Data | `MonsterDefinition.attackElement` + `MonsterDatabase` `EA:` parse |
| Math | `CombatAdjustment` helpers (resistance-by-level, DA%, ML + raincore water×10, expected_damage) |
| ASH | AshP39: ML/weight/mana/DA/DR/combat_rate wrappers + `elemental_resistance` (0/1-arg) + `expected_damage` (0/1-arg) |
| Tests | `CombatAdjustmentTest`, `MonsterDatabaseAttackElementTest`, `GameRuntimeLibraryAshP39Test`, corpus |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- Adventure-queue-aware `appearance_rates(..., true)`
- Class/path monster remaps
- Full MonsterStatusTracker attack modifiers
- Hero of the Half Shell path
- `bastille.txt`, pulverize, campground inventory
- `numeric_modifier(monster, …)`
- Stretch drop/XP modifiers (`meat_drop_modifier`, etc.)
