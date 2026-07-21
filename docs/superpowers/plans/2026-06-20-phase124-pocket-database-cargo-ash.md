# Phase 124: AshP82 PocketDatabase + cargo routing ASH/CLI

**Date:** 2026-06-20  
**Revision:** `phase124`  
**Tests:** 2,473

## Goal

Port full desktop `PocketDatabase` from `cultshorts.txt`, extend `cargo` CLI with pocket/count/list/typed-pick routing, and wire AshP82 ASH (`pocket_monster`, `*_pockets`, `available_pocket`, `pick_pocket`).

## Delivered

### PocketDatabase.kt

- Parses all cultshorts rows (stats/monster/effect/item/scrap/poem/meat/joke/etc.)
- Lookup maps: `effectPockets`, `itemPockets`, `monsterPockets`, `statsPockets`, `all*Pockets` sets
- Ordered lists: `scrapSyllables`, `poemHalfLines`, `meatPockets`
- Query helpers: `pocketByNumber`, `firstUnpickedPocket`, `removePickedPockets`, `sortPockets`/`sortResults`/`sortStats`
- Wired via `GameDatabase.load()`; `CultShortsDatabase` delegates scrap ordering

### CargoCultManager CLI

- `cargo pocket #` — describe pocket contents
- `cargo count|list type|unpicked TYPE` — count/list by pocket type tag
- `cargo count|list monster|effect|item|stat NAME` — filtered count/list
- `cargo monster|effect|item|stat NAME` — pick first unpicked matching pocket

### AshP82 ASH

- `pocket_monster(pocket)`
- `monster_pockets()`, `effect_pockets()`, `item_pockets()`, `stats_pockets()`
- `available_pocket(monster|effect|item|stat)`
- `pick_pocket(int|monster|effect|item|stat)` — delegates to `CargoCultManager` via `runBlocking`

## Deferred (Phase 125+)

- Monster pocket fight registration (`registerPocketFight`)
- Remaining cargo ASH introspection (`potential_pockets`, `pocket_effects`, `meat_pockets`, etc.)
- DESC_ITEM consequence expressions
- Maximizer unified Evaluator

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```
