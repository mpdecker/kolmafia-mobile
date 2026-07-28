package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.DescriptionCache
import net.sourceforge.kolmafia.data.GameDatabase

class GameRuntimeLibraryAshP114Test {

    @AfterTest
    fun tearDown() {
        DescriptionCache.clear()
    }

    private val itemDescHtml = """
        <html><body>
        <!-- itemid: 2 -->
        <div id="description"><p>A sharp tooth from a seal.</p>
        <script type="text/javascript">var x = 1;</script>
        </body></html>
    """.trimIndent()

    @Test
    fun descItem_returnsCachedDescription() = runBlocking {
        val db = GameDatabase()
        db.load()
        DescriptionCache.cacheItem(2, itemDescHtml)
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "<p>A sharp tooth from a seal.</p>",
            outputLib(lib, """print(desc(to_item("seal tooth")));""").trim(),
        )
    }

    @Test
    fun descItem_returnsEmptyWhenUncached() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("", outputLib(lib, """print(desc(to_item("seal tooth")));""").trim())
    }

    @Test
    fun descEffect_returnsCachedDescription() = runBlocking {
        val db = GameDatabase()
        db.load()
        DescriptionCache.cacheEffect(1, """<div id="description"><p>Light desc</p></div>""")
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "<p>Light desc</p>",
            outputLib(lib, """print(desc(to_effect("Light!")));""").trim(),
        )
    }

    @Test
    fun descSkill_returnsCachedDescription() = runBlocking {
        val db = GameDatabase()
        db.load()
        DescriptionCache.cacheSkill(1, """<div id="description"><p>Steel liver</p></div>""")
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "<p>Steel liver</p>",
            outputLib(lib, """print(desc(to_skill("Liver of Steel")));""").trim(),
        )
    }

    @Test
    fun descItem_visitHookPopulatesCache() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        lib.processVisitResponseHooks(
            itemDescHtml,
            "https://www.kingdomofloathing.com/desc_item.php?whichitem=617818041",
        )
        assertEquals(
            "<p>A sharp tooth from a seal.</p>",
            outputLib(lib, """print(desc(to_item("seal tooth")));""").trim(),
        )
    }

    @Test
    fun descFamiliar_returnsEmpty() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("", outputLib(lib, """print(desc(to_familiar("none")));""").trim())
    }

    @Test
    fun revision_phase160() {
        assertEquals("phase200", GameRuntimeLibrary.REVISION)
    }
}
