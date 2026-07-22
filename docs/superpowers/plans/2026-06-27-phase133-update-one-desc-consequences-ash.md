# Phase 133: AshP91 consequence updateOneDesc rotation

**Date:** 2026-06-27  
**Revision:** `phase133`  
**Tests:** 2,578

## Goal

Port desktop `ConsequenceManager.updateOneDesc()` — build an ordered catalog of desc URLs from consequences.txt at load time, rotate one prefetch visit per KoL day on rollover/login day change, reusing existing desc consequence hooks.

## Delivered

### URL catalog

- [`DescriptionConsequenceRegistry.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/DescriptionConsequenceRegistry.kt) — file-order DESC_ITEM/EFFECT/SKILL URLs; tab split preserves trailing empty fields (desktop `split("\t", -1)`); includes empty-regex rows like Kremlin/Baseball Diamond
- Wired via [`GameDatabase.load()`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/GameDatabase.kt)

### KoL day index

- [`KoLRolloverCalendar.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/KoLRolloverCalendar.kt) + `expect fun kolRolloverDayDifference()` in [`Platform.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/Platform.kt) with JVM/Android `java.time` actuals (GMT-0330, 2005-09-17 epoch, White Wednesday boundary)

### Visit orchestration

- [`DescriptionConsequenceSync.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/DescriptionConsequenceSync.kt) — `pathForToday(dayDifference)`
- [`GameRuntimeLibrary.updateOneDesc()`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt) — visits selected desc via existing `visitKolPage` + consequence hooks
- [`SessionManager`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/SessionManager.kt) — calls `updateOneDesc()` on login when `dayCount` changed

### Tests

- `DescriptionConsequenceRegistryTest`, `DescriptionConsequenceSyncTest`, `KoLRolloverCalendarTest`

## Deferred (Phase 134+)

- Extended `InventoryManager.checkMods()` special cases
- Closet-inclusive accessibility for mod checks

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```
