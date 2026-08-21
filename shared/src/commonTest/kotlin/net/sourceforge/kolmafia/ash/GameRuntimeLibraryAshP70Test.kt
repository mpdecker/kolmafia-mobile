package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP70Test {

    @BeforeTest
    fun resetTracker() {
        MonsterStatusTracker.resetLastMonster()
    }

    @Test
    fun lastMonster_hugeModifierDoublesBaseStats() = runBlocking {
        MonsterDatabase.load()
        val db = GameDatabase()
        db.load()
        val template = MonsterDatabase.getByName("huge mosquito")!!
        MonsterStatusTracker.setNextMonster(template, listOf("huge"))
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_MONSTER, template.name)
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals(
            "36",
            outputLib(lib, """print(last_monster()["base_hp"]);""").trim(),
        )
        assertEquals(
            "32",
            outputLib(lib, """print(last_monster()["base_attack"]);""").trim(),
        )
        assertEquals(
            "28",
            outputLib(lib, """print(last_monster()["base_defense"]);""").trim(),
        )
    }

    @Test
    fun toMonster_templateStatsUnchangedWithTrackerSeeded() = runBlocking {
        MonsterDatabase.load()
        val db = GameDatabase()
        db.load()
        val template = MonsterDatabase.getByName("huge mosquito")!!
        MonsterStatusTracker.setNextMonster(template, listOf("huge"))
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "18",
            outputLib(lib, """print(to_monster("huge mosquito")["base_hp"]);""").trim(),
        )
        assertEquals(
            "16",
            outputLib(lib, """print(to_monster("huge mosquito")["base_attack"]);""").trim(),
        )
        assertEquals(
            "14",
            outputLib(lib, """print(to_monster("huge mosquito")["base_defense"]);""").trim(),
        )
    }

    @Test
    fun revision_phase120() {
        assertEquals("phase826", GameRuntimeLibrary.REVISION)
    }
}
