# Phase 336 — Adventurer of Leisure uneffect maps (TCRS applyModifiers v77)

**Track:** TCRS `applyModifiers` v77  
**Revision:** `phase336`

## Goal

Close the long-deferred Adventurer of Leisure (5011) guild-skill tail from Phase 330: port desktop `UneffectRequest.reset()` removable-effect maps and wire refresh on guild buyskill and skill sync.

## Delivered

- `UneffectRemovableMaps` — desktop `UneffectRequest.reset()` skill/item→effectId maps; AoL-gated Disco Nap extras (15 additional effects); `getUneffectSkill()` query API
- `BattleLearnSkillIds.ADVENTURER_OF_LEISURE = 5011`
- `GuildSkillSync` — calls `UneffectRemovableMaps.resetFromSession` after learning skill 5011
- `SkillManager.fetchSkills` / `learnLocalSkill` — CharSheet-parity map refresh after skill sync
- `GameRuntimeLibrary.uneffectByName` — routes removable effects through `SkillManager.cast` when a mapped skill is owned
- `UneffectRemovableMapsTest` (5 tests) + `GuildSkillSyncTest` AoL extension (1 test)

## Deferred (Phase 337+)

- `removeEffectsWithSkill` / active-effect local removal (no `UseSkillRequest` on mobile)
- Hot Tub / item-remedy uneffect branches
- `DreadScrollManager.decorate()` choice-UI highlighting
- EGO key / guild challenge item consumption on guild visits
