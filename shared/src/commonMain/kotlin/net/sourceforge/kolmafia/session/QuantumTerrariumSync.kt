package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [QuantumTerrariumRequest.parseResponse] pref sync for Quantum Terrarium visits. */
object QuantumTerrariumSync {

    const val FAMILIAR_COUNTER = "Quantum Familiar"
    const val COOLDOWN_COUNTER = "Q.F.I.D.M.A."

    private val CURRENT_FAM_PATTERN = Regex(
        """Your Current Familiar.*? onClick='fam\((\d+)\)'.*?<br\s*/?><b>(.*?)</b><br\s*/?><a.*?who=(\d+)>(.*?)</a>'s (.*?)<br""",
        setOf(RegexOption.DOT_MATCHES_ALL),
    )
    private val NEXT_FAM_PATTERN = Regex(
        """Your Familiar in <b>(\d+)</b> Adventures.*? onClick='fam\((\d+)\)'.*?<br\s*/?><b>(.*?)</b><br\s*/?><a.*?who=(\d+)>(.*?)</a>'s (.*?)<br""",
        setOf(RegexOption.DOT_MATCHES_ALL),
    )
    private val REALIGNMENT_COOLDOWN_PATTERN = Regex(
        """You will be able to align the quanta again in <b>(\d+) adventures.</b>""",
    )

    data class ParseResult(
        val needsStatusRefresh: Boolean = false,
        val currentFamiliarId: Int? = null,
        val currentFamiliarName: String? = null,
        val forcedNextFamiliarId: Int? = null,
        val forcedAlign: Boolean = false,
    )

    fun parseResponse(
        html: String,
        preferences: Preferences?,
        characterState: CharacterState,
    ): ParseResult {
        var needsStatusRefresh = false
        var currentFamiliarId: Int? = null
        var currentFamiliarName: String? = null
        var forcedNextFamiliarId: Int? = null

        CURRENT_FAM_PATTERN.find(html)?.let { match ->
            val currentId = match.groupValues[1].toIntOrNull() ?: 0
            currentFamiliarId = currentId
            currentFamiliarName = match.groupValues[2]
            if (currentId != characterState.familiarId) {
                needsStatusRefresh = true
            }
        }

        NEXT_FAM_PATTERN.find(html)?.let { match ->
            val turns = match.groupValues[1].toIntOrNull() ?: 0
            val nextFamId = match.groupValues[2].toIntOrNull() ?: -1
            forcedNextFamiliarId = nextFamId
            preferences?.setString("nextQuantumFamiliar", familiarName(nextFamId))
            preferences?.setString("nextQuantumFamiliarName", match.groupValues[3])
            preferences?.setString("nextQuantumFamiliarOwner", match.groupValues[5])
            preferences?.setInt("nextQuantumFamiliarOwnerId", match.groupValues[4].toIntOrNull() ?: 0)
            preferences?.setInt(
                "nextQuantumFamiliarTurn",
                characterState.turnsPlayed + turns,
            )
            if (preferences != null) {
                TurnCounter.stopCounting(preferences, FAMILIAR_COUNTER)
                TurnCounter.startCounting(
                    preferences = preferences,
                    currentRun = characterState.currentRun,
                    turns = turns,
                    label = "$FAMILIAR_COUNTER loc=*",
                    image = familiarImage(nextFamId),
                )
            }
        }

        val realignmentTurns = REALIGNMENT_COOLDOWN_PATTERN.find(html)?.groupValues
            ?.get(1)
            ?.toIntOrNull() ?: -1
        preferences?.setInt(
            "_nextQuantumAlignment",
            characterState.turnsPlayed + realignmentTurns,
        )
        if (preferences != null) {
            TurnCounter.stopCounting(preferences, COOLDOWN_COUNTER)
            if (realignmentTurns >= 0) {
                TurnCounter.startCounting(
                    preferences = preferences,
                    currentRun = characterState.currentRun,
                    turns = realignmentTurns,
                    label = "$COOLDOWN_COUNTER loc=*",
                    image = "quantum.gif",
                )
            }
        }

        val forcedAlign = html.contains("arranging the quanta to force your desired future")

        return ParseResult(
            needsStatusRefresh = needsStatusRefresh,
            currentFamiliarId = currentFamiliarId,
            currentFamiliarName = currentFamiliarName,
            forcedNextFamiliarId = forcedNextFamiliarId,
            forcedAlign = forcedAlign,
        )
    }

    private fun familiarName(familiarId: Int): String =
        if (familiarId <= 0) {
            "none"
        } else {
            FamiliarDefinitionDatabase.getById(familiarId)?.name ?: "none"
        }

    private fun familiarImage(familiarId: Int): String =
        if (familiarId <= 0) {
            "none.gif"
        } else {
            FamiliarDefinitionDatabase.getById(familiarId)?.image ?: "none.gif"
        }
}
