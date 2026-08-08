# Phase 297 — TCRS applyModifiers v38 (craft queue craft-only drain v1)

## Context

Phase 296 wired inventory eat/drink for `itemId > 0`. Desktop `consumeItem` next uses `CreateItemRequest` when `item.getItemId() <= 0` (~1025–1028).

## Scope

- `ConcoctionCreateRequest` — station (COMBINE/COOK/MIX/SMITH) + SUSE craft by concoction name via `RetrieveItemService` + `CraftRequest`/`UseItemRequest`
- `ConcoctionQueueRunner` — route `itemId <= 0` auto-craftable concoctions through create preflight + HTTP
- Optional post-craft eat/drink when output item id resolves after craft

## Deferred (Phase 298+)

- Cafe menus (HellKitchen/MicroBrewery/etc.)
- SPLEEN/POTION/ghost/hobo drain
- Full `CreateItemRequest` (coinmaster/still/rolling pin/subclass)
- `ClanManager`+`clanId`, Floundry buy, failure re-queue
