# Phase 60: PirateRealm Sync, AshP24 Class Modifiers, tower_door ASH

**Goal:** Close remaining quest-log gaps (PirateRealm sail progress, TOPPING peak fix, PIRATE finish, absence clearing), live CLASS entity modifiers (AshP24), and `tower_door()` ASH.

**Status:** Complete (`GameRuntimeLibrary.REVISION = "phase60"`)

---

## Track A — Quest depth

| File | Change |
| ---- | ------ |
| `PirateRealmSync.kt` | sail1/2/3 → step1/6/11, finish + unlock prefs |
| `GameRuntimeLibrary.kt` | `realm_pirate` visit hook |
| `QuestSpecialSync.kt` | Peak status → `Quest.TOPPING` (was MACGUFFIN) |
| `QuestAdvanceRules.kt` | PIRATE belowdecks finish rule |
| `QuestLogRequest.kt` | Page-1 absence clearing (ghost, new-you, doctor bag, Toot) |

---

## Track B — AshP24 CLASS modifiers

| File | Change |
| ---- | ------ |
| `ClassModifiers.kt` | Stat Tuning per standard ascension class |
| `GameRuntimeLibrary.AshP24Batch.kt` | Live CLASS `numeric/boolean/string_modifier` |
| `GameRuntimeLibrary.AshP11Batch.kt` | Remove CLASS from stub list |
| `CurrentModifiers.kt` | Include class modifiers in accumulation |

---

## Track C — tower_door ASH

| File | Change |
| ---- | ------ |
| `GameRuntimeLibrary.kt` | `tower_door()` ASH → `runTowerDoor` |
| `TowerSync.kt` | Universal key unlock parsing |

**Deferred:** Low-Key auto-adventure, SERVANT/VYKEA, `tower`/`lowkey` status CLI.

---

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```
