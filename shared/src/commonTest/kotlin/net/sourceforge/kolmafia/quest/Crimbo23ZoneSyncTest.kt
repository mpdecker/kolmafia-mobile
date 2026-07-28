package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class Crimbo23ZoneSyncTest {

    @Test
    fun armory_elfControl() {
        val prefs = Preferences(MapSettings())
        Crimbo23ZoneSync.syncFromPlaceHtml("""<img src="armory_elf.gif">""", prefs)
        assertFalse(prefs.getBoolean("crimbo23ArmoryAtWar", true))
        assertEquals("elf", prefs.getString("crimbo23ArmoryControl", ""))
    }

    @Test
    fun armory_pirateControl() {
        val prefs = Preferences(MapSettings())
        Crimbo23ZoneSync.syncFromPlaceHtml("""<img src="armory_pirate.gif">""", prefs)
        assertFalse(prefs.getBoolean("crimbo23ArmoryAtWar", true))
        assertEquals("pirate", prefs.getString("crimbo23ArmoryControl", ""))
    }

    @Test
    fun armory_contested() {
        val prefs = Preferences(MapSettings())
        Crimbo23ZoneSync.syncFromPlaceHtml("""<img src="armory_war.gif">""", prefs)
        assertTrue(prefs.getBoolean("crimbo23ArmoryAtWar", false))
        assertEquals("contested", prefs.getString("crimbo23ArmoryControl", ""))
    }

    @Test
    fun armory_noneWhenNoMarkers() {
        val prefs = Preferences(MapSettings())
        prefs.setString("crimbo23ArmoryControl", "elf")
        Crimbo23ZoneSync.syncFromPlaceHtml("<p>CrimboTown</p>", prefs)
        assertFalse(prefs.getBoolean("crimbo23ArmoryAtWar", true))
        assertEquals("none", prefs.getString("crimbo23ArmoryControl", ""))
    }

    @Test
    fun bar_cafe_factoryAndCottageSync() {
        val prefs = Preferences(MapSettings())
        val html = """
            bar_elf.gif
            cafe_pirate.gif
            abuela_war.gif
            factory_elf.gif
        """.trimIndent()
        Crimbo23ZoneSync.syncFromPlaceHtml(html, prefs)

        assertEquals("elf", prefs.getString("crimbo23BarControl", ""))
        assertEquals("pirate", prefs.getString("crimbo23CafeControl", ""))
        assertTrue(prefs.getBoolean("crimbo23CottageAtWar", false))
        assertEquals("contested", prefs.getString("crimbo23CottageControl", ""))
        assertEquals("elf", prefs.getString("crimbo23FoundryControl", ""))
    }
}
