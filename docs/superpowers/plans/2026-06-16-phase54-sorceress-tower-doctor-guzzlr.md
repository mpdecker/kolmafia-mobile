# Phase 54: Sorceress Tower Sync, Doctor/Guzzlr Depth, hedge_maze ASH

**Goal:** Close Tier 2 quest tracking gaps for NS tower HTML, Doctor Bag / Guzzlr delivery lifecycle, and hedge maze ASH automation.

**Status:** Complete (`GameRuntimeLibrary.REVISION = "phase54"`)

---

## Track A — TowerSync

| File | Change |
| ---- | ------ |
| `TowerSync.kt` | **New** — `parseTower` maps tower GIF markers → `Quest.FINAL` steps; clears `nsContestants*` past step1 |
| `QuestLogSync.kt` | Calls `TowerSync.parseTower` on every response |
| `GameRuntimeLibrary.kt` | Tower hook in `processVisitQuestHooks`; `whichplace=nstower` place extraction |

---

## Track B — Doctor Bag / Guzzlr depth

| File | Change |
| ---- | ------ |
| `QuestSpecialSync.kt` | Delivery completion, abandon, tier delivery counters |
| `QuestChoiceRules.kt` | Choices 1340/1341 (doctor bag), 1412 (guzzlr abandon) |

---

## Track C — hedge_maze ASH

| File | Change |
| ---- | ------ |
| `GameRuntimeLibrary.AshP20Batch.kt` | **New** — `hedge_maze("traps")` sets choiceAdventure1005–1013 prefs when FINAL at step4 |

---

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```
