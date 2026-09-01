package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.PandamoniumVisitSync
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.ResultProcessor
import net.sourceforge.kolmafia.session.SessionLogger

/** Typed headless request for Pandamonium NPC interactions, including Sven. */
open class PandamoniumRequest(
    private val client: HttpClient,
    private val questDatabase: QuestDatabase? = null,
    private val inventoryManager: InventoryManager? = null,
    private val preferences: Preferences? = null,
    private val sessionLogger: SessionLogger? = null,
) {
    data class BandMember(val name: String, val role: String)

    companion object {
        const val MOAN = "moan"
        const val TEMPLE = "temp"
        const val MOURN = "mourn"
        const val SVEN = "sven"

        val COMEDY_TYPES = setOf("insult", "observe", "prop")
        val BAND_MEMBERS = listOf(
            BandMember("Bognort", "guitarist"),
            BandMember("Stinkface", "vocalist"),
            BandMember("Flargwurm", "bassist"),
            BandMember("Jim", "drummer"),
        )

        fun comedyType(raw: String): String? =
            COMEDY_TYPES.firstOrNull { it.equals(raw, ignoreCase = true) }

        fun bandMember(raw: String): String? =
            BAND_MEMBERS.firstOrNull {
                it.name.equals(raw, ignoreCase = true) || it.role.equals(raw, ignoreCase = true)
            }?.name

        fun parseResponse(
            url: String,
            responseText: String,
            inventoryManager: InventoryManager? = null,
            preferences: Preferences? = null,
            questDatabase: QuestDatabase? = null,
        ): Boolean {
            if (!url.startsWith("pandamonium.php", ignoreCase = true)) return false
            val action = Regex("""(?:^|[?&])action=([^&]+)""", RegexOption.IGNORE_CASE)
                .find(url)?.groupValues?.get(1) ?: return false
            if (action.equals(SVEN, ignoreCase = true) &&
                url.contains("preaction=try", ignoreCase = true)
            ) {
                val itemId = Regex("""(?:^|[?&])togive=(\d+)""")
                    .find(url)?.groupValues?.get(1)?.toIntOrNull()
                if (itemId != null) {
                    ResultProcessor.processItem(itemId, -1, preferences, questDatabase, inventoryManager)
                }
                return itemId != null
            }
            if (action.equals(MOAN, ignoreCase = true) &&
                responseText.contains("Here's your talisman", ignoreCase = true)
            ) {
                ResultProcessor.processItem(4698, -5, preferences, questDatabase, inventoryManager)
                ResultProcessor.processItem(4699, -5, preferences, questDatabase, inventoryManager)
            }
            return true
        }
    }

    suspend fun visit(action: String): Result<String> =
        post(Parameters.build { append("action", action) })

    suspend fun comedy(type: String): Result<String> {
        val normalized = comedyType(type)
            ?: return Result.failure(IllegalArgumentException("What kind of comedy is \"$type\"?"))
        return post(Parameters.build {
            append("action", MOURN)
            append("preaction", normalized)
        })
    }

    suspend fun give(member: String, itemId: Int): Result<String> {
        val normalized = bandMember(member)
            ?: return Result.failure(IllegalArgumentException("Unknown Pandamonium band member: $member"))
        if (itemId <= 0) return Result.failure(IllegalArgumentException("Item id must be positive."))
        if (inventoryManager != null &&
            (inventoryManager.state.value.items[itemId]?.quantity ?: 0) < 1
        ) {
            return Result.failure(IllegalStateException("You do not have item #$itemId."))
        }
        return post(Parameters.build {
            append("action", SVEN)
            append("bandmember", normalized)
            append("togive", itemId.toString())
            append("preaction", "try")
        })
    }

    private suspend fun post(parameters: Parameters): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/pandamonium.php",
            formParameters = parameters,
        )
        if (!response.status.isSuccess()) {
            Result.failure(Exception("HTTP ${response.status.value}"))
        } else {
            val body = response.bodyAsText()
            val url = buildUrl(parameters)
            PandamoniumVisitSync.applyFromVisit(url, questDatabase)
            parseResponse(url, body, inventoryManager, preferences, questDatabase)
            sessionLogger?.appendRawLine("Pandamonium: ${logLine(parameters)}")
            Result.success(body)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun buildUrl(parameters: Parameters): String =
        "pandamonium.php?" + parameters.entries().joinToString("&") { (key, values) ->
            "$key=${values.firstOrNull().orEmpty()}"
        }

    private fun logLine(parameters: Parameters): String =
        parameters.entries().joinToString(" ") { (key, values) -> "$key=${values.firstOrNull().orEmpty()}" }
}
