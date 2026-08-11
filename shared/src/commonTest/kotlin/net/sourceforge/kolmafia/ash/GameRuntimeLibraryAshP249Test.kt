package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import com.russhwolf.settings.MapSettings

class GameRuntimeLibraryAshP249Test {

    @Test
    fun revision_phase236() {
        assertEquals("phase450", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun skillBracket_typeAndClass() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "combat",
            outputLib(lib, """print(to_skill("CLEESH")["type"]);""").trim(),
        )
        assertEquals(
            "seal clubber",
            outputLib(lib, """print(to_skill("Thrust-Smack")["class"]);""").trim(),
        )
    }

    @Test
    fun skillBracket_levelAndTraincost() = runBlocking {
        val db = GameDatabase()
        db.load()
        val p = Preferences(MapSettings())
        p.setInt("skillLevel1003", 4)
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = p)
        assertEquals("4", outputLib(lib, """print(to_skill("Thrust-Smack")["level"]);""").trim())
        assertEquals("750", outputLib(lib, """print(to_skill("Thrust-Smack")["traincost"]);""").trim())
    }

    @Test
    fun skillBracket_idNameImage() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("15", outputLib(lib, """print(to_skill("CLEESH")["id"]);""").trim())
        assertEquals("CLEESH", outputLib(lib, """print(to_skill("CLEESH")["name"]);""").trim())
        assertEquals("commacha.gif", outputLib(lib, """print(to_skill("CLEESH")["image"]);""").trim())
    }
}
