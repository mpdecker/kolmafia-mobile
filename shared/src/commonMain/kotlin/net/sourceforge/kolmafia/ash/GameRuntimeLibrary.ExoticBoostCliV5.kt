package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.request.AprilBandRequest
import net.sourceforge.kolmafia.request.DaycareRequest
import net.sourceforge.kolmafia.request.FalloutShelterRequest
import net.sourceforge.kolmafia.request.GapRequest
import net.sourceforge.kolmafia.request.GrimRequest
import net.sourceforge.kolmafia.request.SpacegateRequest

internal fun GameRuntimeLibrary.cliGap(parameters: String, print: (String) -> Unit) {
    val option = GapRequest.findOption(parameters)
    if (option == 0) {
        val remaining = 5 - (preferences?.getInt(GapRequest.BUFFS_PREF, 0) ?: 0)
        print("$remaining superbuffs remaining.")
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
    val gapName = gameDatabase?.item(GapRequest.GREAT_PANTS_ID)?.name
    val replicaName = gameDatabase?.item(GapRequest.REPLICA_GREAT_PANTS_ID)?.name
    runBlocking {
        GapRequest(client, choice)
            .takeBuff(
                option = option,
                preferences = preferences,
                charState = character?.state?.value,
                gapPantsName = gapName,
                replicaPantsName = replicaName,
            )
            .onFailure { print(it.message ?: "GAP buff failed.") }
    }
}

internal fun GameRuntimeLibrary.cliSpacegate(parameters: String, print: (String) -> Unit) {
    val vaccine = SpacegateRequest.parseVaccine(parameters)
    val destination = SpacegateRequest.parseDestination(parameters)
    if (vaccine == 0 && destination == null) {
        print(SpacegateRequest.USAGE)
        return
    }
    if (destination != null && destination.isEmpty()) {
        print(SpacegateRequest.USAGE)
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
        val request = SpacegateRequest(client, choice)
        val result = if (vaccine != 0) {
            request.takeVaccine(vaccine, preferences)
        } else {
            request.chooseDestination(destination.orEmpty(), preferences)
        }
        result.onFailure { print(it.message ?: "Spacegate command failed.") }
    }
}

internal fun GameRuntimeLibrary.cliDaycare(parameters: String, print: (String) -> Unit) {
    val option = DaycareRequest.findSpaOption(parameters)
    if (option == 0) {
        print("Usage: daycare muscle|mysticality|moxie|regen")
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
        DaycareRequest(client, choice)
            .visitSpa(option, preferences)
            .onFailure { print(it.message ?: "Daycare spa failed.") }
    }
}

internal fun GameRuntimeLibrary.cliCampgroundVault3(print: (String) -> Unit) {
    val charState = character?.state?.value
    val err = FalloutShelterRequest.preflightError(
        preferences = preferences,
        inNuclearAutumn = charState?.inNuclearAutumn == true,
        limitMode = charState?.limitMode.orEmpty(),
    )
    if (err != null) {
        print(err)
        return
    }
    val client = httpClient ?: run {
        print("HTTP client is not available.")
        return
    }
    runBlocking {
        FalloutShelterRequest(client)
            .visitVault3(preferences)
            .onFailure { print(it.message ?: "Vault 3 spa failed.") }
    }
}

internal fun GameRuntimeLibrary.cliGrim(parameters: String, print: (String) -> Unit) {
    val option = GrimRequest.findOption(parameters)
    if (option == 0) {
        print("Usage: grim init|hpmp|damage")
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
    val owns = familiarManager?.state?.value?.ownedFamiliars
        ?.any { it.id == GrimRequest.GRIM_BROTHER_ID } == true
    runBlocking {
        GrimRequest(client, choice)
            .takeBuff(option, preferences, owns)
            .onFailure { print(it.message ?: "Grim buff failed.") }
    }
}

internal fun GameRuntimeLibrary.cliAprilband(parameters: String, print: (String) -> Unit) {
    val effectChoice = AprilBandRequest.findEffectChoice(parameters)
    if (effectChoice == 0) {
        print("Usage: aprilband effect <nc|c|drop>")
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
    val counts: (Int) -> Int = { id ->
        inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0
    }
    val helmetName = gameDatabase?.item(AprilBandRequest.HELMET_ID)?.name
    runBlocking {
        AprilBandRequest(client, choice)
            .takeEffect(
                choice = effectChoice,
                preferences = preferences,
                charState = character?.state?.value,
                inventoryCounts = counts,
                helmetName = helmetName,
            )
            .onFailure { print(it.message ?: "Aprilband effect failed.") }
    }
}
