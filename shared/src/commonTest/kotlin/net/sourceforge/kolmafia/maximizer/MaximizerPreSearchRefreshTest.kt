package net.sourceforge.kolmafia.maximizer

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.effect.EffectManager
import kotlin.test.Test
import kotlin.test.assertTrue

class MaximizerPreSearchRefreshTest {

    @Test
    fun refresh_delegatesToRefreshCharacterStatus() = runBlocking {
        var syncCalled = false
        val inv = object : InventoryManager(
            client = HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) }),
            eventBus = GameEventBus(),
        ) {
            override suspend fun refreshCharacterStatus(effectManager: EffectManager?): Boolean {
                syncCalled = true
                return true
            }
        }
        MaximizerPreSearchRefresh.refresh(inv, null)
        assertTrue(syncCalled)
    }
}
