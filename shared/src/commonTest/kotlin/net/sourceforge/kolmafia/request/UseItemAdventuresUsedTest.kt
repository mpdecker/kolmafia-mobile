package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals

class UseItemAdventuresUsedTest {

    @Test
    fun fightAndChoiceItems_costOneTurnPerItem() {
        assertEquals(1, UseItemAdventuresUsed.getAdventuresUsed("inv_use.php?whichitem=8599"))
        assertEquals(3, UseItemAdventuresUsed.getAdventuresUsed("inv_use.php?whichitem=4509&quantity=3"))
        assertEquals(1, UseItemAdventuresUsed.getAdventuresUsed("inv_use.php?action=chateau_painting"))
        assertEquals(1, UseItemAdventuresUsed.getAdventuresUsed("inv_use.php?fightgodlobster=1"))
        assertEquals(1, UseItemAdventuresUsed.getAdventuresUsed("inv_use.php?action=witchess"))
    }

    @Test
    fun dice_costTurnsOnlyAtDesktopCounts() {
        assertEquals(0, UseItemAdventuresUsed.getAdventuresUsed("inv_use.php?whichitem=5285&quantity=1"))
        assertEquals(100, UseItemAdventuresUsed.getAdventuresUsed("inv_use.php?whichitem=5285&quantity=100"))
        assertEquals(1, UseItemAdventuresUsed.getAdventuresUsed("inv_use.php?whichitem=5288&quantity=1"))
        assertEquals(2, UseItemAdventuresUsed.getAdventuresUsed("inv_use.php?whichitem=5288&quantity=2"))
        assertEquals(0, UseItemAdventuresUsed.getAdventuresUsed("inv_use.php?whichitem=5288&quantity=3"))
    }

    @Test
    fun drumMachine_isFreeWithWormRidingHooks() {
        assertEquals(1, UseItemAdventuresUsed.getAdventuresUsed("inv_use.php?whichitem=2328"))
        assertEquals(
            0,
            UseItemAdventuresUsed.getAdventuresUsed(
                "inv_use.php?whichitem=2328",
                ownsItem = { it == UseItemAdventuresUsed.WORM_RIDING_HOOKS },
            ),
        )
        assertEquals(
            0,
            UseItemAdventuresUsed.getAdventuresUsed(
                "inv_use.php?whichitem=2328",
                equippedWeaponId = UseItemAdventuresUsed.WORM_RIDING_HOOKS,
            ),
        )
    }

    @Test
    fun gong_costsThreeTurnsOnRoachform() {
        val prefs = Preferences(MapSettings())
        assertEquals(0, UseItemAdventuresUsed.getAdventuresUsed("inv_use.php?whichitem=3353", prefs))
        prefs.setInt("choiceAdventure276", 1)
        assertEquals(3, UseItemAdventuresUsed.getAdventuresUsed("inv_use.php?whichitem=3353", prefs))
        assertEquals(6, UseItemAdventuresUsed.getAdventuresUsed("inv_use.php?whichitem=3353&quantity=2", prefs))
    }

    @Test
    fun deck_followsTheDrawnCard() {
        assertEquals(1, UseItemAdventuresUsed.getAdventuresUsed("inv_use.php?whichitem=8382"))
        assertEquals(0, UseItemAdventuresUsed.getAdventuresUsed("inv_use.php?which=3&whichitem=8382"))
        assertEquals(1, DeckOfEveryCardRequest.getAdventuresUsed("choice.php?whichchoice=1086&which=46"))
        assertEquals(0, DeckOfEveryCardRequest.getAdventuresUsed("choice.php?whichchoice=1086&which=51"))
        assertEquals(1, DeckOfEveryCardRequest.getAdventuresUsed(null as DeckOfEveryCardRequest.EveryCard?))
        assertEquals(0, DeckOfEveryCardRequest.getAdventuresUsed(DeckOfEveryCardRequest.STRENGTH))
    }

    @Test
    fun proxyItems_costTurnsOnlyOnTheirActionUrl() {
        assertEquals(1, UseItemAdventuresUsed.getAdventuresUsed("inv_use.php?whichitem=8033"))
        assertEquals(0, UseItemAdventuresUsed.forItem(8033, 1))
        assertEquals(0, UseItemAdventuresUsed.forItem(9661, 1))
        assertEquals(0, UseItemAdventuresUsed.forItem(8989, 1))
    }

    @Test
    fun otherPages_costNothing() {
        assertEquals(0, UseItemAdventuresUsed.getAdventuresUsed("place.php?whichplace=desertbeach"))
        assertEquals(0, UseItemAdventuresUsed.getAdventuresUsed("inventory.php?action=ghost&whichitem=1"))
        assertEquals(0, UseItemAdventuresUsed.getAdventuresUsed("inv_use.php?whichitem=1"))
    }
}
