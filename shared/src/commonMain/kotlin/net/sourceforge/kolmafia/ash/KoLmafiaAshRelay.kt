package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.platform.UserDataFileIO
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.RelayRequest
import net.sourceforge.kolmafia.webui.RelayAssets

/**
 * Desktop [KoLmafiaASH.getClientHTML] — run `relay/<page>.ash` overrides.
 * JS page overrides (`relay=true`) remain a non-goal.
 */
object KoLmafiaAshRelay {
    fun getClientHTML(
        request: RelayRequest,
        library: GameRuntimeLibrary?,
        preferences: Preferences?,
    ): Boolean {
        if (library == null) return false
        val path = request.getBasePath()
        val scriptName = resolveScriptName(path, request) ?: return false
        val source = loadRelayAsh(scriptName) ?: return false
        return try {
            val runtime = AshRuntime(library)
            val nodes = AshParser().parse(source)
            runtime.execute(nodes, executeTopLevel = true)
            runtime.executeUserFunction("main", emptyList())
            val reply = runtime.output.toString()
            if (reply.isNotBlank()) {
                request.pseudoResponse("HTTP/1.1 200 OK", reply)
                true
            } else {
                // Script ran but produced no buffer — treat as handled empty OK
                request.pseudoResponse("HTTP/1.1 200 OK", "")
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun resolveScriptName(path: String, request: RelayRequest): String? {
        val base = path.trimStart('/').substringBefore('?')
        when {
            base.endsWith(".ash", ignoreCase = true) -> return base.substringAfterLast('/')
            base.startsWith("place.php") -> {
                val which = request.getFormField("whichplace") ?: return null
                return "place.$which.php.ash"
            }
            base.startsWith("shop.php") -> {
                val which = request.getFormField("whichshop") ?: return null
                return "shop.$which.php.ash"
            }
            base.endsWith(".php", ignoreCase = true) -> {
                return "$base.ash"
            }
        }
        return null
    }

    fun loadRelayAsh(scriptName: String): String? {
        val name = scriptName.removePrefix("relay/")
        UserDataFileIO.readText("relay/$name")?.let { return it }
        // Bundled afterlife etc.
        return RelayAssets.loadText(name)
    }
}
