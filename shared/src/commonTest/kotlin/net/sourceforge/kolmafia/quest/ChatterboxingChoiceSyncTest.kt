package net.sourceforge.kolmafia.quest

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.banish.BanishManager
import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences

class ChatterboxingChoiceSyncTest {

    @Test
    fun banishesChattyPirateOnTrinketChoice() {
        val prefs = Preferences(MapSettings())
        val manager = BanishManager(prefs)
        val applied = ChatterboxingChoiceSync.apply(
            choiceId = 191,
            decision = 2,
            responseText = "You find a valuable trinket that looks promising.",
            banishManager = manager,
            currentTurn = 10,
        )
        assertTrue(applied)
        assertTrue(manager.isBanished("chatty pirate", 10))
    }

    @Test
    fun ignoresWrongDecision() {
        val prefs = Preferences(MapSettings())
        val manager = BanishManager(prefs)
        assertFalse(
            ChatterboxingChoiceSync.apply(
                choiceId = 191,
                decision = 1,
                responseText = "You find a valuable trinket that looks promising.",
                banishManager = manager,
            ),
        )
    }
}
