package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.ConcoctionConsumptionType
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ConcoctionQueueRunner
import net.sourceforge.kolmafia.http.KOL_BASE_URL

/** Desktop StillSuitRequest — distill + choice 1476 for stillsuit distillate queue drain. */
open class StillSuitRequest(
    private val client: HttpClient,
    private val inventoryManager: InventoryManager? = null,
) {

    open suspend fun distill(
        name: String,
        type: ConcoctionConsumptionType,
        state: CharacterState?,
        prefs: Preferences?,
        inventoryCountById: (Int) -> Int = { defaultInventoryCount(it) },
        isEquipped: (Int) -> Boolean = { defaultIsEquipped(it, state) },
    ): Result<Unit> {
        preflight(name, type, state, prefs, inventoryCountById, isEquipped)
            .onFailure { return Result.failure(it) }
        return runDistillHttp()
    }

    private fun defaultInventoryCount(itemId: Int): Int =
        inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0

    private fun defaultIsEquipped(itemId: Int, state: CharacterState?): Boolean {
        if (state == null) return false
        val itemName = ItemDatabase.getById(itemId)?.name ?: return false
        return state.equipment[EquipmentSlot.FAMILIAR]?.equals(itemName, ignoreCase = true) == true
    }

    internal suspend fun runDistillHttp(): Result<Unit> = try {
        val distill = client.get("$KOL_BASE_URL/inventory.php") {
            parameter("action", "distill")
        }
        if (!distill.status.isSuccess()) {
            Result.failure(Exception("HTTP ${distill.status.value}"))
        } else {
            val choice = client.get("$KOL_BASE_URL/choice.php") {
                parameter("whichchoice", DISTILL_CHOICE)
                parameter("option", 1)
            }
            if (choice.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${choice.status.value}"))
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    companion object {
        const val STILLSUIT_ITEM_ID = 10932
        const val DISTILLATE_NAME = "stillsuit distillate"
        private const val DISTILL_CHOICE = 1476
        private const val MIN_FAMILIAR_SWEAT = 10

        fun isDistillate(name: String): Boolean =
            name.equals(DISTILLATE_NAME, ignoreCase = true)

        fun canMake(
            prefs: Preferences?,
            inventoryCountById: (Int) -> Int,
            isEquipped: (Int) -> Boolean,
        ): Result<Unit> {
            if (inventoryCountById(STILLSUIT_ITEM_ID) <= 0 && !isEquipped(STILLSUIT_ITEM_ID)) {
                return Result.failure(IllegalStateException("You don't have a tiny stillsuit"))
            }
            if ((prefs?.getInt("familiarSweat", 0) ?: 0) < MIN_FAMILIAR_SWEAT) {
                return Result.failure(
                    IllegalStateException(
                        "You need at least 10 drams of familiar sweat to drink the delicious distillate.",
                    ),
                )
            }
            return Result.success(Unit)
        }

        internal fun preflight(
            name: String,
            type: ConcoctionConsumptionType,
            state: CharacterState?,
            prefs: Preferences?,
            inventoryCountById: (Int) -> Int,
            isEquipped: (Int) -> Boolean,
        ): Result<Unit> {
            if (!isDistillate(name)) {
                return Result.failure(IllegalStateException("Not stillsuit distillate: $name"))
            }
            if (ConcoctionDatabase.getByResult(name) == null) {
                return Result.failure(IllegalStateException("No concoction for: $name"))
            }
            ConcoctionQueueRunner.preflightCafeConsume(name, type, state, prefs)
                .onFailure { return Result.failure(it) }
            return canMake(prefs, inventoryCountById, isEquipped)
        }
    }
}
