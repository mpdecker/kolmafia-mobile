package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter

class GameRuntimeLibraryAshP491Test {

    private fun lines(out: String): List<String> =
        out.lines().map { it.trimEnd() }

    private fun statusLib(
        name: String = "TestPlayer",
        classId: String = "1",
        level: String = "5",
        hp: String = "30",
        hpmax: String = "50",
        mp: String = "10",
        mpmax: String = "20",
        mus: String = "10",
        buffedmus: String = "10",
        musexp: String = "100",
        mys: String = "8",
        buffedmys: String = "8",
        mysexp: String = "64",
        mox: String = "12",
        buffedmox: String = "12",
        moxexp: String = "144",
        adventures: String = "12",
        meat: String = "999",
        fullness: String = "3",
        drunk: String = "2",
        spleen: String = "1",
    ): GameRuntimeLibrary {
        val char = KoLCharacter()
        char.updateFromApiResponse(
            CharacterApiResponse(
                name = name,
                classId = classId,
                level = level,
                hp = hp,
                hpmax = hpmax,
                mp = mp,
                mpmax = mpmax,
                mus = mus,
                buffedmus = buffedmus,
                musexp = musexp,
                mys = mys,
                buffedmys = buffedmys,
                mysexp = mysexp,
                mox = mox,
                buffedmox = buffedmox,
                moxexp = moxexp,
                adventures = adventures,
                meat = meat,
                fullness = fullness,
                drunk = drunk,
                spleen = spleen,
            ),
        )
        return GameRuntimeLibrary(character = char)
    }

    @Test
    fun revision_phase491() {
        assertEquals("phase2450", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun status_printsDesktopDumpHeaders() {
        val out = outputLib(statusLib(), """cli_execute("status");""")
        val listed = lines(out)
        assertTrue(listed.contains("Name: TestPlayer"))
        assertTrue(listed.contains("Class: Seal Clubber"))
        assertTrue(listed.contains("Lv: 5"))
        assertTrue(listed.contains("HP: 30 / 50"))
        assertTrue(listed.contains("MP: 10 / 20"))
        assertTrue(listed.contains("Advs: 12"))
        assertTrue(listed.contains("Meat: 999"))
        assertTrue(listed.contains("Full: 3 / 15"))
        assertTrue(listed.contains("Drunk: 2 / 14"))
        assertTrue(listed.contains("Spleen: 1 / 15"))
        assertFalse(out.contains("Level 5"))
        assertFalse(out.contains("12 adventures"))
    }

    @Test
    fun status_printsBaseInParensWhenBuffedDiffers() {
        val out = outputLib(
            statusLib(mus = "10", buffedmus = "25", musexp = "100"),
            """cli_execute("status");""",
        )
        assertTrue(lines(out).any { it.startsWith("Mus: 25 (10), tnp = ") })
    }

    @Test
    fun status_omitsBaseParensWhenBuffedEqualsBase() {
        val out = outputLib(
            statusLib(mus = "10", buffedmus = "10", musexp = "100"),
            """cli_execute("status");""",
        )
        val mus = lines(out).first { it.startsWith("Mus:") }
        assertTrue(mus.startsWith("Mus: 10, tnp = "))
        assertFalse(mus.contains("("))
    }
}
