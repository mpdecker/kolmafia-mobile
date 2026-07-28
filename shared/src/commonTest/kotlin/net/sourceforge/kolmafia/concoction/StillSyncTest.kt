package net.sourceforge.kolmafia.concoction

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import net.sourceforge.kolmafia.character.KoLCharacter

class StillSyncTest {

    @Test
    fun parseStillsAvailable_readsBrightCount() {
        val html = """You are standing in front of a still with 3 bright copper stills."""
        assertEquals(3, StillSync.parseStillsAvailable(html))
    }

    @Test
    fun parseStillsAvailable_missingPatternReturnsNull() {
        assertNull(StillSync.parseStillsAvailable("<html>no stills here</html>"))
    }

    @Test
    fun apply_updatesCharacterOnStillShop() {
        val char = KoLCharacter()
        StillSync.apply(
            char,
            """with 5 bright copper stills gleaming in the sun""",
            "https://www.kingdomofloathing.com/shop.php?whichshop=still",
        )
        assertEquals(5, char.state.value.stillsAvailable)
    }

    @Test
    fun apply_skipsNonStillShopUrl() {
        val char = KoLCharacter().also { it.setStillsAvailable(2) }
        StillSync.apply(
            char,
            """with 5 bright copper stills gleaming in the sun""",
            "https://www.kingdomofloathing.com/shop.php?whichshop=general",
        )
        assertEquals(2, char.state.value.stillsAvailable)
    }
}
