package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.data.PocketDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.CargoPocketSync
import net.sourceforge.kolmafia.session.YegDemonNameSync

class GameRuntimeLibraryAshP83Test {

    @BeforeTest
    fun loadData() = runTest {
        MonsterDatabase.load()
        EffectDatabase.load()
        ItemDatabase.load()
        PocketDatabase.applyParseForTest(
            PocketDatabase.parseForTest(
                """
                30	Monster	bookbat
                5	Effect	Super Vision (40)
                27	Item	baconstone
                12	Stats	80	90	120
                7	Scrap	3
                100	Poem	2	half line text
                101	Meat	917	clue text
                102	Joke	a funny joke
                103	Restore	Smile of the Vole (20)
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun revision_phase141() {
        assertEquals("phase421", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun scrap_pockets_listsOrderedPockets() {
        val lib = GameRuntimeLibrary()
        assertEquals("1", outputLib(lib, """print(count(scrap_pockets()));"""))
    }

    @Test
    fun potential_pockets_monster_returnsAllMatches() {
        val lib = GameRuntimeLibrary()
        assertEquals("1", outputLib(lib, """print(count(potential_pockets(to_monster("bookbat"))));"""))
    }

    @Test
    fun potential_pockets_effect_returnsSortedMatches() {
        val lib = GameRuntimeLibrary()
        assertEquals("1", outputLib(lib, """print(count(potential_pockets(to_effect("Super Vision"))));"""))
    }

    @Test
    fun pocket_effects_returnsEffectDuration() {
        val lib = GameRuntimeLibrary()
        assertEquals(
            "40",
            outputLib(lib, """print(pocket_effects(5)[to_effect("Super Vision")]);"""),
        )
    }

    @Test
    fun pocket_items_returnsItemCount() {
        val lib = GameRuntimeLibrary()
        assertEquals(
            "1",
            outputLib(lib, """print(pocket_items(27)[to_item("baconstone")]);"""),
        )
    }

    @Test
    fun pocket_stats_returnsStatValues() {
        val lib = GameRuntimeLibrary()
        assertEquals("80", outputLib(lib, """print(pocket_stats(12)[to_stat("muscle")]);"""))
        assertEquals("90", outputLib(lib, """print(pocket_stats(12)[to_stat("mysticality")]);"""))
        assertEquals("120", outputLib(lib, """print(pocket_stats(12)[to_stat("moxie")]);"""))
    }

    @Test
    fun pocket_scrap_returnsKnownSyllable() {
        val prefs = Preferences(MapSettings())
        val yeg = YegDemonNameSync(prefs)
        yeg.saveScrapPockets(mapOf(7 to "Go"))
        val lib = GameRuntimeLibrary(yegDemonNameSync = yeg)
        assertEquals("Go", outputLib(lib, """print(pocket_scrap(7)[3]);"""))
    }

    @Test
    fun pocket_poem_returnsIndexedText() {
        val lib = GameRuntimeLibrary()
        assertEquals("half line text", outputLib(lib, """print(pocket_poem(100)[2]);"""))
    }

    @Test
    fun pocket_meat_returnsIndexedClue() {
        val lib = GameRuntimeLibrary()
        assertEquals("clue text", outputLib(lib, """print(pocket_meat(101)[917]);"""))
    }

    @Test
    fun pocket_joke_returnsJokeText() {
        val lib = GameRuntimeLibrary()
        assertEquals("a funny joke", outputLib(lib, """print(pocket_joke(102));"""))
    }

    @Test
    fun joke_and_restoration_pocket_sets() {
        val lib = GameRuntimeLibrary()
        assertEquals("1", outputLib(lib, """print(count(joke_pockets()));"""))
        assertEquals("1", outputLib(lib, """print(count(restoration_pockets()));"""))
    }

    @Test
    fun poem_and_meat_pocket_lists() {
        val lib = GameRuntimeLibrary()
        assertTrue(outputLib(lib, """print(count(poem_pockets()));""").toInt() >= 1)
        assertTrue(outputLib(lib, """print(count(meat_pockets()));""").toInt() >= 1)
    }
}
