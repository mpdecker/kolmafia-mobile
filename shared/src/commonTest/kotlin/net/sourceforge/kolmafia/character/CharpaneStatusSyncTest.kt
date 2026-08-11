package net.sourceforge.kolmafia.character

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CharpaneStatusSyncTest {

    @Test
    fun parse_compactStatsWithModifiedPattern() {
        val html = """
            <br>Lvl. 5
            >Mus</td><td><b><font color=red>50</font>&nbsp;(40)</b></td>
            >Mys</td><td><b><font color=blue>30</font>&nbsp;(25)</b></td>
            >Mox</td><td><b>20</b></td>
            HP: <b>75/100</b>
            MP: <b>40/50</b>
            Meat: <b>1234</b>
            Adv: <b>12</b>
            MC</a>: </td><td><b>7</b>
        """.trimIndent()
        val parsed = CharpaneStatusSync.parse(html, CharacterState())
        assertEquals(50, parsed.buffedMusc)
        assertEquals(30, parsed.buffedMyst)
        assertEquals(20, parsed.buffedMoxie)
        assertEquals(75, parsed.currentHp)
        assertEquals(100, parsed.maxHp)
        assertEquals(40, parsed.currentMp)
        assertEquals(50, parsed.maxMp)
        assertEquals(1234, parsed.meat)
        assertEquals(12, parsed.adventuresLeft)
        assertEquals(7, parsed.mindControlLevel)
    }

    @Test
    fun parse_expandedStatsAndMisc() {
        val html = """
            >Muscle</td><td><b>60</b></td>
            >Mysticality</td><td><b>45</b></td>
            >Moxie</td><td><b>35</b></td>
            /hp.gif"><span>80&nbsp;/&nbsp;90</span>
            /mp.gif"><span>30&nbsp;/&nbsp;40</span>
            /meat.gif"><span>5000</span>
            /hourglass.gif"><span>8</span>
            Mind Control</a>: </td><td><b>3</b>
        """.trimIndent()
        val parsed = CharpaneStatusSync.parse(html, CharacterState())
        assertEquals(60, parsed.buffedMusc)
        assertEquals(45, parsed.buffedMyst)
        assertEquals(35, parsed.buffedMoxie)
        assertEquals(80, parsed.currentHp)
        assertEquals(90, parsed.maxHp)
        assertEquals(30, parsed.currentMp)
        assertEquals(40, parsed.maxMp)
        assertEquals(5000, parsed.meat)
        assertEquals(8, parsed.adventuresLeft)
        assertEquals(3, parsed.mindControlLevel)
    }

    @Test
    fun updateFromCharpane_preservesEquipment() {
        val char = KoLCharacter()
        char.updateFromApiResponse(
            net.sourceforge.kolmafia.character.CharacterApiResponse(
                weapon = "big stick",
                hat = "fedora",
            ),
        )
        char.updateFromCharpane(
            CharpaneStatusSync.ParsedStatus(
                buffedMusc = 99,
                currentHp = 10,
                maxHp = 100,
            ),
        )
        val state = char.state.value
        assertEquals(99, state.buffedMusc)
        assertEquals(10, state.currentHp)
        assertEquals("big stick", state.equipment[EquipmentSlot.WEAPON])
        assertEquals("fedora", state.equipment[EquipmentSlot.HAT])
    }

    @Test
    fun hasTransfunctionerEquipped_caseInsensitive() {
        val state = CharacterState(
            equipment = mapOf(EquipmentSlot.ACC1 to "Continuum Transfunctioner"),
        )
        assertTrue(CharpaneStatusSync.hasTransfunctionerEquipped(state))
    }

    @Test
    fun parse_noMatchReturnsNulls() {
        val parsed = CharpaneStatusSync.parse("<html></html>", CharacterState())
        assertNull(parsed.buffedMusc)
        assertNull(parsed.currentHp)
    }
}
