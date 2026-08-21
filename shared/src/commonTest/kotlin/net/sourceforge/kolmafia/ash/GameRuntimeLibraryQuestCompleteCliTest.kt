package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.campground.MushroomPlotSync
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.LightsOutChoiceSync
import net.sourceforge.kolmafia.quest.TavernCellarSync

/** Phases 1033–1042 quest-complete CLI corpus. */
class GameRuntimeLibraryQuestCompleteCliTest {

    @Test
    fun tavern_reportsFaucetSquare() {
        val p = Preferences(MapSettings())
        p.setString("tavernLayout", "0000000000003000000000000")
        val lib = GameRuntimeLibrary(preferences = p, character = KoLCharacter())
        val out = outputLib(lib, """cli_execute("tavern");""")
        assertTrue(out.contains("Faucet found"), out)
        assertTrue(out.contains("square 13"), out)
    }

    @Test
    fun baron_reportsEmptyMansion() {
        val p = Preferences(MapSettings())
        p.setString("tavernLayout", "0000000000000006000000000")
        val lib = GameRuntimeLibrary(preferences = p, character = KoLCharacter())
        val out = outputLib(lib, """cli_execute("baron");""")
        assertTrue(out.contains("empty mansion"), out)
        assertTrue(out.contains("already defeated"), out)
    }

    @Test
    fun gourd_status_printsNeededCount() {
        val p = Preferences(MapSettings())
        p.setInt("gourdItemCount", 7)
        val lib = GameRuntimeLibrary(preferences = p, character = KoLCharacter())
        val out = outputLib(lib, """cli_execute("gourd");""")
        assertTrue(out.contains("7"), out)
        assertTrue(out.contains("Usage: gourd"), out)
    }

    @Test
    fun dvorak_status_stub() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("dvorak");""")
        assertTrue(out.contains("not ported"), out)
    }

    @Test
    fun sven_usage_withoutArgs() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("sven");""")
        assertTrue(out.contains("sven member=item"), out)
    }

    @Test
    fun basement_printsLevel() {
        val p = Preferences(MapSettings())
        p.setInt("basementLevel", 42)
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("basement");""")
        assertTrue(out.contains("Level 42"), out)
    }

    @Test
    fun nemesis_strips_listsPrefs() {
        val p = Preferences(MapSettings())
        p.setString("lastPaperStrip4139", "left:WORD:right")
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("nemesis strips");""")
        assertTrue(out.contains("WORD"), out)
        assertTrue(out.contains("creased") || out.contains("4139") || out.contains("="), out)
    }

    @Test
    fun nemesis_password_fromLinkedStrips() {
        val p = Preferences(MapSettings())
        // Chain: a->b->c ... using eight strips left:CODE:right
        val chain = listOf(
            3144 to "a:ONE:b",
            4138 to "b:TWO:c",
            4139 to "c:THREE:d",
            4140 to "d:FOUR:e",
            4141 to "e:FIVE:f",
            4142 to "f:SIX:g",
            4143 to "g:SEVEN:h",
            4144 to "h:EIGHT:i",
        )
        for ((id, value) in chain) {
            p.setString("lastPaperStrip$id", value)
        }
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("nemesis password");""")
        assertTrue(out.contains("ONETWOTHREEFOURFIVESIXSEVENEIGHT"), out)
    }

    @Test
    fun spookyraven_report() {
        val p = Preferences(MapSettings())
        p.setString(LightsOutChoiceSync.ELIZABETH_PREF, "The Haunted Kitchen")
        p.setString(LightsOutChoiceSync.STEPHEN_PREF, "none")
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("spookyraven");""")
        assertTrue(out.contains("Elizabeth will next show up in The Haunted Kitchen"), out)
        assertTrue(out.contains("defeated Stephen"), out)
    }

    @Test
    fun taleofdread_syntaxError() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("taleofdread hot");""")
        assertTrue(out.contains("Syntax: taleofdread"), out)
    }

    @Test
    fun field_printsPlotGrid() {
        val p = Preferences(MapSettings())
        p.setInt("lastMushroomPlot", 0)
        p.setString(MushroomPlotSync.MUSHROOM_PLOT_SQUARES_PREF, "kb".repeat(16))
        val lib = GameRuntimeLibrary(preferences = p, character = KoLCharacter())
        val out = outputLib(lib, """cli_execute("field");""")
        assertTrue(out.contains("Current:"), out)
        assertTrue(out.contains("kb"), out)
    }

    @Test
    fun help_listsQuestCompleteVerbs() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("help tavern");""")
        assertTrue(out.contains("tavern"), out)
        val out2 = outputLib(lib, """cli_execute("help nemesis");""")
        assertTrue(out2.contains("nemesis"), out2)
        val out3 = outputLib(lib, """cli_execute("help spookyraven");""")
        assertTrue(out3.contains("spookyraven"), out3)
    }

    @Test
    fun tavernLayout_helperKeepsLength() {
        val p = Preferences(MapSettings())
        val layout = TavernCellarSync.tavernLayout(p, 1)
        assertTrue(layout.length == 25, layout)
    }
}
