# Phase 181: AshP157 Drip Armory Shop Sync + AshP158 Validate v16

**Revision:** `phase181`  
**Follows:** Phase 180 (`REVISION = "phase180"`, AshP155/AshP156 Fun-a-Log)

## Goal

Wire Drip Institute Armory (`whichshop=driparmory`) visit sync and validate gates so `is_coinmaster_item(id, validate=true)` matches desktop `DripArmoryRequest`.

## AshP157 — Shop visit sync v6

- [`DripArmoryPrefs.kt`](../../shared/src/commonMain/kotlin/net/sourceforge/kolmafia/shop/DripArmoryPrefs.kt) — shared shield unlock pref + validate helper
- [`CoinmasterShopSync.kt`](../../shared/src/commonMain/kotlin/net/sourceforge/kolmafia/shop/CoinmasterShopSync.kt) — `driparmory` branch: `drippyShieldUnlocked` when HTML contains `drippy shield`, skip buy URLs

## AshP158 — Validate v16

- [`CoinmasterPurchaseAccessibility.kt`](../../shared/src/commonMain/kotlin/net/sourceforge/kolmafia/shop/CoinmasterPurchaseAccessibility.kt) — `driparmory` branch: shield requires pref + not owned
- Tests: `CoinmasterShopSyncTest`, `GameRuntimeLibraryAshP157Test`, `GameRuntimeLibraryAshP158Test`, `AshCompatibilityCorpusTest.corpus_dripArmoryShieldValidate_live`

## Deferred

- Time Tower cluster (`timeTowerAvailable` + 8 Chroner shops)
- Swagger `CoinmasterManager.visit()` peevpee routing
- Tinkering Bench / SBB taco-brogurt / vending machine validate-only gates
