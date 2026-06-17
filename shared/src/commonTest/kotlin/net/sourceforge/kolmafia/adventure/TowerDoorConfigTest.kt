package net.sourceforge.kolmafia.adventure

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class TowerDoorConfigTest {

    private fun prefs() = Preferences(MapSettings())

    @Test
    fun parseTowerDoor_deducesUsedKeysFromMissingActions() {
        val html = """
            <a href="place.php?whichplace=nstower_door&action=ns_lock3 ">Pete</a>
            <a href="place.php?whichplace=nstower_door&action=ns_lock4 ">Star</a>
            <a href="place.php?whichplace=nstower_door&action=ns_lock5 ">Digital</a>
            <a href="place.php?whichplace=nstower_door&action=ns_lock6 ">Skeleton</a>
            <a href="place.php?whichplace=nstower_door&action=ns_doorknob ">Knob</a>
        """.trimIndent()
        val used = TowerDoorConfig.parseTowerDoor(html)
        assertEquals("Boris's key,Jarlsberg's key", used)
    }

    @Test
    fun parseTowerDoor_allLocksPresent_meansNoKeysUsed() {
        val html = buildString {
            for (lock in TowerDoorConfig.STANDARD_LOCKS) {
                append("""<a href="action=${lock.action} ">x</a> """)
            }
        }
        assertEquals("", TowerDoorConfig.parseTowerDoor(html))
    }

    @Test
    fun appendKeyUsed_appendsCommaSeparatedNames() {
        val prefs = prefs()
        TowerDoorConfig.appendKeyUsed(prefs, "Boris's key")
        TowerDoorConfig.appendKeyUsed(prefs, "Jarlsberg's key")
        assertEquals("Boris's key,Jarlsberg's key", prefs.getString(TowerDoorConfig.KEYS_USED_PREF, ""))
    }

    @Test
    fun neededLocks_excludesUsedKeys() {
        val prefs = prefs()
        prefs.setString(TowerDoorConfig.KEYS_USED_PREF, "Boris's key,Jarlsberg's key")
        val needed = TowerDoorConfig.neededLocks(prefs, TowerDoorConfig.STANDARD_LOCKS)
        assertEquals(4, needed.size)
        assertFalse(needed.any { it.keyName == "Boris's key" })
    }

    @Test
    fun lowKeyLocks_hasThirtyEntries() {
        assertEquals(30, TowerDoorConfig.LOW_KEY_LOCKS.size)
    }

    @Test
    fun lowKeyLocks_adventureLocksHaveLocationMetadata() {
        val clownLock = TowerDoorConfig.LOW_KEY_LOCKS.first { it.keyItemId == net.sourceforge.kolmafia.adventure.choice.ItemPool.CLOWN_CAR_KEY }
        assertEquals("The \"Fun\" House", clownLock.locationName)
        assertEquals("Carpool Lane", clownLock.encounterName)
        assertTrue(clownLock.action.startsWith("nstower_doowlow"))
    }

    @Test
    fun locksFor_lowKeyPath_returnsLowKeyTable() {
        val state = net.sourceforge.kolmafia.character.CharacterState(challengePath = "Low Key")
        assertEquals(TowerDoorConfig.LOW_KEY_LOCKS, TowerDoorConfig.locksFor(state))
        assertEquals(TowerDoorConfig.LOW_KEY_DOOR_PLACE, TowerDoorConfig.doorPlaceFor(state))
    }

    @Test
    fun adventureKeyErrorMessage_matchesDesktop() {
        val lock = TowerDoorConfig.LOW_KEY_LOCKS.first { it.keyItemId == net.sourceforge.kolmafia.adventure.choice.ItemPool.ICE_KEY }
        assertEquals(
            "Adventure in The Icy Peak until you find a ice key",
            TowerDoorConfig.adventureKeyErrorMessage(lock),
        )
    }

    @Test
    fun isUnlockSuccess_matchesDesktopPatterns() {
        assertTrue(TowerDoorConfig.isUnlockSuccess("You hear a jolly bellowing as the lock vanishes."))
        assertTrue(TowerDoorConfig.isUnlockSuccess("When you turn back to the lock it is gone."))
        assertFalse(TowerDoorConfig.isUnlockSuccess("There's at least one lock left locked."))
    }
}
