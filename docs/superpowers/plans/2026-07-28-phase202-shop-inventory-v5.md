# Phase 202: AshP199 Shop Inventory v5 + AshP200 Validate v33

## Summary

Continued the shop visit-sync + validate rhythm: legacy coinmaster buy/sell row classification (desktop `newBuyRows`/`newSellRows`), unknown-shop registration, and sell visit-overlay validate probes.

## Delivered

### AshP199 — shop inventory v5

- **`ShopRowDatabase.registerShop`** — returns true for newly seen shop ids; session log `New shop: (id, "name")`
- **`ShopInventorySync.parseShopNameFromHtml`** — desktop `SHOP_PATTERN` table title parse
- **`CoinmasterData.currencyItemIds`** — token + shop-row buy costs (legacy buy uses token)
- **`ShopInventorySync` row buckets** — `LEGACY_BUY` / `LEGACY_SELL` / `COIN` split mirroring desktop `ShopRequest.parseShopInventory`
- **`ShopRowFormatting.toLegacyBuyData` / `toLegacySellData`** — coinmasters.txt buy/sell session-log lines
- **Tests** — `ShopInventorySyncTest` (new shop registration, legacy buy log, legacy sell log + sell overlay)
- **`GameRuntimeLibrary.AshP199Batch.kt`** — batch marker registered

### AshP200 — validate v33

- **`CoinmasterVisitInventory`** — sell overlay (`registerVisitSellRows`, `findSellRow`, `containsSellItem`, `hasVisitSellOverlay`, `replaceSellRows`); `hasVisited` covers buy or sell maps
- **`CoinmasterDatabase`** — `findSellRowForItem`, `containsSellItem(validate)` with inventory gate; `registerForTest` helper
- **`CoinmasterPurchaseAccessibility.visitInventorySellAvailable`** — sell overlay authority parallel to buy/skill helpers
- **`CoinmasterManager.sellsItem`** — respects visit sell overlay when present
- **Tests** — `GameRuntimeLibraryAshP200Test`; corpus `corpus_legacySellShopVisitOverlay_live`
- **`GameRuntimeLibrary.AshP200Batch.kt`** — batch marker registered

### Docs / revision

- **`REVISION`** — `phase202` (3,472 tests)
- **`docs/parity-audit.md`** — Tier 1 AshP199/AshP200 struck; Phase 202 history entry

## Explicit non-goals (defer Phase 203+)

- HTTP `desc_skill.php` prefetch for skill registration
- Disk write-back of learned rows to `shoprows.txt`
- Desktop `CoinmasterData.isDisabled` / shop-row coinmaster custom hooks
- Public `is_coinmaster_sell` ASH (desktop has none)
