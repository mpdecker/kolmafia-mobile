package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.combat.EncounterModifierPipeline
import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP71Test {

    @BeforeTest
    fun resetTracker() {
        MonsterStatusTracker.resetLastMonster()
    }

    @Test
    fun lastMonster_maskModifierFromPipeline() = runBlocking {
        MonsterDatabase.load()
        val db = GameDatabase()
        db.load()
        val template = MonsterDatabase.getByName("Naughty Sorceress")!!
        val modifiers = mutableListOf<String>()
        EncounterModifierPipeline.applyPostOcrs(
            "Naughty Sorceress wearing a Boss Bat mask",
            modifiers,
            EncounterModifierPipeline.EncounterModifierContext(
                familiarId = 0,
                ascensionPath = AscensionPath.DISGUISES_DELIMIT,
            ),
        )
        MonsterStatusTracker.setNextMonster(template, modifiers)
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_MONSTER, template.name)
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals(
            "1",
            outputLib(lib, """print(count(last_monster()["random_modifiers"]));""").trim(),
        )
        assertEquals(
            "Boss Bat mask",
            outputLib(lib, """print(last_monster()["random_modifiers"][0]);""").trim(),
        )
    }

    @Test
    fun revision_phase120() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }
}
