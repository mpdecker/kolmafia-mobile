# Phase 352 — ManaBurn last-chance burn tail (TCRS applyModifiers v93)

## Context

Phase 351 completed breakfast/libram ManaBurn precompute (`REVISION = phase351`, TCRS v92). Desktop `lastChanceBurn`/`lastChanceThreshold` prefs existed in `defaults.txt` but were not wired on mobile.

Desktop reference: `ManaBurnManager.java` lines 203–221.

## Goal

After all existing `pickBurnPick` fallbacks fail, match desktop last-chance behavior:

- Gate on `lastChanceThreshold` (default 100 MP above burn reserve)
- Substitute `#` in `lastChanceBurn` with available burn MP
- Dispatch via CLI (`cliExecutor`), not `skillManager.cast`

## Deliverables

1. **`Preferences`** — `LAST_CHANCE_BURN`, `LAST_CHANCE_THRESHOLD`
2. **`ManaBurnManager`** — `considerLastChanceBurn`, `resolveBurnAction`, `ManaBurnAction.Cli`, `cliExecutor`, `burnIfEnabled` CLI path
3. **`GameRuntimeLibrary` + `SharedModule`** — inject `ManaBurnManager`, wire `cliExecutor` to `dispatchCli`
4. **Tests** — 5 new cases in `ManaBurnManagerTest`

## Deferred (Phase 353+)

| Item | Reason |
|------|--------|
| `ManaBurn.simulateCast` balanced multi-effect loop | Requires `ManaBurn.kt` + effect-duration lookup |
| Real `EffectGainGate.cannotGainEffect` | Maximizer Tier 2 #4 |
| `libramSkillMPConsumption` mana-cost adjustment | No mobile `getManaCostAdjustment` yet |
| Multi-skill libram in one HTTP request | Acceptable v1 per Phase 351 |

## Verification

```powershell
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.ManaBurnManagerTest"
.\gradlew.bat :shared:jvmTest
```

Result: TCRS **v93**, `REVISION = phase352`.
