package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.mood.Mood
import net.sourceforge.kolmafia.quest.IslandWarConcert
import net.sourceforge.kolmafia.request.IslandWarRequest
import net.sourceforge.kolmafia.skill.SkillState

internal fun GameRuntimeLibrary.cliConcert(parameters: String, print: (String) -> Unit) {
    val prefs = preferences ?: run {
        print("Preferences are not available.")
        return
    }
    val arg = parameters.trim()
    val option = IslandWarConcert.resolveConcertOption(arg, prefs)
    if (option == null) {
        val error = IslandWarConcert.concertError(arg, prefs)
        print(if (error.isNotEmpty()) error else "Could not visit the Mysterious Island Arena.")
        return
    }

    val client = httpClient ?: run {
        print("HTTP client is not available.")
        return
    }

    runBlocking {
        print("Visiting the Mysterious Island Arena...")
        val result = IslandWarRequest(client).runConcert(option, prefs)
        result.fold(
            onSuccess = { (html, url) ->
                processVisitResponseHooks(html, url)
                when {
                    html.contains("pretty much tapped out") ||
                        html.contains("You're all rocked out") ->
                        print("You can only visit the Mysterious Island Arena once a day.")
                    html.contains("The stage at the Mysterious Island Arena is empty") ->
                        print("Nobody is performing.")
                    html.contains("You acquire an effect") ->
                        print("A music lover is you.")
                    else ->
                        print("You couldn't get to the Mysterious Island Arena.")
                }
            },
            onFailure = { print(it.message ?: "Concert request failed.") },
        )
    }
}

internal fun GameRuntimeLibrary.cliNuns(parameters: String, print: (String) -> Unit) {
    val prefs = preferences ?: run {
        print("Preferences are not available.")
        return
    }

    if (prefs.getInt("nunsVisits", 0) >= 3) {
        print("Nun of the nuns are available right now.")
        return
    }

    val side = prefs.getString("sidequestNunsCompleted", "none")
    if (side != "fratboy" && side != "hippy") {
        print("You have not opened the Nunnery yet.")
        return
    }

    val arg = parameters.trim()
    if (side == "hippy" && arg.equals("mp", ignoreCase = true)) {
        print("Only HP restoration is available from the nuns.")
        return
    }

    if (side == "fratboy") {
        val burn = manaBurnManager
        val char = character?.state?.value
        if (burn != null && char != null) {
            runBlocking {
                burn.burnIfEnabled(
                    mood = moodManager?.activeMood,
                    effectState = effectManager?.state?.value ?: EffectState(),
                    skillState = skillManager?.state?.value ?: SkillState(),
                    charState = char,
                    moodLibrary = moodManager?.moodLibrary ?: emptyMap<String, Mood>(),
                )
            }
        }
    }

    val client = httpClient ?: run {
        print("HTTP client is not available.")
        return
    }

    runBlocking {
        print("Get thee to a nunnery!")
        val result = IslandWarRequest(client).runNunnery(prefs)
        result.fold(
            onSuccess = { (html, url) ->
                processVisitResponseHooks(html, url)
            },
            onFailure = { print(it.message ?: "Nunnery request failed.") },
        )
    }
}
