package net.sourceforge.kolmafia.webui

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.platform.UserDataFileIO
import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Bundled desktop `src/relay/` assets plus optional user overrides under `relay/`.
 * Built-in text files rewrite `MAFIAHIT` → `pwd=<passwordHash>`.
 */
@OptIn(ExperimentalResourceApi::class)
object RelayAssets {
    val BUILTIN_FILES = listOf(
        "afterlife.1.ash",
        "barrel_sounds.js",
        "basement.js",
        "basics.1.css",
        "basics.js",
        "chat.html",
        "cli.html",
        "combatfilter.1.js",
        "hotkeys.js",
        "ircm_extend.4.js",
        "macrohelper.6.js",
        "onfocus.1.js",
        "stationarybuttons.2.css",
        "stationarybuttons.2.js",
        "volcanomaze.5.js",
    )

    fun isBuiltin(filename: String): Boolean =
        BUILTIN_FILES.any { it.equals(filename, ignoreCase = true) }

    fun contentTypeFor(filename: String): String {
        val lower = filename.lowercase()
        return when {
            lower.endsWith(".html") || lower.endsWith(".htm") -> "text/html; charset=UTF-8"
            lower.endsWith(".js") -> "application/javascript; charset=UTF-8"
            lower.endsWith(".css") -> "text/css; charset=UTF-8"
            lower.endsWith(".ash") -> "text/plain; charset=UTF-8"
            lower.endsWith(".json") -> "application/json; charset=UTF-8"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".gif") -> "image/gif"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            else -> "application/octet-stream"
        }
    }

    fun isTextContentType(contentType: String): Boolean =
        contentType.startsWith("text/") || contentType == "application/json" ||
            contentType.startsWith("application/javascript")

    /**
     * Load a relay file: user override first, then bundled compose resource.
     * Returns null when missing.
     */
    fun loadText(filename: String, passwordHash: String = ""): String? {
        val safe = filename.trimStart('/').substringAfterLast('/').ifBlank { filename.trimStart('/') }
        val user = UserDataFileIO.readText("relay/$safe")
        val raw = user ?: loadBundledText(safe) ?: return null
        return if (isBuiltin(safe) && raw.contains("MAFIAHIT")) {
            raw.replace("MAFIAHIT", "pwd=$passwordHash")
        } else {
            raw
        }
    }

    fun loadBytes(filename: String): ByteArray? {
        val safe = filename.trimStart('/').substringAfterLast('/').ifBlank { filename.trimStart('/') }
        UserDataFileIO.readText("relay/$safe")?.let { return it.encodeToByteArray() }
        return loadBundledBytes(safe)
    }

    private fun loadBundledText(filename: String): String? =
        loadBundledBytes(filename)?.decodeToString()

    private fun loadBundledBytes(filename: String): ByteArray? =
        runBlocking {
            try {
                Res.readBytes("files/relay/$filename")
            } catch (_: Exception) {
                null
            }
        }
}
