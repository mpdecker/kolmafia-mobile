package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.VillainLairChoiceSync

class GameRuntimeLibraryAshP726Test {

    @Test
    fun revision_phase814() {
        assertEquals("phase814", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_setsKeyFromBoris() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            VillainLairChoiceSync.applyVisit(
                1261,
                "Which Door? Boris would approve.",
                prefs,
            ),
        )
        assertEquals("boris", prefs.getString("_villainLairKey", ""))
    }

    @Test
    fun decision1_dropsProgress() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_villainLairProgress", 5)
        assertTrue(
            VillainLairChoiceSync.apply(
                choiceId = 1261,
                decision = 1,
                html = "You drop 1000 Meat and the door opens.",
                preferences = prefs,
            ),
        )
        assertEquals(15, prefs.getInt("_villainLairProgress", 0))
        assertTrue(prefs.getBoolean("_villainLairDoorChoiceUsed"))
    }

    @Test
    fun decision2_consumesKey() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_villainLairKey", "boris")
        prefs.setInt("_villainLairProgress", 0)
        var consumedId = -1
        var consumedQty = 0
        assertTrue(
            VillainLairChoiceSync.apply(
                choiceId = 1261,
                decision = 2,
                html = "You insert the key and the door opens.",
                preferences = prefs,
                consumeItem = { id, qty ->
                    consumedId = id
                    consumedQty = qty
                },
            ),
        )
        assertEquals(VillainLairChoiceSync.BORIS_KEY, consumedId)
        assertEquals(1, consumedQty)
        assertEquals(15, prefs.getInt("_villainLairProgress", 0))
        assertTrue(prefs.getBoolean("_villainLairDoorChoiceUsed"))
    }

    @Test
    fun decision3_decrementsProgress() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_villainLairProgress", 20)
        assertTrue(
            VillainLairChoiceSync.apply(
                choiceId = 1261,
                decision = 3,
                html = "You force the door.",
                preferences = prefs,
            ),
        )
        assertEquals(7, prefs.getInt("_villainLairProgress", 0))
        assertTrue(prefs.getBoolean("_villainLairDoorChoiceUsed"))
    }

    @Test
    fun questChoiceRules_wires1261() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_villainLairProgress", 0)
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1261,
                responseText = "You drop 1000 Meat and the door opens.",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals(10, prefs.getInt("_villainLairProgress", 0))
        assertTrue(prefs.getBoolean("_villainLairDoorChoiceUsed"))
    }

    @Test
    fun ignoresOtherChoices() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            VillainLairChoiceSync.apply(
                choiceId = 1219,
                decision = 1,
                html = "You drop 1000 Meat",
                preferences = prefs,
            ),
        )
    }
}
