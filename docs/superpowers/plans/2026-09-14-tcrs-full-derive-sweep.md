# TCRS Full Derive Sweep (7211–7270)

> Tip: `phase7210` → wrap `phase7270`. No mid-track REVISION bump.
> Deepen desktop-shaped `TCRSDatabase.derive*` / CLI. Do not invent dump formats.
> Relay/JS remain non-goals.

**Goal:** Port desktop Two Crazy Random Summer PRNG derivation so `tcrs derive` (no-arg) builds class/sign item + cafe maps, matching `TCRSDatabase.java`.

**Architecture:** Bundle `tcrs.txt` string tables; PHP MT/rand streams already live. New `TCRSDerive` implements `deriveItem` / `deriveCafe` / `derive(class,sign)` / `update` / `introspect` on existing `TCRSDatabase` maps. HTML `TCRSDeriver` stays the unknown/live-item introspect path.

**Tech Stack:** Kotlin Multiplatform commonMain, bundled composeResources, `PHPMTRandom` / `PHPRandom` / `PHPRandomSelection`.

## Global Constraints

- Desktop-shaped APIs only; no non-desktop dump filenames.
- Headless sequential desc visits for introspect; no Relay.
- Coinmaster `*Request` subclasses stay consolidated (out of scope).
- Tests: `.\gradlew.bat :shared:jvmTest`

## Track A (7211–7230) — tables, RNG, router, generic

- Copy desktop `tcrs.txt`; load ordered Color/Cosmetic/Potion/Food/Booze/Spleen/Equipment/Adjective lists
- `PHPRandom.array` + `PHPRandomSelection`
- `seedFor` / cosmetics / `unalteredModifiers` / `getRetainedModifiers`
- `deriveGeneric` + `deriveItem` router (`NOT_RE_ROLLED`, Display Name, unknown→introspect, GLITCH_ITEM)

## Track B (7231–7250) — consumables + cafe

- `TCRSEffectPool` (GOOD, hookah/Fishy, !notcrs, id ≤ Tiki Temerity)
- `derivePotion` / `deriveFoodBooze` / `deriveSpleen` / `deriveCafe`
- Quality/size rolls, hardcoded effect overrides, beverage/zero-size specials

## Track C (7251–7270) — equipment, sweep, CLI wrap

- `enchantCount` + `deriveEquipment` (seed+10 stream, cosmetic shuffle)
- `derive(class,sign)` all real items + cafe maps; `update` / `introspect`
- CLI: no-arg `tcrs derive`, `tcrs introspect [#]`, `tcrs update`
- Markers + golden potion names from desktop `TCRSDatabaseTest`
- Parent wrap: `REVISION` → `phase7270`; parity-audit + AGENTS
