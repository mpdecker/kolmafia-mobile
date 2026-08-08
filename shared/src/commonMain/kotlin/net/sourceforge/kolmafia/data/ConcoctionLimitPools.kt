package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.item.FreeCraftingTurns
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillData

/** Desktop cachePermitted pseudo-concoction — adventure/turn-free budget pool. */
class ConcoctionLimitPool(
    visibleTotal: Int,
    initial: Int = visibleTotal,
) {
    /** Gross pool size before queue reservation (desktop Concoction.visibleTotal). */
    val visibleTotal: Int = visibleTotal.coerceAtLeast(0)
    /** Net pool remaining after queue reservation (desktop Concoction.initial). */
    val initial: Int = initial.coerceAtLeast(0)
    var allocated: Int = 0
        private set

    fun resetAllocated() {
        allocated = 0
    }

    /**
     * Desktop limit pseudo-concoction canMake (NOCREATE) — returns [initial - allocated]
     * before this request, after reserving [requested] units.
     */
    fun canMake(requested: Int): Int {
        if (requested <= 0) return 0
        val alreadyHave = initial - allocated
        if (alreadyHave < 0) return 0
        allocated += requested
        return alreadyHave
    }

    fun subtractAllocated(amount: Int) {
        allocated -= amount
    }
}

