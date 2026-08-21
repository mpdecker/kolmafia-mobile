package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop WLF Bunker choice 1093 visit and redemption synchronization. */
object WlfBunkerChoiceSync {
    const val CHOICE_ID = 1093
    private val formPattern = Regex(
        """<form action=choice\.php>(.*?)</form>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val descItemPattern = Regex("""descitem\(([^)]+)\)""", RegexOption.IGNORE_CASE)
    private val optionPattern = Regex("""name=["']?option["']?\s+value=["']?(\d+)""", RegexOption.IGNORE_CASE)
    private val countPattern = Regex("""\((\d+)\)\s*<""")

    fun applyVisit(choiceId: Int, html: String, preferences: Preferences?, itemIdFromDesc: (String) -> Int?): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (html.contains("the speaker is silent")) {
            clear(preferences, redeemed = true)
            return true
        }
        preferences.setBoolean("_volcanoItemRedeemed", false)
        for (form in formPattern.findAll(html)) {
            val body = form.groupValues[1]
            val desc = descItemPattern.find(body)?.groupValues?.get(1) ?: continue
            val itemId = itemIdFromDesc(desc) ?: continue
            val index = optionPattern.find(body)?.groupValues?.get(1) ?: continue
            val count = countPattern.find(body)?.groupValues?.get(1)?.toIntOrNull() ?: 1
            preferences.setInt("_volcanoItem$index", itemId)
            preferences.setInt("_volcanoItemCount$index", count)
        }
        return true
    }

    fun apply(choiceId: Int, decision: Int, html: String, preferences: Preferences?, consumeItem: (Int, Int) -> Unit): Boolean {
        if (choiceId != CHOICE_ID || preferences == null || !html.contains("hands you a coin")) return false
        val itemId = preferences.getInt("_volcanoItem$decision", 0)
        val count = preferences.getInt("_volcanoItemCount$decision", 0)
        if (itemId > 0 && count > 0) consumeItem(itemId, count)
        clear(preferences, redeemed = true)
        return true
    }

    private fun clear(preferences: Preferences, redeemed: Boolean) {
        preferences.setBoolean("_volcanoItemRedeemed", redeemed)
        for (index in 1..3) {
            preferences.setInt("_volcanoItem$index", 0)
            preferences.setInt("_volcanoItemCount$index", 0)
        }
    }
}
