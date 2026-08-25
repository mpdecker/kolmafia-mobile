package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.combat.DiscoCombatHelper
import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillManager

/**
 * Adventure-loop glue for [DiscoCombatHelper] (Phases 1566–1580).
 */
object FightDiscoComboSync {

    private val SKILL_IN_MACRO = Regex("""skill\s+(\d+)""", RegexOption.IGNORE_CASE)

    fun initializeFromCharacter(
        character: KoLCharacter?,
        preferences: Preferences?,
        skillManager: SkillManager? = null,
    ) {
        preferences ?: return
        val state = character?.state?.value
        val isDb = state?.isDiscoBandit == true
        DiscoCombatHelper.ensureUpdatedNemesisStatus(
            preferences,
            state?.ascensionNumber ?: preferences.getInt("ascensions", 0),
        )
        DiscoCombatHelper.initialize(
            isDiscoBandit = isDb,
            preferences = preferences,
            hasSkill = { name ->
                val fromManager = skillManager?.state?.value?.skills.orEmpty()
                    .any { it.name.equals(name, ignoreCase = true) }
                if (fromManager) return@initialize true
                val id = when (name) {
                    "Break It On Down" -> 50
                    "Pop and Lock It" -> 51
                    "Run Like the Wind" -> 52
                    else -> return@initialize false
                }
                preferences.getInt("skillLevel$id", 0) > 0
            },
            monsterNameProvider = { MonsterStatusTracker.getLastMonsterName() },
        )
    }

    /**
     * Feed posted macro skill ids + response HTML into [DiscoCombatHelper.parseFightRound].
     */
    fun apply(macro: String, html: String) {
        if (!DiscoCombatHelper.canCombo || html.isBlank()) return
        val skills = SKILL_IN_MACRO.findAll(macro).map { it.groupValues[1] }.toList()
        if (skills.isEmpty()) {
            DiscoCombatHelper.parseFightRound(null, html)
            return
        }
        for (id in skills) {
            DiscoCombatHelper.parseFightRound("skill$id", html)
        }
    }
}
