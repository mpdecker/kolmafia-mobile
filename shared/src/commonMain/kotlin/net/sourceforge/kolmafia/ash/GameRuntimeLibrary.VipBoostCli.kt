package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.ChezSnooteeDatabase
import net.sourceforge.kolmafia.data.ConcoctionConsumptionType
import net.sourceforge.kolmafia.data.CrimboCafeDatabase
import net.sourceforge.kolmafia.data.HellKitchenDatabase
import net.sourceforge.kolmafia.data.HotDogDatabase
import net.sourceforge.kolmafia.data.MicroBreweryDatabase
import net.sourceforge.kolmafia.data.SpeakeasyDatabase
import net.sourceforge.kolmafia.maximizer.BangPotionResolver
import net.sourceforge.kolmafia.request.ClanLoungeRequest
import net.sourceforge.kolmafia.request.ClanRumpusRequest
import net.sourceforge.kolmafia.request.PhotoBoothRequest
import net.sourceforge.kolmafia.request.PillKeeperRequest
import net.sourceforge.kolmafia.request.StillSuitRequest

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

/** Desktop PoolCommand — play 1–3 VIP lounge pool games by stance. */
internal fun GameRuntimeLibrary.cliPool(parameters: String, print: (String) -> Unit) {
    val arg = parameters.trim()
    if (arg.isEmpty()) {
        print("What stance do you wish to take?")
        return
    }
    val tags = arg.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    if (tags.isEmpty() || tags.size > 3) {
        print("Specify from 1 to 3 pool games")
        return
    }
    val stances = mutableListOf<Int>()
    for (tag in tags) {
        val stance = ClanLoungeRequest.findPoolGame(tag)
        if (stance == 0) {
            print("I don't understand what a '$tag' pool game is.")
            return
        }
        stances += stance
    }
    val lounge = clanLoungeRequest ?: run {
        print("Clan lounge request is not available.")
        return
    }
    runBlocking {
        for (stance in stances) {
            lounge.playPoolGame(stance, preferences)
                .onFailure {
                    print(it.message ?: "Pool game failed.")
                    return@runBlocking
                }
        }
    }
}

/** Desktop JukeboxCommand — play a clan rumpus jukebox song. */
internal fun GameRuntimeLibrary.cliJukebox(parameters: String, print: (String) -> Unit) {
    val arg = parameters.trim()
    if (arg.isEmpty()) {
        print("Which song do you want to listen to?")
        return
    }
    val song = ClanRumpusRequest.findSong(arg)
    if (song == 0) {
        print("What kind of a song is that?")
        return
    }
    val client = httpClient ?: run {
        print("HTTP client is not available.")
        return
    }
    runBlocking {
        ClanRumpusRequest(client).playJukebox(song, preferences)
            .onFailure { print(it.message ?: "Jukebox failed.") }
    }
}

/** Result of attempting one consume candidate (either / multi-item lists). */
internal enum class CliConsumeAttempt {
    /** Item consumed successfully — either lists stop. */
    Success,
    /** Skip this candidate and try the next (either) or continue (multi). */
    Skip,
    /** Hard failure — abort the whole command (non-either unresolved/retrieve). */
    Abort,
}

/**
 * Desktop UseItemCommand `either ` prefix — strip and flag first-success mode.
 */
internal fun stripEitherConsumePrefix(parameters: String): Pair<Boolean, String> {
    val rest = parameters.trim()
    return if (rest.startsWith("either ", ignoreCase = true)) {
        true to rest.substring(7).trim()
    } else {
        false to rest
    }
}

internal fun GameRuntimeLibrary.cliInventoryCount(itemId: Int): Int =
    inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0

/**
 * Desktop UseItemCommand either levels 0–1 — inventory-owned first, then retrieve.
 */
internal fun GameRuntimeLibrary.runEitherConsume(
    items: List<Pair<Int, String>>,
    attempt: (qty: Int, name: String, allowRetrieve: Boolean) -> CliConsumeAttempt,
) {
    for ((qty, name) in items) {
        if (attempt(qty, name, false) == CliConsumeAttempt.Success) return
    }
    for ((qty, name) in items) {
        if (attempt(qty, name, true) == CliConsumeAttempt.Success) return
    }
}

/**
 * Desktop UseItemCommand / RestaurantCommand.makeHotDogStandRequest —
 * VIP hot dogs go through lounge HTTP; cafe menu via CafePurchaseRequest; else inventory eat.
 * Supports comma-separated item lists (Trivia-style multi consume) and `either` first-success.
 */
