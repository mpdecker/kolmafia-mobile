package net.sourceforge.kolmafia.data

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.round

class ConsumableDatabaseVariableTest {

    @Test
    fun setSmoresData_scalesAdventuresWithSmoresEaten() = runBlocking {
        ConsumableDatabase.load()
        val prefs = Preferences(MapSettings())
        prefs.setInt("smoresEaten", 2)

        ConsumableDatabase.setSmoresData(prefs)

        val size = 3
        val expectedAdv = ceil(size.toDouble().pow(1.75)).toInt()
        assertEquals(size, ConsumableDatabase.getFood("s'more")?.amount)
        assertEquals(expectedAdv.toString(), ConsumableDatabase.getAdventureRange("s'more"))
    }

    @Test
    fun setAffirmationCookieData_scalesWithCookiesEaten() = runBlocking {
        ConsumableDatabase.load()
        val prefs = Preferences(MapSettings())
        prefs.setInt("affirmationCookiesEaten", 2)

        ConsumableDatabase.setAffirmationCookieData(prefs)

        assertEquals("7", ConsumableDatabase.getAdventureRange("Affirmation Cookie"))
        assertEquals("90", ConsumableDatabase.getMuscleRange("Affirmation Cookie"))
        assertEquals("90", ConsumableDatabase.getMysticalityRange("Affirmation Cookie"))
        assertEquals("90", ConsumableDatabase.getMoxieRange("Affirmation Cookie"))
    }

    @Test
    fun setDistillateData_scalesWithFamiliarSweat() = runBlocking {
        ConsumableDatabase.load()
        val prefs = Preferences(MapSettings())
        prefs.setInt("familiarSweat", 25)

        ConsumableDatabase.setDistillateData(prefs)

        val expectedAdv = round(25.0.pow(0.4)).toInt()
        assertEquals(expectedAdv.toString(), ConsumableDatabase.getAdventureRange("stillsuit distillate"))
        assertEquals("5 Buzzed on Distillate", ConsumableDatabase.getNotesByName("stillsuit distillate"))
    }

    @Test
    fun setDistillateData_clampsLowFamiliarSweatToZero() = runBlocking {
        ConsumableDatabase.load()
        val prefs = Preferences(MapSettings())
        prefs.setInt("familiarSweat", 5)

        ConsumableDatabase.setDistillateData(prefs)

        assertEquals("0", ConsumableDatabase.getAdventureRange("stillsuit distillate"))
        assertEquals("0 Buzzed on Distillate", ConsumableDatabase.getNotesByName("stillsuit distillate"))
    }

    @Test
    fun setVariableConsumables_appliesAllPrefDrivenEntries() = runBlocking {
        ConsumableDatabase.load()
        val prefs = Preferences(MapSettings())
        prefs.setInt("smoresEaten", 1)
        prefs.setInt("affirmationCookiesEaten", 0)
        prefs.setInt("familiarSweat", 50)

        ConsumableDatabase.setVariableConsumables(prefs, characterLevel = 5)

        assertTrue(ConsumableDatabase.getAdventureRange("s'more").isNotBlank())
        assertEquals("3", ConsumableDatabase.getAdventureRange("Affirmation Cookie"))
        assertTrue(ConsumableDatabase.getAdventureRange("stillsuit distillate").isNotBlank())
        assertEquals("5", ConsumableDatabase.getAdventureRange("astral pilsner"))
    }
}
