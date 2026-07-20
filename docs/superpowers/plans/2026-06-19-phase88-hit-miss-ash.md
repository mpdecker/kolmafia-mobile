# Phase 88: AshP46 Hit/Miss Combat APIs

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase88`

## Goal

Desktop-lite `will_usually_miss` / `will_usually_dodge` / `buffed_hit_stat` / `current_hit_stat` using AshP41 last-monster ML stats and Mox-req weapon hit-stat (no full MonsterStatusTracker).

## Deliverables

| Area | Change |
|------|--------|
| Math | `hitPercent`, `willUsuallyMiss`/`Dodge`, `buffedHitStat`, `hitStatKind` |
| ASH | AshP46 four no-arg APIs |
| Tests | Formula, AshP46, corpus |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- Full `MonsterStatusTracker` / mid-combat delevel
- Knife / Fourth Saber / Surprising Fist hit-stat edges
- Jump Overclocked / missing-`Init:`; queue-aware appearance rates
- Maximizer Evaluator; bastille/nonfilling
