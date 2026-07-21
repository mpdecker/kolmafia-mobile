package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking

internal fun GameRuntimeLibrary.cliSummon(parameters: String, print: (String) -> Unit) {
    val mgr = summoningChamberManager ?: run {
        print("Summoning Chamber is not available.")
        return
    }
    runBlocking {
        mgr.summon(parameters, print)
    }
}
