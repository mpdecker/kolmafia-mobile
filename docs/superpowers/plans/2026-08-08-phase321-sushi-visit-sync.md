# Phase 321 — sushi.php visit consumption sync

**Track:** TCRS `applyModifiers` v62  
**Revision:** `phase321`

## Summary

Ports desktop `ResponseTextParser` sushi.php visit handling so manual browser eats and `visit_url` POSTs sync ingredient deduction, fullness, fancy doily, and worktea clue — not only automated `SushiCreateRequest` craft.

## Delivered

- `SushiChoiceMapper.formFieldsFromUrl()` — URL query param extraction (`whichsushi`, `whichtopping`, `whichfilling1`, `veggie`, `dipping`)
- `SushiConsumptionSync.parseConsumptionFromVisit()` — visit wrapper with `updateFullness=true`
- `GameRuntimeLibrary.processVisitResponseHooks` sushi.php hook + `eventBus` DI wiring
- Tests: `SushiChoiceMapperTest`, `SushiConsumptionSyncTest`, `GameRuntimeLibraryAshP321Test`

## Deferred Phase 322+

- RollingPin `makeDough` depth (NPC dough purchase recursion)
- Full `DreadScrollManager`
- lazy-load class/sign TCRS files (document as non-goal)
