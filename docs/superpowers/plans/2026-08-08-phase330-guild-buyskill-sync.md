# Phase 330 — GuildSkillSync (TCRS applyModifiers v71)

**Track:** TCRS `applyModifiers` v71  
**Revision:** `phase330`

## Goal

Wire guild.php `buyskill` post-response sync deferred from Phases 328–329: meat deduction, skill learn, and concoction refresh on successful guild skill purchases.

## Delivered

- `SkillDefinition.guildLevel` + classskills.txt `Level: N` loader attribute
- `SkillDefinitionProxy.classSkillBase` / `findSkillFromUrl` / `getGuildPurchaseCost`
- `GuildSkillSync.parseBuyskill` — desktop `GuildRequest` buyskill branch parity
- `GuildVisitSync.parseFromVisit` routing for `buyskill` + hook wiring in `processVisitResponseHooks`
- `GuildSkillSyncTest` (7 tests)

## Deferred (Phase 331+)

- Adventurer of Leisure `UneffectRequest.reset()` + HP restore list rebuild
- `handleGuildQuests` meatcar/citadel/factory quest progress
- `DreadScrollManager.decorate()` + choice 703 quest UI sync
- Maximizer guild-skill candidate scoring
