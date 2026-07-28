# Phase 180: AshP155 Fun-a-Log Shop Sync + AshP156 Validate v15

**Revision:** `phase180`  
**Follows:** Phase 179 (`REVISION = "phase179"`, AshP153/AshP154 mrreplica + blackmarket)

## Goal

Wire PirateRealm Fun-a-Log (`whichshop=piraterealm`) visit sync and validate gates so `is_coinmaster_item(id, validate=true)` matches desktop `FunALogRequest`.

## AshP155 — Shop visit sync v5

- [`FunALogUnlockPrefs.kt`](../../shared/src/commonMain/kotlin/net/sourceforge/kolmafia/shop/FunALogUnlockPrefs.kt) — shared item→pref map from desktop `FunALogRequest.ITEM_TO_UNLOCK_PREF`
- [`CoinmasterShopSync.kt`](../../shared/src/commonMain/kotlin/net/sourceforge/kolmafia/shop/CoinmasterShopSync.kt) — `piraterealm` branch: unlock prefs from `<tr rel="id">`, `availableFunPoints` parse, skip buy URLs
- [`CoinmasterAccessibility.kt`](../../shared/src/commonMain/kotlin/net/sourceforge/kolmafia/shop/CoinmasterAccessibility.kt) — requires Fun-a-Log item (10225) in accessible inventory

## AshP156 — Validate v15

- [`CoinmasterPurchaseAccessibility.kt`](../../shared/src/commonMain/kotlin/net/sourceforge/kolmafia/shop/CoinmasterPurchaseAccessibility.kt) — `piraterealm` branch delegates to `FunALogUnlockPrefs.isItemAvailable`
- Tests: `CoinmasterShopSyncTest`, `GameRuntimeLibraryAshP155Test`, `GameRuntimeLibraryAshP156Test`, `AshCompatibilityCorpusTest.corpus_pirateRealmFunALogValidate_live`

## Deferred

- CRIMBO05–12 legacy craft methods
- STAR/SUGAR/PIXEL standalone method gates
- Full swagger dynamic inventory rebuild
- War dimemaster/quartersmaster visit sync (desktop has none)
