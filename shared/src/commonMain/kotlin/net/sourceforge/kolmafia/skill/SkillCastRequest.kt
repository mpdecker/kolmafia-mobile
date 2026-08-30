package net.sourceforge.kolmafia.skill

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parameters
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.DailyLimitDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.EquipmentRequest
import net.sourceforge.kolmafia.request.RequestAbortGate
import net.sourceforge.kolmafia.request.UneffectRemovableMaps
import net.sourceforge.kolmafia.session.EquipmentManager
import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.SessionLogger

class SkillCastRequest(
    private val client: HttpClient,
    private val preferences: Preferences? = null,
    private val character: KoLCharacter? = null,
    private val inventoryManager: InventoryManager? = null,
    private val equipmentManager: EquipmentManager? = null,
    private val equipmentRequest: EquipmentRequest? = null,
    private val sessionLogger: SessionLogger? = null,
    private val effectManager: EffectManager? = null,
) {

    suspend fun cast(skillId: Int, quantity: Int = 1): Result<String> {
        if (RequestAbortGate.abortIfInFightOrChoice()) {
            return Result.failure(
                IllegalStateException(
                    RequestAbortGate.lastAbortMessage.ifEmpty {
                        "You are currently in a fight or choice."
                    },
                ),
            )
        }
        val maxOk = UseSkillCastGates.maxCastsAllowed(skillId, quantity, preferences)
        if (!maxOk) {
            UseSkillSync.lastUpdate = "Daily limit reached"
            return Result.failure(Exception("Daily limit reached"))
        }
        return try {
            UseSkillOptimize.optimizeEquipment(
                skillId = skillId,
                preferences = preferences,
                character = character,
                inventory = inventoryManager,
                equipmentManager = equipmentManager,
                equipmentRequest = equipmentRequest,
            )
            UseSkillSync.noteCast(skillId, quantity)
            val response = client.submitForm(
                url = "$KOL_BASE_URL/skills.php",
                formParameters = parameters {
                    append("action", "Skillz")
                    append("whichskill", skillId.toString())
                    append("quantity", quantity.toString())
                    append("ajax", "1")
                },
            )
            val body = response.bodyAsText()
            val url = "skills.php?action=Skillz&whichskill=$skillId&quantity=$quantity"
            RequestLogger.registerRequest(url, sessionLogger, preferences)
            val stopped = UseSkillSync.parseResponse(
                urlString = url,
                responseText = body,
                preferences = preferences,
                character = character,
                mpCostPerCast = SkillDefinitionDatabase.getById(skillId)?.mpCost,
            )
            when {
                stopped && UseSkillSync.lastUpdate.contains("Not enough", ignoreCase = true) ->
                    Result.failure(Exception("Not enough MP"))
                stopped && UseSkillSync.lastUpdate.contains("Daily limit", ignoreCase = true) ->
                    Result.failure(Exception("Daily limit reached"))
                stopped && UseSkillSync.lastUpdate.isNotEmpty() ->
                    Result.failure(Exception(UseSkillSync.lastUpdate))
                body.contains("don't have enough", ignoreCase = true) ||
                    body.contains("not enough mp", ignoreCase = true) ->
                    Result.failure(Exception("Not enough MP"))
                body.contains("daily limit", ignoreCase = true) ->
                    Result.failure(Exception("Daily limit reached"))
                else -> {
                    val skillName = SkillDefinitionDatabase.getById(skillId)?.name.orEmpty()
                    effectManager?.removeEffects(
                        UneffectRemovableMaps.removableEffectIdsForSkill(skillName),
                    )
                    Result.success(body)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/** Desktop UseSkillRequest max-casts / daily-limit preflight (Phases 2406–2420). */
object UseSkillCastGates {
    fun maxCastsAllowed(
        skillId: Int,
        quantity: Int,
        preferences: Preferences?,
    ): Boolean {
        if (quantity <= 0) return false
        val pref = DailyLimitDatabase.getCastPrefForSkill(skillId)
        if (pref.isBlank() || preferences == null) return true
        // Soft gate: if pref already at a high watermark and quantity > remaining, reject.
        // Without max from DB at call site, allow cast and let parseResponse set-to-max.
        val used = preferences.getInt(pref, 0)
        if (used < 0) return true
        // If previous response set an explicit max via UseSkillSync, honor it.
        val max = preferences.getInt("${pref}_max", 0)
        if (max > 0 && used + quantity > max) return false
        return true
    }

    fun registerSuccessfulCasts(
        skillId: Int,
        count: Int,
        preferences: Preferences?,
    ) {
        preferences ?: return
        val pref = DailyLimitDatabase.getCastPrefForSkill(skillId)
        if (pref.isNotBlank()) {
            preferences.setInt(pref, preferences.getInt(pref, 0) + count.coerceAtLeast(1))
        }
    }
}
