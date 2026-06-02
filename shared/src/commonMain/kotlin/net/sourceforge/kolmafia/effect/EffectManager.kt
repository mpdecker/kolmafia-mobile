package net.sourceforge.kolmafia.effect

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.sourceforge.kolmafia.event.GameEvent
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.http.KOL_BASE_URL

// Verify field names against live api.php?what=effects response before shipping.
@Serializable
private data class EffectApiEntry(
    val name: String = "",
    val duration: Int = 0
)

class EffectManager(
    private val client: HttpClient,
    private val eventBus: GameEventBus
) {
    private val _state = MutableStateFlow(EffectState())
    val state: StateFlow<EffectState> = _state.asStateFlow()

    fun initialize(scope: CoroutineScope) {
        scope.launch {
            fetchEffects()
            eventBus.events.collect { event ->
                when (event) {
                    is GameEvent.TurnConsumed, is GameEvent.SkillCast -> fetchEffects()
                    else -> {}
                }
            }
        }
    }

    suspend fun fetchEffects() {
        try {
            val response = client.get("$KOL_BASE_URL/api.php") {
                parameter("what", "effects")
                parameter("for", "KoLmafia-Mobile")
            }
            if (!response.status.isSuccess()) {
                _state.value = _state.value.copy(isStale = true)
                return
            }
            val raw: Map<String, EffectApiEntry> = response.body()
            val effects = raw.entries.mapNotNull { (idStr, entry) ->
                val id = idStr.toIntOrNull() ?: return@mapNotNull null
                EffectData(id = id, name = entry.name, duration = entry.duration)
            }.sortedBy { it.name }
            _state.value = EffectState(effects = effects, isStale = false)
            eventBus.tryEmit(GameEvent.EffectsRefreshed)
        } catch (e: Exception) {
            _state.value = _state.value.copy(isStale = true)
        }
    }
}
