package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.familiar.FamiliarState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.CrownBjornDescSync

class GameRuntimeLibraryAshP96Test {

    @AfterTest
    fun tearDown() {
        ItemDatabase.resetForTest()
    }

    private val sealLarva = FamiliarData(
        id = 1,
        name = "Adorable Seal Larva",
        race = "Seal Larva",
        weight = 5,
        experience = 0,
        kills = 0,
    )

    @Test
    fun revision_phase141() {
        assertEquals("phase370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun crownDescVisit_updatesEnthronedFamiliarFromHtml() {
        val crown = ItemData(
            id = CrownBjornDescSync.CROWN_ITEM_ID,
            name = "Crown of Thrones",
            descId = "239178788",
            image = "chairhat.gif",
            primaryUse = ItemPrimaryUse.HAT,
            secondaryUses = emptySet(),
            access = emptySet(),
            autosellPrice = 0,
            plural = "Crowns of Thrones",
        )
        ItemDatabase.registerForTest(crown)
        val char = KoLCharacter()
        val fm = FamiliarManager(HttpClient(MockEngine { respond("") }), GameEventBus())
        fm.testSetState(FamiliarState(ownedFamiliars = listOf(sealLarva)))
        val lib = GameRuntimeLibrary(
            character = char,
            familiarManager = fm,
            preferences = Preferences(MapSettings()),
        )
        val html =
            """<center><b>Crown of Thrones</b><br>Current Occupant: <b>Adorable Seal Larva the Seal Larva</b></center>"""
        lib.processVisitResponseHooks(html, "desc_item.php?whichitem=${crown.descId}")
        assertEquals("Seal Larva", outputLib(lib, """print(my_enthroned_familiar());"""))
    }

    @Test
    fun bjornDescVisit_updatesBjornedFamiliarFromHtml() {
        val bjorn = ItemData(
            id = CrownBjornDescSync.BJORN_ITEM_ID,
            name = "Buddy Bjorn",
            descId = "697608546",
            image = "buddybjorn.gif",
            primaryUse = ItemPrimaryUse.CONTAINER,
            secondaryUses = emptySet(),
            access = emptySet(),
            autosellPrice = 0,
            plural = null,
        )
        ItemDatabase.registerForTest(bjorn)
        val char = KoLCharacter()
        val fm = FamiliarManager(HttpClient(MockEngine { respond("") }), GameEventBus())
        fm.testSetState(FamiliarState(ownedFamiliars = listOf(sealLarva)))
        val lib = GameRuntimeLibrary(
            character = char,
            familiarManager = fm,
            preferences = Preferences(MapSettings()),
        )
        val html =
            """<center><b>Buddy Bjorn</b><br>Current Occupant: <b>Adorable Seal Larva the Seal Larva</b></center>"""
        lib.processVisitResponseHooks(html, "desc_item.php?whichitem=${bjorn.descId}")
        assertEquals("Seal Larva", outputLib(lib, """print(my_bjornified_familiar());"""))
    }
}
