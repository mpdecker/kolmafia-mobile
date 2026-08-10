package net.sourceforge.kolmafia.mood

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.ConsumptionEligibility
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillState

/** Desktop [net.sourceforge.kolmafia.request.UseSkillRequest] breakfast/libram skill lists. */
object BreakfastBurnSkills {

    val breakfastSkills = arrayOf(
        "Advanced Cocktailcrafting",
        "Advanced Saucecrafting",
        "Canticle of Carboloading",
        "Pastamastery",
        "Summon Crimbo Candy",
        "Lunch Break",
        "Spaghetti Breakfast",
        "Grab a Cold One",
        "Summon Holiday Fun!",
        "Summon Carrot",
        "Summon Kokomo Resort Pass",
        "Generate Irony",
        "Perfect Freeze",
        "Acquire Rhinestones",
        "Prevent Scurvy and Sobriety",
        "Bowl Full of Jelly",
        "Eye and a Twist",
        "Chubby and Plump",
    )

    val libramSkills = arrayOf(
        "Summon Candy Heart",
        "Summon Party Favor",
        "Summon Love Song",
        "Summon BRICKOs",
        "Summon Dice",
        "Summon Resolutions",
        "Summon Taffy",
    )

    fun maximumCastRemaining(skill: SkillData): Long =
        if (skill.dailyLimit == 0) Long.MAX_VALUE
        else (skill.dailyLimit - skill.timesCast).toLong().coerceAtLeast(0)

    fun findSkill(skillState: SkillState, name: String): SkillData? =
        skillState.skills.firstOrNull { it.name.equals(name, ignoreCase = true) }

    fun canCastBreakfastSkill(name: String, charState: CharacterState, skills: List<SkillData>): Boolean =
        when (name) {
            "Pastamastery" -> ConsumptionEligibility.canEat(charState, skills)
            "Advanced Cocktailcrafting" -> ConsumptionEligibility.canDrink(charState, skills)
            else -> true
        }

    /** Desktop [BreakfastManager.getBreakfastLibramSkills]. */
    fun getBreakfastLibramSkills(
        prefs: Preferences,
        skillState: SkillState,
        charState: CharacterState,
    ): List<String> {
        val prefName = prefs.getString(Preferences.libramSkillsPrefKey(charState.isHardcore), "none")
        if (prefName.equals("none", ignoreCase = true) || prefName.isBlank()) return emptyList()

        val normalized = if (prefName.equals("Summon Candy Hearts", ignoreCase = true)) {
            "Summon Candy Heart"
        } else {
            prefName
        }

        val castAll = normalized.equals("all", ignoreCase = true)
        return libramSkills.filter { skillName ->
            (castAll || skillName.equals(normalized, ignoreCase = true)) &&
                findSkill(skillState, skillName) != null
        }
    }
}
