package net.sourceforge.kolmafia.session

/**
 * Headless cache for KoL's action bar JSON.
 *
 * Relay/UI presentation is intentionally outside the mobile port; this keeps
 * the server-backed state available to requests and scripts.
 */
object ActionBarManager {
    private var initialJson: String = ""
    private var currentJson: String = ""

    fun current(): String = currentJson

    fun update(json: String) {
        currentJson = json
        if (initialJson.isBlank()) initialJson = json
    }

    fun reset() {
        initialJson = ""
        currentJson = ""
    }
}
