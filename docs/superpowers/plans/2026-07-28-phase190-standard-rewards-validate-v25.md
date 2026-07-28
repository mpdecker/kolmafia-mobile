# Phase 190: AshP175 Standard Reward Sync + AshP176 Validate v25

**Revision:** `phase190`  
**Follows:** Phase 189 (`REVISION = "phase189"`, AshP173/AshP174)

## AshP175 — Shop visit sync v14

| Component | Behavior |
|-----------|----------|
| `StandardRewardDatabase` | Load `standard-rewards.txt` + `standard-pulverized.txt`; findPulverization + register on visit |
| `ArmoryAndLeggeryShopRows` | Build armory buy rows from database; register `CoinmasterData` shopId=armory |
| `ArmoryAndLeggerySync` | Parse armory visit HTML; learn UNKNOWN rows + new pulverized currencies |
| `CoinmasterShopSync` | Wire `armory` visit branch |

## AshP176 — Coinmaster validate v25

| Component | Behavior |
|-----------|----------|
| `CoinmasterPurchaseAccessibility.standardRewardItemAvailable` | Block UNKNOWN rows + unreleased current-year rewards |
| Validate probes | moss mace (11504) / adobe arsecover (11512) / phrygian cap (11520) token gates |

**Batch files:** `GameRuntimeLibrary.AshP175Batch.kt`, `GameRuntimeLibrary.AshP176Batch.kt`

## Key probe IDs

| Item | ID | Currency | ID |
|------|-----|----------|-----|
| moss mace | 11504 | crepe paper pared cuttings | 11526 |
| adobe arsecover | 11512 | petrified wood waste parts | 11534 |
| crepe paper phrygian cap | 11520 | angelbone fragments | 12074 |

## Deferred (non-goals)

- Tower freepull bucket migration
- Crimbo23 factory validate probes
- EquipmentDatabase.derivePulverization / ResultProcessor pulverized hooks
- Desktop session-log toData line output on visit learn
