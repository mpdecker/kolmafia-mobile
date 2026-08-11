package net.sourceforge.kolmafia.data

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

class PokefamDatabaseRegisterTest {

    @BeforeTest
    @AfterTest
    fun resetDatabase() {
        PokefamDatabase.resetForTest()
    }

    @Test
    fun registerFromFight_createsNewEntry() = runTest {
        FamiliarDefinitionDatabase.load()
        val prefs = Preferences(MapSettings())
        val logger = SessionLogger(prefs, GameEventBus())
        PokefamDatabase.registerFromFight(
            race = "Globmule",
            level = 2,
            power = 3,
            hp = 4,
            attribute = "Armor",
            move1 = "Punch",
            move2 = "Tackle",
            move3 = "Deluxe Impale",
            sessionLogger = logger,
        )
        val data = PokefamDatabase.getByName("Globmule")
        assertNotNull(data)
        assertEquals(3, data.power2)
        assertEquals(4, data.hp2)
        assertEquals("Punch", data.move1)
        assertEquals("Tackle", data.move2)
        assertEquals("Deluxe Impale", data.move3)
        assertEquals("Armor", data.attribute)
        val log = prefs.getString(SessionLogger.SESSION_LOG_KEY, "")
        assertEquals(true, log.contains("Globmule"))
    }

    @Test
    fun registerFromFight_updatesExistingLevel2Stats() = runTest {
        FamiliarDefinitionDatabase.load()
        PokefamDatabase.load()
        val prefs = Preferences(MapSettings())
        val logger = SessionLogger(prefs, GameEventBus())
        val before = PokefamDatabase.getByName("Globmule")
        assertNotNull(before)
        PokefamDatabase.registerFromFight(
            race = "Globmule",
            level = 2,
            power = 99,
            hp = 88,
            attribute = before.attribute,
            move1 = before.move1,
            move2 = before.move2,
            move3 = before.move3,
            sessionLogger = logger,
        )
        val after = PokefamDatabase.getByName("Globmule")
        assertNotNull(after)
        assertEquals(99, after.power2)
        assertEquals(88, after.hp2)
    }
}
