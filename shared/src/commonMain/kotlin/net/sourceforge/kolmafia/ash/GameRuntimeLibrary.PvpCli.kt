package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.chat.ChatHtmlParser
import net.sourceforge.kolmafia.chat.ChatProbe
import net.sourceforge.kolmafia.chat.PlayerIdRegistry
import net.sourceforge.kolmafia.request.ProfileRequest
import net.sourceforge.kolmafia.session.PvpManager

internal sealed class PvpStealParseResult {
    data class Run(
        val attacks: Int,
        val tougher: Boolean,
        val mission: String,
        val missionType: String,
        val stance: Int,
        val stanceName: String,
    ) : PvpStealParseResult()

    data class Error(val message: String) : PvpStealParseResult()
}

/** Desktop [net.sourceforge.kolmafia.textui.command.PvpStealCommand] argument parse. */
internal object PvpStealParser {
    fun parse(parameters: String, canInteract: Boolean): PvpStealParseResult {
        val trimmed = parameters.trim()
        if (trimmed.isEmpty()) {
            return PvpStealParseResult.Error("Must specify both mission and stance")
        }
        val tokens = trimmed.split(Regex("\\s+")).toMutableList()
        if (tokens.size < 2) {
            return PvpStealParseResult.Error("Must specify both mission and stance")
        }

        var attacks = 0
        var tougher = false
        var index = 0

        tokens[0].toIntOrNull()?.let { value ->
            attacks = value
            index++
            if (tokens.size - index < 2) {
                return PvpStealParseResult.Error("Must specify both mission and stance")
            }
        }

        val maybeRank = tokens[index].lowercase()
        if (maybeRank == "random" || maybeRank == "tougher") {
            tougher = maybeRank == "tougher"
            index++
            if (tokens.size - index < 2) {
                return PvpStealParseResult.Error("Must specify both mission and stance")
            }
        }

        val missionType = tokens[index]
        val mission = when {
            missionType.equals("flowers", ignoreCase = true) ||
                missionType.equals("fame", ignoreCase = true) -> missionType.lowercase()
            missionType.lowercase().startsWith("loot") -> {
                if (!canInteract) {
                    return PvpStealParseResult.Error("You cannot attack for loot now.")
                }
                "lootwhatever"
            }
            else -> return PvpStealParseResult.Error("What do you want to steal?")
        }
        index++
        if (index >= tokens.size) {
            return PvpStealParseResult.Error("Must specify both mission and stance")
        }

        val stanceString = tokens.subList(index, tokens.size).joinToString(" ")
        val stance: Int
        val stanceName: String
        val asNumber = stanceString.toIntOrNull()
        if (asNumber != null) {
            val name = PvpManager.findStance(asNumber)
                ?: return PvpStealParseResult.Error("$asNumber is not a valid stance")
            stance = asNumber
            stanceName = name
        } else {
            val found = PvpManager.findStance(stanceString)
            if (found < 0) {
                return PvpStealParseResult.Error(
                    "\"$stanceString\" does not uniquely match a currently known stance",
                )
            }
            stance = found
            stanceName = PvpManager.findStance(found) ?: stanceString
        }

        return PvpStealParseResult.Run(
            attacks = attacks,
            tougher = tougher,
            mission = mission,
            missionType = missionType,
            stance = stance,
            stanceName = stanceName,
        )
    }
}

private val familiarStealArgsPattern = Regex("""^\d+\s+\S""")

/**
 * Route `steal N item` to familiar steal when args are not a PvP mission.
 *
 * Desktop `steal` is PvP-only ([net.sourceforge.kolmafia.textui.command.PvpStealCommand]).
 * Mobile intentionally preserves Phase 25 `steal N item` familiar-steal as a dual-route
 * when [PvpStealParser] rejects the args as a non-mission ("What do you want to steal?")
 * and the args look like `qty item`. Prefer [cliFamiliarSteal] / `famsteal` / `familiarsteal`
 * when calling familiar steal explicitly.
 */
