package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.Gender
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.skill.SkillData

/** Desktop ConcoctionDatabase.isPermittedMethod v1 (method + requirement gating). */
object ConcoctionPermitted {

    private const val TENDERIZING_HAMMER = 338
    private const val GRIMACITE_HAMMER = 3542
    private const val PULVERIZE = 1016
    private const val SAUSAGE_O_MATIC = 10058
    private const val REPLICA_SAUSAGE_O_MATIC = 11241

    private val PERMIT_METHODS = setOf(
        "COMBINE", "ACOMBINE", "COOK", "COOK_FANCY", "MIX", "MIX_FANCY",
        "SMITH", "SSMITH", "STILL", "SUSE", "MUSE", "PHINEAS", "SEWER",
        "STAR", "SUGAR", "PIXEL", "ROLL", "TINKER", "STAFF", "SUSHI",
        "JEWEL", "MALUS", "GNOME_TINKER", "JEWELRY", "MULTI_USE", "SINGLE_USE",
        "ROLLING_PIN", "CLIPART", "SAUSAGE_O_MATIC", "COINMASTER",
    )

    private val REQUIREMENT_TOKENS = setOf(
        "MALE", "FEMALE", "SSPD", "HAMMER", "GRIMACITE", "TORSO", "WEAPON",
        "ARMOR", "ELDRITCH", "EXPENSIVE", "REAGENT", "WAY", "DEEP", "PASTAMASTERY",
        "TRANSNOODLE", "TEMPURAMANCY", "PATENT", "AC", "SHC", "SALACIOUS", "NOBEE",
    )

    private val SKILL_REQUIREMENTS = mapOf(
        "TORSO" to 12,
        "ELDRITCH" to 161,
        "EXPENSIVE" to 20,
        "REAGENT" to 4006,
        "WAY" to 4018,
        "DEEP" to 4021,
        "PASTAMASTERY" to 3006,
        "TRANSNOODLE" to 3018,
        "TEMPURAMANCY" to 3021,
        "PATENT" to 20004,
        "AC" to 5014,
        "SHC" to 5018,
        "SALACIOUS" to 5022,
        "WEAPON" to 1006,
        "ARMOR" to 2006,
    )

    fun isPermittedMethod(
        concoction: ConcoctionData,
        state: CharacterState,
        skills: List<SkillData> = emptyList(),
        kolHoliday: String = KolGameHolidayCalendar.getHoliday(),
        accessibleCount: (Int) -> Int = { 0 },
        prefs: Preferences? = null,
    ): Boolean {
        val method = ConcoctionCreationCost.primaryMethod(concoction.methods) ?: return false
        if (method !in PERMIT_METHODS) return false
        if ("MANUAL" in concoction.methods) return false

        if (method == "COINMASTER") {
            if (prefs?.getBoolean("autoSatisfyWithCoinmasters", false) != true) return false
            val resultId = ItemDatabase.getByName(concoction.result)?.id ?: return false
            return CoinmasterDatabase.containsBuyItem(
                resultId,
                validate = true,
                state = state,
                prefs = prefs,
                accessibleCount = accessibleCount,
            )
        }

        if (!isMethodPermitted(method, state, skills, prefs, accessibleCount)) return false

        if ("NOBEE" in concoction.methods && state.inBeecore) return false

        if ("MALE" in concoction.methods && state.gender != Gender.MALE) return false
        if ("FEMALE" in concoction.methods && state.gender != Gender.FEMALE) return false

        if (method == "SMITH" || method == "SSMITH" || "HAMMER" in concoction.methods) {
            if (accessibleCount(TENDERIZING_HAMMER) <= 0) return false
        }
        if ("GRIMACITE" in concoction.methods && accessibleCount(GRIMACITE_HAMMER) <= 0) {
            return false
        }

        if ("SSPD" in concoction.methods &&
            !kolHoliday.contains("St. Sneaky Pete's Day") &&
            !kolHoliday.contains("Drunksgiving")
        ) {
            return false
        }

        for ((token, skillId) in SKILL_REQUIREMENTS) {
            if (token in concoction.methods && !hasSkill(skills, skillId)) return false
        }

        for (token in concoction.methods) {
            if (token in REQUIREMENT_TOKENS && token !in SKILL_REQUIREMENTS.keys &&
                token !in setOf("MALE", "FEMALE", "NOBEE", "HAMMER", "GRIMACITE", "SSPD")
            ) {
                return false
            }
        }

        return true
    }

    private fun isMethodPermitted(
        method: String,
        state: CharacterState,
        skills: List<SkillData>,
        prefs: Preferences?,
        accessibleCount: (Int) -> Int,
    ): Boolean = when (method) {
        "STILL" -> state.stillsAvailable > 0
        "SUSHI" -> prefs?.getBoolean("hasSushiMat", false) == true
        "MALUS" -> canUseMalus(state, skills, prefs)
        "SAUSAGE_O_MATIC" -> hasSausageOMatic(state, accessibleCount)
        else -> true
    }

    private fun canUseMalus(state: CharacterState, skills: List<SkillData>, prefs: Preferences?): Boolean {
        if (!state.characterClassEnum.isMuscleBased) return false
        if (!hasSkill(skills, PULVERIZE)) return false
        if (state.inNuclearAutumn || state.inPokefam) return false
        return prefs?.getInt("lastGuildStoreOpen", -1) == state.ascensionNumber
    }

    private fun hasSausageOMatic(state: CharacterState, accessibleCount: (Int) -> Int): Boolean {
        if (accessibleCount(SAUSAGE_O_MATIC) > 0) return true
        if (state.inLegacyOfLoathing && accessibleCount(REPLICA_SAUSAGE_O_MATIC) > 0) return true
        return false
    }

    private fun hasSkill(skills: List<SkillData>, skillId: Int): Boolean =
        skills.any { it.id == skillId }
}