internal fun GameRuntimeLibrary.cliEat(parameters: String, print: (String) -> Unit) {
    val (either, rest) = stripEitherConsumePrefix(parameters)
    val items = parseConsumeItemList(rest)
    if (items.isEmpty()) {
        print("What do you want to eat?")
        return
    }
    if (either) {
        runEitherConsume(items) { qty, name, allowRetrieve ->
            cliEatOne(qty, name, allowRetrieve, print, softFail = true)
        }
        return
    }
    for ((qty, name) in items) {
        when (cliEatOne(qty, name, allowRetrieve = true, print, softFail = false)) {
            CliConsumeAttempt.Abort -> return
            CliConsumeAttempt.Success, CliConsumeAttempt.Skip -> Unit
        }
    }
}

internal fun GameRuntimeLibrary.cliEatOne(
    qty: Int,
    name: String,
    allowRetrieve: Boolean,
    print: (String) -> Unit,
    softFail: Boolean,
): CliConsumeAttempt {
    if (HotDogDatabase.isHotDog(name)) {
        if (!allowRetrieve) return CliConsumeAttempt.Skip
        val lounge = clanLoungeRequest ?: run {
            print("Clan lounge request is not available.")
            return if (softFail) CliConsumeAttempt.Skip else CliConsumeAttempt.Abort
        }
        var ok = true
        val state = character?.state?.value
        runBlocking {
            repeat(qty) {
                lounge.eatHotDog(name, preferences, state)
                    .onFailure {
                        print(it.message ?: "Hot dog failed.")
                        ok = false
                        return@runBlocking
                    }
            }
        }
        return if (ok) CliConsumeAttempt.Success else if (softFail) CliConsumeAttempt.Skip else CliConsumeAttempt.Success
    }
    if (isCafePurchaseMenuItem(name)) {
        if (!allowRetrieve) return CliConsumeAttempt.Skip
        cliCafePurchase(name, qty, ConcoctionConsumptionType.EAT, print)
        return CliConsumeAttempt.Success
    }
    val itemId = resolveCliConsumeItemId(name, if (softFail) { _ -> } else print)
        ?: return if (softFail) CliConsumeAttempt.Skip else CliConsumeAttempt.Abort
    if (!allowRetrieve) {
        if (cliInventoryCount(itemId) < qty) return CliConsumeAttempt.Skip
    } else if (!retrieveForCliConsume(itemId, qty, print)) {
        return if (softFail) CliConsumeAttempt.Skip else CliConsumeAttempt.Abort
    }
    runBlocking { eatFoodRequest?.eat(itemId, qty) }
    return CliConsumeAttempt.Success
}

/**
 * Desktop UseItemCommand / RestaurantCommand.makeSpeakeasyRequest —
 * VIP speakeasy drinks go through lounge HTTP; cafe menu via CafePurchaseRequest;
 * StillSuit distillate via StillSuitRequest; else inventory drink.
 * Supports comma-separated item lists and `either` first-success.
 */
internal fun GameRuntimeLibrary.cliDrink(parameters: String, print: (String) -> Unit) {
    val (either, rest) = stripEitherConsumePrefix(parameters)
    val items = parseConsumeItemList(rest)
    if (items.isEmpty()) {
        print("What do you want to drink?")
        return
    }
    if (either) {
        runEitherConsume(items) { qty, name, allowRetrieve ->
            cliDrinkOne(qty, name, allowRetrieve, print, softFail = true)
        }
        return
    }
    for ((qty, name) in items) {
        when (cliDrinkOne(qty, name, allowRetrieve = true, print, softFail = false)) {
            CliConsumeAttempt.Abort -> return
            CliConsumeAttempt.Success, CliConsumeAttempt.Skip -> Unit
        }
    }
}

