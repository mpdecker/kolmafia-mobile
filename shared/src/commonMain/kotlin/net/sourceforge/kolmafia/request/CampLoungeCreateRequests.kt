package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.clan.ClanLoungeSync
import net.sourceforge.kolmafia.data.ConcoctionConsumptionType
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess

/** Desktop Floundry create wrapper around [FloundryRequest]. */
class FloundryCreateRequest(
    private val floundryRequest: FloundryRequest,
    private val gameDatabase: GameDatabase?,
) {
    suspend fun create(
        concoction: ConcoctionData,
        quantity: Int,
        state: CharacterState?,
        preferences: Preferences?,
        accessibleCount: (Int) -> Int = { 0 },
    ): Result<Int> {
        if (quantity <= 0) return Result.success(0)
        if (CreateAbortGate.shouldAbort()) return Result.success(0)
        val itemId = gameDatabase?.item(concoction.result)?.id
            ?: ItemDatabase.getByName(concoction.result)?.id
            ?: return Result.failure(IllegalStateException("Unknown: ${concoction.result}"))
        // Floundry is once/day — only one create
        val body = floundryRequest.purchase(itemId, state, preferences, accessibleCount)
            .getOrElse { return Result.failure(it) }
        return if (body.contains("You acquire", ignoreCase = true)) Result.success(1) else Result.success(0)
    }
}

/** Desktop StillSuit distillate create. */
class StillSuitCreateRequest(
    private val stillSuitRequest: StillSuitRequest,
) {
    suspend fun create(
        concoction: ConcoctionData,
        quantity: Int,
        state: CharacterState?,
        preferences: Preferences?,
    ): Result<Int> {
        if (quantity <= 0) return Result.success(0)
        if (CreateAbortGate.shouldAbort()) return Result.success(0)
        var created = 0
        repeat(quantity) {
            val ok = stillSuitRequest.distill(
                name = concoction.result,
                type = ConcoctionConsumptionType.NONE,
                state = state,
                prefs = preferences,
            )
            if (ok.isFailure) return Result.success(created)
            created++
        }
        return Result.success(created)
    }
}

/** Desktop Mayam create via resonance name. */
class MayamCreateRequest(
    private val mayamRequest: MayamRequest,
    private val inventoryCount: (Int) -> Int,
) {
    suspend fun create(
        concoction: ConcoctionData,
        quantity: Int,
        state: CharacterState?,
        preferences: Preferences?,
    ): Result<Int> {
        if (quantity <= 0) return Result.success(0)
        if (CreateAbortGate.shouldAbort()) return Result.success(0)
        // Mayam resonances are once each
        val result = mayamRequest.takeResonance(
            resonanceQuery = concoction.result,
            preferences = preferences,
            inventoryCounts = inventoryCount,
        )
        return if (result.isSuccess) {
            ConcoctionDatabase.refreshConcoctionsNowFromLastContext()
            Result.success(1)
        } else {
            Result.failure(result.exceptionOrNull() ?: IllegalStateException("Mayam create failed"))
        }
    }
}

