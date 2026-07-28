# Phase 160: AshP118 refresh_status + restore_hp/mp ASH

**Date:** 2026-07-27  
**Revision:** `phase160`  
**Ash batch:** AshP118

## Summary

Phase 160 wires live `refresh_status()`, `restore_hp(amount)`, and `restore_mp(amount)` ASH — the next desktop RuntimeLibrary player-status functions after `craft_type()`.

## Delivered

### Checkpointed recovery

- **`RecoveryManager.checkpointedRecoverHp/Mp`** — loop until target HP/MP with post-action state refresh; `amount > 0` targets explicit value, `amount == 0` uses stop-pct prefs
- **`hpRecoveryTarget` / `mpRecoveryTarget`** — companion helpers for target semantics

### AshP118 wiring

- **`GameRuntimeLibrary.AshP118Batch.kt`** — registers `refresh_status()` (api.php status only), `restore_hp(amount)`, `restore_mp(amount)`
- **`refreshCharacterStates()`** — shared api.php refresh helper for recovery loops
- Wired in `GameRuntimeLibrary.registerAll` after AshP117

### Tests

- `RecoveryManagerTest` — checkpoint target + loop + no-restore cases
- `GameRuntimeLibraryAshP118Test` — ASH integration for refresh/restore
- Corpus: `corpus_refreshStatus_updatesCharacterHp`, `corpus_restoreHp_toAmount`

## Deferred (unchanged)

- `mood_execute()` ASH (Phase 161 candidate)
- Desktop charpane fallback for `refresh_status` (Valhalla/limit modes/transfunctioner)
- Purchase-based restore items in checkpointed loop
- Recovery custom scripts (`invokeRecoveryScript`)
- `desc(entity)` HTTP prefetch
- Garden crop yield, mushroom plot square parse

## Verification

- `.\gradlew.bat :shared:jvmTest` — 2,881 tests pass
- `.\gradlew.bat :androidApp:assembleDebug` — OK
