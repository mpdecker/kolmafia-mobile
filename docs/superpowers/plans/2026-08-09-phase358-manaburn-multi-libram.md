# Phase 358 — Multi-skill libram ManaBurn (TCRS applyModifiers v99)

## Context

Phase 357 wired `EffectGainGate.cannotGainEffect`. Mobile `considerLibramSummon` v1 returned only the first libram batch as `ManaBurnPick`.

## Goal

When multiple libram skills are configured, emit desktop-style semicolon-separated `cast N skill;...` via `ManaBurnAction.Cli`.

## Deliverables

1. **`LibramSkillCasts.buildLibramSummonCommand`** — desktop rotation/batch math
2. **`considerBreakfastSkill`/`considerLibramSummon`/`pickFromActiveEffects`** → `ManaBurnAction?` (Cast for single skill, Cli for multi-libram)
3. **`resolveBurnAction`** — pass through Cli from active-effect scan before last-chance tail
4. **Tests** — command builder, multi/single libram, resolveBurnAction, burnIfEnabled CLI path
5. **Audit** — TCRS v99, `REVISION = phase358`

## Deferred (Phase 359+)

| Item | Reason |
|------|--------|
| Unused-skill `skillBurn` sweep | Still TODO on desktop |
| `libramSkillMPConsumption` + `getManaCostAdjustment` | No mobile mana-cost adjustment pipeline |
| Full Maximizer `Evaluator` | Tier 2 Top Priority |

## Verification

```powershell
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.ManaBurnManagerTest"
.\gradlew.bat :shared:jvmTest
```

## Result

`REVISION = phase358`, TCRS v99.
