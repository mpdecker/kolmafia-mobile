# Phase 183: AshP161 Shop Sync v8 + AshP162 Validate v18

**Revision:** `phase183`  
**Follows:** Phase 182 (`REVISION = "phase182"`, AshP159/AshP160 Time Tower)

## AshP161 — Shop visit sync v8

| Shop ID | Sync behavior | Key prefs |
|---------|---------------|-----------|
| `trapper` | If HTML contains `"yeti furs"`: set `lastTr4pz0rQuest` = ascension, `questL08Trapper` = `finished` | `lastTr4pz0rQuest`, `questL08Trapper` |
| `lathe` | Set `_spinmasterLatheVisited = true` on non-buy visit | `_spinmasterLatheVisited` |
| `september` | First visit: parse `<b>You have ([\d,]+) Ember` → `availableSeptEmbers`, set `_septEmberBalanceChecked` | `availableSeptEmbers`, `_septEmberBalanceChecked` |
| `junkmagazine` | If HIPPY not past step1: set `questM19Hippy` → `step2` | `questM19Hippy` |

**New files:** `TrapperSync.kt`, `SpinMasterLatheSync.kt`, `SeptEmberSync.kt`, `JunkMagazineSync.kt`  
**Batch:** `GameRuntimeLibrary.AshP161Batch.kt`

## AshP162 — Coinmaster validate v18

### Accessibility gates

| Nickname / shopId | Gate |
|-------------------|------|
| `trapper` | level ≥ 8; `lastTr4pz0rQuest == ascensionNumber`; `!inZombiecore` |
| `lathe` | `accessibleCount(10582) > 0` |
| `september` | `accessibleCount(11642) > 0`; `!isKingdomOfExploathing` |
| `junkmagazine` | `accessibleCount(6731) > 0` |
| `sbb_brogurt`, `sbb_taco`, `sbb_jimmy` | `_sleazeAirportToday \|\| sleazeAirportAlways` (+ optional `LimitModeGates.limitZone`) |
| `damachine` / `vendingmachine` | `!isKingdomOfExploathing` |
| `wereprofessor_tinker` | active effect **2897** Mild-Mannered Professor |

**New helpers:** `SpringBreakBeachAccessibility.kt`, `TinkeringBenchGates.kt`  
**Override:** `"Vending Machine"` → `nickname = "vendingmachine"`, `shopId = "damachine"`

### Per-item gates

| Cluster | canBuyItem logic |
|---------|------------------|
| SBB brogurt (7455/7456/7457) | `questESlBacteria == finished` |
| SBB taco (7451/7452) | `questESlFish` / `questESlSprinkles == finished` |
| Vending sewing kit 7300 | only when `accessibleCount(7300) == 0` |
| Tinkering bench | one-of-each upgrade tree via `TinkeringBenchGates` |

**Batch:** `GameRuntimeLibrary.AshP162Batch.kt`

## Key probe IDs

| Item | ID |
|------|-----|
| broberry brogurt (corpus) | 7455 |
| Beach Buck | 7429 |
| sewing kit | 7300 |
| biphasic molecular oculus | 11550 |
| yak skin | 394 |

## Deferred (non-goals)

- MerchTable `conmerch` dynamic row refresh
- Toolbelt storage/freepull bucket migration
- Sleaze-airport visit sync (`_sleazeAirportToday` source)
- Tinkering Bench `purchasedItem` outfit checkpoint side effects
- SeptEmber login `checkBalance()` auto-visit
