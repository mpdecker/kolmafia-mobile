package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.DailyLimitDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionProxy
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillManager

/**
 * Resolves `$skill[field]` bracket access. Mirrors desktop SkillProxy metadata,
 * tag booleans, and runtime cast limits.
 */
internal object SkillEntityFields {

    fun resolve(
        skillRef: String,
        fieldName: String,
        gameDatabase: GameDatabase?,
        skillManager: SkillManager?,
        preferences: Preferences?,
    ): AshValue {
        val skill = SkillDefinitionProxy.getByIdOrName(skillRef)
            ?: gameDatabase?.skill(skillRef)
        val skillId = skill?.id ?: SkillDefinitionProxy.resolveSkillId(skillRef)
        val skillName = skill?.name ?: skillRef
        val level = SkillDefinitionProxy.getSkillLevel(skillId, preferences)

        return when (fieldName.lowercase()) {
            "id" -> AshValue.of(skillId.toLong())
            "name" -> AshValue.of(skillName)
            "type" -> AshValue.of(SkillDefinitionProxy.getSkillTypeName(skillId))
            "level" -> AshValue.of(level.toLong())
            "image" -> AshValue.of(skill?.image ?: "")
            "traincost" -> AshValue.of(
                SkillDefinitionProxy.getPurchaseCost(skillId, level).toLong(),
            )
            "class" -> AshValue(
                AshType.CLASS,
                SkillDefinitionProxy.getSkillCategory(skillId).displayName,
            )
            "libram" -> AshValue.of(SkillDefinitionProxy.isLibram(skillId))
            "passive" -> AshValue.of(SkillDefinitionProxy.isPassive(skillId))
            "buff" -> AshValue.of(SkillDefinitionProxy.isBuff(skillId))
            "combat" -> AshValue.of(SkillDefinitionProxy.isCombat(skillId))
            "spell" -> AshValue.of(SkillDefinitionProxy.isSpell(skillId))
            "song" -> AshValue.of(SkillDefinitionProxy.isSong(skillId))
            "expression" -> AshValue.of(SkillDefinitionProxy.isExpression(skillId))
            "walk" -> AshValue.of(SkillDefinitionProxy.isWalk(skillId))
            "shanty" -> AshValue.of(SkillDefinitionProxy.isShanty(skillId))
            "summon" -> AshValue.of(SkillDefinitionProxy.isSummon(skillId))
            "permable" -> AshValue.of(SkillDefinitionProxy.isPermable(skillId))
            "dailylimit" -> AshValue.of(runtimeDailyLimit(skillId, skillName, skillManager).toLong())
            "dailylimitpref" -> AshValue.of(DailyLimitDatabase.getCastPrefForSkill(skillId))
            "timescast" -> AshValue.of(
                runtimeTimesCast(skillId, skillName, skillManager).toLong(),
            )
            else -> throw ScriptException("skill has no field '$fieldName'")
        }
    }

    private fun runtimeSkill(skillId: Int, skillName: String, skillManager: SkillManager?) =
        skillManager?.state?.value?.skills?.find { owned ->
            owned.id == skillId || owned.name.equals(skillName, ignoreCase = true)
        }

    private fun runtimeDailyLimit(skillId: Int, skillName: String, skillManager: SkillManager?): Int {
        val owned = runtimeSkill(skillId, skillName, skillManager) ?: return -1
        return if (owned.dailyLimit == 0) -1 else owned.dailyLimit
    }

    private fun runtimeTimesCast(skillId: Int, skillName: String, skillManager: SkillManager?): Int =
        runtimeSkill(skillId, skillName, skillManager)?.timesCast ?: 0
}
