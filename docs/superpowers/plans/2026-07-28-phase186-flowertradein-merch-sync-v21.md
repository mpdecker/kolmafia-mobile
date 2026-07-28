# Phase 186: AshP167 Flower/Merch Sync + AshP168 Validate v21

**Revision:** `phase186`  
**Follows:** Phase 185 (`REVISION = "phase185"`, AshP165/AshP166)

## AshP167 — Shop visit sync v10

| Component | Behavior |
|-----------|----------|
| `FlowerTradeinSync` | Parse flowertradein visit HTML; populate `CoinmasterVisitInventory` rows 759–762 |
| `MerchTableSync` | Parse Mr. A / Chroner token balance lines into `availableMerchMrA` / `availableMerchChroners` prefs |
| `CoinmasterVisitInventory` | Add `FLOWER_TRADEIN` to dynamic shops alongside `conmerch` / `swagger` |
| `CoinmasterShopSync` | Wire `"flowertradein"` visit branch |

## AshP168 — Coinmaster validate v21

| Component | Behavior |
|-----------|----------|
| Flower tradein validate | `CoinmasterDatabase.findBuyRowForItem` prefers visit overlay; Chroner probeable after sync with rose accessible |
| `get_storage()` | Single `fetchClassifiedContents()` saves both `CACHED_STORAGE` and `CACHED_FREEPULLS` |

**Batch files:** `GameRuntimeLibrary.AshP167Batch.kt`, `GameRuntimeLibrary.AshP168Batch.kt`

## Key probe IDs

| Item | ID |
|------|-----|
| Chroner | 7567 |
| rose | 8668 |
| red tulip | 8670 |
| Mr. Accessory | 194 |
| Twitching Television Tattoo | 9148 |

## Deferred (non-goals)

- Armory & Leggery `visitShopRows` + StandardRewardDatabase
- Crimbo25 Sammy dynamic wad pricing
- Full storage/freepull bucket migration on tower toggle
- MerchTable token prefs driving validate instead of `accessibleCount`
