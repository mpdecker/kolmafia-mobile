# Phase 137: AshP95 Eternity codpiece gem slots

**Date:** 2026-07-22  
**Revision:** `phase137`  
**Tests:** 2,624

## Goal

Close the Phase 136 deferral: parse `eternitycod` from api.php status into CODPIECE1–5 equipment slots, apply EternityCodpiece gem modifiers, extend skill-granting equipment for gem conditional skills, and align `have_skill` with equipment-granted skills.

## Delivered

### Character API + equipment slots

- [`EquipmentSlot.CODPIECE1`–`CODPIECE5`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/character/EquipmentSlot.kt) with `codpiece1`…`codpiece5` api keys
- [`CharacterApiResponse.eternitycod`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/character/CharacterApiResponse.kt) — int array from status JSON
- [`KoLCharacter.buildEquipmentMap`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/character/KoLCharacter.kt) resolves gem ids via `ItemDatabase.getById`

### ModifierDatabase

- [`getEternityCodpiece`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/ModifierDatabase.kt) + `isCodpieceGem(itemId)`

### SkillGrantingEquipmentSync codpiece branch

- [`CheckContext.codpieceGemNames`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/DynamicItemModifierSync.kt) populated from CODPIECE slots in [`buildCheckContext`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt)
- [`SkillGrantingEquipmentSync`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/SkillGrantingEquipmentSync.kt) — when The Eternity Codpiece is equipped or in inventory, merge inventory + equipped NC conditional skills from each gem's `EternityCodpiece` modifiers

### CurrentModifiers + have_skill

- [`CurrentModifiers`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/modifiers/CurrentModifiers.kt) step 1b accumulates gem modifiers when codpiece context is active
- [`resolvedSkillNames()`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt) shared by `buildCurrentModifiers()` and `have_skill`

### Tests

- [`KoLCharacterTest`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/character/KoLCharacterTest.kt) — `eternitycod` → CODPIECE slots
- [`SkillGrantingEquipmentSyncTest`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/quest/SkillGrantingEquipmentSyncTest.kt) — codpiece gem skill grant/filter
- [`CurrentModifiersCodpieceTest`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/modifiers/CurrentModifiersCodpieceTest.kt) — gem Muscle modifier
- [`GameRuntimeLibraryAshP95Test`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryAshP95Test.kt) — `have_skill` for equipment-granted NC skill

## Deferred (Phase 138+)

- Full desktop `itemAvailable` (mall/NPC/coinmaster pref gates)
- Post-bird `skillManager.fetchSkills()` / explicit `learnSkill`
- Crown/bjorn familiar-id parsing from item desc HTML
- Codpiece equip HTTP (`inventory.php?action=docodpiece`) automation
