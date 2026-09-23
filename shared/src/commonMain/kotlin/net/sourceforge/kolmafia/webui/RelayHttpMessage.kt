package net.sourceforge.kolmafia.webui

/**
 * Parsed browser HTTP request / response for the local Relay loopback server.
 */
data class RelayBrowserRequest(
    val method: String,
    val pathWithQuery: String,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray = ByteArray(0),
) {
    val path: String
        get() {
            val raw = pathWithQuery.substringBefore('?').trimStart('/')
            return raw
        }

    val query: String
        get() = pathWithQuery.substringAfter('?', missingDelimiterValue = "")

    fun formField(name: String): String? {
        val fromQuery = queryParams()[name]
        if (fromQuery != null) return fromQuery
        if (body.isEmpty()) return null
        return formParamsFromBody()[name]
    }

    fun formField(name: String, urlDecode: Boolean): String? {
        val value = formField(name) ?: return null
        return if (urlDecode) urlDecode(value) else value
    }

    fun queryParams(): Map<String, String> = parseForm(query)

    fun formParamsFromBody(): Map<String, String> {
        val contentType = headers.entries.firstOrNull { it.key.equals("Content-Type", true) }?.value.orEmpty()
        if (!contentType.contains("application/x-www-form-urlencoded", ignoreCase = true) &&
            body.isNotEmpty() && !contentType.contains("multipart", ignoreCase = true)
        ) {
            // KoL often posts urlencoded without always declaring; still try.
        }
        return parseForm(body.decodeToString())
    }

    fun allFormFields(): Map<String, String> = queryParams() + formParamsFromBody()

    companion object {
        fun parse(raw: String): RelayBrowserRequest? {
            val lines = raw.replace("\r\n", "\n").split('\n')
            if (lines.isEmpty()) return null
            val requestLine = lines[0].trim()
            val parts = requestLine.split(' ')
            if (parts.size < 2) return null
            val method = parts[0].uppercase()
            val pathWithQuery = parts[1]
            val headers = linkedMapOf<String, String>()
            var i = 1
            while (i < lines.size) {
                val line = lines[i]
                if (line.isEmpty()) {
                    i++
                    break
                }
                val colon = line.indexOf(':')
                if (colon > 0) {
                    headers[line.substring(0, colon).trim()] = line.substring(colon + 1).trim()
                }
                i++
            }
            val bodyText = if (i < lines.size) lines.drop(i).joinToString("\n") else ""
            return RelayBrowserRequest(method, pathWithQuery, headers, bodyText.encodeToByteArray())
        }

        fun parseForm(encoded: String): Map<String, String> {
            if (encoded.isBlank()) return emptyMap()
            val out = linkedMapOf<String, String>()
            encoded.split('&').forEach { pair ->
                if (pair.isBlank()) return@forEach
                val eq = pair.indexOf('=')
                if (eq < 0) {
                    out[urlDecode(pair)] = ""
                } else {
                    out[urlDecode(pair.substring(0, eq))] = urlDecode(pair.substring(eq + 1))
                }
            }
            return out
        }

        fun urlDecode(value: String): String {
            val sb = StringBuilder()
            var i = 0
            while (i < value.length) {
                when (val c = value[i]) {
                    '+' -> {
                        sb.append(' ')
                        i++
                    }
                    '%' -> {
                        if (i + 2 < value.length) {
                            val hex = value.substring(i + 1, i + 3)
                            val code = hex.toIntOrNull(16)
                            if (code != null) {
                                sb.append(code.toChar())
                                i += 3
                            } else {
                                sb.append(c)
                                i++
                            }
                        } else {
                            sb.append(c)
                            i++
                        }
                    }
                    else -> {
                        sb.append(c)
                        i++
                    }
                }
            }
            return sb.toString()
        }

        fun urlEncode(value: String): String = buildString {
            for (c in value) {
                when {
                    c.isLetterOrDigit() || c in "-_.~" -> append(c)
                    c == ' ' -> append('+')
                    else -> append('%').append(c.code.toString(16).uppercase().padStart(2, '0'))
                }
            }
        }
    }
}

data class RelayHttpResponse(
    val statusLine: String = "HTTP/1.1 200 OK",
    val headers: MutableList<String> = mutableListOf(),
    val body: ByteArray = ByteArray(0),
    val contentType: String = "text/html; charset=UTF-8",
) {
    val responseCode: Int
        get() = statusLine.split(' ').getOrNull(1)?.toIntOrNull() ?: 200

    val bodyText: String
        get() = body.decodeToString()

    fun toHttpBytes(): ByteArray {
        val headerBlock = buildString {
            append(statusLine).append("\r\n")
            if (headers.none { it.startsWith("Content-Type:", ignoreCase = true) }) {
                append("Content-Type: ").append(contentType).append("\r\n")
            }
            if (headers.none { it.startsWith("Content-Length:", ignoreCase = true) }) {
                append("Content-Length: ").append(body.size).append("\r\n")
            }
            headers.forEach { append(it).append("\r\n") }
            append("Connection: close\r\n")
            append("\r\n")
        }.encodeToByteArray()
        return headerBlock + body
    }

    companion object {
        fun ok(html: String, contentType: String = "text/html; charset=UTF-8") =
            RelayHttpResponse(
                statusLine = "HTTP/1.1 200 OK",
                body = html.encodeToByteArray(),
                contentType = contentType,
            )

        fun notFound() = RelayHttpResponse(statusLine = "HTTP/1.1 404 Not Found", body = ByteArray(0))

        fun unauthorized() = RelayHttpResponse(statusLine = "HTTP/1.1 401 Unauthorized", body = ByteArray(0))

        fun redirect(location: String) = RelayHttpResponse(
            statusLine = "HTTP/1.1 302 Found",
            headers = mutableListOf("Location: $location"),
            body = ByteArray(0),
        )

        fun notModified() = RelayHttpResponse(statusLine = "HTTP/1.1 304 Not Modified", body = ByteArray(0))
    }
}
