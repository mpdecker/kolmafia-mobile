package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Chatting with your Burly Bodyguard choice 1532.
 */
object BodyguardChoiceSync {

    const val CHOICE_ID = 1532

    private val BGID_FIELD = Regex("""(?:^|[?&])bgid=(\d+)""", RegexOption.IGNORE_CASE)

    fun apply(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        choiceUrl: String = "",
        monsterNameForId: (Int) -> String? = { id -> MonsterDatabase.getById(id)?.name },
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (!html.contains("You set off to find a monster with a specific bodyguard to challenge.")) {
            return false
        }
        val bgid = BGID_FIELD.find(choiceUrl)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return false
        val monsterName = monsterNameForId(bgid) ?: return false
        preferences.setInt("bodyguardCharge", 0)
        preferences.setString("bodyguardChatMonster", monsterName)
        return true
    }
}
