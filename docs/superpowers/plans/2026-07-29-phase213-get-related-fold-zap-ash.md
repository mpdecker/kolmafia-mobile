# Phase 213: AshP221 get_related fold + AshP222 get_related zap ASH

## Summary

Extended live `get_related(item, type)` ASH with desktop-parity `fold` and `zap` branches, reusing already-loaded `foldgroups.txt` / `zapgroups.txt` data from Phase 212 deferral.

## Delivered

### AshP221 — get_related fold

- **`RelatedAggregate.kt`** — `decodeFoldToAggregate()` maps fold chain items to 1-based positions (reverse iteration like desktop)
- **`GameRuntimeLibrary.Related.kt`** — `"fold"` branch via `FoldGroupDatabase.groupFor(itemName)`
- **`FoldGroupDatabase.kt`** — `registerGroupForTest` / `resetForTest` for unit tests
- **Tests** — `GameRuntimeLibraryAshP221Test`, `AshCompatibilityCorpusTest.corpus_getRelatedFold_live`

### AshP222 — get_related zap

- **`RelatedAggregate.kt`** — `decodeZapToAggregate()` maps other zap-group items to `0`, excluding query item
- **`ZapGroupDatabase.kt`** — `groupForItemId(itemId)` + test helpers
- **`GameRuntimeLibrary.Related.kt`** — `"zap"` branch
- **Tests** — `GameRuntimeLibraryAshP222Test`, `AshCompatibilityCorpusTest.corpus_getRelatedZap_live`

### Batch markers + revision

- **`GameRuntimeLibrary.AshP221Batch.kt`** / **`GameRuntimeLibrary.AshP222Batch.kt`**
- **`REVISION`** — `phase213`

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```

Result: **3,554 tests**, all green; Android debug build successful.

## Deferred (Phase 214+)

- `cleanup junk` CLI + `KoLConstants.junkList` + `UntinkerRequest`
- Tender-hammer auto-retrieve before smash
- `zap` / `get_zap_wand` HTTP CLI (separate from get_related map decode)
- `bastille.txt` manager (Tier 3 #7)
