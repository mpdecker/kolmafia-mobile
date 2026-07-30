# Phase 230 — AshP248 ItemMaximumUses v2

**Date:** 2026-07-29  
**Revision:** `phase230`  
**Batch:** AshP248

## Goal

Close the deferred `$item[dailyusesleft]` v2 gap by porting desktop `UseItemRequest.maximumUses` early guards: multi-fight, choice-follows-fight, choice-in-progress, limit-mode item bans, Beecore/Glover/Robocore path gates, and item-specific exceptions (Beecore-usable foldables, Cobb's Knob map → encryption key count).

## Changes

- `ItemUseLimitsContext` extended with combat/choice flags, `canUsePotions`, and `accessibleCount`
- `ItemMaximumUses.earlyMaximumUses()` before existing eat/drink/spleen/restore/daily-limit logic
- `LimitModeGates.limitItem()` for spelunky/batman/ed/bird/roach/mole/astral modes
- `ItemDatabase.unusableInBeecore()` / `unusableInGLover()` via `Beeosity`
- `AdventureManager.inChoiceResolution` during `resolveChoice()` loop
- `GameRuntimeLibrary.buildItemUseLimitsContext()` wired into `$item[dailyusesleft]`
- `GameRuntimeLibrary.AshP248Batch.kt` registered; `REVISION = phase230`

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```

## Deferred

- Full `canWalkAwayFromChoice` HTML parity
- ConsumptionType binge/robortender/pasta-guardian branches
- Glover cafe/restaurant item exceptions
