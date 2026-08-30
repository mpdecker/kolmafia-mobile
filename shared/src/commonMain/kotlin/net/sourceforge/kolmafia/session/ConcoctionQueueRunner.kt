package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.ConsumptionEligibility
import net.sourceforge.kolmafia.data.ConcoctionConsumptionType
import net.sourceforge.kolmafia.data.ConcoctionCraftQueue
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionOrganAmounts.QueueBucket
import net.sourceforge.kolmafia.data.ConcoctionPermitted
import net.sourceforge.kolmafia.data.ConcoctionQueueReservation
import net.sourceforge.kolmafia.data.ConsumableDatabase
import net.sourceforge.kolmafia.data.HotDogDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.SpeakeasyDatabase
import net.sourceforge.kolmafia.data.isAutoCraftable
import net.sourceforge.kolmafia.data.isCreateAndConsume
import net.sourceforge.kolmafia.data.isCreateSupported
import net.sourceforge.kolmafia.data.isStillsuitCraftable
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.modifiers.ExpressionContext
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CafeNotOnMenuException
import net.sourceforge.kolmafia.request.CafePurchaseRequest
import net.sourceforge.kolmafia.request.ConsumptionRequestOutcome
import net.sourceforge.kolmafia.request.ClanLoungeRequest
import net.sourceforge.kolmafia.request.ConcoctionCreateRequest
import net.sourceforge.kolmafia.request.DrinkBoozeRequest
import net.sourceforge.kolmafia.request.EatFoodRequest
import net.sourceforge.kolmafia.request.ChewRequest
import net.sourceforge.kolmafia.request.FloundryRequest
import net.sourceforge.kolmafia.request.ItemUseLimitsContext
import net.sourceforge.kolmafia.request.StillSuitRequest
import net.sourceforge.kolmafia.request.UseItemRequest
import net.sourceforge.kolmafia.request.maximumUses

private data class QueueProcessOutcome(
    val result: Result<Unit>,
    val requeueEligible: Boolean = false,
) {
    val isFailure: Boolean get() = result.isFailure
}

/**
 * Desktop [ConcoctionDatabase.handleQueue] drain — lounge v1 + inventory eat/drink v2 + craft-only v3 +
 * cafe v4 + stillsuit v5 + spleen v6 + potion use v7 + ghost/hobo binge v8 + floundry v9 + re-queue v10 +
 * slimeling/robortender/mimic familiar feed v11 + NONE create-only v12 + helper/partial re-queue v13.
 */
