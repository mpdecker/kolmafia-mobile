# Release readiness — KoLmafia Mobile

*Updated 2026-10-05 at runtime revision `phase10510`.*

Checklist for Play internal-track and App Store preparation. This document is not a store listing upload.

## Current packaging

| Item | Status |
| ---- | ------ |
| Application id | `net.sourceforge.kolmafia.android` |
| `versionCode` / `versionName` | `1` / `0.1.0` (first internal track) |
| Release minify | Enabled with [`androidApp/proguard-rules.pro`](../androidApp/proguard-rules.pro) |
| Signing config | **Optional local** — [`keystore.properties`](../keystore.properties) (gitignored) wires `signingConfigs.release` in [`androidApp/build.gradle.kts`](../androidApp/build.gradle.kts); CI stays unsigned |
| Launcher icon | Adaptive `@mipmap/ic_launcher` + density PNGs synced from [`docs/store-assets/play-icon-512.png`](store-assets/play-icon-512.png) |
| Play 512 icon | [`docs/store-assets/play-icon-512.png`](store-assets/play-icon-512.png) |
| Play feature graphic | [`docs/store-assets/play-feature-graphic-1024x500.png`](store-assets/play-feature-graphic-1024x500.png) |
| App Store 1024 icon | [`docs/store-assets/app-icon-1024.png`](store-assets/app-icon-1024.png) (synced into `iosApp` AppIcon set) |
| Phone screenshots | Offline pack in [`docs/store-assets/screenshots/`](store-assets/screenshots/) — `StoreScreenshotPackTest` asserts 1080×1920 and can regenerate via Compose capture; replace with device captures before publish |
| `allowBackup` | `false` until session cookies have an explicit backup schema |
| Permissions | `INTERNET` only |
| Store listing copy | [`docs/store-listing.md`](store-listing.md) + [`docs/store-listing/*.txt`](store-listing/) (title/short/full/subtitle/keywords + Data safety notes) |
| Hostable privacy HTML | [`docs/privacy-policy.html`](privacy-policy.html) — GitHub Pages workflow [`.github/workflows/pages.yml`](.github/workflows/pages.yml); **live** at `https://mpdecker.github.io/kolmafia-mobile/privacy-policy.html` (HTTP 200 verified 2026-10-05; Pages `build_type=workflow` enabled via API + successful deploy run) |
| In-app privacy / source | `AppAbout.PRIVACY_POLICY_URL` + `AppAbout.SOURCE_URL` |
| CI release gates | [`.github/workflows/release-gates.yml`](../.github/workflows/release-gates.yml) — UserJourney + `assembleRelease` + `bundleRelease` |

Verify locally (Windows):

```powershell
.\gradlew.bat :androidApp:assembleDebug
.\gradlew.bat :androidApp:assembleRelease
.\gradlew.bat :androidApp:bundleRelease
.\gradlew.bat :shared:jvmTest --tests "*UserJourney*" --tests "*StoreScreenshot*"
```

Regenerate screenshot placeholders (overwrites `docs/store-assets/screenshots/01–06.png` at 1080×1920):

```powershell
.\gradlew.bat :shared:jvmTest --tests "*StoreScreenshotPackTest.capture*"
```

## GitHub Pages (privacy URL)

**Status (2026-10-05):** **Live.** Public URL returns HTTP 200:

`https://mpdecker.github.io/kolmafia-mobile/privacy-policy.html`

What was done (phase10450 release assist):

1. Confirmed prior 404 + Pages API “Not Found” (site never created).
2. Enabled Pages with `build_type=workflow` via GitHub API (no Settings UI click required).
3. Ran **Deploy privacy policy Pages** (`workflow_dispatch`); build + deploy succeeded.
4. Workflow now sets `configure-pages` `enablement: true` so a wiped Pages site can self-heal on the next run.

Re-deploy: push to `master`/`main` touching `docs/privacy-policy.html` or `.github/workflows/pages.yml`, or run **Deploy privacy policy Pages** via `workflow_dispatch`.

Paste that URL into Play Console and App Store Connect privacy fields (already set in-app via `AppAbout`).

## External signing (Android)

Do **not** commit keystores or passwords. Keep signing outside the repo. `keystore.properties` and `*.jks` / `*.keystore` are gitignored (see root [`.gitignore`](../.gitignore)).

1. Create a release keystore locally (one-time), e.g. outside the project tree.
2. Create a local `keystore.properties` at the **repo root**:

```properties
storeFile=C:\\path\\to\\release.keystore
storePassword=********
keyAlias=kolmafia
keyPassword=********
```

3. [`androidApp/build.gradle.kts`](../androidApp/build.gradle.kts) loads that file when present and attaches `signingConfigs.release` to the release build type. Without the file, release builds stay **unsigned** (CI path).
4. Produce a signed bundle: `.\gradlew.bat :androidApp:bundleRelease`.
5. Upload the signed AAB to Play internal track manually.

CI continues to verify **unsigned** `assembleRelease` and `bundleRelease` only. This prep does **not** invent or store signing credentials.

## Ordered human gates (remaining Top Priority #1)

Pages privacy hosting is **done**. Complete these in order for store binaries / listings:

