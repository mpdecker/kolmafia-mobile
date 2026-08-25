package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.SpelunkyRequest
import net.sourceforge.kolmafia.skill.SkillManager

/**
 * Desktop [KoLCharacter.enterLimitmode] / [resetAfterLimitmode] glue for Spelunky + Bat.
 */
object LimitModeController {

    const val SPELUNKY = "spelunky"
    const val BATMAN = "batman"

    fun enterLimitMode(
        mode: String,
        character: KoLCharacter?,
        preferences: Preferences?,
        inventory: InventoryManager? = null,
        skillManager: SkillManager? = null,
    ) {
        val canonical = when (mode.lowercase()) {
            "spelunky", "spelunk" -> SPELUNKY
            "batman" -> BATMAN
            else -> mode
        }
        character?.updateLimitMode(canonical)
        when (canonical) {
            SPELUNKY -> SpelunkyRequest.reset(preferences, inventory, character)
            BATMAN -> BatManager.begin(preferences, inventory, character, skillManager)
        }
    }

    fun exitLimitMode(
        character: KoLCharacter?,
        preferences: Preferences?,
        inventory: InventoryManager? = null,
        skillManager: SkillManager? = null,
        previousMode: String = character?.state?.value?.limitMode.orEmpty(),
    ) {
        val prev = previousMode.lowercase()
        when {
            prev == SPELUNKY || prev == "spelunk" ->
                SpelunkyRequest.resetItems(preferences, inventory, character)
            prev == BATMAN ->
                BatManager.end(preferences, inventory, character, skillManager)
        }
        character?.updateLimitMode("")
    }
}
