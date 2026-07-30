# Phase 224: AshP242 Item Restore Bracket Fields

**Delivered:** 2026-07-29  
**Revision:** `phase224`  
**Tests:** 3,680 (+14 from Phase 223)

## Context

Phase 223 delivered consumable v1 `$item[field]` reads via `ItemEntityFields`. Phase 224 adds restore HP/MP bracket fields using `RestoreDatabase` expression evaluation.

## AshP242 — item restore bracket fields

- Extended `ExpressionContext` with `characterMaxMp`, `characterCurrentHp`, and `MP`/`CURHP` tokens
- Added `RestoreDatabase` eval helpers: `getHpMinByName`, `getHpMaxByName`, `getMpMinByName`, `getMpMaxByName`, `evalRestoreValue`, `pathSafeHp`/`pathSafeMp`
- Extended `ItemEntityFields` with `minhp`, `maxhp`, `minmp`, `maxmp` (ExpressionContext-aware)
- Added `buildRestoreExpressionContext()` in `GameRuntimeLibrary` and wired into `resolveEntityIndex`
- Batch marker: `GameRuntimeLibrary.AshP242Batch.kt`

## Tests

- `RestoreDatabaseRestoreLookupTest` — numeric HP/MP, path expressions, `[HP]` full restore, pathSafe gates
- `ItemEntityFieldsTest` — restore field resolver coverage
- `GameRuntimeLibraryAshP242Test` — revision pin + Ash interpreter smoke + You Robot battery path
- `AshCompatibilityCorpusTest.corpus_itemRestoreFields_live`
- Bulk revision pins: `phase223` → `phase224`

## Deferred (Phase 225+)

- Item flag brackets (`tradeable`, `giftable`, equipment fields)
- Item `dailyusesleft`, `notes`, `advcost` brackets
- Buffbot XML fetch + philanthropic `getOffering`
- `faxbots.txt` registry loader
- Bastille Battalion automation / optimal solver CLI
- Remaining unwired data files: `ocean.txt`, `fambattle.txt`, `wereprofessor.txt`
