# Phase 87: AshP45 Meat and Item Drops

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase87`

## Goal

Live `meat_drop`, `item_drops`, and `item_drops_array` from `MonsterDefinition` drop data (last-monster + monster overloads).

## Deliverables

| Area | Change |
|------|--------|
| ASH | AshP45 `meat_drop` / `item_drops` / `item_drops_array` (6 overloads) |
| Types | `ITEM_DROP_REC` `{drop, rate, type}` |
| Tests | AshP45 + corpus |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- `will_usually_miss` / `will_usually_dodge` (needs `MonsterStatusTracker`)
- Overclocked / missing-`Init:` jump edge cases
- Queue-aware appearance rates, bastille/nonfilling, Maximizer Evaluator
