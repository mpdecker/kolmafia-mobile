package net.sourceforge.kolmafia.thrall

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.PastaThrall

class PastaThrallManagerTest {

    private fun pastamancer(prefs: Preferences): PastaThrallManager {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(name = "Test", classId = "3"))
        }
        return PastaThrallManager(prefs, char)
    }

    @Test
    fun syncFromCharpane_updatesThrallPrefs() {
        val prefs = Preferences(MapSettings())
        val manager = pastamancer(prefs)
        val html = """
            <a href="desc_guardian.php?whichguardian=1">guardian</a>
            <img src="itemimages/vampieroghi.gif">
            <b>Count Alfredo</b> is the Lvl. 7 Vampieroghi</font>
        """.trimIndent()
        manager.syncFromCharpane(html)
        assertEquals("Vampieroghi", prefs.getString(PastaThrallManager.CURRENT_THRALL_PREF, ""))
        assertEquals("7,Count Alfredo", prefs.getString(PastaThrall.prefKey(1), ""))
    }

    @Test
    fun syncFromCharpane_clearsCurrentThrallWhenMissing() {
        val prefs = Preferences(MapSettings())
        prefs.setString(PastaThrallManager.CURRENT_THRALL_PREF, "Vampieroghi")
        val manager = pastamancer(prefs)
        manager.syncFromCharpane("<html><body>No thrall</body></html>")
        assertEquals("", prefs.getString(PastaThrallManager.CURRENT_THRALL_PREF, ""))
    }

    @Test
    fun syncFromCharpane_skipsNonPastamancer() {
        val prefs = Preferences(MapSettings())
        prefs.setString(PastaThrallManager.CURRENT_THRALL_PREF, "Vampieroghi")
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(name = "Test", classId = "1"))
        }
        val manager = PastaThrallManager(prefs, char)
        val html = """
            desc_guardian.php itemimages/vampieroghi.gif
            <b>Count Alfredo</b> the Lvl. 7 Vampieroghi</font>
        """.trimIndent()
        manager.syncFromCharpane(html)
        assertEquals("Vampieroghi", prefs.getString(PastaThrallManager.CURRENT_THRALL_PREF, ""))
    }

    @Test
    fun myThrall_prefersManagerValue() {
        val prefs = Preferences(MapSettings())
        prefs.setString(PastaThrallManager.CURRENT_THRALL_PREF, "Vampieroghi")
        prefs.setString(PastaThrall.prefKey(1), "7,Count Alfredo")
        val manager = pastamancer(prefs)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            pastaThrallManager = manager,
        )
        assertEquals(
            "Vampieroghi",
            net.sourceforge.kolmafia.ash.outputLib(lib, "print(my_thrall());").trim(),
        )
    }
}
