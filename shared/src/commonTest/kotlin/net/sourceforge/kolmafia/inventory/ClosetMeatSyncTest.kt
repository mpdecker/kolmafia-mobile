package net.sourceforge.kolmafia.inventory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import net.sourceforge.kolmafia.character.KoLCharacter

class ClosetMeatSyncTest {

    @Test
    fun parseClosetMeat_withCommas() {
        val html = """Your closet contains <b>170,000,000</b> meat."""
        assertEquals(170_000_000L, ClosetMeatSync.parseClosetMeat(html))
    }

    @Test
    fun parseClosetMeat_missingPatternReturnsNull() {
        assertNull(ClosetMeatSync.parseClosetMeat("<html><body>Empty closet</body></html>"))
    }

    @Test
    fun apply_updatesCharacterState() {
        val char = KoLCharacter()
        ClosetMeatSync.apply(
            char,
            """Your closet contains <b>42,000</b> meat.""",
            "https://www.kingdomofloathing.com/closet.php",
        )
        assertEquals(42_000L, char.state.value.closetMeat)
    }

    @Test
    fun apply_skipsNonClosetUrl() {
        val char = KoLCharacter()
        ClosetMeatSync.apply(
            char,
            """Your closet contains <b>42,000</b> meat.""",
            "https://www.kingdomofloathing.com/storage.php",
        )
        assertEquals(0L, char.state.value.closetMeat)
    }
}
