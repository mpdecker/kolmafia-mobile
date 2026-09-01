package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.clan.ClanManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.ProfileRequest

class GameRuntimeLibraryAshP722Test {

    @BeforeTest
    fun reset() {
        ClanManager.resetForTest()
    }

    @AfterTest
    fun cleanup() {
        ClanManager.resetForTest()
    }

    private val fameClanHtml = """
        <b>(Hardcore)</b></td>
        Fame</td><td>1234</td>
        Clan: <b><a class=nounder href="showclan.php?whichclan=42">Loathing Legion</a>
    """.trimIndent()

    @Test
    fun revision_phase848() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun parse_fameAndClanFromShowplayerHtml() {
        val profile = ProfileRequest.parse(fameClanHtml, "Hero", "99")
        assertEquals(1234, profile.pvpRank)
        assertEquals(42, profile.clanId)
        assertEquals("Loathing Legion", profile.clanName)
        assertEquals(true, profile.isHardcore)
    }

    @Test
    fun parse_missingFameIsZero() {
        val profile = ProfileRequest.parse("<b>(In Ronin)</b>", "Hero", "99")
        assertEquals(0, profile.pvpRank)
        assertEquals(-1, profile.clanId)
        assertEquals("", profile.clanName)
        assertEquals(true, profile.inRonin)
    }

    @Test
    fun visitHook_ownProfileUpdatesClan() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(playerid = "99"))
        }
        val lib = GameRuntimeLibrary(character = char, preferences = Preferences(MapSettings()))
        lib.processVisitResponseHooks(
            html = fameClanHtml,
            url = "showplayer.php?who=99",
        )
        assertEquals(42, ClanManager.getClanId())
        assertEquals("Loathing Legion", ClanManager.getClanName())
    }
}
