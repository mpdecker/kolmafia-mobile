## Learned User Preferences

- Do not edit attached plan files during implementation; treat them as read-only reference.
- Do not recreate plan todos; mark existing ones in_progress and complete all before stopping.
- Do not git commit unless explicitly requested.
- Use the `gh` CLI for GitHub pull requests when asked.
- After phase implementation, update `docs/parity-audit.md` with metrics and a history entry.
- Phase planning and gap-closing work should follow `docs/parity-audit.md` Top Priorities.

## Learned Workspace Facts

- Kotlin Multiplatform KoLmafia mobile port; core logic lives in `shared/src/commonMain`.
- Modules include `shared`, `androidApp`, and platform targets.
- Desktop KoLmafia reference source is at `C:\Development\kolmafia\kolmafia\`.
- `docs/parity-audit.md` is the authoritative desktop-vs-mobile parity document.
- Completed phase plans are recorded under `docs/superpowers/plans/`.
- Run unit tests with `.\gradlew.bat :shared:jvmTest`.
- Verify Android builds with `.\gradlew.bat :androidApp:assembleDebug`.
- On Windows PowerShell, chain shell commands with `;`, not `&&`.
- Phase completion is tracked via `GameRuntimeLibrary.REVISION` and parity-audit history entries.
- Use `main` branch `shared/` as the canonical source tree; avoid stale worktrees for active work.
