package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.VoteBallotChoiceSync

class GameRuntimeLibraryAshP807Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_parsesLocalsAndMonsters() {
        val prefs = Preferences(MapSettings())
        val html = """
            <label><input type="radio" value="0" class="locals" /> Foo<br /><span style="color: blue">Muscle +5</span><br /></label>
            <p><input type='radio' name='g' value='0' /> <b>Leader</b> of the Pork Elf Historical Preservation Party<br><blockquote>strict curtailing of unnatural modern technologies</blockquote>
        """.trimIndent()
        assertTrue(
            VoteBallotChoiceSync.applyVisit(
                choiceId = 1331,
                html = html,
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getString("_voteLocal1", "").isNotBlank())
        assertEquals("government bureaucrat", prefs.getString("_voteMonster1", ""))
    }

    @Test
    fun post_mergesVoteModifier() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_voteLocal1", "Muscle: +5")
        prefs.setString("_voteLocal2", "Moxie: +3")
        val logs = mutableListOf<String>()
        assertTrue(
            VoteBallotChoiceSync.apply(
                choiceId = 1331,
                decision = 1,
                html = "ok",
                preferences = prefs,
                choiceUrl = "choice.php?whichchoice=1331&option=1&local[]=0&local[]=1",
                sessionLog = { logs += it },
            ),
        )
        assertEquals("Muscle: +5, Moxie: +3", prefs.getString("_voteModifier", ""))
        assertEquals(listOf("You have cast your votes"), logs)
    }

    @Test
    fun questChoiceRules_wires1331() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_voteLocal1", "Item Drop: +10")
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1331,
                responseText = "voted",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
                choiceUrl = "choice.php?option=1&local[]=0",
            ),
        )
        assertEquals("Item Drop: +10", prefs.getString("_voteModifier", ""))
    }
}
