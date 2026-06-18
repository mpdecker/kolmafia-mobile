# Phase 62: AshP26 SERVANT/VYKEA, windicle hook

**Goal:** Close AshP25 deferral with live SERVANT/VYKEA entity validation and modifiers, and wire PirateRealm windicle combat hook.

**Status:** Complete (`GameRuntimeLibrary.REVISION = "phase62"`)

---

## Track A — AshP26 SERVANT/VYKEA

| File | Change |
| ---- | ------ |
| `modifiers/ServantData.kt` | 7 servants + fuzzy `resolve()` |
| `modifiers/VykeaCompanionData.kt` | VYKEA catalog, `fromString` regex, couch/lamp modifiers |
| `GameRuntimeLibrary.AshP26Batch.kt` | Live `is_valid`/`type_of`/`have_servant`; VYKEA couch/lamp `numeric_modifier` |
| `GameRuntimeLibrary.kt` | Register AshP26; live `to_servant`/`to_vykea` |
| `GameRuntimeLibrary.AshP11Batch.kt` / `AshP13Batch.kt` | Remove SERVANT/VYKEA from stub lists |
| `GameRuntimeLibraryAshP26Test.kt` + corpus | SERVANT/VYKEA live tests |

**Deferred:** `use_servant` CLI, charpane VYKEA sync, full EdServant runtime, BOUNTY/SLOT live `is_valid`

---

## Track B — Windicle combat hook

| File | Change |
| ---- | ------ |
| `PirateRealmSync.kt` | `applyWindicleFromFightHtml` |
| `AdventureManager.kt` | Call after fight on adventure 531 |
| `PirateRealmSyncTest.kt` | Island + skip-other-adventure tests |

---

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```
