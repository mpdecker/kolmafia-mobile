package net.sourceforge.kolmafia.webui

import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.ash.currentTimeMillis
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.RelayRequest
import net.sourceforge.kolmafia.session.ActionBarManager

/**
 * Desktop [RelayServer] — loopback HTTP accept loop (ports 60080–60090 by default).
 * Platform socket I/O lives in expect/actual [RelayServerPlatform].
 */
object RelayServer {
    const val DEFAULT_MIN_PORT = 60080
    const val DEFAULT_MAX_PORT = 60090

    @Volatile
    private var listening: Boolean = false

    @Volatile
    private var port: Int = DEFAULT_MIN_PORT

    private val statusMessages = StringBuilder()
    private var lastStatusMessageMs: Long = 0
    private var updateStatus: Boolean = false

    var library: GameRuntimeLibrary? = null
    var preferences: Preferences? = null

    fun getPort(): Int = port
    fun isRunning(): Boolean = listening

    fun updateStatus() {
        updateStatus = true
    }

    fun addStatusMessage(message: String) {
        val now = currentTimeMillis()
        if (now - lastStatusMessageMs < 4000) {
            statusMessages.append(message)
        }
        lastStatusMessageMs = now
    }

    fun getNewStatusMessages(): String {
        if (updateStatus) {
            updateStatus = false
            statusMessages.append("<!-- REFRESH -->")
        }
        val out = statusMessages.toString()
        statusMessages.setLength(0)
        lastStatusMessageMs = currentTimeMillis()
        return out
    }

    fun trimPrefix(location: String): String {
        if (!location.startsWith("http://")) return location
        val pathIndex = location.indexOf('/', 7)
        if (pathIndex == -1) return location
        val colon = location.indexOf(':', 7)
        if (colon == -1 || colon > pathIndex) return location
        val p = location.substring(colon + 1, pathIndex).toIntOrNull() ?: return location
        if (p != port) return location
        return location.substring(pathIndex + 1)
    }

    /**
     * Start the relay server. Returns true when listening.
     * Prefs: `relayPort` (0 = scan 60080–60090), `relayAllowRemoteAccess`.
     */
    fun startThread(): Boolean {
        if (listening) return true
        val prefs = preferences
        prefs?.setBoolean("relayActive", true)
        val preferred = prefs?.getInt("relayPort", 0) ?: 0
        val allowRemote = prefs?.getBoolean("relayAllowRemoteAccess", false) == true
        val result = RelayServerPlatform.start(
            preferredPort = preferred,
            minPort = DEFAULT_MIN_PORT,
            maxPort = DEFAULT_MAX_PORT,
            allowRemote = allowRemote,
            onAccept = { rawRequestBytes -> handleRawRequest(rawRequestBytes) },
        )
        if (result != null) {
            port = result
            listening = true
            return true
        }
        prefs?.setBoolean("relayActive", false)
        return false
    }

    fun stop() {
        listening = false
        RelayServerPlatform.stop()
        preferences?.setBoolean("relayActive", false)
    }

    /** Process one raw HTTP request bytes → response bytes (for platform + tests). */
    fun handleRawRequest(raw: ByteArray): ByteArray {
        val text = raw.decodeToString()
        val parsed = RelayBrowserRequest.parse(text) ?: return RelayHttpResponse.notFound().toHttpBytes()
        return handleBrowserRequest(parsed).toHttpBytes()
    }

    fun handleBrowserRequest(request: RelayBrowserRequest): RelayHttpResponse {
        val path = request.path
        val action = request.formField("action")
        when {
            path.startsWith("fight.php") && action == "custom" -> {
                val html = library?.visitKolPage("fight.php?action=custom") ?: ""
                val relay = RelayRequest(library, preferences)
                relay.responseText = html
                relay.formatResponse()
                return relay.toHttpResponse()
            }
            path.startsWith("fight.php") && action == "abort" -> {
                return RelayHttpResponse.ok("<html><body>Fight aborted.</body></html>")
            }
            path.startsWith("choice.php") && action == "auto" -> {
                val html = library?.visitKolPage("choice.php") ?: ""
                val choiceId = Regex("""whichchoice["']?\s*[:=]\s*["']?(\d+)""")
                    .find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()
                if (choiceId != null) {
                    library?.runCliCommand("choice $choiceId")
                }
                val relay = RelayRequest(library, preferences)
                relay.responseText = library?.visitKolPage("choice.php") ?: html
                relay.formatResponse()
                return relay.toHttpResponse()
            }
            path.startsWith("leaflet.php") && action == "auto" -> {
                library?.runCliCommand("leaflet")
                val html = library?.visitKolPage("leaflet.php")
                    ?: "<html><body>Leaflet automation requested.</body></html>"
                val relay = RelayRequest(library, preferences)
                relay.responseText = html
                relay.formatResponse()
                return relay.toHttpResponse()
            }
            path.startsWith("actionbar.php") -> {
                val relay = RelayRequest(library, preferences)
                relay.bindBrowserRequest(request)
                if (ActionBarManager.updateJSONString(relay)) {
                    return relay.toHttpResponse()
                }
                return relay.run(request)
            }
            path.startsWith("volcanomaze.php") &&
                (request.query.contains("autostep") || request.formField("autostep") != null) -> {
                library?.runCliCommand("volcano step")
                val html = library?.visitKolPage("volcanomaze.php") ?: "false"
                val relay = RelayRequest(library, preferences)
                if (html == "false" || html.isBlank()) {
                    relay.contentType = "text/plain; charset=UTF-8"
                    relay.pseudoResponse("HTTP/1.1 200 OK", "false")
                } else {
                    relay.responseText = html
                    relay.formatResponse()
                }
                return relay.toHttpResponse()
            }
            path.equals("jsonApi", ignoreCase = true) || path.endsWith("/jsonApi") ||
                path.equals("jsonApi.php", ignoreCase = true) -> {
                val body = when {
                    request.method.equals("POST", ignoreCase = true) ->
                        request.body.decodeToString().ifBlank {
                            request.formField("body").orEmpty()
                        }
                    else -> request.formField("body").orEmpty()
                }
                val (code, text) = RelayJsonApi.handle(body, library, preferences)
                return RelayHttpResponse(
                    statusLine = if (code == 200) "HTTP/1.1 200 OK" else "HTTP/1.1 $code Error",
                    headers = mutableListOf(),
                    body = text.encodeToByteArray(),
                    contentType = "application/json; charset=UTF-8",
                )
            }
        }
        val relay = RelayRequest(library, preferences)
        return relay.run(request)
    }
}

/** Platform loopback socket — ServerSocket on JVM/Android; stub on iOS until Darwin CIO. */
expect object RelayServerPlatform {
    /**
     * Bind and start accept loop. Returns bound port or null on failure.
     * [onAccept] receives raw HTTP request bytes and must return raw HTTP response bytes.
     */
    fun start(
        preferredPort: Int,
        minPort: Int,
        maxPort: Int,
        allowRemote: Boolean,
        onAccept: (ByteArray) -> ByteArray,
    ): Int?

    fun stop()
}
