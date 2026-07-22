# Phase 136: AshP94 skill-granting equipment + coat-of-paint

**Date:** 2026-07-21  
**Revision:** `phase136`  
**Tests:** 2,616

## Goal

Port desktop `checkSkillGrantingEquipment` conditional-skill merge into modifier context, and add `checkCoatOfPaint` with closet accessibility plus class-change forced refresh.

## Delivered

### SkillGrantingEquipmentSync

- [`ModifierDatabase.inventorySkillProviderNames()`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/ModifierDatabase.kt) — index built at load for items with `Conditional Skill (Inventory)` or equipped NC conditional skills
- [`stringsFromEntry`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Modifiers.kt) helper for multi-value string modifiers
- [`SkillGrantingEquipmentSync.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/SkillGrantingEquipmentSync.kt) — desktop filter: inventory skills always; equipped skills only when NC
- [`GameRuntimeLibrary.buildCurrentModifiers()`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt) merges API skills + equipment-granted skills
- [`ExpressionContext.from`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/modifiers/ExpressionContext.kt) lowercases skill names for `hasSkill()` parity

### checkCoatOfPaint

- Coat removed from v1 `PREF_TO_ITEM`; dedicated [`checkCoatOfPaint`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/DynamicItemModifierSync.kt) uses `isAccessible()` (closet-inclusive)
- [`checkDynamicModifiers`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt) tracks `_lastKnownClass` pref for class-change forced desc refresh

### Tests

- [`SkillGrantingEquipmentSyncTest.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/quest/SkillGrantingEquipmentSyncTest.kt)
- Extended [`DynamicItemModifierSyncTest.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/quest/DynamicItemModifierSyncTest.kt) coat cases

## Deferred (Phase 137+)

- Eternity codpiece gem slots + `EternityCodpiece` modifier type in equipment
- Full desktop `itemAvailable` (mall/storage/stash/coinmaster)
- Post-bird `skillManager.fetchSkills()` / explicit `learnSkill`
- Crown/bjorn familiar-id parsing from item desc HTML

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```
