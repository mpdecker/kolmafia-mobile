package net.sourceforge.kolmafia.equipment

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class ModeableTest {

    @Test
    fun find_byItemId_andName() {
        assertEquals(Modeable.UMBRELLA, Modeable.find(10899))
        assertEquals(Modeable.UMBRELLA, Modeable.find("unbreakable umbrella"))
        assertEquals(Modeable.PARKA, Modeable.find("Jurassic Parka"))
    }

    @Test
    fun retroCape_compositeState() {
        val prefs = Preferences(MapSettings())
        prefs.setString("retroCapeSuperhero", "vampire")
        prefs.setString("retroCapeWashingInstructions", "hold")
        assertEquals("vampire hold", ModeableState.currentMode(prefs, Modeable.RETROCAPE))
    }

    @Test
    fun umbrella_modes_includeBucketStyle() {
        assertTrue("bucket style" in Modeable.UMBRELLA.modes)
    }

    @Test
    fun normalizeMode_matchesCanonicalName() {
        assertEquals("bucket style", Modeable.UMBRELLA.normalizeMode("Bucket Style"))
    }

    @Test
    fun replicaParka_sharesParkaModes() {
        assertEquals(Modeable.PARKA.modes, Modeable.REPLICA_PARKA.modes)
        assertNotNull(Modeable.find(11249))
    }
}
