package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.request.BarrelChoiceMapper
import net.sourceforge.kolmafia.request.BarrelPrayerRequest

internal fun GameRuntimeLibrary.cliBarrelPrayer(parameters: String, print: (String) -> Unit) {
    if (parameters.isEmpty()) {
        print("Usage: barrelprayer protection | glamour | vigor | buff")
        print("protection or barrel lid: get barrel lid (1/ascension)")
        print("glamour or barrel hoop earring: get barrel hoop earring (1/ascension)")
        print("vigor or bankruptcy barrel : get bankruptcy barrel (1/ascension)")
        print("buff: get class buff")
        return
    }

    val option = BarrelChoiceMapper.findPrayer(parameters)
    if (option == 0) {
        print("I don't understand what '$parameters' barrel prayer is.")
        return
    }

    val client = httpClient ?: run {
        print("HTTP client is not available.")
        return
    }
    val choice = choiceRequest ?: run {
        print("Choice request is not available.")
        return
    }
    val prefs = preferences
    val state = character?.state?.value

    runBlocking {
        val result = BarrelPrayerRequest(client, choice).pray(option, state, prefs)
        result.onFailure { print(it.message ?: "Barrel prayer failed.") }
    }
}
