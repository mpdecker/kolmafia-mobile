package net.sourceforge.kolmafia.session

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CampgroundRequest
import net.sourceforge.kolmafia.request.ClanLoungeRequest
import net.sourceforge.kolmafia.request.ClanRumpusRequest
import net.sourceforge.kolmafia.request.HermitRequest
import net.sourceforge.kolmafia.request.UseItemRequest

open class BreakfastManager(
    private val campgroundRequest: CampgroundRequest,
    private val clanRumpusRequest: ClanRumpusRequest,
    private val clanLoungeRequest: ClanLoungeRequest,
    private val preferences: Preferences,
    private val useItemRequest: UseItemRequest,
    private val hermitRequest: HermitRequest,
    private val httpClient: HttpClient,
) {
    companion object {
        const val VIP_LOUNGE_KEY_ID   = 5479
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
        getHermitClovers(inventoryState)
        collectHardwood()
        collect2002MrStoreCredits(inventoryState)
        collectAprilShowerGlobs(inventoryState)
        useSpinningWheel()
        visitBigIsland()
        visitVolcanoIsland()
        makePocketWishes(inventoryState)
        haveBoxingDaydream()
        useToys(inventoryState)
        collectAnticheese(inventoryState)
        visitServerRoom()
        harvestBatteries(inventoryState)
        useBookOfEverySkill(inventoryState)
        useReplicaBooks(inventoryState)
        makeHandheldRadios(inventoryState)

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
        // Phase 13 sentinels
        preferences.setBoolean(Preferences.CLOVER_SOUGHT, false)
        preferences.setBoolean(Preferences.APRIL_SHOWER_GLOBS, false)
        preferences.setBoolean(Preferences.BOOK_OF_EVERY_SKILL_USED, false)
        preferences.setBoolean(Preferences.REPLICA_SNOWCONE_USED, false)
        preferences.setBoolean(Preferences.REPLICA_RESOLUTION_USED, false)
        preferences.setBoolean(Preferences.REPLICA_SMITH_USED, false)
        preferences.setBoolean(Preferences.HAND_RADIO_USED, false)
        preferences.setBoolean(Preferences.ANTICHEESE_COLLECTED, false)
        preferences.setBoolean(Preferences.BATTERIES_HARVESTED, false)
        preferences.setBoolean(Preferences.POCKET_WISHES_USED, false)
        preferences.setBoolean(Preferences.BOXING_DAYDREAM, false)
        preferences.setBoolean(Preferences.SPINNING_WHEEL_USED, false)
        preferences.setBoolean(Preferences.BIG_ISLAND_VISITED, false)
        preferences.setBoolean(Preferences.VOLCANO_ISLAND_VISITED, false)
        preferences.setBoolean(Preferences.HARDWOOD_COLLECTED, false)
        preferences.setBoolean(Preferences.MR_STORE_CREDITS_COLLECTED, false)
        preferences.setBoolean(Preferences.SERVER_ROOM_VISITED, false)
        // Clear per-toy sentinels
        for (toyId in BreakfastItemIds.TOYS.keys) {
            preferences.setBoolean("_toyUsed_$toyId", false)
        }
    }

    private suspend fun httpGet(path: String): Result<String> = try {
        val response = httpClient.get("$KOL_BASE_URL/$path")
        if (response.status.isSuccess()) Result.success(response.bodyAsText())
        else Result.failure(Exception("HTTP ${response.status.value}"))
    } catch (e: Exception) { Result.failure(e) }

    private suspend fun httpPost(path: String, params: Map<String, String>): Result<String> = try {
        val response = httpClient.submitForm(
            "$KOL_BASE_URL/$path",
            formParameters = Parameters.build { params.forEach { (k, v) -> append(k, v) } }
        )
        if (response.status.isSuccess()) Result.success(response.bodyAsText())
        else Result.failure(Exception("HTTP ${response.status.value}"))
    } catch (e: Exception) { Result.failure(e) }

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
        useGuildManual(manualId)
    }

    private suspend fun useGuildManual(manualId: Int) {
        if (preferences.getBoolean(Preferences.GUILD_MANUAL_USED, false)) return
        val result = useItemRequest.use(manualId, 1)
        if (result.isSuccess) {
            preferences.setBoolean(Preferences.GUILD_MANUAL_USED, true)
        }
    }

    // ── Tier 1 action methods ─────────────────────────────────────────────────

    private suspend fun getHermitClovers(inventoryState: InventoryState) {
        if (preferences.getBoolean(Preferences.CLOVER_SOUGHT, false)) return
        val hasWorthless = listOf(
            BreakfastItemIds.WORTHLESS_TRINKET_ID,
            BreakfastItemIds.WORTHLESS_KNICK_KNACK_ID,
            BreakfastItemIds.WORTHLESS_GEWGAW_ID,
        ).any { inventoryState.items.containsKey(it) }
        if (!hasWorthless) return
        hermitRequest.trade(BreakfastItemIds.CLOVER_ITEM_ID, 1).onSuccess {
            preferences.setBoolean(Preferences.CLOVER_SOUGHT, true)
        }
    }

    private suspend fun collectAprilShowerGlobs(inventoryState: InventoryState) {
        if (preferences.getBoolean(Preferences.APRIL_SHOWER_GLOBS, false)) return
        if (!inventoryState.items.containsKey(BreakfastItemIds.APRIL_SHOWER_THOUGHTS_SHIELD)) return
        httpGet("inventory.php?action=shower").onSuccess {
            preferences.setBoolean(Preferences.APRIL_SHOWER_GLOBS, true)
        }
    }

    private suspend fun useBookOfEverySkill(inventoryState: InventoryState) {
        if (preferences.getBoolean(Preferences.BOOK_OF_EVERY_SKILL_USED, false)) return
        if (!inventoryState.items.containsKey(BreakfastItemIds.BOOK_OF_EVERY_SKILL_ID)) return
        useItemRequest.use(BreakfastItemIds.BOOK_OF_EVERY_SKILL_ID, 1).onSuccess {
            preferences.setBoolean(Preferences.BOOK_OF_EVERY_SKILL_USED, true)
        }
    }

    private suspend fun useReplicaBooks(inventoryState: InventoryState) {
        if (!preferences.getBoolean(Preferences.REPLICA_SNOWCONE_USED, false)
                && inventoryState.items.containsKey(BreakfastItemIds.REPLICA_SNOWCONE_ID)) {
            useItemRequest.use(BreakfastItemIds.REPLICA_SNOWCONE_ID, 1).onSuccess {
                preferences.setBoolean(Preferences.REPLICA_SNOWCONE_USED, true)
            }
        }
        if (!preferences.getBoolean(Preferences.REPLICA_RESOLUTION_USED, false)
                && inventoryState.items.containsKey(BreakfastItemIds.REPLICA_RESOLUTION_ID)) {
            useItemRequest.use(BreakfastItemIds.REPLICA_RESOLUTION_ID, 1).onSuccess {
                preferences.setBoolean(Preferences.REPLICA_RESOLUTION_USED, true)
            }
        }
        if (!preferences.getBoolean(Preferences.REPLICA_SMITH_USED, false)
                && inventoryState.items.containsKey(BreakfastItemIds.REPLICA_SMITH_ID)) {
            useItemRequest.use(BreakfastItemIds.REPLICA_SMITH_ID, 1).onSuccess {
                preferences.setBoolean(Preferences.REPLICA_SMITH_USED, true)
            }
        }
    }

    private suspend fun makeHandheldRadios(inventoryState: InventoryState) {
        if (preferences.getBoolean(Preferences.HAND_RADIO_USED, false)) return
        if (!inventoryState.items.containsKey(BreakfastItemIds.ALLIED_RADIO_BACKPACK_ID)) return
        useItemRequest.use(BreakfastItemIds.ALLIED_RADIO_BACKPACK_ID, 1).onSuccess {
            preferences.setBoolean(Preferences.HAND_RADIO_USED, true)
        }
    }

    private suspend fun collectAnticheese(inventoryState: InventoryState) {
        if (preferences.getBoolean(Preferences.ANTICHEESE_COLLECTED, false)) return
        val lastAnticheeseDay = preferences.getInt(Preferences.LAST_ANTICHEESE_DAY, -1)
        val currentDays = preferences.getInt(Preferences.LAST_DAYCOUNT, -1)
        if (lastAnticheeseDay >= 0 && currentDays >= 0 && currentDays < lastAnticheeseDay + 5) return
        if (!inventoryState.items.containsKey(BreakfastItemIds.ANTICHEESE_ID)) return
        useItemRequest.use(BreakfastItemIds.ANTICHEESE_ID, 1).onSuccess {
            preferences.setBoolean(Preferences.ANTICHEESE_COLLECTED, true)
            if (currentDays >= 0) preferences.setInt(Preferences.LAST_ANTICHEESE_DAY, currentDays)
        }
    }

    private suspend fun harvestBatteries(inventoryState: InventoryState) {
        if (preferences.getBoolean(Preferences.BATTERIES_HARVESTED, false)) return
        val plantId = ItemDatabase.getByName("potted power plant")?.id ?: return
        if (!inventoryState.items.containsKey(plantId)) return
        useItemRequest.use(plantId, 1).onSuccess {
            preferences.setBoolean(Preferences.BATTERIES_HARVESTED, true)
        }
    }

    // ── Tier 2/3 stubs — filled in later tasks ────────────────────────────────

    private suspend fun useSpinningWheel() {
        if (preferences.getBoolean(Preferences.SPINNING_WHEEL_USED, false)) return
        campgroundRequest.useSpinningWheel().onSuccess {
            preferences.setBoolean(Preferences.SPINNING_WHEEL_USED, true)
        }
    }

    private suspend fun makePocketWishes(inventoryState: InventoryState) {
        if (preferences.getBoolean(Preferences.POCKET_WISHES_USED, false)) return
        val bottleId = when {
            inventoryState.items.containsKey(BreakfastItemIds.GENIE_BOTTLE_ID) ->
                BreakfastItemIds.GENIE_BOTTLE_ID
            inventoryState.items.containsKey(BreakfastItemIds.REPLICA_GENIE_BOTTLE_ID) ->
                BreakfastItemIds.REPLICA_GENIE_BOTTLE_ID
            else -> return
        }
        useItemRequest.use(bottleId, 1).onSuccess { html ->
            if (html.contains("whichchoice")) {
                val choiceId = Regex("whichchoice=(\\d+)").find(html)?.groupValues?.get(1) ?: "1"
                httpPost("choice.php", mapOf("whichchoice" to choiceId, "option" to "3"))
            }
            preferences.setBoolean(Preferences.POCKET_WISHES_USED, true)
        }
    }

    private suspend fun haveBoxingDaydream() {
        if (preferences.getBoolean(Preferences.BOXING_DAYDREAM, false)) return
        httpGet("place.php?whichplace=town_wrong&action=townwrong_boxingdaycare").onSuccess { html ->
            if (html.contains("whichchoice")) {
                val choiceId = Regex("whichchoice=(\\d+)").find(html)?.groupValues?.get(1) ?: "1261"
                httpPost("choice.php", mapOf("whichchoice" to choiceId, "option" to "1"))
            }
            preferences.setBoolean(Preferences.BOXING_DAYDREAM, true)
        }
    }
    private suspend fun useToys(inventoryState: InventoryState) {
        for ((toyId, dailyCount) in BreakfastItemIds.TOYS) {
            val sentinelKey = "_toyUsed_$toyId"
            if (preferences.getBoolean(sentinelKey, false)) continue
            if (!inventoryState.items.containsKey(toyId)) continue
            try {
                useItemRequest.use(toyId, dailyCount).onSuccess {
                    preferences.setBoolean(sentinelKey, true)
                }
            } catch (_: Exception) {
                // best-effort; continue to next toy
            }
        }
    }
    private suspend fun collectHardwood() {}
    private suspend fun collect2002MrStoreCredits(inventoryState: InventoryState) {}
    private suspend fun visitBigIsland() {}
    private suspend fun visitVolcanoIsland() {}
    private suspend fun visitServerRoom() {}
}
