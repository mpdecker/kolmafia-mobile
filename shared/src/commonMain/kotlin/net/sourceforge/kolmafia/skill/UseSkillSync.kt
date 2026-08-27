package net.sourceforge.kolmafia.skill

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.DailyLimitDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop UseSkillRequest.parseResponse / lastSkillUsed ledger (Phases 2226–2245, deepened 2406–2420).
 */
object UseSkillSync {
    @Volatile
    var lastSkillUsed: Int = -1

    @Volatile
    var lastSkillCount: Int = 0

    @Volatile
    var lastUpdate: String = ""

    fun noteCast(skillId: Int, quantity: Int) {
        lastSkillUsed = skillId
        lastSkillCount = quantity.coerceAtLeast(1)
        lastUpdate = ""
    }

    /**
     * Parse skills.php cast response. Returns true when the cast should be treated as a stop/failure.
     */
    fun parseResponse(
        urlString: String,
        responseText: String,
        preferences: Preferences? = null,
        character: KoLCharacter? = null,
        mpCostPerCast: Int? = null,
    ): Boolean {
        val skillId = lastSkillUsed
        val count = lastSkillCount.coerceAtLeast(1)
        lastSkillUsed = -1
        lastSkillCount = 0

        if (skillId <= 0 && !urlString.contains("whichskill=", ignoreCase = true)) {
            return false
        }
        val resolvedId = if (skillId > 0) {
            skillId
        } else {
            Regex("""whichskill=(\d+)""", RegexOption.IGNORE_CASE)
                .find(urlString)?.groupValues?.get(1)?.toIntOrNull() ?: return false
        }

        preferences?.setInt("lastSkillUsed", resolvedId)
        preferences?.setInt("lastSkillCount", count)

        // Heartstone unlocks from skillz list pages
        if (urlString.contains("skillz.php", ignoreCase = true) ||
            responseText.contains("heartstone", ignoreCase = true)
        ) {
            parseHeartstoneUnlocks(responseText, preferences)
        }

        when {
            responseText.contains("don't have that skill", ignoreCase = true) ||
                responseText.contains("don't seem to have that skill", ignoreCase = true) -> {
                lastUpdate = "You don't have that skill"
                return true
            }
            responseText.contains("can't fit anymore", ignoreCase = true) ||
                responseText.contains("can't fit any more", ignoreCase = true) -> {
                lastUpdate = "Can't fit any more songs"
                return true
            }
            responseText.contains("Invalid target player", ignoreCase = true) -> {
                lastUpdate = "Invalid target player"
                return true
            }
            responseText.contains("busy fighting", ignoreCase = true) -> {
                lastUpdate = "Target is busy fighting"
                return true
            }
            responseText.contains("receive buffs", ignoreCase = true) -> {
                lastUpdate = "Selected target cannot receive buffs"
                return true
            }
            responseText.contains("lower than level", ignoreCase = true) -> {
                lastUpdate = "Selected target is too low level"
                return true
            }
            responseText.contains("don't have enough", ignoreCase = true) ||
                responseText.contains("not enough mp", ignoreCase = true) ||
                responseText.contains("not enough mana", ignoreCase = true) -> {
                lastUpdate = "Not enough mana to cast skill"
                return true
            }
            responseText.contains("enough for one day", ignoreCase = true) ||
                responseText.contains("daily limit", ignoreCase = true) ||
                responseText.contains("can't cast that many", ignoreCase = true) ||
                responseText.contains("You can't cast that many turns", ignoreCase = true) ||
                responseText.contains("You've already recalled a lot of ancestral memories", ignoreCase = true) -> {
                lastUpdate = "Daily limit reached"
                markCastLimitMax(resolvedId, preferences)
                return true
            }
            responseText.contains("You need", ignoreCase = true) &&
                (
                    responseText.contains("to use that skill", ignoreCase = true) ||
                        responseText.contains("special equipment", ignoreCase = true)
                    ) -> {
                lastUpdate = "Missing required equipment"
                return true
            }
            responseText.contains("can't remember how to use that skill", ignoreCase = true) -> {
                lastUpdate = "That skill is currently unavailable"
                return true
            }
            responseText.contains("not an Accordion Thief", ignoreCase = true) -> {
                lastUpdate = "Only Accordion Thieves can use that skill"
                return true
            }
            responseText.contains("You're already blessed", ignoreCase = true) -> {
                lastUpdate = "You already have that blessing"
                return true
            }
            responseText.contains("not attuned to any particular Turtle Spirit", ignoreCase = true) -> {
                lastUpdate = "You haven't got a Blessing, so can't get a Boon"
                return true
            }
            responseText.contains("You decide not to commit", ignoreCase = true) -> {
                lastUpdate = "You decide to not change your favorite bird"
                return true
            }
        }

        // Limited-use "Y / maxCasts casts used today."
        Regex(
            """(\d+)\s*/\s*(\d+)\s*casts used today""",
            RegexOption.IGNORE_CASE,
        ).find(responseText)?.let { m ->
            val used = m.groupValues[1].toIntOrNull() ?: return@let
            val max = m.groupValues[2].toIntOrNull() ?: return@let
            val pref = DailyLimitDatabase.getCastPrefForSkill(resolvedId)
            if (pref.isNotBlank() && preferences != null) {
                // Synch before this cast's increment in registerSuccessfulCasts
                preferences.setInt(pref, (used - count).coerceAtLeast(0))
                preferences.setInt("${pref}_max", max)
            }
        }

        val unitCost = mpCostPerCast
            ?: SkillDefinitionDatabase.getById(resolvedId)?.mpCost
            ?: 0
        val totalMp = unitCost.toLong() * count
        if (totalMp > 0 && character != null) {
            val state = character.state.value
            val newMp = (state.currentMp - totalMp.toInt()).coerceAtLeast(0)
            character.updateHpMp(state.currentHp, state.maxHp, newMp, state.maxMp)
        }

        UseSkillCastGates.registerSuccessfulCasts(resolvedId, count, preferences)
        lastUpdate = ""
        return false
    }

