package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.CafeAccessibility
import net.sourceforge.kolmafia.data.ChezSnooteeDatabase
import net.sourceforge.kolmafia.data.ConcoctionConsumptionType
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ResultProcessor

open class ChezSnooteeRequest(
    private val hellKitchenRequest: HellKitchenRequest,
    private val cafeRequest: CafeRequest? = null,
) {
    fun onMenu(name: String): Boolean = ChezSnooteeDatabase.isOnMenu(name)

    /** Desktop [ChezSnooteeRequest.getDailySpecial] — visit menu when pref empty. */
    open suspend fun ensureDailySpecial(state: CharacterState?, prefs: Preferences?): String? {
        if (!CafeAccessibility.isChezSnooteeAvailable(state)) return null
        CafeDailySpecialSync.currentSpecialName(prefs)?.let { return it }
        cafeRequest?.visitMenu("1", prefs)
        return CafeDailySpecialSync.currentSpecialName(prefs)
    }

    open suspend fun purchase(
        name: String,
        type: ConcoctionConsumptionType,
        state: CharacterState?,
        prefs: Preferences?,
        larpCount: Int = 0,
    ): Result<Unit> {
        if (!CafeAccessibility.isChezSnooteeAvailable(state)) {
            return Result.failure(IllegalStateException("Chez Snootée not available"))
        }
        val entry = ChezSnooteeDatabase.resolve(name)
            ?: return Result.failure(IllegalStateException("Unknown Chez Snootée item: $name"))
        return hellKitchenRequest.purchaseEntry(entry, type, state, prefs, larpCount)
    }

    companion object {
        /**
         * Desktop [ChezSnooteeRequest.parseResponse] — cafeid=1 CONSUME food-helper + fullness.
         */
        fun parseResponse(
            urlString: String,
            responseText: String,
            preferences: Preferences?,
            inventory: InventoryManager? = ResultProcessor.inventoryProvider?.invoke(),
            character: KoLCharacter? = null,
        ) {
            if (!urlString.contains("cafe.php", ignoreCase = true) ||
                !urlString.contains("cafeid=1")
            ) {
                return
            }
            if (!urlString.contains("action=CONSUME", ignoreCase = true)) {
                CafeDailySpecialSync.parseResponse(urlString, responseText, preferences)
                return
            }
            if (!responseText.contains("You gain")) return
            val itemId = Regex("""[?&]whichitem=(\d+)""", RegexOption.IGNORE_CASE)
                .find(urlString)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val itemName = if (itemId > 0) ItemDatabase.getItemName(itemId) else ""
            EatFoodRequest.handleFoodHelper(
                itemName = itemName,
                count = 1,
                responseText = responseText,
                preferences = preferences,
                inventory = inventory,
                character = character,
                adjustFullness = true,
            )
        }
    }
}
