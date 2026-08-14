package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.chat.ChatProbe
import net.sourceforge.kolmafia.chat.PlayerIdRegistry
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop CrossStreamsCommand — equip proton pack + showplayer crossthestreams. */
class CrossStreamsRequest(
    private val client: HttpClient,
    private val equipmentRequest: EquipmentRequest? = null,
    private val chatProbe: ChatProbe? = null,
) {
    suspend fun crossStreams(
        targetArg: String?,
        preferences: Preferences?,
        charState: CharacterState?,
        inventoryCounts: (Int) -> Int,
    ): Result<String> {
        val preflight = preflightError(preferences, charState, inventoryCounts)
        if (preflight != null) {
            return Result.failure(IllegalStateException(preflight))
        }
        val targetRaw = targetArg?.trim().orEmpty().ifEmpty {
            preferences?.getString(DEFAULT_TARGET_PREF, "")?.trim().orEmpty()
        }
        if (targetRaw.isEmpty()) {
            return Result.failure(IllegalStateException("Cannot find target"))
        }
        val targetId = resolveTargetId(targetRaw)
            ?: return Result.failure(IllegalStateException("Cannot find target $targetRaw"))

        val packName = ItemDatabase.getItemName(PROTON_PACK_ID)
        val equipped = charState?.equipment?.get(EquipmentSlot.CONTAINER)
            ?.equals(packName, ignoreCase = true) == true
        if (!equipped && equipmentRequest != null) {
            equipmentRequest.equipItem(PROTON_PACK_ID, EquipmentSlot.CONTAINER)
                .onFailure { return Result.failure(it) }
        }

        return try {
            val response = client.get(
                "$KOL_BASE_URL/showplayer.php?action=crossthestreams&who=$targetId",
            )
            if (!response.status.isSuccess()) {
                return Result.failure(IllegalStateException("Cross streams failed."))
            }
            val html = response.bodyAsText()
            parseResponse(html, preferences)
            Result.success(html)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun resolveTargetId(raw: String): String? {
        if (raw.all { it.isDigit() }) {
            val name = PlayerIdRegistry.getPlayerName(raw, retrieveName = false)
            if (name != raw) return raw
            val lookedUp = chatProbe?.lookupPlayerName(raw)
            return if (lookedUp != null && lookedUp != raw) raw else raw
        }
        val cached = PlayerIdRegistry.getPlayerId(raw, retrieveId = false)
        if (cached != raw && cached.all { it.isDigit() }) return cached
        val lookedUp = chatProbe?.lookupPlayerId(raw) ?: return null
        return if (lookedUp != raw && lookedUp.all { it.isDigit() }) lookedUp else null
    }

    companion object {
        const val PROTON_PACK_ID = 9082
        const val STREAMS_CROSSED_PREF = "_streamsCrossed"
        const val DEFAULT_TARGET_PREF = "streamCrossDefaultTarget"

        fun preflightError(
            preferences: Preferences?,
            charState: CharacterState?,
            inventoryCounts: (Int) -> Int,
        ): String? {
            val packName = ItemDatabase.getItemName(PROTON_PACK_ID)
            val equipped = charState?.equipment?.values?.any {
                it.equals(packName, ignoreCase = true)
            } == true
            if (!equipped && inventoryCounts(PROTON_PACK_ID) <= 0) {
                return "Do not have a Proton Accelerator Pack"
            }
            if (preferences?.getBoolean(STREAMS_CROSSED_PREF, false) == true) {
                return "Have already crossed streams today"
            }
            return null
        }

        fun parseResponse(html: String, preferences: Preferences?) {
            if (preferences == null) return
            if (html.contains("creating an intense but localized nuclear reaction") ||
                html.contains("You've already crossed the streams today")
            ) {
                preferences.setBoolean(STREAMS_CROSSED_PREF, true)
            }
        }

        /** Resolve without HTTP — for unit tests / cache-only paths. */
        fun resolveTargetIdCached(raw: String): String? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null
            if (trimmed.all { it.isDigit() }) return trimmed
            val id = PlayerIdRegistry.getPlayerId(trimmed, retrieveId = false)
            return if (id != trimmed && id.all { it.isDigit() }) id else null
        }
    }
}
