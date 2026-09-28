# MANUAL Concoction Edges Residual Mega (7511–7570)

> Tip: `phase7510` → wrap `phase7570`. No mid-track REVISION bump.
> Exclude Relay/JS. Deepen desktop-shaped APIs only.
> Do **not** invent a Discoveries GUI or extra CraftingMisc enum — tokens already
> live on `ConcoctionData`. Residual stays on yield + loader skip + create/recipe glue.
> Desktop `CraftingMisc.NODISCOVERY` is GUI-only; keep the parsed flag, no panel.

**Goal:** Match desktop craft-misc residual: `Concoction.getYield()` (result qty × Sauceror SX3), skip `MANUAL` registration (first non-MANUAL wins), and wire that yield through creatable/queue/pricing/makeIngredients/recipe CLI.

## Track A (7511–7530) — ConcoctionYield + parse craftYield

- `ConcoctionYield.getYield(concoction, tripleReagent)` — desktop `Concoction.getYield`:
  base = `max(resultQuantity, craftYield, 1)`; `× 3` when `tripleReagent && isTripleSauce`.
- Parse: set `craftYield = resultQty` (desktop `yield = concoction.getCount()`).
- `ConcoctionDatabase.getYield(name, tripleReagent)` — desktop `ConcoctionDatabase.getYield(itemId)`.
- `tripleReagent` = `CharacterState.isSauceror` (desktop `KoLCharacter.tripleReagent`).

## Track B (7531–7545) — MANUAL loader skip

- Desktop `ConcoctionDatabase` add: skip `CraftingMisc.MANUAL` entirely; skip if existing mixing method ≠ `NOCREATE`.
- Mobile parse: skip `MANUAL` rows; do not overwrite an existing registered recipe.
- `injectForTest` still allowed (tests/virtual lounge rows).

## Track C (7546–7560) — Glue

- `ConcoctionCreatable` / `ConcoctionQueueReserve` / `RetrievePricing` / `CreateItemIngredients` / `getAdventuresNeeded` use `ConcoctionYield.getYield`.
- Thread `tripleReagent` on creatable/queue/price contexts from `characterState.isSauceror`.
- Recipe/ingredients CLI: yield in the result name; flatten missing ingredients like desktop `RecipeCommand`.

## Track D (7561–7570) — tests + wrap

- Markers Phase7511; tests Phase7570; corpus Track A–C.
- Parent wrap: `REVISION` → `phase7570`; parity-audit + AGENTS.
- Close Top Priority #1 MANUAL concoction edges; next is ASH residual polish (optional).
