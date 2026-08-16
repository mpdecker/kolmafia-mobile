package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.SkillDefinitionProxy

/**
 * Desktop ShowDataCommand `skills [filter]` against owned skills.
 * Type prefixes consume the remainder of the filter (desktop `startsWith` then `filter = ""`).
 */
internal fun GameRuntimeLibrary.cliSkills(filter: String, rt: AshRuntimeContext) {
    runBlocking { skillManager?.fetchSkills() }
    val owned = skillManager?.state?.value?.skills.orEmpty()
    val (typeMatches, leftover) = skillsTypeFilter(filter.trim().lowercase())
    for (skill in owned) {
        if (typeMatches != null && !typeMatches(skill.id)) continue
        if (leftover.isNotEmpty() && !skill.name.contains(leftover, ignoreCase = true)) continue
        rt.print(skill.name)
    }
}

internal fun skillsTypeFilter(filter: String): Pair<((Int) -> Boolean)?, String> {
    if (filter.startsWith("cast")) return SkillDefinitionProxy::isNonCombat to ""
    if (filter.startsWith("pass")) return SkillDefinitionProxy::isPassive to ""
    if (filter.startsWith("self")) return SkillDefinitionProxy::isSelf to ""
    if (filter.startsWith("buff")) return SkillDefinitionProxy::isBuff to ""
    if (filter.startsWith("combat")) return SkillDefinitionProxy::isCombat to ""
    if (filter.startsWith("song")) return SkillDefinitionProxy::isSong to ""
    if (filter.startsWith("expression")) return SkillDefinitionProxy::isExpression to ""
    if (filter.startsWith("walk")) return SkillDefinitionProxy::isWalk to ""
    if (filter.startsWith("shanty")) return SkillDefinitionProxy::isShanty to ""
    return null to filter
}
