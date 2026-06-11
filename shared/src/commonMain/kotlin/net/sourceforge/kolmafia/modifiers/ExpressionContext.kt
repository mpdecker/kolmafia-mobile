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
    val isRestricted: Boolean = false,   // in Ronin or HC

    /** Accumulated modifiers so far (for mod(stat) during incremental parsing). */
    val currentModifiers: ModifierValues? = null,
    /** Lowercase familiar name for fam()/famattr(). */
    val familiarName: String = "",
    /** Lowercase main-hand weapon item name for mainhand(). */
    val mainhandItemName: String = "",
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

    /** mod(stat) or mod(stat, itemName) — numeric modifier lookup. */
    fun modValue(stat: String, itemName: String? = null): Double {
        val dm = DoubleModifier.byTag(stat) ?: return 0.0
        if (itemName != null) {
            val entry = resolveItemModifierEntry(itemName) ?: return 0.0
            return ModifierParser.parse(entry.modifiers, this).get(dm)
        }
        return currentModifiers?.get(dm) ?: 0.0
    }

    /** fam(attr) — familiar weight for matching familiar sub-type attribute. */
    fun famValue(attr: String): Double {
        if (familiarName.isBlank()) return 0.0
        val dm = DoubleModifier.byTag(attr) ?: return 0.0
        val entry = net.sourceforge.kolmafia.data.ModifierDatabase.getFamiliar(familiarName)
            ?: return 0.0
        return ModifierParser.parse(entry.modifiers, this).get(dm)
    }

    /** famattr(attr) — numeric attribute from current familiar modifiers. */
    fun famattrValue(attr: String): Double = famValue(attr)

    /** mainhand(attr) — modifier from equipped main-hand weapon. */
    fun mainhandValue(attr: String): Double {
        if (mainhandItemName.isBlank()) return 0.0
        val dm = DoubleModifier.byTag(attr) ?: return 0.0
        val entry = net.sourceforge.kolmafia.data.ModifierDatabase.getItem(mainhandItemName)
            ?: return 0.0
        return ModifierParser.parse(entry.modifiers, this).get(dm)
    }

    /** res(stat) — elemental resistance from current modifiers. */
    fun resValue(stat: String): Double {
        val tag = when {
            stat.equals("Cold", ignoreCase = true) -> "Cold Resistance"
            stat.equals("Hot", ignoreCase = true) -> "Hot Resistance"
            stat.equals("Sleaze", ignoreCase = true) -> "Sleaze Resistance"
            stat.equals("Spooky", ignoreCase = true) -> "Spooky Resistance"
            stat.equals("Stench", ignoreCase = true) -> "Stench Resistance"
            stat.endsWith("Resistance", ignoreCase = true) -> stat
            else -> "$stat Resistance"
        }
        return modValue(tag)
    }

    private fun resolveItemModifierEntry(itemRef: String): net.sourceforge.kolmafia.data.ModifierEntry? {
        val trimmed = itemRef.trim()
        trimmed.toIntOrNull()?.let { id ->
            net.sourceforge.kolmafia.data.ItemDatabase.getById(id)?.name?.let { name ->
                return net.sourceforge.kolmafia.data.ModifierDatabase.getItem(name)
            }
        }
        return net.sourceforge.kolmafia.data.ModifierDatabase.getItem(trimmed)
    }

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
            isRestricted = state.isRestricted,
            familiarName = state.familiarName.lowercase(),
            mainhandItemName = state.equipment[net.sourceforge.kolmafia.character.EquipmentSlot.WEAPON]
                ?.lowercase() ?: "",
        )
    }
}
