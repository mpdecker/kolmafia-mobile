package net.sourceforge.kolmafia.maximizer

/** Desktop Maximizer verboseMaximizer bracket suffixes (Phase 387). */
object MaximizerBoostVerboseSuffix {

    data class BracketInfo(
        val duration: Int = 0,
        val usesRemaining: Int = Int.MAX_VALUE,
        val itemsRemaining: Int = 0,
        val itemsCreatable: Int = 0,
        val meatCost: Long = 0L,
    )

    fun appendVerboseBrackets(
        text: String,
        info: BracketInfo,
        verboseMaximizer: Boolean,
    ): String {
        if (!verboseMaximizer) return text
        var result = text
        if (info.meatCost > 0) {
            result += " [${info.meatCost} meat]"
        }
        val show = info.duration > 0 ||
            (info.usesRemaining in 1 until Int.MAX_VALUE) ||
            info.itemsRemaining > 0 ||
            info.itemsCreatable > 0
        if (!show) return result
        result += " ["
        var count = 0
        if (info.duration > 0) {
            result += when (info.duration) {
                999 -> "intrinsic"
                1 -> "1 adv duration"
                else -> "${info.duration} advs duration"
            }
            count++
        }
        if (info.usesRemaining in 1 until Int.MAX_VALUE) {
            if (count > 0) result += ", "
            result += if (info.usesRemaining == 1) {
                "1 use remaining"
            } else {
                "${info.usesRemaining} uses remaining"
            }
            count++
        }
        if (info.itemsRemaining > 0) {
            if (count > 0) result += ", "
            result += "${info.itemsRemaining} in inventory"
            count++
        }
        if (info.itemsCreatable > 0) {
            if (count > 0) result += ", "
            result += "${info.itemsCreatable} creatable"
            count++
        }
        result += "]"
        return result
    }
}
