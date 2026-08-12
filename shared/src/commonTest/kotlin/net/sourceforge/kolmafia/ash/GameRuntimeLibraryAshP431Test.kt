package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DispensarySync

class GameRuntimeLibraryAshP431Test {

    @Test
    fun revision_phase470() {
        assertEquals("phase470", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun dispensary_available_trueAfterVisitHookUnlock() {
        val prefs = Preferences(MapSettings())
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(ascensions = "5"))
        }
        val lib = GameRuntimeLibrary(character = char, preferences = prefs)
        lib.processVisitResponseHooks(
            """<p>You learn the password from FARQUAR.</p>""",
            "https://www.kingdomofloathing.com/adventure.php",
        )
        assertEquals("5", prefs.getInt(DispensarySync.LAST_DISPENSARY_OPEN_PREF, -1).toString())
    }
}
