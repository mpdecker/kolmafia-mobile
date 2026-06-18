package net.sourceforge.kolmafia.vykea

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.preferences.Preferences

class VykeaCompanionManagerTest {

    @Test
    fun syncFromCharpane_updatesCompanionPrefs() {
        val prefs = Preferences(MapSettings())
        val manager = VykeaCompanionManager(prefs)
        val html = """
            <font size=2><b>VYKEA Companion</b></font><br><font size=2><b>CHEBLI</b> the level 5 lamp<br>
        """.trimIndent()
        prefs.setString(VykeaCompanionManager.RUNE_PREF, "blood")
        manager.syncFromCharpane(html)
        assertEquals("CHEBLI, the level 5 blood lamp", prefs.getString(VykeaCompanionManager.CURRENT_VYKEA_PREF, ""))
        assertEquals("CHEBLI", prefs.getString(VykeaCompanionManager.NAME_PREF, ""))
        assertEquals(5, prefs.getInt(VykeaCompanionManager.LEVEL_PREF, 0))
        assertEquals("lamp", prefs.getString(VykeaCompanionManager.TYPE_PREF, ""))
        assertEquals("blood", prefs.getString(VykeaCompanionManager.RUNE_PREF, ""))
    }

    @Test
    fun syncFromCharpane_skipsWhenCompanionAlreadyStored() {
        val prefs = Preferences(MapSettings())
        prefs.setString(VykeaCompanionManager.CURRENT_VYKEA_PREF, "level 3 couch")
        val manager = VykeaCompanionManager(prefs)
        val html = """
            <font size=2><b>VYKEA Companion</b></font><br><font size=2><b>Other</b> the level 5 lamp<br>
        """.trimIndent()
        manager.syncFromCharpane(html)
        assertEquals("level 3 couch", prefs.getString(VykeaCompanionManager.CURRENT_VYKEA_PREF, ""))
    }

    @Test
    fun myVykeaCompanion_prefersManagerValue() {
        val prefs = Preferences(MapSettings())
        prefs.setString(VykeaCompanionManager.CURRENT_VYKEA_PREF, "CHEBLI, the level 5 blood lamp")
        val manager = VykeaCompanionManager(prefs)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            vykeaCompanionManager = manager,
        )
        assertEquals(
            "CHEBLI, the level 5 blood lamp",
            net.sourceforge.kolmafia.ash.outputLib(lib, "print(my_vykea_companion());").trim(),
        )
    }
}
