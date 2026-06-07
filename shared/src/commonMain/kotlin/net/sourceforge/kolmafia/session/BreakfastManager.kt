package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CampgroundRequest
import net.sourceforge.kolmafia.request.ClanLoungeRequest
import net.sourceforge.kolmafia.request.ClanRumpusRequest

open class BreakfastManager(
    private val campgroundRequest: CampgroundRequest,
    private val clanRumpusRequest: ClanRumpusRequest,
    private val clanLoungeRequest: ClanLoungeRequest,
    private val preferences: Preferences,
) {
    companion object {
        const val VIP_LOUNGE_KEY_ID   = 5479
        const val POCKET_WISH_ITEM_ID = 8765
        const val MUS_MANUAL_ID       = 11
        const val MYS_MANUAL_ID       = 172
        const val MOX_MANUAL_ID       = 173
    }

    open suspend fun runBreakfast(charState: CharacterState, inventoryState: InventoryState) {
        if (preferences.getBoolean(Preferences.BREAKFAST_COMPLETED, false)) return

        val suffix = if (charState.isHardcore) "Hardcore" else "Softcore"

        harvestGarden(suffix)
        checkRumpusRoom(suffix)
        checkVIPLounge(suffix, inventoryState)
        readGuildManual(suffix, charState, inventoryState)
        makePocketWishes(inventoryState)

        preferences.setBoolean(Preferences.BREAKFAST_COMPLETED, true)
    }

    open fun clearBreakfastPrefs() {
        preferences.setBoolean(Preferences.BREAKFAST_COMPLETED, false)
        preferences.setBoolean(Preferences.GARDEN_HARVESTED, false)
        preferences.setBoolean(Preferences.BREAKFAST_RUMPUS, false)
        preferences.setBoolean(Preferences.GUILD_MANUAL_USED, false)
        preferences.setInt(Preferences.DELUXE_KLAW_SUMMONS, 0)
        preferences.setBoolean(Preferences.LOOKING_GLASS, false)
        preferences.setBoolean(Preferences.FIREWORKS_SHOP, false)
        preferences.setInt(Preferences.POOL_GAME_RESULT, 0)
    }

    private suspend fun harvestGarden(suffix: String) {
        val harvestPrefKey = if (suffix == "Softcore") Preferences.HARVEST_GARDEN_SOFTCORE else Preferences.HARVEST_GARDEN_HARDCORE
        val crop = preferences.getString(harvestPrefKey, "none")
        if (crop.equals("none", ignoreCase = true)) return
        if (preferences.getBoolean(Preferences.GARDEN_HARVESTED, false)) return
        campgroundRequest.harvestGarden().onSuccess {
            preferences.setBoolean(Preferences.GARDEN_HARVESTED, true)
        }
    }

    private suspend fun checkRumpusRoom(suffix: String) {
        val rumpusPrefKey = if (suffix == "Softcore") Preferences.VISIT_RUMPUS_SOFTCORE else Preferences.VISIT_RUMPUS_HARDCORE
        if (!preferences.getBoolean(rumpusPrefKey, true)) return
        if (preferences.getBoolean(Preferences.BREAKFAST_RUMPUS, false)) return
        clanRumpusRequest.visit().onSuccess {
            preferences.setBoolean(Preferences.BREAKFAST_RUMPUS, true)
        }
    }

    private suspend fun checkVIPLounge(suffix: String, inventoryState: InventoryState) {
        val loungePrefKey = if (suffix == "Softcore") Preferences.VISIT_LOUNGE_SOFTCORE else Preferences.VISIT_LOUNGE_HARDCORE
        if (!preferences.getBoolean(loungePrefKey, true)) return
        if (!inventoryState.items.containsKey(VIP_LOUNGE_KEY_ID)) return

        while (true) {
            val current = preferences.getInt(Preferences.DELUXE_KLAW_SUMMONS, 0)
            if (current >= 3) break
            val result = clanLoungeRequest.useKlaw()
            if (result.isFailure) break
            preferences.setInt(Preferences.DELUXE_KLAW_SUMMONS, current + 1)
        }

        if (!preferences.getBoolean(Preferences.LOOKING_GLASS, false)) {
            clanLoungeRequest.useLookingGlass().onSuccess {
                preferences.setBoolean(Preferences.LOOKING_GLASS, true)
            }
        }

        if (!preferences.getBoolean(Preferences.FIREWORKS_SHOP, false)) {
            clanLoungeRequest.visitFireworks().onSuccess {
                preferences.setBoolean(Preferences.FIREWORKS_SHOP, true)
            }
        }

        if (preferences.getInt(Preferences.POOL_GAME_RESULT, 0) < 1) {
            clanLoungeRequest.playPoolGame().onSuccess {
                preferences.setInt(Preferences.POOL_GAME_RESULT, 1)
            }
        }
    }

    private suspend fun readGuildManual(
        suffix: String,
        charState: CharacterState,
        inventoryState: InventoryState,
    ) {
        val manualPrefKey = if (suffix == "Softcore") Preferences.READ_MANUAL_SOFTCORE else Preferences.READ_MANUAL_HARDCORE
        if (!preferences.getBoolean(manualPrefKey, true)) return
        if (preferences.getBoolean(Preferences.GUILD_MANUAL_USED, false)) return
        val manualId = when {
            charState.characterClassEnum.isMuscleBased -> MUS_MANUAL_ID
            charState.characterClassEnum.isMysticality  -> MYS_MANUAL_ID
            else                                        -> MOX_MANUAL_ID
        }
        if (!inventoryState.items.containsKey(manualId)) return
        // Guild manual item is in inventory but HTTP use is not yet implemented.
        // Sentinel will be set once the actual inv_use.php call is implemented.
        // For now, this is a detection-only stub.
    }

    private suspend fun makePocketWishes(inventoryState: InventoryState) {
        // Stub: pocket wish choice handling deferred to adventure loop.
        if (!inventoryState.items.containsKey(POCKET_WISH_ITEM_ID)) return
    }
}
