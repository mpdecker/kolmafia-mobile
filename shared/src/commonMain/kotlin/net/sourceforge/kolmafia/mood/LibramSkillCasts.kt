package net.sourceforge.kolmafia.mood

/** Desktop [net.sourceforge.kolmafia.persistence.SkillDatabase] libram MP math. */
object LibramSkillCasts {

    /** Desktop `libramSkillMPConsumption(cast)` including [manaCostAdjustment] (`getManaCostAdjustment`). */
    fun libramSkillMpCost(cast: Int, manaCostAdjustment: Int = 0): Long =
        maxOf(1L, 1L + cast.toLong() * (cast - 1) / 2 + manaCostAdjustment)

    /** Desktop `libramSkillCasts(libramSummonsPref, availableMp)` with mana-cost adjustment. */
    fun libramSkillCasts(
        libramSummonsPref: Int,
        availableMp: Long,
        manaCostAdjustment: Int = 0,
    ): Int {
        var cast = libramSummonsPref + 1
        var remaining = availableMp
        var count = 0
        while (count < 200) {
            val cost = libramSkillMpCost(cast, manaCostAdjustment)
            if (cost > remaining) break
            count++
            remaining -= cost
            cast++
        }
        return count
    }

    /** Desktop `libramSkillMPConsumption(startCast, count)` with mana-cost adjustment. */
    fun libramSkillMpCostTotal(startCast: Int, count: Int, manaCostAdjustment: Int = 0): Long {
        var cast = startCast
        var total = 0L
        repeat(count) {
            total += libramSkillMpCost(cast, manaCostAdjustment)
            cast++
        }
        return total
    }

    /** Desktop libram rotation in [ManaBurnManager.considerLibramSummon]. Returns skill index + batch size. */
    fun firstLibramBatch(totalCasts: Int, skillCount: Int, nextCastIndex: Int): Pair<Int, Int>? {
        if (skillCount <= 0 || totalCasts <= 0) return null
        for (i in 0 until skillCount) {
            val thisCast = (totalCasts + skillCount - 1 - i) / skillCount
            if (thisCast <= 0) continue
            return (i + nextCastIndex) % skillCount to thisCast
        }
        return null
    }

    /** Desktop [ManaBurnManager.considerLibramSummon] semicolon-separated cast command. */
    fun buildLibramSummonCommand(
        totalCasts: Int,
        castable: List<String>,
        nextCastIndex: Int,
    ): String? {
        val skillCount = castable.size
        if (skillCount <= 0 || totalCasts <= 0) return null
        val buf = StringBuilder()
        for (i in 0 until skillCount) {
            val thisCast = (totalCasts + skillCount - 1 - i) / skillCount
            if (thisCast <= 0) continue
            buf.append("cast ")
            buf.append(thisCast)
            buf.append(' ')
            buf.append(castable[(i + nextCastIndex) % skillCount])
            buf.append(';')
        }
        return buf.toString().takeIf { it.isNotEmpty() }
    }
}
