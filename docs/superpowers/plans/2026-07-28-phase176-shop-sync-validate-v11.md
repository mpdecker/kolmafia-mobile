# Phase 176: AshP147 Shop Visit Sync + AshP148 Validate v11

## Summary

Wired coinmaster and wildfire NPC shop HTML visit sync so validate probes reflect live shop state, and closed NPC/coinmaster validate gaps for bugbear, wildfire, and mystic psychosis pixel items.

## Delivered

### AshP147 — Shop visit sync v1

- **`CoinmasterShopSync.kt`** — 5dprinter/bacon/arcade/kiwi/mystic visitShop pref sync + purchasedItem hooks for bacon/kiwi
- **`NpcShopSync.kt`** — wildfire per-ascension bought pref sync (visit + purchase)
- **`CoinmasterManager.visit`** + **`GameRuntimeLibrary.processVisitResponseHooks`** — invoke sync on shop.php visits
- **Arcade `lockedItem` default** — fixed to `true` (matches bundled defaults.txt)
- **`GameRuntimeLibrary.AshP147Batch.kt`** — batch marker; REVISION `phase176`

### AshP148 — Validate v11

- **`NpcPurchaseAccessibility`** — bugbear Bugbear Costume outfit gate; wildfire BLART/caulk/grease per-ascension prefs
- **`CoinmasterPurchaseAccessibility`** — mystic psychosis pixels 5906/5907/6173 gated on `_mysticPsychosisItemsUnlocked`
- **`GameRuntimeLibrary.AshP148Batch.kt`** — batch marker

## Tests

- `CoinmasterShopSyncTest`, `NpcShopSyncTest`
- Extended `NpcPurchaseAccessibilityTest`, `CoinmasterPurchaseAccessibilityTest`
- `GameRuntimeLibraryAshP147Test`, `GameRuntimeLibraryAshP148Test`
- Corpus: wildfire BLART + mystic pixel pill validate snippets

## Deferred (Phase 177+)

- Full CRIMBO05–12 legacy craft methods
- YouRobot/Robocore visitShop (no desktop coinmaster pattern)
- Remaining coinmaster/NPC long tail beyond psychosis pixels
