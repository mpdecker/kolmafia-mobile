package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.request.HatterRequest
import net.sourceforge.kolmafia.request.LeafletRequest
import net.sourceforge.kolmafia.session.LeafletManager
import net.sourceforge.kolmafia.session.RabbitHoleAvailability
import net.sourceforge.kolmafia.session.RabbitHoleManager
import net.sourceforge.kolmafia.session.WumpusManager
import net.sourceforge.kolmafia.session.BastilleBattalionAdvisor
import net.sourceforge.kolmafia.session.BastilleBattleSimulation
import net.sourceforge.kolmafia.session.BastilleBoosts
import net.sourceforge.kolmafia.session.BastilleCheeseEncounter
import net.sourceforge.kolmafia.data.BastilleDatabase.Castle
import net.sourceforge.kolmafia.session.RumpleManager
import net.sourceforge.kolmafia.session.YouRobotManager

internal fun GameRuntimeLibrary.cliChess(parameters: String, print: (String) -> Unit) {
    val command = parameters.trim().lowercase().ifBlank { "board" }
    when (command.substringBefore(' ')) {
        "reset" -> {
            RabbitHoleManager.reset()
            print("Chessboard reset.")
        }
        "load" -> {
            val explicit = parameters.substringAfter(' ', "").trim()
            val board = if (explicit.isNotEmpty()) {
                RabbitHoleManager.load(explicit, save = true, preferences)
            } else {
                RabbitHoleManager.load(preferences)
            }
            print(board?.config() ?: "I haven't seen a chessboard recently.")
        }
        "board" -> print(
            RabbitHoleManager.boardConfig(preferences) ?: "I haven't seen a chessboard recently.",
        )
        "test" -> RabbitHoleManager.test(preferences)
            ?.forEach(print) ?: print("I couldn't solve the current board.")
        "step" -> {
            val (col, row) = RabbitHoleManager.step(preferences)
                ?: run {
                    print("I couldn't solve the current board.")
                    return
                }
            val request = choiceRequest ?: run {
                print("Choice request is not available.")
                return
            }
            runBlocking {
                request.choose(RabbitHoleManager.CHESS_CHOICE, 1, mapOf("xy" to "$col,$row"))
                    .onSuccess { (html, _) ->
                        RabbitHoleManager.parseChessMove("xy=$col,$row", html, preferences)
                            ?.let(print)
                    }
                    .onFailure { print(it.message ?: "Chess move failed.") }
            }
        }
        "solve" -> {
            val path = RabbitHoleManager.solve(preferences)
                ?: run {
                    print("I couldn't solve the current board.")
                    return
                }
            val request = choiceRequest ?: run {
                print("Choice request is not available.")
                return
            }
            runBlocking {
                for (square in path) {
                    val col = square % 8
                    val row = square / 8
                    val result = request.choose(
                        RabbitHoleManager.CHESS_CHOICE,
                        1,
                        mapOf("xy" to "$col,$row"),
                    )
                    val (html, _) = result.getOrElse {
                        print(it.message ?: "Chess move failed.")
                        return@runBlocking
                    }
                    RabbitHoleManager.parseChessMove("xy=$col,$row", html, preferences)
                        ?.let(print)
                }
            }
        }
        else -> print("Usage: chess reset|load [config]|board|test|step|solve")
    }
}

internal fun GameRuntimeLibrary.cliWumpus(parameters: String, print: (String) -> Unit) {
    when (parameters.trim().lowercase()) {
        "", "status" -> WumpusManager.printStatus().forEach(print)
        "code" -> print(WumpusManager.getWumpinatorCode())
        "reset" -> {
            WumpusManager.reset()
            print("Wumpus cave reset.")
        }
        else -> print("Usage: wumpus status|code|reset")
    }
}

internal fun GameRuntimeLibrary.cliLeaflet(parameters: String, print: (String) -> Unit) {
    val client = httpClient ?: run {
        print("HTTP client is not available.")
        return
    }
    val request = LeafletRequest(client)
    runBlocking {
        when (parameters.trim().lowercase()) {
            "location" -> LeafletManager.locationName(request::execute)
                .onSuccess { print("Current leaflet location: $it") }
                .onFailure { print(it.message ?: "Leaflet request failed.") }
            "", "stats" -> LeafletManager.robStrangeLeaflet(true, preferences, request::execute)
                .onSuccess { print("Strange leaflet completed.") }
                .onFailure { print(it.message ?: "Leaflet automation failed.") }
            "nomagic" -> LeafletManager.robStrangeLeaflet(false, preferences, request::execute)
                .onSuccess { print("Serpent-slaying quest complete.") }
                .onFailure { print(it.message ?: "Leaflet automation failed.") }
            else -> print("Usage: leaflet [nomagic|location]")
        }
    }
}

