# Phase 359 — Libram MP Cost Adjustment (TCRS applyModifiers v100)

## Context

Phase 358 wired multi-skill libram CLI. Mobile `LibramSkillCasts.libramSkillMpCost` omitted desktop `KoLCharacter.getManaCostAdjustment()`.

## Goal

Apply modifier-based mana-cost adjustment to libram MP precompute in ManaBurn, matching desktop `SkillDatabase.libramSkillMPConsumption(cast)`.

## Deliverables

1. **`LibramSkillCasts.libramSkillMpCost(cast, manaCostAdjustment)`** — desktop formula with adjustment
2. **`LibramSkillCasts.libramSkillCasts(..., manaCostAdjustment)`** — affordable cast loop uses adjusted costs
3. **`ManaBurnManager.manaCostAdjustmentProvider`** — wired from `CombatAdjustment.manaCostModifier(buildCurrentModifiers(), combat = false)` in `GameRuntimeLibrary` init
4. **Thread `manaCostAdjustment`** through `considerBreakfastSkill`/`considerLibramSummon`/`pickFromActiveEffects`/`resolveBurnAction`/`burnIfEnabled`
5. **Tests** — LibramSkillCasts adjustment cases + ManaBurnManager integration
6. **Audit** — TCRS v100, `REVISION = phase359`, document unused-skill sweep as explicit non-goal

## Deferred

| Item | Reason |
|------|--------|
| Unused-skill `skillBurn` sweep | Still TODO on desktop |
| `UseSkillRequest` libram HTTP path | No mobile equivalent yet |
| Full Maximizer `Evaluator` | Tier 2 Top Priority |

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
```

4,777 tests passing.
