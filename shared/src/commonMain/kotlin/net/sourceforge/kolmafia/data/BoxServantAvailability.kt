package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillData

/** Desktop ConcoctionDatabase.isAvailable(servantId, clockworkId) for box servants. */
object BoxServantAvailability {

    const val CHEF = 438
    const val BARTENDER = 440
    const val CLOCKWORK_BARTENDER = 1111
    const val CLOCKWORK_CHEF = 1112

    fun isAvailable(
        servantId: Int,
        clockworkId: Int,
        prefs: Preferences?,
        state: CharacterState,
        accessibleCount: (Int) -> Int,
        skills: List<SkillData> = emptyList(),
        familiarUsable: (Int) -> Boolean = { false },
    ): Boolean {
        if (prefs?.getBoolean("autoRepairBoxServants", false) != true) return false
        if (state.inGLover) return false
        return creatableCount(servantId, state, prefs, accessibleCount, skills, familiarUsable) > 0 ||
            creatableCount(clockworkId, state, prefs, accessibleCount, skills, familiarUsable) > 0
    }

    internal fun creatableCount(
        itemId: Int,
        state: CharacterState,
        prefs: Preferences?,
        accessibleCount: (Int) -> Int,
        skills: List<SkillData>,
        familiarUsable: (Int) -> Boolean,
    ): Int {
        val itemName = ItemDatabase.getById(itemId)?.name ?: return 0
        val concoction = ConcoctionDatabase.getByResult(itemName) ?: return 0
        if (!ConcoctionPermitted.isPermittedMethod(
                concoction,
                state,
                skills,
                accessibleCount = accessibleCount,
                prefs = prefs,
                familiarUsable = familiarUsable,
            )
        ) {
            return 0
        }
        if (concoction.ingredients.isEmpty()) return 0
        var minYield = Int.MAX_VALUE
        for (ingredient in concoction.ingredients) {
            val ingId = ItemDatabase.getByName(ingredient.name)?.id ?: return 0
            val available = accessibleCount(ingId)
            minYield = minOf(minYield, available / ingredient.quantity.coerceAtLeast(1))
        }
        return if (minYield == Int.MAX_VALUE) 0 else minYield
    }
}
