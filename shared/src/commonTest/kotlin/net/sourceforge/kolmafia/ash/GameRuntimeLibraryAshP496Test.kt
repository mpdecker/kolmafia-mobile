package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.KolGameHolidayCalendar
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

class GameRuntimeLibraryAshP496Test {

    @AfterTest
    fun tearDown() {
        KolGameHolidayCalendar.calendarDayOverride = null
    }

    private fun lines(out: String): List<String> =
        out.lines().map { it.trim() }.filter { it.isNotEmpty() }

    private fun snapshotLib(logger: SessionLogger? = null): GameRuntimeLibrary {
        val char = KoLCharacter()
        char.updateFromApiResponse(
            CharacterApiResponse(
                name = "SnapPlayer",
                classId = "1",
                level = "5",
                hp = "30",
                hpmax = "50",
                mp = "10",
                mpmax = "20",
                mus = "10",
                buffedmus = "10",
                mys = "8",
                buffedmys = "8",
                mox = "12",
                buffedmox = "12",
                adventures = "12",
                meat = "100",
            ),
        )
        char.updateEquipment(EquipmentSlot.HAT, "helmet turtle")
        return GameRuntimeLibrary(character = char, sessionLogger = logger)
    }

    @Test
    fun revision_phase496() {
        assertEquals("phase4430", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun log_snapshot_includesMoonStatusEquipment() {
        KolGameHolidayCalendar.calendarDayOverride = 0
        val listed = lines(outputLib(snapshotLib(), """cli_execute("log snapshot");"""))
        assertTrue(listed.any { it.contains("Player Snapshot") })
        assertTrue(listed.contains("Ronald: new moon"))
        assertTrue(listed.contains("Name: SnapPlayer"))
        assertTrue(listed.contains("Hat: helmet turtle"))
        assertTrue(listed.any { it.startsWith("ML:") })
        assertTrue(listed.any { it.startsWith("Enc:") })
    }

    @Test
    fun log_statusModifiers_onlyThoseSections() {
        val listed = lines(outputLib(snapshotLib(), """cli_execute("log status,modifiers");"""))
        assertTrue(listed.contains("Name: SnapPlayer"))
        assertTrue(listed.any { it.startsWith("ML:") })
        assertFalse(listed.any { it.startsWith("Ronald:") })
        assertFalse(listed.any { it.startsWith("Hat:") })
    }

    @Test
    fun log_snapshot_writesSessionLogBanner() {
        val prefs = Preferences(MapSettings())
        val logger = SessionLogger(prefs, GameEventBus())
        KolGameHolidayCalendar.calendarDayOverride = 0
        outputLib(snapshotLib(logger), """cli_execute("log snapshot");""")
        val logged = logger.recentLines().joinToString("\n")
        assertTrue(logged.contains("Player Snapshot"))
        assertTrue(logged.contains("Name: SnapPlayer"))
        assertTrue(logged.contains("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-="))
    }

    @Test
    fun log_unknownOption_isNoOp() {
        val listed = lines(outputLib(snapshotLib(), """cli_execute("log encounters");"""))
        assertTrue(listed.any { it.contains("Player Snapshot") })
        assertFalse(listed.contains("Name: SnapPlayer"))
        assertFalse(listed.any { it.startsWith("Ronald:") })
    }
}
