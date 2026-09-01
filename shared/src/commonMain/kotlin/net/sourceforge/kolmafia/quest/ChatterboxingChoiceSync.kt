package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.banish.Banisher

/** Desktop ChoiceControl case 191 — Chatterboxing banish on chatty pirate. */
object ChatterboxingChoiceSync {

    const val CHOICE_ID = 191
    private const val TRINKET_TEXT = "find a valuable trinket that looks promising"

    fun apply(
        choiceId: Int,
        decision: Int,
        responseText: String,
        banishManager: BanishManager?,
        currentTurn: Int = 0,
    ): Boolean {
        if (choiceId != CHOICE_ID || banishManager == null || decision != 2) return false
        if (!responseText.contains(TRINKET_TEXT, ignoreCase = true)) return false
        banishManager.banishMonster("chatty pirate", Banisher.CHATTERBOXING, currentTurn)
        return true
    }
}
