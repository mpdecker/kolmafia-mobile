package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.adventure.AdventureSession
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.MimicDnaChoiceSync
import net.sourceforge.kolmafia.session.ChoiceCombatAshState
import net.sourceforge.kolmafia.session.EncounterManager
import net.sourceforge.kolmafia.session.ResultProcessor

/**
 * Desktop [GenericRequest.checkChoiceRedirection] — fight labels after choice.php → fight.
 */
object CheckChoiceRedirection {
    data class Result(val handled: Boolean, val label: String? = null)

    /** Optional pocket-fight registrar (wired to [CargoPocketSync.registerPocketFight]). */
    var registerPocketFight: ((String) -> Unit)? = null

    fun apply(
        location: String,
        preferences: Preferences?,
        choiceId: Int = ChoiceCombatAshState.lastChoice,
    ): Result {
        if (!location.contains("choice.php", ignoreCase = true)) {
            return Result(handled = false)
        }
        val name = when (choiceId) {
            970 -> "Rain Man"
            1103 -> "Calculate the Universe"
            1201 -> {
                preferences?.setBoolean("_eldritchTentacleFought", true)
                "Dr. Gordon Stuart's Science Tent"
            }
            1267 -> "Genie Wish"
            1420 -> {
                registerPocketFight?.invoke(location)
                "Cargo Cultist Shorts"
            }
            1463 -> "Combat Lover's Locket"
            1510 -> "Burning Leaves"
            1516 -> {
                preferences?.let { MimicDnaChoiceSync.updateMimicMonsters(it, location, -1) }
                ResultProcessor.processItem(MimicDnaChoiceSync.MIMIC_EGG, -1, preferences)
                EncounterManager.ignoreSpecialMonsters()
                "mimic egg"
            }
            else -> return Result(handled = false)
        }

        AdventureSession.setLastAdventure("None", preferences)
        AdventureSession.setNextAdventure("None", preferences)
        CheckItemRedirection.setItemMonster(name)
        return Result(handled = true, label = name)
    }

    fun resetForTest() {
        registerPocketFight = null
    }
}
