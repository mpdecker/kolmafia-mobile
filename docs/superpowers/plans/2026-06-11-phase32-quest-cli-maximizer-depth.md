# Phase 32 — Nemesis counters, send/csend kmail, Maximizer constraints

## Track A — Quest depth
- `TurnCounter` writes `relayCounters` pref (start/stop/reset on assassin fight)
- NEMESIS step17 unlock starts assassin window counters
- `QuestFightRules` for assassin fight win/loss steps 18–25 + volcano map
- `QuestChoiceRules` for choice 930/932/1088 text signals

## Track B — CLI send/csend
- `send N item to recipient [|| message]` and `csend N meat to recipient`
- Multi-item comma lists (up to 11) via indexed `whichitemN`/`howmanyN`
- Extend `SendMailRequest` with attachments + meat

## Track C — Maximizer step 4
- Comma goal parsing: primary stat, `+boolean`, `-boolean`, `equip`, `switch`
- Constraint filtering via `ModifierParser`
- `FamiliarManager` switch before familiar-slot equip

## Verification
- `:shared:jvmTest`
- `:androidApp:assembleDebug`
- `REVISION = "phase32"`
