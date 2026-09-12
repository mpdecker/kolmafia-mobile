# HTTP Residual Mega XLVIII (6731–6790)

> **For agentic workers:** Do NOT bump `GameRuntimeLibrary.REVISION` (parent wraps to `phase6790`). Exclude Relay/JS/TCRS full derive. Prefer deepening existing hubs over new named `*Request.kt` files when a residual hub already exists.

**Goal:** Close high-ROI coinmaster `accessible()` + residual naming after ASH 100% sprint. Named desktop Request coverage is ~303/319 (remaining are base classes / Relay / alias gaps).

**Desktop:** `C:\Development\kolmafia\kolmafia\src\net\sourceforge\kolmafia\request\coinmaster\shop\`  
**Mobile tip:** `phase6730` → wrap `phase6790`

## Tracks (parallel; no REVISION bump)

### Track A — Airport coinmaster accessibility (6731–6750)
Desktop `accessible()` gates for elemental-airport shops:

| Shop id | Message / gates |
|---|---|
| `infernodisco` | hotAirportAlways / `_hotAirportToday` + limitZone That 70s Volcano |
| `glaciest` / `walmart` | coldAirport + The Glaciest |
| `landfillstore` | stenchAirport + Dinseylandfill |
| `airport` | any airport always/today |
| `si_shop1` | spookyAirport + `SHAWARMAInitiativeUnlocked` |
| `si_shop2` | spookyAirport + `canteenUnlocked` |
| `si_shop3` | spookyAirport + `armoryUnlocked` |

- Add `AirportShopAccessibility.kt` (mirror `SpringBreakBeachAccessibility`)
- Wire nicknames into `CoinmasterAccessibility.ruleFor`
- Tests: `AirportShopAccessibilityTest.kt` + `GameRuntimeLibraryPhase6750Test.kt`
- Marker: `GameRuntimeLibrary.Phase6731.kt` (comment-only OK)
- Corpus: `corpus_httpResidualXlviiiTrackA_live`

### Track B — Inventory-token shop accessibility (6751–6770)
Desktop inventory-count `accessible()` for assembly/token shops:

| Shop id | Token item id | Message |
|---|---|---|
| `toxic` | 8218 toxic globule | no toxic globule in inventory |
| `fishbones` | 7651 freshwater fishbone | no freshwater fishbone |
| `guzzlr` | 10535 Guzzlrbuck | no Guzzlrbucks |
| `warbear` | (desktop BLACKBOX) | warbear black box |
| `showerthoughts` | glob of wet paper | no wet paper |
| `fdkol` | FDKOL token | already partially token-parsed; add accessible if missing |

- Add `InventoryTokenShopAccessibility.kt`
- Wire into `CoinmasterAccessibility.ruleFor` with `accessibleCount`
- Tests: `InventoryTokenShopAccessibilityTest.kt` + `GameRuntimeLibraryPhase6770Test.kt`
- Marker: `GameRuntimeLibrary.Phase6751.kt`
- Corpus: `corpus_httpResidualXlviiiTrackB_live`

### Track C — Named alias hubs + KOLHS / Batfellow residual (6771–6790)
1. Alias objects (thin) for desktop names still missing:  
   `SHAWARMARequest`, `ArmoryRequest` (si_shop3), `LTTRequest`, `MemeShopRequest`, `NinjaStoreRequest`, `BoutiqueRequest`, `CRIMBCOGiftShopRequest`, `SushiRequest` (→ SushiCreateRequest), `TakerSpaceRequest`, `VYKEARequest`, `CoinMasterRequest`/`CoinMasterShopRequest` thin stubs (no Generic/Relay/Purchase).
2. KOLHS `accessible()`: `kolhs_shop` / `kolhs_art` / `kolhs_chem` via `lastKOLHS*ClassUnlockAdventure` + path/highschool if available.
3. Batfellow shops (`batman_chemicorp` / `batman_orphanage` / `batman_pd`) accessible residual if desktop has gates not yet wired.
4. File: `HttpResidualRequestHubsPhase6790.kt`
5. Tests: `GameRuntimeLibraryPhase6790Test.kt` + `corpus_httpResidualXlviiiTrackC_live`

## Parent wrap
- Bulk bump `"phase6730"` → `"phase6790"` (digit asserts too)
- Update `docs/parity-audit.md` Top Priorities + history + metrics
- Update `AGENTS.md` learned facts with XLVIII summary

## Non-goals
RelayRequest, full GenericRequest port, inventing non-desktop ASH, TCRS derive sweep.
