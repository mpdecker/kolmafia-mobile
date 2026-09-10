package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.data.CombatSkillDropdownParser
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [KoLConstants.availableCombatSkillsSet] — fight-dropdown combat skills.
 * Updated from fight HTML via [CombatSkillDropdownParser] (Phases 5311–5320 + 6631–6650).
 */
object AvailableCombatSkills {
    private val skillIds = linkedSetOf<Int>()

    fun clear() {
        skillIds.clear()
    }

    /**
     * Desktop [FightRequest.parseAvailableCombatSkills]:
     * - no `<select name=whichskill>` → leave prior set unchanged
     * - win page → leave prior set unchanged (dropdown may still be present)
     * - otherwise replace set; Grey Goose / lovebug / gladiator / heartstone side effects
     */
    fun setFromFightHtml(
        html: String,
        preferences: Preferences? = null,
        familiarWeight: Int = 0,
    ) {
        if (!CombatSkillDropdownParser.hasWhichSkillSelect(html)) return
        if (CombatSkillDropdownParser.isFightWon(html)) return

        skillIds.clear()
        for ((id, label) in CombatSkillDropdownParser.parseAvailableCombatSkills(html)) {
            // Grey Goose skills stay in the dropdown after delevel; desktop skips when weight < 6.
            if (id in GREY_GOOSE_SKILLS && familiarWeight < 6) continue
            skillIds += id
            applyUnlockPrefs(id, preferences)
            // Register unknown skill names from the option label when the DB has no entry.
            if (SkillDefinitionDatabase.getById(id) == null) {
                val name = CombatSkillDropdownParser.skillNameFromLabel(label)
                if (name.isNotBlank()) {
                    SkillDefinitionDatabase.registerFromShopVisit(id, name, "skillbook")
                }
            }
        }
    }

    fun add(skillId: Int) {
        if (skillId > 0) skillIds += skillId
    }

    fun has(skillId: Int): Boolean = skillId > 0 && skillId in skillIds

    fun hasName(skillName: String): Boolean {
        if (skillName.isBlank()) return false
        val byName = SkillDefinitionDatabase.getByName(skillName)?.id
        if (byName != null && byName in skillIds) return true
        return skillIds.any { id ->
            SkillDefinitionDatabase.getById(id)?.name.equals(skillName, ignoreCase = true)
        }
    }

    fun ids(): Set<Int> = skillIds.toSet()

    private fun applyUnlockPrefs(skillId: Int, preferences: Preferences?) {
        preferences ?: return
        if (skillId in LOVEBUG_SKILLS) {
            preferences.setBoolean("lovebugsUnlocked", true)
        }
        when (skillId) {
            in 7085..7087 -> {
                val known = preferences.getInt("gladiatorBallMovesKnown", 0)
                if (known + 7084 < skillId) {
                    preferences.setInt("gladiatorBallMovesKnown", skillId - 7084)
                }
            }
            in 7088..7090 -> {
                val known = preferences.getInt("gladiatorNetMovesKnown", 0)
                if (known + 7087 < skillId) {
                    preferences.setInt("gladiatorNetMovesKnown", skillId - 7087)
                }
            }
            in 7091..7093 -> {
                val known = preferences.getInt("gladiatorBladeMovesKnown", 0)
                if (known + 7090 < skillId) {
                    preferences.setInt("gladiatorBladeMovesKnown", skillId - 7090)
                }
            }
            HEARTSTONE_KILL -> preferences.setBoolean("heartstoneKillUnlocked", true)
            HEARTSTONE_BANISH -> preferences.setBoolean("heartstoneBanishUnlocked", true)
            HEARTSTONE_STUN -> preferences.setBoolean("heartstoneStunUnlocked", true)
        }
    }

    private val GREY_GOOSE_SKILLS = 7408..7413
    private val LOVEBUG_SKILLS = 7245..7247
    private const val HEARTSTONE_KILL = 7586
    private const val HEARTSTONE_BANISH = 7587
    private const val HEARTSTONE_STUN = 7588
}