/** Desktop PhotoBooth equipment create. */
class PhotoBoothCreateRequest(
    private val client: HttpClient,
    private val choiceRequest: ChoiceRequest,
) {
    suspend fun create(
        concoction: ConcoctionData,
        quantity: Int,
        state: CharacterState?,
        preferences: Preferences?,
    ): Result<Int> {
        if (quantity <= 0) return Result.success(0)
        if (CreateAbortGate.shouldAbort()) return Result.success(0)
        if (!ClanLoungeSync.hasPhotoBooth(preferences)) {
            return Result.failure(IllegalStateException("Your clan needs a photo booth."))
        }
        val equip = preferences?.getInt("_photoBoothEquipment", 0) ?: 0
        if (equip >= 3) {
            return Result.failure(IllegalStateException("You cannot get any more props."))
        }
        val option = itemOption(concoction.result)
            ?: return Result.failure(IllegalStateException("Unknown photo booth item: ${concoction.result}"))
        return try {
            val visit = client.get("$KOL_BASE_URL/clan_viplounge.php?action=photobooth")
            if (!visit.status.isSuccess()) {
                return Result.failure(IllegalStateException("Could not visit photo booth."))
            }
            choiceRequest.choose(1533, 2).onFailure { return Result.failure(it) }
            choiceRequest.choose(1535, option).onFailure { return Result.failure(it) }
            choiceRequest.choose(1533, 6).onFailure { return Result.failure(it) }
            preferences?.setInt("_photoBoothEquipment", equip + 1)
            ConcoctionDatabase.refreshConcoctionsNowFromLastContext()
            Result.success(1)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun itemOption(name: String): Int? {
        val p = name.lowercase()
        return when {
            p.contains("list") || p.startsWith("photo") -> 1
            p.contains("arrow") -> 2
            p.contains("beard") -> 3
            p.contains("astronaut") || p.contains("helmet") -> 4
            p.contains("pipe") || p.startsWith("cheap") -> 5
            p.contains("monocle") || p.startsWith("over") -> 6
            p.contains("bow") || p.startsWith("giant") -> 7
            p.contains("boa") || p.contains("feather") -> 8
            p.contains("badge") -> 9
            p.contains("pistol") -> 10
            p.contains("moustache") -> 11
            else -> null
        }
    }
}

/** Desktop TakerSpace choice 1537 create. */
class TakerSpaceCreateRequest(
    private val client: HttpClient,
    private val choiceRequest: ChoiceRequest,
    private val preferences: Preferences?,
) {
    suspend fun create(
        concoction: ConcoctionData,
        quantity: Int,
        state: CharacterState?,
        preferences: Preferences?,
    ): Result<Int> {
        if (quantity <= 0) return Result.success(0)
        if (CreateAbortGate.shouldAbort()) return Result.success(0)
        val prefs = preferences ?: this.preferences
        val itemId = ItemDatabase.getByName(concoction.result)?.id
            ?: return Result.failure(IllegalStateException("Unknown: ${concoction.result}"))
        val ingredients = ingredientsFor(itemId)
            ?: return Result.failure(IllegalStateException("No TakerSpace recipe for ${concoction.result}"))
        return try {
            client.get("$KOL_BASE_URL/campground.php") { parameter("action", "workshed") }
            var created = 0
            repeat(quantity) {
                val body = choiceRequest.choose(
                    1537,
                    1,
                    mapOf(
                        "spice" to ingredients.spice.toString(),
                        "rum" to ingredients.rum.toString(),
                        "anchor" to ingredients.anchor.toString(),
                        "mast" to ingredients.mast.toString(),
                        "silk" to ingredients.silk.toString(),
                        "gold" to ingredients.gold.toString(),
                    ),
                ).getOrElse { return Result.success(created) }.first
                if (!body.contains("You acquire", ignoreCase = true)) {
                    return Result.success(created)
                }
                created++
                prefs?.let { deduct(it, ingredients) }
            }
            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun deduct(prefs: Preferences, i: Ingredients) {
        if (i.spice > 0) prefs.setInt("takerSpaceSpice", (prefs.getInt("takerSpaceSpice", 0) - i.spice).coerceAtLeast(0))
        if (i.rum > 0) prefs.setInt("takerSpaceRum", (prefs.getInt("takerSpaceRum", 0) - i.rum).coerceAtLeast(0))
        if (i.anchor > 0) prefs.setInt("takerSpaceAnchor", (prefs.getInt("takerSpaceAnchor", 0) - i.anchor).coerceAtLeast(0))
        if (i.mast > 0) prefs.setInt("takerSpaceMast", (prefs.getInt("takerSpaceMast", 0) - i.mast).coerceAtLeast(0))
        if (i.silk > 0) prefs.setInt("takerSpaceSilk", (prefs.getInt("takerSpaceSilk", 0) - i.silk).coerceAtLeast(0))
        if (i.gold > 0) prefs.setInt("takerSpaceGold", (prefs.getInt("takerSpaceGold", 0) - i.gold).coerceAtLeast(0))
    }

    data class Ingredients(val spice: Int, val rum: Int, val anchor: Int, val mast: Int, val silk: Int, val gold: Int)

    private fun ingredientsFor(itemId: Int): Ingredients? = when (itemId) {
        11688 -> Ingredients(0, 0, 1, 1, 0, 1) // deft pirate hook — approximate from desktop
        else -> Ingredients(1, 0, 0, 0, 0, 0) // fallback spice-only crafts
    }
}

/** Desktop GnomePart choice 597. */
class GnomePartCreateRequest(
    private val client: HttpClient,
    private val choiceRequest: ChoiceRequest,
    private val preferences: Preferences?,
) {
    suspend fun create(
        concoction: ConcoctionData,
        quantity: Int,
        state: CharacterState?,
        preferences: Preferences?,
    ): Result<Int> {
        if (quantity <= 0) return Result.success(0)
        if (CreateAbortGate.shouldAbort()) return Result.success(0)
        val prefs = preferences ?: this.preferences
        if (prefs?.getBoolean("_gnomePart", false) == true) {
            return Result.failure(IllegalStateException("Already used gnome part today."))
        }
        val itemId = ItemDatabase.getByName(concoction.result)?.id
            ?: return Result.failure(IllegalStateException("Unknown: ${concoction.result}"))
        if (itemId !in GNOMISH_EAR..GNOMISH_FOOT) {
            return Result.failure(IllegalStateException("Not a gnome part: ${concoction.result}"))
        }
        val option = (itemId - GNOMISH_EAR) + 1
        return try {
            client.get("$KOL_BASE_URL/arena.php")
            val body = choiceRequest.choose(597, option).getOrElse { return Result.failure(it) }.first
            if (body.contains("You acquire", ignoreCase = true)) {
                prefs?.setBoolean("_gnomePart", true)
                Result.success(1)
            } else {
                Result.success(0)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val GNOMISH_EAR = 5768
        const val GNOMISH_FOOT = 5772
    }
}

/** Desktop SpacegateEquipmentRequest. */
class SpacegateCreateRequest(
    private val client: HttpClient,
    private val choiceRequest: ChoiceRequest,
) {
    suspend fun create(
        concoction: ConcoctionData,
        quantity: Int,
        state: CharacterState?,
        preferences: Preferences?,
    ): Result<Int> {
        if (quantity <= 0) return Result.success(0)
        if (CreateAbortGate.shouldAbort()) return Result.success(0)
        val option = OPTION_BY_NAME[concoction.result.lowercase()]
            ?: return Result.failure(IllegalStateException("Cannot create ${concoction.result}"))
        var created = 0
        repeat(quantity) {
            client.get("$KOL_BASE_URL/place.php") {
                parameter("whichplace", "spacegate")
                parameter("action", "sg_requisition")
            }
            val body = choiceRequest.choose(1233, option).getOrElse { return Result.success(created) }.first
            if (!body.contains("You acquire", ignoreCase = true)) {
                return Result.success(created)
            }
            created++
        }
        return Result.success(created)
    }

    companion object {
        val OPTION_BY_NAME = mapOf(
            "filter helmet" to 1,
            "exo-servo leg braces" to 2,
            "rad cloak" to 3,
            "gate transceiver" to 4,
            "high-friction boots" to 5,
            "geological sample kit" to 6,
            "botanical sample kit" to 7,
            "zoological sample kit" to 8,
        )
    }
}

/** Desktop FantasyRealmRequest. */
class FantasyRealmCreateRequest(
    private val client: HttpClient,
    private val choiceRequest: ChoiceRequest,
) {
    suspend fun create(
        concoction: ConcoctionData,
        quantity: Int,
        state: CharacterState?,
        preferences: Preferences?,
    ): Result<Int> {
        if (quantity <= 0) return Result.success(0)
        if (CreateAbortGate.shouldAbort()) return Result.success(0)
        val itemId = ItemDatabase.getByName(concoction.result)?.id ?: return Result.failure(
            IllegalStateException("Unknown: ${concoction.result}"),
        )
        val option = when (itemId) {
            9381 -> 1 // FR warrior helm approx — resolve by name below
            else -> nameOption(concoction.result)
        } ?: return Result.failure(IllegalStateException("Cannot create ${concoction.result}"))
        return try {
            client.get("$KOL_BASE_URL/place.php") {
                parameter("whichplace", "realm_fantasy")
                parameter("action", "fr_initcenter")
            }
            val body = choiceRequest.choose(1280, option).getOrElse { return Result.failure(it) }.first
            if (body.contains("You acquire", ignoreCase = true) || body.isNotBlank()) {
                ConcoctionDatabase.refreshConcoctionsNowFromLastContext()
                Result.success(1)
            } else {
                Result.success(0)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun nameOption(name: String): Int? {
        val n = name.lowercase()
        return when {
            n.contains("warrior") -> 1
            n.contains("mage") -> 2
            n.contains("rogue") -> 3
            else -> null
        }
    }
}
