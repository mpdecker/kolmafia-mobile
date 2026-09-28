# ASH Residual Polish Mega (7571–7630)

> Tip: `phase7570` → wrap `phase7630`. No mid-track REVISION bump.
> Deepen existing desktop-shaped ASH/script hooks only. Do **not** invent ASH names.
> Do **not** un-stub `user_*` / `git_*` / `svn_*` / `is_headless`. Full xpath remains a non-goal.
> Monster modifier entity rows stay deferred (0 `Monster` rows in `modifiers.txt`).

**Goal:** Close remaining user-script lifecycle prefs (`loginScript` / `kingLiberatedScript` / `choiceAdventureScript` / `counterScript` / ASH `beforePVPScript`) plus two thin runtime leftovers (`to_float` entity ids, SimpleXPath `starts-with`).

## Track A (7571–7590) — Script lifecycle residual

- Pref constants matching desktop names.
- `ScriptHookRunner`: `onLogin` / `onKingLiberated` / `onChoiceAdventure` / `onCounter` / `onBeforePvp`.
- Pref parse: optional `function@script`; strip `call `.
- `ScriptManager.runScriptSync(name, functionName, args, executeTopLevel)`.
- Wire: SessionManager login (after breakfast); AdventureManager choice loop + king + expired counters; GRL `liberateKingAndMaybeRefreshSkills`; PvpManager ASH-then-CLI.

## Track B (7591–7610) — Thin ASH runtime leftover

- `to_float(entity)` via `entityToInt` (desktop `Value.toFloatValue`).
- SimpleXPath `starts-with(@attr,'…')` (common KoL pattern, not full xpath).
- AshP8 LOCATION STAT-typed modifier lookup via `locationModifier` (MONSTER stays null).
- Marker `GameRuntimeLibrary.Phase7571.kt`.

## Track C (7611–7630) — tests + wrap

- ScriptHookRunner / xpath / to_float tests + `corpus_ashResidualPolish_live`.
- Parent wrap: `REVISION` → `phase7630`; parity-audit + AGENTS.
- Close Top Priority #1 ASH residual polish. Remaining: leftover NPC hub HTML (low ROI) + Relay/JS non-goals.
