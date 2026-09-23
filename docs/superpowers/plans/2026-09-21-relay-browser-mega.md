# Relay Browser Mega (7631–7690)

> Tip: `phase7630` → wrap `phase7690`. No mid-track REVISION bump.
> Promote Relay from non-goal. JS runtime / Rhino remain non-goals.
> Desktop names only. Reuse shared Ktor cookies + `pwdHash`.

**Goal:** Loopback Relay proxy + RequestEditorKit decorate pipeline + in-app WebView tab.

## Track A (7631–7645) — Proxy core

- `RelayRequest` / `RelayServer` / `RelayLoader` / `RelayAssets` / `RelayBrowserRequest`
- CLI `relay` / `nobrowser` / `stop` / `status`
- Bundled `composeResources/files/relay/*`

## Track B (7646–7660) — Decorate

- `RequestEditorKit` + CharPane / TopMenu / StationaryButton / Fight + choice spoilers + basics.js/css

## Track C (7661–7675) — Special URLs + niche decorators

- `/KoLmafia/submitCommand|sideCommand|logout|messageUpdate`
- fight/choice automation; mall decorate with pwd; Basement/Mine/BeerPong/Island/Hobopolis/Valhalla

## Track D (7676–7690) — WebView + ASH + wrap

- Relay tab + expect/actual WebView (Android live; JVM/iOS stub)
- `KoLmafiaAshRelay.getClientHTML` for `relay/<page>.ash`
- UseLink v1; `REVISION` → `phase7690`; parity-audit wrap
