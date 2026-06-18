# Phase 59: Low-Key Tower Door, Rufus Shadow NC, AshP23 Element Modifiers

**Goal:** Low-Key tower door automation, Rufus Shadow Rift NC parity, and live ELEMENT + numerics_modifier ASH.

**Status:** Complete (`GameRuntimeLibrary.REVISION = "phase59"`)

---

## Track A — Low-Key Tower Door v2

| File | Change |
| ---- | ------ |
| `AscensionPath.kt` | `LOW_KEY`, `KINGDOM_OF_EXPLOATHING` |
| `CharacterState.kt` | `isLowkey`, `isKingdomOfExploathing` |
| `TowerDoorConfig.kt` | 30-lock Low-Key table, path-aware helpers |
| `TowerDoorRunner.kt` | Adventure-key abort, lock pick, KOE coinmaster |
| `ItemPool.kt` | 23 low-key key IDs |
| `TowerSync.kt` | `_lk` / low-key action hooks |

**Deferred:** Auto-adventure for low-key keys.

---

## Track B — Rufus Shadow Rift NC

| File | Change |
| ---- | ------ |
| `RufusManager.kt` | `handleShadowRiftNC` (1499/1500) |
| `QuestChoiceRules.kt` | Wire choices 1499/1500 |
| `InventoryManager.kt` | `consumeItemLocally` for lodestone |

---

## Track C — AshP23

| File | Change |
| ---- | ------ |
| `GameRuntimeLibrary.AshP23Batch.kt` | Live ELEMENT modifiers + `numerics_modifier` |
| `GameRuntimeLibrary.AshP11Batch.kt` | Remove ELEMENT from stub list |

---

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```
