package net.sourceforge.kolmafia.data

/** Desktop [FightRequest.parseAvailableCombatSkills] dropdown extraction. */
object CombatSkillDropdownParser {

    private val WHICH_SKILL_SELECT = Regex(
        """<select[^>]*\bname\s*=\s*"?whichskill"?[^>]*>""",
        RegexOption.IGNORE_CASE,
    )
    private val AVAILABLE_COMBAT_SKILL = Regex(
        """<option[^>]*?value="(\d+)[^>]*?>((.*?) \((\d+)[^<]*)</option>""",
    )
    private val WIN_PATTERN = Regex("""You win the fight""")

    fun parseAvailableCombatSkills(html: String): List<Pair<Int, String>> {
        if (!WHICH_SKILL_SELECT.containsMatchIn(html)) return emptyList()
        if (WIN_PATTERN.containsMatchIn(html)) return emptyList()

        val result = mutableListOf<Pair<Int, String>>()
        for (match in AVAILABLE_COMBAT_SKILL.findAll(html)) {
            val skillId = match.groupValues[1].toIntOrNull() ?: continue
            val label = match.groupValues[2]
            result.add(skillId to label)
        }
        return result
    }
}
