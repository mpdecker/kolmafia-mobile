package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.equipment.OutfitManager
import net.sourceforge.kolmafia.equipment.ResolvedOutfit
import net.sourceforge.kolmafia.request.CustomOutfitRequest
import net.sourceforge.kolmafia.request.EquipmentRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameRuntimeLibraryAshP494Test {

    private fun lines(out: String): List<String> =
        out.lines().map { it.trim() }.filter { it.isNotEmpty() }

    private fun listingLib(
        outfits: List<String>,
        worn: MutableList<String> = mutableListOf(),
    ): GameRuntimeLibrary {
        val manager = object : OutfitManager(
            retrieveItemService = null,
            equipmentRequest = EquipmentRequest(HttpClient(MockEngine { respond("") })),
            customOutfitRequest = CustomOutfitRequest(HttpClient(MockEngine { respond("") })),
            character = KoLCharacter(),
            gameDatabase = GameDatabase(),
            closetRequest = null,
            storageRequest = null,
            displayCaseRequest = null,
            clanStashRequest = null,
            inventoryManager = null,
        ) {
            override suspend fun getOutfitsWithPieces(): List<ResolvedOutfit> =
                outfits.map { ResolvedOutfit(1, it, emptyList()) }

            override suspend fun wearOutfit(name: String, postWear: ((String) -> Unit)?): Boolean {
                worn += name
                return true
            }
        }
        return GameRuntimeLibrary(outfitManager = manager)
    }

    @Test
    fun revision_phase494() {
        assertEquals("phase635", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun outfit_bareListsWithoutWearing() {
        val worn = mutableListOf<String>()
        val listed = lines(
            outputLib(
                listingLib(listOf("Mining Gear", "Frat Boy Ensemble"), worn),
                """cli_execute("outfit");""",
            ),
        )
        assertTrue(listed.contains("Mining Gear"))
        assertTrue(listed.contains("Frat Boy Ensemble"))
        assertTrue(worn.isEmpty())
    }

    @Test
    fun outfit_bareEmptyPrintsNothing() {
        val listed = lines(outputLib(listingLib(emptyList()), """cli_execute("outfit");"""))
        assertTrue(listed.isEmpty())
    }

    @Test
    fun outfit_listStillFilters() {
        val listed = lines(
            outputLib(
                listingLib(listOf("Mining Gear", "Frat Boy Ensemble")),
                """cli_execute("outfit list Mining");""",
            ),
        )
        assertTrue(listed.contains("Mining Gear"))
        assertFalse(listed.contains("Frat Boy Ensemble"))
    }

    @Test
    fun outfit_namedStillWears() {
        val worn = mutableListOf<String>()
        outputLib(listingLib(listOf("Mining Gear"), worn), """cli_execute("outfit Mining Gear");""")
        assertEquals(listOf("Mining Gear"), worn)
    }
}
