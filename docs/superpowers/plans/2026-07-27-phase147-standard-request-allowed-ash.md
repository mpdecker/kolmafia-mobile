# Phase 147: AshP105 StandardRequest.isAllowed Full Parity

## Summary

Completed desktop `StandardRequest.isAllowed` parity deferred from Phase 146: Trendy path gate, Thrifty evergreen item-use check, Quantum familiar exception, and full wiring through `ItemRestriction` for physical accessible counts.

## Delivered

- **`TrendyRequest.kt`** — `typeii.php` parse + `isTrendy()` cache
- **`AscensionPath.TRENDY`** + `CharacterState.isTrendy` / `inQuantum`
- **`ModifierDatabase.getStringModifier()`** — `LAST_AVAILABLE_DATE` lookup for Thrifty evergreen
- **`StandardRequest.isAllowed()`** — full desktop rule order (Trendy → Thrifty → Quantum → HC/ronin standard list)
- **`StandardRequest.isNotRestricted()`** — respects `CharacterState.isRestricted` like desktop
- **`ItemRestriction.isAllowed()`** — delegates to full `StandardRequest.isAllowed`
- **`GameRuntimeLibrary`** — `trendyRequest` DI + `buildCheckContext` refresh when Trendy
- **Tests** — `TrendyRequestTest`, extended `StandardRequestTest`, `AccessibleItemCountTest`, `GameRuntimeLibraryAshP105Test`, corpus Trendy gate snippet
- **`REVISION`** — `phase147` (2,714 tests)

## Deferred (Phase 148+)

- `have_familiar` / `usableFamiliar` path gates
- Retrieve/create early exit when `!ItemDatabase.isAllowed`
- Bulk `AshCompatibilityCorpusTest` expansion
- AshP8–P18 interactive/PvP stubs
