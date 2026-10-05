package net.sourceforge.kolmafia.ui

import net.sourceforge.kolmafia.ash.GameRuntimeLibrary

/**
 * In-app About copy and store-facing links.
 * Privacy HTML is published by [.github/workflows/pages.yml] after Pages is enabled
 * (Settings → Pages → Source: GitHub Actions).
 */
object AppAbout {
    const val APP_NAME = "KoLmafia Mobile"

    /** Public HTTPS privacy-policy URL (GitHub Pages). */
    const val PRIVACY_POLICY_URL =
        "https://mpdecker.github.io/kolmafia-mobile/privacy-policy.html"

    /** Public source / GPL offer URL for store About fields. */
    const val SOURCE_URL = "https://github.com/mpdecker/kolmafia-mobile"

    const val GPL_BLURB =
        "KoLmafia Mobile is free software under the GNU General Public License. " +
            "Unofficial fan client — not affiliated with Asymmetric Publications or Kingdom of Loathing."

    fun revisionLine(): String = "Revision ${GameRuntimeLibrary.REVISION}"

    fun privacyPolicyDisplay(): String =
        PRIVACY_POLICY_URL.ifBlank { "docs/privacy-policy.html (host for store URL)" }

    fun sourceDisplay(): String = SOURCE_URL
}
