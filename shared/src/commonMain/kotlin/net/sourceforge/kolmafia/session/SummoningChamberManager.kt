package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.familiar.FamiliarIds
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.familiar.FamiliarRequest
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.SummoningChamberRequest

/** Orchestrates desktop-style `summon` CLI / Summoning Chamber HTTP. */
open class SummoningChamberManager(
    private val preferences: Preferences,
    private val request: SummoningChamberRequest,
    private val retrieveItemService: RetrieveItemService?,
    private val inventoryManager: InventoryManager?,
    private val familiarRequest: FamiliarRequest?,
    private val familiarManager: FamiliarManager?,
) {

    data class ResolvedDemon(val name: String, val number: Int)

    suspend fun summon(parameters: String, print: (String) -> Unit): Result<Unit> {
        val trimmed = parameters.trim()
        if (trimmed.isEmpty()) {
            return Result.success(Unit)
        }
        if (preferences.getBoolean(Preferences.DEMON_SUMMONED, false)) {
            print("You've already summoned a demon today.")
            return Result.failure(IllegalStateException("already summoned"))
        }

        val resolved = resolveDemon(trimmed) ?: run {
            print("You don't know the name of that demon.")
            return Result.failure(IllegalArgumentException("unknown demon"))
        }

        if (resolved.name.isEmpty()) {
            print("You don't know the name of that demon.")
            return Result.failure(IllegalArgumentException("empty demon name"))
        }

        if (resolved.number == 12 && !resolved.name.startsWith("Neil")) {
            print("You don't know the full name of that demon.")
            return Result.failure(IllegalArgumentException("incomplete intergnat name"))
        }

        val retrieve = retrieveItemService
        if (retrieve == null ||
            retrieve.retrieve(DemonTypes.BLACK_CANDLE, 3) < 3 ||
            retrieve.retrieve(DemonTypes.EVIL_SCROLL, 1) < 1
        ) {
            print("You don't have the items required to summon a demon.")
            return Result.failure(IllegalStateException("missing items"))
        }

        print("Summoning ${resolved.name}...")

        val previousFamiliarId = if (resolved.number == 12) {
            familiarManager?.state?.value?.activeFamiliar?.id
        } else {
            null
        }

        if (resolved.number == 12) {
            familiarRequest?.switchFamiliar(FamiliarIds.INTERGNAT)
        }

        val result = request.summon(resolved.name)

        if (resolved.number == 12 && previousFamiliarId != null && previousFamiliarId != FamiliarIds.INTERGNAT) {
            familiarRequest?.switchFamiliar(previousFamiliarId)
        }

        return result.fold(
            onSuccess = { summonResult ->
                val parsed = SummoningChamberRequest.parseResponse(
                    summonResult.location,
                    summonResult.responseText,
                    preferences,
                )
                if (parsed.consumeSummoningItems) {
                    inventoryManager?.consumeItemLocally(DemonTypes.BLACK_CANDLE, 3)
                    inventoryManager?.consumeItemLocally(DemonTypes.EVIL_SCROLL, 1)
                }
                parsed.brownWord?.let { word ->
                    print("Infernal Thirst demon Brown Word found: $word")
                }
                Result.success(Unit)
            },
            onFailure = { Result.failure(it) },
        )
    }

    fun resolveDemon(parameters: String): ResolvedDemon? {
        var demon = parameters
        var demonNumber = -1

        if (parameters.first().isDigit()) {
            demonNumber = parameters.toIntOrNull() ?: return null
            if (demonNumber !in 1..DemonTypes.DEMON_COUNT) return null
            demon = preferences.getString(DemonTypes.demonNameKey(demonNumber), "")
            return ResolvedDemon(demon, demonNumber)
        }

        for (i in DemonTypes.ENTRIES.indices) {
            val (location, effect) = DemonTypes.ENTRIES[i]
            val number = i + 1
            if (location != null && parameters.equals(location, ignoreCase = true)) {
                return ResolvedDemon(
                    preferences.getString(DemonTypes.demonNameKey(number), ""),
                    number,
                )
            }
            if (parameters.equals(effect, ignoreCase = true)) {
                return ResolvedDemon(
                    preferences.getString(DemonTypes.demonNameKey(number), ""),
                    number,
                )
            }
            val storedName = preferences.getString(DemonTypes.demonNameKey(number), "")
            if (storedName.isNotEmpty() && parameters.equals(storedName, ignoreCase = true)) {
                return ResolvedDemon(storedName, number)
            }
        }

        return null
    }
}
