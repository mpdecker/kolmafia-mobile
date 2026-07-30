package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.BastilleDatabase
import net.sourceforge.kolmafia.data.BastilleDatabase.Castle
import net.sourceforge.kolmafia.data.BastilleDatabase.Stats
import net.sourceforge.kolmafia.platform.UserDataFilePaths
import net.sourceforge.kolmafia.preferences.Preferences

class BastilleBattalionFileLogTest {

    private lateinit var prefs: Preferences
    private lateinit var tempDir: File

    @BeforeTest
    fun setUp() {
        tempDir = File.createTempFile("kolmafia-bastille-log-", "").apply {
            delete()
            mkdirs()
        }
        UserDataFilePaths.testBasePath = tempDir.absolutePath
        prefs = Preferences(MapSettings())
    }

    @AfterTest
    fun tearDown() {
        UserDataFilePaths.testBasePath = null
        tempDir.deleteRecursively()
    }

    @Test
    fun saveBattle_skippedWhenLoggingDisabled() {
        val battle = sampleBattle()
        BastilleBattalionFileLog.saveBattle(battle, prefs, playerId = 12345)
        assertFalse(File(tempDir, "Bastille.battles.txt").exists())
    }

    @Test
    fun saveBattle_writesTabDelimitedRowWhenEnabled() {
        prefs.setBoolean("logBastilleBattalionBattles", true)
        prefs.setInt("_bastilleGames", 0)
        val battle = sampleBattle()
        BastilleBattalionFileLog.saveBattle(battle, prefs, playerId = 12345)
        val line = File(tempDir, "Bastille.battles.txt").readText().trim()
        assertTrue(line.contains("\t1\t"))
        assertTrue(line.contains("\tbarracks\t"))
        assertTrue(line.contains("\toffensive\t"))
        assertTrue(line.contains("\ttrue\t"))
        assertTrue(line.endsWith("\t45"))
    }

    @Test
    fun saveCheese_writesEncounterFieldsWhenEnabled() {
        prefs.setBoolean("logBastilleBattalionBattles", true)
        prefs.setInt("_bastilleGames", 1)
        val record = BastilleCheeseRecord.fromEncounter(
            turn = 4,
            encounterName = "Raid the cave",
            cheese = 133,
            currentStat = { if (it == BastilleDatabase.Stat.MA) 6 else 0 },
            boosts = BastilleBoosts("M"),
        )
        BastilleBattalionFileLog.saveCheese(record, prefs, playerId = 99)
        val line = File(tempDir, "Bastille.cheese.txt").readText().trim()
        assertTrue(line.contains("\t4\tRaid the cave\tMA\t6\t1\t133"))
    }

    private fun sampleBattle(): BastilleBattle {
        val results = BastilleBattleResults(
            aggressor = true,
            military = true,
            castle = true,
            psychological = false,
        )
        return BastilleBattle(
            number = 1,
            stats = Stats(ma = 1, md = 2, ca = 3, cd = 4, pa = 5, pd = 6),
            boosts = BastilleBoosts("MC"),
            enemy = Castle.MILITARY,
            stance = BastilleStance.OFFENSE,
            results = results,
            cheese = 45,
        )
    }
}
