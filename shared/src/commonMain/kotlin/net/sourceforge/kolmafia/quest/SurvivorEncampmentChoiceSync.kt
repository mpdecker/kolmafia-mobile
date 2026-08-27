package net.sourceforge.kolmafia.quest

/** Desktop post-apocalyptic survivor encampment choice 987 donations. */
object SurvivorEncampmentChoiceSync {
    const val CHOICE_ID = 987

    fun apply(choiceId: Int, choiceUrl: String, html: String, consumeItem: (Int, Int) -> Unit): Boolean {
        if (choiceId != CHOICE_ID || !html.contains("accept your donation")) return false
        val itemId = Regex("""(?:[?&])whichfood=(\d+)""").find(choiceUrl)?.groupValues?.get(1)?.toIntOrNull()
            ?: return false
        consumeItem(itemId, if (choiceUrl.contains("giveten")) 10 else 1)
        return true
    }
}
