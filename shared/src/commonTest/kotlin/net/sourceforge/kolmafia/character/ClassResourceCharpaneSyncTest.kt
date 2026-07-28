package net.sourceforge.kolmafia.character

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClassResourceCharpaneSyncTest {

    @Test
    fun parse_fury_sealClubber() {
        val state = CharacterState(characterClass = CharacterClass.SEAL_CLUBBER.id)
        val html = """>3 gal.</span>"""
        assertEquals(3, ClassResourceCharpaneSync.parse(html, state).fury)
    }

    @Test
    fun parse_audience_loveAndHate() {
        val state = CharacterState(challengePath = AscensionPath.AVATAR_OF_SNEAKY_PETE.apiName)
        assertEquals(
            12,
            ClassResourceCharpaneSync.parse("""<b>12 Love</td>""", state).audience,
        )
        assertEquals(
            -7,
            ClassResourceCharpaneSync.parse("""<b>7 Hate</td>""", state).audience,
        )
        assertEquals(
            0,
            ClassResourceCharpaneSync.parse("""<b>Bored</td>""", state).audience,
        )
    }

    @Test
    fun parse_paradoxicity() {
        val state = CharacterState()
        assertEquals(
            42,
            ClassResourceCharpaneSync.parse("""Paradoxicity:</td><td><b>42""", state).paradoxicity,
        )
    }

    @Test
    fun parse_mask_idToName() {
        val state = CharacterState(challengePath = AscensionPath.DISGUISES_DELIMIT.apiName)
        val html = """<img src="masks/mask4.png">"""
        assertEquals("batmask", ClassResourceCharpaneSync.parse(html, state).currentMask)
        assertEquals("batmask", ClassResourceCharpaneSync.maskNameForId(4))
        assertNull(ClassResourceCharpaneSync.maskNameForId(999))
    }

    @Test
    fun apply_updatesCharacterState() {
        val character = KoLCharacter().also {
            it.updateFromApiResponse(
                net.sourceforge.kolmafia.character.CharacterApiResponse(
                    path = AscensionPath.DISGUISES_DELIMIT.apiName,
                ),
            )
        }
        ClassResourceCharpaneSync.apply(
            character,
            """Paradoxicity: 9<img src="masks/mask10.png">""",
        )
        assertEquals(9, character.state.value.paradoxicity)
        assertEquals("skull mask", character.state.value.currentMask)
    }
}
