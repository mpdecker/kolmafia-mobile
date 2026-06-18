# Phase 61: PirateRealm Islands, AshP25 STAT, tower/lowkey CLI

**Goal:** Complete PirateRealm per-island quest steps (Phase 60 follow-on), add AshP25 live STAT entity validation and substat queries (Tier 1), and fix tower/lowkey CLI to match desktop door-status semantics (Tier 1 CLI long-tail).

**Status:** Complete (`GameRuntimeLibrary.REVISION = "phase61"`)

---

## Track A — PirateRealm island sub-progress

| File | Change |
| ---- | ------ |
| `PirateRealmSync.kt` | `getPirateRealmIslandNumber`, `setPirateRealmIslandQuestProgress`, `applyChoice`, island combat + windicle |
| `QuestChoiceRules.kt` | Wire choices 1347–1385 via `PirateRealmSync.applyChoice`; pass `optionLabel` |
| `QuestFightRules.kt` | Adventure 531 combat win → island monster threshold |
| `AdventureManager.kt` | Pass `location.id` + choice option label |
| `ItemPool.kt` | `WINDICLE = 10209` |
| `PirateRealmSyncTest.kt` | Formula, choice, combat, windicle tests |

---

## Track B — AshP25 STAT modifiers

| File | Change |
| ---- | ------ |
| `StatNames.kt` | Six canonical stats + aliases |
| `GameRuntimeLibrary.AshP25Batch.kt` | Live `is_valid`, `type_of`, STAT modifier no-ops |
| `GameRuntimeLibrary.AshP11Batch.kt` / `AshP13Batch.kt` | Remove STAT from stubs |
| `AshP10Batch.kt` / `Character.kt` | Substat `my_basestat` support |
| `GameRuntimeLibraryAshP25Test.kt` + corpus | STAT entity live tests |

**Deferred:** SERVANT/VYKEA → Phase 62

---

## Track C — tower / lowkey status CLI

| File | Change |
| ---- | ------ |
| `TowerDoorStatus.kt` | Lock status HTML table |
| `GameRuntimeLibrary.kt` | `tower` / `lowkey` → status table; `fern` unchanged for `tower.php` |
| `GameRuntimeLibraryCliTest.kt` | Standard + low-key + needed filter tests |

**Deferred:** Low-Key auto-adventure loop

---

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```
