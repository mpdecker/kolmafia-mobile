# Phase 29: Script Compatibility + Maximizer MVP

*Completed: 2026-06-11*

## Summary

Dual-track batch closing script compatibility blockers and shipping a scoped Maximizer MVP on the existing KMP `shared/` architecture.

## Track A: Script Compatibility

| Item | Status | Notes |
|------|--------|-------|
| A1 CLI dispatch | Done | `goal choice/substats`, `set`/`get`, `counter`, `ccs`/`ccprep`/`macro`, location shortcuts (`spooky`, `cellar`, `tower`, `guild`), `jukebox` |
| A2 Modifier expressions | Done | `mod()`, `fam()`, `famattr()`, `mainhand()`, `res()` live; item-ID modifier lookup |
| A3 Combat state | Done | `inMultiFight` / `fightFollowsChoice` in `AdventureManager`; ASH reads live values |
| A4 Quest sync | Done | Guild/misc rules (SEA_OLD_GUY, PIRATEREALM, TELEGRAM, etc.); expanded `QuestLogSync.SYNC_SIGNALS` |
| A5 Session logging | Done | `SessionLogger` on `GameEventBus`; ASH `session_logs(days)` |
| A6 Bug fixes | Done | VampOut returns `null` when all locations visited; breakfast `checkJackass` + `collectSeaJelly` (22/22) |

## Track B: Maximizer MVP

| Item | Status | Notes |
|------|--------|-------|
| B1 Core package | Done | `MaximizerManager`, `MaximizeGoal`, `MaximizeResult` — inventory-only greedy per-slot |
| B2 CLI/ASH wire | Done | `maximize <goal>` CLI, `maximize()` / `maximize(goal)` ASH; registered in `SharedModule` |
| B3 Thrall groundwork | Done | `AshType.THRALL`; `my_thrall()` reads `_currentThrall` pref |
| B4 Tests | Done | `MaximizerManagerTest`, updated script/CLI/combat tests |

## Verification

- `:shared:jvmTest` — **1,494 tests**, all passing
- `:androidApp:assembleDebug` — success
- `docs/parity-audit.md` updated (Phase 29 history entry)

## Out of Scope (deferred)

- Full desktop Maximizer (constraints, storage pulls, familiar optimization)
- Full relay server (`webui/` decorators)
- Desktop `RequestLogger` aggregate dump

## Stale Worktree

`.worktrees/phase6-goal-depth/` was 22 phases behind `main` and contained only untracked duplicate sources. **Archived/removed** — use `main` branch `shared/` as the sole source of truth.
