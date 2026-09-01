package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.PerilChoiceSync
import net.sourceforge.kolmafia.session.EquipmentManager
import net.sourceforge.kolmafia.session.SessionLogger

/** Typed request for Foreseeing Peril via inventory.php and choice 1558. */
open class ForeseeRequest(
    private val client: HttpClient,
    private val choiceRequest: ChoiceRequest,
    private val inventoryManager: InventoryManager? = null,
    private val equipmentManager: EquipmentManager? = null,
    private val preferences: Preferences? = null,
    private val sessionLogger: SessionLogger? = null,
) {
    private val handledSignatures = mutableSetOf<Pair<String, String>>()

    open suspend fun foresee(perilId: Int? = null): Result<String> {
        if (perilId != null && perilId <= 0) {
            return Result.failure(IllegalArgumentException("Peril target ID must be positive."))
        }
        if (!hasPeridot()) {
            return Result.failure(IllegalStateException("You do not own a Peridot of Peril."))
        }
        if (remainingUses() < 1) {
            return Result.failure(IllegalStateException("You can only foresee peril thrice daily."))
        }
        return try {
            val visitResponse = client.get("$KOL_BASE_URL/inventory.php") {
                parameter("action", "foresee")
            }
            if (!visitResponse.status.isSuccess()) {
                return Result.failure(IllegalStateException("Foresee inventory request failed."))
            }
            val visitHtml = visitResponse.bodyAsText()
            if (perilId == null) {
                parseResponse("inventory.php?action=foresee", visitHtml)
                return Result.success(visitHtml)
            }
            val choiceResult = choiceRequest.choose(
                PerilChoiceSync.CHOICE_ID,
                FORESEE_OPTION,
                mapOf("who" to perilId.toString()),
            )
            val (html, url) = choiceResult.getOrElse { return Result.failure(it) }
            val responseUrl = buildString {
                append(url)
                append(if (url.contains('?')) '&' else '?')
                append("whichchoice=${PerilChoiceSync.CHOICE_ID}")
                append("&option=$FORESEE_OPTION")
                append("&who=$perilId")
            }
            if (!parseResponse(responseUrl, html)) {
                return Result.failure(IllegalStateException("Foresee choice response was not successful."))
            }
            sessionLogger?.appendRawLine("foresee $perilId")
            Result.success(html)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseResponse(url: String, html: String): Boolean {
        val signature = url to html
        if (signature in handledSignatures) return true
        val choiceId = extractChoiceId(url)
        val handled = if (choiceId == PerilChoiceSync.CHOICE_ID) {
            PerilChoiceSync.applyDecision(html, preferences)
        } else {
            PerilChoiceSync.applyVisit(PerilChoiceSync.CHOICE_ID, html, preferences)
        }
        if (!handled) return false
        handledSignatures += signature
        return true
    }

    private fun hasPeridot(): Boolean {
        if ((inventoryManager?.getCount(PERIDOT_OF_PERIL_ID) ?: 0) > 0) return true
        return equipmentManager?.hasEquipped(PERIDOT_OF_PERIL_ID) == true
    }

    private fun remainingUses(): Int =
        (PerilChoiceSync.MAX_PERILS - (preferences?.getInt("_perilsForeseen", 0) ?: 0))
            .coerceAtLeast(0)

    private fun extractChoiceId(url: String): Int? =
        WHICH_CHOICE.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()

    companion object {
        const val PERIDOT_OF_PERIL_ID = 11905
        private const val FORESEE_OPTION = 1
        private val WHICH_CHOICE = Regex("""(?:^|[?&])whichchoice=(\d+)""", RegexOption.IGNORE_CASE)
    }
}
