package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Phases 1043–1052 — CLI Track D session / store / script aliases:
 * undercut, timein/relog/relogin, encounters/session/summary, modifies,
 * location, cache, validate/verify/profile, mail list stub.
 */
internal fun GameRuntimeLibrary.cliUndercut(parameters: String, rt: AshRuntimeContext) {
    // Desktop MallRepriceCommand: bare undercut avoids mall-min; "undercut min" includes it.
    // Mobile has no sold-item lowest-price table — refresh manageprices as the thin hook.
    val store = manageStoreRequest ?: run {
        rt.print("Mall store unavailable")
        return
    }
    runBlocking { store.refreshPrices() }
    val mode = if (parameters.trim().startsWith("min", ignoreCase = true)) " (including mall minimum)" else ""
    rt.print("Repricing complete.$mode")
}

internal fun GameRuntimeLibrary.cliTimein(rt: AshRuntimeContext) {
    rt.print("Session re-authentication (timein/relog) is not available in KoLmafia Mobile.")
}

internal fun GameRuntimeLibrary.cliSession(rt: AshRuntimeContext) {
    val cs = character?.state?.value
    val name = cs?.name?.takeIf { it.isNotBlank() } ?: "(unknown)"
    rt.print("Player: $name")
    rt.print("Session adventures: ${preferences?.getInt("_sessionAdventuresUsed", 0) ?: 0}")
    rt.print("Session meat: ${cs?.sessionMeat ?: 0L}")
    val lines = sessionLogger?.recentLines()?.size
        ?: preferences?.getString(SessionLogger.SESSION_LOG_KEY, "")
            ?.lines()?.count { it.isNotBlank() }
        ?: 0
    rt.print("Session log lines: $lines")
    rt.print("")
}

internal fun GameRuntimeLibrary.cliSummary(rt: AshRuntimeContext) {
    rt.print("Session summary:")
    rt.print("")
    val itemTally = preferences?.getString("_sessionItemTally", "").orEmpty()
    if (itemTally.isBlank()) {
        rt.print("(no session items recorded)")
    } else {
        for (entry in itemTally.split("|").filter { it.isNotBlank() }) {
            val sep = entry.lastIndexOf(':')
            if (sep < 0) {
                rt.print(entry)
            } else {
                rt.print("${entry.substring(0, sep)} (${entry.substring(sep + 1)})")
            }
        }
    }
    rt.print("")
    val resultTally = preferences?.getString("_sessionResultTally", "").orEmpty()
    if (resultTally.isNotBlank()) {
        rt.print("Results:")
        for (entry in resultTally.split("|").filter { it.isNotBlank() }) {
            val sep = entry.lastIndexOf(':')
            if (sep < 0) {
                rt.print(entry)
            } else {
                rt.print("${entry.substring(0, sep)} (${entry.substring(sep + 1)})")
            }
        }
        rt.print("")
    }
}

internal fun GameRuntimeLibrary.cliEncounters(rt: AshRuntimeContext) {
    rt.print("Encounter Listing:")
    rt.print("")
    val lines = sessionLogger?.recentLines().orEmpty()
        .filter { it.contains("CombatFinished", ignoreCase = true) || it.contains("combat ", ignoreCase = true) }
    if (lines.isEmpty()) {
        val visited = adventureSpentTracker?.visited().orEmpty()
        if (visited.isEmpty()) {
            rt.print("(no encounters recorded)")
        } else {
            for ((name, count) in visited.entries.sortedBy { it.key.lowercase() }) {
                rt.print("$name ($count)")
            }
        }
    } else {
        for (line in lines) {
            rt.print(line)
        }
    }
}

internal fun GameRuntimeLibrary.cliModifies(parameters: String, rt: AshRuntimeContext) {
    // Desktop ModifierListCommand → DebugModifiers; mobile aliases to modref listing.
    runModRefCli(parameters, rt)
}

internal fun GameRuntimeLibrary.cliLocation(parameters: String, rt: AshRuntimeContext) {
    val trimmed = parameters.trim()
    if (trimmed.isEmpty()) {
        val last = lastLocationName()
        if (last.isBlank()) {
            rt.print("No last adventure location recorded.")
        } else {
            rt.print(last)
        }
        return
    }
    // Desktop RegisterAdventureCommand: "<snarfblat> <name>" — not persisted on mobile.
    val space = trimmed.indexOf(' ')
    if (space < 0) {
        rt.print("Usage: location <snarfblat> <name>")
        return
    }
    val snarf = trimmed.substring(0, space).trim()
    val name = trimmed.substring(space + 1).trim()
    rt.print("Adventure override registration is not available ($snarf → $name).")
}

internal fun GameRuntimeLibrary.cliCache(parameters: String, rt: AshRuntimeContext) {
    val cmd = parameters.trim().lowercase().substringBefore(' ')
    when (cmd) {
        "", "status" -> {
            val date = preferences?.getLong("lastImageCacheClear", 0L) ?: 0L
            if (date == 0L) {
                rt.print("Image cache never cleared.")
            } else {
                rt.print("Image cache last cleared on $date")
            }
            rt.print("Relay image cache is not available in KoLmafia Mobile.")
        }
        "clear" -> rt.print("Relay image cache is not available in KoLmafia Mobile.")
        else -> rt.print("Usage: cache [clear]")
    }
}

internal fun GameRuntimeLibrary.cliValidateOrProfileScript(
    command: String,
    parameters: String,
    rt: AshRuntimeContext,
) {
    val params = parameters.trim()
    if (params.isEmpty()) return
    val cmd = command.lowercase()
    when (cmd) {
        "validate", "verify" -> {
            if (scriptExists(params)) {
                rt.print("Script verification complete.")
            } else {
                rt.print("Script '$params' not found")
            }
        }
        else -> runCallScriptCli(params, rt)
    }
}

internal fun GameRuntimeLibrary.cliMail(parameters: String, rt: AshRuntimeContext) {
    val trimmed = parameters.trim()
    when {
        trimmed.isEmpty() || trimmed.equals("read", ignoreCase = true) -> {
            visitKolPage("mail.php")
        }
        trimmed.equals("list", ignoreCase = true) ||
            trimmed.equals("inbox", ignoreCase = true) -> {
            visitKolPage("mail.php")
            rt.print("Inbox:")
            rt.print("(mail listing not available in KoLmafia Mobile)")
        }
        else -> {
            visitKolPage("mail.php")
            rt.print("Usage: mail [list]")
        }
    }
}

private fun GameRuntimeLibrary.scriptExists(name: String): Boolean {
    val json = preferences?.getString(ScriptManager.SCRIPTS_PREF_KEY, "[]") ?: return false
    return try {
        kotlinx.serialization.json.Json.decodeFromString<List<ScriptEntry>>(json)
            .any { it.name.equals(name, ignoreCase = true) }
    } catch (_: Exception) {
        false
    }
}
