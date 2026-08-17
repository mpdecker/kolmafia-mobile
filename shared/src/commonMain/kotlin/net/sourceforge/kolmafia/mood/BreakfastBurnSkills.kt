package net.sourceforge.kolmafia.mood

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.ConsumptionEligibility
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillState

/** Desktop [net.sourceforge.kolmafia.request.UseSkillRequest] breakfast/libram skill lists. */
object BreakfastBurnSkills {

    val breakfastAlwaysSkills = arrayOf(
        "Summon Annoyance",
        "Communism!",
    )

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

    val tomeSkills = arrayOf(
        "Summon Snowcones",
        "Summon Stickers",
        "Summon Sugar Sheets",
        "Summon Rad Libs",
        "Summon Smithsness",
    )

    val grimoireSkills = arrayOf(
        "Summon Hilarious Objects",
        "Summon Tasteful Items",
        "Summon Alice's Army Cards",
        "Summon Geeky Gifts",
        "Summon Confiscated Things",
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

    fun prefContainsSkill(setting: String, skillName: String): Boolean =
        setting.contains(skillName, ignoreCase = true)

    /** Desktop [BreakfastManager.getBreakfastBookSkills]. */
    fun getBreakfastBookSkills(
        prefs: Preferences,
        settingPrefix: String,
        catalog: Array<String>,
        skillState: SkillState,
        isHardcore: Boolean,
    ): List<String> {
        val suffix = if (isHardcore) "Hardcore" else "Softcore"
        var name = prefs.getString("$settingPrefix$suffix", "none")
        if (name.equals("none", ignoreCase = true) || name.isBlank()) return emptyList()
        if (name.equals("Summon Candy Hearts", ignoreCase = true)) {
            name = "Summon Candy Heart"
            prefs.setString("$settingPrefix$suffix", name)
        }
        val castAll = name.equals("all", ignoreCase = true)
        return catalog.filter { skillName ->
            (castAll || skillName.equals(name, ignoreCase = true)) &&
                findSkill(skillState, skillName) != null
        }
    }

    /** Desktop [BreakfastManager.getBreakfastLibramSkills]. */
    fun getBreakfastLibramSkills(
        prefs: Preferences,
        skillState: SkillState,
        charState: CharacterState,
    ): List<String> = getBreakfastBookSkills(
        prefs = prefs,
        settingPrefix = "libramSkills",
        catalog = libramSkills,
        skillState = skillState,
        isHardcore = charState.isHardcore,
    )
}
