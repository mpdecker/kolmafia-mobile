# Phase 156: AshP114 Description Cache for desc(entity)

**Date:** 2026-07-27  
**Revision:** `phase156`  
**Ash batch:** AshP114

## Summary

Phase 156 closes the long-deferred AshP12 `desc(entity)` stub by caching parsed description text from desc_item/effect/skill visit HTML and wiring `entityDesc()` lookups for item/effect/skill entities.

## Delivered

### DescriptionCache

- **`DescriptionCache`** — in-memory session cache keyed by entity id (item/effect/skill int)
- Desktop regex patterns from `DebugDatabase.java`:
  - Item: `<div id="description"[^>]*>(.*?)<script` (DOTALL)
  - Effect/Skill: `<div id="description"[^>]*>(.*?)</div>` (DOTALL)
  - Item ID fallback: `<!-- itemid: N -->`

### Visit hooks

- `processVisitResponseHooks` caches on existing `desc_item.php`, `desc_effect.php`, and `desc_skill.php?self=true` branches
- Item id resolved via `ItemDatabase.getByDescId(descId)?.id ?: DescriptionCache.parseItemIdFromHtml(html)`
- Effect id via `EffectDatabase.getByDescId(descId)?.id`
- Skill id via `extractDescSkillId(url)`

### AshP12 wiring

- **`entityDesc()`** added to `EntityIntrospection.kt`
- AshP12 `desc(entity)` overloads call `entityDesc(captured, args[0].toString())`
- Returns cached text for ITEM/EFFECT/SKILL; `""` for unsupported types (unchanged)

### AshP112/AshP113 repair (prerequisite)

Restored missing Phase 154–155 sync files that hooks already referenced:
- `ClosetMeatSync.kt`, `StorageMeatSync.kt`, `SessionMeatSync.kt`
- Tests: `ClosetMeatSyncTest`, `StorageMeatSyncTest`, `SessionMeatSyncTest`, `GameRuntimeLibraryAshP112Test`, `GameRuntimeLibraryAshP113Test`

### Tests

- `DescriptionCacheTest` — parser + cache + clear
- `GameRuntimeLibraryAshP114Test` — seeded cache, visit hook, uncached empty
- Corpus: `corpus_descItem_fromCache`

## Deferred (unchanged)

- On-demand HTTP prefetch for entities never visited (cache-only v1)
- `desc()` for FAMILIAR/MONSTER/LOCATION/etc.
- `has_queued_commands` (non-goal)
- Garden crop counts / campground dwelling-workshed parse

## Verification

- `.\gradlew.bat :shared:jvmTest` — 2,854 tests pass
- `.\gradlew.bat :androidApp:assembleDebug` — OK
