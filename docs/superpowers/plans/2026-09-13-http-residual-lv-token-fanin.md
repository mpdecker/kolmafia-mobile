# HTTP Residual Mega LV (7151–7210)

> Tip: `phase7150` → wrap `phase7210`. No mid-track REVISION bump.
> Exclude Relay/JS/full TCRS derive. Deepen desktop-shaped APIs only.
> Coinmaster `*Request` subclasses stay consolidated (hubs + MiscShopToken / Legacy parse).

**Goal:** Close leftover inventory-token fan-in (pref-only shops), non-`shop.php` legacy deepen (Hermit/Skeleton/Altar/giftshop), and NPC/visit polish (`mayoLevel`, Chroner visit) after LIV.

## Track A (7151–7170) — MiscShopToken / Legacy inventory fan-in

Extend `MiscShopTokenResponseParse` (+ Legacy isotope) so pref balances also re-sync inventory:

- Topiary nugglet **7968** (`topiary`) — desktop `<td>N topiary nugglet`; align `NuggletCraftingRequestHub`
- Chroner **7567** — Chroner tower shops (`applestore`/`caveshop`/`nina`/`shakeshop`/`shoeshop`/`twitchsoup`/…)
- Lunar isotope **5134** — `isotope`/`elvishp*` via `LegacyCoinmasterResponseParse` + inventory
- Rubee **9838** (`fantasyrealm`), FDKOL **5707**, FunFunds **8205** — add `syncInventoryCount` to existing pref branches
- Bone chips **4743** — optional Misc fan-in or Altar deepen (Track B owns Altar)

## Track B (7171–7190) — Non-shop.php legacy deepen

- `HermitRequest` HTML `parseResponse`: tradable count + clover stock prefs; wire `hermit.php` visit hook
- `SkeletonOfCrimboPastRequest.parseResponse`: knucklebones **12051** inv sync (choice 1567)
- `AltarOfBonesRequest`: bone-chip inventory sync on `bone_altar.php`
- `TownGiftShopRequestHub`: buy session-log (`buy N X for P each from The Town Gift Shop`)

## Track C (7191–7210) — NPC/visit polish + wrap

- `NpcShopSync` mayoLevel from `blood mayonnaise concentration: N mayograms`
- Chroner visit hubs: `AppleStoreRequestHub` (+ siblings as needed) call `TimeTowerSync` + Chroner inv parse
- Markers Phase7151/7171/7191; tests Phase7170/7190/7210; corpus LV Tracks A–C
- Parent wrap: `REVISION` → `phase7210`; parity-audit + AGENTS
