package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.request.DeckOfEveryCardRequest
import net.sourceforge.kolmafia.request.GongRequest
import net.sourceforge.kolmafia.request.HeyDezeRequest
import net.sourceforge.kolmafia.request.SkeletonRequest

internal fun GameRuntimeLibrary.cliStyx(parameters: String, print: (String) -> Unit) {
    val arg = parameters.trim()
    if (arg.isEmpty()) {
        print("Usage: styx muscle|mysticality|moxie")
        return
    }
    val buffId = HeyDezeRequest.findBuffId(arg)
    if (buffId == 0) {
        print("You can only buff muscle, mysticality, or moxie.")
        return
    }
    val client = httpClient ?: run {
        print("HTTP client is not available.")
        return
    }
    runBlocking {
        HeyDezeRequest(client)
            .takeBuff(buffId, preferences, character?.state?.value)
            .onFailure { print(it.message ?: "Styx buff failed.") }
    }
}

internal fun GameRuntimeLibrary.cliSkeleton(parameters: String, print: (String) -> Unit) {
    val arg = parameters.trim()
    if (arg.isEmpty()) {
        print("Usage: skeleton warrior|cleric|wizard|rogue|buddy")
        return
    }
    val option = SkeletonRequest.findSkeleton(arg)
    if (option == 0) {
        print("I don't understand that skeleton type.")
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
        SkeletonRequest(useReq, choice)
            .useSkeleton(option, counts)
            .onFailure { print(it.message ?: "Skeleton use failed.") }
    }
}

internal fun GameRuntimeLibrary.cliPlay(parameters: String, print: (String) -> Unit) {
    val mainStat = character?.state?.value?.mainStat
        ?: net.sourceforge.kolmafia.character.MainStat.MUSCLE
    val cardResult = DeckOfEveryCardRequest.resolvePlay(parameters, mainStat)
    val card = cardResult.getOrElse {
        print(it.message ?: "Play failed.")
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
        DeckOfEveryCardRequest(client, choice)
            .play(
                card = card,
                preferences = preferences,
                inventoryCounts = counts,
                inLegacyOfLoathing = character?.state?.value?.inLegacyOfLoathing == true,
            )
            .onFailure { print(it.message ?: "Play failed.") }
    }
}

internal fun GameRuntimeLibrary.cliGong(parameters: String, print: (String) -> Unit) {
    val useReq = useItemRequest ?: run {
        print("Use item request is not available.")
        return
    }
    val counts: (Int) -> Int = { id ->
        inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0
    }
    runBlocking {
        GongRequest(useReq)
            .run(
                parameters = parameters,
                preferences = preferences,
                charState = character?.state?.value,
                inventoryCounts = counts,
                activeEffects = effectManager?.state?.value?.effects.orEmpty(),
            )
            .onFailure { print(it.message ?: "Gong failed.") }
            .onSuccess { html ->
                if (html.startsWith("Gong path set:")) print(html)
            }
    }
}
