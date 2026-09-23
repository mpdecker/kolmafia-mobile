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

    /**
     * Desktop [ActionBarManager.updateJSONString] — relay actionbar.php fetch/set.
     * Returns true when the request was fully handled as a pseudo-response.
     */
    fun updateJSONString(request: net.sourceforge.kolmafia.request.RelayRequest): Boolean {
        val action = request.getFormField("action")
        if (action != null && action.equals("fetch", ignoreCase = true)) {
            request.contentType = "application/json; charset=UTF-8"
            request.pseudoResponse("HTTP/1.1 200 OK", currentJson.ifBlank { "{}" })
            return true
        }
        val bar = request.getFormField("bar")
        if (bar != null) {
            currentJson = bar
        }
        // Non-fetch: fall through to KoL proxy after caching locally.
        return false
    }
}
