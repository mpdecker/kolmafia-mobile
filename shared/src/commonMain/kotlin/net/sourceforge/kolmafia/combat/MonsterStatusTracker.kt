package net.sourceforge.kolmafia.combat

import net.sourceforge.kolmafia.data.MonsterDefinition

/**
 * Session-scoped last-fight monster instance. Mirrors desktop [MonsterStatusTracker]
 * including live HP/atk/def modifiers (Phases 1301–1310).
 */
object MonsterStatusTracker {
    private var monsterData: MonsterDefinition? = null
    private var lastMonsterName: String = ""

    private var healthModifier: Int = 0
    private var attackModifier: Int = 0
    private var defenseModifier: Int = 0
    private var healthManuel: Int = 0
    private var attackManuel: Int = 0
    private var defenseManuel: Int = 0
    private var manuelFound: Boolean = false
    private var originalHealth: Int = 0
    private var originalAttack: Int = 0
    private var originalDefense: Int = 0

    fun setNextMonster(template: MonsterDefinition, modifiers: List<String>) {
        reset()
        monsterData = template.copy(randomModifiers = modifiers)
        lastMonsterName = template.name
        originalHealth = template.hp
        originalAttack = template.attack
        originalDefense = template.defense
    }

    fun getLastMonster(): MonsterDefinition? = monsterData

    fun getLastMonsterName(): String = lastMonsterName

    fun reset() {
        healthModifier = 0
        attackModifier = 0
        defenseModifier = 0
        healthManuel = 0
        attackManuel = 0
        defenseManuel = 0
        manuelFound = false
    }

    fun resetLastMonster() {
        lastMonsterName = ""
        monsterData = null
        reset()
        originalHealth = 0
        originalAttack = 0
        originalDefense = 0
    }

    fun getMonsterHealth(): Int {
        if (monsterData == null) return 0
        return (originalHealth - healthModifier).coerceAtLeast(0)
    }

    fun getMonsterAttack(): Int {
        if (monsterData == null) return 0
        val adjusted = originalAttack + attackModifier
        return if (originalAttack == 0) adjusted else adjusted.coerceAtLeast(1)
    }

    fun getMonsterDefense(): Int {
        if (monsterData == null) return 0
        val adjusted = originalDefense + defenseModifier
        return if (originalDefense == 0) adjusted else adjusted.coerceAtLeast(1)
    }

    fun getMonsterOriginalAttack(): Int =
        if (monsterData == null) 0 else originalAttack

    fun getMonsterAttackModifier(): Int =
        if (monsterData == null) 0 else attackModifier

    fun getMonsterDefenseModifier(): Int =
        if (monsterData == null) 0 else defenseModifier

    fun healMonster(amount: Int) {
        healthModifier = (healthModifier - amount).coerceAtLeast(0)
    }

    fun damageMonster(amount: Int) {
        healthModifier += amount
    }

    fun resetAttackAndDefense() {
        attackModifier = 0
        defenseModifier = 0
    }

    fun lowerMonsterAttack(amount: Int) {
        attackModifier -= amount
    }

    fun lowerMonsterDefense(amount: Int) {
        defenseModifier -= amount
    }

    fun setManuelStats(attack: Int, defense: Int, hp: Int) {
        attackManuel = attack
        defenseManuel = defense
        healthManuel = hp
        if (!manuelFound && originalAttack == 0) {
            originalAttack = attack
            originalDefense = defense
            originalHealth = hp
        }
        manuelFound = true
    }

    fun applyManuelStats() {
        if (!manuelFound) return
        attackModifier = attackManuel - originalAttack
        defenseModifier = defenseManuel - originalDefense
        healthModifier = originalHealth - healthManuel
    }

    fun hasManuelStats(): Boolean = manuelFound
}
