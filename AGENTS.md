## Learned User Preferences

- Do not edit attached plan files during implementation; treat them as read-only reference.
- Do not recreate plan todos; mark existing ones in_progress and complete all before stopping.
- Do not git commit unless explicitly requested.
- Use the `gh` CLI for GitHub pull requests when asked.
- After phase implementation, update `docs/parity-audit.md` with metrics and a history entry.
- Phase planning and gap-closing work should follow `docs/parity-audit.md` Top Priorities.
- Short "pr" means commit current work and open or update a GitHub PR via `gh`.
- Short "push" means git push the current branch to remote.
- "next pass" or "next" means continue the next incremental parity phase from `docs/parity-audit.md` Top Priorities.
- Short "continue" means resume in-progress parity work or proceed with Top Priorities when between tasks.

## Learned Workspace Facts

- Kotlin Multiplatform KoLmafia mobile port; core logic lives in `shared/src/commonMain`.
- Modules include `shared`, `androidApp`, and platform targets.
- Desktop KoLmafia Java reference source is at `C:\Development\kolmafia\kolmafia\src\`.
- `docs/parity-audit.md` is the authoritative desktop-vs-mobile parity document.
- Phase plans live under `docs/superpowers/plans/`; ASH overload batches register in `GameRuntimeLibrary.AshP*Batch.kt` (original roadmap: `2026-06-11-ash-script-full-parity.md`).
- Run unit tests with `.\gradlew.bat :shared:jvmTest`.
- Verify Android builds with `.\gradlew.bat :androidApp:assembleDebug`.
- On Windows PowerShell, chain shell commands with `;`, not `&&`.
- Incremental parity phases use Quest/CLI/Maximizer tracks; completion is tracked via `GameRuntimeLibrary.REVISION` and parity-audit history.
- Quest state sync is split across QuestAdvanceRules, QuestChoiceRules, QuestFightRules, QuestItemRules, QuestLogSync, and QuestSpecialSync.
- ASH runtime functions register via `regFn()` in `GameRuntimeLibrary.*.kt` under `ash/`.
- Git default branch is `master`; avoid stale `.worktrees/` for active development.
