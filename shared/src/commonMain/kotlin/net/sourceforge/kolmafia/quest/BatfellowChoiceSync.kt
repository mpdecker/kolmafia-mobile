package net.sourceforge.kolmafia.quest

/**
 * Desktop [ChoiceControl] Batfellow begin/end limit-mode choices 1133/1134/1168.
 */
object BatfellowChoiceSync {

    const val BEGINS = 1133
    const val ENDS = 1134
    const val ENDS_TIMEOUT = 1168
    const val BATMAN = "batman"

    fun apply(
        choiceId: Int,
        decision: Int,
        setLimitMode: (String) -> Unit = {},
    ): Boolean {
        return when (choiceId) {
            BEGINS -> {
                if (decision != 1) return false
                setLimitMode(BATMAN)
                true
            }
            ENDS -> {
                if (decision != 1) return false
                setLimitMode("")
                true
            }
            ENDS_TIMEOUT -> {
                setLimitMode("")
                true
            }
            else -> false
        }
    }
}
