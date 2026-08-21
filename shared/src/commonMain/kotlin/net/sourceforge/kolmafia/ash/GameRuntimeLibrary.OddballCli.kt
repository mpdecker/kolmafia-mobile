package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.ZodiacSign
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.quest.HashingChoiceSync

/**
 * Phases 1053–1062 — Oddball CLI Track E:
 * skeeball, vise, throw, buffbot, beach (already complete), crimbotrain, badmoon, flicker.
 */

private const val GG_TOKEN_ID = 4621
private const val HASHING_VISE_ID = 11826
private const val CRIMBO_TRAINING_MANUAL_ID = 11046
private const val BRICK_ITEM_ID = 1649

private val VISE_SCHEMATIC_NAMES = listOf(
    "dedigitizer schematic: cyburger",
    "dedigitizer schematic: cybeer",
    "dedigitizer schematic: psilocyber mushroom",
    "dedigitizer schematic: brute force hammer",
    "dedigitizer schematic: malware injector",
    "dedigitizer schematic: cybervisor",
    "dedigitizer schematic: digibritches",
    "dedigitizer schematic: cryptocloak",
    "dedigitizer schematic: zero-trust tanktop",
    "dedigitizer schematic: retro floppy disk",
    "dedigitizer schematic: pocket GPU",
    "dedigitizer schematic: trojan horseshoe",
    "dedigitizer schematic: familiar-in-the-middle",
    "dedigitizer schematic: 3d printed server room key",
    "dedigitizer schematic: SLEEP(5) rom chip",
    "dedigitizer schematic: OVERCLOCK(10) rom chip",
    "dedigitizer schematic: STATS+++ rom chip",
    "dedigitizer schematic: insignificant bit",
    "dedigitizer schematic: hashing vise",
    "dedigitizer schematic: geofencing rapier",
    "dedigitizer schematic: geofencing shield",
    "dedigitizer schematic: virtual cybertattoo",
)

private val FLICKERING_PIXELS = listOf(
    arrayOf("flickeringPixel1", "Anger", "Stupid Pipes", "25 hot resistance"),
    arrayOf("flickeringPixel2", "Anger", "You're Freaking Kidding Me", "500 buffed Muscle/Mysticality/Moxie"),
    arrayOf("flickeringPixel3", "Fear", "Snakes", "300 buffed Moxie"),
    arrayOf("flickeringPixel4", "Fear", "So... Many... Skulls...", "25 spooky resistance"),
    arrayOf("flickeringPixel5", "Doubt", "A Stupid Dummy", "+300 bonus damage"),
    arrayOf("flickeringPixel6", "Doubt", "Slings and Arrows", "1000 HP"),
    arrayOf("flickeringPixel7", "Regret", "This Is Your Life", "1000 MP"),
    arrayOf("flickeringPixel8", "Regret", "The Wall of Wailing", "60 prismatic damage"),
)

private fun GameRuntimeLibrary.oddballInvQty(itemId: Int): Int =
    inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0

/** Desktop SkeeballCommand — Game Grid tokens at the broken Skeeball machine. */
internal fun GameRuntimeLibrary.cliSkeeball(parameters: String, print: (String) -> Unit) {
    val tokens = oddballInvQty(GG_TOKEN_ID)
    val trimmed = parameters.trim()
    val count = when {
        trimmed.isEmpty() -> 1
        trimmed == "*" -> tokens
        trimmed.toIntOrNull() != null -> minOf(trimmed.toInt(), tokens)
        else -> {
            print("\"$trimmed\" doesn't look like a number.")
            return
        }
    }
    if (httpClient == null) {
        print("Usage: skeeball [<count>|*] - squander Game Grid tokens at the broken Skeeball machine")
        print("Game Grid tokens available: $tokens")
        return
    }
    repeat(count) {
        visitKolPage("place.php?whichplace=arcade&action=arcade_skeeball")
    }
    if (count > 0) {
        print("Played skeeball $count time(s).")
    } else {
        print("No Game Grid tokens to spend.")
    }
}

