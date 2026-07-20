# Phase 99: AshP57 Phys/Elem Monster Resistance Brackets

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase99`

## Goal

Parse `Phys:` / `Elem:` / `ElemHot|Cold|Stench|Sleaze|Spooky:` and expose `$monster[physical_resistance|elemental_resistance|*_resistance]` INT brackets (desktop MonsterProxy). Per-element falls back to `Elem:` when sub-attr is 0.

## Deliverables

| Area | Change |
|------|--------|
| Model | Resistance numeric + expression fields on `MonsterDefinition` |
| Parse | `Phys:` / `Elem:` / `Elem*` via bracket-aware reader |
| Eval | `monsterPhysicalResistance` / `monsterElementalResistance` / `monsterElementResistance` |
| Bracket | Seven INT fields on `MonsterEntityFields` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- Beeosity (non-1) + Beecore; BIG-core
- `MLMult:` effective ML
- Fight-time ML phys-res boost
- Manuel; queue-aware `appearance_rates`; full `MonsterStatusTracker`
- Maximizer Evaluator
