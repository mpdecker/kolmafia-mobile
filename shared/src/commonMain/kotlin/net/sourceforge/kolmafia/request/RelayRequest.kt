package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.ash.KoLmafiaAshRelay
import net.sourceforge.kolmafia.mall.MallSearchRelayHook
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.webui.RelayAssets
import net.sourceforge.kolmafia.webui.RelayBrowserRequest
import net.sourceforge.kolmafia.webui.RelayHttpResponse
import net.sourceforge.kolmafia.webui.RelayServer
import net.sourceforge.kolmafia.webui.RequestEditorKit

/**
 * Desktop RelayRequest — proxies KoL pages through the local Relay loopback,
 * serves bundled relay assets, and handles KoLmafia control URLs.
 */
class RelayRequest(
    private val library: GameRuntimeLibrary? = null,
    private val preferences: Preferences? = null,
    private val allowOverride: Boolean = true,
) {
    var statusLine: String = "HTTP/1.1 200 OK"
    var contentType: String = "text/html; charset=UTF-8"
    var responseText: String = ""
    var rawByteBuffer: ByteArray? = null
    val headers: MutableList<String> = mutableListOf()
    var responseCode: Int = 200

    var browserRequest: RelayBrowserRequest? = null
        private set

    /** Bind a browser request without proxying (tests / ASH resolve). */
    fun bindBrowserRequest(request: RelayBrowserRequest) {
        browserRequest = request
    }

    fun passwordHash(): String =
        preferences?.getString("pwdHash", "").orEmpty()

    fun getBasePath(): String {
        val path = browserRequest?.path.orEmpty()
        return path.substringBefore('?').trimStart('/')
    }

    fun getFormField(name: String): String? = browserRequest?.formField(name)

    fun getFormField(name: String, urlDecode: Boolean): String? =
        browserRequest?.formField(name, urlDecode)

    fun getURLString(): String = browserRequest?.pathWithQuery?.trimStart('/') ?: getBasePath()

    fun pseudoResponse(status: String, body: String) {
        statusLine = status
        responseText = body
        responseCode = status.split(' ').getOrNull(1)?.toIntOrNull() ?: 200
        rawByteBuffer = body.encodeToByteArray()
        if (status.startsWith("HTTP/1.1 302") && body.startsWith("/")) {
            headers.removeAll { it.startsWith("Location:", ignoreCase = true) }
            headers.add("Location: $body")
            responseText = ""
            rawByteBuffer = ByteArray(0)
        }
    }

    fun sendNotFound() {
        statusLine = "HTTP/1.1 404 Not Found"
        responseCode = 404
        responseText = ""
        rawByteBuffer = ByteArray(0)
    }

    fun run(request: RelayBrowserRequest): RelayHttpResponse {
        browserRequest = request
        headers.clear()
        rawByteBuffer = null
        responseText = ""
        statusLine = "HTTP/1.1 200 OK"
        responseCode = 200
        contentType = "text/html; charset=UTF-8"

        val path = getBasePath()
        if (path.startsWith("http")) {
            proxyToKol(path)
            return toHttpResponse()
        }

        if (!path.endsWith(".php", ignoreCase = true)) {
            handleSimple()
            return toHttpResponse()
        }

        if (allowOverride && KoLmafiaAshRelay.getClientHTML(this, library, preferences)) {
            return toHttpResponse()
        }

        // Special fight/choice automation paths handled by RelayAgent before run;
        // normal proxy:
        val query = request.query
        val kolPath = if (query.isNotEmpty()) "$path?$query" else path
        when (request.method.uppercase()) {
            "POST" -> {
                val body = request.body.decodeToString()
                val html = library?.visitKolPost(path, body) ?: ""
                responseText = html
            }
            else -> {
                val html = library?.visitKolPage(kolPath) ?: ""
                responseText = html
            }
        }
        formatResponse()
        return toHttpResponse()
    }

    fun formatResponse() {
        statusLine = "HTTP/1.1 200 OK"
        responseCode = 200
        var text = responseText
        val path = getBasePath()

        if (path.startsWith("mall.php", ignoreCase = true) &&
            MallSearchRelayHook.isMallSearchHtml(text)
        ) {
            val playerId = library?.character?.state?.value?.playerId ?: 0
            text = MallSearchRelayHook.maybeDecorate(
                html = text,
                preferences = preferences,
                passwordHash = passwordHash(),
                playerId = playerId,
            )
        }

        val decorated = RequestEditorKit.getFeatureRichHTML(
            getURLString(),
            text,
            preferences = preferences,
        )
        text = decorated.replace("frames.length == 0", "frames.length == -1")
        responseText = text
        rawByteBuffer = text.encodeToByteArray()
    }

    private fun handleSimple() {
        val path = getBasePath()

        if (path.startsWith("KoLmafia/", ignoreCase = true) || path.equals("KoLmafia", ignoreCase = true)) {
            handleCommand()
            return
        }

        if (path.endsWith(".ash", ignoreCase = true)) {
            if (!KoLmafiaAshRelay.getClientHTML(this, library, preferences)) {
                sendNotFound()
            }
            return
        }

        if (path.isEmpty() || path == "/") {
            pseudoResponse("HTTP/1.1 302 Found", "/game.php")
            return
        }

        // Image CDN rewrite: serve from KoL images host via redirect
        if (path.startsWith("images/", ignoreCase = true) || path.startsWith("ii/", ignoreCase = true)) {
            pseudoResponse("HTTP/1.1 302 Found", "https://www.kingdomofloathing.com/$path")
            return
        }

        sendLocalFile(path)
    }

    fun sendLocalFile(filename: String) {
        val name = filename.trimStart('/')
        contentType = RelayAssets.contentTypeFor(name)
        if (!RelayAssets.isTextContentType(contentType)) {
            val bytes = RelayAssets.loadBytes(name)
            if (bytes == null || bytes.isEmpty()) {
                sendNotFound()
                return
            }
            statusLine = "HTTP/1.1 200 OK"
            responseCode = 200
            rawByteBuffer = bytes
            responseText = ""
            return
        }
        val text = RelayAssets.loadText(name, passwordHash())
        if (text == null) {
            sendNotFound()
            return
        }
        pseudoResponse("HTTP/1.1 200 OK", text)
        contentType = RelayAssets.contentTypeFor(name)
    }

    fun handleCommand() {
        val pwd = getFormField("pwd")
        if (pwd == null || pwd != passwordHash() || passwordHash().isEmpty()) {
            statusLine = "HTTP/1.1 401 Unauthorized"
            responseCode = 401
            responseText = ""
            rawByteBuffer = ByteArray(0)
            return
        }

        val path = getBasePath()
        when {
            path.endsWith("submitCommand") -> {
                submitCommand(getFormField("cmd", false).orEmpty())
                pseudoResponse("HTTP/1.1 200 OK", "")
            }
            path.endsWith("sideCommand") -> {
                submitCommand(getFormField("cmd", false).orEmpty())
                pseudoResponse("HTTP/1.1 302 Found", "/charpane.php")
            }
            path.endsWith("redirectedCommand") || path.endsWith("polledredirectedCommand") -> {
                val cmd = getFormField("cmd").orEmpty()
                if (cmd != "wait") {
                    submitCommand(cmd)
                }
                val target = redirectedCommandURL.ifBlank { "/main.php" }
                pseudoResponse("HTTP/1.1 302 Found", target)
            }
            path.endsWith("logout") -> {
                submitCommand("logout")
                pseudoResponse("HTTP/1.1 302 Found", "/loggedout.php")
            }
            path.endsWith("messageUpdate") -> {
                pseudoResponse("HTTP/1.1 200 OK", RelayServer.getNewStatusMessages())
            }
            else -> {
                pseudoResponse("HTTP/1.1 200 OK", "")
            }
        }
    }

    private fun submitCommand(cmd: String) {
        if (cmd.isBlank()) return
        library?.runCliCommand(cmd)
    }

    private fun proxyToKol(absoluteUrl: String) {
        val stripped = absoluteUrl.removePrefix("https://www.kingdomofloathing.com/")
            .removePrefix("http://www.kingdomofloathing.com/")
        responseText = library?.visitKolPage(stripped) ?: ""
        formatResponse()
    }

    fun toHttpResponse(): RelayHttpResponse {
        val body = rawByteBuffer ?: responseText.encodeToByteArray()
        return RelayHttpResponse(
            statusLine = statusLine,
            headers = headers.toMutableList(),
            body = body,
            contentType = contentType,
        )
    }

    companion object {
        var redirectedCommandURL: String = "/main.php"

        fun builtinRelayFile(file: String): Boolean = RelayAssets.isBuiltin(file)
    }
}
