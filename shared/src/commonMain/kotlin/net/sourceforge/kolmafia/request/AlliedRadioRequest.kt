package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.BreakfastItemIds
import net.sourceforge.kolmafia.session.DemonInCombatNameSync

/** Desktop [AlliedRadioRequest] — Allied Radio backpack/handheld choice automation. */
open class AlliedRadioRequest(
    private val client: HttpClient,
    private val useItemRequest: UseItemRequest,
) {

    data class RadioResult(val responseText: String, val handheld: Boolean)

    open suspend fun openBackpack(): Result<Unit> = try {
        val response = client.get("$KOL_BASE_URL/inventory.php?action=requestdrop")
        if (response.status.isSuccess()) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("HTTP ${response.status.value}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    open suspend fun useHandheldRadio(): Result<String> =
        useItemRequest.use(BreakfastItemIds.HANDHELD_ALLIED_RADIO_ID)

    open suspend fun submitRequest(request: String, handheld: Boolean): Result<RadioResult> = try {
        val whichchoice = if (handheld) {
            DemonInCombatNameSync.ALLIED_RADIO_HANDHELD_CHOICE
        } else {
            DemonInCombatNameSync.ALLIED_RADIO_BACKPACK_CHOICE
        }
        val response = client.submitForm(
            url = "$KOL_BASE_URL/choice.php",
            formParameters = parameters {
                append("whichchoice", whichchoice.toString())
                append("option", "1")
                append("request", request)
            },
        )
        if (!response.status.isSuccess()) {
            Result.failure(Exception("HTTP ${response.status.value}"))
        } else {
            Result.success(RadioResult(response.bodyAsText(), handheld))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    open suspend fun requestRadioCall(requestText: String, handheld: Boolean): Result<RadioResult> {
        if (handheld) {
            useHandheldRadio().onFailure { return Result.failure(it) }
        } else {
            openBackpack().onFailure { return Result.failure(it) }
        }
        return submitRequest(requestText, handheld)
    }

    companion object {
        private val BATTERY_PATTERN =
            Regex("""Looks like you have enough battery left to make (\d) calls? today\.""")

        private val NUMBER_LETTER_PATTERN =
            Regex(
                """voice saying <b>(?:&quot;|")(\d+)\.\.\. ([A-Z])\.\.\.(?:&quot;|")</b>""",
            )

        fun parseVisitChoice(html: String, preferences: Preferences) {
            val match = BATTERY_PATTERN.find(html) ?: return
            val batteryLeft = match.groupValues[1].toIntOrNull() ?: return
            preferences.setInt(Preferences.ALLIED_RADIO_DROPS_USED, 3 - batteryLeft)
        }

        data class PostChoiceResult(val logMessages: List<String> = emptyList())

        fun parsePostChoice(
            html: String,
            handheld: Boolean,
            request: String,
            preferences: Preferences,
            segmentSync: DemonInCombatNameSync?,
        ): PostChoiceResult {
            val logs = mutableListOf<String>()
            val req = request.lowercase()

            if (req == "sniper support") {
                preferences.setBoolean(Preferences.NONCOMBAT_FORCER_ACTIVE, true)
            }
            if (req == "materiel intel") {
                preferences.setBoolean(Preferences.ALLIED_RADIO_MATERIEL_INTEL, true)
            }
            if (req == "wildsun boon") {
                preferences.setBoolean(Preferences.ALLIED_RADIO_WILDSUN_BOON, true)
            }

            NUMBER_LETTER_PATTERN.find(html)?.let { match ->
                logs.add(
                    "Radio number / letter pattern received: ${match.groupValues[1]} - ${match.groupValues[2]}",
                )
            }

            segmentSync?.parseRadioResponse(html)?.hintMessage?.let { logs.add(it) }

            if (!handheld && !html.contains("Please request something else")) {
                val used = preferences.getInt(Preferences.ALLIED_RADIO_DROPS_USED, 0)
                preferences.setInt(Preferences.ALLIED_RADIO_DROPS_USED, used + 1)
            }

            return PostChoiceResult(logs)
        }
    }
}
