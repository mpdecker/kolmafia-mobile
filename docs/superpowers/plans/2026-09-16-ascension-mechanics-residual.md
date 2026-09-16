# Ascension Mechanics Residual Mega (7451–7510)

> Tip: `phase7450` → wrap `phase7510`. No mid-track REVISION bump.
> Exclude Relay/JS. Deepen desktop-shaped APIs only.
> Desktop has **no AscensionManager.java** and the inline post-ascension HTTP storm
> is intentionally headless — do not invent one.
> Residual stays on `ValhallaManager` / `AfterLifeRequest` / `ChoiceCombatAshState` /
> `QuestDatabase` / `BanishManager` / `BugbearManager` / `TurnCounter`.

**Goal:** Match desktop Valhalla/gash residual: `ascend.php` preAscension + `lastBreakfast=0`, afterlife `onAscension` from `lastBreakfast`, reincarnate `postAscension` vs `ascendAfterChoice`, per-ascension counter resets, AfterLife register/parse depth.

## Track A (7451–7470) — ValhallaManager residual

- Fix bear-arm IDs (desktop 5792/5791/5790, not 118/119/120).
- `preAscension`: gunpowder pyro DI, ItemPool usable/freepull IDs, `lastBreakfast` stays caller-owned.
- `onAscension`: `lastBreakfast=-1`, `lastGuildStoreOpen=-1`, `resetPerAscensionCounters`.
- `resetPerAscensionCounters`: `DefaultsDatabase` + `TrackManager.resetAscension` + `BanishManager.resetAscension` (keep NEVER) + `QuestDatabase.resetQuests` + `IslandWarResetSync.resetIsland` + `BugbearManager.resetStatus` + `TurnCounter.clearCounters` + `AdventureQueueDatabase.resetQueue` + spent-turns reset.
- `postAscension`: Rain/WoL/Source TurnCounters + KoE council/manor DI, autoQuest telegram DI, clan lounge DI, moonsign cafe via Canadia/Gnomads signs (not BHY/level).
- Drop `suspend` from deps so visit hooks can call them.

## Track B (7471–7490) — AfterLifeRequest residual

- Empty afterlife response → error log, false.
- `lastBreakfast != -1` → `onAscension` (not `confirmascend=1`).
- `returnskill` session-log; perm skill names; full `ascend` confirm line (type/gender/class/sign/path/karma).
- Silent vendor `place=` visits (no extra "Visiting Valhalla vendor").

## Track C (7491–7505) — Glue

- `ascend.php` + `action=ascend` → `preAscension` + `lastBreakfast=0` (stop calling `onAscension` here).
- `afterlife.php?confirmascend=1` → `postAscension`, or `ChoiceCombatAshState.ascendAfterChoice()` when redirected to `choice.php`.
- `ChoiceCombatAshState` `PostChoiceAction.ASCEND` consume → `postAscension`.

## Track D (7506–7510) — tests + wrap

- Markers Phase7451; tests Phase7510; corpus Track A–C.
- Parent wrap: `REVISION` → `phase7510`; parity-audit + AGENTS.
