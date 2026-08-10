package net.sourceforge.kolmafia.skill

import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.data.SkillDefinitionProxy

/** Desktop [net.sourceforge.kolmafia.request.UseSkillRequest.BuffTool] tool tables. */
data class BuffTool(
    val itemId: Int,
    val bonusTurns: Int,
    val ascensionClass: CharacterClass? = null,
) {
    val isClassLimited: Boolean get() = ascensionClass != null
}

object BuffTools {

    val TAMER_TOOLS = arrayOf(
        BuffTool(9439, 25),
        BuffTool(4317, 15),
        BuffTool(2558, 10),
        BuffTool(12069, 6),
        BuffTool(60, 5),
        BuffTool(7025, 2),
        BuffTool(4, 0),
    )

    val SAUCE_TOOLS = arrayOf(
        BuffTool(4319, 15),
        BuffTool(6538, 15),
        BuffTool(7027, 10),
        BuffTool(2560, 10),
        BuffTool(6042, 7),
        BuffTool(57, 5),
        BuffTool(7, 0),
    )

    val THIEF_TOOLS = arrayOf(
        BuffTool(4321, 15, CharacterClass.ACCORDION_THIEF),
        BuffTool(6455, 15, CharacterClass.ACCORDION_THIEF),
        BuffTool(6825, 15, CharacterClass.ACCORDION_THIEF),
        BuffTool(6824, 14, CharacterClass.ACCORDION_THIEF),
        BuffTool(6821, 13, CharacterClass.ACCORDION_THIEF),
        BuffTool(6822, 12, CharacterClass.ACCORDION_THIEF),
        BuffTool(6823, 11, CharacterClass.ACCORDION_THIEF),
        BuffTool(2557, 10, CharacterClass.ACCORDION_THIEF),
        BuffTool(7029, 10, CharacterClass.ACCORDION_THIEF),
        BuffTool(6820, 10, CharacterClass.ACCORDION_THIEF),
        BuffTool(6818, 10, CharacterClass.ACCORDION_THIEF),
        BuffTool(6819, 9, CharacterClass.ACCORDION_THIEF),
        BuffTool(6817, 7, CharacterClass.ACCORDION_THIEF),
        BuffTool(6816, 6, CharacterClass.ACCORDION_THIEF),
        BuffTool(8111, 5),
        BuffTool(6809, 5),
        BuffTool(6815, 5, CharacterClass.ACCORDION_THIEF),
        BuffTool(6814, 5, CharacterClass.ACCORDION_THIEF),
        BuffTool(6856, 5, CharacterClass.ACCORDION_THIEF),
        BuffTool(6857, 5, CharacterClass.ACCORDION_THIEF),
        BuffTool(6858, 5, CharacterClass.ACCORDION_THIEF),
        BuffTool(6859, 5, CharacterClass.ACCORDION_THIEF),
        BuffTool(50, 5, CharacterClass.ACCORDION_THIEF),
        BuffTool(6813, 4, CharacterClass.ACCORDION_THIEF),
        BuffTool(6812, 3, CharacterClass.ACCORDION_THIEF),
        BuffTool(6811, 2, CharacterClass.ACCORDION_THIEF),
        BuffTool(2234, 2, CharacterClass.ACCORDION_THIEF),
        BuffTool(6810, 1, CharacterClass.ACCORDION_THIEF),
        BuffTool(11, 0, CharacterClass.ACCORDION_THIEF),
        BuffTool(10103, 0),
        BuffTool(6808, 0),
    )

    fun toolsForSkill(skillId: Int): Array<BuffTool>? = when {
        SkillDefinitionProxy.isTurtleTamerBuff(skillId) -> TAMER_TOOLS
        SkillDefinitionProxy.isSaucerorBuff(skillId) -> SAUCE_TOOLS
        SkillDefinitionProxy.isAccordionThiefSong(skillId) -> THIEF_TOOLS
        else -> null
    }
}
