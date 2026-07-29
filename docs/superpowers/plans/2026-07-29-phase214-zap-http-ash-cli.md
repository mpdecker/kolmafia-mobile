# Phase 214: AshP223 get_zap_wand + AshP224 Zap HTTP/ASH/CLI

## Summary

Delivered desktop-parity wand discovery and zap automation after Phase 213’s get_related zap map decode.

## Delivered

### AshP223 — get_zap_wand ASH

- **`WandDiscovery.kt`** — wand IDs (1267–1272), `findWand()` (sets `lastZapperWand` pref), `getZapper()` (dead mimic auto-use when ascension allows)
- **`GameRuntimeLibrary.Zap.kt`** — `get_zap_wand()` ASH via `WandDiscovery.findWand`
- **`GameRuntimeLibrary.AshP223Batch.kt`** — batch marker
- **Tests** — `GameRuntimeLibraryAshP223Test`, `AshCompatibilityCorpusTest.corpus_getZapWand_live`

### AshP224 — ZapRequest HTTP + zap ASH + CLI

- **`ZapRequest.kt`** — POST `wand.php` (`action=zap`, `whichwand`, `whichitem`); retrieve-before-zap; `parseResponse` (acquire parse, `"nothing happens"`, wand explosion + `_zapCount`/`lastZapperWandExplosionDay` prefs); returns acquired item id
- **`GameRuntimeLibrary.Zap.kt`** — `zap(item)` ASH returns acquired item or empty
- **`GameRuntimeLibrary.kt`** — `zap item[, item]...` CLI (`runZapCli` loops inventory qty)
- **DI** — `SharedModule` registers `ZapRequest`; `GameRuntimeLibrary` injects `zapRequest`
- **`GameRuntimeLibrary.AshP224Batch.kt`** — batch marker
- **Tests** — `ZapRequestTest`, `GameRuntimeLibraryAshP224Test`, `AshCompatibilityCorpusTest.corpus_zap_live`

### Batch markers + revision

- **`REVISION`** — `phase214`

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```

Result: **3,565 tests**, all green; Android debug build successful.

## Deferred (Phase 215+)

- `cleanup junk` CLI + `UntinkerRequest` + junk list
- Tender-hammer auto-retrieve before pulverize/smash
- Desktop `ZapRequest.decorate` / `relayTrimsZapList` (relay-only; mobile non-goal)
- `bastille.txt` manager (Tier 3 #7)
