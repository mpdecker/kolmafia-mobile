package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences

fun runLib(lib: GameRuntimeLibrary, src: String): AshRuntime {
    val runtime = AshRuntime(lib)
    runtime.execute(AshParser().parse(src))
    return runtime
}

fun outputLib(lib: GameRuntimeLibrary, src: String): String =
    runLib(lib, src).output.toString().trim()

fun prefs(): Preferences = Preferences(MapSettings())
