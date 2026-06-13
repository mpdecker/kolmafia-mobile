# Phase 40 — Worship/desert quests, zone CLI, best familiar switch

## Track A — Quest depth
- QuestItemRules: ancient amulet finishes WORSHIP; desert pamphlet starts DESERT
- QuestAdvanceRules: WORSHIP step2; DESERT started signals
- QuestChoiceRules: 125/3→step3, 584/4→step2, 1002 amulet finish
- QuestLogSync: PYRAMID started → DESERT finished

## Track B — CLI niche
- woods / mountains / beach / pyramid location shortcuts
- ASH hippy_store_available / dispensary_available / hidden_temple_unlocked

## Track C — Maximizer step 12
- Pick highest-scoring owned familiar when multiple `switch` races are listed

## Verification
- `:shared:jvmTest`
- `:androidApp:assembleDebug`
- `REVISION = "phase40"`
