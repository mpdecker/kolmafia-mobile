# Clan Dungeon Residual Mega (7391–7450)

> Tip: `phase7390` → wrap `phase7450`. No mid-track REVISION bump.
> Exclude Relay/JS. Deepen desktop-shaped APIs only.
> Desktop has **no headless Hobopolis/Slime Tube zone SM** — do not invent one.
> Residual stays on `HobopolisManager` / `SlimeStackManager` / `SlimeTubeManager` /
> `RichardRequest` / `DreadsylvaniaRequest` / `QuestChoiceRules` / `CurrentModifiers`.

**Goal:** Match desktop clan-dungeon residual behavior: HobopolisDecorator Town Square + sewer exploration + boss waits, Richard gym session-log, Slimeling stack/fullness accounting, Slime Tube hatred ML + Mother Slime wait, Dreadsylvania feedbooze consume, sewer `requireSewerTestItems` prepare.

## Track A (7391–7410) — Hobopolis residual

- `HobopolisManager.hobopolisBossName` / `bossWaitMessage` (choices 200–205, 518; decision 2).
- `HobopolisManager.checkDungeonSewers` (choices 197–199; decision 1) — code/item/grate explorations + consume + `requireSewerTestItems`.
- `QuestChoiceRules` + `AdventureManager` consume pending stop.
- `RichardRequest` gym names: grenades/shakes; session-log `(N turns)`.
- Headless `tireKills(tires)` from decorator formula (no Relay HTML).

## Track B (7411–7430) — Slime Tube / Slimeling

- `SlimeStackManager.getSlimeStackTurns` / `recordFeed` / `recordFightLeaps`.
- `ResultProcessor` slime-stack drop (`4137`) + due resync.
- `FightFamiliarMessageSync` `"leaps on your opponent"` fullness −1.
- `familiarFeedItem` SLIMELING feed accounting (autoplunger/meat-stack vs power/10).
- `CurrentModifiers` Slime Hatred ML when `lastAdventure` is The Slime Tube.
- `SlimeTubeManager.motherSlimeWait` (choice 326 decision 2).

## Track C (7431–7445) — Dreadsylvania + sewer prep

- `DreadsylvaniaRequest.parseResponse` feedbooze → `ResultProcessor.processItem`.
- `AdventurePrepareActions` `requireSewerTestItems` gate for A Maze of Sewer Tunnels.
- ItemPool sewer constants (3220–3230, 4137, 127).

## Track D (7446–7450) — tests + wrap

- Markers Phase7391/7411/7431; tests Phase7410/7430/7450; corpus Tracks A–C.
- Parent wrap: `REVISION` → `phase7450`; parity-audit + AGENTS.