    private fun markCastLimitMax(skillId: Int, preferences: Preferences?) {
        preferences ?: return
        val pref = DailyLimitDatabase.getCastPrefForSkill(skillId)
        if (pref.isBlank()) return
        val max = preferences.getInt("${pref}_max", 0)
        if (max > 0) {
            preferences.setInt(pref, max)
        } else {
            // Bump used high enough that soft gate trips next cast
            preferences.setInt(pref, preferences.getInt(pref, 0) + 99)
        }
    }

    private fun parseHeartstoneUnlocks(html: String, preferences: Preferences?) {
        preferences ?: return
        // Desktop skill id constants — match by name fragments in HTML when present
        if (html.contains("heartstoneKill", ignoreCase = true) ||
            html.contains("Heartstone Kill", ignoreCase = true)
        ) {
            preferences.setBoolean("heartstoneKillUnlocked", true)
        }
        if (html.contains("heartstoneBanish", ignoreCase = true) ||
            html.contains("Heartstone Banish", ignoreCase = true)
        ) {
            preferences.setBoolean("heartstoneBanishUnlocked", true)
        }
        if (html.contains("heartstoneStun", ignoreCase = true)) {
            preferences.setBoolean("heartstoneStunUnlocked", true)
        }
        if (html.contains("heartstoneLuck", ignoreCase = true)) {
            preferences.setBoolean("heartstoneLuckUnlocked", true)
        }
        if (html.contains("heartstonePals", ignoreCase = true)) {
            preferences.setBoolean("heartstonePalsUnlocked", true)
        }
        if (html.contains("heartstoneBuff", ignoreCase = true)) {
            preferences.setBoolean("heartstoneBuffUnlocked", true)
        }
    }

    fun resetForTest() {
        lastSkillUsed = -1
        lastSkillCount = 0
        lastUpdate = ""
    }
}
