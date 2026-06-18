# Phase 65: AshP29 CLASS/ELEMENT + use_servant CLI

**Goal:** Close the remaining AshP13 blank CLASS/ELEMENT entity stubs via AshP29 with live `to_class`/`to_element`, and wire deferred Ed `servant` CLI + `use_servant` ASH.

**Status:** Complete (`GameRuntimeLibrary.REVISION = "phase65"`)

---

## Track A — AshP29 CLASS/ELEMENT entity validation

| File | Change |
| ---- | ------ |
| `modifiers/ClassNames.kt` | `resolve` / `isValid` via `CharacterClass` display names + numeric id |
| `modifiers/ElementNames.kt` | Canonical cold/hot/sleaze/spooky/stench/slime/supercold |
| `GameRuntimeLibrary.AshP29Batch.kt` | Live `is_valid` / `type_of` + modifier no-ops for CLASS/ELEMENT |
| `GameRuntimeLibrary.AshP8Batch.kt` | Live `to_class` / `to_element` |
| `AshP13` | Remove CLASS/ELEMENT from `stubEntityTypes` |
| Tests + corpus | AshP29, `corpus_classElementEntity_live` |

---

## Track B — servant CLI + use_servant ASH

| File | Change |
| ---- | ------ |
| `servant/EdServantManager.kt` | Prefs `_edServants` / `_edActiveServant`; HTTP `edbase_door` + choice 1053 |
| `GameRuntimeLibrary.kt` | `servant <type>` + `servants` in `cliDispatch`; `edServantManager` DI |
| `GameRuntimeLibrary.AshP26Batch.kt` | `use_servant` ASH; `have_servant` prefers summoned list |
| Tests | CLI servant (Ed/non-Ed); `use_servant` ASH |

**Deferred:** charpane HTML parse, servant XP/level sync, `servants` HTML table output.

---

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```
