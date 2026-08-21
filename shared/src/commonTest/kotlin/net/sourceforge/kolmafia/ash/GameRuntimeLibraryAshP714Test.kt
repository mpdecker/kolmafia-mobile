package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.OracleChoiceSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP714Test {

    @Test
    fun revision_phase814() {
        assertEquals("phase814", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun decision2_resetsQuestAndConsumesSpoon() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.ORACLE, QuestDatabase.STARTED)
        prefs.setString("sourceOracleTarget", "a kitchen")
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            OracleChoiceSync.apply(
                choiceId = 1190,
                decision = 2,
                html = "",
                questDatabase = db,
                preferences = prefs,
                consumeItem = { id, qty -> consumed.add(id to qty) },
            ),
        )
        assertEquals(listOf(OracleChoiceSync.NO_SPOON to 1), consumed)
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.ORACLE))
        assertEquals(1, prefs.getInt("sourceEnlightenment", 0))
        assertEquals("", prefs.getString("sourceOracleTarget"))
    }

    @Test
    fun decision1_startsQuestAndParsesTarget() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            OracleChoiceSync.apply(
                choiceId = 1190,
                decision = 1,
                html = """don't remember leaving any spoons in The Haunted Kitchen&quot;""",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.ORACLE))
        assertEquals("The Haunted Kitchen", prefs.getString("sourceOracleTarget"))
    }

    @Test
    fun questChoiceRules_wires1190() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1190,
                responseText = """don't remember leaving any spoons in The Haunted Bathroom&quot;""",
                questDatabase = db,
                decision = 3,
                preferences = prefs,
            ),
        )
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.ORACLE))
        assertEquals("The Haunted Bathroom", prefs.getString("sourceOracleTarget"))
    }
}
