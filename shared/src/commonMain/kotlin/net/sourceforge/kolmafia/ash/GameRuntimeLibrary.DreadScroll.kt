package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.session.DreadScrollManager

internal fun GameRuntimeLibrary.cliDreadscroll(print: (String) -> Unit) {
    val prefs = preferences ?: run {
        print("Preferences are not available.")
        return
    }
    print(DreadScrollManager.getClues(prefs))
    print("")
    print(DreadScrollManager.getScrollText(prefs))
}
