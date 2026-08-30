package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.CanadianWildlifeChoiceSync
import net.sourceforge.kolmafia.quest.LatteChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP828Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun latteVisit_parsesRefillsAndUnlocks() {
        val prefs = Preferences(MapSettings())
        val html = """
            You've got <b>2</b> refill left today.
            <tr style="whatever"><td><input type=radio name="l1" value="1"> Cinna- </td></tr>
            <tr style="whatever"><td><input type=radio name="l1" value="2"> Autumnal </td>&Dagger;</tr>
            <tr style="whatever"><td><input type=radio name="l1" checked value="3"> Vanilla </td></tr>
        """.trimIndent()
        assertTrue(LatteChoiceSync.applyVisit(1329, html, prefs))
        assertEquals(1, prefs.getInt("_latteRefillsUsed", -1))
        assertEquals("cinnamon,vanilla", prefs.getString("latteUnlocks", ""))
    }

    @Test
    fun latteFill_setsIngredientsModifiersAndFlags() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_latteRefillsUsed", 0)
        prefs.setBoolean("_latteBanishUsed", true)
        prefs.setBoolean("_latteCopyUsed", true)
        prefs.setBoolean("_latteDrinkUsed", true)
        val html = """
            <span>You get your mug filled with a delicious Cinna- pumpkin spice Latte with a shot of vanilla.</span>
        """.trimIndent()
        assertTrue(LatteChoiceSync.apply(1329, 1, html, prefs))
        assertEquals("cinnamon,pumpkin,vanilla", prefs.getString("latteIngredients", ""))
        assertTrue(prefs.getString("latteModifier", "").contains("Experience (Moxie): 1"))
        assertTrue(prefs.getString("latteModifier", "").contains("Experience (Mysticality): 1"))
        assertTrue(prefs.getString("latteModifier", "").contains("Weapon Damage Percent: 5"))
        assertEquals(1, prefs.getInt("_latteRefillsUsed", -1))
        assertFalse(prefs.getBoolean("_latteBanishUsed", true))
        assertFalse(prefs.getBoolean("_latteCopyUsed", true))
        assertFalse(prefs.getBoolean("_latteDrinkUsed", true))
    }

    @Test
    fun canadian1332_consumesForm() {
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(CanadianWildlifeChoiceSync.apply(1332, 1) { id, qty -> consumed += id to qty })
        assertEquals(listOf(CanadianWildlifeChoiceSync.GOVERNMENT_REQUISITION_FORM to 1), consumed)
    }

    @Test
    fun canadian1333_decisionConsumes() {
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(CanadianWildlifeChoiceSync.apply(1333, 2) { id, qty -> consumed += id to qty })
        assertTrue(CanadianWildlifeChoiceSync.apply(1333, 3) { id, qty -> consumed += id to qty })
        assertTrue(CanadianWildlifeChoiceSync.apply(1333, 4) { id, qty -> consumed += id to qty })
        assertEquals(
            listOf(
                CanadianWildlifeChoiceSync.MOOSEFLANK to 1,
                CanadianWildlifeChoiceSync.WALRUS_BLUBBER to 10,
                CanadianWildlifeChoiceSync.TINY_BOMB to 10,
            ),
            consumed,
        )
    }

    @Test
    fun questChoiceRules_wires1332And1329() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1332,
                responseText = "",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
            ),
        )
        val latteHtml = """
            <span>You get your mug filled with a delicious Cinna- pumpkin spice Latte with a shot of vanilla.</span>
        """.trimIndent()
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1329,
                responseText = latteHtml,
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals("cinnamon,pumpkin,vanilla", prefs.getString("latteIngredients", ""))
    }
}
