# Residual HTTP Request Parity Mega Design

## Scope

Implement the next parity mega, Phases 3951–4010, for the remaining named HTTP
request gaps. The work covers Hashing Vise, Potted Tea Tree, Umbrella, KGB,
Foresee, Palm Frond/MUSE compatibility, Pizza Cube, Flea Market buy/sell, and
Ascension History. Existing partial implementations are extended rather than
duplicated.

Live mutations are required for the item-use, choice, equipment, campground,
and Flea Market actions. Ascension History is read-only. GUI/Relay behavior,
JavaScript, full TCRS class/sign dump generation, and desktop scripting remain
explicit non-goals.

## Architecture

Typed request classes own endpoint construction, validation, response parsing,
success-only local accounting, and session-log registration. Existing managers
and synchronizers remain the state boundary. `GameRuntimeLibrary.*` files only
resolve dependencies, parse CLI arguments, invoke typed requests, and print
results.

The shared response pipeline remains authoritative: preserve final response
URLs through redirects, route relevant pages and choices through
`processVisitResponseHooks`, and make inventory, meat, preference, quest, and
modifier updates idempotent. DI registrations provide test seams for every new
request. Failed or malformed responses must not consume items, mark daily uses,
or apply local gains.

## Tracks

### Phases 3951–3965: request substrate and routing

Centralize request result routing and redirect-safe URL handling. Add explicit
visit/request logging and response-hook routes for Tea Tree choices 1104/1105,
Umbrella choice 1466, Hashing Vise choice 1551, Foresee choice 1558, KGB
place actions, Flea Market endpoints, and Ascension History. Correct stale
choice-to-command mappings and prevent duplicate state application.

### Phases 3966–3980: IoTM actions

Add typed Hashing Vise, Potted Tea Tree, and Foresee requests; complete
Umbrella mode validation through the existing modeable path; and add typed KGB
state/action handling. Preserve ownership, equipment, daily-use, and choice
validation. Migrate current CLI adapters to these requests while retaining
their command syntax.

### Phases 3981–3995: MUSE, campground, and Flea Market

Extend Palm Frond/MUSE response and session accounting without creating a
second crafting engine. Add Pizza Cube creation and result accounting. Add
live Flea Market purchase and sale requests with success-only inventory and
meat deltas, tolerant HTML parsing, and request logging. Exclude general mall
and store-screen parity.

### Phases 3996–4010: Ascension History and closure

Add a typed read-only Ascension History request and historical row model.
Support current-player and optional player-targeted retrieval, tolerant class,
path, turn, and challenge-point parsing, and unknown-value preservation.
Finish DI/CLI/help reconciliation, regression coverage, metrics, revision, and
parity documentation.

## Error handling and safety

- Validate ownership, item IDs, choice parameters, daily limits, and required
  equipment before live mutation.
- Treat non-success HTTP responses, redirect failures, malformed HTML, and
  rejected game responses as failures.
- Apply local inventory, meat, preference, quest, and modifier changes only
  after a successful response and only once per response signature.
- Preserve item/use state on failure; never retry a destructive mutation
  automatically.
- Keep read-only status and history commands usable when HTTP is unavailable
  by reporting cached state or a clear unavailable result.

## Verification

Add focused common tests for endpoint forms, response fixtures, malformed
responses, idempotent synchronization, daily-use and ownership guards, CLI
adapters, and DI registration. Run the complete shared JVM suite and Android
debug build. Update `GameRuntimeLibrary.REVISION` to `phase4010`, refresh
metrics, and add a Phase History entry to `docs/parity-audit.md` and
`AGENTS.md`.
