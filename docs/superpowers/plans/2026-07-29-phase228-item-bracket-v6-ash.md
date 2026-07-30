# Phase 228: AshP246 Item Bracket Fields v6

## Summary

Wired the six remaining non-TCRS `$item[...]` bracket fields from desktop `ItemProxy`: `smallimage`, `seller`, `buyer`, `skill`, `recipe`, and `noob_skill`.

## Delivered

- **`ItemDatabase.getSmallImage`** — folder thumbnail switch (`folder1.gif` / `folder2.gif`) + default to full image
- **`ItemDatabase.getNoobSkillId`** — noobcore absorb skill index at items.txt load (descId formula + Robortender overrides)
- **`CoinmasterRegistry.findSeller` / `findBuyer`** — delegate to existing buy/sell row lookups
- **`ItemEntityFields`** — six new bracket cases via `ModifierDatabase` and `SkillDefinitionDatabase`
- **`GameRuntimeLibrary.AshP246Batch.kt`** — marker batch registered after AshP245
- **Tests** — `GameRuntimeLibraryAshP246Test`, `corpus_itemBracketFieldsV6_live`, revision pin bulk update
- **`REVISION`** — `phase228`

## Deferred (Phase 229+)

- **`$item[tcrs_name]`** — TCRS data strategy (Tier 3)
- ItemMaximumUses v2 depth
- AshP8–P18 interactive/PvP stubs