internal fun GameRuntimeLibrary.cliBastille(parameters: String, print: (String) -> Unit) {
    val prefs = preferences ?: run {
        print("Preferences are not available.")
        return
    }
    when (parameters.trim().lowercase()) {
        "test" -> {
            print("${BastilleCheeseEncounter.scalingEncounters().size} scaling cheese formulae loaded.")
            val stats = BastilleBattalionAdvisor.parseStats(prefs.getString("_bastilleStats"))
            val enemy = Castle.entries.firstOrNull { it.prefix == prefs.getString("_bastilleEnemyCastle") }
            if (enemy != null) {
                val battle = ((prefs.getInt("_bastilleGameTurn") + 2) / 3).coerceAtLeast(1)
                BastilleBattleSimulation.probabilities(
                    stats,
                    BastilleBoosts(prefs.getString("_bastilleBoosts")),
                    enemy,
                    battle,
                ).forEach { (stance, chance) ->
                    print("${stance.label}: ${(chance * 100).toInt()}%")
                }
            }
        }
        "", "status", "advise" -> {
            val choice = when {
                prefs.getString("_bastilleChoice1").isNotBlank() -> {
                    val first = prefs.getString("_bastilleChoice1")
                    if (BastilleCheeseEncounter.forName(first) != BastilleCheeseEncounter.unknown) 1319 else 1317
                }
                prefs.getString("_bastilleEnemyCastle").isNotBlank() -> 1315
                else -> 0
            }
            val advice = BastilleBattalionAdvisor.advise(choice, prefs)
            if (advice == null) {
                print("No Bastille choice is ready for advice.")
            } else {
                print("Bastille recommends option ${advice.option}: ${advice.reason}")
            }
        }
        "reset" -> {
            net.sourceforge.kolmafia.session.BastilleBattalionSync.reset(prefs)
            print("Bastille state reset.")
        }
        else -> print("Usage: bastille [status|advise|test|reset]")
    }
}

internal fun GameRuntimeLibrary.cliRobot(parameters: String, print: (String) -> Unit) {
    val prefs = preferences
    val state = character?.state?.value
    if (state?.inRobocore != true) {
        print("You are not a robot.")
        return
    }
    val command = parameters.trim().lowercase().ifBlank { "status" }
    when (command.substringBefore(' ')) {
        "status", "" -> YouRobotManager.statusLines(prefs, character).forEach(print)
        "restore" -> {
            YouRobotManager.restoreFromPreferences(prefs, skillManager)
            print("Restored You, Robot parts from preferences.")
            YouRobotManager.statusLines(prefs, character).forEach(print)
        }
        "chrono", "chronolith" -> {
            val before = state.youRobotEnergy
            print("Energy before chronolith: $before")
            visitKolPage("place.php?whichplace=scrapheap&action=sh_chronolith", applyQuestHooks = true)
            val after = character?.state?.value?.youRobotEnergy ?: prefs?.getInt("youRobotEnergy", 0) ?: 0
            print("Energy after chronolith: $after (next cost ${prefs?.getInt("_chronolithNextCost", 0) ?: 0})")
        }
        "power" -> {
            val before = state.youRobotEnergy
            print("Energy before collect: $before")
            visitKolPage("place.php?whichplace=scrapheap&action=sh_getpower", applyQuestHooks = true)
            val after = character?.state?.value?.youRobotEnergy ?: prefs?.getInt("youRobotEnergy", 0) ?: 0
            print("Energy after collect: $after")
        }
        else -> print("Usage: robot [status|restore|chrono|power]")
    }
}

internal fun GameRuntimeLibrary.cliRumple(parameters: String, print: (String) -> Unit) {
    val prefs = preferences
    val command = parameters.trim().lowercase().ifBlank { "status" }
    when (command.substringBefore(' ')) {
        "status", "" -> RumpleManager.statusLines(prefs).forEach(print)
        "advise" -> RumpleManager.advisorLines(prefs).forEach(print)
        "reset" -> {
            RumpleManager.reset(0, inventoryManager, prefs)
            print("Rumple materials and sins reset.")
        }
        else -> print("Usage: rumple [status|advise|reset]")
    }
}

internal fun GameRuntimeLibrary.resolveHatterLength(parameters: String): Int? {
    HatterRequest.parseLength(parameters)?.let { return it }
    val query = parameters.trim()
    if (query.isBlank()) return null
    val matches = ItemDatabase.all().filter { item ->
        item.primaryUse == net.sourceforge.kolmafia.data.ItemPrimaryUse.HAT &&
            item.name.contains(query, ignoreCase = true) &&
            (
                inventoryManager?.state?.value?.items?.get(item.id)?.quantity ?: 0
                ) > 0
    }
    return matches.singleOrNull()?.let { RabbitHoleAvailability.hatLength(it.name) }
}
