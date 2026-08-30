package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger
import net.sourceforge.kolmafia.session.YouRobotManager
import net.sourceforge.kolmafia.skill.SkillManager

/**
 * Pref/HTTP sync slice of desktop [YouRobotManager] for choices 1445 / 1447.
 * Delegates install accounting and combat-skill updates to [YouRobotManager].
 */
object YouRobotChoiceSync {

    const val REASSEMBLY_CHOICE = YouRobotManager.REASSEMBLY_CHOICE
    const val STATBOT_CHOICE = YouRobotManager.STATBOT_CHOICE

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        choiceUrl: String = "",
        skillManager: SkillManager? = null,
    ): Boolean = YouRobotManager.visitChoice(choiceId, html, preferences, choiceUrl, skillManager)

    fun apply(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        choiceUrl: String = "",
        character: KoLCharacter? = null,
        skillManager: SkillManager? = null,
        decision: Int = 0,
    ): Boolean = YouRobotManager.postChoice(
        choiceId = choiceId,
        html = html,
        preferences = preferences,
        choiceUrl = choiceUrl,
        character = character,
        skillManager = skillManager,
        decision = decision,
    )

    fun registerRequest(
        urlString: String,
        sessionLogger: SessionLogger?,
        preferences: Preferences? = null,
    ): Boolean = YouRobotManager.registerRequest(urlString, sessionLogger, preferences)
}
