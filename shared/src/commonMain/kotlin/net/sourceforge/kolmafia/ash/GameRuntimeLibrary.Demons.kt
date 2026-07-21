package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.cliDemons(parameters: String, print: (String) -> Unit) {
    val mgr = demonNamesManager ?: run {
        print("Demon name listing is not available.")
        return
    }
    when (parameters.trim()) {
        "", "list" -> mgr.listDemons(print)
        "solve14" -> mgr.solve14(print)
        else -> print("Unknown demons command: $parameters")
    }
}
