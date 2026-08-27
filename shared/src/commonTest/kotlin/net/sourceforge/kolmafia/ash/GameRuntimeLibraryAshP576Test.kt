package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.WhiteCitadelSync

class GameRuntimeLibraryAshP576Test {

    @Test
    fun revision_phase605() {
        assertEquals("phase2450", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun whiteysGrove_sign_setsCitadelStep1() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            WhiteCitadelSync.applyFromAdventure(
                adventureId = "100",
                html = "You find a NC: It's A Sign!",
                questDatabase = db,
            ),
        )
        assertEquals("step1", db.getProgress(Quest.CITADEL))
    }

    @Test
    fun whiteCitadel_default_setsStep2() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.CITADEL, "step1")
        assertTrue(
            WhiteCitadelSync.applyFromAdventure(
                adventureId = "413",
                html = "You wander the Road to the White Citadel.",
                questDatabase = db,
            ),
        )
        assertEquals("step2", db.getProgress(Quest.CITADEL))
    }

    @Test
    fun whiteCitadel_bluesBrothers_setsStep3() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            WhiteCitadelSync.applyFromAdventure(
                adventureId = "413",
                html = "I Guess They Were the Existential Blues Brothers",
                questDatabase = db,
            ),
        )
        assertEquals("step3", db.getProgress(Quest.CITADEL))
    }
}