internal fun GameRuntimeLibrary.cliDrinkOne(
    qty: Int,
    name: String,
    allowRetrieve: Boolean,
    print: (String) -> Unit,
    softFail: Boolean,
): CliConsumeAttempt {
    if (SpeakeasyDatabase.isSpeakeasyDrink(name)) {
        if (!allowRetrieve) return CliConsumeAttempt.Skip
        val lounge = clanLoungeRequest ?: run {
            print("Clan lounge request is not available.")
            return if (softFail) CliConsumeAttempt.Skip else CliConsumeAttempt.Abort
        }
        var ok = true
        val state = character?.state?.value
        runBlocking {
            repeat(qty) {
                lounge.drinkSpeakeasy(name, preferences, state)
                    .onFailure {
                        print(it.message ?: "Speakeasy drink failed.")
                        ok = false
                        return@runBlocking
                    }
            }
        }
        return if (ok) CliConsumeAttempt.Success else if (softFail) CliConsumeAttempt.Skip else CliConsumeAttempt.Success
    }
    if (isCafePurchaseMenuItem(name)) {
        if (!allowRetrieve) return CliConsumeAttempt.Skip
        cliCafePurchase(name, qty, ConcoctionConsumptionType.DRINK, print)
        return CliConsumeAttempt.Success
    }
    if (StillSuitRequest.isDistillate(name)) {
        if (!allowRetrieve) return CliConsumeAttempt.Skip
        cliStillSuitDistill(name, qty, print)
        return CliConsumeAttempt.Success
    }
    val itemId = resolveCliConsumeItemId(name, if (softFail) { _ -> } else print)
        ?: return if (softFail) CliConsumeAttempt.Skip else CliConsumeAttempt.Abort
    if (!allowRetrieve) {
        if (cliInventoryCount(itemId) < qty) return CliConsumeAttempt.Skip
    } else if (!retrieveForCliConsume(itemId, qty, print)) {
        return if (softFail) CliConsumeAttempt.Skip else CliConsumeAttempt.Abort
    }
    runBlocking { drinkBoozeRequest?.drink(itemId, qty) }
    return CliConsumeAttempt.Success
}

/** Hell's Kitchen / Chez Snootée / Microbrewery / Crimbo Cafe static menus. */
internal fun isCafePurchaseMenuItem(name: String): Boolean =
    HellKitchenDatabase.isOnMenu(name) ||
        ChezSnooteeDatabase.isOnMenu(name) ||
        MicroBreweryDatabase.isOnMenu(name) ||
        CrimboCafeDatabase.isOnMenu(name)

/**
 * Desktop UseItemCommand restaurant preflight — CafePurchaseRequest dispatch.
 */
internal fun GameRuntimeLibrary.cliCafePurchase(
    name: String,
    qty: Int,
    type: ConcoctionConsumptionType,
    print: (String) -> Unit,
) {
    val cafe = cafePurchaseRequest ?: run {
        print("Cafe purchase request is not available.")
        return
    }
    val state = character?.state?.value
    val inventoryCountById: (Int) -> Int = { id ->
        inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0
    }
    runBlocking {
        repeat(qty) {
            cafe.purchase(name, type, state, preferences, inventoryCountById)
                .onFailure {
                    print(it.message ?: "Cafe purchase failed.")
                    return@runBlocking
                }
        }
    }
}

/**
 * Desktop UseItemCommand drink preflight — StillSuitRequest distillate.
 */
internal fun GameRuntimeLibrary.cliStillSuitDistill(
    name: String,
    qty: Int,
    print: (String) -> Unit,
) {
    val still = stillSuitRequest ?: run {
        print("StillSuit request is not available.")
        return
    }
    val state = character?.state?.value
    val inventoryCountById: (Int) -> Int = { id ->
        inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0
    }
    runBlocking {
        repeat(qty) {
            still.distill(
                name = name,
                type = ConcoctionConsumptionType.DRINK,
                state = state,
                prefs = preferences,
                inventoryCountById = inventoryCountById,
            ).onFailure {
                print(it.message ?: "StillSuit distillate failed.")
                return@runBlocking
            }
        }
    }
}

/** Desktop UseItemCommand — inventory use with bang potion / slime vial resolution. */
internal fun GameRuntimeLibrary.cliUse(parameters: String, print: (String) -> Unit) {
    val (either, rest) = stripEitherConsumePrefix(parameters)
    val items = parseConsumeItemList(rest)
    if (items.isEmpty()) {
        print("What do you want to use?")
        return
    }
    if (either) {
        runEitherConsume(items) { qty, name, allowRetrieve ->
            cliUseOne(qty, name, allowRetrieve, print, softFail = true)
        }
        return
    }
    for ((qty, name) in items) {
        when (cliUseOne(qty, name, allowRetrieve = true, print, softFail = false)) {
            CliConsumeAttempt.Abort -> return
            CliConsumeAttempt.Success, CliConsumeAttempt.Skip -> Unit
        }
    }
}