class ConcoctionQueueRunner(
    private val clanLoungeRequest: ClanLoungeRequest,
    private val eatFoodRequest: EatFoodRequest? = null,
    private val drinkBoozeRequest: DrinkBoozeRequest? = null,
    private val chewRequest: ChewRequest? = null,
    private val useItemRequest: UseItemRequest? = null,
    private val retrieveItemService: RetrieveItemService? = null,
    private val concoctionCreateRequest: ConcoctionCreateRequest? = null,
    private val cafePurchaseRequest: CafePurchaseRequest? = null,
    private val stillSuitRequest: StillSuitRequest? = null,
    private val floundryRequest: FloundryRequest? = null,
    private val familiarManager: FamiliarManager? = null,
) {

    suspend fun handleQueue(
        bucket: QueueBucket,
        type: ConcoctionConsumptionType,
        preferences: Preferences? = null,
        state: CharacterState? = null,
    ): Result<Unit> {
        if (!isSupportedConsumptionType(type)) {
            return Result.failure(IllegalArgumentException("Unsupported consumption type: $type"))
        }

        val toProcess = mutableListOf<ConcoctionQueueReservation>()
        while (true) {
            val item = ConcoctionCraftQueue.pop(bucket) ?: break
            toProcess.add(item)
        }

        ConcoctionDatabase.refreshConcoctionsNowFromLastContext()

        when (type) {
            ConcoctionConsumptionType.EAT -> ConsumptionHelperState.resetConsumedCounters()
            ConcoctionConsumptionType.DRINK -> ConsumptionHelperState.resetConsumedCounters()
            else -> Unit
        }

        var loungeConsumed = false
        val ordered = toProcess.asReversed()
        for ((index, reservation) in ordered.withIndex()) {
            val outcome = processReservation(
                reservation = reservation,
                type = type,
                preferences = preferences,
                state = state,
                onLoungeConsumed = { loungeConsumed = true },
            )
            if (outcome.isFailure) {
                if (outcome.requeueEligible && shouldRequeueOnFailure(preferences)) {
                    requeueOnFailure(reservation, ordered, index)
                    ConcoctionDatabase.refreshConcoctionsNowFromLastContext()
                    return Result.success(Unit)
                }
                return outcome.result
            }
        }

        if (loungeConsumed) {
            ConcoctionDatabase.refreshAfterLoungeMutation(preferences)
        }
        ConcoctionDatabase.refreshConcoctionsNowFromLastContext()
        return Result.success(Unit)
    }

    private suspend fun processReservation(
        reservation: ConcoctionQueueReservation,
        type: ConcoctionConsumptionType,
        preferences: Preferences?,
        state: CharacterState?,
        onLoungeConsumed: () -> Unit,
    ): QueueProcessOutcome {
        val name = reservation.resultName
        val quantity = reservation.quantity
        when (type) {
            ConcoctionConsumptionType.EAT -> {
                if (HotDogDatabase.isHotDog(name)) {
                    onLoungeConsumed()
                    repeat(quantity) {
                        clanLoungeRequest.eatHotDog(name, preferences, state)
                            .onFailure { return QueueProcessOutcome(Result.failure(it), requeueEligible = true) }
                    }
                    return QueueProcessOutcome(Result.success(Unit))
                }
                return consumeQueuedItem(name, quantity, type, state, preferences)
            }
            ConcoctionConsumptionType.DRINK -> {
                if (SpeakeasyDatabase.isSpeakeasyDrink(name)) {
                    onLoungeConsumed()
                    repeat(quantity) {
                        clanLoungeRequest.drinkSpeakeasy(name, preferences, state)
                            .onFailure { return QueueProcessOutcome(Result.failure(it), requeueEligible = true) }
                    }
                    return QueueProcessOutcome(Result.success(Unit))
                }
                return consumeQueuedItem(name, quantity, type, state, preferences)
            }
            ConcoctionConsumptionType.SPLEEN,
            ConcoctionConsumptionType.USE,
            -> return consumeQueuedItem(name, quantity, type, state, preferences)
            ConcoctionConsumptionType.GLUTTONOUS_GHOST,
            ConcoctionConsumptionType.SPIRIT_HOBO,
            ConcoctionConsumptionType.SLIMELING,
            ConcoctionConsumptionType.ROBORTENDER,
            ConcoctionConsumptionType.STOCKING_MIMIC,
            -> return consumeFamiliarFeedQueuedItem(name, quantity, type, state, preferences)
            ConcoctionConsumptionType.NONE ->
                return consumeQueuedItem(name, quantity, type, state, preferences)
        }
        return QueueProcessOutcome(Result.success(Unit))
    }

    private suspend fun consumeQueuedItem(
        name: String,
        quantity: Int,
        type: ConcoctionConsumptionType,
        state: CharacterState?,
        preferences: Preferences?,
    ): QueueProcessOutcome {
        val concoction = ConcoctionDatabase.getByResult(name)
        if (concoction?.methods?.contains("FLOUNDRY") == true) {
            return consumeFloundryItem(name, quantity, state, preferences)
        }
        val itemId = ItemDatabase.getByName(name)?.id ?: 0
        return if (itemId > 0) {
            consumeInventoryItem(name, itemId, quantity, type, state, preferences)
        } else {
            consumeCraftOnlyItem(name, quantity, type, state, preferences)
        }
    }

    private suspend fun consumeFamiliarFeedQueuedItem(
        name: String,
        quantity: Int,
        type: ConcoctionConsumptionType,
        state: CharacterState?,
        preferences: Preferences?,
    ): QueueProcessOutcome {
        val itemId = ItemDatabase.getByName(name)?.id ?: 0
        if (itemId <= 0) {
            return QueueProcessOutcome(Result.success(Unit))
        }
        if (!isFamiliarFeedEligible(name, type)) {
            return QueueProcessOutcome(Result.success(Unit))
        }
        preflightBinge(type).onFailure { return QueueProcessOutcome(Result.failure(it)) }
        preflightInventoryConsume(name, type, state, preferences)
            .onFailure { return QueueProcessOutcome(Result.failure(it)) }

        val retrieve = retrieveItemService
            ?: return QueueProcessOutcome(
                Result.failure(IllegalStateException("RetrieveItemService not configured")),
            )
        val retrieved = retrieve.retrieve(itemId, quantity)
        if (retrieved < quantity) {
            return QueueProcessOutcome(
                Result.failure(IllegalStateException("Could not retrieve $quantity of $name (got $retrieved)")),
            )
        }

        val use = useItemRequest
            ?: return QueueProcessOutcome(
                Result.failure(IllegalStateException("UseItemRequest not configured")),
            )
        return when (type) {
            ConcoctionConsumptionType.STOCKING_MIMIC ->
                use.feedCandy(itemId, quantity).fold(
                    onSuccess = { QueueProcessOutcome(Result.success(Unit)) },
                    onFailure = { QueueProcessOutcome(Result.failure(it), requeueEligible = true) },
                )
            ConcoctionConsumptionType.ROBORTENDER -> {
                repeat(quantity) {
                    use.robooze(itemId).onFailure {
                        return QueueProcessOutcome(Result.failure(it), requeueEligible = true)
                    }
                }
                QueueProcessOutcome(Result.success(Unit))
            }
            ConcoctionConsumptionType.GLUTTONOUS_GHOST,
            ConcoctionConsumptionType.SPIRIT_HOBO,
            ConcoctionConsumptionType.SLIMELING,
            -> use.binge(itemId, quantity).fold(
                onSuccess = { QueueProcessOutcome(Result.success(Unit)) },
                onFailure = { QueueProcessOutcome(Result.failure(it), requeueEligible = true) },
            )
            else -> QueueProcessOutcome(Result.success(Unit))
        }
    }

    private suspend fun consumeInventoryItem(
        name: String,
        itemId: Int,
        quantity: Int,
        type: ConcoctionConsumptionType,
        state: CharacterState?,
        preferences: Preferences?,
    ): QueueProcessOutcome {
        if (type != ConcoctionConsumptionType.NONE) {
            preflightForType(name, itemId, quantity, type, state, preferences)
                .onFailure { return QueueProcessOutcome(Result.failure(it)) }
        }

        val retrieve = retrieveItemService
            ?: return QueueProcessOutcome(
                Result.failure(IllegalStateException("RetrieveItemService not configured")),
            )
        val retrieved = retrieve.retrieve(itemId, quantity)
        if (retrieved < quantity) {
            return QueueProcessOutcome(
                Result.failure(IllegalStateException("Could not retrieve $quantity of $name (got $retrieved)")),
            )
        }

        val primaryUse = ItemDatabase.getByName(name)?.primaryUse
        if (type == ConcoctionConsumptionType.EAT && primaryUse == ItemPrimaryUse.FOOD_HELPER) {
            eatFoodRequest?.queueFoodHelper(itemId, quantity)
                ?: return QueueProcessOutcome(
                    Result.failure(IllegalStateException("EatFoodRequest not configured")),
                )
            return QueueProcessOutcome(Result.success(Unit))
        }
        if (type == ConcoctionConsumptionType.DRINK && primaryUse == ItemPrimaryUse.DRINK_HELPER) {
            drinkBoozeRequest?.queueDrinkHelper(itemId, quantity)
                ?: return QueueProcessOutcome(
                    Result.failure(IllegalStateException("DrinkBoozeRequest not configured")),
                )
            return QueueProcessOutcome(Result.success(Unit))
        }

        return consumeWithEatDrinkChewOrUse(itemId, quantity, type).toConsumptionOutcome()
    }

    private suspend fun consumeCraftOnlyItem(
        name: String,
        quantity: Int,
        type: ConcoctionConsumptionType,
        state: CharacterState?,
        preferences: Preferences?,
    ): QueueProcessOutcome {
        val concoction = ConcoctionDatabase.getByResult(name) ?: return QueueProcessOutcome(Result.success(Unit))
        if (concoction.methods.contains("FLOUNDRY")) {
            return consumeFloundryItem(name, quantity, state, preferences)
        }
        // Distillate is create+consume in one StillSuitRequest flow — before generic create.
        if (StillSuitRequest.isDistillate(name) || concoction.isStillsuitCraftable()) {
            return consumeStillSuitItem(name, quantity, type, state, preferences)
        }
        if (concoction.isCreateSupported()) {
            val itemId = ItemDatabase.getByName(name)?.id ?: 0
            if (type != ConcoctionConsumptionType.NONE) {
                preflightForType(name, itemId, quantity, type, state, preferences)
                    .onFailure { return QueueProcessOutcome(Result.failure(it)) }
            }

            val create = concoctionCreateRequest
                ?: return QueueProcessOutcome(
                    Result.failure(IllegalStateException("ConcoctionCreateRequest not configured")),
                )
            create.create(name, quantity, state, preferences)
                .onFailure { return QueueProcessOutcome(Result.failure(it)) }

            val outputId = ItemDatabase.getByName(name)?.id ?: 0
            return if (outputId > 0 && type != ConcoctionConsumptionType.NONE && !concoction.isCreateAndConsume()) {
                consumeWithEatDrinkChewOrUse(outputId, quantity, type).toConsumptionOutcome()
            } else {
                QueueProcessOutcome(Result.success(Unit))
            }
        }
        return consumeCafeItem(name, quantity, type, state, preferences)
    }

    private suspend fun consumeFloundryItem(
        name: String,
        quantity: Int,
        state: CharacterState?,
        preferences: Preferences?,
    ): QueueProcessOutcome {
        val request = floundryRequest
            ?: return QueueProcessOutcome(
                Result.failure(IllegalStateException("FloundryRequest not configured")),
            )
        repeat(quantity) {
            request.purchase(name, state, preferences).onFailure {
                return QueueProcessOutcome(Result.failure(it))
            }
        }
        return QueueProcessOutcome(Result.success(Unit))
    }

    private fun preflightForType(
        name: String,
        itemId: Int,
        quantity: Int,
        type: ConcoctionConsumptionType,
        state: CharacterState?,
        preferences: Preferences?,
    ): Result<Unit> = when (type) {
        ConcoctionConsumptionType.USE ->
            preflightPotionConsume(name, itemId, quantity, state, preferences)
        else ->
            preflightInventoryConsume(name, type, state, preferences)
    }

    private suspend fun consumeStillSuitItem(
        name: String,
        quantity: Int,
        type: ConcoctionConsumptionType,
        state: CharacterState?,
        preferences: Preferences?,
    ): QueueProcessOutcome {
        val request = stillSuitRequest
            ?: return QueueProcessOutcome(
                Result.failure(IllegalStateException("StillSuitRequest not configured")),
            )
        repeat(quantity) {
            request.distill(name, type, state, preferences)
                .onFailure { return QueueProcessOutcome(Result.failure(it)) }
            ConcoctionDatabase.refreshConcoctionsNowFromLastContext()
        }
        return QueueProcessOutcome(Result.success(Unit))
    }

    private suspend fun consumeCafeItem(
        name: String,
        quantity: Int,
        type: ConcoctionConsumptionType,
        state: CharacterState?,
        preferences: Preferences?,
    ): QueueProcessOutcome {
        val purchase = cafePurchaseRequest
            ?: return QueueProcessOutcome(Result.success(Unit))
        repeat(quantity) {
            val outcome = purchase.purchase(name, type, state, preferences)
            if (outcome.isFailure) {
                val error = outcome.exceptionOrNull()
                if (error is CafeNotOnMenuException) {
                    return QueueProcessOutcome(Result.success(Unit))
                }
                return QueueProcessOutcome(Result.failure(error ?: IllegalStateException("Cafe purchase failed")))
            }
        }
        return QueueProcessOutcome(Result.success(Unit))
    }

    private fun requeueOnFailure(
        reservation: ConcoctionQueueReservation,
        ordered: List<ConcoctionQueueReservation>,
        index: Int,
    ) {
        val bucket = reservation.queueBucket
        val helper = ConsumptionHelperState.captureAndClearHelper(bucket)
        helper?.let { (helperId, helperCount) ->
            val helperName = ItemDatabase.getById(helperId)?.name
            if (helperName != null) {
                ConcoctionCraftQueue.restore(
                    helperReservation(helperName, helperCount, reservation),
                )
            }
        }
        val partial = ConsumptionHelperState.lastUnconsumed(reservation.quantity, bucket)
        if (partial > 0) {
            ConcoctionCraftQueue.restore(reservation, quantity = partial)
        }
        for (remaining in ordered.drop(index + 1)) {
            ConcoctionCraftQueue.restore(remaining)
        }
    }

    private fun helperReservation(
        name: String,
        quantity: Int,
        template: ConcoctionQueueReservation,
    ): ConcoctionQueueReservation = ConcoctionQueueReservation(
        resultName = name,
        quantity = quantity,
        queueBucket = template.queueBucket,
        preferences = template.preferences,
    )

    private suspend fun consumeWithEatDrinkChewOrUse(
        itemId: Int,
        quantity: Int,
        type: ConcoctionConsumptionType,
    ): Result<Unit> {
        return when (type) {
            ConcoctionConsumptionType.EAT -> {
                val eat = eatFoodRequest
                    ?: return Result.failure(IllegalStateException("EatFoodRequest not configured"))
                eat.consumeFood(itemId, quantity).fold(
                    onSuccess = { outcome -> outcome.toUnitResult(quantity) },
                    onFailure = { Result.failure(it) },
                )
            }
            ConcoctionConsumptionType.DRINK -> {
                val drink = drinkBoozeRequest
                    ?: return Result.failure(IllegalStateException("DrinkBoozeRequest not configured"))
                drink.consumeDrink(itemId, quantity).fold(
                    onSuccess = { outcome -> outcome.toUnitResult(quantity) },
                    onFailure = { Result.failure(it) },
                )
            }
            ConcoctionConsumptionType.SPLEEN -> {
                val chew = chewRequest
                    ?: return Result.failure(IllegalStateException("ChewRequest not configured"))
                chew.chew(itemId, quantity).map { }
            }
            ConcoctionConsumptionType.USE -> {
                val use = useItemRequest
                    ?: return Result.failure(IllegalStateException("UseItemRequest not configured"))
                use.use(itemId, quantity).map { }
            }
            ConcoctionConsumptionType.NONE,
            ConcoctionConsumptionType.GLUTTONOUS_GHOST,
            ConcoctionConsumptionType.SPIRIT_HOBO,
            ConcoctionConsumptionType.SLIMELING,
            ConcoctionConsumptionType.ROBORTENDER,
            ConcoctionConsumptionType.STOCKING_MIMIC,
            -> Result.success(Unit)
        }
    }

    private fun Result<Unit>.toConsumptionOutcome(): QueueProcessOutcome =
        QueueProcessOutcome(this, requeueEligible = isFailure)

    private fun ConsumptionRequestOutcome.toUnitResult(quantity: Int): Result<Unit> = when (this) {
        is ConsumptionRequestOutcome.Aborted ->
            Result.failure(IllegalStateException(reason))
        is ConsumptionRequestOutcome.Completed ->
            if (consumed < quantity) {
                Result.failure(IllegalStateException("Partial consumption: $consumed of $quantity"))
            } else {
                Result.success(Unit)
            }
    }

    companion object {
        private const val GLUTTONOUS_GHOST_ID = 74
        private const val SPIRIT_HOBO_ID = 52
        private const val SLIMELING_ID = 112
        private const val STOCKING_MIMIC_ID = 120
        private const val ROBORTENDER_ID = 211

        internal fun isSupportedConsumptionType(type: ConcoctionConsumptionType): Boolean =
            type == ConcoctionConsumptionType.NONE ||
                type == ConcoctionConsumptionType.EAT ||
                type == ConcoctionConsumptionType.DRINK ||
                type == ConcoctionConsumptionType.SPLEEN ||
                type == ConcoctionConsumptionType.USE ||
                type == ConcoctionConsumptionType.GLUTTONOUS_GHOST ||
                type == ConcoctionConsumptionType.SPIRIT_HOBO ||
                type == ConcoctionConsumptionType.SLIMELING ||
                type == ConcoctionConsumptionType.ROBORTENDER ||
                type == ConcoctionConsumptionType.STOCKING_MIMIC

        internal fun shouldRequeueOnFailure(preferences: Preferences?): Boolean =
            preferences?.getBoolean("addCreationQueue", true) == true

        internal fun isFamiliarFeedEligible(name: String, type: ConcoctionConsumptionType): Boolean {
            val item = ItemDatabase.getByName(name) ?: return false
            when (item.primaryUse) {
                ItemPrimaryUse.FOOD_HELPER, ItemPrimaryUse.DRINK_HELPER -> return false
                else -> Unit
            }
            return when (type) {
                ConcoctionConsumptionType.GLUTTONOUS_GHOST ->
                    item.primaryUse == ItemPrimaryUse.FOOD
                ConcoctionConsumptionType.SPIRIT_HOBO,
                ConcoctionConsumptionType.ROBORTENDER,
                -> item.primaryUse == ItemPrimaryUse.DRINK
                ConcoctionConsumptionType.SLIMELING -> true
                ConcoctionConsumptionType.STOCKING_MIMIC ->
                    ItemDatabase.isCandyItem(item.id)
                else -> false
            }
        }

        /** @deprecated use [isFamiliarFeedEligible] */
        internal fun isBingeEligible(name: String, type: ConcoctionConsumptionType): Boolean =
            isFamiliarFeedEligible(name, type)

        internal fun preflightBingeWithFamiliar(
            type: ConcoctionConsumptionType,
            activeFamiliarId: Int?,
        ): Result<Unit> {
            val requiredId = when (type) {
                ConcoctionConsumptionType.GLUTTONOUS_GHOST -> GLUTTONOUS_GHOST_ID
                ConcoctionConsumptionType.SPIRIT_HOBO -> SPIRIT_HOBO_ID
                ConcoctionConsumptionType.SLIMELING -> SLIMELING_ID
                ConcoctionConsumptionType.STOCKING_MIMIC -> STOCKING_MIMIC_ID
                ConcoctionConsumptionType.ROBORTENDER -> ROBORTENDER_ID
                else -> return Result.success(Unit)
            }
            if (activeFamiliarId != requiredId) {
                val label = when (type) {
                    ConcoctionConsumptionType.GLUTTONOUS_GHOST ->
                        "You don't have a Gluttonous Green Ghost equipped"
                    ConcoctionConsumptionType.SPIRIT_HOBO ->
                        "You don't have a Spirit Hobo equipped"
                    ConcoctionConsumptionType.SLIMELING ->
                        "You don't have a Slimeling equipped"
                    ConcoctionConsumptionType.STOCKING_MIMIC ->
                        "You don't have a Stocking Mimic equipped"
                    ConcoctionConsumptionType.ROBORTENDER ->
                        "You don't have a Robortender equipped"
                    else -> "Wrong familiar equipped"
                }
                return Result.failure(IllegalStateException(label))
            }
            return Result.success(Unit)
        }

        internal fun preflightInventoryConsume(
            name: String,
            type: ConcoctionConsumptionType,
            state: CharacterState?,
            prefs: Preferences?,
        ): Result<Unit> {
            val concoction = ConcoctionDatabase.getByResult(name)
                ?: return Result.failure(IllegalStateException("No concoction for: $name"))
            if (state != null) {
                if (!ConcoctionPermitted.isPermittedMethod(
                        concoction,
                        state,
                        prefs = prefs,
                        limitMode = state.limitMode,
                    )
                ) {
                    return Result.failure(IllegalStateException("Consumption not permitted: $name"))
                }
                when (type) {
                    ConcoctionConsumptionType.EAT,
                    ConcoctionConsumptionType.GLUTTONOUS_GHOST,
                    -> {
                        if (!ConsumptionEligibility.canEat(state)) {
                            return Result.failure(IllegalStateException("Cannot eat: $name"))
                        }
                        val fullness = ConsumableDatabase.getFullnessByName(name)
                        if (fullness > 0 &&
                            ConsumptionEligibility.effectiveFullnessRemaining(state) < fullness
                        ) {
                            return Result.failure(IllegalStateException("Not enough fullness for: $name"))
                        }
                    }
                    ConcoctionConsumptionType.DRINK,
                    ConcoctionConsumptionType.SPIRIT_HOBO,
                    ConcoctionConsumptionType.ROBORTENDER,
                    -> {
                        if (!ConsumptionEligibility.canDrink(state)) {
                            return Result.failure(IllegalStateException("Cannot drink: $name"))
                        }
                        val inebriety = ConsumableDatabase.getInebrietyByName(name)
                        if (inebriety > 0 &&
                            ConsumptionEligibility.effectiveInebrietyRemaining(state) < inebriety
                        ) {
                            return Result.failure(IllegalStateException("Not enough liver for: $name"))
                        }
                    }
                    ConcoctionConsumptionType.SPLEEN -> {
                        if (!ConsumptionEligibility.canChew(state)) {
                            return Result.failure(IllegalStateException("Cannot chew: $name"))
                        }
                        val spleenHit = ConsumableDatabase.getSpleenByName(name)
                        if (spleenHit > 0 &&
                            ConsumptionEligibility.effectiveSpleenRemaining(state) < spleenHit
                        ) {
                            return Result.failure(IllegalStateException("Not enough spleen for: $name"))
                        }
                    }
                    ConcoctionConsumptionType.USE,
                    ConcoctionConsumptionType.NONE,
                    ConcoctionConsumptionType.SLIMELING,
                    ConcoctionConsumptionType.STOCKING_MIMIC,
                    -> Unit
                }
            }
            return Result.success(Unit)
        }

        internal fun preflightPotionConsume(
            name: String,
            itemId: Int,
            quantity: Int,
            state: CharacterState?,
            prefs: Preferences?,
        ): Result<Unit> {
            if (ConcoctionDatabase.getByResult(name) == null) {
                return Result.failure(IllegalStateException("No concoction for: $name"))
            }
            if (itemId > 0 && !ItemDatabase.isPotion(itemId)) {
                return Result.failure(IllegalStateException("Not a potion: $name"))
            }
            if (state == null) return Result.success(Unit)

            val concoction = ConcoctionDatabase.getByResult(name)!!
            if (!ConcoctionPermitted.isPermittedMethod(
                    concoction,
                    state,
                    prefs = prefs,
                    limitMode = state.limitMode,
                )
            ) {
                return Result.failure(IllegalStateException("Consumption not permitted: $name"))
            }

            if (itemId <= 0) return Result.success(Unit)

            val canUsePotions = !state.inRobocore || YouRobotManager.canUsePotions()
            if (!canUsePotions) {
                return Result.failure(IllegalStateException("Cannot use potions: $name"))
            }

            val ctx = ItemUseLimitsContext(
                character = state,
                preferences = prefs,
                expressionContext = ExpressionContext.EMPTY,
                canUsePotions = canUsePotions,
            )
            val allowed = maximumUses(itemId, name, ctx)
            if (allowed <= 0) {
                return Result.failure(IllegalStateException("Cannot use potion: $name"))
            }
            if (allowed != Int.MAX_VALUE && allowed < quantity) {
                return Result.failure(IllegalStateException("Cannot use $quantity of $name"))
            }
            return Result.success(Unit)
        }

        /** Cafe purchase preflight — organ/eligibility only (NOCREATE items skip craft-method gate). */
        internal fun preflightCafeConsume(
            name: String,
            type: ConcoctionConsumptionType,
            state: CharacterState?,
            prefs: Preferences?,
        ): Result<Unit> {
            if (ConcoctionDatabase.getByResult(name) == null) {
                return Result.failure(IllegalStateException("No concoction for: $name"))
            }
            if (state == null) return Result.success(Unit)
            when (type) {
                ConcoctionConsumptionType.EAT -> {
                    if (!ConsumptionEligibility.canEat(state)) {
                        return Result.failure(IllegalStateException("Cannot eat: $name"))
                    }
                    val fullness = ConsumableDatabase.getFullnessByName(name)
                    if (fullness > 0 &&
                        ConsumptionEligibility.effectiveFullnessRemaining(state) < fullness
                    ) {
                        return Result.failure(IllegalStateException("Not enough fullness for: $name"))
                    }
                }
                ConcoctionConsumptionType.DRINK -> {
                    if (!ConsumptionEligibility.canDrink(state)) {
                        return Result.failure(IllegalStateException("Cannot drink: $name"))
                    }
                    val inebriety = ConsumableDatabase.getInebrietyByName(name)
                    if (inebriety > 0 &&
                        ConsumptionEligibility.effectiveInebrietyRemaining(state) < inebriety
                    ) {
                        return Result.failure(IllegalStateException("Not enough liver for: $name"))
                    }
                }
                ConcoctionConsumptionType.SPLEEN -> {
                    if (!ConsumptionEligibility.canChew(state)) {
                        return Result.failure(IllegalStateException("Cannot chew: $name"))
                    }
                    val spleenHit = ConsumableDatabase.getSpleenByName(name)
                    if (spleenHit > 0 &&
                        ConsumptionEligibility.effectiveSpleenRemaining(state) < spleenHit
                    ) {
                        return Result.failure(IllegalStateException("Not enough spleen for: $name"))
                    }
                }
                ConcoctionConsumptionType.USE,
                ConcoctionConsumptionType.NONE,
                ConcoctionConsumptionType.GLUTTONOUS_GHOST,
                ConcoctionConsumptionType.SPIRIT_HOBO,
                ConcoctionConsumptionType.SLIMELING,
                ConcoctionConsumptionType.ROBORTENDER,
                ConcoctionConsumptionType.STOCKING_MIMIC,
                -> Unit
            }
            return Result.success(Unit)
        }
    }

    private fun preflightBinge(type: ConcoctionConsumptionType): Result<Unit> {
        val activeFamiliarId = familiarManager?.state?.value?.activeFamiliar?.id
        return preflightBingeWithFamiliar(type, activeFamiliarId)
    }
}
