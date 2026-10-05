package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.adventure.AdventureSession
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [GenericRequest.checkSkillRedirection] — fight labels after runskillz.php → fight.
 */
object CheckSkillRedirection {
    const val RAIN_MAN = 16011
    const val EVOKE_ELDRITCH_HORROR = 168

    data class Result(val handled: Boolean, val label: String? = null)

    fun apply(location: String, preferences: Preferences?): Result {
        if (!location.contains("runskillz.php", ignoreCase = true)) {
            return Result(handled = false)
        }
        val skillId = Regex("""whichskill=(\d+)""", RegexOption.IGNORE_CASE)
            .find(location)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return Result(handled = false)

        val skillName = when (skillId) {
            RAIN_MAN -> "Rain Man"
            EVOKE_ELDRITCH_HORROR -> {
                preferences?.setBoolean("_eldritchHorrorEvoked", true)
                "Evoke Eldritch Horror"
            }
            else -> return Result(handled = false)
        }

        AdventureSession.setLastAdventure("None", preferences)
        AdventureSession.setNextAdventure("None", preferences)
        CheckItemRedirection.setItemMonster(skillName)
        return Result(handled = true, label = skillName)
    }
}
