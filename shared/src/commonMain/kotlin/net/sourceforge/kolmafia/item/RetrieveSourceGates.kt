package net.sourceforge.kolmafia.item

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.inventory.LimitModeGates
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.StoragePullRules

/**
 * Desktop [InventoryManager.canUseCloset]/[canUseStorage]/[canUseClanStash]/[canUseMall]
 * preference + limit-mode gates for the live retrieve path (Phases 2511–2525).
 *
 * When [prefs] is null (incomplete DI / legacy tests), sources are allowed.
 */
object RetrieveSourceGates {

    fun canInteract(state: CharacterState?): Boolean =
        StoragePullRules.canInteract(state)

    fun canUseCloset(prefs: Preferences?, state: CharacterState?): Boolean {
        prefs ?: return true
        if (!prefs.getBoolean("autoSatisfyWithCloset", false)) return false
        val limit = state?.limitMode.orEmpty()
        return !LimitModeGates.limitCampground(limit)
    }

    fun canUseStorage(prefs: Preferences?, state: CharacterState?): Boolean {
        prefs ?: return true
        if (!canInteract(state)) return false
        if (!prefs.getBoolean("autoSatisfyWithStorage", true)) return false
        val limit = state?.limitMode.orEmpty()
        return !LimitModeGates.limitStorage(limit)
    }

    fun canUseClanStash(prefs: Preferences?, state: CharacterState?): Boolean {
        prefs ?: return true
        if (!canInteract(state)) return false
        if (!prefs.getBoolean("autoSatisfyWithStash", false)) return false
        if (state?.hasClan != true) return false
        val limit = state.limitMode
        return !LimitModeGates.limitClan(limit)
    }

    fun canUseDisplay(prefs: Preferences?): Boolean {
        prefs ?: return true
        return prefs.getBoolean("autoSatisfyWithDisplay", true)
    }

    fun canUseNPCStores(prefs: Preferences?, state: CharacterState?): Boolean {
        prefs ?: return true
        if (!prefs.getBoolean("autoSatisfyWithNPCs", false)) return false
        val limit = state?.limitMode.orEmpty()
        return !LimitModeGates.limitNPCStores(limit)
    }

    fun canUseMall(prefs: Preferences?, state: CharacterState?, tradeable: Boolean): Boolean {
        prefs ?: return true
        if (!tradeable) return false
        if (!canInteract(state)) return false
        if (!prefs.getBoolean("autoSatisfyWithMall", false)) return false
        val limit = state?.limitMode.orEmpty()
        return !LimitModeGates.limitMall(limit)
    }

    fun canUseCoinmasters(prefs: Preferences?, state: CharacterState?): Boolean {
        prefs ?: return true
        if (!prefs.getBoolean("autoSatisfyWithCoinmasters", false)) return false
        val limit = state?.limitMode.orEmpty()
        return !LimitModeGates.limitCoinmasters(limit)
    }

    fun canUseShop(prefs: Preferences?, state: CharacterState?, tradeable: Boolean): Boolean =
        canUseMall(prefs, state, tradeable) &&
            (prefs == null || prefs.getBoolean("autoSatisfyWithShop", false))
}
