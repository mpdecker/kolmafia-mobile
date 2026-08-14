package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.request.ClanFortuneRequest
import net.sourceforge.kolmafia.request.FriarRequest
import net.sourceforge.kolmafia.request.MayoSoakRequest
import net.sourceforge.kolmafia.request.MomRequest

internal fun GameRuntimeLibrary.cliFortune(parameters: String, print: (String) -> Unit) {
    val parts = parameters.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (parts.isEmpty()) {
        print("What do you want to request from the clan fortune teller?")
        return
    }
    when (parts[0].lowercase()) {
        "buff", "effect" -> {
            if (preferences?.getBoolean(ClanFortuneRequest.BUFF_USED_PREF, false) == true) {
                print("You already received a buff from the clan fortune teller.")
                return
            }
            val buffName = parts.getOrNull(1)?.trim().orEmpty()
            val buff = ClanFortuneRequest.findBuff(buffName)
            if (buff == null) {
                print("That isn't a valid buff.")
                return
            }
            val lounge = clanLoungeRequest ?: run {
                print("Clan lounge request is not available.")
                return
            }
            val choice = choiceRequest ?: run {
                print("Choice request is not available.")
                return
            }
            val prefs = preferences ?: run {
                print("Preferences are not available.")
                return
            }
            when (parts.size) {
                2 -> runBlocking {
                    ClanFortuneRequest(lounge, choice)
                        .takeBuff(buff, prefs)
                        .onFailure { print(it.message ?: "Fortune buff failed.") }
                }
                5 -> runBlocking {
                    ClanFortuneRequest(lounge, choice)
                        .takeBuff(buff, prefs, parts[2], parts[3], parts[4])
                        .onFailure { print(it.message ?: "Fortune buff failed.") }
                }
                else -> print(
                    "You need to choose all 3 words or none of the words for your compatibility test.",
                )
            }
        }
        else -> print("Fortune clanmate consult is not supported yet. Use: fortune buff <name> [w1 w2 w3]")
    }
}

internal fun GameRuntimeLibrary.cliMom(parameters: String, print: (String) -> Unit) {
    val arg = parameters.trim()
    if (arg.isEmpty()) {
        print("Decide which food to get.")
        return
    }
    val option = MomRequest.findFoodOption(arg)
    if (option == 0) {
        print("Decide which food to get.")
        return
    }
    val client = httpClient ?: run {
        print("HTTP client is not available.")
        return
    }
    runBlocking {
        MomRequest(client)
            .getFood(option, preferences, questDatabase)
            .onFailure { print(it.message ?: "Mom food failed.") }
    }
}

internal fun GameRuntimeLibrary.cliMayosoak(print: (String) -> Unit) {
    val err = MayoSoakRequest.preflightError(preferences)
    if (err != null) {
        print(err)
        return
    }
    val client = httpClient ?: run {
        print("HTTP client is not available.")
        return
    }
    runBlocking {
        MayoSoakRequest(client)
            .soak(preferences)
            .onFailure { print(it.message ?: "Mayo soak failed.") }
    }
}

internal fun GameRuntimeLibrary.cliFriars(parameters: String, print: (String) -> Unit) {
    val arg = parameters.trim()
    if (arg.isEmpty()) {
        visitKolPage("friars.php", applyQuestHooks = true)
        return
    }
    val parts = arg.split(Regex("\\s+")).filter { it.isNotEmpty() }
    val command = when {
        parts.size == 2 && parts[0].equals("blessing", ignoreCase = true) -> parts[1]
        parts.size == 1 -> parts[0]
        else -> {
            print("Syntax: friars [blessing] food|familiar|booze")
            return
        }
    }
    val option = FriarRequest.findBlessingOption(command)
    if (option == 0) {
        print("Syntax: friars [blessing] food|familiar|booze")
        return
    }
    val client = httpClient ?: run {
        print("HTTP client is not available.")
        return
    }
    val knownAscensions = preferences?.getInt("knownAscensions", 0) ?: 0
    runBlocking {
        FriarRequest(client)
            .getBlessing(option, preferences, questDatabase, knownAscensions)
            .onFailure { print(it.message ?: "Friar blessing failed.") }
    }
}

internal fun GameRuntimeLibrary.cliTelescope(parameters: String) {
    val parts = parameters.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    var command = parts.firstOrNull()?.lowercase().orEmpty()
    if (command == "look") {
        command = parts.getOrNull(1)?.lowercase().orEmpty()
    }
    val direction = when (command) {
        "high" -> "high"
        "low", "" -> "low"
        else -> "low"
    }
    val action = if (direction == "high") "telescopehigh" else "telescopelow"
    visitKolPage("campground.php?action=$action", applyQuestHooks = true)
}
