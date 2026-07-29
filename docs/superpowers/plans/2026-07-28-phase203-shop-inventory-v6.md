# Phase 203: AshP201 Shop Inventory v6 + AshP202 Validate v34

**Delivered:** 2026-07-28

## AshP201 — Shop inventory v6

- `SkillDefinitionDatabase.registerFromShopVisit(skillId, name, image)` — minimal visit-learned skill definitions when HTML exposes unknown skill ids
- `ShopRowParser` TD3A skill name parse + `whichskill=` image capture + registration hook during multi-cost row parse
- `ShopRowDatabase.registerShop(shopId, shopName, shopType)` + `promoteShopType` — desktop NONE→COIN/NPC and NPC+COIN→NPCCOIN promotion with CONC conflict session-log warning
- `ShopInventorySync` inferred shop type on visit (coinmaster → COIN, meat rows without coinmaster → NPC)

## AshP202 — Validate v34

- Shop visit `desc_skill.php?whichskill={id}&self=true` prefetch for newly registered skills (reuses existing desc_skill visit hook)
- `GameRuntimeLibraryAshP202Test` + `corpus_dynamicSkillShopVisit_live` — dynamic skill shop overlay + `containsBuySkill(validate=true)` without pre-bundled classskills entry

## Revision

- `GameRuntimeLibrary.REVISION = phase203`
- Batch markers: `GameRuntimeLibrary.AshP201Batch.kt`, `GameRuntimeLibrary.AshP202Batch.kt`

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
```
