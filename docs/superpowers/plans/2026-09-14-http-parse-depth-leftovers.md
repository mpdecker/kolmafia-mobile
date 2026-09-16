# HTTP Parse-Depth Leftovers Mega (7271–7330)

> Tip: `phase7270` → wrap `phase7330`. No mid-track REVISION bump.
> Exclude Relay/JS. Deepen desktop-shaped APIs only.
> Coinmaster `*Request` subclasses stay consolidated (hubs + visit overlay). Do not invent per-shop Request classes.
> Padding register-only NPC hubs with no-op `parseResponse` is low ROI — desktop also relies on `ShopInventorySync`.

**Goal:** Close honest leftover parse bodies: cafe.php visit/consume side effects, mayo-clinic workshed, Skeleton of Crimbo Past daily-special buy overlay.

## Track A (7271–7290) — cafe.php visit + consume helpers

- Wire `cafe.php` in `processVisitResponseHooks` and `ResponseTextParser` `"cafe"` to `CafeRequest.parseResponse` → `CafeDailySpecialSync` (skip `action=CONSUME`) plus Chez/Micro consume parsers.
- `MicroBreweryRequest.parseResponse`: cafeid=2 CONSUME HTML `"You pour your drink into your mime army shotglass"` → `_mimeArmyShotglassUsed`.
- `DrinkBoozeRequest.parseDrinkHelpers`: same shotglass string + `"You pour your drink into your flagellate flagon."` → decrement `flagellateFlagonsActive`; call from `UseItemConsumptionSync.parseDrink`.
- `EatFoodRequest.handleFoodHelper` (desktop `EatItemRequest.handleFoodHelper`): salt **6672** / swamp honey **8226** / dry rub **7553** / Special Seasoning **9924** / whetstone / mini kiwi aioli / infinite jelly / milk of magnesium / Pastamancer carbo + spice ghost / Mayo Minder + Mayonex gurgling; call from `parseEat` (`adjustFullness=false`) and Chez CONSUME (`You gain`, cafeid=1).
- `CafeRequest.consume` / `HellKitchenRequest.purchaseEntry` pass prefs into parse.

## Track B (7291–7310) — Mayo clinic workshed

- `CampgroundItemSync.setCurrentWorkshedItem(prefs, itemId)` — previous-id + `_workshedChanged`; mayo **8260** refreshes concoctions.
- `NpcShopSync.syncMayoclinic`: set workshed **before** ajax return (desktop `NPCPurchaseRequest.parseShopResponse`); then mayoLevel / device / tank.
- `applyShopVisit` must not skip ajax for `mayoclinic`.
- Skip Chateau `parseShopResponse` (desktop only `recalculateAdjustments`).

## Track C (7311–7330) — Skeleton `applySpecial` + wrap

- `SkeletonOfCrimboPastRequest.applySpecial`: inject `_crimboPastDailySpecialItem` / `_crimboPastDailySpecialPrice` into `CoinmasterVisitInventory` overlay (`socp`); strip overlay ids `< SMOKING_POPE` (**12052**); keep catalog static rows.
- `CoinmasterDatabase` Skeleton override `shopId = "socp"`; `CoinmasterVisitInventory.SOCP` dynamic shop.
- Call `applySpecial` from `CrimboPastChoiceSync.applyVisit`.
- Markers Phase7271/7291/7311; tests Phase7290/7310/7330; corpus Tracks A–C.
- Parent wrap: `REVISION` → `phase7330`; parity-audit + AGENTS.
