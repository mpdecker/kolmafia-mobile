package net.sourceforge.kolmafia.session

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.campground.GardenCropAvailability
import net.sourceforge.kolmafia.clan.ClanHotdogMenuCache
import net.sourceforge.kolmafia.clan.ClanLoungeSync
import net.sourceforge.kolmafia.adventure.choice.OutfitPool
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.OutfitDatabase
import net.sourceforge.kolmafia.equipment.OutfitManager
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.LimitModeGates
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.IslandWarActionResponseSync
import net.sourceforge.kolmafia.quest.IslandWarPaths
import net.sourceforge.kolmafia.quest.IslandWarVisitSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.request.CampgroundRequest
import net.sourceforge.kolmafia.request.ClanLoungeRequest
import net.sourceforge.kolmafia.request.ClanRumpusRequest
import net.sourceforge.kolmafia.request.HermitRequest
import net.sourceforge.kolmafia.request.UseItemRequest
import net.sourceforge.kolmafia.shop.NpcShopSync
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.mood.BreakfastBurnSkills
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState

open class BreakfastManager(
    private val campgroundRequest: CampgroundRequest,
    private val clanRumpusRequest: ClanRumpusRequest,
    private val clanLoungeRequest: ClanLoungeRequest,
    private val preferences: Preferences,
    private val useItemRequest: UseItemRequest,
    private val hermitRequest: HermitRequest,
    private val httpClient: HttpClient,
    private val familiarManager: FamiliarManager? = null,
    private val questDatabase: QuestDatabase? = null,
    private val outfitManager: OutfitManager? = null,
    private val inventoryManager: InventoryManager? = null,
    private val skillManager: SkillManager? = null,
) {
    companion object {
        const val VIP_LOUNGE_KEY_ID   = 5479
        const val MUS_MANUAL_ID       = 11
        const val MYS_MANUAL_ID       = 172
        const val MOX_MANUAL_ID       = 173
        const val GUNPOWDER_ID        = 2403
        const val PREF_HIPPY_MEAT_COLLECTED = "_hippyMeatCollected"

        internal fun sidequestOutfit(
            property: String,
            preferences: Preferences,
            hippyAvailable: Boolean,
            fratboyAvailable: Boolean,
        ): WarSideOutfit? =
            when (preferences.getString(property, "none")) {
                "hippy" -> if (hippyAvailable) WarSideOutfit.HIPPY else null
                "fratboy" -> if (fratboyAvailable) WarSideOutfit.FRATBOY else null
                else -> null
            }

        internal fun gunpowderCount(inventoryState: InventoryState): Int =
            inventoryState.items[GUNPOWDER_ID]?.quantity ?: 0
    }

    enum class WarSideOutfit(val outfitId: Int) {
        HIPPY(OutfitPool.WAR_HIPPY_OUTFIT),
        FRATBOY(OutfitPool.WAR_FRAT_OUTFIT),
    }

    open suspend fun runBreakfast(charState: CharacterState, inventoryState: InventoryState) {
        if (preferences.getBoolean(Preferences.BREAKFAST_COMPLETED, false)) return

        val suffix = if (charState.isHardcore) "Hardcore" else "Softcore"

        castSkills(charState)
        castBookSkills(charState)

        harvestGarden(suffix)
        checkRumpusRoom(suffix)
        checkVIPLounge(suffix, charState, inventoryState)
        readGuildManual(suffix, charState, inventoryState)
        getHermitClovers(inventoryState)
        collectHardwood()
        collect2002MrStoreCredits(inventoryState)
        collectAprilShowerGlobs(inventoryState)
        useSpinningWheel()
        visitBigIsland(charState, inventoryState)
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
        checkJackass(suffix, inventoryState)
        collectSeaJelly(suffix, charState)

        preferences.setBoolean(Preferences.BREAKFAST_COMPLETED, true)
    }

    /**
     * Desktop [BreakfastManager.castSkills] — breakfastAlways + breakfast{Soft,Hard}core.
     * Phase 553: no MP restore; clamp casts to available MP.
     */
    open suspend fun castSkills(charState: CharacterState, manaRemaining: Long = 0L) {
        val manager = skillManager ?: return
        val skillState = manager.state.value
        val alwaysSetting = preferences.getString("breakfastAlways", "")
        for (skillName in BreakfastBurnSkills.breakfastAlwaysSkills) {
            if (!BreakfastBurnSkills.prefContainsSkill(alwaysSetting, skillName)) continue
            castBreakfastSkill(skillName, Long.MAX_VALUE, allowRestore = false, manaRemaining, charState, skillState)
        }

        val suffix = if (charState.isHardcore) "Hardcore" else "Softcore"
        val skillSetting = preferences.getString("breakfast$suffix", "")
        if (skillSetting.isBlank()) return
        val pathedSummons = preferences.getBoolean("pathedSummons$suffix", false)
        val skills = skillState.skills

        for (skillName in BreakfastBurnSkills.breakfastSkills) {
            if (!BreakfastBurnSkills.prefContainsSkill(skillSetting, skillName)) continue
            if (BreakfastBurnSkills.findSkill(skillState, skillName) == null) continue
            if (pathedSummons && !passesPathedSummonGate(skillName, charState, skills)) continue
            if (!BreakfastBurnSkills.canCastBreakfastSkill(skillName, charState, skills)) continue
            castBreakfastSkill(skillName, Long.MAX_VALUE, allowRestore = false, manaRemaining, charState, skillState)
        }
    }

    /**
     * Desktop [BreakfastManager.castBookSkills] — tome / grimoire / libram pref lists.
     * Phase 554: no MP restore; tome/grimoire use remaining daily casts; libram uses remaining MP.
     */
    open suspend fun castBookSkills(charState: CharacterState, manaRemaining: Int = 0) {
        val manager = skillManager ?: return
        val skillState = manager.state.value
        val tome = BreakfastBurnSkills.getBreakfastBookSkills(
            preferences, "tomeSkills", BreakfastBurnSkills.tomeSkills, skillState, charState.isHardcore,
        )
        val grimoire = BreakfastBurnSkills.getBreakfastBookSkills(
            preferences, "grimoireSkills", BreakfastBurnSkills.grimoireSkills, skillState, charState.isHardcore,
        )
        val libram = BreakfastBurnSkills.getBreakfastLibramSkills(preferences, skillState, charState)

        castBookSkillList(tome, BookType.TOME, charState, manaRemaining.toLong())
        castBookSkillList(grimoire, BookType.GRIMOIRE, charState, manaRemaining.toLong())
        castBookSkillList(libram, BookType.LIBRAM, charState, manaRemaining.toLong())
    }

    private enum class BookType { TOME, GRIMOIRE, LIBRAM }

    private suspend fun castBookSkillList(
        castable: List<String>,
        type: BookType,
        charState: CharacterState,
        manaRemaining: Long,
    ) {
        if (castable.isEmpty()) return
        val skillState = skillManager?.state?.value ?: return
        val skillCount = castable.size
        val totalCasts = when (type) {
            BookType.TOME -> if (charState.isHardcore) 3L else skillCount * 3L
            BookType.GRIMOIRE -> skillCount.toLong()
            BookType.LIBRAM -> {
                var available = (charState.currentMp - manaRemaining).coerceAtLeast(0)
                var casts = 0L
                // Approximate libram casts from remaining MP using first skill cost when available.
                val sample = BreakfastBurnSkills.findSkill(skillState, castable.first())
                val cost = (sample?.mpCost ?: 1).coerceAtLeast(1)
                while (available >= cost) {
                    casts++
                    available -= cost
                }
                casts
            }
        }
        if (totalCasts <= 0) return
        if (skillCount == 1) {
            castBreakfastSkill(castable[0], totalCasts, allowRestore = false, manaRemaining, charState, skillState)
            return
        }
        val nextCast = totalCasts / skillCount
        var cast = nextCast + totalCasts - (nextCast * skillCount)
        for (skillName in castable) {
            castBreakfastSkill(skillName, cast, allowRestore = false, manaRemaining, charState, skillState)
            cast = nextCast
        }
    }

    private fun passesPathedSummonGate(
        skillName: String,
        charState: CharacterState,
        skills: List<net.sourceforge.kolmafia.skill.SkillData>,
    ): Boolean {
        when (skillName) {
            "Pastamastery", "Lunch Break", "Spaghetti Breakfast" ->
                if (!net.sourceforge.kolmafia.character.ConsumptionEligibility.canEat(charState, skills)) return false
            "Advanced Cocktailcrafting", "Grab a Cold One" ->
                if (!net.sourceforge.kolmafia.character.ConsumptionEligibility.canDrink(charState, skills) ||
                    charState.inKoLHS
                ) {
                    return false
                }
            "Summon Crimbo Candy" -> if (charState.inBeecore) return false
        }
        if (skillName == "Spaghetti Breakfast" && charState.inBeecore) return false
        return true
    }

    private suspend fun castBreakfastSkill(
        name: String,
        casts: Long,
        allowRestore: Boolean,
        manaRemaining: Long,
        charState: CharacterState,
        skillState: SkillState,
    ) {
        val manager = skillManager ?: return
        val skill = BreakfastBurnSkills.findSkill(skillState, name) ?: return
        var castCount = minOf(casts, BreakfastBurnSkills.maximumCastRemaining(skill))
        if (castCount <= 0) return
        if (!allowRestore) {
            val available = (charState.currentMp - manaRemaining).coerceAtLeast(0)
            val perCast = skill.mpCost.toLong()
            if (perCast > 0) {
                castCount = minOf(castCount, available / perCast)
            }
        }
        if (castCount <= 0) return
        manager.cast(skill, castCount.toInt().coerceAtLeast(1))
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
        preferences.setBoolean(Preferences.JACKASS_PLUMBER_USED, false)
        preferences.setBoolean(Preferences.SEA_JELLY_COLLECTED, false)
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
        val crop = preferences.getString(harvestPrefKey, "none").trim()
        if (crop.equals("none", ignoreCase = true)) return
        if (preferences.getBoolean(Preferences.GARDEN_HARVESTED, false)) return
        if (!crop.equals("any", ignoreCase = true) &&
            !GardenCropAvailability.hasCropOrBetter(preferences, crop)
        ) {
            return
        }
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

    private suspend fun checkVIPLounge(
        suffix: String,
        charState: CharacterState,
        inventoryState: InventoryState,
    ) {
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

        if (preferences.getBoolean(ClanLoungeSync.CLAN_HAS_HOT_DOG_STAND_PREF, false)) {
            ClanHotdogMenuCache.restoreIntoAvailability(preferences)
            clanLoungeRequest.visitHotDogStand(preferences, charState)
        }
        if (preferences.getBoolean(ClanLoungeSync.CLAN_HAS_SPEAKEASY_PREF, false)) {
            clanLoungeRequest.visitSpeakeasy(preferences, charState)
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
        val worthlessIds = listOf(
            BreakfastItemIds.WORTHLESS_TRINKET_ID,
            BreakfastItemIds.WORTHLESS_KNICK_KNACK_ID,
            BreakfastItemIds.WORTHLESS_GEWGAW_ID,
        )
        val count = worthlessIds.sumOf { inventoryState.items[it]?.quantity ?: 0 }.coerceAtMost(5)
        if (count == 0) return
        hermitRequest.trade(BreakfastItemIds.CLOVER_ITEM_ID, count).onSuccess {
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
    private suspend fun collectHardwood() {
        if (preferences.getBoolean(Preferences.HARDWOOD_COLLECTED, false)) return
        httpGet("shop.php?whichshop=lathe").onSuccess {
            preferences.setBoolean(Preferences.HARDWOOD_COLLECTED, true)
        }
    }

    private suspend fun collect2002MrStoreCredits(inventoryState: InventoryState) {
        if (preferences.getBoolean(Preferences.MR_STORE_CREDITS_COLLECTED, false)) return
        val catalogId = when {
            inventoryState.items.containsKey(BreakfastItemIds.MR_STORE_2002_CATALOG_ID) ->
                BreakfastItemIds.MR_STORE_2002_CATALOG_ID
            inventoryState.items.containsKey(BreakfastItemIds.REPLICA_MR_STORE_CATALOG_ID) ->
                BreakfastItemIds.REPLICA_MR_STORE_CATALOG_ID
            else -> return
        }
        useItemRequest.use(catalogId, 1).onSuccess {
            preferences.setBoolean(Preferences.MR_STORE_CREDITS_COLLECTED, true)
        }
    }

    private suspend fun visitBigIsland(
        charState: CharacterState,
        inventoryState: InventoryState,
    ) {
        if (preferences.getBoolean(Preferences.BIG_ISLAND_VISITED, false)) return

        if (LimitModeGates.limitZone("Island", charState.limitMode) ||
            LimitModeGates.limitZone("IsleWar", charState.limitMode)
        ) {
            preferences.setBoolean(Preferences.BIG_ISLAND_VISITED, true)
            return
        }

        if (preferences.getInt("lastFilthClearance", -1) == charState.ascensionNumber &&
            !charState.isFistcore
        ) {
            visitHippy(charState.ascensionNumber)
        }

        if (preferences.getString("warProgress", "unstarted") != "started") {
            preferences.setBoolean(Preferences.BIG_ISLAND_VISITED, true)
            return
        }

        val hippyAvailable = outfitManager?.hasOutfit(OutfitPool.WAR_HIPPY_OUTFIT) == true
        val fratboyAvailable = outfitManager?.hasOutfit(OutfitPool.WAR_FRAT_OUTFIT) == true

        var lighthouseOutfit =
            if (gunpowderCount(inventoryState) > 0) {
                sidequestOutfit(
                    "sidequestLighthouseCompleted",
                    preferences,
                    hippyAvailable,
                    fratboyAvailable,
                )
            } else {
                null
            }

        var farmOutfit =
            if (!preferences.getBoolean(IslandWarActionResponseSync.PREF_FARMER_ITEMS_COLLECTED, false)) {
                sidequestOutfit(
                    "sidequestFarmCompleted",
                    preferences,
                    hippyAvailable,
                    fratboyAvailable,
                )
            } else {
                null
            }

        if (lighthouseOutfit == null && farmOutfit == null) {
            preferences.setBoolean(Preferences.BIG_ISLAND_VISITED, true)
            return
        }

        val equipment = charState.equipment

        if (farmOutfit != null && isWearingWarOutfit(farmOutfit, equipment)) {
            visitFarmer(charState, inventoryState)
            farmOutfit = null
        }

        if (lighthouseOutfit != null && isWearingWarOutfit(lighthouseOutfit, equipment)) {
            visitPyro(charState, inventoryState)
            lighthouseOutfit = null
        }

        var current = nextOutfit(farmOutfit, lighthouseOutfit)
        if (current == null) {
            preferences.setBoolean(Preferences.BIG_ISLAND_VISITED, true)
            return
        }

        if (current == farmOutfit) {
            visitFarmer(charState, inventoryState)
            farmOutfit = null
        }

        if (current == lighthouseOutfit) {
            visitPyro(charState, inventoryState)
            lighthouseOutfit = null
        }

        current = nextOutfit(farmOutfit, lighthouseOutfit)
        if (current == null) {
            preferences.setBoolean(Preferences.BIG_ISLAND_VISITED, true)
            return
        }

        if (current == farmOutfit) {
            visitFarmer(charState, inventoryState)
        }

        if (current == lighthouseOutfit) {
            visitPyro(charState, inventoryState)
        }

        preferences.setBoolean(Preferences.BIG_ISLAND_VISITED, true)
    }

    private suspend fun visitHippy(ascensionNumber: Int) {
        if (preferences.getBoolean(PREF_HIPPY_MEAT_COLLECTED, false)) return
        val url = "shop.php?whichshop=hippy"
        httpGet(url).onSuccess { html ->
            NpcShopSync.syncFromStoreHtml("hippy", html, preferences, ascensionNumber, url)
        }
    }

    private suspend fun visitFarmer(
        charState: CharacterState,
        inventoryState: InventoryState,
    ) {
        val island = IslandWarPaths.currentIsland(preferences)
        if (island == "bogus.php") return
        val url = "$island?place=farm&action=farmer"
        httpGet(url).onSuccess { html ->
            applyIslandVisit(url, html, charState, inventoryState)
        }
    }

    private suspend fun visitPyro(
        charState: CharacterState,
        inventoryState: InventoryState,
    ) {
        val island = IslandWarPaths.currentIsland(preferences)
        if (island == "bogus.php") return
        val url = "$island?place=lighthouse&action=pyro"
        httpGet(url).onSuccess { html ->
            applyIslandVisit(url, html, charState, inventoryState)
        }
    }

    private fun applyIslandVisit(
        url: String,
        html: String,
        charState: CharacterState,
        inventoryState: InventoryState,
    ) {
        val context = islandVisitContext(charState, inventoryState)
        if (url.contains("postwarisland.php", ignoreCase = true)) {
            IslandWarVisitSync.applyFromPostwarIslandVisit(url, html, preferences, context)
        } else {
            IslandWarVisitSync.applyFromBigIslandVisit(url, html, preferences, context)
        }
        IslandWarActionResponseSync.parseActionResponse(url, html, preferences, context)
    }

    private fun islandVisitContext(
        charState: CharacterState,
        inventoryState: InventoryState,
    ): IslandWarVisitSync.IslandVisitContext {
        val liveInventory = inventoryManager?.state?.value ?: inventoryState
        return IslandWarVisitSync.IslandVisitContext(
            hasItemId = { id -> (liveInventory.items[id]?.quantity ?: 0) > 0 },
            consumeItem = { itemId, quantity ->
                inventoryManager?.consumeItemLocally(itemId, quantity)
            },
            isWearingWarHippyOutfit = {
                val outfit = OutfitDatabase.getById(OutfitPool.WAR_HIPPY_OUTFIT)
                    ?: return@IslandVisitContext false
                OutfitManager.isWearingPieces(outfit.equipment, charState.equipment)
            },
            ascensionNumber = charState.ascensionNumber,
            itemCount = { id -> liveInventory.items[id]?.quantity ?: 0 },
        )
    }

    private fun isWearingWarOutfit(
        side: WarSideOutfit,
        equipment: Map<net.sourceforge.kolmafia.character.EquipmentSlot, String>,
    ): Boolean {
        val outfit = OutfitDatabase.getById(side.outfitId) ?: return false
        return OutfitManager.isWearingPieces(outfit.equipment, equipment)
    }

    private suspend fun nextOutfit(
        one: WarSideOutfit?,
        two: WarSideOutfit?,
    ): WarSideOutfit? {
        val outfit = one ?: two ?: return null
        val data = OutfitDatabase.getById(outfit.outfitId) ?: return outfit
        outfitManager?.wearOutfit(data.name)
        return outfit
    }

    private suspend fun visitVolcanoIsland() {
        if (preferences.getBoolean(Preferences.VOLCANO_ISLAND_VISITED, false)) return
        httpGet("place.php?whichplace=island_camp").onSuccess {
            preferences.setBoolean(Preferences.VOLCANO_ISLAND_VISITED, true)
        }
    }

    private suspend fun visitServerRoom() {
        if (preferences.getBoolean(Preferences.SERVER_ROOM_VISITED, false)) return
        httpGet("place.php?whichplace=airport_spooky_bunker").onSuccess {
            preferences.setBoolean(Preferences.SERVER_ROOM_VISITED, true)
        }
    }

    private suspend fun checkJackass(suffix: String, inventoryState: InventoryState) {
        val prefKey = if (suffix == "Softcore") Preferences.CHECK_JACKASS_SOFTCORE
                      else Preferences.CHECK_JACKASS_HARDCORE
        if (!preferences.getBoolean(prefKey, true)) return
        if (preferences.getBoolean(Preferences.JACKASS_PLUMBER_USED, false)) return
        if (!inventoryState.items.containsKey(BreakfastItemIds.JACKASS_PLUMBER_GAME_ID)) return
        useItemRequest.use(BreakfastItemIds.JACKASS_PLUMBER_GAME_ID, 1).onSuccess {
            preferences.setBoolean(Preferences.JACKASS_PLUMBER_USED, true)
        }
    }

    private suspend fun collectSeaJelly(suffix: String, charState: CharacterState) {
        val prefKey = if (suffix == "Softcore") Preferences.COLLECT_SEA_JELLY_SOFTCORE
                      else Preferences.COLLECT_SEA_JELLY_HARDCORE
        if (!preferences.getBoolean(prefKey, true)) return
        if (preferences.getBoolean(Preferences.SEA_JELLY_COLLECTED, false)) return
        if (charState.adventuresLeft <= 0) return
        val db = questDatabase ?: return
        val seaStarted = db.progressFor(Quest.SEA_OLD_GUY.prefKey) != QuestDatabase.UNSTARTED
        if (!seaStarted) return
        val familiar = familiarManager ?: return
        val hasJellyfish = familiar.state.value.ownedFamiliars
            .any { it.id == BreakfastItemIds.SPACE_JELLYFISH_ID }
        if (!hasJellyfish) return
        familiar.setFamiliar("Space Jellyfish")
        httpGet("adventure.php?snarfblat=143").onSuccess { html ->
            if (html.contains("sea jelly", ignoreCase = true) ||
                html.contains("You acquire an item: sea jelly", ignoreCase = true)
            ) {
                preferences.setBoolean(Preferences.SEA_JELLY_COLLECTED, true)
            }
        }
    }
}
