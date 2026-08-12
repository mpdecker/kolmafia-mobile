package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.request.ClanLoungeRequest
import net.sourceforge.kolmafia.request.ClanRumpusRequest
import net.sourceforge.kolmafia.request.PhotoBoothRequest
import net.sourceforge.kolmafia.request.PillKeeperRequest

internal fun GameRuntimeLibrary.cliShower(parameters: String, print: (String) -> Unit) {
    val arg = parameters.trim()
    if (arg.isEmpty()) {
        print("What temperature should your shower be?")
        return
    }
    val option = ClanLoungeRequest.findShowerOption(arg)
    if (option == 0) {
        print("I don't understand what a '$arg' shower is.")
        return
    }
    val lounge = clanLoungeRequest ?: run {
        print("Clan lounge request is not available.")
        return
    }
    runBlocking {
        lounge.takeShower(option, preferences)
            .onFailure { print(it.message ?: "Shower failed.") }
    }
}

internal fun GameRuntimeLibrary.cliSwim(parameters: String, print: (String) -> Unit) {
    val arg = parameters.trim()
    if (arg.isEmpty()) {
        print("What do you want to do in the swimming pool?")
        return
    }
    val option = ClanLoungeRequest.findSwimmingOption(arg)
    if (option == 0) {
        print("I don't understand what '$arg' is.")
        return
    }
    val lounge = clanLoungeRequest ?: run {
        print("Clan lounge request is not available.")
        return
    }
    runBlocking {
        lounge.swimPool(option, preferences, choiceRequest)
            .onFailure { print(it.message ?: "Swim failed.") }
    }
}

internal fun GameRuntimeLibrary.cliBallpit(print: (String) -> Unit) {
    val client = httpClient ?: run {
        print("HTTP client is not available.")
        return
    }
    runBlocking {
        ClanRumpusRequest(client).jumpInBallpit(preferences)
            .onFailure { print(it.message ?: "Ball pit failed.") }
    }
}

internal fun GameRuntimeLibrary.cliPillkeeper(parameters: String, print: (String) -> Unit) {
    val client = httpClient ?: run {
        print("HTTP client is not available.")
        return
    }
    val choice = choiceRequest ?: run {
        print("Choice request is not available.")
        return
    }
    val hasKeeper = inventoryManager?.state?.value?.items
        ?.get(PillKeeperRequest.PILL_KEEPER_ITEM_ID)?.quantity?.let { it > 0 } == true
    val resolved = PillKeeperRequest.resolve(parameters)
    if (resolved == null) {
        print("Invalid choice")
        return
    }
    print("Taking pills for ${resolved.pillText}")
    runBlocking {
        PillKeeperRequest(client, choice)
            .takePills(parameters, character?.state?.value, preferences, hasKeeper)
            .onFailure { print(it.message ?: "Pill keeper failed.") }
    }
}

internal fun GameRuntimeLibrary.cliPhotobooth(parameters: String, print: (String) -> Unit) {
    val parts = parameters.trim().split(Regex("\\s+"), limit = 2)
    if (parts.isEmpty() || parts[0].isEmpty()) {
        print("Usage: photobooth effect [ wild | tower | space ]")
        return
    }
    when (parts[0].lowercase()) {
        "effect" -> {
            val effectArg = parts.getOrNull(1)?.trim().orEmpty()
            if (effectArg.isEmpty()) {
                print("Which effect do you want?")
                return
            }
            val client = httpClient ?: run {
                print("HTTP client is not available.")
                return
            }
            val choice = choiceRequest ?: run {
                print("Choice request is not available.")
                return
            }
            runBlocking {
                PhotoBoothRequest(client, choice)
                    .takeEffect(effectArg, preferences)
                    .onFailure { print(it.message ?: "Photo booth failed.") }
            }
        }
        else -> print("Usage: photobooth effect [ wild | tower | space ]")
    }
}
