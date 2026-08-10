# Phase 361 — Maximizer Evaluator v1 (desktop tiebreaker + weighted scoring)

## Context

Phase 360 completed the ManaBurn track. Tier 2 Top Priority #4 is the Maximizer unified Evaluator port from desktop `Evaluator.java`.

## Goal

Introduce `Evaluator.kt` with weighted goal parsing and desktop `getScore(CurrentModifiers)` special cases; wire the desktop TIEBREAKER string into `MaximizerSpeculation` tie-breaking.

## Deliverables

1. **`Evaluator.kt`** — KEYWORD_PATTERN parse, weight/min/max maps, getScore special cases, `Evaluator.tiebreaker()` factory
2. **`MaximizerSpeculation.tiebreakerScore`** — uses `Evaluator.tiebreaker().getScore()` instead of hardcoded 6-modifier sum
3. **`CurrentModifiers`** — internal `characterLevel()` / `primeStatExperience()` / `primeStatExperiencePercent()` for EXPERIENCE scoring
4. **`EvaluatorTest.kt`** — parse, getScore, tiebreaker ordering tests
5. **REVISION `phase361`**, AshP bulk update, parity-audit Phase 361 entry

## Deferred (Phase 362+)

- Multi-weight primary goals in `scoreLoadout` / `MaximizeSpec`
- Full Evaluator item-ranking pipeline
- `addFudge` cross-modifier propagation
- Dedicated `getCurrentML()` for experience scoring precision
