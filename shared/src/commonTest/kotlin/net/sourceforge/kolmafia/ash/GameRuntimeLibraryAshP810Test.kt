package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.ClanFortuneChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP810Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase2450", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun post_setsBuffUsedWhenNotAvailable() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            ClanFortuneChoiceSync.apply(
                choiceId = 1278,
                html = "Relationship Fortune Teller — fortune complete",
                preferences = prefs,
                choiceUrl = "choice.php?whichchoice=1278",
            ),
        )
        assertEquals(true, prefs.getBoolean("_clanFortuneBuffUsed", false))
    }

    @Test
    fun post_availableStillFalse() {
        val prefs = Preferences(MapSettings())
        ClanFortuneChoiceSync.apply(
            choiceId = 1278,
            html = "Relationship Fortune Teller — ask a resident of Seaside Town",
            preferences = prefs,
            choiceUrl = "choice.php",
        )
        assertEquals(false, prefs.getBoolean("_clanFortuneBuffUsed", false))
    }

    @Test
    fun questChoiceRules_wires1278() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1278,
                responseText = "Relationship Fortune Teller done",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
                choiceUrl = "choice.php?whichchoice=1278",
            ),
        )
        assertEquals(true, prefs.getBoolean("_clanFortuneBuffUsed", false))
    }
}
