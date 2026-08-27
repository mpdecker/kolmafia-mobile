package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.BatManager
import net.sourceforge.kolmafia.session.LimitModeController
import net.sourceforge.kolmafia.skill.SkillManager

/**
 * Desktop [ChoiceControl] Batfellow begin/end limit-mode choices 1133/1134/1168.
 * Delegates to [BatManager.begin]/[BatManager.end] (Phases 1341+).
 */
object BatfellowChoiceSync {

    const val BEGINS = 1133
    const val ENDS = 1134
    const val ENDS_TIMEOUT = 1168
    const val SEDAN = 1135
    const val BATMAN = "batman"

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String = "",
        preferences: Preferences? = null,
        character: KoLCharacter? = null,
        inventory: InventoryManager? = null,
        skillManager: SkillManager? = null,
        setLimitMode: (String) -> Unit = {},
    ): Boolean {
        return when (choiceId) {
            BEGINS -> {
                if (decision != 1) return false
                LimitModeController.enterLimitMode(
                    LimitModeController.BATMAN,
                    character,
                    preferences,
                    inventory,
                    skillManager,
                )
                setLimitMode(BATMAN)
                true
            }
            ENDS -> {
                if (decision != 1) return false
                LimitModeController.exitLimitMode(
                    character,
                    preferences,
                    inventory,
                    skillManager,
                    previousMode = BATMAN,
                )
                setLimitMode("")
                true
            }
            ENDS_TIMEOUT -> {
                LimitModeController.exitLimitMode(
                    character,
                    preferences,
                    inventory,
                    skillManager,
                    previousMode = BATMAN,
                )
                setLimitMode("")
                true
            }
            SEDAN -> BatManager.parseBatSedan(html, decision, preferences)
            else -> false
        }
    }
}
