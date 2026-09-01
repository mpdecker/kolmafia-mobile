package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.PlumberShopChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP756Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_costumeInfersWorn() {
        val prefs = Preferences(MapSettings())
        val html = """
            <form><input name=option value=1 type=submit value="Gardener Costume">50 coins</form>
            <form><input name=option value=2 type=submit value="Ballerina Costume">50 coins</form>
        """.trimIndent()
        assertTrue(PlumberShopChoiceSync.applyVisit(1407, html, prefs))
        assertEquals(50, prefs.getInt(PlumberShopChoiceSync.COSTUME_COST_PREF, 0))
        assertEquals("muscle", prefs.getString(PlumberShopChoiceSync.COSTUME_WORN_PREF, ""))
    }

    @Test
    fun post_costumeConsumesCoins() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(PlumberShopChoiceSync.COSTUME_COST_PREF, 50)
        var coins = 0
        assertTrue(
            PlumberShopChoiceSync.apply(
                1407,
                "You slip into something a little more carpentable",
                prefs,
                consumeItem = { id, qty ->
                    assertEquals(PlumberShopChoiceSync.COIN, id)
                    coins = qty
                },
            ),
        )
        assertEquals(50, coins)
        assertEquals(100, prefs.getInt(PlumberShopChoiceSync.COSTUME_COST_PREF, 0))
        assertEquals("muscle", prefs.getString(PlumberShopChoiceSync.COSTUME_WORN_PREF, ""))
    }

    @Test
    fun post_badgeIncrementsCost() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(PlumberShopChoiceSync.BADGE_COST_PREF, 25)
        assertTrue(
            PlumberShopChoiceSync.apply(1408, "You acquire a skill: Jumpman", prefs),
        )
        assertEquals(50, prefs.getInt(PlumberShopChoiceSync.BADGE_COST_PREF, 0))
    }

    @Test
    fun questChoiceRules_wires1407() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(PlumberShopChoiceSync.COSTUME_COST_PREF, 10)
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1407,
                responseText = "Todge holds out a tutu and you jump into it",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals("moxie", prefs.getString(PlumberShopChoiceSync.COSTUME_WORN_PREF, ""))
    }

    @Test
    fun ignoresOtherChoices() {
        assertFalse(PlumberShopChoiceSync.apply(1449, "You acquire a skill", Preferences(MapSettings())))
    }
}
