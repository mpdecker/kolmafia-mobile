package net.sourceforge.kolmafia.session

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.VolcanoMazeDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.volcano.VolcanoMapRng
import net.sourceforge.kolmafia.volcano.VolcanoMazeConstants
import net.sourceforge.kolmafia.volcano.VolcanoMazeConstants.START
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VolcanoMazeManagerTest {

    private lateinit var preferences: Preferences

    @BeforeTest
    fun setUp() {
        preferences = Preferences(com.russhwolf.settings.MapSettings())
        VolcanoMazeManager.resetForTest()
        VolcanoMapRng.resetRng()
    }

    @AfterTest
    fun tearDown() {
        VolcanoMazeManager.resetForTest()
        VolcanoMazeDatabase.resetForTest()
    }

    @Test
    fun validateMapSequence_key1() = runBlocking { validateMapSequence(1, 65) }

    @Test
    fun validateMapSequence_key2() = runBlocking { validateMapSequence(2, 65) }

    @Test
    fun validateMapSequence_key3() = runBlocking { validateMapSequence(3, 41) }

    @Test
    fun validateMapSequence_key4() = runBlocking { validateMapSequence(4, 41) }

    @Test
    fun validateMapSequence_key5() = runBlocking { validateMapSequence(5, 47) }

    @Test
    fun validateMapSequence_key6() = runBlocking { validateMapSequence(6, 47) }

    private suspend fun validateMapSequence(key: Int, pathLength: Int) {
        VolcanoMazeDatabase.load()
        loadSequenceIntoPrefs(preferences, key)
        VolcanoMazeManager.loadCurrentMaps(preferences, START, 0)
        val solution = VolcanoMazeManager.solve(START, 0)
        assertEquals(pathLength - 1, solution?.size())
    }

    @Test
    fun parseJsonCoords_setsLocationAndPlatforms() {
        val json =
            """{"won":false,"pos":"5,12","show":["3","6","10","14","18","23","26","30","32","41","43","50","52","57","59","60","64","84","86","92","97","102","106","109","111","114","115","117","119","129","136","145","148","153","154","157","164"]}"""
        VolcanoMazeManager.resetForTest()
        VolcanoMazeManager.parseJsonCoords(json)
        assertEquals(5, VolcanoMazeConstants.col(VolcanoMazeManager.currentLocationForTest()))
        assertEquals(12, VolcanoMazeConstants.row(VolcanoMazeManager.currentLocationForTest()))
        val parsed = VolcanoMazeManager.parseCoords(json)
        assertTrue(parsed!!.contains("3"))
        assertTrue(parsed.contains("164"))
        assertFalse(parsed.contains("84"))
    }

    @Test
    fun useCachedVolcanoMaps_fillsAllPrefs() = runBlocking {
        VolcanoMazeDatabase.load()
        val sequence = VolcanoMazeDatabase.getMapSequence(1)!!
        val coords = sequence[0]!!.coordinates
        preferences.setBoolean("useCachedVolcanoMaps", true)
        VolcanoMazeManager.parseResult(preferences, sampleJsonForCoords(coords))
        for (i in 1..5) {
            assertEquals(sequence[i - 1]!!.coordinates, preferences.getString("volcanoMaze$i", ""))
        }
        assertEquals(169, VolcanoMazeManager.foundForTest())
    }

    private fun loadSequenceIntoPrefs(prefs: Preferences, key: Int) {
        val sequence = VolcanoMazeDatabase.getMapSequence(key) ?: error("missing sequence $key")
        for (i in sequence.indices) {
            prefs.setString("volcanoMaze${i + 1}", sequence[i]!!.coordinates)
        }
    }

    private fun sampleJsonForCoords(coords: String): String {
        val show = coords.split(',').joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
        return """{"won":false,"pos":"6,12","show":$show}"""
    }
}
