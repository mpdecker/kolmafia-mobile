package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP968TrackLTest {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    @Test
    fun phase969_familiarEquipmentLockSync_detectsLocked() {
        val p = prefs()
        val html = """familiar.php <b>Locked</b>"""
        FamiliarEquipmentLockSync.parseAndWrite(html, p)
        assertEquals(true, p.getBoolean("familiarEquipmentLocked", false))
    }

    @Test
    fun phase969_familiarEquipmentLockSync_detectsUnlocked() {
        val p = prefs { putBoolean("familiarEquipmentLocked", true) }
        val html = """familiar.php familiar.php?action=lockequip"""
        FamiliarEquipmentLockSync.parseAndWrite(html, p)
        assertEquals(false, p.getBoolean("familiarEquipmentLocked", false))
    }

    @Test
    fun trackL_registrationAnchor() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("false", outputLib(lib, "print(is_familiar_equipment_locked());"))
    }
}
