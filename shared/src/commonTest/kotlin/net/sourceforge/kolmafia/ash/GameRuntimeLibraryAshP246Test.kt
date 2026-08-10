package net.sourceforge.kolmafia.ash

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase

class GameRuntimeLibraryAshP246Test {

    @BeforeTest
    fun setUp() = runTest {
        GameDatabase().load()
    }

    @Test
    fun revision_isphase236() {
        assertEquals("phase370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun itemBracket_smallImage_folderUsesThumbnail() = runTest {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "folder2.gif",
            outputLib(lib, """print(to_item("folder (red)")["smallimage"]);""").trim(),
        )
        assertEquals(
            "folder1.gif",
            outputLib(lib, """print(to_item("folder (yellow)")["smallimage"]);""").trim(),
        )
    }

    @Test
    fun itemBracket_smallImage_defaultMatchesImage() = runTest {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "batwing.gif",
            outputLib(lib, """print(to_item("hot wing")["smallimage"]);""").trim(),
        )
    }

    @Test
    fun itemBracket_seller_returnsCoinmasterName() = runTest {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "The Shore, Inc. Gift Shop",
            outputLib(lib, """print(to_item("dinghy plans")["seller"]);""").trim(),
        )
    }

    @Test
    fun itemBracket_buyer_returnsCoinmasterName() = runTest {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "Dimemaster",
            outputLib(lib, """print(to_item("beer bong")["buyer"]);""").trim(),
        )
    }

    @Test
    fun itemBracket_skill_returnsGrantedSkill() = runTest {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "Holiday Weight Gain",
            outputLib(lib, """print(to_item("A Crimbo Carol, Ch. 1")["skill"]);""").trim(),
        )
    }

    @Test
    fun itemBracket_recipe_returnsLearnedRecipe() = runTest {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "concoction of clumsiness",
            outputLib(lib, """print(to_item("fumble formula")["recipe"]);""").trim(),
        )
    }

    @Test
    fun itemBracket_noobSkill_returnsAbsorbSkill() = runTest {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "Work Ethic",
            outputLib(lib, """print(to_item("hot wing")["noob_skill"]);""").trim(),
        )
    }

    @Test
    fun itemBracket_emptyEntityFields_returnInitValues() = runTest {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("", outputLib(lib, """print(to_item("hot wing")["seller"]);""").trim())
        assertEquals("", outputLib(lib, """print(to_item("dinghy plans")["buyer"]);""").trim())
        assertEquals("", outputLib(lib, """print(to_item("hot wing")["skill"]);""").trim())
        assertEquals("", outputLib(lib, """print(to_item("hot wing")["recipe"]);""").trim())
    }

    @Test
    fun itemDatabase_getNoobSkillId_matchesDescFormula() = runTest {
        GameDatabase().load()
        assertEquals(23041, ItemDatabase.getNoobSkillId(471))
    }
}
