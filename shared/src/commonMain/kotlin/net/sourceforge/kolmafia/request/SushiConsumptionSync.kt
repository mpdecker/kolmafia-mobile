package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConsumableDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.event.GameEvent
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [net.sourceforge.kolmafia.request.concoction.SushiRequest] post-create consumption sync. */
object SushiConsumptionSync {
    private const val SUSHI_DOILY = 6328
    private const val MERKIN_WORKTEA = 6356
    private const val WORKTEA_CLUE_PREF = "workteaClue"

    private val WORKTEA_PATTERN =
        Regex("""the leaves in the bottom look just like <b>([^<]*)</b>""")

    fun parseConsumption(
        formFields: Map<String, String>,
        responseText: String,
        updateFullness: Boolean,
        character: KoLCharacter? = null,
        eventBus: GameEventBus? = null,
        preferences: Preferences? = null,
    ) {
        if (responseText.contains("too full to eat it", ignoreCase = true)) {
            return
        }

        val name = SushiChoiceMapper.resultNameFromFormFields(formFields) ?: return
        val concoction = ConcoctionDatabase.getByResult(name) ?: return

        for (ingredient in concoction.ingredients) {
            val itemId = ItemDatabase.getByName(ingredient.name)?.id ?: continue
            emitConsumed(eventBus, itemId, ingredient.quantity)
        }

        if (updateFullness) {
            val fullness = ConsumableDatabase.getFullnessByName(name)
            val charState = character?.state?.value
            if (fullness > 0 &&
                charState != null &&
                !responseText.contains("Fullness", ignoreCase = true)
            ) {
                character.updateConsumables(
                    fullness = charState.fullness + fullness,
                    inebriety = charState.inebriety,
                    spleenUsed = charState.spleenUsed,
                )
            }
        }

        if (responseText.contains("fancy doily", ignoreCase = true)) {
            emitConsumed(eventBus, SUSHI_DOILY, 1)
        }

        handleWorktea(responseText, preferences, eventBus)
    }

    fun registerRequest(
        formFields: Map<String, String>,
        sessionLogger: SessionLogger?,
    ) {
        val name = SushiChoiceMapper.resultNameFromFormFields(formFields) ?: return
        val concoction = ConcoctionDatabase.getByResult(name) ?: return

        val verb = if (name.contains("bento", ignoreCase = true)) "Pack" else "Roll"
        val ingredients = concoction.ingredients.joinToString(", ") { ingredient ->
            "${ingredient.quantity} ${ingredient.name}"
        }
        sessionLogger?.appendRawLine("$verb and eat $name from $ingredients")
    }

    fun handleWorktea(
        responseText: String,
        preferences: Preferences?,
        eventBus: GameEventBus?,
    ) {
        val clue = WORKTEA_PATTERN.find(responseText)?.groupValues?.get(1) ?: return
        preferences?.setString(WORKTEA_CLUE_PREF, clue)
        eventBus?.tryEmit(GameEvent.ItemConsumed(MERKIN_WORKTEA, 1))
    }

    private fun emitConsumed(eventBus: GameEventBus?, itemId: Int, quantity: Int) {
        if (quantity <= 0) return
        eventBus?.tryEmit(GameEvent.ItemConsumed(itemId, quantity))
    }
}
