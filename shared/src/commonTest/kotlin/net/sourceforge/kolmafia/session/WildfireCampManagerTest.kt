package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class WildfireCampManagerTest {

    @Test
    fun getFireLevel_defaultsToFiveForUnknownLocation() = runBlocking {
        AdventureDatabase.load()
        val manager = WildfireCampManager(Preferences(MapSettings()))
        assertEquals(5, manager.getFireLevel("The Spooky Forest"))
    }

    @Test
    fun parseCaptain_populatesFireLevelMap() = runBlocking {
        AdventureDatabase.load()
        val prefs = Preferences(MapSettings())
        val manager = WildfireCampManager(prefs)
        val html = """
            <select>
              <option value="338">Dreadsylvanian Woods (Fire: 3)</option>
              <option value="339">Dreadsylvanian Village (Zone: 1)</option>
            </select>
        """.trimIndent()

        manager.parseCaptain(html)

        assertEquals(3, manager.getFireLevel("Dreadsylvanian Woods"))
        assertEquals(1, manager.getFireLevel("Dreadsylvanian Village"))
    }

    @Test
    fun persistsAcrossLoad() = runBlocking {
        val settings = MapSettings()
        AdventureDatabase.load()
        val manager1 = WildfireCampManager(Preferences(settings))
        manager1.setFireLevelForTest("Dreadsylvanian Castle", 2)

        val manager2 = WildfireCampManager(Preferences(settings))
        manager2.load()
        assertEquals(2, manager2.getFireLevel("Dreadsylvanian Castle"))
    }
}
