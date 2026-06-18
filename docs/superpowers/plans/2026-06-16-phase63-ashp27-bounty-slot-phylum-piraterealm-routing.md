# Phase 63: AshP27 BOUNTY/SLOT/PHYLUM, PirateRealm routing

**Goal:** Continue Tier 1 ASH stub closure with BOUNTY/SLOT/PHYLUM entity validation, and wire PirateRealm zone gating for island adventures.

**Status:** Complete (`GameRuntimeLibrary.REVISION = "phase63"`)

---

## Track A — AshP27 BOUNTY/SLOT/PHYLUM

| File | Change |
| ---- | ------ |
| `BountyDatabase.kt` | Canonical index + `getMatchingNames` / `resolve` / `isValid` |
| `modifiers/SlotNames.kt` | Core desktop slot names + aliases |
| `modifiers/PhylumNames.kt` | Desktop phylum catalog |
| `GameRuntimeLibrary.AshP27Batch.kt` | Live `is_valid` / `type_of` + modifier no-ops |
| `GameRuntimeLibrary.kt` / `Equipment.kt` / `AshP8Batch.kt` | Live `to_bounty` / `to_slot` / `to_phylum` |
| `AshP11` / `AshP13` | Remove SLOT/BOUNTY/PHYLUM stubs |
| Tests + corpus | AshP27, BountyDatabase, corpus_bountySlotPhylumEntity_live |

**Deferred:** COINMASTER/MODIFIER live `is_valid`; full pseudo-slot equip support

---

## Track B — PirateRealm adventure routing

| File | Change |
| ---- | ------ |
| `AdventurePrep.kt` | `canAdventureAtPirateRealm`; integrated into `canAdventureAtZone` |
| `AdventureManager.kt` / `Location.kt` / `Character.kt` | Pass `preferences` into zone checks |
| `AdventurePrepTest.kt` | Daily access + island name gating tests |

---

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```
