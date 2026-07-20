# Phase 101: AshP59 ML Physical Resistance Boost

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase101`

## Goal

Fold desktop fight-time `handleMonsterLevel` phys-res boost (`min(floor(ML()/2.5), 50)`) into `monsterPhysicalResistance` / `$monster[physical_resistance]`, using AshP58 effective ML (MLMult).

## Deliverables

| Area | Change |
|------|--------|
| Eval | `mlPhysicalResistanceBoost` + merge into `monsterPhysicalResistance(..., ml)` |
| Bracket | Pass `ml` into phys-res resolve in `MonsterEntityFields` |
| REVISION | `phase101` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- Beeosity + Beecore / BIG-core (needs path enums)
- `$monster[raw_hp|raw_attack|raw_defense|raw_initiative]`
- Manuel; queue `appearance_rates`; full MonsterStatusTracker
- Maximizer Evaluator (Tier 2)
