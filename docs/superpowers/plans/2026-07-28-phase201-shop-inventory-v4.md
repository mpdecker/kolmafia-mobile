# Phase 201: AshP197 Shop Inventory v4 + AshP198 Validate v32

## Summary

Continued the shop inventory visit-sync + validate rhythm after Phase 200: skill-row HTML parsing for coinmaster shops (`whichskill=` / TD2A), visit-learn routing, and internal validate probes for visit-overlay skill availability.

## Delivered

### AshP197 — shop inventory v4

- **`ItemStack.isSkill`** — when true, `itemId` holds skill id (desktop `SkillResult` convention)
- **`ShopRow.isSkillPurchase`** — skill buy-row helper
- **`ShopRowParser.parseMultiCostRow`** — TD2A `whichskill=(\d+)` branch + TD3A skill name parse after item `descitem` miss
- **`ShopRowFormatting.formatStack`** — skill names via `SkillDefinitionDatabase`
- **`ShopRowDatabase.parseItemOrMeatOrSkill`** — skill name lookup before item token parse
- **`ShopInventorySync`** — explicit `isSkillPurchase → newCoinRows` routing before meat/conc split
- **Tests** — `ShopRowParserTest.parseShop_multiCostSkillRow`, `ShopInventorySyncTest.parseAndLearn_logsSkillRowAndRegistersOverlay`
- **`GameRuntimeLibrary.AshP197Batch.kt`** — batch marker registered

### AshP198 — validate v32

- **`CoinmasterVisitInventory`** — `findBuyRowBySkill`, `containsSkill`; `findBuyRow` skips skill rows; `hasVisitOverlay` uses `containsKey` so empty post-visit overlay denies bundled rows
- **`CoinmasterDatabase`** — `findBuyRowForSkill`, `containsBuySkill(..., validate=true)` with accessibility + `hasSkill` + affordable token costs
- **`CoinmasterPurchaseAccessibility`** — `visitInventorySkillAvailable` helper
- **Tests** — `GameRuntimeLibraryAshP198Test` (revision, overlay authority, visit hook validate, hasSkill gate)
- **`AshCompatibilityCorpusTest`** — `corpus_skillShopVisitOverlay_live`
- **`GameRuntimeLibrary.AshP198Batch.kt`** — batch marker registered

### Docs / revision

- **`REVISION`** — `phase201` (3,465 tests)
- **`docs/parity-audit.md`** — Tier 1 AshP197/AshP198 struck; Phase 201 history entry

## Explicit non-goals (defer Phase 202+)

- Desktop buy/sell row split with currency sets
- Unknown-shop `ShopDatabase.registerShop` session logging
- HTTP `desc_skill.php` prefetch for skill registration
- Disk write-back of learned rows to `shoprows.txt`
- Public `is_coinmaster_skill` ASH (desktop has none)
