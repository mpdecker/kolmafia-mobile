package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.request.BeachCombRequest
import net.sourceforge.kolmafia.request.HatterRequest
import net.sourceforge.kolmafia.request.SkateParkRequest
import net.sourceforge.kolmafia.request.SweetSynthesisRequest
import net.sourceforge.kolmafia.session.RabbitHoleAvailability
import net.sourceforge.kolmafia.session.RabbitHoleManager

internal fun GameRuntimeLibrary.cliBeach(parameters: String, print: (String) -> Unit) {
    val parsed = BeachCombRequest.parseCommand(parameters)
        ?: run {
            print("Usage: beach common|head DESC|print|visit|random|wander MINUTES|comb ROW COL|exit")
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
    val request = beachCombRequest ?: BeachCombRequest(
        client = client,
        choiceRequest = choice,
        character = character,
        equipmentManager = equipmentManager,
        equipmentRequest = equipmentRequest,
        sessionLogger = sessionLogger,
    )
    runBlocking {
        request
            .execute(parsed, preferences, counts)
            .onSuccess {
                if (parsed.command == BeachCombRequest.Command.PRINT) print(it)
            }
            .onFailure { print(it.message ?: "Beach Comb command failed.") }
    }
}

internal fun GameRuntimeLibrary.cliBeachHead(parameters: String, print: (String) -> Unit) =
    cliBeach(parameters, print)

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
        skateParkRequest ?: SkateParkRequest(
            client = client,
            character = character,
            inventory = inventoryManager,
            equipmentManager = equipmentManager,
            equipmentRequest = equipmentRequest,
            sessionLogger = sessionLogger,
        )
            .takeBuff(place, preferences)
            .onFailure { print(it.message ?: "Skate Park buff failed.") }
    }
}

internal fun GameRuntimeLibrary.cliHatter(parameters: String, print: (String) -> Unit) {
    val counts: (Int) -> Int = { id ->
        inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0
    }
    val charState = character?.state?.value
    if (parameters.isBlank()) {
        val lines = RabbitHoleManager.hatCommand(
            inventoryCount = counts,
            equippedHatName = charState?.equippedItem(
                net.sourceforge.kolmafia.character.EquipmentSlot.HAT,
            ),
        )
        if (lines.isEmpty()) print("You don't have any usable hats.") else lines.forEach(print)
        return
    }
    if (charState?.inTwoCrazyRandomSummer == true) {
        print("You can't get Down the Rabbit Hole in Two Crazy Random Summer.")
        return
    }
    val length = resolveHatterLength(parameters)
    if (length == null) {
        print("No unique matching hat found.")
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
    val hasEffect = effectManager?.state?.value?.effects.orEmpty().any {
        it.id == RabbitHoleAvailability.DOWN_THE_RABBIT_HOLE_EFFECT ||
            it.name.equals("Down the Rabbit Hole", ignoreCase = true)
    }
    runBlocking {
        HatterRequest(client, choice, useItemRequest, equipmentRequest)
            .takeBuff(
                length = length,
                preferences = preferences,
                charState = charState,
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
