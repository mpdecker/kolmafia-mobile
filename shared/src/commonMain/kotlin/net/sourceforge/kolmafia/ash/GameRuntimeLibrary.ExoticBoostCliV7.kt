package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.request.BeachCombRequest
import net.sourceforge.kolmafia.request.HatterRequest
import net.sourceforge.kolmafia.request.SkateParkRequest
import net.sourceforge.kolmafia.request.SweetSynthesisRequest
import net.sourceforge.kolmafia.session.RabbitHoleAvailability

internal fun GameRuntimeLibrary.cliBeachHead(parameters: String, print: (String) -> Unit) {
    val query = BeachCombRequest.parseHeadQuery(parameters)
        ?: run {
            print("Usage: beach head <effect|id|keyword>")
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
        BeachCombRequest(client, choice)
            .combHead(query, preferences, counts)
            .onFailure { print(it.message ?: "Beach head comb failed.") }
    }
}

internal fun GameRuntimeLibrary.cliSkate(parameters: String, print: (String) -> Unit) {
    val place = parameters.trim()
    if (place.isEmpty()) {
        print("Usage: skate lutz|comet|band shell|eels|merry-go-round")
        return
    }
    val client = httpClient ?: run {
        print("HTTP client is not available.")
        return
    }
    runBlocking {
        SkateParkRequest(client)
            .takeBuff(place, preferences)
            .onFailure { print(it.message ?: "Skate Park buff failed.") }
    }
}

internal fun GameRuntimeLibrary.cliHatter(parameters: String, print: (String) -> Unit) {
    val length = HatterRequest.parseLength(parameters)
    if (length == null) {
        print("Usage: hatter <length>")
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
    val hasEffect = effectManager?.state?.value?.effects.orEmpty().any {
        it.id == RabbitHoleAvailability.DOWN_THE_RABBIT_HOLE_EFFECT ||
            it.name.equals("Down the Rabbit Hole", ignoreCase = true)
    }
    runBlocking {
        HatterRequest(client, choice, useItemRequest, equipmentRequest)
            .takeBuff(
                length = length,
                preferences = preferences,
                charState = character?.state?.value,
                inventoryCounts = counts,
                hasRabbitHoleEffect = hasEffect,
            )
            .onFailure { print(it.message ?: "Hatter buff failed.") }
    }
}

internal fun GameRuntimeLibrary.cliSynthesize(parameters: String, print: (String) -> Unit) {
    val query = SweetSynthesisRequest.parseEffectQuery(parameters)
    if (query == null) {
        print("Usage: synthesize <Synthesis: Hot|Cold|…>")
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
    val hasSkill = skillManager?.state?.value?.skills.orEmpty().any {
        it.id == SweetSynthesisRequest.SKILL_ID ||
            it.name.equals("Sweet Synthesis", ignoreCase = true)
    }
    runBlocking {
        SweetSynthesisRequest(client, choice, retrieveItemService)
            .synthesize(
                effectQuery = query,
                preferences = preferences,
                charState = character?.state?.value,
                inventoryCounts = counts,
                hasSkill = hasSkill,
            )
            .onFailure { print(it.message ?: "Synthesis failed.") }
    }
}
