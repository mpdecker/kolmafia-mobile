# Phase 357 — EffectGainGate cannotGainEffect (TCRS applyModifiers v98)

## Context

Phase 356 wired ManaBurn `physicalAccessibleCount`. Mobile `EffectGainGate.cannotGainEffect` remained a stub returning `false`.

## Goal

Port desktop `Evaluator.cannotGainEffect` so ManaBurn and mood removal triggers skip effects the character cannot gain (class, TT blessing track, pasta thrall binds, Sneaky Pete muffler, etc.).

## Deliverables

1. **`TurtleBlessing.kt`** + **`EffectGainEffectIds.kt`** + **`CharacterState` class helpers** + **`PETE_MOTORBIKE_MUFFLER` pref**
2. **`EffectGainGate.cannotGainEffect(effectId, charState, effectState, prefs)`** — full desktop switch
3. **Call sites** — `ManaBurnManager.pickFromActiveEffects`, `MoodRemovalTriggerExecution.shouldExecute`
4. **Tests** — `EffectGainGateTest` parity cases, ManaBurn pastamancer thrall skip, `MoodRemovalTriggerExecutionTest` signature update
5. **Audit** — TCRS v98, `REVISION = phase357`

## Deferred (Phase 358+)

| Item | Reason |
|------|--------|
| Unused-skill `skillBurn` sweep | Desktop TODO in `ManaBurnManager.java` line 209 |
| `libramSkillMPConsumption` | No mobile mana-cost adjustment yet |
| Multi-skill libram in one HTTP request | Acceptable v1 per Phase 351 |
| Full Maximizer `Evaluator` | Tier 2 Top Priority; separate track |

## Verification

```powershell
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.EffectGainGateTest"
.\gradlew.bat :shared:jvmTest
```

## Result

`REVISION = phase357`, TCRS v98.