/** Desktop ViseCommand — smash dedigitizer schematics with hashing vise (choice 1551). */
internal fun GameRuntimeLibrary.cliVise(parameters: String, print: (String) -> Unit) {
    if (oddballInvQty(HASHING_VISE_ID) < 1) {
        print("You don't have an available hashing vise.")
        return
    }
    val trimmed = parameters.trim()
    if (trimmed.isEmpty()) {
        print("Usage: vise [count] <item> [, <another>]... - smash schematics into bits")
        return
    }
    val itemPattern = Regex("""(?:(\d+)\s+)?(.+)""")
    val specs = mutableListOf<Pair<String, Int>>()
    for (raw in trimmed.split(',')) {
        val part = raw.trim()
        if (part.isEmpty()) continue
        val m = itemPattern.matchEntire(part) ?: run {
            print("Usage: vise [count] <item> [, <another>]...")
            return
        }
        val qty = m.groupValues[1].toIntOrNull() ?: 1
        val name = m.groupValues[2].trim()
        val matches = VISE_SCHEMATIC_NAMES.filter {
            it.contains(name, ignoreCase = true)
        }
        when {
            matches.isEmpty() -> {
                print("'$name' matches no schematics")
                return
            }
            matches.size > 1 -> {
                print("'$name' matches ${matches.size} schematics")
                return
            }
            else -> specs.add(matches[0] to qty)
        }
    }
    if (specs.isEmpty()) {
        print("Usage: vise [count] <item> [, <another>]...")
        return
    }
    val useReq = useItemRequest
    val choice = choiceRequest
    if (useReq == null || choice == null) {
        print("Hashing vise HTTP is not available.")
        return
    }
    runBlocking {
        useReq.use(HASHING_VISE_ID, 1).exceptionOrNull()?.let {
            print(it.message ?: "Failed to use hashing vise.")
            return@runBlocking
        }
        for ((schematicName, qty) in specs) {
            val itemId = ItemDatabase.getByName(schematicName)?.id
                ?: gameDatabase?.item(schematicName)?.id
            if (itemId == null) {
                print("Unknown schematic: $schematicName")
                continue
            }
            val available = oddballInvQty(itemId)
            val needed = minOf(qty, available).coerceAtLeast(0)
            if (needed < qty) {
                print("(hashable quantity of $schematicName is limited to $available by availability in inventory)")
            }
            repeat(needed) {
                choice.choose(
                    HashingChoiceSync.CHOICE_ID,
                    1,
                    mapOf("iid" to itemId.toString()),
                ).onSuccess { (html, url) ->
                    HashingChoiceSync.apply(
                        HashingChoiceSync.CHOICE_ID,
                        html,
                        choiceUrl = url,
                    ) { id, amount ->
                        inventoryManager?.consumeItemLocally(id, amount)
                    }
                    print("vise $schematicName")
                }.onFailure {
                    print(it.message ?: "Failed to smash $schematicName")
                }
            }
        }
    }
}

/** Desktop ThrowItemCommand — curse.php use on another player (not combat throw_item). */
internal fun GameRuntimeLibrary.cliThrow(parameters: String, print: (String) -> Unit) {
    var rest = parameters.trim()
    var message = ""
    val msgSplit = rest.indexOf("||")
    if (msgSplit >= 0) {
        message = rest.substring(msgSplit + 2).trim()
        rest = rest.substring(0, msgSplit).trim()
    }
    val atSplit = rest.indexOf(" at ")
    if (atSplit < 0) {
        print("No recipient specified.")
        print("Usage: throw <item> at <player> [ || <message> ]")
        return
    }
    val target = rest.substring(atSplit + 4).trim()
    val itemQuery = rest.substring(0, atSplit).trim()
    if (itemQuery.isEmpty() || target.isEmpty()) {
        print("No recipient specified.")
        return
    }
    val item = resolveCurseItem(itemQuery)
        ?: run {
            print("Unable to find item matching '$itemQuery'.")
            return
        }
    if (!item.secondaryUses.any { it.equals("curse", ignoreCase = true) }) {
        print("The ${item.name} is not properly balanced for throwing.")
        return
    }
    if (httpClient == null) {
        print("HTTP client is not available.")
        return
    }
    print("Throwing ${item.name} at $target...")
    val post = buildString {
        append("action=use&whichitem=${item.id}&targetplayer=$target")
        if (message.isNotEmpty()) {
            if (item.id == BRICK_ITEM_ID) {
                append("&message=$message")
            } else {
                message.split(Regex("""\s*\|\s*""")).forEachIndexed { i, part ->
                    append("&text${('a' + i)}=$part")
                }
            }
        }
    }
    val html = visitKolPost("curse.php", post)
    when {
        html == null -> print("Throw failed.")
        html.contains("That player could not be found", ignoreCase = true) ->
            print("$target evaded your thrown item by the unusual strategy of being nonexistent.")
        html.contains("try again later", ignoreCase = true) ||
            html.contains("cannot be used", ignoreCase = true) ||
            html.contains("can't use this item", ignoreCase = true) ->
            print("Can't use the item on that player at the moment.")
        html.contains("No message?", ignoreCase = true) ||
            html.contains("no message", ignoreCase = true) ->
            print("That item requires a message.")
        else -> print("throw ${item.name} at $target")
    }
}

