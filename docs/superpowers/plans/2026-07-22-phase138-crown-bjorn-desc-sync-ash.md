# Phase 138: AshP96 Crown/Bjorn familiar desc sync

**Date:** 2026-07-22  
**Revision:** `phase138`  
**Tests:** 2,630

## Goal

Close the Phase 135/137 deferral: parse enthroned/bjorned familiar race from crown/bjorn desc_item.php HTML and update character state so ASH familiar queries reflect inventory-held occupants before api.php refresh.

## Delivered

### CrownBjornDescSync

- [`CrownBjornDescSync.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/CrownBjornDescSync.kt) — desktop `COT_PATTERN` race extract; resolve owned familiar by race; `CLEARED` when no match

### Character state

- [`KoLCharacter.updateEnthroned`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/character/KoLCharacter.kt) / `updateBjorned` — local state without HTTP

### Visit hook

- [`GameRuntimeLibrary.processVisitResponseHooks`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt) — after `ItemDescriptionConsequenceSync`, resolve item by descId; for HATSEAT (4614) / Buddy Bjorn (7200) parse occupant when path allows familiars

### Tests

- [`CrownBjornDescSyncTest.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/quest/CrownBjornDescSyncTest.kt)
- [`GameRuntimeLibraryAshP96Test.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryAshP96Test.kt) — crown/bjorn desc visit → `my_enthroned_familiar` / `my_bjornified_familiar`

## Deferred (Phase 139+)

- Full desktop `InventoryManager.itemAvailable` (mall/NPC/coinmaster pref gates)
- Post-bird `skillManager.fetchSkills()` / explicit `learnSkill`
- Throne/Bjorn modifier accumulation in `CurrentModifiers`
- Codpiece equip HTTP automation
