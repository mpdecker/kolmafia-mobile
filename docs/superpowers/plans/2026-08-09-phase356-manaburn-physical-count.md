# Phase 356 — ManaBurn physicalCount provider (TCRS applyModifiers v97)

## Context

Phase 355 wired BuffTool/wizard-hat duration with an inventory-only `accessibleCountProvider`. Full `AccessibleItemCount.physicalCount` (closet, storage, display, stash, equipped) was deferred.

## Goal

ManaBurn `simulateBalancedCasts` must see BuffTool items outside inventory when computing `effectDurationPerCast`.

## Deliverables

1. **`ManaBurnManager`** — suspend `accessibleCountProvider`; suspend `pickFromActiveEffects`/`pickBurnPick`/`resolveBurnAction`/`pickSkillToBurn`; `prefetchAccessibleCounts` for sync `getEffectDuration`
2. **`GameRuntimeLibrary` init** — provider delegates to `physicalAccessibleCount(itemId, itemName)`
3. **Tests** — `ManaBurnManagerTest` migrated to `runTest`; closet-only BuffTool integration case
4. **Audit** — TCRS v97, `REVISION = phase356`

## Deferred (Phase 357+)

| Item | Reason |
|------|--------|
| `EffectGainGate.cannotGainEffect` | Desktop `Evaluator` switch; Maximizer Tier 2 |
| Unused-skill `skillBurn` sweep | Desktop TODO |
| `libramSkillMPConsumption` | No mobile mana-cost adjustment yet |
| Multi-skill libram in one HTTP request | Acceptable v1 per Phase 351 |

## Verification

```powershell
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.ManaBurnManagerTest"
.\gradlew.bat :shared:jvmTest
```

## Result

`REVISION = phase356`, TCRS v97, 4,759 tests.
