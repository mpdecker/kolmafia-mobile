package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.banish.Banisher
import net.sourceforge.kolmafia.banish.BanishManager

/**
 * Desktop [ChoiceControl] Adventures Who Live in Ice Houses… choice 836.
 */
object IceHouseChoiceSync {

    const val CHOICE_ID = 836

    private val ICEHOUSE_PATTERN = Regex("""perfectly-preserved (.*?), right""")

    fun applyVisit(
        choiceId: Int,
        html: String,
        banishManager: BanishManager?,
        currentTurn: Int = 0,
    ): Boolean {
        if (choiceId != CHOICE_ID || banishManager == null) return false
        val monster = ICEHOUSE_PATTERN.find(html)?.groupValues?.getOrNull(1)?.trim()
            ?: return false
        banishManager.banishMonster(monster, Banisher.ICE_HOUSE, currentTurn)
        return true
    }

    fun apply(
        choiceId: Int,
        decision: Int,
        banishManager: BanishManager?,
    ): Boolean {
        if (choiceId != CHOICE_ID || banishManager == null) return false
        if (decision != 1) return false
        banishManager.removeBanishByBanisher(Banisher.ICE_HOUSE)
        return true
    }
}