/** Desktop BuffbotCommand — client hosting stub; request path remains `buff bot skill`. */
internal fun GameRuntimeLibrary.cliBuffbot(parameters: String, print: (String) -> Unit) {
    print("Client buff-bot hosting is not available on mobile.")
    print("To request buffs from a bot, use: buff <bot> <skill> [turns]")
    if (parameters.trim().isNotEmpty()) {
        print("(ignored iterations: ${parameters.trim()})")
    }
}

/** Desktop CrimboTrainCommand — throw Crimbo training manual via curse.php. */
internal fun GameRuntimeLibrary.cliCrimboTrain(parameters: String, print: (String) -> Unit) {
    val target = parameters.trim()
    if (target.isEmpty()) {
        print("Train whom?")
        return
    }
    if (preferences?.getBoolean("_crimboTraining", false) == true) {
        print("You've already trained somebody today.")
        return
    }
    if (httpClient == null) {
        print("HTTP client is not available.")
        return
    }
    print("Training $target...")
    val html = visitKolPost(
        "curse.php",
        "action=use&whichitem=$CRIMBO_TRAINING_MANUAL_ID&targetplayer=$target",
    )
    when {
        html == null -> print("Training failed.")
        html.contains("You've already trained somebody today", ignoreCase = true) -> {
            preferences?.setBoolean("_crimboTraining", true)
            print("You've already trained somebody today.")
        }
        html.contains("They already know that skill", ignoreCase = true) ->
            print("They already know that skill.")
        html.contains("You train", ignoreCase = true) -> {
            preferences?.setBoolean("_crimboTraining", true)
            print("You train $target.")
        }
        html.contains("That player could not be found", ignoreCase = true) ->
            print("$target could not be found.")
        else -> print("Training request sent to $target.")
    }
}

/** Desktop BadMoonCommand — zodiac / encounter pref status report. */
internal fun GameRuntimeLibrary.cliBadMoon(@Suppress("UNUSED_PARAMETER") parameters: String, print: (String) -> Unit) {
    val signName = character?.state?.value?.zodiacSign.orEmpty().ifBlank { "(unknown)" }
    val sign = ZodiacSign.find(signName)
    print("Zodiac sign: $signName")
    print("In Bad Moon: ${sign?.isBadMoon == true}")
    val prefs = preferences
    var have = 0
    for (i in 1..48) {
        val key = "badMoonEncounter" + i.toString().padStart(2, '0')
        val done = prefs?.getBoolean(key, false) == true
        if (done) have++
        print("$key: ${if (done) "have" else "NEED"}")
    }
    print("Special encounters completed: $have / 48")
}

/** Desktop FlickerCommand — flickering pixel pref status table. */
internal fun GameRuntimeLibrary.cliFlicker(@Suppress("UNUSED_PARAMETER") parameters: String, print: (String) -> Unit) {
    print("# | Location | Choice | Requirement | Status")
    FLICKERING_PIXELS.forEachIndexed { index, row ->
        val status = if (preferences?.getBoolean(row[0], false) == true) "have" else "NEED"
        print("${index + 1} | ${row[1]} | ${row[2]} | ${row[3]} | $status")
    }
}

private fun GameRuntimeLibrary.resolveCurseItem(query: String): net.sourceforge.kolmafia.data.ItemData? {
    val exact = ItemDatabase.getByPluralOrName(query)
        ?: gameDatabase?.item(query)
    if (exact != null) return exact
    val lower = query.lowercase()
    val matches = ItemDatabase.all().filter { it.name.lowercase().contains(lower) }
    return matches.singleOrNull()
}
