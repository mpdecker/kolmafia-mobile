package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.DemonTypes

/** Desktop [SummoningChamberRequest] — Summoning Chamber choice 922 demon summon. */
open class SummoningChamberRequest(private val client: HttpClient) {

    data class ParseResult(
        val setDemonSummoned: Boolean = false,
        val consumeSummoningItems: Boolean = false,
        val brownWord: String? = null,
    )

    open suspend fun visitChamber(): Result<Unit> = try {
        val response = client.get(
            "$KOL_BASE_URL/place.php?whichplace=manor4&action=manor4_chamber",
        )
        if (!response.status.isSuccess()) {
            Result.failure(Exception("HTTP ${response.status.value}"))
        } else {
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    open suspend fun summon(demonName: String): Result<SummonResult> = try {
        visitChamber().onFailure { return Result.failure(it) }
        val response = client.submitForm(
            url = "$KOL_BASE_URL/choice.php",
            formParameters = parameters {
                append("whichchoice", DemonTypes.SUMMONING_CHOICE.toString())
                append("option", "1")
                append("demonname", demonName)
            },
        )
        if (!response.status.isSuccess()) {
            Result.failure(Exception("HTTP ${response.status.value}"))
        } else {
            val body = response.bodyAsText()
            val location = buildLocation(demonName)
            Result.success(SummonResult(location, body))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    data class SummonResult(val location: String, val responseText: String)

    companion object {
        private val DEMON_PATTERN = Regex("demonname=([^&]*)")
        private val BROWN_WORD_PATTERN =
            Regex("""tell him that the passhword is <font color=brown><b>(.*?)</b></font>""")

        private val FAILED_SUMMON_MARKERS = listOf(
            "some sort of crossed signal",
            "hum, which eventually cuts off",
            "get right back to you",
            "Please check the listing",
        )

        fun buildLocation(demonName: String): String =
            "choice.php?whichchoice=${DemonTypes.SUMMONING_CHOICE}&option=1&demonname=$demonName"

        fun parseResponse(
            location: String,
            responseText: String,
            preferences: Preferences,
        ): ParseResult {
            if (!location.startsWith("choice.php") ||
                !location.contains("whichchoice=922") ||
                !location.contains("option=1")
            ) {
                return ParseResult()
            }
            if (!DEMON_PATTERN.containsMatchIn(location)) {
                return ParseResult()
            }

            return when {
                responseText.contains("greasy static-electricity feel") ->
                    ParseResult(setDemonSummoned = true).also {
                        preferences.setBoolean(Preferences.DEMON_SUMMONED, true)
                    }
                responseText.contains("You light three black candles") -> {
                    val brownWord = BROWN_WORD_PATTERN.find(responseText)?.groupValues?.getOrNull(1)
                    val failed = FAILED_SUMMON_MARKERS.any { responseText.contains(it) }
                    val setSummoned = !failed
                    if (setSummoned) {
                        preferences.setBoolean(Preferences.DEMON_SUMMONED, true)
                    }
                    ParseResult(
                        setDemonSummoned = setSummoned,
                        consumeSummoningItems = true,
                        brownWord = brownWord,
                    )
                }
                responseText.contains("Great Old One Shub-Internet") -> {
                    preferences.setBoolean(Preferences.DEMON_SUMMONED, true)
                    ParseResult(setDemonSummoned = true, consumeSummoningItems = true)
                }
                else -> ParseResult()
            }
        }
    }
}
