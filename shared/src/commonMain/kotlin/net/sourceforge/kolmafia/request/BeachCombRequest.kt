package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.BeachCombChoiceSync
import net.sourceforge.kolmafia.session.BeachHeadAvailability

/** Desktop [BeachCombRequest] — Beach Comb head buffs (choice 1388). */
class BeachCombRequest(
    private val client: HttpClient,
    private val choiceRequest: ChoiceRequest,
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
        val hasComb = inventoryCounts(BeachHeadAvailability.BEACH_COMB_ID) > 0 ||
            inventoryCounts(BeachHeadAvailability.DRIFTWOOD_BEACH_COMB_ID) > 0
        if (!hasComb) {
            return Result.failure(
                IllegalStateException("You need either a Beach Comb or a driftwood beach comb"),
            )
        }
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
            val unlocked = BeachHeadAvailability.parseBeachHeadsUnlocked(prefs)
            val html = if (head.id in unlocked) {
                choiceRequest.choose(
                    CHOICE_ID,
                    HEAD_OPTION,
                    mapOf("buff" to head.id.toString()),
                ).getOrElse { return Result.failure(it) }.first
            } else {
                choiceRequest.choose(
                    CHOICE_ID,
                    WANDER_OPTION,
                    mapOf("minutes" to head.beach.toString()),
                ).exceptionOrNull()?.let { return Result.failure(it) }
                choiceRequest.choose(
                    CHOICE_ID,
                    COMB_OPTION,
                    mapOf("coords" to head.coords),
                ).getOrElse { return Result.failure(it) }.first
            }
            choiceRequest.choose(CHOICE_ID, EXIT_OPTION)
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
        return try {
            if (parsed.command == Command.VISIT) {
                val response = client.get("$KOL_BASE_URL/main.php") { parameter("comb", "1") }
                if (!response.status.isSuccess()) {
                    return Result.failure(IllegalStateException("Beach Comb visit failed."))
                }
                val html: String = response.body()
                BeachCombChoiceSync.apply(CHOICE_ID, 0, html, prefs, "main.php?comb=1")
                return Result.success(html)
            }
            val fields = when (parsed.command) {
                Command.WANDER -> mapOf("minutes" to parsed.minutes.toString())
                Command.COMB -> {
                    if (!prefs.getBoolean("_beachCombing", false)) {
                        return Result.failure(
                            IllegalStateException("Visit a square on the beach before you comb it."),
                        )
                    }
                    val beach = prefs.getInt("_beachMinutes", 0)
                    mapOf("coords" to "${parsed.row},${beach * 10 - (parsed.column ?: 0)}")
                }
                else -> emptyMap()
            }
            val (html, _) = choiceRequest.choose(CHOICE_ID, parsed.command.option, fields)
                .getOrElse { return Result.failure(it) }
            val choiceUrl = buildString {
                append("whichchoice=$CHOICE_ID&option=${parsed.command.option}")
                for ((key, value) in fields) append("&$key=$value")
            }
            BeachCombChoiceSync.apply(CHOICE_ID, parsed.command.option, html, prefs, choiceUrl)
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

        fun formatLayout(preferences: Preferences): String {
            val minutes = preferences.getInt("_beachMinutes", 0)
            val rows = net.sourceforge.kolmafia.session.BeachCombManager
                .stringToLayout(preferences.getString("_beachLayout", ""))
                .entries.sortedByDescending { it.key }
            return buildString {
                appendLine("Beach at $minutes")
                for ((row, squares) in rows) appendLine("$row: $squares")
            }.trimEnd()
        }
    }
}
