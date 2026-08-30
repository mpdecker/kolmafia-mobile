package net.sourceforge.kolmafia.session

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NicheEnginesTest {
    @AfterTest
    fun resetManagers() {
        RabbitHoleManager.reset()
        WumpusManager.reset()
        LeafletManager.reset()
    }

    @Test
    fun parsesAndSolvesSyntheticChessboard() {
        val html = buildString {
            repeat(64) { index ->
                val whiteSquare = (index / 8 + index % 8) % 2 == 0
                val color = if (whiteSquare) "fff" else "979797"
                val image = if (index == 9) "chess_pww" else "blanktrans"
                val title = if (index == 9) "White Pawn" else "blank square"
                append(
                    """<td style="background-color: #$color;" title="$title">""" +
                        """<img src="otherimages/chess/$image.gif"></td>""",
                )
            }
        }
        val board = RabbitHoleManager.parseChessPuzzle(html)
        assertNotNull(board)
        assertEquals("WPb2", board.config())
        assertEquals(listOf(1), RabbitHoleManager.solve())
        assertEquals(1 to 0, RabbitHoleManager.step())
    }

    @Test
    fun parsesChessMoveCoordinatesAndCompletion() {
        RabbitHoleManager.load("WPb2")
        val log = RabbitHoleManager.parseChessMove(
            "choice.php?whichchoice=443&option=1&xy=1%2C0",
            "You acquire a queen cookie",
        )
        assertEquals("1: WPb2-b1", log)
    }

    @Test
    fun parsesWumpusRoomWarningsAndExits() {
        val html = """
            <b>>The Acrid Chamber</b>
            Enter the breezy chamber
            Enter the creepy chamber
            Enter the dripping chamber
            You hear the fluttering of wings and a high-pitched squeaking coming from somewhere nearby.
        """.trimIndent()
        val deductions = WumpusManager.visitChoice(html)
        assertEquals("acrid", WumpusManager.current?.name)
        assertEquals(listOf("breezy", "creepy", "dripping"), WumpusManager.current?.exits?.map { it?.name })
        assertTrue(deductions.any { it.startsWith("Sounds: bats") })
        assertEquals(6, WumpusManager.dynamicChoiceOptions().size)
        assertTrue(WumpusManager.getWumpinatorCode().contains("::P::B:BCD"))
    }

    @Test
    fun marksWumpusFromDarknessFight() {
        assertTrue(WumpusManager.onWumpusFight("""<img src="darkness.gif">"""))
        assertTrue(WumpusManager.isWumpus())
        WumpusManager.onWumpusFight("You win the fight")
        assertFalse(WumpusManager.isWumpus())
    }

    @Test
    fun parsesLeafletLocationsAndInventory() {
        val state = LeafletManager.initialize(
            """
            <td><b>>West of House</b></td>
            A junk mail leaflet
            A pair of large rubber wading boots (equipped)
            The front door is closed.
            """.trimIndent(),
        )
        assertEquals(LeafletManager.Location.FIELD, state.location)
        assertTrue(state.leaflet)
        assertTrue(state.boots)
        assertTrue(state.wornBoots)
        assertFalse(state.door)
        assertEquals("west of the house", LeafletManager.locationName(">West of House</b>"))
    }

    @Test
    fun parsesLeafletMazeExitAndMagicWord() {
        LeafletManager.parseLocation(
            """
            <td><b>>Forest</b></td>
            Gaps in the dense, forbidding foliage lead north,
            """.trimIndent(),
        )
        assertEquals(LeafletManager.Location.FOREST, LeafletManager.state.location)
        assertEquals("north", LeafletManager.state.mazeExit)
        assertEquals(
            "plover",
            LeafletManager.parseMantelpiece("A carved driftwood bird sits on the mantelpiece."),
        )
    }
}
