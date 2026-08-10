# Phase 351 — ManaBurn breakfast/libram path (TCRS applyModifiers v92)

## Context

Phase 350 completed active-effect ManaBurn scanning (`REVISION = phase350`, TCRS v91). `ALLOW_SUMMON_BURNING` was added but not wired.

Desktop reference: `ManaBurnManager.java` `considerBreakfastSkill` / `considerLibramSummon` (lines 244–303).

## Goal

When `allowSummonBurning` is true, match desktop burn selection for non-effect MP sinks:

1. Pre-compute a breakfast or libram pick before the active-effect scan
2. During scan: if all scanned effects have `duration >= manaBurnSummonThreshold`, return the breakfast/libram pick early
3. Clear the breakfast candidate when an extendable effect is found
4. When no effect is extendable, fall back to the breakfast/libram pick

## Deliverables

1. **`Preferences`** — `LIBRAM_SUMMONS`, `LIBRAM_SKILLS_HARDCORE`/`LIBRAM_SKILLS_SOFTCORE`, `libramSkillsPrefKey(isHardcore)`
2. **`BreakfastBurnSkills.kt`** — desktop `BREAKFAST_SKILLS`/`LIBRAM_SKILLS` arrays + `getBreakfastLibramSkills` + Pastamastery/`canEat` and Cocktailcrafting/`canDrink` gates
3. **`LibramSkillCasts.kt`** — triangulated MP cost + `libramSkillCasts` + `firstLibramBatch` rotation
4. **`ManaBurnManager`** — `ManaBurnPick`, `considerBreakfastSkill`/`considerLibramSummon`, breakfast integration in `pickFromActiveEffects`, multi-cast `burnIfEnabled` + `LIBRAM_SUMMONS` increment on libram casts
5. **Tests** — extended `ManaBurnManagerTest`, new `LibramSkillCastsTest`

## Deferred (Phase 352+)

| Item | Reason |
|------|--------|
| `lastChanceBurn` / `lastChanceThreshold` tail | Separate burn tail |
| Full `ManaBurn.simulateCast` balanced loop | Complex MP budgeting |
| Real `Evaluator.cannotGainEffect` | Maximizer Tier 2 #4 |
| `libramSkillMPConsumption` mana-cost adjustment | No mobile `getManaCostAdjustment` yet |
| Multi-skill libram in one HTTP request | Mobile v1 returns one skill batch per burn iteration |

## Verification

```powershell
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.ManaBurnManagerTest"
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.LibramSkillCastsTest"
.\gradlew.bat :shared:jvmTest
```

Result: **4,739 tests**, `REVISION = phase351`, TCRS v92.
