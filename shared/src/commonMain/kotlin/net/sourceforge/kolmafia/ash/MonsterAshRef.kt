package net.sourceforge.kolmafia.ash

/**
 * MONSTER entity reference. [useInstance] true for [last_monster] fight-time clone.
 */
internal data class MonsterAshRef(
    val name: String,
    val useInstance: Boolean = false,
) {
    override fun toString(): String = name
}

internal fun AshValue.monsterRefName(): String = when (val value = content) {
    is MonsterAshRef -> value.name
    else -> toString()
}

internal fun AshValue.monsterUseInstance(): Boolean =
    (content as? MonsterAshRef)?.useInstance == true
