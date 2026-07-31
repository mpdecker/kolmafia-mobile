package net.sourceforge.kolmafia.data

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TCRSDatabaseVariableTest {

    @Test
    fun resetModifiers_appliesVariableConsumablesFromPrefs() = runBlocking {
        ConsumableDatabase.load()
        val prefs = Preferences(MapSettings())
        prefs.setInt("smoresEaten", 3)

        TCRSDatabase.injectMapForTest(
            entries = mapOf(
                471 to TCRSDatabase.TcrsEntry(
                    name = "test item",
                    size = 1,
                    quality = "good",
                    modifiers = "Muscle: +1",
                ),
            ),
        )
        val bundledSmoresAdv = ConsumableDatabase.getAdventureRange("s'more")

        TCRSDatabase.resetModifiers(prefs, characterLevel = 5)

        assertNotEquals(bundledSmoresAdv, ConsumableDatabase.getAdventureRange("s'more"))
        assertEquals(4, ConsumableDatabase.getFood("s'more")?.amount)
        TCRSDatabase.reset()
    }
}
