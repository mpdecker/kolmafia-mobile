package net.sourceforge.kolmafia.event

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class GameEventBusTest {
    @Test
    fun emit_isReceivedBySubscriber() = runTest(UnconfinedTestDispatcher()) {
        val bus = GameEventBus()
        val received = mutableListOf<GameEvent>()
        val job = launch { bus.events.collect { received.add(it) } }

        val location = net.sourceforge.kolmafia.adventure.AdventureLocation("1", "Spooky Forest", "Nearby Plains")
        val result = net.sourceforge.kolmafia.adventure.AdventureResult.NonCombat("A Spooky Treehouse", "", emptyList(), 0)
        bus.emit(GameEvent.TurnConsumed(location, result))

        job.cancel()
        assertEquals(1, received.size)
    }
}
