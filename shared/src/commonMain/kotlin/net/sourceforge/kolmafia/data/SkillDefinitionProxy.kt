package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop SkillDatabase helpers for `$skill[field]` proxy reads. */
object SkillDefinitionProxy {

    private val libramSkillIds = setOf(7219, 7220, 7221, 7222, 7223, 7224, 7225)
    private val SKILL_ID_FROM_URL = Regex("""skillid=(\d+)""")

    fun classSkillBase(characterClassId: Int): Int = characterClassId * 1000

    fun findSkillFromUrl(url: String, characterClassId: Int): Int {
        val relativeId = SKILL_ID_FROM_URL.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return 0
        return classSkillBase(characterClassId) + relativeId
    }

    fun getGuildPurchaseCost(skillId: Int): Int {
        val guildLevel = SkillDefinitionDatabase.getById(skillId)?.guildLevel ?: 0
        return getPurchaseCost(skillId, guildLevel)
    }

    fun getByIdOrName(skillRef: String): SkillDefinition? {
        skillRef.toIntOrNull()?.let { SkillDefinitionDatabase.getById(it) }?.let { return it }
        return SkillDefinitionDatabase.getByName(skillRef)
    }

    fun resolveSkillId(skillRef: String): Int =
        skillRef.toIntOrNull()
            ?: SkillDefinitionDatabase.getByName(skillRef)?.id
            ?: 0

    fun getSkillTypeName(skillId: Int): String {
        val skill = SkillDefinitionDatabase.getById(skillId) ?: return "unknown"
        val tags = skill.tags
        if ("passive" in tags) {
            if ("combat" in tags) return "combat/passive"
            if ("nc" in tags && "heal" in tags) return "noncombat remedy/passive"
            return "passive"
        }
        if ("combat" in tags) {
            if ("nc" in tags && "heal" in tags) return "combat/noncombat remedy"
            return "combat"
        }
        if ("item" in tags) return "summon"
        if ("heal" in tags) return "remedy"
        if ("shanty" in tags) return "shanty"
        if ("walk" in tags) return "walk"
        if ("expression" in tags) return "expression"
        if ("song" in tags) return "song"
        if ("other" in tags) return "buff"
        if ("self" in tags) return "self-only"
        return "unknown"
    }

    fun getSkillCategory(skillId: Int): SkillCategory = SkillCategory.bySkillId(skillId)

    fun getSkillLevel(skillId: Int, preferences: Preferences?): Int {
        if (SkillDefinitionDatabase.getById(skillId) == null) return -1
        return preferences?.getInt("skillLevel$skillId", 0) ?: 0
    }

    fun getPurchaseCost(skillId: Int, level: Int): Int {
        if (skillId !in 1000..6999) return 0
        return when (level) {
            1 -> 125
            2 -> 250
            3 -> 500
            4 -> 750
            5 -> 1250
            6 -> 1750
            7 -> 2500
            8 -> 3250
            9 -> 4000
            10 -> 5000
            11 -> 6250
            12 -> 7500
            13 -> 10000
            14 -> 12500
            15 -> 15000
            else -> 0
        }
    }

    fun isLibram(skillId: Int): Boolean = skillId in libramSkillIds

    fun isPassive(skillId: Int): Boolean {
        val skill = SkillDefinitionDatabase.getById(skillId) ?: return false
        return skill.isPassive || getSkillCategory(skillId) == SkillCategory.VAMPYRE
    }

    fun isBuff(skillId: Int): Boolean = hasTag(skillId, "other")

    /** Desktop [SkillDatabase.isAccordionThiefSong]. */
    fun isAccordionThiefSong(skillId: Int): Boolean =
        skillId in 6001..6999 && isBuff(skillId)

    fun isCombat(skillId: Int): Boolean = hasTag(skillId, "combat")

    fun isSpell(skillId: Int): Boolean = hasTag(skillId, "spell")

    fun isSong(skillId: Int): Boolean = hasTag(skillId, "song")

    fun isExpression(skillId: Int): Boolean = hasTag(skillId, "expression")

    fun isWalk(skillId: Int): Boolean = hasTag(skillId, "walk")

    fun isShanty(skillId: Int): Boolean = hasTag(skillId, "shanty")

    fun isSummon(skillId: Int): Boolean = hasTag(skillId, "item")

    fun isPermable(skillId: Int): Boolean {
        val skill = SkillDefinitionDatabase.getById(skillId) ?: return skillId < 7000
        return skill.isPermable
    }

    private fun hasTag(skillId: Int, tag: String): Boolean =
        SkillDefinitionDatabase.getById(skillId)?.tags?.contains(tag) == true
}
