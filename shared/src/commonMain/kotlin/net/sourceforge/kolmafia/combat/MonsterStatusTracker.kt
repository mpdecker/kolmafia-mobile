package net.sourceforge.kolmafia.combat

import net.sourceforge.kolmafia.data.MonsterDefinition

/**
 * Session-scoped last-fight monster instance. Mirrors desktop [MonsterStatusTracker]
 * for modifier list population only (no combat stat mutation in Phase 111).
 */
object MonsterStatusTracker {
    private var monsterData: MonsterDefinition? = null
    private var lastMonsterName: String = ""

    fun setNextMonster(template: MonsterDefinition, modifiers: List<String>) {
        reset()
        monsterData = template.copy(randomModifiers = modifiers)
        lastMonsterName = template.name
    }

    fun getLastMonster(): MonsterDefinition? = monsterData

    fun getLastMonsterName(): String = lastMonsterName

    fun reset() {
        // Combat-round stat modifiers deferred; resetLastMonster clears instance.
    }

    fun resetLastMonster() {
        lastMonsterName = ""
        monsterData = null
    }
}
