# Phase 134: AshP92 extended checkMods v2

**Date:** 2026-07-20  
**Revision:** `phase134`  
**Tests:** 2,594

## Goal

Close Phase 132/133 deferrals: closet-inclusive item accessibility and remaining desktop `InventoryManager.checkMods()` / login-owned-item desc visit special cases.

## Delivered

### DynamicItemModifierSync extensions

- [`DynamicItemModifierSync.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/DynamicItemModifierSync.kt) — `CheckContext` now includes `closetItemIds` + `ascensionPath`; `isAccessible()` / `isInInventory()` helpers; `checkExtendedMods()` for saber, umbrella, vampire wine, crimbo manual, ring, G9, zootomist, heartstone
- Zoot/heartstone removed from v1 effect visit loop (handled in extended with desktop semantics)
- `_g9Effect` cached override via `applyG9CachedOverride()`
- `OWNED_DESC_ITEMS` extended with Everfull Dart Holster + mimic egg

### Visit orchestration

- [`GameRuntimeLibrary.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt) — `buildCheckContext()` fetches closet IDs; `checkDynamicModifiers()` merges v1 + extended + owned visits with `distinctBy { path }`

### Tests

- Extended [`DynamicItemModifierSyncTest.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/quest/DynamicItemModifierSyncTest.kt) — 28 tests covering all extended cases

## Deferred (Phase 135+)

- `checkBirdOfTheDay` — skill desc parse + multiple effect descs
- `checkCrownOfThrones` / `checkBuddyBjorn` — familiar-in-item desc logic
- `checkCoatOfPaint(playerClassChanged)` — class-change refresh hook
- Full desktop `itemAvailable` (mall/storage/stash/coinmaster)
- `ResultProcessor.updateEntauntauned` / `updateSavageBeast`
- `checkSkillGrantingEquipment`
- KGB/Baseball closet-inclusive owned checks

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```
