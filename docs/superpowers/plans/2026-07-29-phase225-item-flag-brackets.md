# Phase 225: AshP243 Item Flag Bracket Fields

**Revision:** `phase225`  
**Follows:** Phase 224 (AshP242 item restore bracket fields)

## Goal

Wire desktop `$item[field]` boolean flag brackets derivable from bundled `items.txt` data, matching desktop `ItemProxy` getters for quest/gift/tradeable/discardable, usability flags, and craft-ingredient flags.

## Scope

- `ItemDatabase` attribute helpers: `isGiftItem`, `isUsable`, `isMultiUsable`, `isReusable`, `isCombatUsable`, `isCombatReusable`, `isFancyItem`, `isPasteable`, `isSmithable`, `isCookable`, `isMixable`
- `ItemEntityFields` flag v3 bracket fields
- `GameRuntimeLibrary.AshP243Batch.kt` marker batch + `REVISION=phase225`

## Deferred

- `dailyusesleft`, `notes`, candy/chocolate/potion, seller/buyer, smallimage, name_length, tcrs_name, skill/recipe/noob_skill

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```
