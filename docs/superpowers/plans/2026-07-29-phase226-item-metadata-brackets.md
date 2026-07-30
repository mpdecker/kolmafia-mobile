# Phase 226: AshP244 Item Metadata Bracket Fields

**Revision:** `phase226`  
**Follows:** Phase 225 (AshP243 item flag bracket fields)

## Goal

Wire desktop `$item[field]` metadata brackets derivable from bundled consumable and item data: notes, potion/candy/chocolate flags, candy_type, and name_length.

## Scope

- `ConsumableDatabase.getNotesByName`
- `ItemDatabase`: `isPotion`, `isChocolateItem`, `isCandyItem`, `getCandyTypeName`, `getNameLength`
- `ItemEntityFields` metadata v4 bracket fields
- `GameRuntimeLibrary.AshP244Batch.kt` marker batch + `REVISION=phase226`

## Deferred

- `dailyusesleft` (needs `UseItemRequest.maximumUses`)
- `seller`/`buyer`, `smallimage`, `tcrs_name`, `skill`/`recipe`/`noob_skill`

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```
