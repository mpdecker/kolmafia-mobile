# Phase 198: AshP191 Generic Shop Inventory Parse + AshP192 Validate v29

## Summary

Closed Phase 197 deferral of full desktop `ShopRequest.parseShopInventory` parity (v1): wired bundled `shoprows.txt`, added generic shop-row HTML parsing, visit-time learn with session-log spading output, and validate v29 for visit-learned coinmaster rows.

## Delivered

- **`ShopRowDatabase`** — loads `shoprows.txt` + runtime visit-learned rows; hooked from `GameDatabase.load()`
- **`ShopRowParser.parseShop`** — shared HTML inventory parser (single- and multi-cost rows)
- **`ShopRowFormatting`** — desktop `toData` / shoprows.txt string helpers
- **`ShopInventorySync.parseAndLearn`** — visit hook on `shop.php` (skip ajax/Uh-Oh); session-log + `NpcStoreVisitOverlay` + `CoinmasterVisitInventory` overlays
- **`ArmoryAndLeggerySync`** — delegates HTML parsing to `ShopRowParser` (standard-reward learn unchanged)
- **`CoinmasterPurchaseAccessibility`** — AshP192 `visitInventoryItemAvailable` + `fdkol` visit overlay validate
- **`GameRuntimeLibrary.AshP191Batch.kt`** / **`AshP192Batch.kt`** — batch markers; `REVISION = phase198`

## Tests

- `ShopRowDatabaseTest`, `ShopRowParserTest`, `ShopInventorySyncTest`
- `GameRuntimeLibraryAshP191Test`, `GameRuntimeLibraryAshP192Test`
- Corpus: `corpus_shopRowDatabase_loadsBundledRow`, `corpus_shopVisitLearn_sessionLog`, `corpus_visitLearnedCoinmasterValidate_live`
- **3,438 tests** (was 3,428 at Phase 197)

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
```
