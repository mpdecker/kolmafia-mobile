package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionPermitted
import net.sourceforge.kolmafia.data.terminalExtrudeCommand
import net.sourceforge.kolmafia.item.CreateItemIngredients
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [TerminalExtrudeRequest] — Source Terminal extrude create for TERMINAL concoctions. */
class TerminalExtrudeCreateRequest(
    private val terminalRequest: TerminalRequest,
    private val createItemIngredients: CreateItemIngredients,
) {
    suspend fun create(
        concoction: ConcoctionData,
        quantity: Int,
        state: CharacterState?,
        preferences: Preferences?,
        accessibleCount: (Int) -> Int = { 0 },
    ): Result<Int> {
        if (quantity <= 0) return Result.success(0)
        val command = concoction.terminalExtrudeCommand()
            ?: return Result.failure(IllegalStateException("Missing terminal extrude command for: ${concoction.result}"))
        if (state != null &&
            !ConcoctionPermitted.isPermittedMethod(
                concoction,
                state,
                prefs = preferences,
                limitMode = state.limitMode,
                accessibleCount = accessibleCount,
            )
        ) {
            return Result.failure(IllegalStateException("Terminal craft not permitted: ${concoction.result}"))
        }

        var created = 0
        repeat(quantity) {
            if (!createItemIngredients.makeIngredients(concoction, 1, state)) {
                return if (created > 0) {
                    Result.failure(
                        IllegalStateException(
                            "Could not create $quantity of ${concoction.result} (got $created)",
                        ),
                    )
                } else {
                    Result.failure(
                        IllegalStateException(
                            "Could not retrieve ingredients for ${concoction.result}",
                        ),
                    )
                }
            }
            val response = terminalRequest.extrude(command, state, preferences, accessibleCount)
            response.exceptionOrNull()?.let { return Result.failure(it) }
            val body = response.getOrThrow()
            if (!body.contains("You acquire")) {
                return Result.failure(IllegalStateException("Terminal extrude was unsuccessful."))
            }
            recordTerminalExtrude(preferences)
            created++
        }
        return Result.success(created)
    }

    private fun recordTerminalExtrude(preferences: Preferences?) {
        val prefs = preferences ?: return
        prefs.setInt(
            "_sourceTerminalExtrudes",
            prefs.getInt("_sourceTerminalExtrudes", 0) + 1,
        )
    }
}
