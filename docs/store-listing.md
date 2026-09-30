# Store listing pack — KoLmafia Mobile

*Updated 2026-09-29 at runtime revision `phase10030`.*

Copy suitable for Play Console / App Store Connect and for hosting a public privacy-policy URL.
This is not an uploaded listing.

## Draft privacy policy

**KoLmafia Mobile Privacy Policy**

Effective date: 2026-09-29

KoLmafia Mobile (“the App”) is an unofficial client for the free browser game Kingdom of Loathing. The App is provided as open-source software under the GNU General Public License.

### What we collect

The App does **not** operate its own backend and does **not** include advertising or analytics SDKs.

When you log in, the App sends your Kingdom of Loathing **username and password** over HTTPS to Asymmetric Publications’ servers at `https://www.kingdomofloathing.com` (and related KoL hosts) so you can play. Session cookies and game state returned by those servers are stored on your device for the session.

If you use in-game chat, messages you send are **user-generated content** transmitted to Kingdom of Loathing chat services under that game’s rules.

### What we do not collect

- No account with the App publisher separate from Kingdom of Loathing
- No advertising identifiers
- No crash/analytics SDKs bundled at this revision
- No sale of personal data

### Data retention

Credentials and session data remain on your device (and with Kingdom of Loathing, per that game’s policies). Uninstalling the App removes local App storage. `allowBackup` is disabled on Android until an explicit backup schema exists for session cookies.

### Children’s privacy

Kingdom of Loathing and this App are intended for players who meet that game’s age requirements. The App does not knowingly collect data from children independently of KoL.

### Contact / source

Source code and issue tracking: the project repository for KoLmafia Mobile.  
Game operator privacy practices: refer to Kingdom of Loathing / Asymmetric Publications.

---

Host this section at a stable HTTPS URL and paste that URL into Play Console and App Store Connect.

**Hostable HTML:** [`docs/privacy-policy.html`](privacy-policy.html) is published by [`.github/workflows/pages.yml`](../.github/workflows/pages.yml). Enable **Settings → Pages → Source: GitHub Actions** once. Expected URL:

`https://mpdecker.github.io/kolmafia-mobile/privacy-policy.html`

[`AppAbout.PRIVACY_POLICY_URL`](../shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/AppAbout.kt) is already set to that URL.

## Play Data safety — answer sheet

| Question | Answer |
| -------- | ------ |
| Does the app collect user data? | **Yes** — account credentials in transit to KoL; optional chat messages |
| Collected data types | Account credentials (username/password); User-generated content (chat) |
| Data is collected | Ephemeral / transmitted for app functionality |
| Data is shared with third parties? | Credentials and chat go to **Kingdom of Loathing** (required for gameplay), not to advertisers |
| Data encrypted in transit? | **Yes** (HTTPS) |
| Users can request deletion? | Local: uninstall / clear app data. KoL account: via KoL/Asymmetric processes |
| Committed to Play Families? | No |
| Advertising ID used? | No |
| Independent security review? | No (disclose honestly) |

## Content rating — questionnaire notes

Expect an **Everyone** or mild teen rating depending on Kingdom of Loathing content (cartoon violence, mild language, simulated gambling-style game mechanics in places). Answer questionnaires based on **KoL game content the client can display**, not on ads (none). No user-to-user offline meetings facilitated by the App beyond KoL’s own chat.

## GPL source-offer blurb (listing / about)

> KoLmafia Mobile is free software licensed under the GNU General Public License. The complete corresponding source code is available in the project’s public Git repository. This App is an unofficial fan client and is not affiliated with or endorsed by Asymmetric Publications or Kingdom of Loathing.

Include a link to the repository LICENSE and source tree in the store “About” / “Privacy policy” area as required by distribution policy.

## Screenshot checklist

Capture on a current phone size (and tablet if listing tablet support):

1. Login screen (`KoLmafia Mobile` title)
2. Character / stats after login
3. Adventure destination
4. Inventory
5. Scripts list
6. Relay browser (optional but distinctive)
7. Navigation drawer showing all destinations

Compose destinations under the logged-in drawer: Character, Adventure, Inventory, Skills, Scripts, Familiars, Chat, Shop, Mall, Relay.

Replace the placeholder launcher / App Store 1024×1024 icon before production publish if you prefer a production design (Android adaptive + iOS `AppIcon` currently use a temporary green triangle glyph). In-repo assets:

| Asset | Path | Use |
| ----- | ---- | --- |
| Play high-res icon 512×512 | [`docs/store-assets/play-icon-512.png`](store-assets/play-icon-512.png) | Play Console high-res icon |
| Play feature graphic 1024×500 | [`docs/store-assets/play-feature-graphic-1024x500.png`](store-assets/play-feature-graphic-1024x500.png) | Play Console feature graphic |
| App Store / marketing 1024×1024 | [`docs/store-assets/app-icon-1024.png`](store-assets/app-icon-1024.png) | Copy into Xcode AppIcon / App Store Connect |

All three share the same green-triangle family.

## Bundle identifiers

| Platform | Application / bundle id |
| -------- | ----------------------- |
| Android | `net.sourceforge.kolmafia.android` |
| iOS | `net.sourceforge.kolmafia.ios` |
| Version | `0.1.0` (versionCode / CFBundleVersion `1`) |
