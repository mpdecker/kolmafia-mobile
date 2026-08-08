# Phase 329 — barrelprayer CLI (TCRS applyModifiers v70)

**Track:** CLI long-tail + TCRS `applyModifiers` v70  
**Revision:** `phase329`

## Goal

Wire the `barrelprayer` CLI deferred from Phases 327–328: automate da.php barrel shrine visits + choice 1100 for protection/glamour/vigor items and the class-buff (option 4) path.

## Delivered

- `BarrelChoiceMapper` — `OPTION_BUFF`, `PRAYERS`, `findPrayer()`, `resultNameForOption()`, `applyPrayerSuccess()`
- `BarrelPrayerRequest` — desktop preflight gates + shrine visit + choice 1100 HTTP + success pref tails
- `GameRuntimeLibrary.BarrelPrayer.kt` — `cliBarrelPrayer` + `cliDispatch` regex wiring
- `BarrelPrayerRequestTest` (9 tests)

## Deferred (Phase 330+)

- Guild `buyskill` meat/skill sync + concoction refresh
- Guild quest handlers (`handleGuildQuests` meatcar/citadel/etc.)
- `DreadScrollManager.decorate()` + choice 703 quest UI sync
- Maximizer `barrelprayer` candidate scoring
