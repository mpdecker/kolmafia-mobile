package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SpadingManager

class GameRuntimeLibraryPhase3470Test {
    @Test
    fun revisionAndNewAshHelpersAreLive() {
        val prefs = Preferences(MapSettings())
        prefs.setString("spadingScript", "capture.ash")
        val lib = GameRuntimeLibrary(preferences = prefs)
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
        assertEquals("false", outputLib(lib, "print(mail_has_new_messages());"))
        assertTrue(outputLib(lib, "print(dvorak_status());").contains("Dvorak"))
        assertEquals("true", outputLib(lib, "print(spading_enabled());"))
        SpadingManager.record(
            SpadingManager.Event.PLACE,
            "cellar.php",
            "response",
            prefs,
            null,
        )
        assertEquals("PLACE", outputLib(lib, "print(spading_last_event());"))
    }
}
