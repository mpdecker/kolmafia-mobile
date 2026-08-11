package net.sourceforge.kolmafia.data

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

class PokefamMoveRegistryTest {

    @AfterTest
    fun tearDown() {
        PokefamMoveRegistry.resetForTest()
    }

    @Test
    fun moveToAction_returnsSeededMapping() {
        assertEquals("bite", PokefamMoveRegistry.moveToAction(1, "Bite"))
        assertEquals("tackle", PokefamMoveRegistry.moveToAction(2, "Tackle"))
        assertEquals("ult_impale", PokefamMoveRegistry.moveToAction(3, "Deluxe Impale"))
    }

    @Test
    fun registerMove_logsNewMapping() {
        val prefs = Preferences(MapSettings())
        val logger = SessionLogger(prefs, GameEventBus())
        PokefamMoveRegistry.registerMove(
            slot = 1,
            move = "New Move",
            action = "newmove",
            description = "Test description",
            sessionLogger = logger,
        )
        assertEquals("newmove", PokefamMoveRegistry.moveToAction(1, "New Move"))
        val log = prefs.getString(SessionLogger.SESSION_LOG_KEY, "")
        assertEquals(true, log.contains("Pokefam move1 'New Move' -> 'newmove'"))
    }

    @Test
    fun registerMove_skipsDuplicateAction() {
        val prefs = Preferences(MapSettings())
        val logger = SessionLogger(prefs, GameEventBus())
        PokefamMoveRegistry.registerMove(1, "Bite", "bite", "Deal damage", logger)
        assertEquals("", prefs.getString(SessionLogger.SESSION_LOG_KEY, ""))
    }

    @Test
    fun registerMove_skipsWhenActionMissing() {
        val prefs = Preferences(MapSettings())
        val logger = SessionLogger(prefs, GameEventBus())
        PokefamMoveRegistry.registerMove(1, "Mystery", null, "desc", logger)
        assertNull(PokefamMoveRegistry.moveToAction(1, "Mystery"))
    }
}
