package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.handlePalindomeChange] visit hooks + `palindomeDudesDefeated` combat
 * counter + Dr. Awkward finish.
 */
object PalindomeSync {

    const val PALINDOME_ADVENTURE = 386
    const val PALINDROME_BOOK_2 = 7270

    private val PALINDOME_DUDES = setOf(
        "drab bard",
        "bob racecar",
        "racecar bob",
    )

    data class PalindomeVisitContext(
        val consumeItem: (Int, Int) -> Unit = { _, _ -> },
    )

    fun parseSnarfblat(url: String?): Int? =
        url?.let { Regex("""snarfblat=(\d+)""", RegexOption.IGNORE_CASE).find(it)?.groupValues?.getOrNull(1)?.toIntOrNull() }

    fun applyFromVisit(
        url: String?,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        context: PalindomeVisitContext = PalindomeVisitContext(),
    ): Boolean {
        if (questDatabase == null || preferences == null) return false
        val location = url.orEmpty()
        var changed = false
        if (parseSnarfblat(url) == PALINDOME_ADVENTURE) {
            if (!html.contains("That place isn't accessible to you right now.") &&
                !html.contains("You find yourself unable to get near the Palindome.")
            ) {
                questDatabase.setQuestIfBetter(Quest.PALINDOME, QuestDatabase.STARTED)
                changed = true
            }
        }
        if (location.contains("whichplace=palindome", ignoreCase = true) ||
            location.contains("action=pal_mr", ignoreCase = true)
        ) {
            if (location.contains("action=pal_mr", ignoreCase = true) &&
                html.contains("in the mood for a bowl of wet stunt nut stew")
            ) {
                questDatabase.setProgress(Quest.PALINDOME, "step3")
                context.consumeItem(PALINDROME_BOOK_2, 1)
                changed = true
            }
        }
        return changed
    }

    fun applyCombatWin(
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        monster: String,
        won: Boolean,
    ): Boolean {
        if (questDatabase == null || preferences == null || !won || monster.isBlank()) return false
        val name = monster.trim().lowercase()
        if (name == "dr. awkward" || name == "dr awkward") {
            questDatabase.setProgress(Quest.PALINDOME, QuestDatabase.FINISHED)
            return true
        }
        if (name !in PALINDOME_DUDES) return false
        if (!questDatabase.isQuestStep(Quest.PALINDOME, QuestDatabase.STARTED)) return false
        val current = preferences.getInt("palindomeDudesDefeated", 0)
        if (current >= 20) return false
        preferences.setInt("palindomeDudesDefeated", current + 1)
        return true
    }
}
