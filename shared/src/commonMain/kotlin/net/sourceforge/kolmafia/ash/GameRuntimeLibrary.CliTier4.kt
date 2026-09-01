package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.session.ClanCliManager
import net.sourceforge.kolmafia.session.DadCliState
import net.sourceforge.kolmafia.session.SlimeStackManager

/** Phases 3891–3920 — CLI inventory and local state reporters. */

internal fun GameRuntimeLibrary.cliDad(print: (String) -> Unit) {
    DadCliState.report().forEach(print)
}

internal fun GameRuntimeLibrary.cliSlimeStack(print: (String) -> Unit) {
    val prefs = preferences
    if (prefs == null) {
        print("Preferences unavailable.")
        return
    }
    print(SlimeStackManager.status(prefs))
}

internal suspend fun GameRuntimeLibrary.cliClan(
    parameters: String,
    print: (String) -> Unit,
) {
    val command = parameters.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    val manager = clanCliManager
    when (command.firstOrNull()?.lowercase()) {
        null, "status" -> {
            (manager?.statusLines() ?: ClanCliManager().statusLines()).forEach(print)
        }
        "refresh", "visit" -> {
            if (manager == null) {
                print("Clan refresh is unavailable.")
                return
            }
            val result = manager.refresh()
            print(
                "Clan refresh: " +
                    listOf(
                        "members=${result.members}",
                        "ranks=${result.ranks}",
                        "stash=${result.stash}",
                        "log=${result.log}",
                    ).joinToString(", "),
            )
            manager.statusLines().forEach(print)
        }
        "snapshot" -> {
            if (manager == null) print("Clan snapshot is unavailable.")
            else manager.snapshot().forEach(print)
        }
        "stashlog", "stash-log" -> {
            if (manager == null) print("Clan stash log is unavailable.")
            else manager.stashLog().forEach(print)
        }
        else -> print("Usage: clan [status|refresh|snapshot|stashlog]")
    }
}

internal suspend fun GameRuntimeLibrary.cliTcrs(
    parameters: String,
    print: (String) -> Unit,
) {
    val manager = tcrsCliManager
    if (manager == null) {
        print("TCRS operations are unavailable.")
        return
    }
    val tokens = parameters.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    when (tokens.firstOrNull()?.lowercase()) {
        null, "status" -> manager.status().forEach(print)
        "load" -> print(manager.load())
        "save" -> print(manager.save())
        "fetch" -> manager.fetch().forEach(print)
        "apply" -> print(manager.apply())
        "reset" -> print(manager.reset())
        "derive" -> print(manager.derive(tokens.getOrNull(1)?.toIntOrNull()))
        "check" -> {
            val itemId = tokens.getOrNull(1)?.toIntOrNull()
            if (itemId == null) print("Usage: tcrs check <item id>")
            else print(manager.check(itemId))
        }
        "help" -> {
            print("Usage: tcrs [status|load|save|fetch|derive [item id]|check <item id>|apply|reset]")
            print("Use `tcrs fetch` to download class/sign dumps from the KoLmafia repository.")
        }
        else -> print("Usage: tcrs [status|load|save|fetch|derive [item id]|check <item id>|apply|reset]")
    }
}

internal suspend fun GameRuntimeLibrary.cliSpade(
    parameters: String,
    print: (String) -> Unit,
) {
    val request = spadeRequest
    if (request == null) {
        print("Spade submission is unavailable.")
        return
    }
    val tokens = parameters.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    when (tokens.firstOrNull()?.lowercase()) {
        null, "status" -> print("${request.pending().size} spading entries queued.")
        "submit" -> {
            val result = request.submit().getOrElse {
                print("Spade submission failed: ${it.message ?: "request failed"}")
                return
            }
            print(
                "Spade submission: sent=${result.sent}, failed=${result.failed}, " +
                    "malformed=${result.malformed}, remaining=${result.remaining}.",
            )
        }
        "help" -> print("Usage: spade [status|submit]")
        else -> print("Usage: spade [status|submit]")
    }
}
