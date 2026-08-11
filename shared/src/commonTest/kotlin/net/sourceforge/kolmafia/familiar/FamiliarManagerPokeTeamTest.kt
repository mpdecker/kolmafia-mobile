package net.sourceforge.kolmafia.familiar

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.PokefamTeamSlot
import net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase
import net.sourceforge.kolmafia.event.GameEventBus

class FamiliarManagerPokeTeamTest {

    @Test
    fun mergePokeTeam_upsertsByIdAndPreservesExistingStats() = runTest {
        FamiliarDefinitionDatabase.load()
        val client = HttpClient(MockEngine { respond("ok") })
        val manager = FamiliarManager(client, GameEventBus())
        manager.testSetState(
            FamiliarState(
                ownedFamiliars = listOf(
                    FamiliarData(
                        id = 215,
                        name = "Old Name",
                        race = "Globmule",
                        weight = 15,
                        experience = 999,
                        kills = 3,
                        pokeLevel = 1,
                    ),
                ),
            ),
        )
        manager.mergePokeTeam(
            listOf(
                PokefamTeamSlot(familiarId = 215, name = "Globmule", level = 12),
                PokefamTeamSlot(familiarId = 216, name = "Bluzzard", level = 8),
            ),
        )
        val owned = manager.state.value.ownedFamiliars
        assertEquals(2, owned.size)
        val globmule = owned.first { it.id == 215 }
        assertEquals("Globmule", globmule.name)
        assertEquals(12, globmule.pokeLevel)
        assertEquals(15, globmule.weight)
        assertEquals(999, globmule.experience)
        assertEquals(3, globmule.kills)
        val bluzzard = owned.first { it.id == 216 }
        assertEquals("Bluzzard", bluzzard.name)
        assertEquals("Bluzzard", bluzzard.race)
        assertEquals(8, bluzzard.pokeLevel)
        assertEquals(8, bluzzard.weight)
    }

    @Test
    fun registerPokefamFamiliar_upsertsBullpenEntry() = runTest {
        FamiliarDefinitionDatabase.load()
        val client = HttpClient(MockEngine { respond("ok") })
        val manager = FamiliarManager(client, GameEventBus())
        manager.registerPokefamFamiliar(217, "Faux", 5)
        var owned = manager.state.value.ownedFamiliars
        assertEquals(1, owned.size)
        assertEquals(5, owned[0].pokeLevel)
        manager.registerPokefamFamiliar(217, "Faux", 7)
        owned = manager.state.value.ownedFamiliars
        assertEquals(1, owned.size)
        assertEquals(7, owned[0].pokeLevel)
    }
}
