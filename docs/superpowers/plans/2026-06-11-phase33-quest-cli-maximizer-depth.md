# Phase 33 — Turn counter expiry, gift CLI, Maximizer slot/familiar depth

## Track A — Quest depth
- `TurnCounter.removeExpired` prunes past-due relay counters each adventure turn
- `QuestLogSync` EGO key turn-in when visiting guild with Fernswarthy's key at STARTED
- `AdventureManager.checkQuestAdvancement` passes full `QuestSyncContext`

## Track B — CLI gift
- `SendGiftRequest` via `town_sendgift.php` with package auto-selection
- `gift N item to recipient [|| message]` CLI dispatch
- `send` falls back to gift when kmail fails (non-meat attachments)

## Track C — Maximizer step 5
- `+melee` / `+hands` slot constraints in comma goals
- `enthrone` / `bjornify` familiar terms (like `switch`)
- Score enthroned/bjornified familiar modifiers in maximize totals

## Verification
- `:shared:jvmTest`
- `:androidApp:assembleDebug`
- `REVISION = "phase33"`
