package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.BodyguardChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP780Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun challenge_setsMonsterAndResetsCharge() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("bodyguardCharge", 40)
        assertTrue(
            BodyguardChoiceSync.apply(
                choiceId = 1532,
                html = "You set off to find a monster with a specific bodyguard to challenge.",
                preferences = prefs,
                choiceUrl = "bgid=123",
                monsterNameForId = { id -> if (id == 123) "Scary Monster" else null },
            ),
        )
        assertEquals(0, prefs.getInt("bodyguardCharge", -1))
        assertEquals("Scary Monster", prefs.getString("bodyguardChatMonster", ""))
    }

    @Test
    fun withoutChallengeText_noop() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            BodyguardChoiceSync.apply(
                1532,
                "chat only",
                prefs,
                choiceUrl = "bgid=1",
                monsterNameForId = { "X" },
            ),
        )
    }

    @Test
    fun questChoiceRules_wires1532() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("bodyguardCharge", 10)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1532,
                responseText = "You set off to find a monster with a specific bodyguard to challenge.",
                questDatabase = QuestDatabase(prefs),
                preferences = prefs,
                choiceUrl = "bgid=42",
                monsterNameForId = { if (it == 42) "Test Guard" else null },
            ),
        )
        assertEquals(0, prefs.getInt("bodyguardCharge", -1))
        assertEquals("Test Guard", prefs.getString("bodyguardChatMonster", ""))
    }
}
