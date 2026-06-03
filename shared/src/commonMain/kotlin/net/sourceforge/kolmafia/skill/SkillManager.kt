package net.sourceforge.kolmafia.skill

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

// Verify field names against live api.php?what=skills response before shipping.
@Serializable
private data class SkillApiEntry(
    val name: String = "",
    val type: Int = 0,
    val dailylimit: Int = 0,
    val timescast: Int = 0,
    val mpcost: Int = 0
)

open class SkillManager(
    private val client: HttpClient,
    private val castRequest: SkillCastRequest,
    private val eventBus: GameEventBus
) {
    private val _state = MutableStateFlow(SkillState())
    val state: StateFlow<SkillState> = _state.asStateFlow()

    fun initialize(scope: CoroutineScope) {
        scope.launch { fetchSkills() }
    }

    suspend fun fetchSkills() {
        try {
            val response = client.get("$KOL_BASE_URL/api.php") {
                parameter("what", "skills")
                parameter("for", "KoLmafia-Mobile")
            }
            if (!response.status.isSuccess()) {
                _state.value = _state.value.copy(isStale = true)
                return
            }
            val raw: Map<String, SkillApiEntry> = response.body()
            val skills = raw.entries.mapNotNull { (idStr, entry) ->
                val id = idStr.toIntOrNull() ?: return@mapNotNull null
                SkillData(
                    id = id,
                    name = entry.name,
                    type = SkillType.fromApiInt(entry.type),
                    mpCost = entry.mpcost,
                    dailyLimit = entry.dailylimit,
                    timesCast = entry.timescast
                )
            }.sortedBy { it.name }
            _state.value = SkillState(skills = skills, isStale = false)
        } catch (e: Exception) {
            _state.value = _state.value.copy(isStale = true)
        }
    }

    open suspend fun cast(skill: SkillData, quantity: Int = 1): Result<Unit> {
        val result = castRequest.cast(skill.id, quantity)
        result.onSuccess {
            val updatedSkills = _state.value.skills.map { s ->
                if (s.id == skill.id) s.copy(timesCast = s.timesCast + quantity) else s
            }
            _state.value = _state.value.copy(skills = updatedSkills)
            eventBus.tryEmit(GameEvent.SkillCast(skill.id, skill.name, quantity))
        }
        return result.map { Unit }
    }
}
