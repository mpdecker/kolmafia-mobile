# Dynamic ChoiceAdventures Spoilers Mega (7331–7390)

> Tip: `phase7330` → wrap `phase7390`. No mid-track REVISION bump.
> Exclude Relay/JS. Deepen desktop-shaped APIs only.
> Residual runtime spoilers stay on `ChoiceAdventures` / `DynamicChoiceSpoilers` / existing managers.
> Do not invent a second spoiler engine. Static catalog + matrix already live.

**Goal:** Match desktop `ChoiceAdventures.choiceSpoilers` / `choiceSpoiler` / `parseChoicesWithSpoilers` / `getOptions` residual behavior so `available_choice_options(true)` and `pickGoalChoice` agree with desktop.

## Track A (7331–7350) — router + bees + parse gate

- `choiceSpoilers`: `DynamicChoiceSpoilers` first, then Violet Fog / Louvre / Monorail (desktop order).
- Early `null` for choices **535 / 536 / 546 / 594** (Safety Shelter / Interview With You / Lost Room) so catalog is not appended.
- `choiceSpoiler(105, 3)`: `ensureUpdatedGuyMadeOfBees` + `"guy made of bees: defeated"` / `"called N times"`.
- `ChoiceUtilities.parseChoicesWithSpoilers`: require `ChoiceCombatAshState.handlingChoice`; look up spoilers via `lastChoice`.

## Track B (7351–7370) — getOptions + solver goal catalogs

- Empty catalog rows (`184`/`185`) fall through to `DynamicChoiceSpoilers` in `pickGoalChoice` / `optionsFor` (desktop `ChoiceAdventure.getOptions`).
- Catalog **535/536/546** carry desktop `RonaldGoals` / `GrimaceGoals` / `VampOutGoals` (GUI/automation options; `choiceSpoilers` still null).
- Wumpus `dynamicChoiceOptions`: desktop 3-slot empty / 6-slot with `i` and `i+3` warnings.

## Track C (7371–7385) — SKIP_ADVENTURE + option-text

- Shared `ChoiceAdventures.SKIP_ADVENTURE` (`"skip adventure"`).
- Public `DynamicChoiceSpoilers.dynamicChoiceOptions(choice)` wrapping live spoiler options.
- Remaining skip-option literals on existing spoilers use the shared constant.

## Track D (7386–7390) — tests + wrap

- Markers Phase7331/7351/7371; tests Phase7350/7370/7390; corpus Tracks A–C.
- Parent wrap: `REVISION` → `phase7390`; parity-audit + AGENTS.
