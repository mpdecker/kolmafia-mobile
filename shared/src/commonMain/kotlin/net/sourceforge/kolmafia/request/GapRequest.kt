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

/** Desktop GapCommand — Greatest American Pants superbuffs (choice 508). */
class GapRequest(
    private val client: HttpClient,
    private val choiceRequest: ChoiceRequest,
) {
    suspend fun takeBuff(
        option: Int,
        preferences: Preferences?,
        charState: CharacterState?,
        gapPantsName: String? = null,
        replicaPantsName: String? = null,
    ): Result<String> {
        if (option !in 1..5) {
            return Result.failure(IllegalArgumentException("Unknown GAP buff."))
        }
        val used = preferences?.getInt(BUFFS_PREF, 0) ?: 0
        if (used >= 5) {
            return Result.failure(IllegalStateException("You're out of superpowers."))
        }
        if (!hasGapPantsEquipped(charState, gapPantsName, replicaPantsName)) {
            return Result.failure(
                IllegalStateException("You need to equip your superpants first."),
            )
        }
        return try {
            val activate = client.get("$KOL_BASE_URL/inventory.php") {
                parameter("action", "activatesuperpants")
            }
            if (!activate.status.isSuccess()) {
                return Result.failure(IllegalStateException("GAP activate failed."))
            }
            val buffName = BUFF_NAMES[option - 1]
            val result = choiceRequest.choose(
                CHOICE_ID,
                option,
                mapOf("choiceform$option" to buffName),
            )
            result.onSuccess {
                preferences?.setInt(BUFFS_PREF, used + 1)
            }
            result.map { (html, _) -> html }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val BUFFS_PREF = "_gapBuffs"
        const val CHOICE_ID = 508
        const val GREAT_PANTS_ID = 4696
        const val REPLICA_GREAT_PANTS_ID = 11209

        private val BUFF_NAMES = listOf(
            "Super Skill",
            "Super Structure",
            "Super Vision",
            "Super Speed",
            "Super Accuracy",
        )

        fun findOption(parameters: String): Int {
            val p = parameters.trim().lowercase()
            if (p.isEmpty()) return 0
            if (p.length == 1 && p[0].isDigit()) {
                val n = p.toIntOrNull() ?: return 0
                return if (n in 1..5) n else 0
            }
            return when {
                p.contains("skill") -> 1
                p.contains("structure") -> 2
                p.contains("vision") -> 3
                p.contains("speed") -> 4
                p.contains("accuracy") -> 5
                else -> 0
            }
        }

        fun hasGapPantsEquipped(
            charState: CharacterState?,
            gapPantsName: String?,
            replicaPantsName: String?,
        ): Boolean {
            val pants = charState?.equippedItem(EquipmentSlot.PANTS).orEmpty()
            if (pants.isBlank()) return false
            if (gapPantsName != null && pants.equals(gapPantsName, ignoreCase = true)) return true
            if (replicaPantsName != null && pants.equals(replicaPantsName, ignoreCase = true)) {
                return true
            }
            return pants.contains("Greatest American Pants", ignoreCase = true)
        }
    }
}
