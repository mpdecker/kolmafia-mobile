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
import net.sourceforge.kolmafia.data.SpeakeasyDatabase
import net.sourceforge.kolmafia.data.isAutoCraftable
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CafeNotOnMenuException
import net.sourceforge.kolmafia.request.CafePurchaseRequest
import net.sourceforge.kolmafia.request.ClanLoungeRequest
import net.sourceforge.kolmafia.request.ConcoctionCreateRequest
import net.sourceforge.kolmafia.request.DrinkBoozeRequest
import net.sourceforge.kolmafia.request.EatFoodRequest
import net.sourceforge.kolmafia.request.ChewRequest
import net.sourceforge.kolmafia.request.StillSuitRequest

/**
 * Desktop [ConcoctionDatabase.handleQueue] drain — lounge v1 + inventory eat/drink v2 + craft-only v3 + cafe v4 + stillsuit v5 + spleen v6.
 */
class ConcoctionQueueRunner(
    private val clanLoungeRequest: ClanLoungeRequest,
    private val eatFoodRequest: EatFoodRequest? = null,
    private val drinkBoozeRequest: DrinkBoozeRequest? = null,
    private val chewRequest: ChewRequest? = null,
    private val retrieveItemService: RetrieveItemService? = null,
    private val concoctionCreateRequest: ConcoctionCreateRequest? = null,
    private val cafePurchaseRequest: CafePurchaseRequest? = null,
    private val stillSuitRequest: StillSuitRequest? = null,
) {

    suspend fun handleQueue(
        bucket: QueueBucket,
        type: ConcoctionConsumptionType,
        preferences: Preferences? = null,
        state: CharacterState? = null,
    ): Result<Unit> {
        if (type != ConcoctionConsumptionType.EAT &&
            type != ConcoctionConsumptionType.DRINK &&
            type != ConcoctionConsumptionType.SPLEEN
        ) {
            return Result.failure(IllegalArgumentException("Unsupported consumption type: $type"))
        }

        val toProcess = mutableListOf<ConcoctionQueueReservation>()
        while (true) {
            val item = ConcoctionCraftQueue.pop(bucket) ?: break
            toProcess.add(item)
        }

        ConcoctionDatabase.refreshConcoctionsNowFromLastContext()

        var loungeConsumed = false
        for (reservation in toProcess.asReversed()) {
            val outcome = processReservation(
                reservation = reservation,
                type = type,
                preferences = preferences,
                state = state,
                onLoungeConsumed = { loungeConsumed = true },
            )
            if (outcome.isFailure) {
                return outcome
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
    ): Result<Unit> {
        val name = reservation.resultName
        val quantity = reservation.quantity
        when (type) {
            ConcoctionConsumptionType.EAT -> {
                if (HotDogDatabase.isHotDog(name)) {
                    onLoungeConsumed()
                    repeat(quantity) {
                        clanLoungeRequest.eatHotDog(name, preferences, state)
                            .onFailure { return Result.failure(it) }
                    }
                    return Result.success(Unit)
                }
                return consumeQueuedItem(name, quantity, type, state, preferences)
            }
            ConcoctionConsumptionType.DRINK -> {
                if (SpeakeasyDatabase.isSpeakeasyDrink(name)) {
                    onLoungeConsumed()
                    repeat(quantity) {
                        clanLoungeRequest.drinkSpeakeasy(name, preferences, state)
                            .onFailure { return Result.failure(it) }
                    }
                    return Result.success(Unit)
                }
                return consumeQueuedItem(name, quantity, type, state, preferences)
            }
            ConcoctionConsumptionType.SPLEEN ->
                return consumeQueuedItem(name, quantity, type, state, preferences)
            ConcoctionConsumptionType.NONE -> Unit
        }
        return Result.success(Unit)
    }

    private suspend fun consumeQueuedItem(
        name: String,
        quantity: Int,
        type: ConcoctionConsumptionType,
        state: CharacterState?,
        preferences: Preferences?,
    ): Result<Unit> {
        val itemId = ItemDatabase.getByName(name)?.id ?: 0
        return if (itemId > 0) {
            consumeInventoryItem(name, itemId, quantity, type, state, preferences)
        } else {
            consumeCraftOnlyItem(name, quantity, type, state, preferences)
        }
    }

    private suspend fun consumeInventoryItem(
        name: String,
        itemId: Int,
        quantity: Int,
        type: ConcoctionConsumptionType,
        state: CharacterState?,
        preferences: Preferences?,
    ): Result<Unit> {
        preflightInventoryConsume(name, type, state, preferences)
            .onFailure { return Result.failure(it) }

        val retrieve = retrieveItemService
            ?: return Result.failure(IllegalStateException("RetrieveItemService not configured"))
        val retrieved = retrieve.retrieve(itemId, quantity)
        if (retrieved < quantity) {
            return Result.failure(IllegalStateException("Could not retrieve $quantity of $name (got $retrieved)"))
        }

        return consumeWithEatDrinkOrChew(itemId, quantity, type)
    }

    private suspend fun consumeCraftOnlyItem(
        name: String,
        quantity: Int,
        type: ConcoctionConsumptionType,
        state: CharacterState?,
        preferences: Preferences?,
    ): Result<Unit> {
        val concoction = ConcoctionDatabase.getByResult(name) ?: return Result.success(Unit)
        if (concoction.isAutoCraftable()) {
            preflightInventoryConsume(name, type, state, preferences)
                .onFailure { return Result.failure(it) }

            val create = concoctionCreateRequest
                ?: return Result.failure(IllegalStateException("ConcoctionCreateRequest not configured"))
            create.create(name, quantity).onFailure { return Result.failure(it) }

            val outputId = ItemDatabase.getByName(name)?.id ?: 0
            return if (outputId > 0) {
                consumeWithEatDrinkOrChew(outputId, quantity, type)
            } else {
                Result.success(Unit)
            }
        }
        if (StillSuitRequest.isDistillate(name)) {
            return consumeStillSuitItem(name, quantity, type, state, preferences)
        }
        return consumeCafeItem(name, quantity, type, state, preferences)
    }

    private suspend fun consumeStillSuitItem(
        name: String,
        quantity: Int,
        type: ConcoctionConsumptionType,
        state: CharacterState?,
        preferences: Preferences?,
    ): Result<Unit> {
        val request = stillSuitRequest
            ?: return Result.failure(IllegalStateException("StillSuitRequest not configured"))
        repeat(quantity) {
            request.distill(name, type, state, preferences)
                .onFailure { return Result.failure(it) }
            ConcoctionDatabase.refreshConcoctionsNowFromLastContext()
        }
        return Result.success(Unit)
    }

    private suspend fun consumeCafeItem(
        name: String,
        quantity: Int,
        type: ConcoctionConsumptionType,
        state: CharacterState?,
        preferences: Preferences?,
    ): Result<Unit> {
        val purchase = cafePurchaseRequest
            ?: return Result.success(Unit)
        repeat(quantity) {
            val outcome = purchase.purchase(name, type, state, preferences)
            if (outcome.isFailure) {
                val error = outcome.exceptionOrNull()
                if (error is CafeNotOnMenuException) {
                    return Result.success(Unit)
                }
                return Result.failure(error ?: IllegalStateException("Cafe purchase failed"))
            }
        }
        return Result.success(Unit)
    }

    private suspend fun consumeWithEatDrinkOrChew(
        itemId: Int,
        quantity: Int,
        type: ConcoctionConsumptionType,
    ): Result<Unit> {
        return when (type) {
            ConcoctionConsumptionType.EAT -> {
                val eat = eatFoodRequest
                    ?: return Result.failure(IllegalStateException("EatFoodRequest not configured"))
                eat.eat(itemId, quantity).map { }
            }
            ConcoctionConsumptionType.DRINK -> {
                val drink = drinkBoozeRequest
                    ?: return Result.failure(IllegalStateException("DrinkBoozeRequest not configured"))
                drink.drink(itemId, quantity).map { }
            }
            ConcoctionConsumptionType.SPLEEN -> {
                val chew = chewRequest
                    ?: return Result.failure(IllegalStateException("ChewRequest not configured"))
                chew.chew(itemId, quantity).map { }
            }
            ConcoctionConsumptionType.NONE -> Result.success(Unit)
        }
    }

    companion object {
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
                    ConcoctionConsumptionType.NONE -> Unit
                }
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
                ConcoctionConsumptionType.NONE -> Unit
            }
            return Result.success(Unit)
        }
    }
}
