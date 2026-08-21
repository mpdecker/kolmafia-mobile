package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop ChoiceControl residual handlers in the 1229–1233 range. Choice 1230/1233 have no mutation. */
object SpacegateLeftoversChoiceSync {
    val CHOICE_IDS = 1229..1233
    const val GUMMY_MEMORY = 9345

    fun applyVisit(choiceId: Int, html: String, sessionLog: (String) -> Unit): Boolean {
        if (choiceId != 1229) return choiceId in CHOICE_IDS
        Regex("""a sign above it that says <b>(.*?)</b>""").find(html)?.groupValues?.get(1)?.let {
            sessionLog("L.O.V. Exit word: $it")
        }
        return true
    }

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
        sessionLog: (String) -> Unit = {},
        resetAfterAvatar: (String) -> Unit = {},
    ): Boolean {
        if (choiceId !in CHOICE_IDS) return false
        when (choiceId) {
            1229 -> Regex("""you scrawl <b>(.*?)</b>""").find(html)?.groupValues?.get(1)?.let {
                sessionLog("Your log entry: $it")
            }
            1231 -> if (decision == 1 && preferences != null) {
                consumeItem(GUMMY_MEMORY, 1)
                preferences.setInt("noobDeferredPoints", preferences.getInt("noobDeferredPoints", 0) + 5)
            }
            1232 -> resetAfterAvatar(
                listOf("Unknown", "Seal Clubber", "Turtle Tamer", "Pastamancer", "Sauceror", "Disco Bandit", "Accordion Thief")
                    .getOrElse(decision) { "Unknown" },
            )
        }
        return true
    }
}
