# Phase 199: AshP193 Shop Inventory v2 + AshP194 Validate v30

## Summary

Continued shop inventory parity after Phase 198: CONC concoction session-log learn, shops.txt type metadata, dynamic coinmaster parser dedupe, and visit-overlay validate authority fix.

## Delivered

- **`ShopType` enum + `ShopRowDatabase.shopType`/`craftingType`** — parse CONC/COIN/NPC/NPCCOIN + crafting type from `shops.txt`
- **`ShopRowFormatting.toConcoctionData`** — desktop concoctions.txt session-log format
- **`ShopInventorySync` CONC path** — routes new rows to concoction log; skips coinmaster overlay for CONC shops
- **`MerchTableSync`/`Crimbo25SammySync`** — delegate HTML parsing to `ShopRowParser` (currency maps retained)
- **`CoinmasterVisitInventory.hasVisitOverlay`** + **`visitInventoryItemAvailable` v30** — visit overlay authoritative over bundled buy rows
- **`GameRuntimeLibrary.AshP193Batch.kt`** / **`AshP194Batch.kt`** — batch markers; `REVISION = phase199`

## Tests

- `ShopRowDatabaseTest` shop type/crafting type
- `ShopInventorySyncTest` CONC concoction log
- `MerchTableSyncTest`, `Crimbo25SammySyncTest`
- `GameRuntimeLibraryAshP193Test`, `GameRuntimeLibraryAshP194Test`
- Corpus: `corpus_concShopVisitLearn_sessionLog`, `corpus_mysticVisitOverlayValidate_live`
- **3,449 tests** (was 3,438 at Phase 198)

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
```
