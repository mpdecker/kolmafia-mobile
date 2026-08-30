package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpaaaceRequestTest {

    private lateinit var prefs: Preferences

    @BeforeTest
    fun setUp() {
        prefs = Preferences(MapSettings())
        SpaaaceRequest.resetForTest()
    }

    @AfterTest
    fun tearDown() {
        SpaaaceRequest.resetForTest()
    }

    @Test
    fun porkoSolver_computesExpectedPayouts() {
        val board = buildString {
            repeat(136) { append('3') }
        }
        val payouts = "012101210"
        assertTrue(SpaaaceRequest.validBoard(board, payouts))
        SpaaaceRequest.loadGameBoard(board, payouts, prefs)
        SpaaaceRequest.solveGameBoard(prefs)
        val expected = prefs.getString("lastPorkoExpected", "")
        assertTrue(expected.contains(':'))
        assertTrue((SpaaaceRequest.bestExpectedPayout() ?: 0f) >= 0f)
    }

    @Test
    fun parseGameBoard_extractsPegStyles() {
        val html = """title="peg style 1" title="peg style 2" title="peg style 3""""
        assertEquals("123", SpaaaceRequest.parseGameBoard(html))
    }
}
