# Phase 76: AshP34 MONSTER entity bracket fields

**Date:** 2026-06-18  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase76`

## Goal

Wire `$monster[field]` bracket reads from bundled `monsters.txt`, continuing the AshP32→AshP33 entity-bracket arc (SERVANT → THRALL/VYKEA → MONSTER).

## Deliverables

| Area | Change |
|------|--------|
| Fields | `MonsterEntityFields` — id, name, image, base stats, meat, phylum, boss/ghost/lucky, article, copyable, wishable |
| Parser | `MonsterDatabase` stores `Article:`, `NOCOPY`, `NOWISH` in `MonsterDefinition` |
| Wiring | `GameRuntimeLibrary.resolveEntityIndex` handles `AshType.MONSTER` |
| Tests | `GameRuntimeLibraryAshP34Test` + `corpus_monsterEntityFields_live` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- `numeric_modifier(monster, …)` — no monster modifier data file
- `$location[field]` / `$path[field]` — AshP35 candidate
- Tier 3 `bastille.txt` loader
