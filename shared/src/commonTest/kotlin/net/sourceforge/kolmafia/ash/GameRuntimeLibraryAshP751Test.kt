package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.PowerPlantChoiceSync

class GameRuntimeLibraryAshP751Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_parsesStalks() {
        val prefs = Preferences(MapSettings())
        val html = """
            <button class=button name="pp" value="1" type=submit>
            <img src="https://example.com/otherimages/powerplant/3.png">
            <button class=button name="pp" value="3" type=submit>
            <img src="https://example.com/otherimages/powerplant/7.png">
        """.trimIndent()
        assertTrue(PowerPlantChoiceSync.applyVisit(1448, html, prefs))
        val parts = prefs.getString(PowerPlantChoiceSync.STALKS_PREF, "").split(",")
        assertEquals(7, parts.size)
        assertEquals("3", parts[0])
        assertEquals("7", parts[2])
    }

    @Test
    fun visit_ignoresOtherChoices() {
        assertFalse(
            PowerPlantChoiceSync.applyVisit(
                1406,
                """<button name="pp" value="1"><img src="/otherimages/powerplant/1.png">""",
                Preferences(MapSettings()),
            ),
        )
    }
}
