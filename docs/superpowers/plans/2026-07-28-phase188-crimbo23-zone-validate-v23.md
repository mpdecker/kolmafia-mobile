# Phase 188: AshP171 Crimbo23 Zone Sync + AshP172 Validate v23

**Revision:** `phase188`  
**Follows:** Phase 187 (`REVISION = "phase187"`, AshP169/AshP170)

## AshP171 — Shop visit sync v12

| Component | Behavior |
|-----------|----------|
| `Crimbo23ZoneSync` | Parse `place.php?whichplace=crimbo23` HTML; set armory/bar/cafe/cottage/foundry control + at-war prefs |
| `GameRuntimeLibrary` | Wire crimbo23 place visit hook |

## AshP172 — Coinmaster validate v23

| Component | Behavior |
|-----------|----------|
| `Crimbo23ShopAccessibility` | Desktop `accessible()` gates for all 8 crimbo23 elf/pirate coinmasters |
| `CoinmasterAccessibility` | Delegate crimbo23 shop nicknames to accessibility helper |
| Validate probes | Kelflar vest **11440** (elf armory) + Crimbuccaneer shirt **11407** (pirate armory) |

**Batch files:** `GameRuntimeLibrary.AshP171Batch.kt`, `GameRuntimeLibrary.AshP172Batch.kt`

## Key probe IDs

| Item | ID |
|------|-----|
| Kelflar vest | 11440 |
| Crimbuccaneer shirt | 11407 |
| Elf Army machine parts | 11402 |
| Crimbuccaneer flotsam | 11405 |

## Deferred (non-goals)

- Crimbo23 shop token balance parse on shop visit
- Dynamic row overlay for Crimbo23 shops
- Armory & Leggery / MerchTable token-pref validate / tower freepull bucket migration
