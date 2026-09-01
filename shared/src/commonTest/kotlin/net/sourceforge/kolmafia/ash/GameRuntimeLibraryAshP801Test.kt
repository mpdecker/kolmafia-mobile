package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.BoomBoxChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP801Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_parsesSongsLeftAndCurrent() {
        val prefs = Preferences(MapSettings())
        val html = """
            you can do <b>7</b> more
            &quot;Food Vibrations&quot; (Keep playing)
            &quot;Eye of the Giger&quot;
        """.trimIndent()
        assertTrue(
            BoomBoxChoiceSync.applyVisit(
                choiceId = 1312,
                html = html,
                preferences = prefs,
            ),
        )
        assertEquals(7, prefs.getInt("_boomBoxSongsLeft", 0))
        assertEquals("Food Vibrations", prefs.getString("boomBoxSong", ""))
    }

    @Test
    fun post_setsSongAndDecrements() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_boomBoxSongsLeft", 5)
        prefs.setString("boomBoxSong", "")
        val learned = mutableListOf<Int>()
        val logs = mutableListOf<String>()
        assertTrue(
            BoomBoxChoiceSync.apply(
                choiceId = 1312,
                decision = 1,
                html = "ok",
                preferences = prefs,
                learnSkill = { learned += it },
                sessionLog = { logs += it },
            ),
        )
        assertEquals("Eye of the Giger", prefs.getString("boomBoxSong", ""))
        assertEquals(4, prefs.getInt("_boomBoxSongsLeft", 0))
        assertEquals(listOf(BoomBoxChoiceSync.SING_ALONG_SKILL_ID), learned)
        assertEquals(listOf("Setting soundtrack to Eye of the Giger"), logs)
    }

    @Test
    fun questChoiceRules_wires1312() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_boomBoxSongsLeft", 3)
        prefs.setString("boomBoxSong", "Food Vibrations")
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1312,
                responseText = "changed",
                questDatabase = QuestDatabase(prefs),
                decision = 6,
                preferences = prefs,
            ),
        )
        assertEquals("", prefs.getString("boomBoxSong", "x"))
        assertEquals(0, prefs.getInt("skillLevel7297", 1))
    }
}
