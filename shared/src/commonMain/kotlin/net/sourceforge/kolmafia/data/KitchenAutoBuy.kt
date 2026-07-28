package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.ZodiacSign
import net.sourceforge.kolmafia.inventory.LimitModeGates
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop ConcoctionDatabase.cachePermitted willBuyTool / willBuyServant helpers. */
object KitchenAutoBuy {

    fun toolCost(state: CharacterState): Int =
        if (ZodiacSign.find(state.zodiacSign)?.isBadMoon == true) 500 else 1000

    fun willBuyTool(
        state: CharacterState,
        prefs: Preferences?,
        limitMode: String = "none",
    ): Boolean {
        if (state.meat < toolCost(state)) return false
        if (prefs?.getBoolean("autoSatisfyWithNPCs", false) != true) return false
        if (LimitModeGates.limitNPCStores(limitMode)) return false
        return true
    }

    fun willBuyServant(
        prefs: Preferences?,
        state: CharacterState,
        limitMode: String = "none",
    ): Boolean {
        if (prefs?.getBoolean("autoRepairBoxServants", false) != true) return false
        if (state.inGLover) return false
        return canUseMall(prefs, limitMode) || canUseClanStash(prefs, limitMode)
    }

    private fun canUseMall(prefs: Preferences?, limitMode: String): Boolean {
        if (prefs?.getBoolean("autoSatisfyWithMall", false) != true) return false
        if (LimitModeGates.limitMall(limitMode)) return false
        return true
    }

    private fun canUseClanStash(prefs: Preferences?, limitMode: String): Boolean {
        if (prefs?.getBoolean("autoSatisfyWithStash", false) != true) return false
        if (LimitModeGates.limitClan(limitMode)) return false
        return true
    }
}
