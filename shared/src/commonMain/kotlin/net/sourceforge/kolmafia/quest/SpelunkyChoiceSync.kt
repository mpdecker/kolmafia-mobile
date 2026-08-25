package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.SpelunkyRequest
import net.sourceforge.kolmafia.session.LimitModeController
import net.sourceforge.kolmafia.skill.SkillManager

/**
 * Desktop ChoiceControl Spelunky enter/exit + NC/shop/perk choices 993 / 1027–1045.
 */
object SpelunkyChoiceSync {

    const val ENTER = SpelunkyRequest.ENTER_CHOICE
    const val EXIT = SpelunkyRequest.EXIT_CHOICE
    val CHOICE_IDS = (1027..1045).toSet() + setOf(ENTER)

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
            ENTER -> {
                if (decision != 1) return false
                LimitModeController.enterLimitMode(
                    LimitModeController.SPELUNKY,
                    character,
                    preferences,
                    inventory,
                    skillManager,
                )
                setLimitMode(LimitModeController.SPELUNKY)
                true
            }
            EXIT -> {
                LimitModeController.exitLimitMode(
                    character,
                    preferences,
                    inventory,
                    skillManager,
                    previousMode = LimitModeController.SPELUNKY,
                )
                setLimitMode("")
                true
            }
            in 1028..1045 -> {
                SpelunkyRequest.parseChoice(choiceId, html, decision, preferences)
            }
            else -> false
        }
    }
}
