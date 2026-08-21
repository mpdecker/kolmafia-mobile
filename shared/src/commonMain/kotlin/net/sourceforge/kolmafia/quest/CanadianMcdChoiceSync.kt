package net.sourceforge.kolmafia.quest

/**
 * Desktop [MindControlRequest] / ChoiceControl choice 769 —
 * Super-Secret Canadian Mind-Control Device dial sync from choice URL.
 */
object CanadianMcdChoiceSync {

    const val CHOICE_ID = 769

    private val SETTING_PATTERN = Regex("""setting=(\d+)""")

    fun apply(
        choiceId: Int,
        decision: Int,
        choiceUrl: String,
        html: String,
        setMindControlLevel: (Int) -> Unit,
    ): Boolean {
        if (choiceId != CHOICE_ID || decision != 1) return false
        val level = SETTING_PATTERN.find(choiceUrl)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return false
        if (!html.contains("switch the dial", ignoreCase = true) &&
            !html.contains("the radio", ignoreCase = true)
        ) {
            return false
        }
        setMindControlLevel(level)
        return true
    }
}
