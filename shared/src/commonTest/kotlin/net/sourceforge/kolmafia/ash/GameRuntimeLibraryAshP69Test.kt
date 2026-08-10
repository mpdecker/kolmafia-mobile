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

class GameRuntimeLibraryAshP69Test {

    @BeforeTest
    fun resetTracker() {
        MonsterStatusTracker.resetLastMonster()
    }

    @Test
    fun lastMonster_randomModifiersPopulatedFromTracker() = runBlocking {
        MonsterDatabase.load()
        val db = GameDatabase()
        db.load()
        val template = MonsterDatabase.getByName("huge mosquito")!!
        MonsterStatusTracker.setNextMonster(template, listOf("huge", "red-hot"))
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_MONSTER, template.name)
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals(
            "2",
            outputLib(lib, """print(count(last_monster()["random_modifiers"]));""").trim(),
        )
        assertEquals(
            "huge",
            outputLib(lib, """print(last_monster()["random_modifiers"][0]);""").trim(),
        )
        assertEquals(
            "red-hot",
            outputLib(lib, """print(last_monster()["random_modifiers"][1]);""").trim(),
        )
    }

    @Test
    fun toMonster_randomModifiersStayEmptyOnTemplate() = runBlocking {
        MonsterDatabase.load()
        val db = GameDatabase()
        db.load()
        val template = MonsterDatabase.getByName("huge mosquito")!!
        MonsterStatusTracker.setNextMonster(template, listOf("huge"))
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "0",
            outputLib(
                lib,
                """print(count(to_monster("huge mosquito")["random_modifiers"]));""",
            ).trim(),
        )
    }

    @Test
    fun revision_phase120() {
        assertEquals("phase390", GameRuntimeLibrary.REVISION)
    }
}
