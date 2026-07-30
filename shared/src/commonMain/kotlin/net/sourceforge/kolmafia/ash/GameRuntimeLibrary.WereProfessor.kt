package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.session.WereProfessorManager

internal fun GameRuntimeLibrary.cliWereProfessor(params: String, print: (String) -> Unit) {
    val tokens = params.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (tokens.isEmpty() || !tokens[0].equals("research", ignoreCase = true)) {
        print("Do what?")
        return
    }

    val charState = character?.state?.value ?: CharacterState()
    val effectNames = effectManager?.state?.value?.effects?.map { it.name }.orEmpty()

    when {
        tokens.size == 1 -> runBlocking {
            WereProfessorManager.printSkillTrees(
                charState = charState,
                effectNames = effectNames,
                preferences = preferences,
                request = researchBenchRequest,
                verbose = false,
                print = print,
            )
        }
        tokens[1].equals("verbose", ignoreCase = true) -> runBlocking {
            WereProfessorManager.printSkillTrees(
                charState = charState,
                effectNames = effectNames,
                preferences = preferences,
                request = researchBenchRequest,
                verbose = true,
                print = print,
            )
        }
        else -> {
            val prefs = preferences ?: run {
                print("Preferences are not available.")
                return
            }
            runBlocking {
                WereProfessorManager.researchSkill(
                    field = tokens[1],
                    charState = charState,
                    effectNames = effectNames,
                    preferences = prefs,
                    request = researchBenchRequest,
                    print = print,
                )
            }
        }
    }
}
