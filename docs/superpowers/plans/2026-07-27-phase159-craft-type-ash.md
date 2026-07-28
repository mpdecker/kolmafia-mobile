# Phase 159: AshP117 craft_type ASH

**Date:** 2026-07-27  
**Revision:** `phase159`  
**Ash batch:** AshP117

## Summary

Phase 159 wires live `craft_type(item)` ASH by porting desktop concoction method-token descriptions onto mobile `ConcoctionData.methods` from `concoctions.txt`.

## Delivered

### Craft type description

- **`CraftTypeDescription.kt`** — maps concoctions.txt method tokens to desktop description strings (primary type + requirement suffixes); ignores `ROW*` and informational flags
- **`ConcoctionData.craftTypeDescription()`** extension in `ConcoctionExtensions.kt`
- **`ConcoctionDatabase.craftTypeForItem()`** convenience lookup

### AshP117 wiring

- **`GameRuntimeLibrary.AshP117Batch.kt`** — registers `craft_type(item)` and `craft_type(id)` → description or `"none"`
- Wired in `GameRuntimeLibrary.registerAll` after AshP116

### Tests

- `CraftTypeDescriptionTest` — token matrix (COMBINE, STILL, JEWEL, COOK+PASTAMASTERY, ACOCK, ROW ignore, unknown)
- `GameRuntimeLibraryAshP117Test` — ASH integration + INT overload + alias resolution
- Corpus: `corpus_craftType_fromConcoctionDatabase`

## Deferred (unchanged)

- Crafting permission runtime (PERMIT_METHOD / adventure cost)
- `mood_execute()` ASH (Phase 161 candidate)
- `desc(entity)` HTTP prefetch
- Garden crop yield, mushroom plot square parse

## Verification

- `.\gradlew.bat :shared:jvmTest` — 2,865 tests pass
- `.\gradlew.bat :androidApp:assembleDebug` — OK
