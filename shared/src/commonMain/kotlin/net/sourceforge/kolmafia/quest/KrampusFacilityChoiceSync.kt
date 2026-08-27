package net.sourceforge.kolmafia.quest

/** Desktop K.R.A.M.P.U.S. facility choice 810 costs. */
object KrampusFacilityChoiceSync {
    const val CHOICE_ID = 810
    const val WARBEAR_WHOSIT = 6913

    fun apply(choiceId: Int, decision: Int, choiceUrl: String, html: String, consumeItem: (Int, Int) -> Unit): Boolean {
        if (choiceId != CHOICE_ID) return false
        if (decision == 2) consumeItem(WARBEAR_WHOSIT, 100)
        if (decision == 4 && html.contains("You upgrade the robot!")) {
            val slot = Regex("""(?:[?&])slot=(\d+)""").find(choiceUrl)?.groupValues?.get(1)
            val bot = slot?.let {
                Regex("""<td.*?<img alt=['"]([^'"]*)['"].*?</td>""", setOf(RegexOption.DOT_MATCHES_ALL))
                    .findAll(html).firstOrNull { match -> match.value.contains("slot=$it") }?.groupValues?.get(1)
            }
            val cost = when {
                bot?.contains("Level 2") == true -> 250
                bot?.contains("Level 3") == true -> 500
                else -> 0
            }
            if (cost > 0) consumeItem(WARBEAR_WHOSIT, cost)
        }
        return true
    }
}
