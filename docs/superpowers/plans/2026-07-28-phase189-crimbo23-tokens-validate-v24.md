# Phase 189: AshP173 Crimbo23 Token Sync + AshP174 Validate v24

**Revision:** `phase189`  
**Follows:** Phase 188 (`REVISION = "phase188"`, AshP171/AshP172)

## AshP173 — Shop visit sync v13

| Component | Behavior |
|-----------|----------|
| `Crimbo23ShopSync` | Parse crimbo23_* shop visit HTML; set Elf MPC / piece-of-12 / machine-parts / flotsam token prefs |
| `CoinmasterShopSync` | Wire `crimbo23_*` visit branch |

## AshP174 — Coinmaster validate v24

| Component | Behavior |
|-----------|----------|
| `CoinmasterSyncedTokenCount` | `max(physical, syncedPref)` for MerchTable + Crimbo23 token IDs |
| `craftAccessibleCount` | Delegates through synced token helper |
| Validate probes | conmerch tattoo pref-only + mulled wine / sugarplum ration bar/cafe gates |

**Batch files:** `GameRuntimeLibrary.AshP173Batch.kt`, `GameRuntimeLibrary.AshP174Batch.kt`

## Key probe IDs

| Item | ID |
|------|-----|
| Twitching Television Tattoo | 9148 |
| Chroner | 7567 |
| mulled wine | 11465 |
| sugarplum ration | 11459 |
| Elf Guard MPC | 11408 |

## Deferred (non-goals)

- Armory & Leggery / tower freepull bucket migration
- Crimbo23 factory validate probes (tokens still sync)
