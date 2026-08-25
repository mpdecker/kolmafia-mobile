package net.sourceforge.kolmafia.familiar

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.RequestAbortGate
import net.sourceforge.kolmafia.session.EquipmentManager
import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.SessionLogger

open class FamiliarRequest(
    private val client: HttpClient,
    private val familiarManager: FamiliarManager? = null,
    private val preferences: Preferences? = null,
    private val character: KoLCharacter? = null,
    private val equipmentManager: EquipmentManager? = null,
    private val sessionLogger: SessionLogger? = null,
) {
    suspend fun switchFamiliar(familiarId: Int): Result<Unit> {
        if (RequestAbortGate.abortIfInFightOrChoice()) {
            return Result.failure(
                IllegalStateException(
                    RequestAbortGate.lastAbortMessage.ifEmpty {
                        "You are currently in a fight or choice."
                    },
                ),
            )
        }
        return visitAction(
            action = "newfam",
            params = mapOf("whichfam" to familiarId.toString()),
        ).map { }
    }

    suspend fun putBack(): Result<Unit> =
        visitAction("putback").map { }

    /** Enthrone a familiar in the Crown of Thrones (0 = clear). */
    suspend fun enthrone(familiarId: Int): Result<Unit> =
        visitAction(
            action = "hatseat",
            params = mapOf("famid" to familiarId.toString(), "ajax" to "1"),
        ).map { }

    /** Bjornify a familiar in Buddy Bjorn (0 = clear). */
    suspend fun bjornify(familiarId: Int): Result<Unit> =
        visitAction(
            action = "backpack",
            params = mapOf("famid" to familiarId.toString(), "ajax" to "1"),
        ).map { }

    /** Steal an item with the active familiar (Pixie, etc.). */
    open suspend fun stealItem(itemId: Int): Result<String> =
        visitAction(
            action = "steal",
            params = mapOf("whichitem" to itemId.toString()),
        )

    open suspend fun equipFamiliarItem(itemId: Int, familiarId: Int? = null): Result<String> {
        val params = mutableMapOf("whichitem" to itemId.toString())
        if (familiarId != null) params["whichfam"] = familiarId.toString()
        return visitAction("equip", params)
    }

    open suspend fun unequipFamiliar(familiarId: Int? = null): Result<String> {
        val params = mutableMapOf<String, String>()
        if (familiarId != null) params["famid"] = familiarId.toString()
        return visitAction("unequip", params)
    }

    open suspend fun lockEquip(): Result<String> = visitAction("lockequip")

    private suspend fun visitAction(
        action: String,
        params: Map<String, String> = emptyMap(),
    ): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/familiar.php",
            formParameters = parameters {
                append("action", action)
                for ((k, v) in params) append(k, v)
            },
        )
        if (!response.status.isSuccess()) {
            Result.failure(Exception("HTTP ${response.status.value}"))
        } else {
            val html = response.bodyAsText()
            val url = buildString {
                append("familiar.php?action=").append(action)
                for ((k, v) in params) append('&').append(k).append('=').append(v)
            }
            RequestLogger.registerRequest(url, sessionLogger, preferences)
            FamiliarSync.parseResponse(
                url = url,
                html = html,
                familiarManager = familiarManager,
                preferences = preferences,
                character = character,
                equipmentManager = equipmentManager,
            )
            Result.success(html)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
