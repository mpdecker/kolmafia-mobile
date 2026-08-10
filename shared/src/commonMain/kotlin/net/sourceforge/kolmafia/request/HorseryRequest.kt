package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop HorseryCommand — ride a horsery horse via choice 1266. */
open class HorseryRequest(
    private val client: HttpClient,
    private val choiceRequest: ChoiceRequest,
    private val preferences: Preferences? = null,
) {
    open suspend fun ride(horseName: String): Result<Unit> {
        if (preferences?.getBoolean("horseryAvailable", false) != true) {
            return Result.failure(IllegalStateException("You need a horsery first."))
        }
        val option = resolveOption(horseName)
            ?: return Result.failure(IllegalArgumentException("Unknown horse: $horseName"))
        val current = preferences.getString("_horsery", "")
        if (current.equals(horseName, ignoreCase = true)) {
            return Result.failure(IllegalStateException("You already have the $horseName."))
        }
        return try {
            client.get(
                "$KOL_BASE_URL/place.php?whichplace=town_right&action=town_horsery",
            ).bodyAsText()
            choiceRequest.choose(1266, option).getOrElse { return Result.failure(it) }
            preferences.setString("_horsery", canonicalHorseName(option))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun resolveOption(params: String): Int? {
        val lower = params.lowercase()
        lower.toIntOrNull()?.takeIf { it in 1..4 }?.let { return it }
        return when {
            lower.contains("init") || lower.contains("regen") || lower.startsWith("normal") -> 1
            lower.contains("-combat") || lower.contains("meat") || lower.startsWith("dark") -> 2
            lower.contains("stat") || lower.contains("random") || lower.startsWith("crazy") -> 3
            lower.contains("resist") || lower.contains("spooky") || lower.startsWith("pale") -> 4
            else -> null
        }
    }

    private fun canonicalHorseName(option: Int): String = when (option) {
        1 -> "normal horse"
        2 -> "dark horse"
        3 -> "crazy horse"
        4 -> "pale horse"
        else -> "normal horse"
    }
}
