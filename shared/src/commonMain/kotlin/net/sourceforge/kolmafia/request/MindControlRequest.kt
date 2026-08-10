package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.character.ZodiacSign
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.DesertBeachAccessibility
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parameters

/** Desktop MindControlRequest — set MCD dial via Canadia/Knoll/Gnomads. */
open class MindControlRequest(
    private val client: HttpClient,
    private val character: KoLCharacter? = null,
    private val preferences: Preferences? = null,
    private val retrieveItemService: RetrieveItemService? = null,
) {
    companion object {
        const val DETUNED_RADIO_ID = 2682
    }

    open suspend fun setLevel(level: Int): Result<Unit> {
        val state = character?.state?.value ?: CharacterState()
        val maxLevel = maxLevel(state)
        if (level !in 0..maxLevel) {
            return Result.failure(
                IllegalArgumentException("The dial only goes from 0 to $maxLevel."),
            )
        }
        if (level == state.mindControlLevel) {
            return Result.failure(IllegalStateException("Mind control device already at $level"))
        }
        if (!mcdAvailable(state)) {
            return Result.failure(IllegalStateException("Mind control device not available"))
        }
        return try {
            when {
                canadiaAvailable(state) -> setCanadia(level)
                knollAvailable(state) && !state.inGLover -> setKnoll(level)
                gnomadsAvailable(state) &&
                    DesertBeachAccessibility.isAvailable(state, preferences) -> setGnomads(level)
                else -> Result.failure(IllegalStateException("Mind control device not available"))
            }.onSuccess {
                character?.setMindControlLevel(level)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun setCanadia(level: Int): Result<Unit> {
        client.get("$KOL_BASE_URL/place.php?whichplace=canadia&action=lc_mcd").bodyAsText()
        val body = client.submitForm(
            url = "$KOL_BASE_URL/choice.php",
            formParameters = parameters {
                append("whichchoice", "769")
                append("option", "1")
                append("setting", level.toString())
            },
        ).bodyAsText()
        return if (body.contains("switch the dial", ignoreCase = true) ||
            body.contains("the radio", ignoreCase = true)
        ) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Failed to set mind control device"))
        }
    }

    private suspend fun setGnomads(level: Int): Result<Unit> {
        val body = client.submitForm(
            url = "$KOL_BASE_URL/gnomes.php",
            formParameters = parameters {
                append("action", "changedial")
                append("whichlevel", level.toString())
            },
        ).bodyAsText()
        return if (body.contains("switch the dial", ignoreCase = true)) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Failed to set mind control device"))
        }
    }

    private suspend fun setKnoll(level: Int): Result<Unit> {
        if ((retrieveItemService?.retrieve(DETUNED_RADIO_ID, 1) ?: 0) < 1) {
            return Result.failure(IllegalStateException("Need a detuned radio"))
        }
        val body = client.submitForm(
            url = "$KOL_BASE_URL/inv_use.php",
            formParameters = parameters {
                append("whichitem", DETUNED_RADIO_ID.toString())
                append("tuneradio", level.toString())
                append("ajax", "1")
            },
        ).bodyAsText()
        return if (body.contains("switch the dial", ignoreCase = true) ||
            body.contains("the radio", ignoreCase = true)
        ) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Failed to set mind control device"))
        }
    }

    private fun maxLevel(state: CharacterState): Int =
        if (canadiaAvailable(state)) 11 else 10

    private fun mcdAvailable(state: CharacterState): Boolean {
        if (canadiaAvailable(state)) return true
        if (knollAvailable(state) && !state.inGLover) return true
        if (gnomadsAvailable(state) &&
            DesertBeachAccessibility.isAvailable(state, preferences)
        ) {
            return true
        }
        return false
    }

    private fun canadiaAvailable(state: CharacterState): Boolean {
        if (state.isKingdomOfExploathing) return false
        val sign = ZodiacSign.find(state.zodiacSign) ?: return false
        return sign == ZodiacSign.BLENDER ||
            sign == ZodiacSign.PACKRAT ||
            sign == ZodiacSign.VOLE
    }

    private fun knollAvailable(state: CharacterState): Boolean =
        state.knollAvailable && !state.isKingdomOfExploathing

    private fun gnomadsAvailable(state: CharacterState): Boolean {
        val sign = ZodiacSign.find(state.zodiacSign) ?: return false
        return sign == ZodiacSign.WOMBAT ||
            sign == ZodiacSign.BLENDER ||
            sign == ZodiacSign.PACKRAT
    }
}
