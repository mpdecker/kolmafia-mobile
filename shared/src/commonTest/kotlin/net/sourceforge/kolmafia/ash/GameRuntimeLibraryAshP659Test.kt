package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestItemUsedSync
import net.sourceforge.kolmafia.session.CryptManager

class GameRuntimeLibraryAshP659Test {

    @Test
    fun revision_phase659() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun choice523_decreasesCrannyByEleven() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("cyrptCrannyEvilness", 40)
        prefs.setInt("cyrptTotalEvilness", 80)
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 523,
                responseText = "Your Evilometer beeps 11 times.",
                questDatabase = db,
                decision = 5,
                preferences = prefs,
            ),
        )
        assertEquals(29, prefs.getInt("cyrptCrannyEvilness"))
        assertEquals(69, prefs.getInt("cyrptTotalEvilness"))
        assertTrue(prefs.getBoolean("candyCaneSwordDefiledCranny", false))
    }

    @Test
    fun choice523_withoutBeepLeavesEvilness() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("cyrptCrannyEvilness", 40)
        prefs.setInt("cyrptTotalEvilness", 80)
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 523,
                responseText = "You jam your candy cane sword into the hole.",
                questDatabase = db,
                decision = 5,
                preferences = prefs,
            ),
        )
        assertEquals(40, prefs.getInt("cyrptCrannyEvilness"))
        assertTrue(prefs.getBoolean("candyCaneSwordDefiledCranny", false))
    }

    @Test
    fun evilEye_decreasesNookByThreeTimesCountCapped() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("cyrptNookEvilness", 8)
        prefs.setInt("cyrptTotalEvilness", 8)
        assertTrue(
            CryptManager.applyEvilEye(
                "Your Evilometer emits three quick beeps",
                count = 4,
                preferences = prefs,
            ),
        )
        assertEquals(0, prefs.getInt("cyrptNookEvilness"))
        assertEquals(999, prefs.getInt("cyrptTotalEvilness"))
    }

    @Test
    fun questItemUsedSync_wiresEvilEyeCount() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("cyrptNookEvilness", 20)
        prefs.setInt("cyrptTotalEvilness", 40)
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestItemUsedSync.apply(
                CryptManager.EVIL_EYE,
                "Evilometer emits three quick beeps",
                db,
                prefs,
                count = 2,
            ),
        )
        assertEquals(14, prefs.getInt("cyrptNookEvilness"))
        assertEquals(34, prefs.getInt("cyrptTotalEvilness"))
    }

    @Test
    fun evilEye_withoutBeepIsNoOp() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("cyrptNookEvilness", 20)
        assertFalse(CryptManager.applyEvilEye("Nothing happens.", 3, prefs))
        assertEquals(20, prefs.getInt("cyrptNookEvilness"))
    }
}
