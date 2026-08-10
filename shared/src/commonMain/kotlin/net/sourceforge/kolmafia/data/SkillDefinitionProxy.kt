package net.sourceforge.kolmafia.data

import kotlin.math.abs
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.TurtleBlessingLevel
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.BuffToolDuration
import net.sourceforge.kolmafia.skill.SkillState

/** Desktop SkillDatabase helpers for `$skill[field]` proxy reads. */
object SkillDefinitionProxy {

    private const val GOOD_SINGING_VOICE = 11016
    private const val SPIRIT_BOON = 2039
    private const val WAR_BLESSING = 2030
    private const val SHE_WHO_WAS_BLESSING = 2033
    private const val STORM_BLESSING = 2037
    private const val REV_ENGINE = 15011
    private const val BIKER_SWAGGER = 15019

    private val PASTA_BIND_SKILL_IDS = setOf(3027, 3029, 3031, 3033, 3035, 3037, 3039)

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

    /** Desktop [SkillDatabase.isTurtleTamerBuff]. */
    fun isTurtleTamerBuff(skillId: Int): Boolean =
        skillId in 2001..2999 && isBuff(skillId)

    /** Desktop [SkillDatabase.isSaucerorBuff]. */
    fun isSaucerorBuff(skillId: Int): Boolean =
        skillId in 4001..4999 && isBuff(skillId)

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

    /** Desktop [net.sourceforge.kolmafia.persistence.SkillDatabase.getEffectDuration] v3. */
    fun getEffectDuration(
        skillId: Int,
        skillState: SkillState,
        charState: CharacterState,
        effectState: EffectState,
        accessibleCount: (Int) -> Int = { 0 },
        gameDatabase: GameDatabase? = null,
    ): Int {
        var duration = SkillDefinitionDatabase.getById(skillId)?.duration ?: 0
        if (isSong(skillId) && skillState.skills.any { it.id == GOOD_SINGING_VOICE }) {
            duration *= 2
        }

        if (!isBuff(skillId)) {
            when (skillId) {
                SPIRIT_BOON ->
                    return TurtleBlessingLevel.fromActiveEffects(effectState).boonDuration()
                WAR_BLESSING, SHE_WHO_WAS_BLESSING, STORM_BLESSING ->
                    if (charState.characterClassEnum != CharacterClass.TURTLE_TAMER) {
                        return 10
                    }
                in PASTA_BIND_SKILL_IDS ->
                    return if (charState.characterClassEnum == CharacterClass.PASTAMANCER) 0 else 10
                REV_ENGINE -> return maxOf(abs(charState.audience), 5)
                BIKER_SWAGGER -> return maxOf(abs(charState.audience), 10)
            }
            return duration
        }

        return BuffToolDuration.resolveBuffDuration(
            duration, skillId, charState, accessibleCount, gameDatabase,
        )
    }

    private fun hasTag(skillId: Int, tag: String): Boolean =
        SkillDefinitionDatabase.getById(skillId)?.tags?.contains(tag) == true
}
