package net.sourceforge.kolmafia.data

/** Desktop ConcoctionDatabase queuedFancyDog/Smores/Speakeasy/etc. on craft queue push/pop. */
object ConcoctionSpecialQueue {

    const val SMORE = 5071
    const val AFFIRMATION_COOKIE = 9486
    const val SPAGHETTI_BREAKFAST = 6616
    const val EVERFULL_GLASS = 9966
    const val PIRATE_FORK = 10227

    fun isSpeakeasyDrink(itemId: Int): Boolean =
        DailyLimitDatabase.isSpeakeasyDrink(itemId)

    fun isFancyDog(resultName: String, context: ConcoctionQueueContext): Boolean =
        context.isFancyDog(resultName)

    fun reserve(resultName: String, quantity: Int, context: ConcoctionQueueContext): SpecialQueueDelta {
        if (quantity <= 0) {
            return SpecialQueueDelta(resultName = resultName, quantity = 0)
        }

        val itemId = ItemDatabase.getByName(resultName)?.id ?: 0
        var fancyDogUsed = false
        var speakeasyDrinkUsed = 0
        var smoresUsed = 0
        var affirmationCookiesUsed = 0
        var spaghettiBreakfastUsed = 0
        var everfullGlassUsed = 0
        var pirateForkUsed = 0
        var refreshedSmoresData = false

        if (isFancyDog(resultName, context)) {
            ConcoctionQueueBudget.queuedFancyDog = true
            fancyDogUsed = true
        }

        if (itemId != 0 && isSpeakeasyDrink(itemId)) {
            ConcoctionQueueBudget.queuedSpeakeasyDrink += quantity
            speakeasyDrinkUsed = quantity
        }

        when (itemId) {
            SMORE -> {
                ConcoctionQueueBudget.queuedSmores++
                smoresUsed = 1
                refreshedSmoresData = refreshSmoresData(context)
            }
            AFFIRMATION_COOKIE -> {
                ConcoctionQueueBudget.queuedAffirmationCookies++
                affirmationCookiesUsed = 1
            }
            SPAGHETTI_BREAKFAST -> {
                ConcoctionQueueBudget.queuedSpaghettiBreakfast++
                spaghettiBreakfastUsed = 1
            }
            EVERFULL_GLASS -> {
                ConcoctionQueueBudget.queuedEverfullGlass++
                everfullGlassUsed = 1
            }
            PIRATE_FORK -> {
                ConcoctionQueueBudget.queuedPirateFork++
                pirateForkUsed = 1
            }
        }

        return SpecialQueueDelta(
            resultName = resultName,
            quantity = quantity,
            itemId = itemId,
            fancyDogUsed = fancyDogUsed,
            speakeasyDrinkUsed = speakeasyDrinkUsed,
            smoresUsed = smoresUsed,
            affirmationCookiesUsed = affirmationCookiesUsed,
            spaghettiBreakfastUsed = spaghettiBreakfastUsed,
            everfullGlassUsed = everfullGlassUsed,
            pirateForkUsed = pirateForkUsed,
            refreshedSmoresData = refreshedSmoresData,
        )
    }

    fun release(delta: SpecialQueueDelta, context: ConcoctionQueueContext = ConcoctionQueueContext()) {
        if (delta.fancyDogUsed) {
            ConcoctionQueueBudget.queuedFancyDog = false
        }
        if (delta.speakeasyDrinkUsed != 0) {
            ConcoctionQueueBudget.queuedSpeakeasyDrink -= delta.speakeasyDrinkUsed
        }
        when (delta.itemId) {
            SMORE -> {
                ConcoctionQueueBudget.queuedSmores -= delta.smoresUsed
                refreshSmoresData(context)
                ConcoctionQueueBudget.queuedFullness++
            }
            AFFIRMATION_COOKIE ->
                ConcoctionQueueBudget.queuedAffirmationCookies -= delta.affirmationCookiesUsed
            SPAGHETTI_BREAKFAST ->
                ConcoctionQueueBudget.queuedSpaghettiBreakfast -= delta.spaghettiBreakfastUsed
            EVERFULL_GLASS ->
                ConcoctionQueueBudget.queuedEverfullGlass -= delta.everfullGlassUsed
            PIRATE_FORK ->
                ConcoctionQueueBudget.queuedPirateFork -= delta.pirateForkUsed
        }
    }

    fun reapply(delta: SpecialQueueDelta, context: ConcoctionQueueContext = ConcoctionQueueContext()) {
        if (delta.fancyDogUsed) {
            ConcoctionQueueBudget.queuedFancyDog = true
        }
        if (delta.speakeasyDrinkUsed != 0) {
            ConcoctionQueueBudget.queuedSpeakeasyDrink += delta.speakeasyDrinkUsed
        }
        when (delta.itemId) {
            SMORE -> {
                ConcoctionQueueBudget.queuedSmores += delta.smoresUsed
                if (delta.refreshedSmoresData) {
                    refreshSmoresData(context)
                }
                ConcoctionQueueBudget.queuedFullness--
            }
            AFFIRMATION_COOKIE ->
                ConcoctionQueueBudget.queuedAffirmationCookies += delta.affirmationCookiesUsed
            SPAGHETTI_BREAKFAST ->
                ConcoctionQueueBudget.queuedSpaghettiBreakfast += delta.spaghettiBreakfastUsed
            EVERFULL_GLASS ->
                ConcoctionQueueBudget.queuedEverfullGlass += delta.everfullGlassUsed
            PIRATE_FORK ->
                ConcoctionQueueBudget.queuedPirateFork += delta.pirateForkUsed
        }
    }

    private fun refreshSmoresData(context: ConcoctionQueueContext): Boolean {
        val prefs = context.preferences ?: return false
        ConsumableDatabase.setSmoresData(prefs)
        ConcoctionDatabase.markRecalculateAdventureRange()
        return true
    }
}

/** Special queue metadata for a single craft-queue push. */
data class SpecialQueueDelta(
    val resultName: String,
    val quantity: Int,
    val itemId: Int = 0,
    val fancyDogUsed: Boolean = false,
    val speakeasyDrinkUsed: Int = 0,
    val smoresUsed: Int = 0,
    val affirmationCookiesUsed: Int = 0,
    val spaghettiBreakfastUsed: Int = 0,
    val everfullGlassUsed: Int = 0,
    val pirateForkUsed: Int = 0,
    val refreshedSmoresData: Boolean = false,
)
