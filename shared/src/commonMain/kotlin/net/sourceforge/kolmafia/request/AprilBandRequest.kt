package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop AprilBandCommand effect/conduct path — inventory.php?action=apriling. */
class AprilBandRequest(
    private val client: HttpClient,
    private val choiceRequest: ChoiceRequest,
) {
    suspend fun takeEffect(
        choice: Int,
        preferences: Preferences?,
        charState: CharacterState?,
        inventoryCounts: (Int) -> Int,
        helmetName: String? = null,
    ): Result<String> {
        if (choice !in 1..3) {
            return Result.failure(
                IllegalArgumentException("I don't understand what effect that is."),
            )
        }
        if (!hasHelmet(charState, inventoryCounts, helmetName)) {
            return Result.failure(
                IllegalStateException("You need an Apriling band helmet."),
            )
        }
        val turnsPlayed = charState?.turnsPlayed ?: 0
        val nextConduct = preferences?.getInt(NEXT_TURN_PREF, 0) ?: 0
        val turnsToGo = nextConduct - turnsPlayed
        if (turnsToGo > 0) {
            return Result.failure(
                IllegalStateException(
                    "You cannot change your conduct ($turnsToGo turns to go).",
                ),
            )
        }
        return try {
            val start = client.get("$KOL_BASE_URL/inventory.php") {
                parameter("action", "apriling")
            }
            if (!start.status.isSuccess()) {
                return Result.failure(IllegalStateException("Apriling Band conduct failed."))
            }
            choiceRequest.choose(CHOICE_ID, choice).onFailure { return Result.failure(it) }
            choiceRequest.choose(CHOICE_ID, FINISH_OPTION).map { (html, _) ->
                // Desktop updates nextAprilBandTurn via choice sync; approximate +11 turns.
                preferences?.setInt(NEXT_TURN_PREF, turnsPlayed + 11)
                html
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val HELMET_ID = 11565
        const val NEXT_TURN_PREF = "nextAprilBandTurn"
        const val CHOICE_ID = 1526
        const val FINISH_OPTION = 9

        /**
         * Desktop opens inventory.php?action=apriling which lands in choice 1526
         * (Conduct the Band). Effect options 1–3 then finish option 9.
         */
        fun findEffectChoice(parameters: String): Int {
            val parts = parameters.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (parts.isEmpty()) return 0
            val head = parts[0].lowercase()
            if (head != "effect" && head != "conduct") return 0
            val effect = parts.getOrNull(1)?.lowercase().orEmpty()
            return when {
                effect.startsWith("nc") || effect.startsWith("non") -> 1
                effect.startsWith("c") -> 2
                effect.startsWith("drop") -> 3
                else -> 0
            }
        }

        fun hasHelmet(
            charState: CharacterState?,
            inventoryCounts: (Int) -> Int,
            helmetName: String?,
        ): Boolean {
            if (inventoryCounts(HELMET_ID) > 0) return true
            val hat = charState?.equippedItem(EquipmentSlot.HAT).orEmpty()
            if (hat.isBlank()) return false
            if (helmetName != null && hat.equals(helmetName, ignoreCase = true)) return true
            return hat.contains("Apriling", ignoreCase = true)
        }
    }
}
