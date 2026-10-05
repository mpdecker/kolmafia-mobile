package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.adventure.AdventureSession
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.campground.DwellingSync
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.equipment.OutfitCheckpoint
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.mood.ManaBurnManager
import net.sourceforge.kolmafia.mood.MoodManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.ProtonicGhostSync
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestItemUsedSync
import net.sourceforge.kolmafia.recovery.BetweenBattleInvoker
import net.sourceforge.kolmafia.session.ConsumptionHelperState
import net.sourceforge.kolmafia.session.DreadScrollManager
import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.SessionLogger
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState

open class UseItemRequest(
    private val client: HttpClient,
    private val preferences: Preferences? = null,
    private val sessionLogger: SessionLogger? = null,
    private val eventBus: GameEventBus? = null,
    private val questDatabase: QuestDatabase? = null,
    private val character: KoLCharacter? = null,
    private val inventoryManager: InventoryManager? = null,
    private val familiarManager: FamiliarManager? = null,
    private val effectManager: EffectManager? = null,
    private val manaBurnManager: ManaBurnManager? = null,
    private val skillManager: SkillManager? = null,
    private val moodManager: MoodManager? = null,
    private val gameDatabase: GameDatabase? = null,
    /**
     * Lazy provider avoids the UseItemRequest ↔ RetrieveItemService DI cycle.
     * Evaluated at use-time so [RetrieveItemService] can already hold this request.
     */
    private val retrieveItemServiceProvider: (() -> RetrieveItemService?)? = null,
    /** Lazy provider avoids UseItemRequest ↔ AdventureManager DI cycle. */
    private val adventureManagerProvider: (() -> net.sourceforge.kolmafia.adventure.AdventureManager?)? = null,
    private val zapRequestProvider: (() -> ZapRequest?)? = null,
) {
    /**
     * Uses an item via inv_use.php.
     * @param itemId  KoL item ID
     * @param quantity  number to use (default 1)
     */
    open suspend fun use(itemId: Int, quantity: Int = 1): Result<String> {
        if (RequestAbortGate.abortIfInFightOrChoice()) {
            return Result.failure(IllegalStateException(RequestAbortGate.lastAbortMessage.ifEmpty {
                "You are currently in a fight or choice."
            }))
        }
        val weaponId = character?.state?.value?.equipment[EquipmentSlot.WEAPON]
            ?.let { ItemDatabase.getByName(it)?.id } ?: -1
        val owned = inventoryManager?.getCount(itemId) ?: 0
        if (replacementRefused(itemId)) {
            return Result.success("")
        }
        val useQuantity = if (itemId == UseItemRunPreflight.VOLCANO_MAP) 1 else quantity
        when (val route = UseItemRunPreflight.route(itemId, EquipmentDatabase.isClub(weaponId), owned)) {
            is UseItemRunPreflight.Route.Refuse -> {
                UseItemRunPreflight.lastUpdate = route.message
                return Result.failure(IllegalStateException(route.message))
            }
            is UseItemRunPreflight.Route.BreakBricko -> return breakBricko(route.itemId)
            UseItemRunPreflight.Route.FoldSticker -> return foldStickers()
            UseItemRunPreflight.Route.ReadDiary -> return readDiary()
            UseItemRunPreflight.Route.PlayRandomDeck -> return playRandomDeck()
            UseItemRunPreflight.Route.ReadVolcanoMap,
            UseItemRunPreflight.Route.Proceed,
            -> Unit
        }
        val state = character?.state?.value
        val warning = UseItemRunPreflight.organWarning(
            itemId = itemId,
            itemName = ItemDatabase.getItemName(itemId),
            canEat = state?.let { UseItemRunPreflight.pathCanEat(it) } ?: true,
            canDrink = state?.let { UseItemRunPreflight.pathCanDrink(it) } ?: true,
            fullness = state?.fullness ?: 0,
            inebriety = state?.inebriety ?: 0,
            chocolateSculpturesUsed = preferences?.getInt("_chocolateSculpturesUsed", 0) ?: 0,
        )
        if (warning != null) {
            UseItemRunPreflight.lastUpdate = warning
            return Result.failure(IllegalStateException(warning))
        }
        when (val step = UseItemRunPreflight.consumption(
            itemId,
            state?.equipment.orEmpty(),
            state?.inKoLHS == true,
        )) {
            is UseItemRunPreflight.Consumption.Equip -> return equipFromUse(itemId, step.slot)
            UseItemRunPreflight.Consumption.Portal -> return chargePortal(itemId, useQuantity)
            UseItemRunPreflight.Consumption.Curse -> return castCurse(itemId, state?.playerId ?: 0)
            is UseItemRunPreflight.Consumption.Unusable -> {
                UseItemRunPreflight.lastUpdate = step.message
                return Result.failure(IllegalStateException(step.message))
            }
            null -> Unit
        }
        if (UseItemRunPreflight.resolvedUse(itemId) == ItemPrimaryUse.ZAP) {
            val zap = zapRequestProvider?.invoke()
                ?: ZapRequest(
                    client,
                    inventoryManager,
                    retrieveItemServiceProvider?.invoke(),
                    preferences,
                    character,
                    this,
                )
            return zap.zap(itemId).fold(
                onSuccess = { Result.success("") },
                onFailure = { Result.failure(it) },
            )
        }
        val itemName = ItemDatabase.getItemName(itemId)
        val activeEffects = effectManager?.state?.value?.effects
            ?.map { it.name }
            ?.toSet()
            .orEmpty()
        when (
            val plan = UseItemRunPreflight.planConsume(
                itemId = itemId,
                itemName = itemName,
                quantity = useQuantity,
                character = state ?: net.sourceforge.kolmafia.character.CharacterState(),
                preferences = preferences,
                accessibleCount = { id -> inventoryManager?.getCount(id) ?: 0 },
                activeEffectNames = activeEffects,
            )
        ) {
            is UseItemRunPreflight.ConsumePlan.Refuse -> {
                UseItemRunPreflight.lastUpdate = plan.message
                return Result.failure(IllegalStateException(plan.message))
            }
            is UseItemRunPreflight.ConsumePlan.Batches -> {
                if (plan.counts.isEmpty()) return Result.success("")
                plan.removeEffectId?.let { effectId ->
                    UneffectRequest(client, effectManager = effectManager)
                        .remove(effectId)
                        .getOrElse { return Result.failure(it) }
                }
                if (itemId == UseItemRunPreflight.JUMBO_DR_LUCIFER) {
                    burnLuciferMana(state)
                }
                val totalNeeded = plan.counts.sum()
                if (!retrieveForUse(itemId, totalNeeded)) {
                    UseItemRunPreflight.lastUpdate = UseItemRunPreflight.INSUFFICIENT_ITEMS
                    return Result.failure(
                        IllegalStateException(UseItemRunPreflight.INSUFFICIENT_ITEMS),
                    )
                }
                return if (itemId == UseItemRunPreflight.MAFIA_ARIA) {
                    withAriaCummerbund {
                        consumeBatches(itemId, itemName, plan.counts, weaponId)
                    }
                } else {
                    consumeBatches(itemId, itemName, plan.counts, weaponId)
                }
            }
        }
    }

    private suspend fun burnLuciferMana(state: net.sourceforge.kolmafia.character.CharacterState?) {
        val burner = manaBurnManager ?: return
        val liveState = state ?: character?.state?.value ?: return
        val minimum = UseItemRunPreflight.luciferMinimumMp(liveState.maxMp, liveState.currentHp)
        val live = { character?.state?.value ?: liveState }
        burner.burnMana(
            minimumMp = minimum,
            mood = moodManager?.activeMood,
            effectState = effectManager?.state?.value ?: EffectState(),
            skillState = skillManager?.state?.value ?: SkillState(),
            charState = live(),
            moodLibrary = moodManager?.moodLibrary ?: emptyMap(),
            currentCharState = live,
        )
    }

    /**
     * Desktop [InventoryManager.retrieveItem] before useOnce; skips USE_INFINITE/reusable.
     * When no retrieve service is wired (unit tests), acquisition is assumed already satisfied.
     */
    private suspend fun retrieveForUse(itemId: Int, quantity: Int): Boolean {
        if (quantity <= 0) return true
        if (!UseItemRunPreflight.shouldRetrieveBeforeUse(itemId)) return true
        val retrieve = retrieveItemServiceProvider?.invoke() ?: return true
        return retrieve.retrieve(itemId, quantity) >= quantity
    }

    private suspend fun <T> withAriaCummerbund(block: suspend () -> T): T {
        val char = character
        val db = gameDatabase
        if (char == null || db == null) {
            ensureCummerbundEquipped()
            return block()
        }
        val equipment = EquipmentRequest(client, character = char)
        val checkpoint = OutfitCheckpoint.snapshot(char, equipment, db)
        return try {
            ensureCummerbundEquipped()
            block()
        } finally {
            checkpoint.restore()
        }
    }

    private suspend fun ensureCummerbundEquipped() {
        val equipment = character?.state?.value?.equipment.orEmpty()
        val name = ItemDatabase.getItemName(UseItemRunPreflight.SUPPORT_CUMMERBUND)
        if (UseItemRunPreflight.hasEquipped(equipment, name)) return
        EquipmentRequest(client, character = character)
            .equipItem(UseItemRunPreflight.SUPPORT_CUMMERBUND, EquipmentSlot.ACC1)
    }

    private suspend fun consumeBatches(
        itemId: Int,
        itemName: String,
        counts: List<Int>,
        weaponId: Int,
    ): Result<String> {
        val total = counts.sum()
        val answerPlz = UseItemRunPreflight.needsAnswerPlz(itemId)
        val checkedFollowUp = UseItemRunPreflight.needsCheckedFollowUp(itemId)
        val confirm = UseItemRunPreflight.needsConfirmFormField(
            itemId = itemId,
            currentBedId = CampgroundItemSync.currentBedItemId,
            currentDwellingId = DwellingSync.currentDwellingItemId(preferences),
        )
        return try {
            var lastBody = ""
            for (batch in counts) {
                val turns = UseItemAdventuresUsed.forItem(
                    itemId = itemId,
                    count = batch,
                    preferences = preferences,
                    ownsItem = { id -> (inventoryManager?.getCount(id) ?: 0) > 0 },
                    equippedWeaponId = weaponId,
                )
                if (turns > 0) {
                    AdventureSession.setNextAdventure("None", preferences)
                    BetweenBattleInvoker.run(true)
                }
                val url = buildString {
                    append("inv_use.php?which=3&whichitem=$itemId&ajax=1")
                    if (batch > 1) append("&quantity=$batch")
                    if (answerPlz) append("&answerplz=1")
                    if (confirm) append("&confirm=true")
                }
                UseItemRequestState.remember(url, batch, preferences, inventoryManager)
                val response = client.get("$KOL_BASE_URL/inv_use.php") {
                    parameter("which", 3)
                    parameter("whichitem", itemId)
                    parameter("ajax", 1)
                    if (batch > 1) parameter("quantity", batch)
                    if (answerPlz) parameter("answerplz", 1)
                    if (confirm) parameter("confirm", "true")
                }
                if (!response.status.isSuccess()) {
                    return Result.failure(Exception("HTTP ${response.status.value}"))
                }
                val body = response.bodyAsText()
                lastBody = body
                val redirect = UseItemRedirect.classify(body, response.request.url.toString())
                if (redirect is UseItemRedirect.Kind.Fight || redirect is UseItemRedirect.Kind.Choice) {
                    CheckItemRedirection.apply(itemId, preferences, inventoryManager, batch)
                    adventureManagerProvider?.invoke()?.followItemUseRedirect(body)
                    UseItemRequestState.refreshFollowUps(client, preferences)
                    BetweenBattleInvoker.run(true)
                    continue
                }
                UseItemConsumptionSync.rememberLastItem(itemId, batch)
                UseItemGiftPackageSync.parse(body, itemId, sessionLogger)
                if (itemId == DreadScrollManager.KNUCKLEBONE_ID) {
                    DreadScrollManager.handleKnucklebone(body, preferences, sessionLogger)
                } else if (itemId == DreadScrollManager.DREADSCROLL_ID) {
                    DreadScrollManager.parseDreadscrollUse(body, preferences, eventBus, sessionLogger)
                } else if (itemId == ProtonicGhostSync.WALKIE_TALKIE) {
                    ProtonicGhostSync.applyFromWalkieTalkie(
                        html = body,
                        questDatabase = questDatabase,
                        preferences = preferences,
                        turnsPlayed = character?.state?.value?.turnsPlayed ?: 0,
                    )
                } else {
                    val questHandled = QuestItemUsedSync.apply(
                        itemId,
                        body,
                        questDatabase,
                        preferences,
                        consumeItem = { id, qty -> inventoryManager?.consumeItemLocally(id, qty) },
                        count = batch,
                    )
                    UseItemConsumptionSync.parseConsumption(
                        responseText = body,
                        itemId = itemId,
                        count = batch,
                        preferences = preferences,
                        character = character,
                        inventory = if (questHandled) null else inventoryManager,
                        familiarManager = familiarManager,
                    )
                }
                UseItemRequestState.refreshFollowUps(client, preferences)
                BetweenBattleInvoker.run(true)
                if (checkedFollowUp) {
                    val confirmFollow = client.get("$KOL_BASE_URL/inv_use.php") {
                        parameter("which", 3)
                        parameter("whichitem", itemId)
                        parameter("ajax", 1)
                        parameter("checked", 1)
                    }
                    if (!confirmFollow.status.isSuccess()) {
                        return Result.failure(Exception("HTTP ${confirmFollow.status.value}"))
                    }
                    lastBody = confirmFollow.bodyAsText()
                }
            }
            if (itemId == UseItemRunPreflight.VOLCANO_MAP) {
                UseItemRunPreflight.lastUpdate = UseItemRunPreflight.VOLCANO_READ
            } else {
                UseItemRunPreflight.lastUpdate = "Finished using $total $itemName."
            }
            Result.success(lastBody)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Desktop MultiUseRequest — multi-use an ingredient stack via multiuse.php. */
    open suspend fun multiUse(itemId: Int, quantity: Int): Result<String> {
        if (quantity <= 0) return Result.success("")
        return try {
            UseItemRequestState.remember(
                "multiuse.php?action=useitem&whichitem=$itemId&quantity=$quantity",
                quantity,
                preferences,
                inventoryManager,
            )
            val response = client.submitForm(
                url = "$KOL_BASE_URL/multiuse.php",
                formParameters = parameters {
                    append("action", "useitem")
                    append("whichitem", itemId.toString())
                    append("quantity", quantity.toString())
                },
            )
            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                UseItemConsumptionSync.rememberLastItem(itemId, quantity)
                QuestItemUsedSync.apply(
                    itemId,
                    body,
                    questDatabase,
                    preferences,
                    consumeItem = { id, qty -> inventoryManager?.consumeItemLocally(id, qty) },
                    count = quantity,
                )
                UseItemConsumptionSync.parseConsumption(
                    responseText = body,
                    itemId = itemId,
                    count = quantity,
                    preferences = preferences,
                    character = character,
                    inventory = inventoryManager,
                    familiarManager = familiarManager,
                )
                UseItemRequestState.refreshFollowUps(client, preferences)
                BetweenBattleInvoker.run(true)
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Desktop UseItemRequest GLUTTONOUS_GHOST / SPIRIT_HOBO / SLIMELING binge via familiarbinger.php. */
    open suspend fun binge(itemId: Int, quantity: Int): Result<String> {
        mementoRefusal(itemId)?.let { return it }
        return try {
            RequestLogger.registerRequest(
                "familiarbinger.php?whichitem=$itemId&action=binge&qty=$quantity",
                sessionLogger,
                preferences,
            )
            val response = client.get("$KOL_BASE_URL/familiarbinger.php") {
                parameter("whichitem", itemId)
                parameter("action", "binge")
                parameter("qty", quantity)
            }
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("HTTP ${response.status.value}"))
            }
            val body = response.bodyAsText()
            // Desktop useOnce: SPIRIT_HOBO clears food helper; GLUTTONOUS_GHOST clears booze helper.
            when (
                familiarManager?.state?.value?.activeFamiliar?.id
                    ?: character?.state?.value?.familiarId
                    ?: 0
            ) {
                UseItemBingeLog.HOBO -> ConsumptionHelperState.clearFoodHelper()
                UseItemBingeLog.GHOST -> ConsumptionHelperState.clearBoozeHelper()
            }
            val accepted = UseItemBingeSync.parse(
                url = "familiarbinger.php?whichitem=$itemId&action=binge&qty=$quantity",
                responseText = body,
                character = character,
                familiarManager = familiarManager,
                inventory = inventoryManager,
                preferences = preferences,
            )
            if (!accepted) {
                Result.failure(IllegalStateException("Your current familiar can't use that."))
            } else {
                Result.success(body)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Desktop UseItemRequest STOCKING_MIMIC candy feed via familiarbinger.php. */
    open suspend fun feedCandy(itemId: Int, quantity: Int): Result<String> {
        mementoRefusal(itemId)?.let { return it }
        return try {
            RequestLogger.registerRequest(
                "familiarbinger.php?whichitem=$itemId&action=candy&qty=$quantity",
                sessionLogger,
                preferences,
            )
            val response = client.get("$KOL_BASE_URL/familiarbinger.php") {
                parameter("whichitem", itemId)
                parameter("action", "candy")
                parameter("qty", quantity)
            }
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("HTTP ${response.status.value}"))
            }
            val body = response.bodyAsText()
            val accepted = UseItemBingeSync.parse(
                url = "familiarbinger.php?whichitem=$itemId&action=candy&qty=$quantity",
                responseText = body,
                character = character,
                familiarManager = familiarManager,
                inventory = inventoryManager,
                preferences = preferences,
            )
            if (!accepted) {
                Result.failure(IllegalStateException("Your current familiar can't use that."))
            } else {
                Result.success(body)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun replacementRefused(itemId: Int): Boolean {
        val bedding = UseItemRunPreflight.needsBeddingReplacementConfirm(
            itemId,
            CampgroundItemSync.currentBedItemId,
        )
        val dwelling = UseItemRunPreflight.needsDwellingReplacementConfirm(
            itemId,
            DwellingSync.currentDwellingItemId(preferences),
        )
        if ((bedding || dwelling) && !UseItemRunPreflight.confirmReplacement()) {
            return true
        }
        return false
    }

    private fun mementoRefusal(itemId: Int): Result<String>? {
        val message = UseItemRunPreflight.mementoRefusal(
            binge = true,
            preferences = preferences,
            itemName = ItemDatabase.getItemName(itemId),
        ) ?: return null
        UseItemRunPreflight.lastUpdate = message
        return Result.failure(IllegalStateException(message))
    }

    private suspend fun breakBricko(itemId: Int): Result<String> {
        UseItemRunPreflight.lastUpdate = UseItemRunPreflight.SPLITTING_BRICKS
        val url = "inventory.php?action=breakbricko&whichitem=$itemId"
        RequestLogger.registerRequest(url, sessionLogger, preferences)
        return try {
            val response = client.get("$KOL_BASE_URL/inventory.php") {
                parameter("action", "breakbricko")
                parameter("whichitem", itemId)
            }
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("HTTP ${response.status.value}"))
            }
            val body = response.bodyAsText()
            UseItemBrickoSync.parse(body, inventoryManager)
            Result.success(body)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun foldStickers(): Result<String> {
        val url = "bedazzle.php?action=fold"
        RequestLogger.registerRequest(url, sessionLogger, preferences)
        return try {
            val response = client.get("$KOL_BASE_URL/bedazzle.php") {
                parameter("action", "fold")
            }
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("HTTP ${response.status.value}"))
            }
            Result.success(response.bodyAsText())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun readDiary(): Result<String> {
        val url = "diary.php?textversion=1"
        RequestLogger.registerRequest(url, sessionLogger, preferences)
        return try {
            val response = client.get("$KOL_BASE_URL/diary.php") {
                parameter("textversion", 1)
            }
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("HTTP ${response.status.value}"))
            }
            val body = response.bodyAsText()
            UseItemDiarySync.handle(
                responseText = body,
                quests = questDatabase,
                preferences = preferences,
                ascensions = character?.state?.value?.ascensionNumber ?: 0,
            )
            UseItemRunPreflight.lastUpdate = UseItemRunPreflight.DIARY_READ
            Result.success(body)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun equipFromUse(itemId: Int, slot: EquipmentSlot): Result<String> =
        EquipmentRequest(client, character = character).equipItem(itemId, slot).map { "" }

    private suspend fun chargePortal(itemId: Int, quantity: Int): Result<String> {
        val action = when (itemId) {
            UseItemRunPreflight.OVERCHARGED_POWER_SPHERE -> "overpowerelvibratoportal"
            else -> "powerelvibratoportal"
        }
        UseItemRunPreflight.lastUpdate = "Charging your El Vibrato portal..."
        return try {
            var body = ""
            repeat(quantity.coerceAtLeast(1)) {
                val response = client.get("$KOL_BASE_URL/campground.php") {
                    parameter("action", action)
                }
                if (!response.status.isSuccess()) {
                    return Result.failure(Exception("HTTP ${response.status.value}"))
                }
                body = response.bodyAsText()
                PortalRequest.parseResponse(
                    url = "campground.php?action=$action",
                    html = body,
                    preferences = preferences,
                    consumeItem = { id, qty -> inventoryManager?.consumeItemLocally(id, qty) },
                )
            }
            Result.success(body)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun castCurse(itemId: Int, playerId: Int): Result<String> {
        val url = "curse.php?action=use&whichitem=$itemId&targetplayer=$playerId"
        RequestLogger.registerRequest(url, sessionLogger, preferences)
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/curse.php",
                formParameters = parameters {
                    append("action", "use")
                    append("whichitem", itemId.toString())
                    append("targetplayer", playerId.toString())
                },
            )
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("HTTP ${response.status.value}"))
            }
            Result.success(response.bodyAsText())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun playRandomDeck(): Result<String> =
        DeckOfEveryCardRequest(client, ChoiceRequest(client)).play(
            card = null,
            preferences = preferences,
            inventoryCounts = { id -> inventoryManager?.getCount(id) ?: 0 },
            inLegacyOfLoathing = character?.state?.value?.inLegacyOfLoathing == true,
        ).onFailure { error ->
            UseItemRunPreflight.lastUpdate = error.message.orEmpty()
        }

    /** Desktop UseItemRequest ROBORTENDER robooze via inventory.php (qty 1 per call). */
    open suspend fun robooze(itemId: Int): Result<String> {
        return try {
            val response = client.get("$KOL_BASE_URL/inventory.php") {
                parameter("action", "robooze")
                parameter("whichitem", itemId)
                parameter("ajax", 1)
            }
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("HTTP ${response.status.value}"))
            }
            val body = response.bodyAsText()
            val accepted = UseItemRobortenderSync.parse(
                url = "inventory.php?action=robooze&whichitem=$itemId",
                responseText = body,
                inventory = inventoryManager,
                preferences = preferences,
            )
            if (!accepted) {
                Result.failure(IllegalStateException("Your Robortender can't drink that."))
            } else {
                Result.success(body)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
