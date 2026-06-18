package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.WitchessSolutionDatabase
import net.sourceforge.kolmafia.request.WitchessRequest
import net.sourceforge.kolmafia.session.WitchessManager

internal fun GameRuntimeLibrary.cliWitchess(params: String, print: (String) -> Unit) {
    val prefs = preferences ?: run {
        print("Preferences are not available.")
        return
    }
    val client = httpClient ?: run {
        print("HTTP client is not available.")
        return
    }

    if (prefs.getBoolean("_witchessBuff", false)) {
        print("You already got your Witchess buff today.")
        return
    }

    val command = params.trim().split(Regex("\\s+")).firstOrNull { it.isNotEmpty() }.orEmpty()

    when (command.lowercase()) {
        "", "buff" -> runBlocking {
            WitchessRequest(client).claimBuff(prefs).fold(
                onSuccess = { message -> print(message) },
                onFailure = { print("Failed to claim Witchess buff.") },
            )
        }
        "solve" -> {
            if (!WitchessSolutionDatabase.isLoaded) {
                print("Witchess solution data is not loaded.")
                return
            }
            runBlocking {
                WitchessManager.solveDailyPuzzles(client, prefs, print = print)
            }
        }
        else -> print("I'm not sure what you want me to do with your Witchess Set.")
    }
}
