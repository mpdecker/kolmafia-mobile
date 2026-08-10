# Phase 350 — ManaBurn active-effect iteration (TCRS applyModifiers v91)

## Context

Phase 349 completed AT song pre-pass eviction (`REVISION = phase349`, TCRS v90). Desktop `ManaBurnManager.getNextBurnCast` scans active effects (lowest duration first) with `skillBurn{skillId}` priority and `maxManaBurn` duration caps; mobile previously walked mood triggers only.

## Goal

Refactor ManaBurn selection to match desktop core loop:
1. Scan active effects mapped to castable skills via `UneffectSkillEffectMap`
2. Apply `skillBurn{skillId}+100` priority and `maxManaBurn + adventuresLeft` duration cap
3. Respect `allowNonMoodBurning` via `MoodManager.effectInMood`
4. Stub `EffectGainGate.cannotGainEffect` (always false until Maximizer lands)

## Deliverables

1. **`Preferences`** — `MAX_MANA_BURN`, `ALLOW_SUMMON_BURNING`, `skillBurnPrefKey(skillId)`
2. **`EffectGainGate.kt`** — `cannotGainEffect` stub
3. **`MoodManager.effectInMood`** — mood trigger membership check
4. **`ManaBurnManager.pickFromActiveEffects`** — active-effect scan before mood-trigger fallback
5. **`MoodRemovalTriggerExecution.shouldExecute`** — `EffectGainGate` guard for cast-mapped lose_effect triggers
6. **Tests** — extended `ManaBurnManagerTest`, new `EffectGainGateTest`

## Deferred (Phase 351+)

| Item | Reason |
|------|--------|
| `considerBreakfastSkill` / libram summon path | BreakfastManager + UseSkillRequest.BREAKFAST_SKILLS depth |
| `lastChanceBurn` / `lastChanceThreshold` CLI dispatch | Separate burn tail |
| Full desktop burn simulation (`ManaBurn.simulateCast`) | Complex MP budgeting |
| Real `Evaluator.cannotGainEffect` | Maximizer unified Evaluator (Tier 2 #4) |
| `MoodManager.willExecute` pre-check | Smaller polish |

## Verification

```powershell
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.ManaBurnManagerTest"
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.EffectGainGateTest"
.\gradlew.bat :shared:jvmTest
```
