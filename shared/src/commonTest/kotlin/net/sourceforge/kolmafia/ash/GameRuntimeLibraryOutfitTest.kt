package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import net.sourceforge.kolmafia.data.OutfitData
import net.sourceforge.kolmafia.data.OutfitDatabase
import net.sourceforge.kolmafia.equipment.OutfitManager
import net.sourceforge.kolmafia.equipment.ResolvedOutfit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryOutfitTest {

    @BeforeTest
    fun setup() {
        OutfitDatabase.clearCustom()
        OutfitDatabase.registerCustom(
            OutfitData(-3, "Test Mining", "", listOf("miner's helmet"), emptyList())
        )
        OutfitDatabase.registerStatic(
            OutfitData(8, "Mining Gear", "miner.gif", listOf("miner's helmet"), emptyList())
        )
        OutfitDatabase.registerStatic(
            OutfitData(3, "Frat Boy Ensemble", "fratboy.gif", listOf("Orcish baseball cap"), listOf("bottle of gin (0.25)"))
        )
    }

    @AfterTest
    fun teardown() {
        OutfitDatabase.clearCustom()
    }

    @Test
    fun is_wearing_outfit_delegatesToManager() {
        var checked: String? = null
        val manager = object : OutfitManager(
            retrieveItemService = null,
            equipmentRequest = net.sourceforge.kolmafia.request.EquipmentRequest(HttpClient(MockEngine { respond("") })),
            customOutfitRequest = net.sourceforge.kolmafia.request.CustomOutfitRequest(HttpClient(MockEngine { respond("") })),
            character = net.sourceforge.kolmafia.character.KoLCharacter(),
            gameDatabase = net.sourceforge.kolmafia.data.GameDatabase(),
            closetRequest = null,
            storageRequest = null,
            displayCaseRequest = null,
            clanStashRequest = null,
            inventoryManager = null,
        ) {
            override fun getMatchingOutfit(name: String) =
                ResolvedOutfit(-3, name, listOf("miner's helmet"))

            override fun isWearingOutfit(
                outfit: ResolvedOutfit,
                equipment: Map<net.sourceforge.kolmafia.character.EquipmentSlot, String>,
            ): Boolean {
                checked = outfit.name
                return true
            }
        }
        val lib = GameRuntimeLibrary(outfitManager = manager)
        assertEquals("true", outputLib(lib, """print(to_string(is_wearing_outfit("Test Mining")));"""))
        assertEquals("Test Mining", checked)
    }

    @Test
    fun outfit_pieces_returnsItemArray() {
        val manager = object : OutfitManager(
            retrieveItemService = null,
            equipmentRequest = net.sourceforge.kolmafia.request.EquipmentRequest(HttpClient(MockEngine { respond("") })),
            customOutfitRequest = net.sourceforge.kolmafia.request.CustomOutfitRequest(HttpClient(MockEngine { respond("") })),
            character = net.sourceforge.kolmafia.character.KoLCharacter(),
            gameDatabase = net.sourceforge.kolmafia.data.GameDatabase(),
            closetRequest = null,
            storageRequest = null,
            displayCaseRequest = null,
            clanStashRequest = null,
            inventoryManager = null,
        ) {
            override fun getMatchingOutfit(name: String) =
                net.sourceforge.kolmafia.equipment.ResolvedOutfit(-3, name, listOf("miner's helmet"))
        }
        val lib = GameRuntimeLibrary(outfitManager = manager)
        val out = outputLib(lib, """print(outfit_pieces("Test Mining")[0]);""")
        assertTrue(out.contains("miner's helmet"))
    }

    @Test
    fun outfit_tattoo_returnsImageFromDatabase() {
        val lib = GameRuntimeLibrary(outfitManager = null)
        val out = outputLib(lib, """print(outfit_tattoo("Frat Boy Ensemble"));""")
        assertEquals("fratboy.gif", out.trim())
    }

    @Test
    fun get_custom_outfits_listsRegisteredCustoms() {
        val lib = GameRuntimeLibrary(outfitManager = null)
        val out = outputLib(lib, """print(get_custom_outfits()[0]);""")
        assertEquals("Test Mining", out.trim())
    }

    @Test
    fun all_normal_outfits_includesStaticOutfits() {
        val lib = GameRuntimeLibrary(outfitManager = null)
        val out = outputLib(lib, """print(all_normal_outfits()[8]);""")
        assertEquals("Mining Gear", out.trim())
    }
}
