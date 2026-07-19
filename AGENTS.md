## Learned User Preferences

- Do not edit attached plan files during implementation; treat them as read-only reference.
- Do not recreate plan todos; mark existing ones in_progress and complete all before stopping.
- Do not git commit unless explicitly requested.
- Use the `gh` CLI for GitHub pull requests when asked.
- After phase implementation, update `docs/parity-audit.md` with metrics and a history entry.
- Phase planning and gap-closing work should follow `docs/parity-audit.md` Top Priorities.
- Short "pr" means commit current work and open or update a GitHub PR via `gh`.
- Short "push" means git push the current branch to remote.
- "next pass", "next", or "next phase" means continue the next incremental parity phase from `docs/parity-audit.md` Top Priorities.
- Short "continue" means resume in-progress parity work or proceed with Top Priorities when between tasks.
- When committing, stage only files related to the current work; exclude unrelated or line-ending-only changes.

## Learned Workspace Facts

- Kotlin Multiplatform KoLmafia mobile port; core logic lives in `shared/src/commonMain`.
- Modules include `shared`, `androidApp`, and platform targets.
- Desktop KoLmafia Java reference source is at `C:\Development\kolmafia\kolmafia\src\`; ported data files live in `shared/src/commonMain/composeResources/files/data/`.
- `docs/parity-audit.md` is the authoritative desktop-vs-mobile parity document.
- Phase plans live under `docs/superpowers/plans/`; ASH batches in `GameRuntimeLibrary.AshP*Batch.kt` — AshP8–P18 registration floor, AshP19–AshP39 live behavioral batches (entity `to_*` resolvers, `$entity[field]` bracket reads via `*EntityFields.kt` including MONSTER/LOCATION/PATH fields, AshP39 `CombatAdjustment`, modifier lookups via `modifiers/*`; roadmap: `2026-06-11-ash-script-full-parity.md`).
- Run unit tests with `.\gradlew.bat :shared:jvmTest`; verify Android builds with `.\gradlew.bat :androidApp:assembleDebug`.
- On Windows PowerShell, chain shell commands with `;`, not `&&`.
- Incremental parity phases use Quest/CLI/Maximizer tracks; Phases 53–81 advanced Tier 1 (AshP19–AshP39 entity batches including MONSTER/LOCATION/PATH brackets and combat adjustment, CLI long-tail including `journey`/`witchess`/`volcano`, charpane sync for Ed/VYKEA/pasta thrall, tower/hedge maze/PirateRealm sync); `GameRuntimeLibrary.REVISION` uses `phaseNN` format; update `docs/parity-audit.md` after each phase.
- Quest state sync combines QuestAdvanceRules, QuestChoiceRules, QuestFightRules, QuestItemRules, QuestLogSync, QuestSpecialSync, and dedicated `quest/*Sync.kt` visit parsers (e.g. TowerSync, TelescopeSync, PirateRealmSync); Ed servants in `servant/EdServantManager.kt` with `EdServantChoiceSync`/`EdServantCharpaneSync`; VYKEA companion in `vykea/VykeaCompanionManager.kt` with `VykeaCharpaneSync`; pasta thrall in `thrall/PastaThrallManager.kt` with `PastaThrallCharpaneSync`; bundled data loaders: `journeyman.txt` via `data/JourneymanDatabase.kt`, `witchess_solutions.txt` via `data/WitchessSolutionDatabase.kt`/`session/WitchessManager.kt`, `volcanomaze.txt` via `data/VolcanoMazeDatabase.kt`/`session/VolcanoMazeManager.kt`; tower automation in `TowerDoorRunner.kt`/`TowerDoorConfig.kt`; hedge maze in `HedgeMazeRunner.kt`/`HedgeMazeConfig.kt`.
- ASH runtime functions register via `regFn()` in `GameRuntimeLibrary.*.kt` under `ash/`; AshScope uses first-match overload resolution — remove AshP11 stubs and wire live lookups in AshP12–P18 `resolveModifier*` helpers when adding AshP19+ behavioral batches.
- `AshCompatibilityCorpusTest` is the behavioral ASH regression corpus; JavaScript runtime and relay server are explicit parity non-goals.
- Git default branch is `master`; avoid stale `.worktrees/` for active development; exclude `.cursor/` hook state from commits unless requested.
