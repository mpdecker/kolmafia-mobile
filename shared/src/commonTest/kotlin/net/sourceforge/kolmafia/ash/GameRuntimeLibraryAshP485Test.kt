package net.sourceforge.kolmafia.ash

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.session.PeeVPeeSync
import net.sourceforge.kolmafia.session.PvpManager

class GameRuntimeLibraryAshP485Test {

    @BeforeTest
    fun reset() {
        PvpManager.resetForTest()
    }

    @AfterTest
    fun cleanup() {
        PvpManager.resetForTest()
    }

    @Test
    fun revision_phase485() {
        assertEquals("phase635", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun myBasestat_afterPvpLoss_crossesSquare() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(
                    name = "Hero",
                    pvpfights = "4",
                    hippystone = "1",
                    mus = "10",
                    musexp = "100",
                    buffedmus = "10",
                    mys = "8",
                    mysexp = "80",
                    mox = "7",
                    moxexp = "60",
                ),
            )
        }
        val lib = GameRuntimeLibrary(character = char)
        assertEquals(
            "10",
            outputLib(lib, """print(to_string(my_basestat(to_stat("Muscle"))));""").trim(),
        )
        PeeVPeeSync.apply(
            html = """
                You have 3 fights remaining today.
                <div class="fight"><a href="showplayer.php?who=1"><b>Hero</b></a> calls out <a href="showplayer.php?who=2"><b>Villain</b></a> for battle!
                <span class="win"><b>Villain</b> won the fight, <b>9</b> to <b>2</b>!
                <td>Hero lost 1 Muscle.</td>
            """.trimIndent(),
            url = "peevpee.php?place=fight&action=fight",
            character = char,
        )
        assertEquals(
            "9",
            outputLib(lib, """print(to_string(my_basestat(to_stat("Muscle"))));""").trim(),
        )
    }
}
