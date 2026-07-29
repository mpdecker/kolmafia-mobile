# Phase 212: AshP219 Pulverize HTTP/CLI + AshP220 get_related ASH

## Summary

Closed the pulverize automation gap after the shop track (AshP197–AshP218): HTTP smash requests, `pulverize`/`smash` CLI aliases, and live `get_related(item, "pulverize")` ASH on top of the existing `EquipmentDatabase` pulverize loader.

## Delivered

### AshP219 — Pulverize HTTP + CLI

- **`PulverizeRequest.kt`** — POST `craft.php` with `action=pulverize`, `smashitem`, `qty`, `ajax=1`, `conftrade=1`; `parseResponse()` failure detection; local inventory consumption + `fetchInventory()` on success
- **`GameRuntimeLibrary.kt`** — `pulverize|smash [N] item[, item]...` CLI regex in `cliDispatch`; `runPulverizeCli()` with optional count (defaults to full inventory)
- **`SharedModule.kt`** — `PulverizeRequest` DI wired into `GameRuntimeLibrary`
- **`GameRuntimeLibrary.AshP219Batch.kt`** — batch marker
- **Tests** — `PulverizeRequestTest`, `GameRuntimeLibraryCliTest` pulverize/smash cases, `GameRuntimeLibraryAshP219Test`

### AshP220 — get_related ASH + login prefetch

- **`GameRuntimeLibrary.Related.kt`** — live `get_related(item, "pulverize")` via `EquipmentDatabase.getPulverization()` → `PulverizeAggregate.decodeToAggregate()`
- **`GameRuntimeLibrary.AshP220Batch.kt`** — registers `registerRelatedFunctions`
- **`checkDynamicModifiers()`** — one-time `EquipmentDatabase.initializePulverization()` gated by `_pulverizationInitialized` pref
- **Tests** — `GameRuntimeLibraryAshP220Test`, `AshCompatibilityCorpusTest` `corpus_getRelatedPulverize_live` + `corpus_pulverizeCli_live`

### Docs / revision

- **`GameRuntimeLibrary.REVISION`** — `phase212`
- **`docs/parity-audit.md`** — 3,548 tests, Phase 212 history, Tier 1 CLI pulverize bullet

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```

Result: **3,548 tests**, all green; Android debug build successful.

## Deferred (Phase 213+)

- `cleanup junk` full desktop junk-list automation
- `get_related(item, "fold"|"zap")` branches
- Tender-hammer auto-retrieve before smash
- `bastille.txt` manager (Tier 3 #7)
