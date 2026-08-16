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
                val probe = chatProbe ?: client?.let { ChatProbe(it) }
                val profiles = mutableListOf<ProfileRequest>()
                for (name in parsed.targets) {
                    val id = resolvePvpTargetId(name, probe) ?: continue
                    val playerName = if (name.startsWith("#")) {
                        PlayerIdRegistry.getPlayerName(id)
                    } else {
                        name
                    }
                    print("Retrieving player data for $playerName...")
                    val profile = if (client != null) {
                        ProfileRequest.retrieve(client, playerName, id).getOrElse {
                            ProfileRequest(playerName = playerName, playerId = id)
                        }
                    } else {
                        ProfileRequest(playerName = playerName, playerId = id)
                    }
                    profiles += profile
                }
                val canInteract = character?.state?.value?.let { !it.isHardcore && !it.isInRonin } ?: true
                val mission = if (canInteract) "lootwhatever" else "flowers"
                PvpManager.executeDirectedPvpRequest(
                    targets = profiles,
                    mission = mission,
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
