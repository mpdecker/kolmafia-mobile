package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.CryptManager

class GameRuntimeLibraryAshP656Test {

    @Test
    fun revision_phase659() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visitCrypt_clearsMissingCorners() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("cyrptAlcoveEvilness", 40)
        prefs.setInt("cyrptCrannyEvilness", 30)
        prefs.setInt("cyrptNicheEvilness", 20)
        prefs.setInt("cyrptNookEvilness", 10)
        prefs.setInt("cyrptTotalEvilness", 100)
        val html = """
            otherimages/cyrpt/ul.gif
            otherimages/cyrpt/ll_clear.gif
            otherimages/cyrpt/ur.gif
            otherimages/cyrpt/lr_clear.gif
        """.trimIndent()
        assertTrue(CryptManager.visitCrypt(html, prefs))
        assertEquals(0, prefs.getInt("cyrptAlcoveEvilness"))
        assertEquals(0, prefs.getInt("cyrptCrannyEvilness"))
        assertEquals(20, prefs.getInt("cyrptNicheEvilness"))
        assertEquals(10, prefs.getInt("cyrptNookEvilness"))
        assertEquals(30, prefs.getInt("cyrptTotalEvilness"))
    }

    @Test
    fun visitCrypt_heartSetsTotal999() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("cyrptAlcoveEvilness", 13)
        prefs.setInt("cyrptNookEvilness", 13)
        val html = "otherimages/cyrpt/thecrypt_heart.gif"
        assertTrue(CryptManager.visitCrypt(html, prefs))
        assertEquals(0, prefs.getInt("cyrptAlcoveEvilness"))
        assertEquals(0, prefs.getInt("cyrptNookEvilness"))
        assertEquals(999, prefs.getInt("cyrptTotalEvilness"))
    }

    @Test
    fun applyFromVisit_requiresCryptPhp() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("cyrptNookEvilness", 50)
        val html = "otherimages/cyrpt/ul.gif otherimages/cyrpt/ur_clear.gif otherimages/cyrpt/ll_clear.gif otherimages/cyrpt/lr_clear.gif"
        assertFalse(CryptManager.applyFromVisit("place.php?whichplace=cemetery", html, prefs))
        assertTrue(CryptManager.applyFromVisit("crypt.php", html, prefs))
        assertEquals(50, prefs.getInt("cyrptNookEvilness"))
        assertEquals(50, prefs.getInt("cyrptTotalEvilness"))
    }
}
