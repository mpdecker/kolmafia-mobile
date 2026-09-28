package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.familiar.FamiliarState
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ChoiceCombatAshState
import net.sourceforge.kolmafia.session.SessionLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UseItemAprilPlaySyncTest {

    private fun prefs() = Preferences(MapSettings())

    private fun inventory(): InventoryManager =
        InventoryManager(
            HttpClient(MockEngine { respond("", HttpStatusCode.OK) }),
            GameEventBus(),
        )

    @Test
    fun saxophone_logsThePlayAndIncrements() {
        val prefs = prefs()
        val logger = SessionLogger(prefs, GameEventBus())
        UseItemAprilPlaySync.parse(
            "inventory.php?iid=11566&action=aprilplay",
            "You play a sexy sax solo.",
            preferences = prefs,
            sessionLogger = logger,
            itemName = { "Apriling band saxophone" },
        )
        assertEquals(1, prefs.getInt("_aprilBandSaxophoneUses", 0))
        assertTrue(logger.recentLines().any { it == "Playing Apriling band saxophone" })
    }

    @Test
    fun enoughForOneDay_capsTheCounter() {
        val prefs = prefs()
        prefs.setInt("_aprilBandStaffUses", 1)
        UseItemAprilPlaySync.parse(
            "inventory.php?iid=11569&action=aprilplay",
            "You've already played this instrument enough for one day.",
            preferences = prefs,
            itemName = { "Apriling band staff" },
        )
        assertEquals(3, prefs.getInt("_aprilBandStaffUses", 0))
    }

    @Test
    fun saxophone_alreadyLucky_doesNotIncrement() {
        val prefs = prefs()
        UseItemAprilPlaySync.parse(
            "inventory.php?iid=11566&action=aprilplay",
            "You already seem lucky enough, maybe play a sexy sax solo later.",
            preferences = prefs,
        )
        assertEquals(0, prefs.getInt("_aprilBandSaxophoneUses", 0))
    }

    @Test
    fun tuba_forcesANoncombat() {
        val prefs = prefs()
        UseItemAprilPlaySync.parse(
            "https://www.kingdomofloathing.com/inventory.php?action=aprilplay&iid=11568",
            "You blast a note.",
            preferences = prefs,
            itemName = { "Apriling band tuba" },
        )
        assertEquals(true, prefs.getBoolean("noncombatForcerActive", false))
        assertEquals(1, prefs.getInt("_aprilBandTubaUses", 0))
    }

    @Test
    fun tom_hooksStillOn_marksGnasirAndDesert() {
        val prefs = prefs()
        prefs.setInt("gnasirProgress", 1)
        prefs.setInt("desertExploration", 10)
        val inventory = inventory()
        inventory.gainItemLocally(UseItemAprilPlaySync.WORM_RIDING_HOOKS, 1)
        UseItemAprilPlaySync.parse(
            "inventory.php?iid=11567&action=aprilplay",
            "And dammit, your hooks were still on there! Oh well.",
            preferences = prefs,
            inventory = inventory,
            itemName = { "Apriling band quad tom" },
        )
        assertEquals(17, prefs.getInt("gnasirProgress", 0))
        assertEquals(40, prefs.getInt("desertExploration", 0))
        assertEquals(0, inventory.getCount(UseItemAprilPlaySync.WORM_RIDING_HOOKS))
        assertEquals(1, prefs.getInt("_aprilBandTomUses", 0))
    }

    @Test
    fun tom_underYourFeet_clearsAdventureAndMarksTheFight() {
        val prefs = prefs()
        prefs.setString("lastAdventure", "The Haunted Kitchen")
        prefs.setString("nextAdventure", "The Haunted Kitchen")
        prefs.setInt("turnsPlayed", 12)
        val logger = SessionLogger(prefs, GameEventBus())
        ChoiceCombatAshState.inMultiFight = false
        try {
            UseItemAprilPlaySync.parse(
                "inventory.php?iid=11567&action=aprilplay",
                """Something moves under your feet. <a href="fight.php">Fight!</a>""",
                preferences = prefs,
                sessionLogger = logger,
                itemName = { "Apriling band quad tom" },
            )
            assertEquals("None", prefs.getString("lastAdventure", ""))
            assertEquals("None", prefs.getString("nextAdventure", ""))
            assertTrue(logger.recentLines().any { it == "[12] Apriling Band Quad Tom" })
            assertEquals(true, ChoiceCombatAshState.inMultiFight)
        } finally {
            ChoiceCombatAshState.inMultiFight = false
        }
    }

    @Test
    fun piccolo_handsOver_addsFamiliarExperience() {
        val char = KoLCharacter().also { it.updateFamiliar(1, "Pet", 1, 5) }
        val manager = FamiliarManager(
            HttpClient(MockEngine { respond("", HttpStatusCode.OK) }),
            GameEventBus(),
        )
        val familiar = FamiliarData(1, "Pet", "Leprechaun", 1, 5, 0)
        manager.testSetState(FamiliarState(activeFamiliar = familiar, ownedFamiliars = listOf(familiar)))
        UseItemAprilPlaySync.parse(
            "inventory.php?iid=11570&action=aprilplay",
            "You hand the piccolo to your familiar.",
            character = char,
            familiarManager = manager,
            itemName = { "Apriling band piccolo" },
        )
        assertEquals(45, char.state.value.familiarExp)
        assertEquals(45, manager.state.value.activeFamiliar?.experience)
    }

    @Test
    fun piccolo_uninterested_doesNotIncrement() {
        val char = KoLCharacter().also { it.updateFamiliar(1, "Pet", 1, 5) }
        val prefs = prefs()
        UseItemAprilPlaySync.parse(
            "inventory.php?iid=11570&action=aprilplay",
            "Your familiar doesn't seem interested in playing.",
            preferences = prefs,
            character = char,
        )
        assertEquals(0, prefs.getInt("_aprilBandPiccoloUses", 0))
        assertEquals(5, char.state.value.familiarExp)
    }
}
