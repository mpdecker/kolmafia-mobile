# Phase 187: AshP169 Crimbo25 Sammy Sync + AshP170 Validate v22

**Revision:** `phase187`  
**Follows:** Phase 186 (`REVISION = "phase186"`, AshP167/AshP168)

## AshP169 — Shop visit sync v11

| Component | Behavior |
|-----------|----------|
| `Crimbo25SammySync` | Parse crimbo25_sammy visit HTML; populate `CoinmasterVisitInventory` with dynamic wad/bone trade rows |
| `CoinmasterVisitInventory` | Add `CRIMBO25_SAMMY` to dynamic shops |
| `CoinmasterShopSync` | Wire `"crimbo25_sammy"` visit branch |

## AshP170 — Coinmaster validate v22

| Component | Behavior |
|-----------|----------|
| Conmerch tattoo validate | Tattoo **9148** blocked without tower / before visit; allowed after MerchTable sync with Chroner **1111** cost |
| Crimbo25 Sammy validate | Crymbocurrency **12121** blocked before visit; allowed after sync with affordable cold wad on row **1649** |

**Batch files:** `GameRuntimeLibrary.AshP169Batch.kt`, `GameRuntimeLibrary.AshP170Batch.kt`

## Key probe IDs

| Item | ID |
|------|-----|
| Twitching Television Tattoo | 9148 |
| Chroner | 7567 |
| Crymbocurrency | 12121 |
| cold wad | 1452 |
| twinkly wad | 1450 |

## Deferred (non-goals)

- Armory & Leggery `visitShopRows` + `StandardRewardDatabase`
- MerchTable token prefs driving validate instead of `accessibleCount`
- Full storage/freepull bucket migration on tower toggle
- Crimbo23 armory pref gates
