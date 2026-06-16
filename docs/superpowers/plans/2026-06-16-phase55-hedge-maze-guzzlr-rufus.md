# Phase 55: Hedge Maze Modes, Guzzlr CLI, Rufus Quest Sync

**Goal:** Complete NS/Guzzlr automation deferred from Phase 54: remaining hedge_maze ASH modes, Guzzlr CLI, and Rufus quest-log sync.

**Status:** Complete (`GameRuntimeLibrary.REVISION = "phase55"`)

---

## Track A — hedge_maze ASH modes

| File | Change |
| ---- | ------ |
| `GameRuntimeLibrary.AshP20Batch.kt` | `gopher`/`duck`, `chihuahua`/`kiwi`, `nugglets` modes; shared `configureHedgeMaze` helper |

---

## Track B — Guzzlr CLI

| File | Change |
| ---- | ------ |
| `GameRuntimeLibrary.kt` | `guzzlr abandon` / `guzzlr accept bronze\|gold\|platinum` in `cliDispatch`; tier gates mirror desktop |

---

## Track C — Rufus quest-log sync

| File | Change |
| ---- | ------ |
| `RufusManager.kt` | `handleQuestLog` with entity/artifact/items matchers |
| `QuestSpecialSync.kt` | `applyRufus` wired into `apply` |
| `ItemDatabase.kt` | `getByPluralOrName` for items quest target resolution |

---

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```
