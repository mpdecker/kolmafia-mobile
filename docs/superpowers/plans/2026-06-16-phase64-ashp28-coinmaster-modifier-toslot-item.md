# Phase 64: AshP28 COINMASTER/MODIFIER + to_slot(item)

**Goal:** Close the final AshP11/AshP13 entity stubs (COINMASTER/MODIFIER) and wire desktop-parity `to_slot(item)` item-to-slot conversion.

**Status:** Complete (`GameRuntimeLibrary.REVISION = "phase64"`)

---

## Track A — AshP28 COINMASTER/MODIFIER

| File | Change |
| ---- | ------ |
| `modifiers/ModifierNames.kt` | `byCaselessName` / `isValid` across Double/Derived/Bitmap/String/Boolean modifier enums |
| `CoinmasterRegistry.kt` / `CoinmasterDatabase.kt` | `resolve` / `isValid` / `findByMasterName`; master name indexed in nickname map |
| `GameRuntimeLibrary.AshP28Batch.kt` | Live `is_valid` / `type_of` + modifier no-ops |
| `GameRuntimeLibrary.kt` | Live `to_coinmaster` / `to_modifier` |
| `AshP11` / `AshP13` | Remove MODIFIER/COINMASTER stubs |
| Tests + corpus | AshP28, corpus_coinmasterModifierEntity_live |

---

## Track B — to_slot(item)

| File | Change |
| ---- | ------ |
| `GameRuntimeLibrary.Equipment.kt` | `to_slot(ITEM)` maps `ItemPrimaryUse` → canonical slot name |
| `GameRuntimeLibraryEquipmentTest.kt` | Hat/weapon/non-equipment round-trip tests |

---

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```

**Result:** 1,892 tests passing.
