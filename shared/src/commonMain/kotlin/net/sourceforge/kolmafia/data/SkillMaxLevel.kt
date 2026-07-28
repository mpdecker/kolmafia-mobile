package net.sourceforge.kolmafia.data

/** Subset of desktop [SkillDatabase.getMaxLevel] for bundled leveled passive skills. */
object SkillMaxLevel {

    fun getMaxLevel(skillId: Int): Int = when (skillId) {
        46, 47, 48 -> 10 // Slimy Sinews / Synapses / Shoulders
        227 -> 11 // Chitinous Soul
        else -> 0
    }
}
