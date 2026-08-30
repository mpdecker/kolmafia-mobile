package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.session.ActionBarManager

/** Headless action-bar command; UI/relay rendering remains out of scope. */
internal fun GameRuntimeLibrary.cliActionBar(
    parameters: String,
    print: (String) -> Unit,
) {
    val request = actionBarRequest
    when {
        parameters.isBlank() || parameters.equals("status", ignoreCase = true) -> {
            print(ActionBarManager.current())
        }
        parameters.equals("fetch", ignoreCase = true) -> {
            if (request == null) {
                print(ActionBarManager.current())
            } else {
                runBlocking {
                    request.fetch()
                        .onSuccess(print)
                        .onFailure { print(it.message ?: "actionbar fetch failed") }
                }
            }
        }
        parameters.startsWith("set ", ignoreCase = true) -> {
            val json = parameters.substring(4).trim()
            if (request == null) {
                print("Action bar request is not available.")
            } else {
                runBlocking {
                    request.set(json)
                        .onSuccess { print("Action bar updated.") }
                        .onFailure { print(it.message ?: "actionbar update failed") }
                }
            }
        }
        else -> print("Usage: actionbar [fetch|status|set <json>]")
    }
}