1. **Android signed AAB** — local `keystore.properties` + `.\gradlew.bat :androidApp:bundleRelease` → upload signed AAB to Play internal track (see External signing).
2. **Play Console listing** — app listing for `net.sourceforge.kolmafia.android`; Data safety from `store-listing.md`; content rating; paste short/full description; upload 512 icon + feature graphic; phone screenshots (placeholders OK for internal; device captures before production).
3. **Apple Team ID + Mac archive** — set Team ID in Xcode Signing; archive / TestFlight per [`iosApp/README.md`](../iosApp/README.md).
4. **Device screenshots** — replace `docs/store-assets/screenshots/01–06.png` placeholders with physical-device captures before production listing.
5. **Device-pass checklist** — run the table below on a real device/emulator with a KoL account; mark Pass/Fail.

Do **not** automate store credentials, Play/App Store API uploads, or KoL login in CI.

## Play Console — internal track (remaining manual work)

1. Create a Play developer account and app listing for `net.sourceforge.kolmafia.android`.
2. Upload a **signed** AAB (see External signing).
3. Confirm privacy URL `https://mpdecker.github.io/kolmafia-mobile/privacy-policy.html` (**live**).
4. Complete **Data safety** using the answer sheet in `store-listing.md`.
5. Complete **content rating** questionnaire (notes in `store-listing.md`).
6. Provide phone screenshots — start from placeholders in `docs/store-assets/screenshots/` or replace with device captures; upload the 512×512 icon + feature graphic.
7. Paste short/full description from `store-listing.md`.
8. Confirm listing name / trademarks and the GPL source-offer text before production publish.

## App Store

| Item | Status |
| ---- | ------ |
| Xcode project | **Live** — [`iosApp/iosApp.xcodeproj`](../iosApp/iosApp.xcodeproj); see [`iosApp/README.md`](../iosApp/README.md) |
| Bundle id | `net.sourceforge.kolmafia.ios` |
| Privacy manifest | [`iosApp/iosApp/PrivacyInfo.xcprivacy`](../iosApp/iosApp/PrivacyInfo.xcprivacy) |
| Swift hosts | [`iosApp/iosApp/`](../iosApp/iosApp/) + KMP `MainViewController()` |
| Apple Team ID | **Unset in-repo** — set locally in Xcode Signing |
| Archive / TestFlight | Steps in [`iosApp/README.md`](../iosApp/README.md) — Mac-only |
| Marketing icon | [`docs/store-assets/app-icon-1024.png`](store-assets/app-icon-1024.png) — copy into Xcode AppIcon before publish |
| Privacy URL | **Live** — `https://mpdecker.github.io/kolmafia-mobile/privacy-policy.html` |

Remaining App Store blockers: Apple team membership, signed archive upload, production iPhone screenshots.

## Device pass checklist (manual)

Run on a physical device or emulator with a real KoL account. Mark Pass/Fail; do not automate credentials in CI.

| # | Step | Pass | Fail | Notes |
| - | ---- | ---- | ---- | ----- |
| 1 | Launch app → Login with real credentials | | | |
| 2 | Character screen shows name / stats | | | |
| 3 | Adventure → pick a zone → take one adventure | | | |
| 4 | Inventory → use one safe item (or equip) | | | |
| 5 | Chat → send one harmless message (or open channel) | | | |
| 6 | Relay → load a KoL page in the WebView | | | |
| 7 | Character → About shows revision + privacy HTTPS URL + source URL | | | |
| 8 | Drawer lists Character through Relay (ten destinations) | | | |

## Automated user journeys (offline)

[`UserJourneyTest`](../shared/src/jvmTest/kotlin/net/sourceforge/kolmafia/ui/UserJourneyTest.kt) covers:

- Login blank validation and error path (no KoL HTTP)
- Successful login → drawer shell + Character
- Inventory, Adventure, Skills, Scripts, Familiars, Chat, Shop, Mall, Relay destinations
- Adventure zone browser open/close; Mall search field input; Scripts Edit → Cancel
- Scripts editor round-trip, saved-script Run/Edit controls, and Run → console → Back
- Character effects sheet and About sheet open/dismiss (privacy + source URLs)
- Drawer lists all ten destinations in one pass

## Known release defects (fix before public track)

1. ~~**Adventure / Relay / Script Run console**~~ **closed** — Koin cycles broken; journey tests cover those destinations.
2. **Manual device pass** (not run in this prep): use the checklist above — needs a device and KoL credentials.
3. ~~**iOS Xcode project**~~ **scaffold live** — still needs Apple Team ID + Mac archive for store binary.
4. **JVM has no production Ktor engine** — Android/iOS engines only; journey tests inject `MockEngine`.
5. ~~**GitHub Pages**~~ **live** *(2026-10-05)* — privacy URL HTTP 200; workflow + API enablement recorded above.
6. **Screenshots** — phone placeholders live; replace with device captures before production listing.

## Non-goals (unchanged)

JavaScript runtime, monster modifier entity rows, ManaBurn unused-skill sweep, `git_*`/`svn_*` ASH stubs, native `user_*` dialogs.
