# Release readiness — KoLmafia Mobile

*Updated 2026-10-04 at runtime revision `phase10090`.*

Checklist for Play internal-track and App Store preparation. This document is not a store listing upload.

## Current packaging

| Item | Status |
| ---- | ------ |
| Application id | `net.sourceforge.kolmafia.android` |
| `versionCode` / `versionName` | `1` / `0.1.0` (first internal track) |
| Release minify | Enabled with [`androidApp/proguard-rules.pro`](../androidApp/proguard-rules.pro) |
| Signing config | **Optional local** — [`keystore.properties`](../keystore.properties) (gitignored) wires `signingConfigs.release` in [`androidApp/build.gradle.kts`](../androidApp/build.gradle.kts); CI stays unsigned |
| Launcher icon | Adaptive + drawable fallback (placeholder green glyph) |
| Play 512 icon | [`docs/store-assets/play-icon-512.png`](store-assets/play-icon-512.png) |
| Play feature graphic | [`docs/store-assets/play-feature-graphic-1024x500.png`](store-assets/play-feature-graphic-1024x500.png) |
| App Store 1024 icon | [`docs/store-assets/app-icon-1024.png`](store-assets/app-icon-1024.png) |
| Phone screenshots | Offline Compose pack in [`docs/store-assets/screenshots/`](store-assets/screenshots/) (`StoreScreenshotPackTest`) — replace with device captures before publish |
| `allowBackup` | `false` until session cookies have an explicit backup schema |
| Permissions | `INTERNET` only |
| Store listing copy | [`docs/store-listing.md`](store-listing.md) + [`docs/store-listing/*.txt`](store-listing/) (title/short/full) + Data safety |
| Hostable privacy HTML | [`docs/privacy-policy.html`](privacy-policy.html) — GitHub Pages workflow [`.github/workflows/pages.yml`](../.github/workflows/pages.yml); expected URL `https://mpdecker.github.io/kolmafia-mobile/privacy-policy.html` |
| In-app privacy / source | `AppAbout.PRIVACY_POLICY_URL` + `AppAbout.SOURCE_URL` |
| CI release gates | [`.github/workflows/release-gates.yml`](../.github/workflows/release-gates.yml) — UserJourney + `assembleRelease` + `bundleRelease` |

Verify locally (Windows):

```powershell
.\gradlew.bat :androidApp:assembleDebug
.\gradlew.bat :androidApp:assembleRelease
.\gradlew.bat :androidApp:bundleRelease
.\gradlew.bat :shared:jvmTest --tests "*UserJourney*"
```

## GitHub Pages (privacy URL)

1. In the GitHub repo: **Settings → Pages → Build and deployment → Source: GitHub Actions**.
2. Merge / push to `master` or `main` (or run **Deploy privacy policy Pages** via `workflow_dispatch`).
3. Public URL: `https://mpdecker.github.io/kolmafia-mobile/privacy-policy.html`.
4. Paste that URL into Play Console and App Store Connect privacy fields (already set in-app via `AppAbout`).

## External signing (Android)

Do **not** commit keystores or passwords. Keep signing outside the repo. `keystore.properties` and `*.jks` / `*.keystore` are gitignored.

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

CI continues to verify **unsigned** `assembleRelease` and `bundleRelease` only.

## Play Console — internal track (remaining manual work)

1. Create a Play developer account and app listing for `net.sourceforge.kolmafia.android`.
2. Upload a **signed** AAB (see External signing).
3. Confirm privacy URL `https://mpdecker.github.io/kolmafia-mobile/privacy-policy.html` (Pages must be enabled once).
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

Remaining App Store blockers: Apple team membership, signed archive upload, Pages enablement if not done, production iPhone screenshots.

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
5. **GitHub Pages** — workflow live; enable Source: GitHub Actions once in repo Settings if not already.
6. **Screenshots** — phone placeholders live; replace with device captures before production listing.

## Non-goals (unchanged)

JavaScript runtime, monster modifier entity rows, ManaBurn unused-skill sweep, `git_*`/`svn_*` ASH stubs, native `user_*` dialogs.
