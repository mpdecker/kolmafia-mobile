# Phase 95: AshP53 Numeric Scale / Cap / Floor Stats

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase95`

## Goal

When a monster has numeric `Scale:` and no `Atk:`/`Def:`/`HP:` attribute, resolve attack/defense/HP via desktop Scale+Cap+Floor (beeosity = 1). Fix Cap/Floor `?` → defaults 10000/10. Route `expected_damage` through `monsterAttack`.

## Deliverables

| Area | Change |
|------|--------|
| Model | `hasAttack`/`hasDefense`/`hasHp`; `DEFAULT_CAP`/`DEFAULT_FLOOR` |
| Parse | Cap/Floor `?` → defaults; omitted Cap/Floor on scaling → defaults |
| Math | `CombatAdjustment.scaledAttack`/`scaledDefense`/`scaledHp` |
| Wire | `monsterAttack`/`Defense`/`Hp` Scale path; AshP39 `expected_damage` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- Expression `Scale:`/`Cap:`/`Floor:` (`equipped()`, etc.)
- Beeosity; BIG-core; `REDUCE_ENEMY_DEFENSE`
- Expression `Exp:` / `$monster[base_mainstat_exp]`
- `dad_sea_monkee_weakness` / `unusual_construct_disc`
- Manuel; queue-aware `appearance_rates`; full `MonsterStatusTracker`
- Maximizer Evaluator; bastille/nonfilling
