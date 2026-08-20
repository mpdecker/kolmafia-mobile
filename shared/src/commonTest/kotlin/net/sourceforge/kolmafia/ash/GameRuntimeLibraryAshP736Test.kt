package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.TrickOrTreatChoiceSync

class GameRuntimeLibraryAshP736Test {

    @Test
    fun visit_rebuildsBlock() {
        val prefs = Preferences(MapSettings())
        val html = """
            <img src='https://images.kingdomofloathing.com/otherimages/trickortreat/house_l1.gif'>
            <img class='faded' src='https://images.kingdomofloathing.com/otherimages/trickortreat/d2.gif'>
            <img src='https://images.kingdomofloathing.com/otherimages/trickortreat/starhouse.gif'>
        """.trimIndent()
        assertTrue(TrickOrTreatChoiceSync.applyVisit(804, html, prefs))
        assertEquals("LdS", prefs.getString(TrickOrTreatChoiceSync.BLOCK_PREF, ""))
    }

    @Test
    fun preChoice_marksHouseUsed() {
        val prefs = Preferences(MapSettings())
        prefs.setString(TrickOrTreatChoiceSync.BLOCK_PREF, "LDS")
        assertTrue(
            TrickOrTreatChoiceSync.apply(
                choiceId = 804,
                preferences = prefs,
                choiceUrl = "choice.php?whichchoice=804&whichhouse=1&option=1",
            ),
        )
        assertEquals("LdS", prefs.getString(TrickOrTreatChoiceSync.BLOCK_PREF, ""))
    }

    @Test
    fun questChoiceRules_wires804() {
        val prefs = Preferences(MapSettings())
        prefs.setString(TrickOrTreatChoiceSync.BLOCK_PREF, "ABC")
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 804,
                responseText = "",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
                choiceUrl = "whichhouse=0",
            ),
        )
        assertEquals("aBC", prefs.getString(TrickOrTreatChoiceSync.BLOCK_PREF, ""))
    }

    @Test
    fun ignoresOtherChoices() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            TrickOrTreatChoiceSync.apply(
                choiceId = 1219,
                preferences = prefs,
                choiceUrl = "whichhouse=0",
            ),
        )
    }
}
