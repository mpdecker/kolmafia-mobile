package net.sourceforge.kolmafia.familiar

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.sourceforge.kolmafia.event.GameEvent
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.ItemType

// Verify field names against actual api.php?what=familiars response before shipping.
@Serializable
private data class FamiliarApiEntry(
    val id: Int = 0,
    val name: String = "",
    val race: String = "",
    val weight: Int = 1,
    val exp: Int = 0,
    val kills: Int = 0,
    @SerialName("active") val isActive: Boolean = false
)

open class FamiliarManager(
    private val client: HttpClient,
    private val eventBus: GameEventBus
) {
    private val _state = MutableStateFlow(FamiliarState())
    val state: StateFlow<FamiliarState> = _state.asStateFlow()

    private val familiarRequest = FamiliarRequest(client)
    private val equipRequest = FamiliarEquipRequest(client)
    private val hatcheryRequest = HatcheryRequest(client)
    private val actionRequest = FamiliarActionRequest(client)

    fun initialize(scope: CoroutineScope) {
        scope.launch { fetchFamiliars() }
    }

    suspend fun fetchFamiliars() {
        try {
            val response = client.get("$KOL_BASE_URL/api.php") {
                parameter("what", "familiars")
                parameter("for", "KoLmafia-Mobile")
            }
            if (!response.status.isSuccess()) {
                _state.value = _state.value.copy(isStale = true)
                return
            }
            val entries: List<FamiliarApiEntry> = response.body()
            val familiars = entries.map { e ->
                FamiliarData(
                    id = e.id,
                    name = e.name,
                    race = e.race,
                    weight = e.weight,
                    experience = e.exp,
                    kills = e.kills
                )
            }
            val active = familiars.firstOrNull { f ->
                entries.find { e -> e.id == f.id }?.isActive == true
            }
            _state.value = FamiliarState(active, familiars, isStale = false)
        } catch (e: Exception) {
            _state.value = _state.value.copy(isStale = true)
        }
    }

    suspend fun switchFamiliar(familiar: FamiliarData): Result<Unit> {
        familiarRequest.switchFamiliar(familiar.id).onFailure { return Result.failure(it) }
        fetchFamiliars()
        eventBus.emit(GameEvent.FamiliarSwitched(familiar))
        return Result.success(Unit)
    }

    suspend fun equipItem(familiar: FamiliarData, itemId: Int): Result<Unit> {
        val item = InventoryItem(itemId, "Familiar item", 1, ItemType.FAMILIAR_ITEM)
        equipRequest.equip(itemId).onFailure { return Result.failure(it) }
        fetchFamiliars()
        eventBus.emit(GameEvent.FamiliarEquipped(familiar, item))
        return Result.success(Unit)
    }

    suspend fun hatch(eggItemId: Int): Result<Unit> {
        hatcheryRequest.hatch(eggItemId).onFailure { return Result.failure(it) }
        fetchFamiliars()
        val newFamiliar = _state.value.ownedFamiliars.lastOrNull()
            ?: return Result.success(Unit)
        eventBus.emit(GameEvent.FamiliarHatched(newFamiliar))
        return Result.success(Unit)
    }

    suspend fun performAction(action: FamiliarAction): Result<Unit> =
        actionRequest.perform(action)

    // ── Phase 10 ASH helpers ──────────────────────────────────────────────────

    /** Sets the active familiar by species name (race). Returns failure if not owned. */
    open suspend fun setFamiliar(name: String): Result<Unit> {
        val familiar = state.value.ownedFamiliars
            .find { it.race.equals(name, ignoreCase = true) }
            ?: return Result.failure(Exception("Familiar not owned: $name"))
        return switchFamiliar(familiar)
    }

    /** Enthrone a familiar by species name, or clear with "none"/empty. */
    open suspend fun setEnthroned(name: String): Result<Unit> {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.equals("none", ignoreCase = true)) {
            return familiarRequest.enthrone(0)
        }
        val familiar = state.value.ownedFamiliars
            .find { it.race.equals(trimmed, ignoreCase = true) }
            ?: return Result.failure(Exception("Familiar not owned: $trimmed"))
        return familiarRequest.enthrone(familiar.id)
    }

    /** Bjornify a familiar by species name, or clear with "none"/empty. */
    open suspend fun setBjornified(name: String): Result<Unit> {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.equals("none", ignoreCase = true)) {
            return familiarRequest.bjornify(0)
        }
        val familiar = state.value.ownedFamiliars
            .find { it.race.equals(trimmed, ignoreCase = true) }
            ?: return Result.failure(Exception("Familiar not owned: $trimmed"))
        return familiarRequest.bjornify(familiar.id)
    }

    /** Test hook — sets internal state without going through the network. */
    internal fun testSetState(state: FamiliarState) { _state.value = state }
}