internal fun GameRuntimeLibrary.cliUseOne(
    qty: Int,
    name: String,
    allowRetrieve: Boolean,
    print: (String) -> Unit,
    softFail: Boolean,
): CliConsumeAttempt {
    val itemId = resolveCliConsumeItemId(name, if (softFail) { _ -> } else print)
        ?: return if (softFail) CliConsumeAttempt.Skip else CliConsumeAttempt.Abort
    if (!allowRetrieve) {
        if (cliInventoryCount(itemId) < qty) return CliConsumeAttempt.Skip
    } else if (!retrieveForCliConsume(itemId, qty, print)) {
        return if (softFail) CliConsumeAttempt.Skip else CliConsumeAttempt.Abort
    }
    runBlocking { useItemRequest?.use(itemId, qty) }
    return CliConsumeAttempt.Success
}

/** Inventory chew with bang potion / slime vial resolution. */
internal fun GameRuntimeLibrary.cliChew(parameters: String, print: (String) -> Unit) {
    val (either, rest) = stripEitherConsumePrefix(parameters)
    val items = parseConsumeItemList(rest)
    if (items.isEmpty()) {
        print("What do you want to chew?")
        return
    }
    if (either) {
        runEitherConsume(items) { qty, name, allowRetrieve ->
            cliChewOne(qty, name, allowRetrieve, print, softFail = true)
        }
        return
    }
    for ((qty, name) in items) {
        when (cliChewOne(qty, name, allowRetrieve = true, print, softFail = false)) {
            CliConsumeAttempt.Abort -> return
            CliConsumeAttempt.Success, CliConsumeAttempt.Skip -> Unit
        }
    }
}

internal fun GameRuntimeLibrary.cliChewOne(
    qty: Int,
    name: String,
    allowRetrieve: Boolean,
    print: (String) -> Unit,
    softFail: Boolean,
): CliConsumeAttempt {
    val itemId = resolveCliConsumeItemId(name, if (softFail) { _ -> } else print)
        ?: return if (softFail) CliConsumeAttempt.Skip else CliConsumeAttempt.Abort
    if (!allowRetrieve) {
        if (cliInventoryCount(itemId) < qty) return CliConsumeAttempt.Skip
    } else if (!retrieveForCliConsume(itemId, qty, print)) {
        return if (softFail) CliConsumeAttempt.Skip else CliConsumeAttempt.Abort
    }
    runBlocking { chewRequest?.chew(itemId, qty) }
    return CliConsumeAttempt.Success
}

/**
 * Desktop UseItemRequest.retrieveItem preflight before inventory consume.
 * Lounge/cafe/StillSuit branches skip this — they are not inventoriable.
 */
internal fun GameRuntimeLibrary.retrieveForCliConsume(
    itemId: Int,
    qty: Int,
    print: (String) -> Unit,
): Boolean {
    val retrieve = retrieveItemService ?: return true
    val got = runBlocking { retrieve.retrieve(itemId, qty) }
    if (got < qty) {
        print("Unable to retrieve ${qty}x item #$itemId (got $got)")
        return false
    }
    return true
}

/**
 * Resolve consume target: item DB name, then bang potion / slime vial prefs.
 * Desktop UseItemCommand → AdventureResult.resolveBangPotion().
 */
internal fun GameRuntimeLibrary.resolveCliConsumeItemId(
    name: String,
    print: (String) -> Unit,
): Int? {
    gameDatabase?.item(name)?.id?.let { return it }
    BangPotionResolver.resolveItemId(name, preferences)?.let { return it }
    if (name.startsWith("potion of ", ignoreCase = true) ||
        name.startsWith("vial of slime:", ignoreCase = true)
    ) {
        print("You have not yet identified the $name")
    }
    return null
}

/**
 * Desktop UseItemCommand / ItemFinder comma list — split `\s*,\s*` then parse each `N name`.
 */
internal fun parseConsumeItemList(parameters: String): List<Pair<Int, String>> {
    val rest = parameters.trim()
    if (rest.isEmpty()) return emptyList()
    return rest.split(Regex("""\s*,\s*"""))
        .mapNotNull { segment -> parseConsumeQtyName(segment) }
}

/** Parse `N name` or bare `name` for eat/drink CLI (Maximizer emits `eat 1 …`). */
internal fun parseConsumeQtyName(parameters: String): Pair<Int, String>? {
    val rest = parameters.trim()
    if (rest.isEmpty()) return null
    val qtyMatch = Regex("""^(\d+)\s+(.+)$""").matchEntire(rest)
    return if (qtyMatch != null) {
        val qty = qtyMatch.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1
        val name = qtyMatch.groupValues[2].trim()
        if (name.isEmpty()) null else qty to name
    } else {
        1 to rest
    }
}
