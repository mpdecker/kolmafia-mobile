package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.VykeaChoiceSync
import net.sourceforge.kolmafia.request.VykeaChoiceMapper
import net.sourceforge.kolmafia.vykea.VykeaCompanionManager

class GameRuntimeLibraryAshP728Test {

    @Test
    fun revision_phase814() {
        assertEquals("phase814", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun start_consumesPlanksAndInstructions() {
        val prefs = Preferences(MapSettings())
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            VykeaChoiceSync.apply(
                choiceId = 1120,
                decision = 1,
                html = "",
                preferences = prefs,
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertTrue(consumed.contains(VykeaChoiceMapper.PLANK_ID to 5))
        assertTrue(consumed.contains(VykeaChoiceMapper.INSTRUCTIONS_ID to 1))
        assertEquals(0, prefs.getInt(VykeaCompanionManager.LEVEL_PREF, -1))
    }

    @Test
    fun rune_setsFrenzy() {
        val prefs = Preferences(MapSettings())
        var consumedId = -1
        assertTrue(
            VykeaChoiceSync.apply(
                choiceId = 1121,
                decision = 1,
                html = "",
                preferences = prefs,
                consumeItem = { id, _ -> consumedId = id },
            ),
        )
        assertEquals(VykeaChoiceMapper.FRENZY_RUNE_ID, consumedId)
        assertEquals("frenzy", prefs.getString(VykeaCompanionManager.RUNE_PREF, ""))
    }

    @Test
    fun dowels_setLevel() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            VykeaChoiceSync.apply(
                choiceId = 1122,
                decision = 2,
                html = "",
                preferences = prefs,
                consumeItem = { _, _ -> },
            ),
        )
        assertEquals(3, prefs.getInt(VykeaCompanionManager.LEVEL_PREF, 0))
    }

    @Test
    fun finish_parsesCompanion() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(VykeaCompanionManager.LEVEL_PREF, 3)
        prefs.setString(VykeaCompanionManager.RUNE_PREF, "blood")
        val html = """
            <span class='guts'>You admire your new... lamp. It's a lamp!<p>
            You decide to name it... <b>ÅVOBÉ</b></span>
        """.trimIndent()
        assertTrue(
            VykeaChoiceSync.apply(
                choiceId = 1123,
                decision = 2,
                html = html,
                preferences = prefs,
                consumeItem = { _, _ -> },
            ),
        )
        assertEquals("ÅVOBÉ", prefs.getString(VykeaCompanionManager.NAME_PREF, ""))
        assertEquals("lamp", prefs.getString(VykeaCompanionManager.TYPE_PREF, ""))
        assertTrue(prefs.getString(VykeaCompanionManager.CURRENT_VYKEA_PREF, "").contains("lamp"))
    }

    @Test
    fun questChoiceRules_wires1120() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1120,
                responseText = "",
                questDatabase = db,
                decision = 2,
                preferences = prefs,
            ),
        )
        assertEquals("", prefs.getString(VykeaCompanionManager.TYPE_PREF, "x"))
    }

    @Test
    fun ignoresOtherChoices() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            VykeaChoiceSync.apply(
                choiceId = 1219,
                decision = 1,
                html = "",
                preferences = prefs,
            ),
        )
    }
}
