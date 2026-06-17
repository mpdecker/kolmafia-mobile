# Phase 57: Hedge Maze Runner, Sign Modifiers, Rufus Shadow Rift

**Goal:** Complete deferred hedge maze automation loop, AshP21 Sign modifiers, and Rufus shadow-rift encounter counter.

**Status:** Complete (`GameRuntimeLibrary.REVISION = "phase57"`)

---

## Track A — Hedge maze full runner

| File | Change |
| ---- | ------ |
| `HedgeMazeRunner.kt` | **New** — turn budget, trap survival, heal, place loop until step5 |
| `HedgeMazeConfig.kt` | `estimateMazeTurns`, `estimateTrapHpLost`, `wouldSurviveTraps` |
| `RecoveryManager.kt` | `recoverHpToMax()` |
| `AdventureManager.kt` | `followAdventureResponse()` |
| `GameRuntimeLibrary.kt` | `cliMaze` → `runHedgeMaze()` |

---

## Track B — AshP21 Sign modifiers

| File | Change |
| ---- | ------ |
| `GameRuntimeLibrary.AshP21Batch.kt` | `"sign"` alias in `resolveModifierByTypeName` |

---

## Track C — Rufus shadow rift counter

| File | Change |
| ---- | ------ |
| `RufusManager.kt` | `handleShadowRiftFight()` |
| `AdventureManager.kt` | Hook on Shadow Rift combat finish |

---

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```
