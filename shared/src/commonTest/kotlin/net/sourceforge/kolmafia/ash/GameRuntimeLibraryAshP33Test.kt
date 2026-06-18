package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.PastaThrall

class GameRuntimeLibraryAshP33Test {

    @Test
    fun thrallBracketLevel_readsPastaThrallPref() = runBlocking {
        val prefs = Preferences(MapSettings())
        prefs.setString(PastaThrall.prefKey(1), "14,Vampieroghi")
        val db = GameDatabase()
        db.load()
        ModifierDatabase.load()
        val lib = GameRuntimeLibrary(preferences = prefs, gameDatabase = db)
        assertEquals("14", outputLib(lib, """print(to_thrall("Vampieroghi")["level"]);""").trim())
        assertEquals("1", outputLib(lib, """print(to_thrall("Vampieroghi")["id"]);""").trim())
    }

    @Test
    fun thrallBracketSkill_readsBindSkill() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        val out = outputLib(lib, """print(to_thrall("Vampieroghi")["skill"]);""").trim()
        assertEquals("Bind Vampieroghi", out)
    }

    @Test
    fun vykeaBracketModifiers_readsCompanionModifiers() {
        val lib = GameRuntimeLibrary()
        assertEquals(
            "Meat Drop: +30",
            outputLib(lib, """print(to_vykea("level 3 couch")["modifiers"]);""").trim(),
        )
        assertEquals("3", outputLib(lib, """print(to_vykea("level 3 couch")["level"]);""").trim())
        assertEquals("couch", outputLib(lib, """print(to_vykea("level 3 couch")["type"]);""").trim())
    }

    @Test
    fun vykeaBracketRune_emptyForUnrunnedCompanion() {
        val lib = GameRuntimeLibrary()
        assertEquals("", outputLib(lib, """print(to_vykea("level 3 couch")["rune"]);""").trim())
    }
}
