package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.campground.MushroomPlotSync
import net.sourceforge.kolmafia.character.MainStat
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.request.DwarfFactoryRequest
import net.sourceforge.kolmafia.quest.LightsOutChoiceSync
import net.sourceforge.kolmafia.quest.TalesOfDreadChoiceSync
import net.sourceforge.kolmafia.quest.TavernCellarSync
import net.sourceforge.kolmafia.session.TurnCounter

/** Phases 1033–1042 — quest-complete / basement / nemesis / Lights Out / Tales of Dread / mushroom field CLIs. */

private val PAPER_STRIPS = listOf(
    3144 to "torn paper strip",
    4138 to "rumpled paper strip",
    4139 to "creased paper strip",
    4140 to "folded paper strip",
    4141 to "crinkled paper strip",
    4142 to "crumpled paper strip",
    4143 to "ragged paper strip",
    4144 to "ripped paper strip",
)

private val TALE_MONSTERS = mapOf(
    "bugbear" to 1,
    "werewolf" to 6,
    "zombie" to 11,
    "ghost" to 16,
    "vampire" to 21,
    "skeleton" to 26,
)

private val TALE_ELEMENTS = mapOf(
    "hot" to 0,
    "cold" to 1,
    "spooky" to 2,
    "stench" to 3,
    "sleaze" to 4,
)

private const val TALES_OF_DREAD_ITEM_ID = 6423

