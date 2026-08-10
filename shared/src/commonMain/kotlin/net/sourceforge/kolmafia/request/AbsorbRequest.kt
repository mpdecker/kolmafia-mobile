package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.character.ClassResourceCharpaneSync
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.character.NoobcoreAbsorbs
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.item.RetrieveItemService

/** Desktop AbsorbCommand — absorb tradeable/gift items in Gelatinous Noob (Phase 388). */
open class AbsorbRequest(
    private val client: HttpClient,
    private val character: KoLCharacter? = null,
    private val gameDatabase: GameDatabase? = null,
    private val retrieveItemService: RetrieveItemService? = null,
) {
    open suspend fun refreshAbsorbs(): Result<Int> = fetchCharpaneAbsorbs()

    open suspend fun absorb(parameters: String): Result<Unit> {
        val char = character ?: return Result.failure(IllegalStateException("Not logged in"))
        val state = char.state.value
        if (!state.inNoobcore) {
            return Result.failure(IllegalStateException("You are not in a Gelatinous Noob run"))
        }
        if (NoobcoreAbsorbs.absorbsRemaining(state) < 1) {
            return Result.failure(IllegalStateException("Cannot absorb items at present."))
        }
        val (quantity, itemToken) = parseQuantityAndItem(parameters)
        val itemId = resolveItemId(itemToken)
            ?: return Result.failure(IllegalArgumentException("What item is $parameters?"))
        if (quantity < 1) {
            return Result.failure(IllegalArgumentException("Invalid quantity"))
        }

        val retrieved = retrieveItemService?.retrieve(itemId, quantity) ?: quantity
        if (retrieved < quantity) {
            return Result.failure(IllegalStateException("Item not accessible."))
        }

        val previousAbsorbs = char.state.value.absorbs
        repeat(quantity) {
            if (NoobcoreAbsorbs.absorbsRemaining(char.state.value) < 1) return@repeat
            val response = client.get("$KOL_BASE_URL/inventory.php") {
                parameter("absorb", itemId)
                parameter("ajax", 1)
            }
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("HTTP ${response.status.value}"))
            }
        }

        fetchCharpaneAbsorbs()
        val absorbed = char.state.value.absorbs - previousAbsorbs
        if (absorbed < quantity) {
            return Result.failure(
                IllegalStateException(
                    "Failed to absorb ${quantity - absorbed} of ${ItemDatabase.getItemName(itemId)}",
                ),
            )
        }
        return Result.success(Unit)
    }

    private suspend fun fetchCharpaneAbsorbs(): Result<Int> {
        val char = character ?: return Result.failure(IllegalStateException("Not logged in"))
        return try {
            val response = client.get("$KOL_BASE_URL/charpane.php")
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("HTTP ${response.status.value}"))
            }
            ClassResourceCharpaneSync.apply(char, response.bodyAsText())
            Result.success(char.state.value.absorbs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseQuantityAndItem(parameters: String): Pair<Int, String> {
        val trimmed = parameters.trim()
        val match = Regex("""^(\d+)\s+(.+)$""").matchEntire(trimmed)
        return if (match != null) {
            (match.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1) to match.groupValues[2].trim()
        } else {
            1 to trimmed
        }
    }

    private fun resolveItemId(token: String): Int? {
        val trimmed = token.trim()
        trimmed.removePrefix("\u00B6").toIntOrNull()?.let { return it }
        trimmed.removePrefix("[").removeSuffix("]").toIntOrNull()?.let { return it }
        gameDatabase?.item(trimmed)?.id?.let { return it }
        return ItemDatabase.getByName(trimmed)?.id
            ?: ItemDatabase.getByPluralOrName(trimmed)?.id
    }
}
