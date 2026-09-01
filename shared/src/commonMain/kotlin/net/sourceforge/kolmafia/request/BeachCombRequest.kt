package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.BeachCombChoiceSync
import net.sourceforge.kolmafia.session.ChoiceCombatAshState
import net.sourceforge.kolmafia.session.BeachHeadAvailability
import net.sourceforge.kolmafia.session.BeachCombManager
import net.sourceforge.kolmafia.session.BeachManager
import net.sourceforge.kolmafia.session.EquipmentManager
import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.SessionLogger
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.adventure.choice.ChoiceWalkAway

/** Desktop [BeachCombRequest] — Beach Comb head buffs (choice 1388). */
class BeachCombRequest(
    private val client: HttpClient,
    private val choiceRequest: ChoiceRequest,
    private val character: KoLCharacter? = null,
    private val equipmentManager: EquipmentManager? = null,
    private val equipmentRequest: EquipmentRequest? = null,
    private val sessionLogger: SessionLogger? = null,
) {
    enum class Command(val option: Int) {
        VISIT(0),
        WANDER(1),
        RANDOM(2),
        HEAD(3),
        COMB(4),
        EXIT(5),
        COMMON(6),
        PRINT(-1),
    }

    data class ParsedCommand(
        val command: Command,
        val minutes: Int? = null,
        val row: Int? = null,
        val column: Int? = null,
        val query: String? = null,
    )

    private fun activeBeachChoice(): Boolean =
        ChoiceCombatAshState.handlingChoice &&
            ChoiceCombatAshState.lastChoice == CHOICE_ID

    private fun validateChoiceState(): Result<Unit> {
        if (ChoiceCombatAshState.currentRound > 0) {
            return Result.failure(IllegalStateException("You cannot use the Beach Comb during a fight."))
        }
        if (ChoiceCombatAshState.handlingChoice &&
            ChoiceCombatAshState.lastChoice != CHOICE_ID &&
            !ChoiceWalkAway.canWalkFromChoice(ChoiceCombatAshState.lastChoice)
        ) {
            return Result.failure(IllegalStateException("You are currently in a choice."))
        }
        return Result.success(Unit)
    }

    private suspend fun choose(
        option: Int,
        fields: Map<String, String> = emptyMap(),
        preferences: Preferences,
    ): Result<String> {
        val choiceUrl = buildString {
            append("choice.php?whichchoice=$CHOICE_ID&option=$option")
            fields.forEach { (key, value) -> append("&$key=$value") }
        }
        RequestLogger.registerRequest(choiceUrl, sessionLogger, preferences, fields)
        return choiceRequest.choose(CHOICE_ID, option, fields).map { (html, finalUrl) ->
            BeachCombChoiceSync.apply(
                CHOICE_ID,
                option,
                html,
                preferences,
                finalUrl.ifBlank { choiceUrl },
                sessionLogger,
            )
            html
        }
    }

    private suspend fun ensureCombEquipped(
        inventoryCounts: (Int) -> Int,
    ): Result<Unit> {
        if (equipmentManager?.hasEquipped(BeachHeadAvailability.BEACH_COMB_ID) == true ||
            equipmentManager?.hasEquipped(BeachHeadAvailability.DRIFTWOOD_BEACH_COMB_ID) == true
        ) {
            return Result.success(Unit)
        }
        val combId = when {
            inventoryCounts(BeachHeadAvailability.BEACH_COMB_ID) > 0 ->
                BeachHeadAvailability.BEACH_COMB_ID
            inventoryCounts(BeachHeadAvailability.DRIFTWOOD_BEACH_COMB_ID) > 0 ->
                BeachHeadAvailability.DRIFTWOOD_BEACH_COMB_ID
            else -> return Result.failure(
                IllegalStateException("You need either a Beach Comb or a driftwood beach comb"),
            )
        }
        if (equipmentManager?.hasEquipped(combId) != false) return Result.success(Unit)
        val equip = equipmentRequest ?: return Result.success(Unit)
        return equip.equipItem(combId, EquipmentSlot.OFFHAND).map { Unit }
    }

    suspend fun combHead(
        query: String,
        preferences: Preferences?,
        inventoryCounts: (Int) -> Int,
    ): Result<String> {
        val prefs = preferences
            ?: return Result.failure(IllegalStateException("Preferences are not available."))
        val head = BeachHeadAvailability.resolveHead(query)
            ?: return Result.failure(
                IllegalArgumentException("Which beach head is $query?"),
            )
        validateChoiceState().getOrElse { return Result.failure(it) }
        ensureCombEquipped(inventoryCounts).getOrElse { return Result.failure(it) }
        if (head.id in BeachHeadAvailability.parseBeachHeadsUsed(prefs)) {
            return Result.failure(
                IllegalStateException("You've already combed beach head #${head.id}"),
            )
        }
        return try {
            val visit = client.get("$KOL_BASE_URL/main.php") {
                parameter("comb", "1")
            }
            if (!visit.status.isSuccess()) {
                return Result.failure(IllegalStateException("Beach Comb visit failed."))
            }
            val visitHtml = visit.bodyAsText()
            BeachCombChoiceSync.apply(
                CHOICE_ID,
                0,
                visitHtml,
                prefs,
                "main.php?comb=1",
                sessionLogger,
            )
            val unlocked = BeachHeadAvailability.parseBeachHeadsUnlocked(prefs)
            val html = if (head.id in unlocked) {
                choose(HEAD_OPTION, mapOf("buff" to head.id.toString()), prefs)
                    .getOrElse { return Result.failure(it) }
            } else {
                choose(WANDER_OPTION, mapOf("minutes" to head.beach.toString()), prefs)
                    .getOrElse { return Result.failure(it) }
                choose(COMB_OPTION, mapOf("coords" to head.coords), prefs)
                    .getOrElse { return Result.failure(it) }
            }
            choose(EXIT_OPTION, preferences = prefs).getOrElse { return Result.failure(it) }
            BeachHeadAvailability.markHeadUsed(prefs, head.id)
            Result.success(html)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun execute(
        parsed: ParsedCommand,
        preferences: Preferences?,
        inventoryCounts: (Int) -> Int,
    ): Result<String> {
        val prefs = preferences
            ?: return Result.failure(IllegalStateException("Preferences are not available."))
        if (parsed.command == Command.HEAD) {
            return combHead(parsed.query.orEmpty(), prefs, inventoryCounts)
        }
        if (parsed.command == Command.PRINT) {
            return Result.success(formatLayout(prefs))
        }
        validateChoiceState().getOrElse { return Result.failure(it) }
        if (!activeBeachChoice()) {
            ensureCombEquipped(inventoryCounts).getOrElse { return Result.failure(it) }
        }
        return try {
            if (parsed.command == Command.VISIT) {
                if (activeBeachChoice()) return Result.success("")
                val response = client.get("$KOL_BASE_URL/main.php") { parameter("comb", "1") }
                if (!response.status.isSuccess()) {
                    return Result.failure(IllegalStateException("Beach Comb visit failed."))
                }
                val html: String = response.body()
                BeachCombChoiceSync.apply(
                    CHOICE_ID,
                    0,
                    html,
                    prefs,
                    "main.php?comb=1",
                    sessionLogger,
                )
                return Result.success(html)
            }
            if (!activeBeachChoice()) {
                return Result.failure(
                    IllegalStateException("You have not VISITed the Beach Comb yet."),
                )
            }
            if (parsed.command == Command.COMMON &&
                prefs.getInt("_freeBeachWalksUsed", 0) > 1
            ) {
                return Result.failure(
                    IllegalStateException(
                        "You must have 10 free wanders available to claim all common items.",
                    ),
                )
            }
            val fields = when (parsed.command) {
                Command.WANDER -> mapOf("minutes" to parsed.minutes.toString())
                Command.COMB -> {
                    if (!prefs.getBoolean("_beachCombing", false)) {
                        return Result.failure(
                            IllegalStateException("Visit a square on the beach before you comb it."),
                        )
                    }
                    val layout = BeachManager.stringToLayout(
                        prefs.getString("_beachLayout", ""),
                    )
                    val row = parsed.row ?: return Result.failure(
                        IllegalArgumentException("A beach row is required."),
                    )
                    val column = parsed.column ?: return Result.failure(
                        IllegalArgumentException("A beach column is required."),
                    )
                    val squares = layout[row] ?: return Result.failure(
                        IllegalArgumentException("That beach row is not available today."),
                    )
                    if (column !in squares.indices) {
                        return Result.failure(
                            IllegalArgumentException("That beach column is not available today."),
                        )
                    }
                    val beach = prefs.getInt("_beachMinutes", 0)
                    mapOf("coords" to "$row,${beach * 10 - column}")
                }
                else -> emptyMap()
            }
            val html = choose(parsed.command.option, fields, prefs)
                .getOrElse { return Result.failure(it) }
            Result.success(html)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val CHOICE_ID = 1388
        const val WANDER_OPTION = 1
        const val HEAD_OPTION = 3
        const val COMB_OPTION = 4
        const val EXIT_OPTION = 5

        /** Parse `head <query>` from `beach head …` CLI parameters (after "beach"). */
        fun parseHeadQuery(parameters: String): String? {
            val trimmed = parameters.trim()
            if (trimmed.isEmpty()) return null
            val lower = trimmed.lowercase()
            if (!lower.startsWith("head")) return null
            return trimmed.substringAfter("head").trim().ifEmpty { null }
        }

        fun parseCommand(parameters: String): ParsedCommand? {
            val trimmed = parameters.trim()
            if (trimmed.isEmpty()) return null
            val pieces = trimmed.split(Regex("""\s+"""))
            return when (pieces.first().lowercase()) {
                "visit" -> ParsedCommand(Command.VISIT)
                "random" -> ParsedCommand(Command.RANDOM)
                "common" -> ParsedCommand(Command.COMMON)
                "exit" -> ParsedCommand(Command.EXIT)
                "print" -> ParsedCommand(Command.PRINT)
                "wander" -> pieces.getOrNull(1)?.toIntOrNull()
                    ?.takeIf { it in 1..10_000 }
                    ?.let { ParsedCommand(Command.WANDER, minutes = it) }
                "comb" -> {
                    val row = pieces.getOrNull(1)?.trimEnd(',')?.toIntOrNull()
                    val column = pieces.getOrNull(2)?.toIntOrNull()
                    if (row != null && column != null && column in 0..9) {
                        ParsedCommand(Command.COMB, row = row, column = column)
                    } else {
                        null
                    }
                }
                "head" -> trimmed.substringAfter(' ', "").trim().ifEmpty { null }
                    ?.let { ParsedCommand(Command.HEAD, query = it) }
                else -> null
            }
        }

        fun getAdventuresUsed(url: String, freeWalksUsed: Int = 11): Int {
            if (!url.contains("choice.php", ignoreCase = true) ||
                !url.contains("whichchoice=$CHOICE_ID", ignoreCase = true)
            ) {
                return 0
            }
            val option = Regex("""(?:^|[?&])option=(\d+)""", RegexOption.IGNORE_CASE)
                .find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            return when (option) {
                Command.VISIT.option, Command.EXIT.option, Command.COMMON.option -> 0
                Command.WANDER.option, Command.RANDOM.option,
                Command.HEAD.option, Command.COMB.option ->
                    if (freeWalksUsed < 11) 0 else 1
                else -> 0
            }
        }

        fun containsEncounter(url: String): Boolean {
            val option = Regex("""(?:^|[?&])option=(\d+)""", RegexOption.IGNORE_CASE)
                .find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            return option == Command.WANDER.option || option == Command.RANDOM.option
        }

        fun registerRequest(
            url: String,
            preferences: Preferences? = null,
            logger: SessionLogger? = null,
        ): Boolean {
            if (!url.startsWith("choice.php", ignoreCase = true) ||
                !url.contains("whichchoice=$CHOICE_ID", ignoreCase = true)
            ) {
                return false
            }
            val option = Regex("""(?:^|[?&])option=(\d+)""", RegexOption.IGNORE_CASE)
                .find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            val message = when (option) {
                Command.VISIT.option -> "Using the Beach Comb"
                Command.EXIT.option -> "Putting down the Beach Comb"
                Command.COMMON.option -> "Collecting common items"
                Command.WANDER.option -> {
                    val minutes = Regex("""(?:^|[?&])minutes=(\d+)""", RegexOption.IGNORE_CASE)
                        .find(url)?.groupValues?.getOrNull(1) ?: "?"
                    "Wandering $minutes minutes down the beach"
                }
                Command.RANDOM.option -> "Wandering to a random section of the beach"
                Command.HEAD.option -> "Combing Beach Head"
                Command.COMB.option -> {
                    val coords = BeachCombManager.coordinatesFromUrl(url)
                    if (coords == null) "Combing an unknown beach square"
                    else "Combing square ${coords.row},${coords.column + 1} (${coords.beach} minutes down the beach)"
                }
                else -> "Beach Comb choice"
            }
            val turns = getAdventuresUsed(
                url,
                preferences?.getInt("_freeBeachWalksUsed", 11) ?: 11,
            )
            if (turns > 0) logger?.appendRawLine("[$turns] $message")
            else logger?.appendRawLine(message)
            return true
        }

        fun formatLayout(preferences: Preferences): String {
            val minutes = preferences.getInt("_beachMinutes", 0)
            val rows = BeachManager
                .stringToLayout(preferences.getString("_beachLayout", ""))
                .entries.sortedByDescending { it.key }
            return buildString {
                appendLine("Beach at $minutes")
                for ((row, squares) in rows) appendLine("$row: $squares")
            }.trimEnd()
        }
    }
}
