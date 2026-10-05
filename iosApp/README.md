# iOS app (KoLmafia Mobile)

Mac-only wrapper around the Kotlin Multiplatform `Shared` framework. Windows and Linux
cannot compile or run this target.

## Prerequisites

- macOS with Xcode 15+
- JDK 17+ on `PATH` (Gradle builds the framework)
- Repo root `./gradlew` executable (`chmod +x gradlew` if needed)
- Apple Developer Program membership (for device / App Store)

## Open and run

1. Open [`iosApp.xcodeproj`](iosApp.xcodeproj) in Xcode (from this `iosApp/` directory).
2. Select an iPhone simulator or device.
3. Set **Signing & Capabilities → Team** to your Apple development team (left blank in-repo).
4. Product → Run.

The **Compile Kotlin Framework** build phase runs:

```sh
cd "$SRCROOT/.."
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

Xcode then links `-framework Shared` from
`shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`.

## Bundle identity

| Setting | Value |
| ------- | ----- |
| Bundle ID | `net.sourceforge.kolmafia.ios` |
| Display name | KoLmafia |
| Version / build | `0.1.0` / `1` |
| Deployment target | iOS 15.0 |
| Privacy manifest | [`iosApp/PrivacyInfo.xcprivacy`](iosApp/PrivacyInfo.xcprivacy) |

Do **not** add `:iosApp` to Gradle `settings.gradle.kts` — Xcode owns the app; Gradle owns `:shared`.

## Archive and upload (App Store / TestFlight)

1. In Xcode, select a **Any iOS Device (arm64)** destination (not a simulator).
2. Confirm **Signing & Capabilities → Team** and automatic signing succeed.
3. Product → **Archive**. Wait for the Organizer window.
4. In Organizer → Archives → **Distribute App**:
   - **App Store Connect** for TestFlight / App Store, or
   - **Ad Hoc** / **Development** for limited device installs.
5. Follow the wizard (upload symbols optional; include bitcode N/A on modern Xcode).
6. In [App Store Connect](https://appstoreconnect.apple.com): create the app record for `net.sourceforge.kolmafia.ios` if needed, then process the build under TestFlight.
7. Paste privacy URL `https://mpdecker.github.io/kolmafia-mobile/privacy-policy.html` and source URL `https://github.com/mpdecker/kolmafia-mobile` into App Privacy / Description fields (see [`docs/store-listing.md`](../docs/store-listing.md)).

### Export compliance

Uses HTTPS / system TLS only to Kingdom of Loathing. When asked about encryption, choose the standard **HTTPS only / exempt** answers unless your legal counsel says otherwise. No proprietary crypto is shipped beyond the OS stack.

### Marketing icon

Copy [`docs/store-assets/app-icon-1024.png`](../docs/store-assets/app-icon-1024.png) into the Xcode AppIcon set (or replace all sizes) before production submit. Current in-repo icons are temporary green-triangle placeholders.

## App Store notes

- Replace placeholder AppIcon art before production submit.
- Host the privacy policy from [`docs/store-listing.md`](../docs/store-listing.md) at a public URL (GitHub Pages workflow already deploys `docs/privacy-policy.html`).
- Screenshots: capture on a current iPhone size; Android phone placeholders under `docs/store-assets/screenshots/` are not App Store–sized — use device captures for iOS slots.
