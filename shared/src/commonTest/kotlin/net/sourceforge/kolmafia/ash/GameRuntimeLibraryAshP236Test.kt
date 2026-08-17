package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.BastilleBattalionSync

class GameRuntimeLibraryAshP236Test {

    @Test
    fun revision_isphase222() {
        assertEquals("phase550", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun processVisitQuestHooks_doesNotThrowOnUnrelatedHtml() {
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        lib.processVisitQuestHooks(
            html = "<html><body>nothing here</body></html>",
            url = "choice.php?whichchoice=1003",
        )
    }

    @Test
    fun bastilleChoiceIds_matchDesktop() {
        assertEquals(1313, BastilleBattalionSync.CHOICE_RIG)
        assertEquals(1319, BastilleBattalionSync.CHOICE_CHEESE_SEEKING)
    }
}
