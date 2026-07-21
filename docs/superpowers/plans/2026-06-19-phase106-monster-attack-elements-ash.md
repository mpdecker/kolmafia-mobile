# Phase 106: AshP64 `$monster[attack_elements]`

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase106`

## Goal

Wire MonsterProxy plural attack-element bracket and multi-`EA:` parse; align singular `attack_element` with desktop EnumSet iteration order.

## Deliverables

| Area | Change |
|------|--------|
| Element order | `ElementTypes.kt`: enum order, `canonicalElementOrder`, `primaryAttackElement` |
| Model | `attackElements: List<String>` on `MonsterDefinition`; `attackElement` = primary |
| Parse | Accumulate all `EA:` tokens including quoted (`"bad spelling"`) |
| Bracket | `$monster[attack_elements]` → `element[int]` aggregate |
| Bracket | `$monster[attack_element]` uses enum-order last |
| REVISION | `phase106` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- Remaining MonsterProxy: `sub_types`, `images`, `random_modifiers`, `attributes`, `fact`/`fact_type`
- Beeosity + Beecore / BIG-core
- Manuel manager; queue `appearance_rates`; MonsterStatusTracker
- Maximizer Evaluator (Tier 2)
