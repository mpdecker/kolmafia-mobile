# Phase 104: AshP62 `monster_initiative()` initPenalty

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase104`

## Goal

Route `monster_initiative()` through `monsterInitiativeWithMl` so it matches `$monster[base_initiative]`, `jump_chance`, and desktop `getInitiative()` when ML > 20.

## Deliverables

| Area | Change |
|------|--------|
| AshP41 | Both `monster_initiative` overloads use `monsterInitiativeWithMl` + `currentMl()` |
| Tests | AshP41 raincore expects initPenalty; AshP62 coverage |
| REVISION | `phase104` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- `$monster[poison|group|manuel_name|wiki_name]`
- Beeosity + Beecore / BIG-core
- Manuel manager; queue `appearance_rates`; MonsterStatusTracker
- Maximizer Evaluator (Tier 2)
