package net.sourceforge.kolmafia.session

/**
 * Desktop [FightRequest.currentRAM] / [FightRequest.getCurrentRAM] —
 * cyber-realm fight RAM remaining.
 */
object FightRamTracker {
    var currentRAM: Int = 0
        private set

    fun calculateInitialRAM(ramModifier: Int): Int = 3 + ramModifier

    fun onFightStart(ramModifier: Int) {
        currentRAM = calculateInitialRAM(ramModifier)
    }

    fun onFightEnd() {
        currentRAM = 0
    }

    /** Desktop getCurrentRAM — initial when not in combat. */
    fun getCurrent(currentRound: Int, ramModifier: Int): Int =
        if (currentRound == 0) calculateInitialRAM(ramModifier) else currentRAM

    fun applySkillCost(cost: Int) {
        if (cost <= 0) return
        currentRAM = (currentRAM - cost).coerceAtLeast(0)
    }

    /**
     * Desktop cyber skill RAM costs (FightRequest payActionCost subset).
     * Returns 0 for non-cyber / unknown skills.
     */
    fun cyberSkillCost(skillId: Int): Int = when (skillId) {
        // 0-cost cyber skills
        7542, 7547, 7548 -> 0
        // 1 RAM
        7544, 7546 -> 1
        // 2 RAM
        7545 -> 2
        // 3 RAM
        7543 -> 3
        // 7 RAM (geofencing rapier)
        7554 -> 7
        else -> 0
    }

    fun reset() {
        currentRAM = 0
    }
}
