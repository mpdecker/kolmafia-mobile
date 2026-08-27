package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.BoxServantAvailability
import net.sourceforge.kolmafia.data.KitchenAutoBuy
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [CreateItemRequest.autoRepairBoxServant] create preflight (Phases 2196–2210).
 */
object CreateBoxServantRepair {
    const val TENDER_HAMMER = 338
    const val RANGE = 236
    const val COCKTAIL_KIT = 237

    /**
     * @param method craft method token e.g. COOK_FANCY / MIX_FANCY / SMITH / SSMITH
     * @return false when repair is required but could not be completed
     */
    suspend fun autoRepair(
        method: String,
        state: CharacterState,
        preferences: Preferences?,
        retrieveItemService: RetrieveItemService?,
        useItemRequest: UseItemRequest?,
        inventoryManager: InventoryManager?,
        accessibleCount: (Int) -> Int = { id ->
            inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0
        },
    ): Boolean {
        if (CreateAbortGate.shouldAbort()) return false
        val prefs = preferences ?: return true
        val methodUpper = method.uppercase()

        when (methodUpper) {
            "SMITH", "SSMITH" -> {
                retrieveItemService?.retrieve(TENDER_HAMMER, 1)
                return accessibleCount(TENDER_HAMMER) > 0 ||
                    (methodUpper == "SMITH" && state.knollAvailable && !state.inZombiecore)
            }
            "COOK_FANCY", "COOK" -> {
                if (prefs.getBoolean("hasRange", false) != true) {
                    retrieveItemService?.retrieve(RANGE, 1)
                    if (accessibleCount(RANGE) <= 0) return false
                    useItemRequest?.use(RANGE, 1)
                    prefs.setBoolean("hasRange", true)
                }
                if (prefs.getBoolean("hasChef", false)) return true
                return useBoxServant(
                    BoxServantAvailability.CHEF,
                    BoxServantAvailability.CLOCKWORK_CHEF,
                    "hasChef",
                    state,
                    prefs,
                    retrieveItemService,
                    useItemRequest,
                    accessibleCount,
                )
            }
            "MIX_FANCY", "COCKTAIL" -> {
                if (prefs.getBoolean("hasCocktailKit", false) != true) {
                    retrieveItemService?.retrieve(COCKTAIL_KIT, 1)
                    if (accessibleCount(COCKTAIL_KIT) <= 0) return false
                    useItemRequest?.use(COCKTAIL_KIT, 1)
                    prefs.setBoolean("hasCocktailKit", true)
                }
                if (prefs.getBoolean("hasBartender", false)) return true
                return useBoxServant(
                    BoxServantAvailability.BARTENDER,
                    BoxServantAvailability.CLOCKWORK_BARTENDER,
                    "hasBartender",
                    state,
                    prefs,
                    retrieveItemService,
                    useItemRequest,
                    accessibleCount,
                )
            }
            else -> return true
        }
    }

    private suspend fun useBoxServant(
        servantId: Int,
        clockworkId: Int,
        prefKey: String,
        state: CharacterState,
        prefs: Preferences,
        retrieveItemService: RetrieveItemService?,
        useItemRequest: UseItemRequest?,
        accessibleCount: (Int) -> Int,
    ): Boolean {
        if (state.inGLover) return true
        if (!prefs.getBoolean("autoRepairBoxServants", false)) {
            return !prefs.getBoolean("requireBoxServants", false) &&
                (state.adventuresLeft > 0)
        }
        if (!KitchenAutoBuy.willBuyServant(prefs, state, state.limitMode) &&
            accessibleCount(servantId) <= 0 && accessibleCount(clockworkId) <= 0
        ) {
            return !prefs.getBoolean("requireBoxServants", false)
        }
        val target = when {
            accessibleCount(clockworkId) > 0 -> clockworkId
            accessibleCount(servantId) > 0 -> servantId
            else -> {
                retrieveItemService?.retrieve(servantId, 1)
                if (accessibleCount(servantId) > 0) servantId
                else {
                    retrieveItemService?.retrieve(clockworkId, 1)
                    if (accessibleCount(clockworkId) > 0) clockworkId else return false
                }
            }
        }
        useItemRequest?.use(target, 1)
        prefs.setBoolean(prefKey, true)
        return true
    }
}