internal fun shouldFallbackToFamiliarSteal(parameters: String): Boolean {
    val trimmed = parameters.trim()
    if (trimmed.isEmpty()) return false
    val parsed = PvpStealParser.parse(trimmed, canInteract = true)
    if (parsed !is PvpStealParseResult.Error) return false
    if (parsed.message != "What do you want to steal?") return false
    return familiarStealArgsPattern.containsMatchIn(trimmed)
}

internal fun GameRuntimeLibrary.cliSteal(parameters: String, rt: AshRuntimeContext) {
    if (shouldFallbackToFamiliarSteal(parameters)) {
        rt.print("Routing to familiar steal (desktop steal is PvP-only; mobile preserves steal N item).")
        cliFamiliarSteal(parameters)
        return
    }
    cliPvp(parameters, rt::print)
}

/** Explicit familiar-steal CLI (`famsteal` / `familiarsteal`); same path as steal dual-route fallback. */
internal fun GameRuntimeLibrary.cliFamiliarStealAlias(parameters: String, rt: AshRuntimeContext) {
    if (parameters.isBlank()) {
        rt.print("Usage: famsteal N item")
        return
    }
    cliFamiliarSteal(parameters)
}

/** Mission for ASH/CLI directed PvP when stance/mission are not specified. */
internal fun GameRuntimeLibrary.defaultPvpMission(): String {
    val canInteract = character?.state?.value?.let { !it.isHardcore && !it.isInRonin } ?: true
    return if (canInteract) "lootwhatever" else "flowers"
}

internal fun GameRuntimeLibrary.defaultPvpStance(): Int = PvpManager.defaultStanceOption()

/**
 * Resolve a player name/id to a [ProfileRequest] using the same helpers as CLI `attack`.
 * Returns null when the target cannot be resolved.
 */
internal suspend fun GameRuntimeLibrary.resolveDirectedPvpTarget(player: String): ProfileRequest? {
    val trimmed = player.trim()
    if (trimmed.isEmpty()) return null
    val client = httpClient
    val probe = chatProbe ?: client?.let { ChatProbe(it) }
    val id = resolvePvpTargetId(trimmed, probe) ?: return null
    val playerName = if (trimmed.startsWith("#") || trimmed.all { it.isDigit() }) {
        PlayerIdRegistry.getPlayerName(id).ifEmpty { trimmed }
    } else {
        trimmed
    }
    return if (client != null) {
        ProfileRequest.retrieve(client, playerName, id).getOrElse {
            ProfileRequest(playerName = playerName, playerId = id)
        }
    } else {
        ProfileRequest(playerName = playerName, playerId = id)
    }
}

/**
 * Mobile convenience ASH (non-desktop): resolve [player] → prefetch stances →
 * mission `lootwhatever` if canInteract else `flowers` → stance 0 or first known →
 * [PvpManager.executeDirectedPvpRequest]. Returns true iff completed without abort.
 */
internal fun GameRuntimeLibrary.runAshPvpAttack(player: String): Boolean {
    val client = httpClient ?: return false
    val char = character ?: return false
    return runBlocking {
        if (!PvpManager.checkStances(client, char, preferences, sessionLogger, inventoryManager)) {
            return@runBlocking false
        }
        val profile = resolveDirectedPvpTarget(player) ?: return@runBlocking false
        val mission = defaultPvpMission()
        val stance = defaultPvpStance()
        PvpManager.executeDirectedPvpRequest(
            targets = listOf(profile),
            mission = mission,
            stance = stance,
            client = client,
            character = char,
            preferences = preferences,
            sessionLogger = sessionLogger,
            cliExecutor = { cmd -> dispatchCli(cmd, object : AshRuntimeContext {
                override fun print(msg: String) = Unit
            }) },
            print = {},
            inventoryManager = inventoryManager,
        )
        PvpManager.abortReason == null
    }
}

