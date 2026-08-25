package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.CraftMode
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

open class CraftRequest(
    private val client: HttpClient,
    private val inventoryManager: InventoryManager? = null,
    private val preferences: Preferences? = null,
    private val character: KoLCharacter? = null,
    private val sessionLogger: SessionLogger? = null,
) {

    companion object {
        private val CRAFT_COMMENT = Regex("""<!-- ?cr:(\d+)x(-?\d+),(-?\d+)=(\d+) ?-->""")

        private val CRAFT_FAILURE_SIGNALS = listOf(
            "You can't craft",
            "don't have enough",
            "don't know how to make",
            "You haven't unlocked",
        )

        fun parseCreatedCount(responseText: String): Int {
            if (isCraftFailure(responseText)) return 0
            var total = 0
            for (match in CRAFT_COMMENT.findAll(responseText)) {
                total += match.groupValues[1].toIntOrNull() ?: 0
            }
            return total
        }

        fun isCraftFailure(responseText: String): Boolean =
            CRAFT_FAILURE_SIGNALS.any { responseText.contains(it, ignoreCase = true) }

        /** Desktop CreateItemRequest adventures used from craft URL qty. */
        fun getAdventuresUsed(url: String): Int {
            if (!url.contains("craft.php", ignoreCase = true)) return 0
            val qty = Regex("""[?&]qty=(\d+)""", RegexOption.IGNORE_CASE)
                .find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 1
            return qty.coerceAtLeast(0)
        }
    }

    open suspend fun craft(mode: String, quantity: Int, itemId1: Int, itemId2: Int): Int {
        if (quantity <= 0) return 0
        if (CreateAbortGate.shouldAbort()) return 0
        val craftMode = mode.lowercase()
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/craft.php",
                formParameters = Parameters.build {
                    append("action", "craft")
                    append("mode", craftMode)
                    append("a", itemId1.toString())
                    append("b", itemId2.toString())
                    append("qty", quantity.toString())
                }
            )
            val body = response.bodyAsText()
            val location =
                "craft.php?action=craft&mode=$craftMode&a=$itemId1&b=$itemId2&qty=$quantity"
            applyCraftResponse(location, body)
        } catch (_: Exception) {
            0
        }
    }

    open suspend fun craft(mode: CraftMode, quantity: Int, itemId1: Int, itemId2: Int): Int =
        craft(mode.apiAction, quantity, itemId1, itemId2)

    fun applyCraftResponse(location: String, body: String): Int {
        val state: CharacterState? = character?.state?.value
        val result = CreateItemCraftSync.parseCrafting(
            location = location,
            responseText = body,
            inventory = inventoryManager,
            preferences = preferences,
            characterState = state,
            sessionLogger = sessionLogger,
        )
        return if (result.created > 0) result.created else parseCreatedCount(body)
    }
}