private val TALE_STORY_PATTERN = Regex(
    """<div class=tiny style='position: absolute; top: 55; left: 365; width: 285; height: 485; overflow-y:scroll; '>(.*?)</div>""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)

internal fun GameRuntimeLibrary.cliTavern(parameters: String, print: (String) -> Unit) {
    cliTavernGoal(goal = '3', label = "Faucet", parameters = parameters, print = print)
}

internal fun GameRuntimeLibrary.cliBaron(parameters: String, print: (String) -> Unit) {
    cliTavernGoal(goal = '4', label = "Baron", parameters = parameters, print = print)
}

private fun GameRuntimeLibrary.cliTavernGoal(
    goal: Char,
    label: String,
    parameters: String,
    print: (String) -> Unit,
) {
    val prefs = preferences
    val ascension = character?.state?.value?.ascensionNumber ?: 0
    val refresh = parameters.trim().equals("refresh", ignoreCase = true) ||
        parameters.trim().equals("visit", ignoreCase = true)
    if (refresh || prefs == null) {
        visitKolPage("tavern.php?place=barkeep", applyQuestHooks = true)
        visitKolPage("cellar.php", applyQuestHooks = true)
    }
    val layout = if (prefs != null) {
        TavernCellarSync.tavernLayout(prefs, ascension)
    } else {
        ""
    }
    printTavernLayoutStatus(layout, goal, label, print)
}

private fun printTavernLayoutStatus(
    layout: String,
    goal: Char,
    label: String,
    print: (String) -> Unit,
) {
    if (layout.length != 25) {
        print("Tavern cellar layout unknown. Visit cellar.php or run: tavern refresh")
        return
    }
    val idx = layout.indexOf(goal)
    if (idx >= 0) {
        val row = idx / 5 + 1
        val col = idx % 5 + 1
        print("$label found in row $row, column $col (square ${idx + 1})")
        return
    }
    if (goal == '4') {
        val mansion = layout.indexOf('6')
        if (mansion >= 0) {
            val row = mansion / 5 + 1
            val col = mansion % 5 + 1
            print("Baron's empty mansion found in row $row, column $col (square ${mansion + 1})")
            print("You already defeated Baron von Ratsworth.")
            return
        }
    }
    val unexplored = layout.count { it == '0' }
    print("$label not yet located in recorded cellar layout ($unexplored unexplored squares).")
    print("Usage: ${if (goal == '3') "tavern" else "baron"} [refresh]")
}

internal fun GameRuntimeLibrary.cliGourd(parameters: String, print: (String) -> Unit) {
    val prefs = preferences
    val needed = prefs?.getInt("gourdItemCount", 5) ?: 5
    val itemId = gourdItemId()
    val itemName = ItemDatabase.getItemName(itemId).ifBlank { "gourd item #$itemId" }
    val have = inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0
    val arg = parameters.trim().lowercase()
    when {
        arg.isEmpty() || arg == "status" -> {
            print("Gourd quest needs $needed $itemName(s); you have $have.")
            print("Usage: gourd [trade|visit]")
        }
        arg == "visit" || arg == "refresh" -> {
            val html = visitKolPage("town_right.php?place=gourd", applyQuestHooks = true)
            if (html == null) {
                print("HTTP client is not available.")
                return
            }
            val updated = preferences?.getInt("gourdItemCount", needed) ?: needed
            print("Visited Captain of the Gourd (next trade wants $updated).")
        }
        arg == "trade" -> {
            // No GourdManager/GourdRequest yet — status + visit tip.
            print("Gourd auto-trade is not ported yet.")
            print("Next trade wants $needed $itemName(s); you have $have.")
            print("Visit with: gourd visit")
        }
        else -> print("Usage: gourd [status|trade|visit]")
    }
}

private fun GameRuntimeLibrary.gourdItemId(): Int =
    when (character?.state?.value?.characterClassEnum?.mainStat) {
        MainStat.MUSCLE -> 747 // knob firecracker
        MainStat.MYSTICALITY -> 559 // can lid
        else -> 27 // spider web
    }

internal fun GameRuntimeLibrary.cliDvorak(parameters: String, print: (String) -> Unit) {
    val arg = parameters.trim().lowercase()
    when (arg) {
        "", "status" -> {
            print("Dvorak tile puzzle solver is not ported yet.")
            print("Usage: dvorak [solve|step]")
        }
        "solve", "step" -> {
            print("Dvorak $arg is not available (DvorakManager not ported).")
            print("Navigate to tiles.php in-game to continue the puzzle.")
        }
        else -> print("Usage: dvorak [solve|step]")
    }
}

internal fun GameRuntimeLibrary.cliSven(parameters: String, print: (String) -> Unit) {
    val arg = parameters.trim()
    if (arg.isEmpty()) {
        print("Pandamonium Sven band solve is not ported yet.")
        print("Usage: sven member=item [member=item ...]")
        print("Visit pandamonium.php and talk to Sven first.")
        return
    }
    // Thin: accept give-list syntax but cannot POST without PandamoniumRequest.
    print("Sven solve HTTP is not available yet.")
    print("Requested gifts: $arg")
    visitKolPage("pandamonium.php", applyQuestHooks = true)
        ?: print("(Optional) Visit pandamonium.php once HTTP is connected.")
}

internal fun GameRuntimeLibrary.cliBasement(parameters: String, print: (String) -> Unit) {
    val fromPref = preferences?.getInt("basementLevel", -1)?.takeIf { it >= 0 }
    val level = fromPref
        ?: net.sourceforge.kolmafia.request.BasementSync.basementLevel.takeIf { it > 0 }
        ?: buildMonsterExpressionContext().basementLevel
    print("Fernswarthy's Basement (Level $level)")
    val summary = net.sourceforge.kolmafia.request.BasementSync.getBasementLevelSummary()
    if (summary.isNotBlank()) print(summary)
    val arg = parameters.trim()
    if (arg.equals("visit", ignoreCase = true) ||
        arg.equals("refresh", ignoreCase = true) ||
        arg.equals("check", ignoreCase = true) ||
        arg.isEmpty()
    ) {
        if (arg.equals("check", ignoreCase = true) || arg.isEmpty()) {
            // Prefer live check when HTTP available; otherwise print cached summary.
        }
        if (arg.equals("visit", ignoreCase = true) ||
            arg.equals("refresh", ignoreCase = true) ||
            arg.equals("check", ignoreCase = true)
        ) {
            visitKolPage("basement.php", applyQuestHooks = true)
                ?: print("HTTP client is not available for basement refresh.")
            val after = net.sourceforge.kolmafia.request.BasementSync
            print(after.getBasementLevelName())
            after.getBasementLevelSummary().takeIf { it.isNotBlank() }?.let(print)
            after.basementErrorMessage?.let(print)
        }
    }
}

internal fun GameRuntimeLibrary.cliDwarfFactory(parameters: String, print: (String) -> Unit) {
    val prefs = preferences ?: run {
        print("Preferences unavailable.")
        return
    }
    val tokens = parameters.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) {
        print("Usage: dwarf check|report|setdigits|solve|vacuum <item>")
        return
    }
    when (tokens[0].lowercase()) {
        "check" -> DwarfFactoryRequest.check(prefs, print)
        "solve" -> DwarfFactoryRequest.solve(prefs, print)
        "report" -> {
            if (tokens.size >= 2) {
                DwarfFactoryRequest.report(tokens[1].trim().uppercase(), prefs, print)
            } else {
                DwarfFactoryRequest.report(prefs, print)
            }
        }
        "setdigits" -> {
            val digits = tokens.getOrNull(1)?.trim()?.uppercase().orEmpty()
            if (digits.length != 7) {
                print("Must supply a 7 character digit string")
            } else {
                DwarfFactoryRequest.setDigits(digits, prefs)
                print("Digit runes set to $digits")
            }
        }
        "vacuum" -> {
            val itemString = parameters.substringAfter("vacuum").trim()
            if (itemString.isEmpty()) {
                print("Usage: dwarf vacuum <item>")
                return
            }
            val itemId = gameDatabase?.item(itemString)?.id
                ?: ItemDatabase.getByName(itemString)?.id
            if (itemId == null || itemId <= 0) {
                print("Unable to find item: $itemString")
                return
            }
            visitKolPage(
                "dwarfcontraption.php?action=dochamber&howmany=1&whichitem=$itemId",
                applyQuestHooks = true,
            ) ?: print("HTTP client is not available for vacuum chamber.")
        }
        else -> print("Usage: dwarf check|report|setdigits|solve|vacuum <item>")
    }
}

internal fun GameRuntimeLibrary.cliNemesis(parameters: String, print: (String) -> Unit) {
    val tokens = parameters.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) {
        print("Usage: nemesis password | strips")
        return
    }
    when (tokens[0].lowercase()) {
        "password" -> {
            val password = nemesisPasswordFromPrefs()
            if (password == null) {
                print("You don't have all the paper strips identified.")
                print("Run: nemesis strips")
            } else {
                print(password)
            }
        }
        "strips" -> {
            for ((id, fallbackName) in PAPER_STRIPS) {
                val name = ItemDatabase.getItemName(id).ifBlank { fallbackName }
                val idStr = preferences?.getString("lastPaperStrip$id", "").orEmpty()
                print("$name = ${idStr.ifBlank { "(unknown)" }}")
            }
        }
        else -> print("Usage: nemesis password | strips")
    }
}

private fun GameRuntimeLibrary.nemesisPasswordFromPrefs(): String? {
    val prefs = preferences ?: return null
    val strips = PAPER_STRIPS.map { (id, _) ->
        val parts = prefs.getString("lastPaperStrip$id", "").split(":")
        if (parts.size != 3) return null
        Triple(parts[0], parts[1], parts[2])
    }
    val leftMap = strips.associateBy { it.first }
    val rightKeys = strips.map { it.third }.toSet()
    val first = strips.firstOrNull { it.first !in rightKeys } ?: return null
    val ordered = mutableListOf(first)
    var current = first
    repeat(strips.size - 1) {
        val next = leftMap[current.third] ?: return null
        ordered.add(next)
        current = next
    }
    return ordered.joinToString("") { it.second }
}

internal fun GameRuntimeLibrary.cliSpookyraven(parameters: String, print: (String) -> Unit) {
    val prefs = preferences
    val arg = parameters.trim().lowercase()
    when (arg) {
        "" -> {
            val elizabeth = prefs?.getString(LightsOutChoiceSync.ELIZABETH_PREF, "")
                ?.ifBlank { "The Haunted Storage Room" }
                .orEmpty()
            val stephen = prefs?.getString(LightsOutChoiceSync.STEPHEN_PREF, "")
                ?.ifBlank { "The Haunted Bedroom" }
                .orEmpty()
            if (elizabeth == "none") {
                print("You have defeated Elizabeth Spookyraven")
            } else {
                print("Elizabeth will next show up in $elizabeth")
            }
            if (stephen == "none") {
                print("You have defeated Stephen Spookyraven")
            } else {
                print("Stephen will next show up in $stephen")
            }
        }
        "on" -> {
            prefs?.setBoolean("trackLightsOut", true)
            print("Spookyraven Lights Out tracking enabled.")
            ensureLightsOutCounter(print)
        }
        "off" -> {
            prefs?.setBoolean("trackLightsOut", false)
            prefs?.let { TurnCounter.stopCounting(it, LightsOutChoiceSync.COUNTER_LABEL) }
            print("Spookyraven Lights Out tracking disabled.")
        }
        "elizabeth" -> {
            prefs?.setString(LightsOutChoiceSync.ELIZABETH_PREF, "none")
            print("Marked Elizabeth Spookyraven quest complete.")
        }
        "stephen" -> {
            prefs?.setString(LightsOutChoiceSync.STEPHEN_PREF, "none")
            print("Marked Stephen Spookyraven quest complete.")
        }
        else -> print("Usage: spookyraven [on|off|elizabeth|stephen]")
    }
}

private fun GameRuntimeLibrary.ensureLightsOutCounter(print: (String) -> Unit) {
    val prefs = preferences ?: return
    if (!prefs.getBoolean("trackLightsOut", false)) return
    val turnsPlayed = character?.state?.value?.turnsPlayed ?: 0
    if (TurnCounter.isCounting(prefs, LightsOutChoiceSync.COUNTER_LABEL, turnsPlayed)) return
    val elizabeth = prefs.getString(LightsOutChoiceSync.ELIZABETH_PREF, "")
    val stephen = prefs.getString(LightsOutChoiceSync.STEPHEN_PREF, "")
    if (elizabeth == "none" && stephen == "none") return
    val turns = 37 - (turnsPlayed % 37).let { if (it == 0) 37 else it }
    TurnCounter.startCounting(
        prefs,
        currentRun = turnsPlayed,
        turns = turns,
        label = LightsOutChoiceSync.COUNTER_LABEL,
        image = "bulb.gif",
    )
    print("Lights Out counter started ($turns turns).")
}

internal fun GameRuntimeLibrary.cliTaleOfDread(parameters: String, print: (String) -> Unit) {
    val parts = parameters.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (parts.size < 2) {
        print("Syntax: taleofdread element monster")
        print("Elements: hot cold spooky stench sleaze")
        print("Monsters: bugbear werewolf zombie ghost vampire skeleton")
        return
    }
    val element = parts[0].lowercase()
    val monster = parts[1].lowercase()
    val base = TALE_MONSTERS[monster]
    if (base == null) {
        print("What kind of dreadful monster is a '${parts[1]}'?")
        return
    }
    val offset = TALE_ELEMENTS[element]
    if (offset == null) {
        print("What kind of element is '${parts[0]}'?")
        return
    }
    val story = base + offset
    val haveBook = (inventoryManager?.state?.value?.items?.get(TALES_OF_DREAD_ITEM_ID)?.quantity ?: 0) > 0
    if (!haveBook) {
        print("You don't own the Tales of Dread")
        return
    }
    val useReq = useItemRequest
    val choice = choiceRequest
    if (useReq == null || choice == null) {
        print("Tales of Dread story #$story ($element $monster).")
        print("HTTP helpers unavailable; open choice ${TalesOfDreadChoiceSync.CHOICE_ID} with whichstory=$story")
        return
    }
    runBlocking {
        useReq.use(TALES_OF_DREAD_ITEM_ID, 1).exceptionOrNull()?.let {
            print(it.message ?: "Failed to use Tales of Dread.")
            return@runBlocking
        }
        choice.choose(
            TalesOfDreadChoiceSync.CHOICE_ID,
            1,
            mapOf("whichstory" to story.toString()),
        ).onSuccess { (html, _) ->
            TalesOfDreadChoiceSync.apply(TalesOfDreadChoiceSync.CHOICE_ID)
            val tale = extractTaleOfDread(html)
            if (tale.isBlank()) {
                print("Opened Tales of Dread story #$story.")
            } else {
                print(tale)
            }
        }.onFailure { print(it.message ?: "Failed to read tale.") }
    }
}

private fun extractTaleOfDread(html: String): String {
    val raw = TALE_STORY_PATTERN.find(html)?.groupValues?.getOrNull(1) ?: return ""
    return raw.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("<[^>]+>"), "")
        .trim()
}

internal fun GameRuntimeLibrary.cliField(parameters: String, print: (String) -> Unit) {
    val prefs = preferences
    val tokens = parameters.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val command = tokens.getOrNull(0)?.lowercase().orEmpty()
    when (command) {
        "plant" -> {
            if (tokens.size < 3) {
                print("Syntax: field plant square spore")
                return
            }
            print("Mushroom plant HTTP is not ported yet.")
            print("Would plant ${tokens.drop(2).joinToString(" ")} in square ${tokens[1]}.")
        }
        "pick" -> {
            if (tokens.size < 2) {
                print("Syntax: field pick square")
                return
            }
            print("Mushroom pick HTTP is not ported yet.")
            print("Would pick square ${tokens[1]}.")
        }
        "harvest" -> print("Mushroom harvest HTTP is not ported yet.")
        "refresh", "visit" -> {
            visitKolPage("knoll_mushrooms.php", applyQuestHooks = true)
                ?: print("HTTP client is not available.")
        }
        "", "status" -> { /* fall through to plot dump */ }
        else -> {
            print("Usage: field [plant <square> <spore> | pick <square> | harvest | refresh]")
            return
        }
    }
    val lastPlot = prefs?.getInt("lastMushroomPlot", -1) ?: -1
    val ascension = character?.state?.value?.ascensionNumber ?: 0
    if (lastPlot != ascension) {
        print("No mushroom plot recorded for this ascension. Visit knoll_mushrooms.php or: field refresh")
    }
    val grid = MushroomPlotSync.plotGrid(prefs)
    print("Current:")
    for (row in 0 until 4) {
        print(grid[row].joinToString(" "))
    }
}