/**
 * Mobile convenience ASH (non-desktop): one tougher/ranked random flower fight
 * (`ranked=2` / `tougher=true`) via [PvpManager.executePvpRequest].
 * Returns true iff completed without abort.
 */
internal fun GameRuntimeLibrary.runAshRankedFam(): Boolean {
    val client = httpClient ?: return false
    val char = character ?: return false
    return runBlocking {
        if (!PvpManager.checkStances(client, char, preferences, sessionLogger, inventoryManager)) {
            return@runBlocking false
        }
        PvpManager.executePvpRequest(
            attacks = 1,
            mission = "flowers",
            stance = defaultPvpStance(),
            tougher = true,
            client = client,
            character = char,
            preferences = preferences,
            sessionLogger = sessionLogger,
            cliExecutor = { cmd -> dispatchCli(cmd, object : AshRuntimeContext {
                override fun print(msg: String) = Unit
            }) },
            print = {},
            inventoryManager = inventoryManager,
        )
        PvpManager.abortReason == null
    }
}

internal fun GameRuntimeLibrary.cliFamiliarSteal(parameters: String) {
    val match = Regex("""^(\d+)\s+(.+)$""").find(parameters.trim()) ?: return
    val qty = match.groupValues[1].toIntOrNull() ?: return
    val itemId = gameDatabase?.item(match.groupValues[2].trim())?.id ?: return
    val req = familiarRequest ?: return
    runBlocking {
        repeat(qty) {
            if (req.stealItem(itemId).isFailure) return@runBlocking
            inventoryManager?.fetchInventory()
        }
    }
}

internal fun GameRuntimeLibrary.cliPvp(parameters: String, print: (String) -> Unit) {
    val client = httpClient
    runBlocking {
        if (!PvpManager.checkStances(client, character, preferences, sessionLogger, inventoryManager)) {
            print("Cannot determine valid stances")
            return@runBlocking
        }
        if (parameters.isBlank()) {
            for ((option, name) in PvpManager.optionToStance) {
                print("$option: $name")
            }
            return@runBlocking
        }
        val canInteract = character?.state?.value?.let { !it.isHardcore && !it.isInRonin } ?: true
        when (val parsed = PvpStealParser.parse(parameters, canInteract)) {
            is PvpStealParseResult.Error -> print(parsed.message)
            is PvpStealParseResult.Run -> {
                val countLabel = if (parsed.attacks == 0) "all remaining" else parsed.attacks.toString()
                print("Use $countLabel PVP attacks to steal ${parsed.missionType} via ${parsed.stanceName}")
                PvpManager.executePvpRequest(
                    attacks = parsed.attacks,
                    mission = parsed.mission,
                    stance = parsed.stance,
                    tougher = parsed.tougher,
                    client = client,
                    character = character,
                    preferences = preferences,
                    sessionLogger = sessionLogger,
                    cliExecutor = { cmd -> dispatchCli(cmd, print.asRuntimeContext()) },
                    print = print,
                    inventoryManager = inventoryManager,
                )
            }
        }
    }
}

internal fun GameRuntimeLibrary.cliFlowers(print: (String) -> Unit) {
    val client = httpClient ?: run {
        print("HTTP client is not available.")
        return
    }
    runBlocking {
        PvpManager.executePvpRequest(
            attacks = 0,
            mission = "flowers",
            stance = 0,
            tougher = false,
            client = client,
            character = character,
            preferences = preferences,
            sessionLogger = sessionLogger,
            cliExecutor = { cmd -> dispatchCli(cmd, print.asRuntimeContext()) },
            print = print,
            inventoryManager = inventoryManager,
        )
    }
}

internal sealed class PvpAttackParseResult {
    data object ListStances : PvpAttackParseResult()
    data class Run(val targets: List<String>, val stance: Int, val stanceName: String) : PvpAttackParseResult()
    data class Error(val message: String) : PvpAttackParseResult()
}

