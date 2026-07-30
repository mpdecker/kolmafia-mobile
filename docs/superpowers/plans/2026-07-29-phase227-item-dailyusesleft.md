# Phase 227: AshP245 Item dailyusesleft Bracket Field

**Revision:** `phase227`  
**Follows:** Phase 226 (AshP244 item metadata bracket fields)

## Goal

Wire desktop `$item[dailyusesleft]` via a scoped v1 of `UseItemRequest.maximumUses` — food/drink/spleen capacity, daily-limit prefs, and restore caps.

## Delivered

- `DailyLimitKind` + `DailyLimitDatabase` item-id index + `getUsesRemaining(entry, preferences)` (boolean/int pref reads)
- `RestoreDatabase.isRestoreItem` + HP/MP averages + `restorationMaximum`
- `ItemMaximumUses.maximumUses` v1 (food/drink/spleen/daily Use limits/restore/default)
- `ItemEntityFields` `dailyusesleft` bracket + `GameRuntimeLibrary` character/prefs pass-through
- `GameRuntimeLibrary.AshP245Batch.kt`; `REVISION=phase227`

## Deferred

- Fight/choice-in-progress guards; 100+ item-specific switch cases; path-specific food gates; concoction queue adjustments; expression max in dailylimits.txt
- `seller`/`buyer`/`smallimage`/`tcrs_name`/`skill`/`recipe`/`noob_skill` brackets (Phase 228+)

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```

**Tests:** 3,702 (+14 from Phase 226)
