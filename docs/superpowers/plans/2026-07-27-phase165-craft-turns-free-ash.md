# Phase 165: AshP125 Recursive Craft Turns + AshP126 Free Crafting ASH

**Date:** 2026-07-27  
**Revision:** `phase165`  
**Tests:** 2,978

## Summary

- **AshP125:** Recursive `CreatableTurns` ingredient-tree + `creatable_turns(item, count, freeCrafting)` 3-arg overloads
- **AshP126:** `FreeCraftingTurns` + `free_crafts`/`free_cooks`/`free_mixes`/`free_smiths` ASH; SSPD/GRIMACITE `ConcoctionPermitted` gates via `KolGameHolidayCalendar`

## Key files

- `GameRuntimeLibrary.AshP125Batch.kt`
- `GameRuntimeLibrary.AshP126Batch.kt`
- `GameRuntimeLibrary.CraftAshHelpers.kt`
- `CreatableTurns.kt`, `FreeCraftingTurns.kt`
- `KolGameHolidayCalendar.kt`
- `ConcoctionPermitted.kt` (SSPD + GRIMACITE hammer gate)

## Deferred to Phase 166+

- NPC/coinmaster `validate=true` shop accessibility
- COINMASTER method in `ConcoctionPermitted`
- Robocore Bird Cage familiar gate
- VYKEA `concoction_price` overload
- Full desktop `ConcoctionDatabase.recalculatePermittedMethods`
