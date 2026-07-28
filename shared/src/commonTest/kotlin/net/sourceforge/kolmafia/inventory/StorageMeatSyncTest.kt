package net.sourceforge.kolmafia.inventory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionDatabase

class StorageMeatSyncTest {

    @Test
    fun parseStorageMeat_normalPattern() {
        val html = """<b>You have 178,634,761 meat in long-term storage.</b>"""
        assertEquals(178_634_761L, StorageMeatSync.parseStorageMeat(html, fistcore = false))
    }

    @Test
    fun parseStorageMeat_fistcorePattern() {
        val html = """Hagnk is thinking about the 50,000 you currently have in his vault."""
        assertEquals(50_000L, StorageMeatSync.parseStorageMeat(html, fistcore = true))
    }

    @Test
    fun parseStorageMeat_zeroMeat() {
        val html = """Hagnk doesn't have any of your meat."""
        assertEquals(0L, StorageMeatSync.parseStorageMeat(html, fistcore = false))
    }

    @Test
    fun parseStorageMeat_missingPatternReturnsNull() {
        assertNull(StorageMeatSync.parseStorageMeat("<html>items only</html>", fistcore = false))
    }

    @Test
    fun apply_updatesCharacterOnWhichFive() {
        val char = KoLCharacter()
        StorageMeatSync.apply(
            char,
            """<b>You have 99,999 meat in long-term storage.</b>""",
            "https://www.kingdomofloathing.com/storage.php?which=5",
        )
        assertEquals(99_999L, char.state.value.storageMeat)
    }

    @Test
    fun apply_skipsNonWhichFiveUrl() {
        val char = KoLCharacter().also { it.setStorageMeat(123L) }
        StorageMeatSync.apply(
            char,
            """<b>You have 99,999 meat in long-term storage.</b>""",
            "https://www.kingdomofloathing.com/storage.php?which=1",
        )
        assertEquals(123L, char.state.value.storageMeat)
    }

    @Test
    fun apply_usesFistcorePatternWhenOnSurprisingFistPath() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(path = "Way of the Surprising Fist", kingliberated = "0"),
            )
        }
        StorageMeatSync.apply(
            char,
            """thinking about the 12,345 you currently have""",
            "https://www.kingdomofloathing.com/storage.php?which=5",
        )
        assertEquals(12_345L, char.state.value.storageMeat)
    }

    @Test
    fun parsePullsRemaining_readsPullsleftSpan() {
        val html = """You have <span class="pullsleft">42</span> pulls remaining."""
        assertEquals(42, StorageMeatSync.parsePullsRemaining(html, restricted = false))
    }

    @Test
    fun parsePullsRemaining_fallbackZeroWhenRestricted() {
        assertEquals(0, StorageMeatSync.parsePullsRemaining("<html>no pulls span</html>", restricted = true))
    }

    @Test
    fun parsePullsRemaining_fallbackNegativeOneWhenUnrestrictedAndMissing() {
        assertEquals(-1, StorageMeatSync.parsePullsRemaining("<html>no pulls span</html>", restricted = false))
    }

    @Test
    fun apply_updatesPullsRemainingOnWhichFive() {
        ConcoctionDatabase.resetForTest()
        val char = KoLCharacter()
        StorageMeatSync.apply(
            char,
            """<b>You have 99,999 meat in long-term storage.</b> <span class="pullsleft">7</span>""",
            "https://www.kingdomofloathing.com/storage.php?which=5",
        )
        assertEquals(7, ConcoctionDatabase.getPullsRemaining())
    }
}
