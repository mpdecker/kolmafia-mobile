package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class MerkinQuestSyncTest {

    private fun prefs() = Preferences(MapSettings())

    @Test
    fun applyFromChoice709_setsShubDefeatedAndQuestDone() {
        val prefs = prefs()
        prefs.setString("merkinQuestPath", "scholar")

        MerkinQuestSync.applyFromChoice(709, prefs, null)

        assertTrue(prefs.getBoolean("shubJigguwattDefeated", false))
        assertEquals("done", prefs.getString("merkinQuestPath", ""))
    }

    @Test
    fun applyFromChoice713_setsYogUrtDefeatedAndQuestDone() {
        val prefs = prefs()
        prefs.setString("merkinQuestPath", "gladiator")

        MerkinQuestSync.applyFromChoice(713, prefs, null)

        assertTrue(prefs.getBoolean("yogUrtDefeated", false))
        assertEquals("done", prefs.getString("merkinQuestPath", ""))
    }

    @Test
    fun applyFromChoice717_setsQuestDoneOnly() {
        val prefs = prefs()
        prefs.setString("merkinQuestPath", "scholar")
        prefs.setBoolean("shubJigguwattDefeated", false)
        prefs.setBoolean("yogUrtDefeated", false)

        MerkinQuestSync.applyFromChoice(717, prefs, null)

        assertEquals("done", prefs.getString("merkinQuestPath", ""))
        assertFalse(prefs.getBoolean("shubJigguwattDefeated", false))
        assertFalse(prefs.getBoolean("yogUrtDefeated", false))
    }

    @Test
    fun applyFromChoice_otherChoice_noOp() {
        val prefs = prefs()
        prefs.setString("merkinQuestPath", "scholar")

        MerkinQuestSync.applyFromChoice(703, prefs, null)

        assertEquals("scholar", prefs.getString("merkinQuestPath", ""))
        assertFalse(prefs.getBoolean("shubJigguwattDefeated", false))
        assertFalse(prefs.getBoolean("yogUrtDefeated", false))
    }

    @Test
    fun applyFromUrl_routesChoice709ViaUrl() {
        val prefs = prefs()

        MerkinQuestSync.applyFromUrl(
            "choice.php?whichchoice=709&option=1",
            prefs,
            null,
        )

        assertTrue(prefs.getBoolean("shubJigguwattDefeated", false))
        assertEquals("done", prefs.getString("merkinQuestPath", ""))
    }

    @Test
    fun applyFromResponse_routesChoice709ViaUrl() {
        val prefs = prefs()

        DreadScrollManager.applyFromResponse(
            url = "choice.php?whichchoice=709&option=1",
            html = "You Beat Shub to a Stub, Bub",
            preferences = prefs,
            sessionLogger = null,
            eventBus = null,
        )

        assertTrue(prefs.getBoolean("shubJigguwattDefeated", false))
        assertEquals("done", prefs.getString("merkinQuestPath", ""))
    }
}
