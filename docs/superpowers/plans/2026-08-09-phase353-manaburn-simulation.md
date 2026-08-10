# Phase 353 — ManaBurn balanced simulation (TCRS applyModifiers v94)

## Context

Phase 352 completed ManaBurn last-chance CLI tail (`REVISION = phase352`, TCRS v93). Mobile `pickFromActiveEffects` still returned on the first extendable effect with `quantity = 1`.

Desktop reference: `ManaBurnManager.java` lines 194–241 + `ManaBurn.java`.

## Goal

When multiple effects are extendable, match desktop MP budgeting: simulate round-robin casts (lowest simulated duration first) until MP exhausted, then return the **chosen** skill with its simulated cast count.

## Deliverables

1. **`ManaBurn.kt`** — port of desktop `ManaBurn.java` with `isCastable`, `simulateCast`, `Comparable` by duration, and `simulateBalancedCasts`
2. **`SkillDefinitionProxy.getEffectDuration` v1** — base duration from `SkillDefinitionDatabase`; double songs when player has Good Singing Voice (11016)
3. **`ManaBurnManager.pickFromActiveEffects`** — collect all extendable burns, run simulation, return multi-cast quantity
4. **Tests** — `ManaBurnSimulationTest` (3 cases) + `pickFromActiveEffects_returnsSimulatedQuantityGreaterThanOne` in `ManaBurnManagerTest`

## Deferred (Phase 354+)

| Item | Reason |
|------|--------|
| Full `getEffectDuration` specials (TT blessings, pasta binds, Spirit Boon, etc.) | Desktop `SkillDatabase.getEffectDuration` has ~20 skill-id branches |
| Real `EffectGainGate.cannotGainEffect` | Maximizer Tier 2 #4 |
| `libramSkillMPConsumption` mana-cost adjustment | No mobile `getManaCostAdjustment` yet |
| Multi-skill libram in one HTTP request | Acceptable v1 per Phase 351 |
| Unused-skill `skillBurn` sweep (desktop TODO line 209) | Separate enhancement |

## Verification

```powershell
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.ManaBurnSimulationTest"
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.ManaBurnManagerTest"
.\gradlew.bat :shared:jvmTest
```

Result: TCRS **v94**, `REVISION = phase353`.
