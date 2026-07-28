# Phase 139: AshP97 Throne/Bjorn modifier accumulation

**Date:** 2026-07-22  
**Revision:** `phase139`  
**Tests:** 2,639

## Goal

When Crown of Thrones or Buddy Bjorn is **worn**, apply bundled `Throne` modifiers for the enthroned/bjorned familiar race in `CurrentModifiers` so advisor/maximizer `buildCurrentModifiers()` matches desktop gear-change predictions.

## Delivered

### ModifierDatabase

- [`ModifierDatabase.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/ModifierDatabase.kt) — `getThrone(race)` (case-insensitive `Throne` tab lookup); `getBjorn(race)` aliases `getThrone` (desktop `BJORN → THRONE`)

### CurrentModifiers

- [`CurrentModifiers.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/modifiers/CurrentModifiers.kt) — step **1c**: HAT=`Crown of Thrones` + non-blank `enthronedFamiliarName` → throne modifiers; step **1d**: CONTAINER=`Buddy Bjorn` + non-blank `bjornedFamiliarName` → bjorn (throne) modifiers; skips blank/`none` modifier rows

### Tests

- [`CurrentModifiersThroneBjornTest.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/modifiers/CurrentModifiersThroneBjornTest.kt) — crown/bjorn worn + race → modifiers; negative cases; `none` row skipped
- [`ModifierDatabaseThroneTest.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/data/ModifierDatabaseThroneTest.kt) — case-insensitive lookup + bjorn alias
- [`GameRuntimeLibraryAshP97Test.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryAshP97Test.kt) — `revision_phase139`

## Deferred (Phase 140+)

- Full desktop `InventoryManager.itemAvailable` (mall/NPC/coinmaster pref gates)
- Post-bird `skillManager.fetchSkills()` / explicit `learnSkill`
- Codpiece equip HTTP automation

## Notes

- `enthronedFamiliarName` / `bjornedFamiliarName` store familiar **race** (Phase 138 desc sync); `Throne` tab keys match race names
- Modifiers apply only when crown/bjorn is **equipped** (not merely in inventory)
