# Phase 177: AshP149 Shop Visit Sync v2 + AshP150 Validate v12

## Context

- Current revision: `phase177` in `GameRuntimeLibrary.kt`
- Phase 176 delivered coinmaster visit sync (5dprinter/bacon/arcade/kiwi/mystic) + wildfire/bartlebys NPC sync, plus validate v11 (bugbear/wildfire/mystic psychosis pixels)
- Phase 176 deferred: remaining shop HTML sync, purchase-side pref hooks beyond bacon/kiwi, and coinmaster long tail (shore toaster, dv per-character buys)
- Alternation pattern: **sync/infrastructure batch first**, **validate batch second** → AshP149 then AshP150

## AshP149 — Shop visit sync v2

### CoinmasterShopSync

**Shore (`whichshop=shore`)** — mirror `ShoreGiftShopRequest.visitShop`:

- On visit: `itemBoughtPerAscension637 = !html.contains("cheap toaster", ignoreCase=true)`

**Purchase hooks in `applyPurchasedItem`**:

| Shop nickname | Item id | Pref |
|---------------|---------|------|
| `shore` | 637 (cheap toaster) | `itemBoughtPerAscension637=true` |
| `dv` | 6423, 6428, 6429 | `itemBoughtPerCharacter6423/6428/6429=true` |

### NpcShopSync

Add `syncFromStoreHtml` / visit handlers for store keys used on `store.php`:

**Hippy (`storeKey == "hippy"`)** — desktop hippy block:

- Detect side from HTML substrings (peach+pear+plum → hippy; rye sprouts+corn+juniper → fratboy)
- Set `lastFilthClearance`, `currentHippyStore`, `sidequestOrchardCompleted`
- Set `_hippyMeatCollected` when `"Oh, hey, boss!  Welcome back!"` present

**Fireworks (`fwshop`)**:

- When `"<b>Combat Explosives"` present: set `_fireworksShop=true`
- `_fireworksShopHatBought = !html.contains("<b>Dangerous Hats")`
- `_fireworksShopEquipmentBought = !html.contains("<b>Explosive Equipment")`

**Mayo clinic (`mayoclinic`)** — desktop mayoclinic block:

- Parse miracle whip / mayo lance / default rental state → `_mayoDeviceRented`, `itemBoughtPerAscension8266`
- `_mayoTankSoaked = !html.contains("Soak in the Mayo Tank")`

Update `needsSync(storeKey)` to include `hippy`, `fwshop`, `mayoclinic` (keep `wildfire`, `bartlebys`).

Route hippy/fwshop/mayoclinic through `shop.php` visit hook via `NpcShopSync.applyShopVisit`.

### Purchase paths

- `NpcBuyRequest.buy(...)`: optional `prefs`; on success call `applyWildfirePurchase` for wildfire store
- `RetrieveItemService`: pass prefs into NPC buy when wildfire items purchased

### Batch

- `GameRuntimeLibrary.AshP149Batch.kt`; register after AshP148; bump `REVISION` → `phase177`

## AshP150 — NPC + coinmaster validate v12

Validate logic already exists for these shops; AshP150 is primarily **sync-driven accuracy verification** plus any small gate fixes discovered during testing.

### Corpus / unit test targets

| Probe | Fixture idea |
|-------|----------------|
| `is_npc_item(peach_id, true)` on Hippy store | After sync HTML with peach row + `currentHippyStore=hippy` |
| `is_npc_item(fwshop_hat_id, true)` blocked/unblocked | Sync with/without Dangerous Hats section |
| `is_coinmaster_item(637, true)` blocked/unblocked | Shore visit sync + level 6+ char with scrip inventory |
| `is_coinmaster_item(6423, true)` blocked after buy pref | dv purchase hook sets per-character pref |

### Batch

- `GameRuntimeLibrary.AshP150Batch.kt`; register after AshP149

## Tests and docs

- `CoinmasterShopSyncTest.kt` — shore visit + toaster purchasedItem; dv flask purchasedItem
- `NpcShopSyncTest.kt` — hippy side detection, fwshop sections, mayoclinic rental HTML
- `NpcPurchaseAccessibilityTest.kt` — sync-driven hippy/fwshop cases
- `CoinmasterPurchaseAccessibilityTest.kt` — shore toaster gate after sync
- `GameRuntimeLibraryAshP149Test.kt` / `AshP150Test` — revision pin
- `AshCompatibilityCorpusTest.kt` — hippy peach, shore toaster, fwshop hat snippets

Verification: `.\gradlew.bat :shared:jvmTest --rerun-tasks`

## Deferred to Phase 178+

- CRIMBO05–12 legacy craft methods (remain `LEGACY_BLOCKED` in `ConcoctionMethodAliases.kt`)
- STAR/SUGAR/PIXEL concoction method gates
- Hidden tavern unlock sync (`hiddenTavernUnlock` + concoction refresh)
- Remaining coinmaster visitShop long tail (swagger season prefs, jarlsberg cosmic six-pack, etc.)
- Global `ResultProcessor`-style dv buy detection outside coinmaster manager
