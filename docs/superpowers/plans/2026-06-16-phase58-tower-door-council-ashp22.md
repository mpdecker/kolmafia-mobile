# Phase 58: Tower Door CLI, questscouncil Loader, AshP22 Current Modifiers

**Goal:** Standard tower door automation, council quest text sync, and live current-character ASH modifiers.

**Status:** Complete (`GameRuntimeLibrary.REVISION = "phase58"`)

---

## Track A — Standard Tower Door runner + CLI `door`

| File | Change |
| ---- | ------ |
| `TowerDoorConfig.kt` | Lock table, `parseTowerDoor`, key pref helpers |
| `TowerDoorRunner.kt` | step5 guard, retrieve/unlock loop, doorknob |
| `TowerSync.kt` | `parseTowerDoorResponse` hooks |
| `GameRuntimeLibrary.kt` | `cliDoor`, `runTowerDoor`, visit hooks |
| `ItemPool.kt` | Legend key item IDs |

**Deferred:** Low-key door, lock picking, KOE cosmic bazaar keys.

---

## Track B — `questscouncil.txt` loader

| File | Change |
| ---- | ------ |
| `QuestCouncilDatabase.kt` | Parse + `handleCouncilText` |
| `GameDatabase.kt` | Load on init |
| `QuestLogSync.kt` | Council.php URL hook |

---

## Track C — AshP22 current modifiers

| File | Change |
| ---- | ------ |
| `GameRuntimeLibrary.AshP22Batch.kt` | No-arg `numeric/boolean/string_modifier` via `CurrentModifiers` |
| `GameRuntimeLibrary.kt` | `buildCurrentModifiers()` helper |

---

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```
