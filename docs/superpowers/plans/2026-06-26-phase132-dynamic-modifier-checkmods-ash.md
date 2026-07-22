# Phase 132: AshP90 dynamic modifier checkMods v1

**Date:** 2026-06-26  
**Revision:** `phase132`  
**Tests:** 2,569

## Goal

Close the dynamic-modifier fetch loop deferred from Phase 131: proactively visit `desc_item.php` / `desc_effect.php` when mod prefs are empty but the player owns the item or has the effect active, plus desktop-style owned-item desc checks for Kremlin's Greatest Briefcase and Baseball Diamond.

## Delivered

### DynamicItemModifierSync extensions

- [`DynamicItemModifierSync.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/DynamicItemModifierSync.kt) — `CheckContext`, `DescVisit`, `checkMods()`, `checkOwnedItemDescriptions()`, `OWNED_DESC_ITEMS`
- Eight item + nine effect pref maps (same as Phase 127) with inventory/equipped and active-effect gating
- KGB + Baseball Diamond owned-item desc visits (no pref gate)

### Visit orchestration

- [`GameRuntimeLibrary.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt) — `checkDynamicModifiers()` + `buildCheckContext()`; reuses existing `visitKolPage` + desc consequence hooks
- `refresh` CLI calls `checkDynamicModifiers()` after inventory/effects sync
- [`SessionManager.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/SessionManager.kt) — post-login `fetchInventory()` + `fetchEffects()` + `checkDynamicModifiers()`
- [`SharedModule.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt) — wires `gameRuntimeLibrary` into `SessionManager`

### Tests

- Extended [`DynamicItemModifierSyncTest.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/quest/DynamicItemModifierSyncTest.kt) — item/effect/owned visit gating + case-insensitive equip match

## Deferred (Phase 133+)

- `ConsequenceManager.updateOneDesc()` day-rotation prefetch
- Extended desktop checks: saber, umbrella, crimbo manual, ring, zootomist, bird-of-day, crown/bjorn, etc.
- Closet-inclusive accessibility for mod checks

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```
