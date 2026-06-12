# Phase 36 — NEMESIS volcano finish, guild choices, CLI stubs, accessory combos

## Track A — Quest depth
- NEMESIS steps 26–29: choice 189, volcanic fight start/win, pants step27, buckle finished
- QuestItemRules: volcano map ID 3291, legendary pants 4267–4272, belt 4327
- Guild choices: 931→CITADEL step6, 932→step8, 542→MOXIE step1, 1061→ARMORER step1
- TurnCounter reset on volcano map gain

## Track B — CLI niche
- reset — alias for clear output buffer
- alias / alias x => y — pref-backed CLI aliases
- volcano visit — bigisland.php page visit
- enable/disable pref — boolean pref toggles
- factory — paco guild visit with quest hooks

## Track C — Maximizer step 8
- Bounded 3-accessory combination pass (top-K candidates)
- maximizerCombinationLimit pref

## Verification
- `:shared:jvmTest`
- `:androidApp:assembleDebug`
- `REVISION = "phase36"`
