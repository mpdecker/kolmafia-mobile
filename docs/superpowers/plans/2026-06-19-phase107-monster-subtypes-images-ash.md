# Phase 107: AshP65 `$monster[sub_types]` + `$monster[images]`

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase107`

## Goal

Wire MonsterProxy subtype and image plural brackets; parse subtype keyword flags and comma-separated image columns; fix singular `image` to first filename only.

## Deliverables

| Area | Change |
|------|--------|
| Model | `subTypes: List<String>`, `images: List<String>` on `MonsterDefinition` |
| Parse | 9 subtype flags (BUGBEAR, GHOST, SKELETON, …); comma-split image column |
| Bracket | `$monster[sub_types]` and `$monster[images]` → `string[int]` |
| Bracket | `$monster[image]` returns first image only |
| REVISION | `phase107` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- `$monster[random_modifiers]`, `attributes`, `fact`/`fact_type`
- Beeosity + Beecore / BIG-core
- Manuel manager; queue `appearance_rates`; MonsterStatusTracker
- Maximizer Evaluator (Tier 2)
