package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.request.CrossStreamsRequest
import net.sourceforge.kolmafia.request.GenieRequest
import net.sourceforge.kolmafia.request.MonkeyPawRequest
import net.sourceforge.kolmafia.request.MonorailRequest
import net.sourceforge.kolmafia.request.ToggleInterestRequest

internal fun GameRuntimeLibrary.cliGenie(parameters: String, print: (String) -> Unit) {
    val wishResult = GenieRequest.resolveWish(parameters)
    val wish = wishResult.getOrElse {
        print(it.message ?: "Invalid genie wish.")
        return
    }
    val useReq = useItemRequest ?: run {
        print("Use item request is not available.")
        return
    }
    val choice = choiceRequest ?: run {
        print("Choice request is not available.")
        return
    }
    val counts: (Int) -> Int = { id ->
        inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0
    }
    runBlocking {
        GenieRequest(useReq, choice, inventoryManager)
            .makeWish(
                wish = wish,
                preferences = preferences,
                inventoryCounts = counts,
                inLegacyOfLoathing = character?.state?.value?.inLegacyOfLoathing == true,
            )
            .onFailure { print(it.message ?: "Genie wish failed.") }
    }
}

internal fun GameRuntimeLibrary.cliMonkeypaw(parameters: String, print: (String) -> Unit) {
    val wishResult = MonkeyPawRequest.resolveWish(parameters)
    val wish = wishResult.getOrElse {
        print(it.message ?: "Invalid monkey paw wish.")
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
    runBlocking {
        MonkeyPawRequest(client, choice, equipmentRequest)
            .makeWish(
                wish = wish,
                preferences = preferences,
                charState = character?.state?.value,
                inventoryCounts = counts,
            )
            .onFailure { print(it.message ?: "Monkey paw wish failed.") }
    }
}

internal fun GameRuntimeLibrary.cliMonorail(print: (String) -> Unit) {
    val client = httpClient ?: run {
        print("HTTP client is not available.")
        return
    }
    runBlocking {
        MonorailRequest(client)
            .visitLyle(preferences)
            .onFailure { print(it.message ?: "Monorail failed.") }
    }
}

internal fun GameRuntimeLibrary.cliToggle(print: (String) -> Unit) {
    val client = httpClient ?: run {
        print("HTTP client is not available.")
        return
    }
    runBlocking {
        ToggleInterestRequest(client)
            .toggle(effectManager?.state?.value)
            .onFailure { print(it.message ?: "Toggle failed.") }
    }
}

internal fun GameRuntimeLibrary.cliCrossstreams(parameters: String, print: (String) -> Unit) {
    val client = httpClient ?: run {
        print("HTTP client is not available.")
        return
    }
    val counts: (Int) -> Int = { id ->
        inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0
    }
    val target = parameters.trim().ifEmpty { null }
    runBlocking {
        CrossStreamsRequest(client, equipmentRequest, chatProbe)
            .crossStreams(
                targetArg = target,
                preferences = preferences,
                charState = character?.state?.value,
                inventoryCounts = counts,
            )
            .onFailure { print(it.message ?: "Cross streams failed.") }
    }
}
