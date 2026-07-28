# Phase 184: AshP163 Shop Sync v9 + AshP164 Validate v19

**Revision:** `phase184`  
**Follows:** Phase 183 (`REVISION = "phase183"`, AshP161/AshP162)

## AshP163 — Shop visit sync v9

| Component | Behavior | Key prefs / IDs |
|-----------|----------|-----------------|
| `SleazeAirportSync` | Adventure 402/403/404 + `airport_sleaze` place visits set sleaze airport open | `_sleazeAirportToday` |
| Spring Beach ticket use | Item **7467** sets pref when use succeeds | `_sleazeAirportToday` |
| `MerchTableSync` | Parse `conmerch` dynamic rows (Mr. A vs Chroner costs) | item **9148**, shop `conmerch` |
| `CoinmasterVisitInventory` | Runtime buy-row overlay for `conmerch` + `swagger` | visit-refreshed rows |

**New files:** `SleazeAirportSync.kt`, `MerchTableSync.kt`, `CoinmasterVisitInventory.kt`  
**Batch:** `GameRuntimeLibrary.AshP163Batch.kt`

## AshP164 — Coinmaster validate v19

| Component | Validate behavior |
|-----------|-------------------|
| `SeptEmberSync.checkBalance()` | Login/refresh auto-visit `shop.php?whichshop=september` when censer owned |
| Swagger non-season items | Require presence in swagger visit inventory after shop visit |
| Conmerch | After visit, only parsed rows count; gated by `timeTowerAvailable` |
| `TimeTowerSync` toggle | Clears `_cachedStorage`; `get_storage()` caches non-freepull storage only |

**Batch:** `GameRuntimeLibrary.AshP164Batch.kt`

## Key probe IDs

| Item | ID |
|------|-----|
| broberry brogurt (corpus) | 7455 |
| Twitching Television Tattoo | 9148 |
| Sept-Ember Censer | 11642 |
| time-twitching toolbelt | 7566 |
| Spring Beach ticket | 7467 |

## Deferred (non-goals)

- Tinkering Bench `purchasedItem` outfit checkpoint side effects
- Full desktop-parity `get_freepulls()` ASH collection API
- MerchTable token balance parsing
