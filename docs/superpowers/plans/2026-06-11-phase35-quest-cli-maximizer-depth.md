# Phase 35 — NEMESIS choice/item depth, cemetery CLI, Maximizer thrall scoring

## Track A — Quest depth
- QuestChoiceRules: 1049→step3, 1087 visit→step11, 1088 visit→step13
- QuestFightRules: Unknown class→step2, Clownlord Beelzebozo→step6
- QuestItemRules: legendary weapon→step8, 6×8427→step14, scalp→step16
- QuestAdvanceRules: EGO step2 alt signals, NEMESIS STARTED variants
- QuestLogSync: frCemetaryUnlocked on snarfblat=507; fernruin→step3

## Track B — CLI niche
- choice N / choice ID N — wire to ChoiceRequest
- thralls — print pastaThrall1–8 status
- cemetery / cemetary — place visit + hooks
- counters — list counter_* prefs
- clear / cls — reset cli_execute_output buffer

## Track C — Maximizer thrall step 7
- ModifierDatabase.getThrall + ExpressionContext P
- Score bound thrall in maximize totals; greedy best thrall pick
- switch thrall goal term + bind-skill dispatch
- MaximizeResult.thrallSwitched

## Verification
- `:shared:jvmTest`
- `:androidApp:assembleDebug`
- `REVISION = "phase35"`
