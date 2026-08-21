package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase

class GameRuntimeLibraryAshP65Test {

    @Test
    fun parse_subTypes_bugbear() = runBlocking {
        MonsterDatabase.load()
        val bugbear = MonsterDatabase.getByName("angry bugbear")!!
        assertEquals(listOf("bugbear"), bugbear.subTypes)
    }

    @Test
    fun parse_subTypes_modelSkeleton() = runBlocking {
        MonsterDatabase.load()
        val skeleton = MonsterDatabase.getByName("model skeleton")!!
        assertEquals(listOf("skeleton", "ghost"), skeleton.subTypes)
    }

    @Test
    fun parse_images_edTheUndying() = runBlocking {
        MonsterDatabase.load()
        val ed = MonsterDatabase.getByName("ed the undying")!!
        assertEquals("ed.gif", ed.image)
        assertEquals(7, ed.images.size)
        assertEquals("ed.gif", ed.images[0])
        assertEquals("ed7.gif", ed.images[6])
    }

    @Test
    fun subTypes_mosquitoEmpty() = runBlocking {
        MonsterDatabase.load()
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "0",
            outputLib(lib, """print(count(to_monster("huge mosquito")["sub_types"]));""").trim(),
        )
        assertEquals(
            "1",
            outputLib(lib, """print(count(to_monster("huge mosquito")["images"]));""").trim(),
        )
        assertEquals(
            "giantmosquito.gif",
            outputLib(lib, """print(to_monster("huge mosquito")["images"][0]);""").trim(),
        )
    }

    @Test
    fun subTypes_angryBugbear() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "1",
            outputLib(lib, """print(count(to_monster("angry bugbear")["sub_types"]));""").trim(),
        )
        assertEquals(
            "bugbear",
            outputLib(lib, """print(to_monster("angry bugbear")["sub_types"][0]);""").trim(),
        )
    }

    @Test
    fun subTypes_apsGhostAndGhostBracket() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "1",
            outputLib(
                lib,
                """print(count(to_monster("ancient protector spirit")["sub_types"]));""",
            ).trim(),
        )
        assertEquals(
            "ghost",
            outputLib(
                lib,
                """print(to_monster("ancient protector spirit")["sub_types"][0]);""",
            ).trim(),
        )
        assertEquals(
            "true",
            outputLib(
                lib,
                """print(to_string(to_monster("ancient protector spirit")["ghost"]));""",
            ).trim(),
        )
    }

    @Test
    fun subTypes_modelSkeleton_twoSubtypes() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "2",
            outputLib(lib, """print(count(to_monster("model skeleton")["sub_types"]));""").trim(),
        )
        assertEquals(
            "skeleton",
            outputLib(lib, """print(to_monster("model skeleton")["sub_types"][0]);""").trim(),
        )
        assertEquals(
            "ghost",
            outputLib(lib, """print(to_monster("model skeleton")["sub_types"][1]);""").trim(),
        )
    }

    @Test
    fun images_edTheUndying_sevenImages() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "ed.gif",
            outputLib(lib, """print(to_monster("Ed the Undying")["image"]);""").trim(),
        )
        assertEquals(
            "7",
            outputLib(lib, """print(count(to_monster("Ed the Undying")["images"]));""").trim(),
        )
        assertEquals(
            "ed.gif",
            outputLib(lib, """print(to_monster("Ed the Undying")["images"][0]);""").trim(),
        )
        assertEquals(
            "ed7.gif",
            outputLib(lib, """print(to_monster("Ed the Undying")["images"][6]);""").trim(),
        )
    }

    @Test
    fun revision_phase120() {
        assertEquals("phase814", GameRuntimeLibrary.REVISION)
    }
}
