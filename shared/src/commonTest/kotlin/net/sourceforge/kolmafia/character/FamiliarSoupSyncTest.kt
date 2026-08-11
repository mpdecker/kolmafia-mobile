package net.sourceforge.kolmafia.character

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.familiar.FamiliarState

class FamiliarSoupSyncTest {

    private fun soupHtml(json: String): String =
        """<html><!-- some soup for you! "$json" --></html>"""

    private fun familiarManagerWith(vararg familiars: FamiliarData): FamiliarManager {
        val client = HttpClient(MockEngine { respond("ok") })
        val manager = FamiliarManager(client, GameEventBus())
        manager.testSetState(FamiliarState(ownedFamiliars = familiars.toList()))
        return manager
    }

    @Test
    fun parse_extractsTimesAndAttributesForMultipleFamiliars() {
        val json = """{\"1\":{\"times\":5,\"attr\":[\"mp\",\"damage\"]},\"2\":{\"times\":3,\"attr\":[\"hp\"]}}"""
        val parsed = FamiliarSoupSync.parse(soupHtml(json))
        assertEquals(5, parsed[1]?.times)
        assertEquals(listOf("mp", "damage"), parsed[1]?.attributes)
        assertEquals(3, parsed[2]?.times)
        assertEquals(listOf("hp"), parsed[2]?.attributes)
    }

    @Test
    fun apply_updatesOwnedFamiliarSoupState() {
        val manager = familiarManagerWith(
            FamiliarData(id = 1, name = "Mosquito", race = "Mosquito", weight = 5, experience = 0, kills = 0),
            FamiliarData(id = 99, name = "Other", race = "Other", weight = 1, experience = 0, kills = 0),
        )
        val json = """{\"1\":{\"times\":7,\"attr\":[\"act\",\"stats\"]}}"""
        FamiliarSoupSync.apply(soupHtml(json), manager)
        val updated = manager.state.value.ownedFamiliars.first { it.id == 1 }
        assertEquals(7, updated.soupWeight)
        assertEquals(setOf("act", "stats"), updated.soupAttributes)
    }

    @Test
    fun apply_skipsUnownedFamiliarIds() {
        val manager = familiarManagerWith(
            FamiliarData(id = 1, name = "Mosquito", race = "Mosquito", weight = 5, experience = 0, kills = 0),
        )
        val json = """{\"1\":{\"times\":2,\"attr\":[\"mp\"]},\"42\":{\"times\":9,\"attr\":[\"hp\"]}}"""
        FamiliarSoupSync.apply(soupHtml(json), manager)
        assertEquals(1, manager.state.value.ownedFamiliars.size)
        assertEquals(2, manager.state.value.ownedFamiliars.first().soupWeight)
    }

    @Test
    fun applySoupData_capsWeightAt111() {
        val manager = familiarManagerWith(
            FamiliarData(id = 1, name = "Mosquito", race = "Mosquito", weight = 5, experience = 0, kills = 0),
        )
        manager.applySoupData(1, 200, listOf("mp"))
        assertEquals(111, manager.state.value.ownedFamiliars.first().soupWeight)
    }

    @Test
    fun applyProtogeneticSoupUse_incrementsActiveFamiliarAndAttribute() {
        val manager = familiarManagerWith(
            FamiliarData(id = 10, name = "Penguin", race = "Penguin", weight = 5, experience = 0, kills = 0),
        )
        FamiliarSoupSync.applyProtogeneticSoupUse(
            itemId = FamiliarSoupSync.SYNAPTIC_SOUP,
            html = "used soup",
            familiarId = 10,
            familiarManager = manager,
        )
        val updated = manager.state.value.ownedFamiliars.first()
        assertEquals(1, updated.soupWeight)
        assertEquals(setOf("mp"), updated.soupAttributes)
    }

    @Test
    fun containsSoupComment_detectsCommentMarker() {
        assertTrue(FamiliarSoupSync.containsSoupComment(soupHtml("{}")))
        assertFalse(FamiliarSoupSync.containsSoupComment("<html></html>"))
    }
}
