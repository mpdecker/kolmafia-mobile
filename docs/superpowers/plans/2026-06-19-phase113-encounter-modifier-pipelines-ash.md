# Phase 113: AshP71 encounter modifier pipelines

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase113`

## Goal

Port desktop post-OCRS encounter modifier pipelines (Intergnat, Nuclear Autumn, mask, dinosaur, hat) and leet name resolution so `MonsterStatusTracker` receives the correct stripped name and full modifier list before AshP70 stat application.

## Deliverables

| Area | Change |
|------|--------|
| `EncounterModifierPipeline.kt` | Intergnat / Nuclear Autumn / mask / dinosaur / hat handlers |
| `MonsterDatabase` | `leetify` map + `translateLeetMonsterName()` |
| `RandomModifierParser` | Leet OCRS token → canonical name |
| `AdventureManager` | Pipeline between OCRS parse and template resolve |
| Path gates | `DISGUISES_DELIMIT`, `DINOSAURS`, `CharacterState` helpers, `FamiliarIds.INTERGNAT` |
| REVISION | `phase113` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred (Phase 114+)

- Beeosity / Beecore stat multiplier
- Intergnat demon name sync
- AshP8–P18 stubs / Maximizer Evaluator