/** Desktop [net.sourceforge.kolmafia.textui.command.PvpAttackCommand] argument parse. */
internal object PvpAttackParser {
    fun parse(parameters: String): PvpAttackParseResult {
        val trimmed = parameters.trim()
        if (trimmed.isEmpty()) return PvpAttackParseResult.ListStances
        val parts = trimmed.split("stance=", limit = 2)
        if (parts.size < 2) {
            return PvpAttackParseResult.Error("You must specify stance=STANCE")
        }
        val stanceString = parts[1].trim()
        val stance: Int
        val stanceName: String
        val asNumber = stanceString.toIntOrNull()
        if (asNumber != null) {
            val name = PvpManager.findStance(asNumber)
                ?: return PvpAttackParseResult.Error("$asNumber is not a valid stance")
            stance = asNumber
            stanceName = name
        } else {
            val found = PvpManager.findStance(stanceString)
            if (found < 0) {
                return PvpAttackParseResult.Error(
                    "\"$stanceString\" does not uniquely match a currently known stance",
                )
            }
            stance = found
            stanceName = PvpManager.findStance(found) ?: stanceString
        }
        val names = parts[0].trim().split(',').map { it.trim() }.filter { it.isNotEmpty() }
        return PvpAttackParseResult.Run(targets = names, stance = stance, stanceName = stanceName)
    }
}

internal val WHOIS_ID_PATTERN = Regex("""\(#(\d+)\)""")

internal suspend fun resolvePvpTargetId(name: String, chatProbe: ChatProbe?): String? {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return null
    if (trimmed.startsWith("#")) {
        return trimmed.substring(1).takeIf { it.isNotEmpty() }
    }
    if (trimmed.all { it.isDigit() }) return trimmed
    val cached = PlayerIdRegistry.getPlayerId(trimmed)
    if (cached != trimmed && cached.all { it.isDigit() }) return cached
    if (chatProbe == null) return null
    val body = chatProbe.sendInternalCommand("/whois $trimmed").getOrNull() ?: return null
    ChatHtmlParser.parsePlayerIds(body)
    val after = PlayerIdRegistry.getPlayerId(trimmed)
    if (after != trimmed && after.all { it.isDigit() }) return after
    val parsed = WHOIS_ID_PATTERN.find(body)?.groupValues?.get(1) ?: return null
    PlayerIdRegistry.register(trimmed, parsed)
    return parsed
}

internal fun GameRuntimeLibrary.cliPvpAttack(parameters: String, rt: AshRuntimeContext) {
    val print = rt::print
    val client = httpClient
    runBlocking {
        if (!PvpManager.checkStances(client, character, preferences, sessionLogger, inventoryManager)) {
            print("Cannot determine valid stances")
            return@runBlocking
        }
        when (val parsed = PvpAttackParser.parse(parameters)) {
            is PvpAttackParseResult.ListStances -> {
                for ((option, name) in PvpManager.optionToStance) {
                    print("$option: $name")
                }
            }
            is PvpAttackParseResult.Error -> print(parsed.message)
            is PvpAttackParseResult.Run -> {
                val profiles = mutableListOf<ProfileRequest>()
                for (name in parsed.targets) {
                    print("Retrieving player data for $name...")
                    val profile = resolveDirectedPvpTarget(name) ?: continue
                    profiles += profile
                }
                PvpManager.executeDirectedPvpRequest(
                    targets = profiles,
                    mission = defaultPvpMission(),
                    stance = parsed.stance,
                    client = client,
                    character = character,
                    preferences = preferences,
                    sessionLogger = sessionLogger,
                    cliExecutor = { cmd -> dispatchCli(cmd, rt) },
                    print = print,
                    inventoryManager = inventoryManager,
                )
            }
        }
    }
}

private fun ((String) -> Unit).asRuntimeContext(): AshRuntimeContext =
    object : AshRuntimeContext {
        override fun print(msg: String) = this@asRuntimeContext(msg)
    }
