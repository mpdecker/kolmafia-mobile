# Phase 142: AshP100 Codpiece Equip HTTP

**Date:** 2026-07-27  
**Revision:** `phase142`  
**Tests:** 2,660

## Goal

Wire desktop-parity Eternity Codpiece gem equip/unequip via `inventory.php?action=docodpiece` + `choice.php?whichchoice=1588`, so CLI `equip codpieceN`, Maximizer, and OutfitCheckpoint restore can change gem slots and sync `eternitycod[]` from api.php.

## Delivered

### EquipmentRequest codpiece flow

- [`EquipmentRequest.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/EquipmentRequest.kt) — `openCodpieceEditor()`, `equipCodpieceGem()`, `unequipCodpieceSlot()`; `equipItem`/`unequipSlot` branch on `CODPIECE1`–`5`; early-out when slot already matches; post-equip `syncCharacterEquipment()`

### CLI equip wiring

- [`GameRuntimeLibrary.cliEquip`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt) — codpiece slots route through `equipmentRequest.equipItem` instead of `inventoryManager.equipItem`

### Tests

- [`EquipmentRequestCodpieceTest.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/EquipmentRequestCodpieceTest.kt) — docodpiece + choice 1588 sequence, unequip option=2, non-gem rejection
- [`GameRuntimeLibraryAshP100Test.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryAshP100Test.kt) — `cli_execute("equip codpiece1 …")` smoke + equipment sync

## Deferred (Phase 143+)

- Rewire all `available_amount` retrieve semantics
- Full async storage/stash fetch in `buildCheckContext()`
- General battle `learnSkillFromResponse`
- Eleven-leaf clover coinmaster special case

## Notes

- Maximizer and OutfitCheckpoint already call `EquipmentRequest.equipItem` — no separate wiring needed
- Inventory tally swap from choice HTML is not parsed; api.php `eternitycod` sync is sufficient for modifiers/`have_skill`
