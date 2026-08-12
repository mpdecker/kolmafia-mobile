package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.RabbitHoleAvailability

/** Desktop [HatterCommand] / [RabbitHoleManager.getHatBuff] — Mad Tea Party hat buff. */
class HatterRequest(
    private val client: HttpClient,
    private val choiceRequest: ChoiceRequest,
    private val useItemRequest: UseItemRequest?,
    private val equipmentRequest: EquipmentRequest?,
) {
    suspend fun takeBuff(
        length: Int,
        preferences: Preferences?,
        charState: CharacterState?,
        inventoryCounts: (Int) -> Int,
        hasRabbitHoleEffect: Boolean,
    ): Result<String> {
        val prefs = preferences
            ?: return Result.failure(IllegalStateException("Preferences are not available."))
        if (!RabbitHoleAvailability.teaPartyAvailable(prefs)) {
            return Result.failure(
                IllegalStateException("You have already attended a Tea Party today."),
            )
        }
        if (RabbitHoleAvailability.hatDataForLength(length) == null) {
            return Result.failure(IllegalArgumentException("No matching hat length found."))
        }
        val equippedHat = charState?.equippedItem(EquipmentSlot.HAT)
        val hatId = RabbitHoleAvailability.findHatIdForLength(
            length,
            inventoryCounts,
            equippedHat,
        ) ?: return Result.failure(IllegalStateException("No matching hat length found."))

        if (!hasRabbitHoleEffect) {
            if (inventoryCounts(RabbitHoleAvailability.DRINK_ME_POTION_ID) <= 0) {
                return Result.failure(
                    IllegalStateException("You need a DRINK ME! potion to get a hatter buff."),
                )
            }
            val useReq = useItemRequest
                ?: return Result.failure(IllegalStateException("Use item request is not available."))
            useReq.use(RabbitHoleAvailability.DRINK_ME_POTION_ID, 1)
                .exceptionOrNull()?.let { return Result.failure(it) }
        }

        val previousHatId = equippedHat?.let { ItemDatabase.getByName(it)?.id }
        val equip = equipmentRequest
        if (equip != null && previousHatId != hatId) {
            equip.equipItem(hatId, EquipmentSlot.HAT)
                .exceptionOrNull()?.let { return Result.failure(it) }
        }

        return try {
            val visit = client.submitForm(
                url = "$KOL_BASE_URL/place.php",
                formParameters = parameters {
                    append("whichplace", "rabbithole")
                    append("action", "rabbithole_teaparty")
                },
            )
            if (!visit.status.isSuccess()) {
                return Result.failure(IllegalStateException("Rabbit Hole visit failed."))
            }
            val result = choiceRequest.choose(CHOICE_ID, TEA_OPTION).map { (html, _) ->
                prefs.setBoolean(TEA_PARTY_PREF, true)
                html
            }
            if (equip != null && previousHatId != null && previousHatId != hatId) {
                equip.equipItem(previousHatId, EquipmentSlot.HAT)
            } else if (equip != null && previousHatId == null) {
                equip.unequipSlot(EquipmentSlot.HAT)
            }
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val CHOICE_ID = 441
        const val TEA_OPTION = 1
        const val TEA_PARTY_PREF = "_madTeaParty"

        fun parseLength(parameters: String): Int? =
            parameters.trim().toIntOrNull()?.takeIf { it in 4..31 }
    }
}
