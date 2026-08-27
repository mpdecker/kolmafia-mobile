package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionBuyables
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionPermitted
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.item.CreateItemIngredients
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [SausageOMaticRequest] — grind magical sausages. */
class SausageOMaticCreateRequest(
    private val client: HttpClient,
    private val createItemIngredients: CreateItemIngredients,
    private val retrieveItemService: RetrieveItemService?,
    private val gameDatabase: GameDatabase?,
    private val preferences: Preferences?,
    private val inventoryManager: InventoryManager?,
) {
    suspend fun create(
        concoction: ConcoctionData,
        quantity: Int,
        state: CharacterState?,
        preferences: Preferences?,
    ): Result<Int> {
        if (quantity <= 0) return Result.success(0)
        if (!concoction.result.equals("magical sausage", ignoreCase = true)) {
            return Result.failure(IllegalStateException("Cannot create ${concoction.result}"))
        }
        if (CreateAbortGate.shouldAbort()) return Result.success(0)
        val prefs = preferences ?: this.preferences
        if (state != null &&
            !ConcoctionPermitted.isPermittedMethod(concoction, state, prefs = prefs, limitMode = state.limitMode)
        ) {
            return Result.failure(IllegalStateException("SAUSAGE_O_MATIC not permitted"))
        }
        if (!createItemIngredients.makeIngredients(concoction, quantity, state)) {
            return Result.success(0)
        }

        var sausagesMade = prefs?.getInt("_sausagesMade", 0) ?: 0
        var grinderUnits = prefs?.getInt("sausageGrinderUnits", 0) ?: 0
        var meatNeeded = 0
        for (i in 0 until quantity) {
            meatNeeded += (sausagesMade + 1 + i) * 111
        }
        val deficit = (meatNeeded - grinderUnits).coerceAtLeast(0)
        val dense = deficit / 1000
        val stacks = (deficit - dense * 1000) / 100
        val paste = ((deficit - dense * 1000 - stacks * 100 + 9) / 10).coerceAtLeast(0)

        retrieveItemService?.retrieve(DENSE_STACK, dense)
        retrieveItemService?.retrieve(MEAT_STACK, stacks)
        retrieveItemService?.retrieve(ConcoctionBuyables.MEAT_PASTE, paste)

        return try {
            client.get("$KOL_BASE_URL/inventory.php") { parameter("action", "grind") }
            if (dense > 0) {
                client.submitForm(
                    "$KOL_BASE_URL/choice.php",
                    Parameters.build {
                        append("whichchoice", "1115")
                        append("option", "1")
                        append("qty", dense.toString())
                    },
                )
            }
            if (stacks > 0) {
                client.submitForm(
                    "$KOL_BASE_URL/choice.php",
                    Parameters.build {
                        append("whichchoice", "1115")
                        append("option", "2")
                        append("qty", stacks.toString())
                    },
                )
            }
            if (paste > 0) {
                client.submitForm(
                    "$KOL_BASE_URL/choice.php",
                    Parameters.build {
                        append("whichchoice", "1115")
                        append("option", "3")
                        append("qty", paste.toString())
                    },
                )
            }
            var created = 0
            repeat(quantity) {
                val resp = client.submitForm(
                    "$KOL_BASE_URL/choice.php",
                    Parameters.build {
                        append("whichchoice", "1115")
                        append("option", "4")
                    },
                )
                val body = resp.bodyAsText()
                if (!body.contains("You acquire", ignoreCase = true)) {
                    return@repeat
                }
                created++
                sausagesMade++
            }
            prefs?.setInt("_sausagesMade", sausagesMade)
            prefs?.setInt("sausageGrinderUnits", 0)
            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val DENSE_STACK = 209
        const val MEAT_STACK = 217
        const val CASING = 10059
    }
}

/** Desktop [BurningLeavesRequest]. */
class BurningLeavesCreateRequest(
    private val client: HttpClient,
    private val choiceRequest: ChoiceRequest,
    private val createItemIngredients: CreateItemIngredients,
    private val gameDatabase: GameDatabase?,
    private val preferences: Preferences?,
) {
    suspend fun create(
        concoction: ConcoctionData,
        quantity: Int,
        state: CharacterState?,
        preferences: Preferences?,
    ): Result<Int> {
        if (quantity <= 0) return Result.success(0)
        if (CreateAbortGate.shouldAbort()) return Result.success(0)
        val prefs = preferences ?: this.preferences
        val resultId = gameDatabase?.item(concoction.result)?.id
            ?: ItemDatabase.getByName(concoction.result)?.id
            ?: return Result.failure(IllegalStateException("Unknown: ${concoction.result}"))
        val leaves = LEAVES_BY_ITEM[resultId]
            ?: concoction.ingredients.firstOrNull()?.quantity
            ?: return Result.failure(IllegalStateException("No leaf cost for ${concoction.result}"))

        if (!createItemIngredients.makeIngredients(concoction, quantity, state)) {
            return Result.success(0)
        }

        return try {
            client.get("$KOL_BASE_URL/campground.php") { parameter("preaction", "leaves") }
            var created = 0
            repeat(quantity) {
                val body = choiceRequest.choose(
                    CHOICE_ID,
                    1,
                    mapOf("leaves" to leaves.toString()),
                ).getOrElse { return Result.success(created) }.first
                if (!body.contains("You acquire", ignoreCase = true)) {
                    return Result.success(created)
                }
                created++
                DAILY_PREF_BY_ITEM[resultId]?.let { (pref, max) ->
                    if (max == 1) prefs?.setBoolean(pref, true)
                    else prefs?.setInt(pref, (prefs.getInt(pref, 0) + 1).coerceAtMost(max))
                }
            }
            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val CHOICE_ID = 1510
        val LEAVES_BY_ITEM = mapOf(
            11342 to 37, // autumnic bomb
            11343 to 42,
            11344 to 43,
            11345 to 44,
            11346 to 50,
            11347 to 66,
            11348 to 69,
            11349 to 74,
            11350 to 99,
            11364 to 222,
            11365 to 1111,
            11366 to 6666,
            11367 to 11111,
        )
        val DAILY_PREF_BY_ITEM = mapOf(
            11348 to ("_leafLassosCrafted" to 3),
            11364 to ("_leafDayShortenerCrafted" to 1),
            11366 to ("_leafcutterAntEggCrafted" to 1),
            11367 to ("_leafTattooCrafted" to 1),
        )
    }
}
