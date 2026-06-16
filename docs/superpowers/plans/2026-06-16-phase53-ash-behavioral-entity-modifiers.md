# Phase 53: ASH Entity Modifiers, Behavioral Corpus, CLI Speculate

**Goal:** Close Tier 1 parity gaps for live entity modifier lookups, behavioral corpus regression, and `speculate` CLI wiring.

**Status:** Complete (`GameRuntimeLibrary.REVISION = "phase53"`)

---

## Track A — ASH-P19: Live entity modifiers

### Changes

| File | Change |
| ---- | ------ |
| `GameDatabase.kt` | `locationModifier`, `zoneModifier`, `pathModifier`, `thrallModifier`, `outfitModifier` |
| `ModifierDatabase.kt` | `getPath()` (deduped duplicate) |
| `GameRuntimeLibrary.Modifiers.kt` | `numericFromEntry` / `booleanFromEntry` / `stringFromEntry` → `internal` |
| `GameRuntimeLibrary.AshP19Batch.kt` | **New** — live `numeric/boolean/string_modifier` for `LOCATION`, `PATH`, `THRALL` |
| `GameRuntimeLibrary.AshP14Batch.kt` | `resolveModifierEntryP14` delegates `LOCATION` → `locationModifier` (first-match wins over AshP19) |
| `GameRuntimeLibrary.AshP11Batch.kt` | Trimmed modifier stubs for covered types; live `is_valid` for LOCATION/MONSTER/PATH/THRALL |

### Registration-order note

`AshScope.resolveFunction` returns the **first** exact-type overload. AshP14 already registered `(LOCATION, STRING)` modifiers returning 0; AshP19 alone could not override them. Fix: wire live lookup in AshP14's resolver. AshP19 covers PATH/THRALL where no earlier `(type, STRING)` modifier overload existed.

### Out of scope this phase

- No `AshType.OUTFIT` — outfit modifier helpers exist on `GameDatabase` for future use
- No `Monster` rows in bundled `modifiers.txt` — monster modifiers deferred

---

## Track B — Behavioral corpus

`AshCompatibilityCorpusTest` additions:

- `corpus_locationModifier_live` — Briny Deeps Item Drop Penalty = -25
- `corpus_pathModifier_live` — You, Robot Energy = 1.0
- `corpus_maximizerModifierSnippet` — modifier_name + numeric_modifier location combo

`GameRuntimeLibraryAshP19Test.kt` — dedicated AshP19 regression suite.

---

## Track C — CLI `speculate`

- `cliDispatch` regex: `speculate <goal>` → `MaximizerManager.speculate(goal)` (no equip side effects)
- `MaximizerManager.speculate()` extracted from maximize plan builder
- `GameRuntimeLibraryCliTest.cliExecute_speculate_printsCandidates` with stub maximizer

---

## Verification

```powershell
.\gradlew.bat :shared:jvmTest      # 1,769 tests
.\gradlew.bat :androidApp:assembleDebug
```
