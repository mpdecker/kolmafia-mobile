package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.character.CharacterState

/** Desktop [net.sourceforge.kolmafia.objectpool.Concoction.getMeatPasteNeeded]. */
object ConcoctionMeatPasteNeeded {
    fun needsPaste(concoction: ConcoctionData, state: CharacterState?): Boolean {
        if (state == null) return false
        if (!isPasteMethod(concoction)) return false
        return !state.knollAvailable || state.inZombiecore
    }

    fun getMeatPasteNeeded(
        concoction: ConcoctionData,
        quantityNeeded: Int,
        initialCount: Int = 0,
        state: CharacterState? = null,
    ): Int {
        val create = quantityNeeded - initialCount
        if (create <= 0) return 0
        if (!isPasteMethod(concoction)) return 0
        if (state != null && state.knollAvailable && !state.inZombiecore) return 0

        var runningTotal = create
        for (ingredient in concoction.ingredients) {
            val child = ConcoctionDatabase.getByResult(ingredient.name) ?: continue
            val childInitial = ConcoctionDatabase.getRuntime(child.result)?.initial ?: 0
            runningTotal += getMeatPasteNeeded(child, create, childInitial, state)
        }
        return runningTotal
    }

    private fun isPasteMethod(concoction: ConcoctionData): Boolean {
        if ("COMBINE" in concoction.methods || "ACOMBINE" in concoction.methods) return true
        if ("JEWEL" in concoction.methods || "EJEWEL" in concoction.methods) return true
        return "JEWELRY" in ConcoctionMethodAliases.normalize(concoction.methods)
    }
}
