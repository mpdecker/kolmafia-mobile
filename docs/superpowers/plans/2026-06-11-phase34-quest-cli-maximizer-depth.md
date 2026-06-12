# Phase 34 — EGO tower hooks, relay counters, Maximizer crown/bjorn carry

## Track A — Quest depth
- Fernswarthy tower place hook: `step2` + key → `step3` on `fern`/`tower.php` visit
- `QuestAdvanceRules` for EGO tower steps 3–6 and finish
- `fern` CLI alias; `tower` visits apply quest hooks

## Track B — Relay counter CLI/ASH
- `TurnCounter.turnsRemaining` / `findByLabel`
- ASH `turnsleft("label")` overload for relay counters
- CLI `counter relay` lists active relay counters

## Track C — Maximizer step 6
- Auto-equip Crown of Thrones / Buddy Bjorn when `enthrone` / `bjornify` goals present

## Verification
- `:shared:jvmTest`
- `:androidApp:assembleDebug`
- `REVISION = "phase34"`
