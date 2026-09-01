package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.TwinPeakChoiceSync

class GameRuntimeLibraryAshP670Test {

    @Test
    fun revision_phase671() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun room237_setsBitOne() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            TwinPeakChoiceSync.apply(
                TwinPeakChoiceSync.ROOM_237,
                "You take a moment to steel your nerves.",
                prefs,
            ),
        )
        assertEquals(1, prefs.getInt(TwinPeakChoiceSync.PREF))
    }

    @Test
    fun goCheckItOut_orsBitTwo() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(TwinPeakChoiceSync.PREF, 1)
        TwinPeakChoiceSync.apply(
            TwinPeakChoiceSync.GO_CHECK_IT_OUT,
            "All work and no play makes Jack a dull boy.",
            prefs,
        )
        assertEquals(3, prefs.getInt(TwinPeakChoiceSync.PREF))
    }

    @Test
    fun armChoice_orsBitFourAndConsumesOil() {
        val prefs = Preferences(MapSettings())
        val consumed = mutableListOf<Pair<Int, Int>>()
        TwinPeakChoiceSync.apply(
            TwinPeakChoiceSync.HE_IS_THE_ARM,
            "You attempt to mingle with the crowd.",
            prefs,
            consumeItem = { id, qty -> consumed.add(id to qty) },
        )
        assertEquals(4, prefs.getInt(TwinPeakChoiceSync.PREF))
        assertTrue(consumed.contains(TwinPeakChoiceSync.JAR_OF_OIL to 1))
    }

    @Test
    fun nowItsDark_setsFifteen() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(TwinPeakChoiceSync.PREF, 7)
        TwinPeakChoiceSync.apply(
            TwinPeakChoiceSync.NOW_ITS_DARK,
            "When the lights come back, the hotel is gone.",
            prefs,
        )
        assertEquals(15, prefs.getInt(TwinPeakChoiceSync.PREF))
    }

    @Test
    fun cabinFever_setsFifteen() {
        val prefs = Preferences(MapSettings())
        TwinPeakChoiceSync.apply(
            TwinPeakChoiceSync.CABIN_FEVER,
            "mercifully, the hotel explodes around you.",
            prefs,
        )
        assertEquals(15, prefs.getInt(TwinPeakChoiceSync.PREF))
    }

    @Test
    fun questChoiceRules_wiresChoice607() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = TwinPeakChoiceSync.ROOM_237,
                responseText = "You take a moment to steel your nerves.",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals(1, prefs.getInt(TwinPeakChoiceSync.PREF))
    }
}
