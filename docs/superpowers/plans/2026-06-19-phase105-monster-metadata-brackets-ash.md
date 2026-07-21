# Phase 105: AshP63 `$monster[poison|group|manuel_name|wiki_name]`

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase105`

## Goal

Wire MonsterProxy metadata brackets: poison as EFFECT (severity → effect name), group INT (default 1), manuel_name / wiki_name STRING (default monster name).

## Deliverables

| Area | Change |
|------|--------|
| Model | `group` / `manuelName` / `wikiName` on `MonsterDefinition` |
| Parse | `Group:` / `Manuel:` / `Wiki:` |
| Poison | `PoisonLevels.effectNameForLevel` |
| Bracket | Four fields on `MonsterEntityFields` |
| REVISION | `phase105` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- Remaining MonsterProxy: `attack_elements`, `sub_types`, `images`, `random_modifiers`, …
- Beeosity + Beecore / BIG-core
- Manuel manager; queue `appearance_rates`; MonsterStatusTracker
- Maximizer Evaluator (Tier 2)
