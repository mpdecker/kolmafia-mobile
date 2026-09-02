# Behavioral Deepen Mega III — Phases 4371–4430

**Goal:** Close remaining high-traffic behavioral gaps without reopening Relay/JS/TCRS-derive non-goals.

**Target revision:** `phase4430`

## Tracks

| Phases | Track | Deliverable |
|--------|-------|-------------|
| 4371–4390 | **A — FightBanish residual** | `ORDER_A_KNEECAPPING` enum; desktop fight HTML patterns; `FightBanishSync` fallbacks; choice/item banish regression |
| 4391–4410 | **B — Maximizer outfit half-set** | Conditional half-set bonus scoring in `CurrentModifiers` / `Evaluator` |
| 4411–4425 | **C — Breakfast niche** | `checkJackass` + `collectSeaJelly` in `BreakfastManager` |
| 4426–4430 | **D — Closure** | `AshCompatibilityCorpusTest` banish/maximizer snippets; `phase4430`; parity audit recount |

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
git diff --check
```
