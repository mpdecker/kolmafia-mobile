package net.sourceforge.kolmafia.modifiers

import net.sourceforge.kolmafia.character.CharacterState

/**
 * Canonical ASH stat entity names. Mirrors desktop [KoLConstants.Stat] / parseStatValue.
 */
object StatNames {

    const val MUSCLE = "Muscle"
    const val MYSTICALITY = "Mysticality"
    const val MOXIE = "Moxie"
    const val SUBMUSCLE = "SubMuscle"
    const val SUBMYSTICALITY = "SubMysticality"
    const val SUBMOXIE = "SubMoxie"

    private val CANONICAL = listOf(
        MUSCLE, MYSTICALITY, MOXIE, SUBMUSCLE, SUBMYSTICALITY, SUBMOXIE,
    )

    fun resolve(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.equals("none", ignoreCase = true)) return null
        return when (trimmed.lowercase()) {
            "muscle", "mus" -> MUSCLE
            "mysticality", "myst", "mys" -> MYSTICALITY
            "moxie", "mox" -> MOXIE
            "submuscle", "submus" -> SUBMUSCLE
            "submysticality", "submyst", "submys" -> SUBMYSTICALITY
            "submoxie", "submox" -> SUBMOXIE
            else -> CANONICAL.firstOrNull { it.equals(trimmed, ignoreCase = true) }
        }
    }

    fun isValid(name: String): Boolean = resolve(name) != null

    fun baseValue(state: CharacterState, statName: String): Long {
        val resolved = resolve(statName) ?: return 0L
        return when (resolved) {
            MUSCLE -> state.baseMusc.toLong()
            MYSTICALITY -> state.baseMyst.toLong()
            MOXIE -> state.baseMoxie.toLong()
            SUBMUSCLE -> state.muscSubpoints
            SUBMYSTICALITY -> state.mystSubpoints
            SUBMOXIE -> state.moxieSubpoints
            else -> 0L
        }
    }

    fun buffedValue(state: CharacterState, statName: String): Long {
        val resolved = resolve(statName) ?: return 0L
        return when (resolved) {
            MUSCLE -> state.buffedMusc.toLong()
            MYSTICALITY -> state.buffedMyst.toLong()
            MOXIE -> state.buffedMoxie.toLong()
            else -> 0L
        }
    }
}
