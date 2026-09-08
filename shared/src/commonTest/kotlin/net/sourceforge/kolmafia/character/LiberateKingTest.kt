package net.sourceforge.kolmafia.character

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.ash.CollectionCache
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.inventory.CollectionCacheSync
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiberateKingTest {

    @Test
    fun liberateKing_awardsAwolPointsAndLiftsHardcore() {
        val prefs = Preferences(MapSettings())
        CollectionCache.save(prefs, Preferences.CACHED_FREEPULLS, mapOf(100 to 2))
        CollectionCache.save(prefs, Preferences.CACHED_STORAGE, mapOf(100 to 1, 200 to 3))
        ConcoctionDatabase.setPullsRemaining(5)

        val char = KoLCharacter()
        char.updateFromApiResponse(
            CharacterApiResponse(
                hardcore = "1",
                roninleft = "40",
                kingliberated = "0",
                path = "Avatar of West of Loathing",
                classId = "9", // Beanslinger (mobile id)
            ),
        )
        assertFalse(char.state.value.kingLiberated)
        assertTrue(char.state.value.isHardcore)

        char.liberateKing(prefs)

        assertTrue(char.state.value.kingLiberated)
        assertFalse(char.state.value.isHardcore)
        assertEquals(0, char.state.value.roninLeft)
        assertTrue(char.state.value.canInteract)
        assertEquals(2, prefs.getInt("awolPointsBeanslinger", 0)) // hardcore = 2 pts
        assertEquals(-1, ConcoctionDatabase.getPullsRemaining())
        assertFalse(prefs.getBoolean("breakfastCompleted", true))
        // Avatar path retained until class pick
        assertEquals("Avatar of West of Loathing", char.state.value.challengePath)
        val storage = CollectionCache.load(prefs, Preferences.CACHED_STORAGE)
        assertEquals(3, storage[100]) // 1 + 2 freepull
        assertEquals(3, storage[200])
        assertTrue(CollectionCache.load(prefs, Preferences.CACHED_FREEPULLS).isEmpty())
    }

    @Test
    fun liberateKing_asolPointsAndClearsNonAvatarPath() {
        val prefs = Preferences(MapSettings())
        val char = KoLCharacter()
        char.updateFromApiResponse(
            CharacterApiResponse(
                hardcore = "0",
                roninleft = "0",
                kingliberated = "0",
                path = "G-Lover",
                classId = "1",
            ),
        )
        char.liberateKing(prefs)
        assertEquals(1, prefs.getInt("gloverPoints", 0))
        assertEquals(1, prefs.getInt("garlandUpgrades", 0))
        assertEquals("None", char.state.value.challengePath)
    }

    @Test
    fun liberateKing_idempotentWhenAlreadyLiberated() {
        val prefs = Preferences(MapSettings())
        val char = KoLCharacter()
        char.updateFromApiResponse(
            CharacterApiResponse(kingliberated = "1", path = "G-Lover"),
        )
        char.liberateKing(prefs)
        assertEquals(0, prefs.getInt("gloverPoints", 0))
    }

    @Test
    fun mergeFreepullsIntoStorage_helper() {
        val prefs = Preferences(MapSettings())
        CollectionCache.save(prefs, Preferences.CACHED_STORAGE, mapOf(1 to 1))
        CollectionCache.save(prefs, Preferences.CACHED_FREEPULLS, mapOf(1 to 4, 2 to 1))
        CollectionCacheSync.mergeFreepullsIntoStorage(prefs)
        assertEquals(5, CollectionCache.load(prefs, Preferences.CACHED_STORAGE)[1])
        assertEquals(1, CollectionCache.load(prefs, Preferences.CACHED_STORAGE)[2])
        assertTrue(CollectionCache.load(prefs, Preferences.CACHED_FREEPULLS).isEmpty())
    }
}
