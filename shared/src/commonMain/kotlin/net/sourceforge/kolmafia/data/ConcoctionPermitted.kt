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
        "FLOUNDRY", "BARREL", "VYKEA",
        "GNOME_PART", "BURNING_LEAVES", "WAX", "NEWSPAPER", "METEOROID", "WOOL",
        "TERMINAL", "SPACEGATE", "FANTASY_REALM", "STILLSUIT", "MAYAM", "PHOTO_BOOTH", "TAKERSPACE",
    )

    private val REQUIREMENT_TOKENS = setOf(
        "MALE", "FEMALE", "SSPD", "HAMMER", "GRIMACITE", "TORSO", "WEAPON",
        "ARMOR", "ELDRITCH", "EXPENSIVE", "REAGENT", "WAY", "DEEP", "PASTAMASTERY",
        "TRANSNOODLE", "TEMPURAMANCY", "PATENT", "AC", "SHC", "SALACIOUS", "NOBEE", "TIKI",
    )

    private val SKILL_REQUIREMENTS = mapOf(
        "TORSO" to 12,
        "TIKI" to 186,
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
        familiarUsable: (Int) -> Boolean = { false },
        limitMode: String = "none",
    ): Boolean {
        val methods = ConcoctionMethodAliases.normalize(concoction.methods)
        if (methods.any { it in ConcoctionMethodAliases.LEGACY_BLOCKED }) return false
        if ("MANUAL" in methods) return false

        val method = ConcoctionCreationCost.primaryMethod(concoction.methods) ?: return false
        if (method !in PERMIT_METHODS) return false

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

        if (!isMethodPermitted(method, state, skills, prefs, accessibleCount, familiarUsable, limitMode)) {
            return false
        }

        if ("NOBEE" in methods && state.inBeecore) return false

        if ("MALE" in methods && state.gender != Gender.MALE) return false
        if ("FEMALE" in methods && state.gender != Gender.FEMALE) return false

        if (method == "SMITH" || method == "SSMITH" || "HAMMER" in methods) {
            if (!SmithingGates.isSmithPermitted(state, prefs, accessibleCount, limitMode)) {
                return false
            }
        }
        if ("GRIMACITE" in methods && accessibleCount(GRIMACITE_HAMMER) <= 0) {
            return false
        }

        if ("SSPD" in methods &&
            !kolHoliday.contains("St. Sneaky Pete's Day") &&
            !kolHoliday.contains("Drunksgiving")
        ) {
            return false
        }

        for ((token, skillId) in SKILL_REQUIREMENTS) {
            if (token !in methods) continue
            when (token) {
                "TORSO" -> if (!TorsoAwareness.hasTorsoAwareness(skills)) return false
                else -> if (!hasSkill(skills, skillId)) return false
            }
        }

        for (token in methods) {
            if (token in REQUIREMENT_TOKENS && token !in SKILL_REQUIREMENTS.keys &&
                token !in setOf("MALE", "FEMALE", "NOBEE", "HAMMER", "GRIMACITE", "SSPD", "TIKI")
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
        familiarUsable: (Int) -> Boolean,
        limitMode: String,
    ): Boolean = when (method) {
        "STILL" -> state.stillsAvailable > 0
        "SUSHI" -> prefs?.getBoolean("hasSushiMat", false) == true
        "MALUS" -> canUseMalus(state, skills, prefs)
        "SAUSAGE_O_MATIC" -> hasSausageOMatic(state, accessibleCount)
        "STAFF", "PHINEAS",
        "COOK", "MIX", "COOK_FANCY", "MIX_FANCY",
        "SMITH", "SSMITH", "CLIPART", "JEWEL", "JEWELRY",
        "ROLL", "ROLLING_PIN", "SEWER", "MUSE", "SUSE", "MULTI_USE", "SINGLE_USE",
        "FLOUNDRY", "BARREL", "GNOME_TINKER", "VYKEA",
        "GNOME_PART", "BURNING_LEAVES", "WAX", "NEWSPAPER", "METEOROID", "WOOL",
        "TERMINAL", "SPACEGATE", "FANTASY_REALM", "STILLSUIT", "MAYAM", "PHOTO_BOOTH", "TAKERSPACE",
        ->
            ConcoctionMethodGates.isPermitted(
                method,
                state,
                prefs,
                accessibleCount,
                familiarUsable,
                skills,
                limitMode,
            )
        "COMBINE", "ACOMBINE" -> true
        "STAR", "SUGAR", "PIXEL", "TINKER" -> false
        else -> false
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
