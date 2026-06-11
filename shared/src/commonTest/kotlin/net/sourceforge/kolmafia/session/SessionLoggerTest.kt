package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.event.GameEvent
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionLoggerTest {

    @Test
    fun append_persistsEventLines() {
        val prefs = Preferences(MapSettings())
        val bus = GameEventBus()
        val logger = SessionLogger(prefs, bus)
        val loc = net.sourceforge.kolmafia.adventure.AdventureLocation("pantry", "The Haunted Pantry", "pantry")
        logger.append(GameEvent.TurnConsumed(loc, net.sourceforge.kolmafia.adventure.AdventureResult.NonCombat("nothing", "")))
        val lines = logger.recentLines()
        assertEquals(1, lines.size)
        assertTrue(lines[0].contains("TurnConsumed"))
    }

    @Test
    fun recentLines_respectsMaxRetention() {
        val prefs = Preferences(MapSettings())
        val logger = SessionLogger(prefs, GameEventBus())
        repeat(5) {
            val loc = net.sourceforge.kolmafia.adventure.AdventureLocation("z$it", "zone", "z")
            logger.append(GameEvent.TurnConsumed(loc, net.sourceforge.kolmafia.adventure.AdventureResult.NonCombat("n", "")))
        }
        assertEquals(5, logger.recentLines().size)
    }
}
