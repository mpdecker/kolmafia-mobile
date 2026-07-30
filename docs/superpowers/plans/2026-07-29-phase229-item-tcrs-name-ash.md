# Phase 229: AshP247 `$item[tcrs_name]`

## Summary

Completed the final desktop `ItemProxy` bracket field: `$item[tcrs_name]` via a new `TCRSDatabase` with desktop-aligned name fallback and preferences-backed per-class/sign map loading on TCRS login.

## Delivered

- **`TCRSDatabase.kt`** — `getTCRSName`, tab-separated parse, `validate`/`filename`/`prefKey`, pref load/save, test hooks
- **`ItemEntityFields`** — `tcrs_name` bracket case
- **`CharacterState.inTwoCrazyRandomSummer`** — path flag for login gating
- **`SessionManager`** — load TCRS map from prefs on TCRS login; reset otherwise
- **`GameRuntimeLibrary.AshP247Batch.kt`** — marker batch registered after AshP246
- **Tests** — `GameRuntimeLibraryAshP247Test`, `TCRSDatabaseTest`, `corpus_itemBracketTcrsName_live`
- **`REVISION`** — `phase229`

## Deferred (Phase 230+)

- Bundling desktop `data/TCRS/*.txt` files (Tier 3 data strategy)
- `TCRSDatabase.deriveItem` / desc_item sync / `applyModifiers`
- `tcrs` CLI command
- ItemMaximumUses v2 (fight/choice guards)
- AshP8–P18 interactive/PvP stubs
