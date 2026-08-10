# Phase 355 — ManaBurn getEffectDuration v3 (TCRS applyModifiers v96)

## Context

Phase 354 completed non-buff `getEffectDuration` v2 (`REVISION = phase354`, TCRS v95, 4,754 tests). Mobile still returned base duration for OTHER buffs without wizard hat +5 or BuffTool equipment/inventory bonuses.

Desktop reference: `SkillDatabase.getEffectDuration` lines 1024–1061; `UseSkillRequest.BuffTool` tables lines 155–212.

## Goal

ManaBurn `effectDurationPerCast` for TT/SA/AT OTHER buffs and wizard-hat-eligible buffs must match desktop tool selection so `simulateBalancedCasts` stops at the correct horizon.

## Deliverables

1. **`BuffTool.kt`** — `TAMER_TOOLS`/`SAUCE_TOOLS`/`THIEF_TOOLS` + `toolsForSkill(skillId)`
2. **`SkillDefinitionProxy`** — `isTurtleTamerBuff`/`isSaucerorBuff`; `getEffectDuration` v3 with `accessibleCount` + `gameDatabase`
3. **`BuffToolDuration.kt`** — wizard hat (+5, IDs 1653/11199), tool availability, best bonus loop
4. **`ManaBurnManager`** — `accessibleCountProvider` + `gameDatabase`; wired in `pickFromActiveEffects`/`burnIfEnabled`
5. **`GameRuntimeLibrary` init** — inventory snapshot provider + `gameDatabase`
6. **Tests** — 4 new cases in `ManaBurnSimulationTest`

## Deferred (Phase 356+)

| Item | Reason |
|------|--------|
| `AccessibleItemCount.physicalCount` in ManaBurn provider | Closet/storage/equipped depth beyond inventory snapshot |
| `EffectGainGate.cannotGainEffect` | Maximizer Tier 2 #4 |
| `libramSkillMPConsumption` | No mobile mana-cost adjustment yet |
| Multi-skill libram in one HTTP request | Acceptable v1 per Phase 351 |
| Unused-skill `skillBurn` sweep | Desktop TODO line 209 |

## Verification

```powershell
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.ManaBurnSimulationTest"
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.ManaBurnManagerTest"
.\gradlew.bat :shared:jvmTest
```

## Result

`REVISION = phase355`, TCRS v96, 4,758 tests.
