package net.sourceforge.kolmafia.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class GameEventBus {
    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    suspend fun emit(event: GameEvent) = _events.emit(event)
    fun tryEmit(event: GameEvent) = _events.tryEmit(event)
}
