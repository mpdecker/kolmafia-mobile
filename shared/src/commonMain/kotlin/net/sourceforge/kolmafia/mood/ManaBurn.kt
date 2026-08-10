package net.sourceforge.kolmafia.mood

/** Desktop [net.sourceforge.kolmafia.moods.ManaBurn]. */
class ManaBurn(
    val skillId: Int,
    val skillName: String,
    val effectName: String,
    var duration: Int,
    val limit: Int,
    val mpCost: Int,
    val effectDurationPerCast: Int,
    var count: Int = 0,
) : Comparable<ManaBurn> {

    fun isCastable(allowedMp: Long): Boolean =
        duration < limit && mpCost.toLong() <= allowedMp

    fun simulateCast(): Long {
        count++
        duration += effectDurationPerCast
        return mpCost.toLong()
    }

    override fun compareTo(other: ManaBurn): Int = duration - other.duration

    companion object {
        /** Desktop balanced cast loop in [ManaBurnManager.getNextBurnCast]. */
        fun simulateBalancedCasts(burns: MutableList<ManaBurn>, allowedMp: Long) {
            var remaining = allowedMp
            var index = 0
            while (index < burns.size) {
                val burn = burns[index]
                if (!burn.isCastable(remaining)) {
                    burns.removeAt(index)
                    continue
                }
                remaining -= burn.simulateCast()
                burns.sort()
                index = 0
            }
        }
    }
}
