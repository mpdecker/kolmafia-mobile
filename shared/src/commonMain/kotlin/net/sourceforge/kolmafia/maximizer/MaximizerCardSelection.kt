package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.equipment.Modeable
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop Evaluator card-sleeve best-card pre-selection (~1667–1690).
 */
object MaximizerCardSelection {

    const val CARD_SLEEVE_ID = 5009
    const val CARD_SLEEVE_NAME = "card sleeve"
    val CARD_ID_RANGE = 4967..5007

    fun cardNeeded(rankedBuckets: SlotList<MaximizerRankedItem>): Boolean =
        MaximizerEquipmentEnumerator.allRankedItems(rankedBuckets)
            .any { it.itemId == CARD_SLEEVE_ID }

    fun selectBestCard(
        spec: MaximizeSpec,
        charState: CharacterState,
        rankedBuckets: SlotList<MaximizerRankedItem>,
        countFor: (Int) -> Int,
        modeOverrides: Map<Modeable, String> = emptyMap(),
        preferences: Preferences? = null,
        familiarBonus: Double = 0.0,
        thrallBonus: Double = 0.0,
        carryFamiliars: List<String> = emptyList(),
        gameDatabase: net.sourceforge.kolmafia.data.GameDatabase? = null,
    ): String? {
        if (!cardNeeded(rankedBuckets)) return null

        val equippedCard = charState.equipment[EquipmentSlot.CARDSLEEVE]
        var bestCard: String? = null
        var bestScore = Double.NEGATIVE_INFINITY

        for (cardId in CARD_ID_RANGE) {
            val count = countFor(cardId)
            val cardName = gameDatabase?.item(cardId)?.name
                ?: continue
            if (count <= 0 && !cardName.equals(equippedCard, ignoreCase = true)) continue

            val assignment = mapOf(
                EquipmentSlot.OFFHAND to (CARD_SLEEVE_NAME to 0.0),
            )
            val score = MaximizerSpeculation.scoreLoadout(
                baseState = charState,
                assignment = assignment,
                evaluator = spec.evaluator,
                familiarBonus = familiarBonus,
                thrallBonus = thrallBonus,
                modeOverrides = MaximizerModeSelection.assignmentModeOverrides(
                    assignment, modeOverrides, carryFamiliars, gameDatabase,
                ),
                preferences = preferences,
                cardInSleeve = cardName,
            )
            if (spec.evaluator.failed) continue
            if (score > bestScore) {
                bestScore = score
                bestCard = cardName
            }
        }
        return bestCard ?: equippedCard?.takeIf { it.isNotBlank() }
    }

    fun cardForOffhand(
        offhandName: String?,
        bestCard: String?,
        charState: CharacterState,
    ): String? {
        if (!offhandName.equals(CARD_SLEEVE_NAME, ignoreCase = true)) return null
        return bestCard ?: charState.equipment[EquipmentSlot.CARDSLEEVE]?.takeIf { it.isNotBlank() }
    }
}
