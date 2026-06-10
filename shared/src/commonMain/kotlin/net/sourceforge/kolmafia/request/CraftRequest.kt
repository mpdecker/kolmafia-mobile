package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.CraftMode

open class CraftRequest(private val client: HttpClient) {

    companion object {
        private val CRAFT_COMMENT = Regex("""<!-- ?cr:(\d+)x(-?\d+),(-?\d+)=(\d+) ?-->""")

        fun parseCreatedCount(responseText: String): Int {
            var total = 0
            for (match in CRAFT_COMMENT.findAll(responseText)) {
                total += match.groupValues[1].toIntOrNull() ?: 0
            }
            return total
        }
    }

    open suspend fun craft(mode: String, quantity: Int, itemId1: Int, itemId2: Int): Int {
        if (quantity <= 0) return 0
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
            parseCreatedCount(response.bodyAsText())
        } catch (_: Exception) {
            0
        }
    }

    open suspend fun craft(mode: CraftMode, quantity: Int, itemId1: Int, itemId2: Int): Int =
        craft(mode.apiAction, quantity, itemId1, itemId2)
}
