package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionProxy
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillLearner
import net.sourceforge.kolmafia.skill.SkillManager

/** Desktop [net.sourceforge.kolmafia.request.GuildRequest.parseResponse] buyskill branch. */
object GuildSkillSync {
    private val ACTION_PATTERN = Regex("""action=([^&]+)""")

    fun parseBuyskill(
        url: String,
        html: String,
        character: KoLCharacter?,
        preferences: Preferences?,
        skillManager: SkillManager?,
        inventoryManager: InventoryManager?,
    ) {
        if (actionFromUrl(url) != "buyskill") return
        if (!html.contains("You learn a new skill")) return
        if (preferences == null) return

        val char = character ?: return
        val characterClassId = char.state.value.characterClass
        val skillId = SkillDefinitionProxy.findSkillFromUrl(url, characterClassId)
        if (skillId <= 0) return

        val cost = SkillDefinitionProxy.getGuildPurchaseCost(skillId)
        if (cost > 0) {
            val newMeat = (char.state.value.meat - cost).coerceAtLeast(0)
            char.updateMeat(newMeat)
        }

        SkillLearner.learnSkill(skillId, preferences, skillManager, inventoryManager)
        ConcoctionDatabase.refreshConcoctionsNowFromLastContext()
    }

    private fun actionFromUrl(url: String): String? =
        ACTION_PATTERN.find(url)?.groupValues?.getOrNull(1)?.lowercase()
}
