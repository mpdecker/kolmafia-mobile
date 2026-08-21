package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.quest.LatteChoiceSync
import net.sourceforge.kolmafia.request.LatteRequest

internal fun GameRuntimeLibrary.cliLatte(parameters: String, print: (String) -> Unit) {
    val params = parameters.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (params.isEmpty()) {
        print("Usage: latte unlocks | unlocked | refill ingredient1 ingredient2 ingredient3")
        return
    }
    val hasMug = (inventoryManager?.state?.value?.items?.get(LatteChoiceSync.LATTE_MUG_ID)?.quantity ?: 0) > 0 ||
        character?.state?.value?.equipment?.get(EquipmentSlot.OFFHAND)
            ?.equals(LatteChoiceSync.LATTE_MUG_NAME, ignoreCase = true) == true
    if (!hasMug) {
        print("You need a latte lovers member's mug first.")
        return
    }
    when (params[0].lowercase()) {
        "unlocks" -> LatteChoiceSync.listUnlocks(all = true, preferences = preferences)
            .lineSequence().filter { it.isNotBlank() }.forEach(print)
        "unlocked" -> LatteChoiceSync.listUnlocks(all = false, preferences = preferences)
            .lineSequence().filter { it.isNotBlank() }.forEach(print)
        "refill" -> {
            if (params.size != 4) {
                print("A latte refill requires exactly three ingredients.")
                return
            }
            val client = httpClient ?: run {
                print("HTTP client is not available.")
                return
            }
            runBlocking {
                LatteRequest(client)
                    .refill(
                        first = params[1],
                        second = params[2],
                        third = params[3],
                        preferences = preferences,
                        sessionLog = { line -> sessionLogger?.appendRawLine(line) },
                    )
                    .onSuccess { print(it) }
                    .onFailure { print(it.message ?: "Latte refill failed.") }
            }
        }
        else -> print("Usage: latte unlocks | unlocked | refill ingredient1 ingredient2 ingredient3")
    }
}
