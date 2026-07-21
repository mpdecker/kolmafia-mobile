# Phase 116: AshP74 equipment beeosity (Maximizer / BHY)

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase116`

## Goal

Port desktop equipment beeosity counting and wire BHY gear limits into Maximizer goal parsing and constraint checks when `CharacterState.inBeecore`.

## Deliverables

| Area | Change |
|------|--------|
| `Beeosity.kt` | `itemBeeosity`, `equipmentBeeosity`, `hasBeeosity` |
| `MaximizeSpec.maxBeeosity` | Default 2; parse `beeosity` / `beeosity N`; equip floor |
| `MaximizerManager` | Per-item + loadout total gates when in Beecore |
| REVISION | `phase116` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred (Phase 117+)

- Inventory beeotch / consumable beeosity in full Evaluator
- Summoning chamber demon summon HTTP
- AshP8–P18 remaining stubs / Maximizer Evaluator unified port
