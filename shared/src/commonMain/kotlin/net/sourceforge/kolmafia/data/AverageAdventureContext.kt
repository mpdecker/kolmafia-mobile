package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.preferences.Preferences

object ConsumableAdventureEffects {
    const val ODE = "Ode to Booze"
    const val GLORIOUS_LUNCH = "Song of the Glorious Lunch"
    const val BARREL_OF_LAUGHS = "Barrel of Laughs"
    const val BEER_BARREL_POLKA = "Beer Barrel Polka"
    const val GOURMAND_SKILL = "Gourmand"
    const val NEUROGOURMET_SKILL = "Neurogourmet"
    const val ROWDY_DRINKER_SKILL = "Rowdy Drinker"
}

data class AverageAdventureContext(
    val perUnit: Boolean = false,
    val milkActive: Boolean = false,
    val gloriousLunchActive: Boolean = false,
    val barrelLaughsCount: Int = 0,
    val odeActive: Boolean = false,
    val beerBarrelPolkaCount: Int = 0,
    val munchiesActive: Boolean = false,
    val hasGourmand: Boolean = false,
    val hasNeurogourmet: Boolean = false,
    val hasRowdyDrinker: Boolean = false,
    val inSlowcore: Boolean = false,
) {
    val lunchActive: Boolean get() = gloriousLunchActive || barrelLaughsCount >= 5
    val gourmandActive: Boolean get() = hasGourmand || hasNeurogourmet
    val rowdyActive: Boolean get() = hasRowdyDrinker || beerBarrelPolkaCount >= 5

    companion object {
        val EMPTY = AverageAdventureContext()
    }
}

fun buildAverageAdventureContext(
    preferences: Preferences? = null,
    activeEffectNames: List<String> = emptyList(),
    skillNames: Set<String> = emptySet(),
    ascensionPath: AscensionPath? = null,
): AverageAdventureContext {
    val normalizedEffects = activeEffectNames.map { it.lowercase() }
    val normalizedSkills = skillNames.map { it.lowercase() }.toSet()

    fun hasEffect(name: String): Boolean =
        normalizedEffects.any { it == name.lowercase() }

    fun countEffect(name: String): Int =
        normalizedEffects.count { it == name.lowercase() }

    fun hasSkill(name: String): Boolean =
        name.lowercase() in normalizedSkills

    return AverageAdventureContext(
        perUnit = preferences?.getBoolean("showGainsPerUnit") ?: false,
        milkActive = preferences?.getBoolean("milkOfMagnesiumActive") ?: false,
        gloriousLunchActive = hasEffect(ConsumableAdventureEffects.GLORIOUS_LUNCH),
        barrelLaughsCount = countEffect(ConsumableAdventureEffects.BARREL_OF_LAUGHS),
        odeActive = hasEffect(ConsumableAdventureEffects.ODE),
        beerBarrelPolkaCount = countEffect(ConsumableAdventureEffects.BEER_BARREL_POLKA),
        munchiesActive = (preferences?.getInt("munchiesPillsUsed") ?: 0) > 0,
        hasGourmand = hasSkill(ConsumableAdventureEffects.GOURMAND_SKILL),
        hasNeurogourmet = hasSkill(ConsumableAdventureEffects.NEUROGOURMET_SKILL),
        hasRowdyDrinker = hasSkill(ConsumableAdventureEffects.ROWDY_DRINKER_SKILL),
        inSlowcore = ascensionPath == AscensionPath.SLOW_AND_STEADY,
    )
}