/** Desktop ConcoctionDatabase limit concoctions from cachePermitted. */
data class ConcoctionLimitPools(
    val adventureLimit: ConcoctionLimitPool,
    val adventureSmithingLimit: ConcoctionLimitPool,
    val cookingLimit: ConcoctionLimitPool,
    val cocktailcraftingLimit: ConcoctionLimitPool,
    val turnFreeLimit: ConcoctionLimitPool,
    val turnFreeSmithingLimit: ConcoctionLimitPool,
    val turnFreeCookingLimit: ConcoctionLimitPool,
    val turnFreeCocktailcraftingLimit: ConcoctionLimitPool,
    val stillsLimit: ConcoctionLimitPool,
    val clipArtLimit: ConcoctionLimitPool,
    val extrudeLimit: ConcoctionLimitPool,
    val meatLimit: ConcoctionLimitPool,
) {
    fun resetAllocated() {
        adventureLimit.resetAllocated()
        adventureSmithingLimit.resetAllocated()
        cookingLimit.resetAllocated()
        cocktailcraftingLimit.resetAllocated()
        turnFreeLimit.resetAllocated()
        turnFreeSmithingLimit.resetAllocated()
        turnFreeCookingLimit.resetAllocated()
        turnFreeCocktailcraftingLimit.resetAllocated()
        stillsLimit.resetAllocated()
        clipArtLimit.resetAllocated()
        extrudeLimit.resetAllocated()
        meatLimit.resetAllocated()
    }

    fun poolFor(methods: Set<String>, turnFreeOnly: Boolean): ConcoctionLimitPool? {
        val method = ConcoctionCreationCost.primaryMethod(methods) ?: return null
        return when (method) {
            "SMITH", "SSMITH" ->
                if (turnFreeOnly) turnFreeSmithingLimit else adventureSmithingLimit
            "COOK_FANCY" ->
                if (turnFreeOnly) turnFreeCookingLimit else cookingLimit
            "MIX_FANCY" ->
                if (turnFreeOnly) turnFreeCocktailcraftingLimit else cocktailcraftingLimit
            else -> {
                if (ConcoctionCreationCost.adventureUsage(methods) <= 0) return null
                if (turnFreeOnly) turnFreeLimit else adventureLimit
            }
        }
    }

    fun methodPool(primaryMethod: String): ConcoctionLimitPool? = when (primaryMethod) {
        "STILL" -> stillsLimit
        "CLIPART" -> clipArtLimit
        "TERMINAL" -> extrudeLimit
        else -> null
    }

    companion object {
        fun fromLiveSession(
            state: CharacterState,
            skills: List<SkillData> = emptyList(),
            prefs: Preferences? = null,
            effects: List<EffectData> = emptyList(),
            itemCount: (Int) -> Int = { 0 },
            ownedFamiliar: (String) -> Boolean = { false },
        ): ConcoctionLimitPools {
            val freeContext = FreeCraftingTurns.Context(
                preferences = prefs,
                state = state,
                skills = skills,
                effects = effects,
                itemCount = itemCount,
                ownedFamiliar = ownedFamiliar,
            )
            val freeCrafts = FreeCraftingTurns.freeCraftingTurns(freeContext)
            val freeSmith = FreeCraftingTurns.freeSmithingTurns(freeContext)
            val freeCook = FreeCraftingTurns.freeCookingTurns(freeContext)
            val freeMix = FreeCraftingTurns.freeCocktailcraftingTurns(freeContext)
            val adventuresLeft = state.adventuresLeft.coerceAtLeast(0)
            val stills = state.stillsAvailable.coerceAtLeast(0)
            val clipArt = clipArtRemaining(prefs, state)
            val extrudes = extrudeRemaining(prefs)
            val meat = state.meat.coerceAtLeast(0)
            val queued = ConcoctionQueueBudget

            val adventureTotal = adventuresLeft + freeCrafts
            val adventureSmithingTotal = adventuresLeft + freeCrafts + freeSmith
            val cookingTotal = adventuresLeft + freeCrafts + freeCook
            val cocktailcraftingTotal = adventuresLeft + freeCrafts + freeMix
            val turnFreeSmithingTotal = freeCrafts + freeSmith
            val turnFreeCookingTotal = freeCrafts + freeCook
            val turnFreeCocktailcraftingTotal = freeCrafts + freeMix

            return ConcoctionLimitPools(
                adventureLimit = limitPool(adventureTotal, queued.adventuresUsed),
                adventureSmithingLimit = limitPool(adventureSmithingTotal, queued.adventuresUsed),
                cookingLimit = limitPool(cookingTotal, queued.adventuresUsed),
                cocktailcraftingLimit = limitPool(cocktailcraftingTotal, queued.adventuresUsed),
                turnFreeLimit = limitPool(freeCrafts, queued.freeCraftingTurns),
                turnFreeSmithingLimit = limitPool(turnFreeSmithingTotal, queued.freeCraftingTurns),
                turnFreeCookingLimit = limitPool(turnFreeCookingTotal, queued.freeCraftingTurns),
                turnFreeCocktailcraftingLimit = limitPool(
                    turnFreeCocktailcraftingTotal,
                    queued.freeCraftingTurns,
                ),
                stillsLimit = limitPool(stills, queued.stillsUsed),
                clipArtLimit = limitPool(clipArt, queued.tomesUsed),
                extrudeLimit = limitPool(extrudes, queued.extrudesUsed),
                meatLimit = limitPool(meat, queued.meatSpent),
            )
        }

        private fun limitPool(gross: Int, queued: Int): ConcoctionLimitPool =
            ConcoctionLimitPool(gross, poolInitial(gross, queued))

        private fun poolInitial(total: Int, queued: Int): Int = (total - queued).coerceAtLeast(0)

        /** Test helper — permissive method pools unless overridden. */
        internal fun forTest(
            adventureLimit: Int = 999,
            adventureSmithingLimit: Int = 999,
            cookingLimit: Int = 999,
            cocktailcraftingLimit: Int = 999,
            turnFreeLimit: Int = 999,
            turnFreeSmithingLimit: Int = 999,
            turnFreeCookingLimit: Int = 999,
            turnFreeCocktailcraftingLimit: Int = 999,
            stillsLimit: Int = 999,
            clipArtLimit: Int = 999,
            extrudeLimit: Int = 999,
            meatLimit: Int = 999,
            adventuresUsed: Int = 0,
            freeCraftingTurns: Int = 0,
            stillsUsed: Int = 0,
            tomesUsed: Int = 0,
            extrudesUsed: Int = 0,
            meatSpent: Int = 0,
        ): ConcoctionLimitPools = ConcoctionLimitPools(
            adventureLimit = limitPool(adventureLimit, adventuresUsed),
            adventureSmithingLimit = limitPool(adventureSmithingLimit, adventuresUsed),
            cookingLimit = limitPool(cookingLimit, adventuresUsed),
            cocktailcraftingLimit = limitPool(cocktailcraftingLimit, adventuresUsed),
            turnFreeLimit = limitPool(turnFreeLimit, freeCraftingTurns),
            turnFreeSmithingLimit = limitPool(turnFreeSmithingLimit, freeCraftingTurns),
            turnFreeCookingLimit = limitPool(turnFreeCookingLimit, freeCraftingTurns),
            turnFreeCocktailcraftingLimit = limitPool(
                turnFreeCocktailcraftingLimit,
                freeCraftingTurns,
            ),
            stillsLimit = limitPool(stillsLimit, stillsUsed),
            clipArtLimit = limitPool(clipArtLimit, tomesUsed),
            extrudeLimit = limitPool(extrudeLimit, extrudesUsed),
            meatLimit = limitPool(meatLimit, meatSpent),
        )

        private fun clipArtRemaining(prefs: Preferences?, state: CharacterState): Int {
            val canInteract = !state.isHardcore && !state.isInRonin
            val summonsUsed = if (canInteract) {
                prefs?.getInt("_clipartSummons", 0) ?: 0
            } else {
                prefs?.getInt("tomeSummons", 0) ?: 0
            }
            return (3 - summonsUsed).coerceAtLeast(0)
        }

        private fun extrudeRemaining(prefs: Preferences?): Int {
            val used = prefs?.getInt("_sourceTerminalExtrudes", 0) ?: 0
            return (3 - used).coerceAtLeast(0)
        }
    }
}
