package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestFightRules
import net.sourceforge.kolmafia.session.BugbearManager

class GameRuntimeLibraryAshP662Test {

    @Test
    fun revision_phase665() {
        assertEquals("phase826", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun keyotron_recordsBiodataCount() {
        val prefs = Preferences(MapSettings())
        prefs.setString("statusMedbay", "0")
        assertTrue(
            BugbearManager.handleKeyotron(
                html = "Your key-o-tron emits 2 short tones, indicating that it has successfully processed biometric data from this subject.",
                monsterName = "hypodermic bugbear",
                preferences = prefs,
            ),
        )
        assertEquals("2", prefs.getString("statusMedbay"))
    }

    @Test
    fun keyotron_thresholdOpensCurrentLevel() {
        val prefs = Preferences(MapSettings())
        prefs.setString("statusMedbay", "2")
        prefs.setInt("mothershipProgress", 0)
        assertTrue(
            BugbearManager.handleKeyotron(
                html = "Your key-o-tron emits a short buzz, indicating that it has already collected enough biometric data of this type.",
                monsterName = "hypodermic bugbear",
                preferences = prefs,
            ),
        )
        assertEquals("open", prefs.getString("statusMedbay"))
    }

    @Test
    fun keyotron_thresholdUnlocksFutureLevel() {
        val prefs = Preferences(MapSettings())
        prefs.setString("statusScienceLab", "5")
        prefs.setInt("mothershipProgress", 0)
        val data = BugbearManager.bugbearToData("bugbear scientist")
        BugbearManager.setBiodata(data, 6, prefs)
        assertEquals("unlocked", prefs.getString("statusScienceLab"))
    }

    @Test
    fun applyCombat_wiresKeyotronWithoutWin() {
        val prefs = Preferences(MapSettings())
        prefs.setString("statusSonar", "0")
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestFightRules.applyCombat(
                db,
                monster = "batbugbear",
                won = false,
                preferences = prefs,
                responseText = "Your key-o-tron emits 1 short tone.",
            ).advanced,
        )
        assertEquals("1", prefs.getString("statusSonar"))
    }

    @Test
    fun withoutKeyotron_isNoOp() {
        val prefs = Preferences(MapSettings())
        prefs.setString("statusMedbay", "0")
        assertFalse(
            BugbearManager.handleKeyotron(
                html = "You win the fight!",
                monsterName = "hypodermic bugbear",
                preferences = prefs,
            ),
        )
        assertEquals("0", prefs.getString("statusMedbay"))
    }
}
