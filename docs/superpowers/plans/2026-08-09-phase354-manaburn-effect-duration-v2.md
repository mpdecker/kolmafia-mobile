# Phase 354 — ManaBurn getEffectDuration v2 (TCRS applyModifiers v95)

## Context

Phase 353 completed balanced multi-cast simulation (`REVISION = phase353`, TCRS v94). Mobile `getEffectDuration` v1 only handled base duration + song doubling.

Desktop reference: `SkillDatabase.getEffectDuration` lines 975–1061 (non-buff branch).

## Goal

Match desktop per-cast duration for class-gated and resource-gated non-buff skills so ManaBurn `simulateBalancedCasts` budgets MP correctly.

## Deliverables

1. **`TurtleBlessingLevel.kt`** — `fromActiveEffects` + `boonDuration()` mirroring desktop `KoLCharacter.getBlessingLevel`
2. **`SkillDefinitionProxy.getEffectDuration` v2** — Spirit Boon, TT blessings, pasta binds, Rev Engine / Biker Swagger audience math
3. **`ManaBurnManager`** — pass `charState` + `effectState` into `getEffectDuration`
4. **Tests** — 6 new cases in `ManaBurnSimulationTest` + updated song test signature

## Deferred (Phase 355+)

| Item | Reason |
|------|--------|
| Wizard hat +5 on OTHER buffs | Needs inventory/equipped hat checks + LoL replica hat |
| BuffTool TAMER/SAUCE/THIEF loops | Needs tool tables + accessible item/equip wiring |
| `EffectGainGate.cannotGainEffect` | Maximizer Tier 2 #4 |
| `libramSkillMPConsumption` | No mobile mana-cost adjustment yet |
| Multi-skill libram in one HTTP request | Acceptable v1 per Phase 351 |

## Verification

```powershell
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.ManaBurnSimulationTest"
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.ManaBurnManagerTest"
.\gradlew.bat :shared:jvmTest
```

Result: TCRS **v95**, `REVISION = phase354`.
