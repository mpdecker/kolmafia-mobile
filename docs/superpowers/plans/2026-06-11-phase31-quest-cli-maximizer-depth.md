# Phase 31 — Quest conditionals, CLI niche aliases, Maximizer display/stash

## Track A — Quest depth
- `QuestLogSync.applyPlaceHooks`: scg visit bumps NEMESIS step8→9, step16→16.5→17
- FACTORY finish when thick padded envelope (id 3201) in inventory on paco visit
- NEMESIS step10 rule excludes "not the required mettle" signal
- `QuestDatabase.stepOrdinal` supports fractional steps (step16.5)

## Track B — CLI niche patterns
- `skill` alias for `cast`
- `mallbuy` alias for `buy`
- `kmail recipient message` via `SendMailRequest`
- `coinmaster buy N nick item` quantity form

## Track C — Maximizer step 3
- Score + pull from display case and clan stash before equip

## Verification
- `:shared:jvmTest`
- `:androidApp:assembleDebug`
- `REVISION = "phase31"`
