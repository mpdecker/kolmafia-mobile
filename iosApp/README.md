# iOS app (KoLmafia Mobile)

Mac-only wrapper around the Kotlin Multiplatform `Shared` framework. Windows and Linux
cannot compile or run this target.

## Prerequisites

- macOS with Xcode 15+
- JDK 17+ on `PATH` (Gradle builds the framework)
- Repo root `./gradlew` executable (`chmod +x gradlew` if needed)

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

## App Store notes

- Replace placeholder AppIcon art before production submit.
- Host the privacy policy from [`docs/store-listing.md`](../docs/store-listing.md) at a public URL.
- Export compliance: app uses HTTPS only to Kingdom of Loathing; no proprietary crypto beyond system TLS.
