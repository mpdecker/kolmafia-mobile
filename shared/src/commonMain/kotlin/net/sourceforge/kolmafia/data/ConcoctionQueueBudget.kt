package net.sourceforge.kolmafia.data

/** Desktop ConcoctionDatabase queued* counters — reserved craft budgets before refresh. */
object ConcoctionQueueBudget {
    var adventuresUsed: Int = 0
    var freeCraftingTurns: Int = 0
    var stillsUsed: Int = 0
    var tomesUsed: Int = 0
    var extrudesUsed: Int = 0
    var meatSpent: Int = 0
    var pullsUsed: Int = 0
    var queuedFullness: Int = 0
    var queuedInebriety: Int = 0
    var queuedSpleenHit: Int = 0
    var queuedMimeShotglass: Boolean = false
    var lastQueuedMayo: Int = 0
    var queuedFancyDog: Boolean = false
    var queuedSpeakeasyDrink: Int = 0
    var queuedSmores: Int = 0
    var queuedAffirmationCookies: Int = 0
    var queuedSpaghettiBreakfast: Int = 0
    var queuedEverfullGlass: Int = 0
    var queuedPirateFork: Int = 0

    internal fun resetForTest() {
        adventuresUsed = 0
        freeCraftingTurns = 0
        stillsUsed = 0
        tomesUsed = 0
        extrudesUsed = 0
        meatSpent = 0
        pullsUsed = 0
        queuedFullness = 0
        queuedInebriety = 0
        queuedSpleenHit = 0
        queuedMimeShotglass = false
        lastQueuedMayo = 0
        queuedFancyDog = false
        queuedSpeakeasyDrink = 0
        queuedSmores = 0
        queuedAffirmationCookies = 0
        queuedSpaghettiBreakfast = 0
        queuedEverfullGlass = 0
        queuedPirateFork = 0
        ConcoctionQueuedIngredients.resetForTest()
        ConcoctionQueuedPseudoIngredients.resetForTest()
    }
}
