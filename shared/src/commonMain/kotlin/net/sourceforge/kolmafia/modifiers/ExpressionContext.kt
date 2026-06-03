package net.sourceforge.kolmafia.modifiers

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.effect.EffectData

/**
 * Snapshot of character state needed to evaluate modifier expressions like
 * [L], [W], [effect(Some Buff)], [skill(Iron Liver)], etc.
 *
 * Variable letters mirror the desktop:
 *   A=ascensions  D=drunk  E=effects-count  F=fullness  H=hobo-power
 *   K=smithsness  L=level  M=moonlight  N=audience  S=spleen
 *   U=telescope   W=familiar-weight  X=gender(-1/1)  Y=fury
 */
data class ExpressionContext(
    val level: Int = 1,
    val inebriety: Int = 0,          // D
    val fullness: Int = 0,           // F
    val spleenUsed: Int = 0,         // S
    val familiarWeight: Int = 0,     // W
    val ascensions: Int = 0,         // A
    val effectsCount: Int = 0,       // E
    val fury: Int = 0,               // Y
    val hoboPower: Double = 0.0,     // H
    val smithsness: Double = 0.0,    // K
    val moonlight: Int = 0,          // M
    val audience: Int = 0,           // N
    val telescopeUpgrades: Int = 0,  // U
    val gender: Int = 0,             // X  (-1=male, 1=female)

    // Map of lowercase effect name → turns remaining
    val activeEffects: Map<String, Int> = emptyMap(),
    // Set of lowercase skill names the character possesses
    val skills: Set<String> = emptySet(),

    val challengePath: String = "",
    val className: String = "",
    val currentLocation: String = "",
    val currentZone: String = "",
    val environment: String = "",
    val isRestricted: Boolean = false   // in Ronin or HC
) {
    fun variable(c: Char): Double = when (c) {
        'A' -> ascensions.toDouble()
        'D' -> inebriety.toDouble()
        'E' -> effectsCount.toDouble()
        'F' -> fullness.toDouble()
        'H' -> hoboPower
        'K' -> smithsness
        'L' -> level.toDouble()
        'M' -> moonlight.toDouble()
        'N' -> audience.toDouble()
        'S' -> spleenUsed.toDouble()
        'U' -> telescopeUpgrades.toDouble()
        'W' -> familiarWeight.toDouble()
        'X' -> gender.toDouble()
        'Y' -> fury.toDouble()
        else -> 0.0
    }

    fun effectTurns(name: String): Double =
        (activeEffects[name.lowercase()] ?: 0).toDouble()

    fun hasSkill(name: String): Boolean = name.lowercase() in skills

    fun locContains(text: String): Boolean =
        currentLocation.contains(text, ignoreCase = true)

    fun zoneContains(text: String): Boolean =
        currentZone.contains(text, ignoreCase = true)

    fun envContains(text: String): Boolean =
        environment.contains(text, ignoreCase = true)

    fun pathContains(text: String): Boolean =
        challengePath.contains(text, ignoreCase = true)

    fun classContains(text: String): Boolean =
        className.contains(text, ignoreCase = true)

    companion object {
        val EMPTY = ExpressionContext()

        fun from(
            state: CharacterState,
            effects: List<EffectData>,
            passiveSkillNames: Set<String> = emptySet()
        ): ExpressionContext = ExpressionContext(
            level = state.level,
            inebriety = state.inebriety,
            fullness = state.fullness,
            spleenUsed = state.spleenUsed,
            familiarWeight = state.familiarWeight,
            ascensions = state.ascensionNumber,
            effectsCount = effects.size,
            fury = state.fury,
            audience = state.audience,
            gender = state.gender.modifierValue,
            telescopeUpgrades = state.telescopeUpgrades,
            activeEffects = effects.associate { it.name.lowercase() to it.duration },
            skills = passiveSkillNames,
            challengePath = state.challengePath,
            className = state.className,
            isRestricted = state.isRestricted
        )
    }
}
