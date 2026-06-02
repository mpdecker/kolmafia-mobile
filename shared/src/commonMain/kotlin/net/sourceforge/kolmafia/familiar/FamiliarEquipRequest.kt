package net.sourceforge.kolmafia.familiar

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class FamiliarEquipRequest(private val client: HttpClient) {
    suspend fun equip(itemId: Int): Result<Unit> = try {
        client.submitForm(
            url = "$KOL_BASE_URL/inv_equip.php",
            formParameters = parameters {
                append("which", "2")
                append("whichitem", itemId.toString())
                append("slot", "familiarequip")
            }
        )
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
}
