package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.equipment.Modeable
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop MaximizerSpeculation.tryAll per-familiar loop (Phase 373).
 * Re-runs tryOutfits + speculate for the base familiar and each switch candidate.
 */
object MaximizerFamiliarSpeculation {

    data class TryAllResult(
        val bestPerSlot: Map<EquipmentSlot, Pair<String, Double>>,
        val familiarSwitch: String?,
        val familiarBonus: Double,
        val enthronedRace: String? = null,
        val bjornifiedRace: String? = null,
    )

    fun tryAll(
        spec: MaximizeSpec,
        charState: CharacterState,
        survivingOutfits: List<net.sourceforge.kolmafia.data.OutfitData>,
        rankedBuckets: SlotList<MaximizerRankedItem>,
        refineBestPerSlot: Map<EquipmentSlot, Pair<String, Double>>,
        comboBudget: ComboBudget,
        thrallBonus: Double,
        gameDatabase: GameDatabase,
        usableSwitchFamiliars: List<String>,
        usableEnthroneFamiliars: List<String> = emptyList(),
        usableBjornFamiliars: List<String> = emptyList(),
        buildCandidates: (familiarRace: String?, isSwitchPass: Boolean) -> Map<EquipmentSlot, List<Pair<String, Double>>>,
        scoreFamiliar: (String?) -> Double,
        priceFor: ((String) -> Int)? = null,
        scoring: MaximizerScoringOptions = MaximizerScoringOptions(),
        preferences: Preferences? = null,
    ): TryAllResult {
        val preferLowerPrice = spec.maxPrice != null && priceFor != null

        if (spec.switchFamiliars.isEmpty()) {
            return runPass(
                spec = spec,
                charState = charState,
                survivingOutfits = survivingOutfits,
                rankedBuckets = rankedBuckets,
                refineBestPerSlot = refineBestPerSlot,
                comboBudget = comboBudget,
                thrallBonus = thrallBonus,
                gameDatabase = gameDatabase,
                familiarRace = null,
                isSwitchPass = false,
                buildCandidates = buildCandidates,
                scoreFamiliar = scoreFamiliar,
                priceFor = priceFor,
                preferLowerPrice = preferLowerPrice,
                usableEnthroneFamiliars = usableEnthroneFamiliars,
                usableBjornFamiliars = usableBjornFamiliars,
                scoring = scoring,
                preferences = preferences,
            )
        }

        val currentFamiliar = charState.familiarName.takeIf { it.isNotBlank() }
        val passes = buildList {
            add(currentFamiliar to false)
            val seen = currentFamiliar?.lowercase()?.let { setOf(it) } ?: emptySet()
            for (race in usableSwitchFamiliars) {
                if (race.lowercase() in seen) continue
                add(race to true)
            }
        }

        var bestResult = runPass(
            spec = spec,
            charState = charState,
            survivingOutfits = survivingOutfits,
            rankedBuckets = rankedBuckets,
            refineBestPerSlot = refineBestPerSlot,
            comboBudget = comboBudget,
            thrallBonus = thrallBonus,
            gameDatabase = gameDatabase,
            familiarRace = passes.first().first,
            isSwitchPass = false,
            buildCandidates = buildCandidates,
            scoreFamiliar = scoreFamiliar,
            priceFor = priceFor,
            preferLowerPrice = preferLowerPrice,
            usableEnthroneFamiliars = usableEnthroneFamiliars,
            usableBjornFamiliars = usableBjornFamiliars,
            scoring = scoring,
            preferences = preferences,
        )
        var bestScore = scoreResult(
            charState, spec, bestResult, thrallBonus, scoreFamiliar, scoring, preferences,
        )
        var bestFailed = spec.evaluator.failed
        var bestTie = tieResult(charState, bestResult, spec, scoring, preferences)
        var bestPrice = priceFor?.let { MaximizerSpeculation.assignmentPrice(bestResult.bestPerSlot, it) } ?: Int.MAX_VALUE

        for ((familiarRace, isSwitchPass) in passes.drop(1)) {
            if (comboBudget.exhausted()) break
            val passResult = runPass(
                spec = spec,
                charState = charState,
                survivingOutfits = survivingOutfits,
                rankedBuckets = rankedBuckets,
                refineBestPerSlot = refineBestPerSlot,
                comboBudget = comboBudget,
                thrallBonus = thrallBonus,
                gameDatabase = gameDatabase,
                familiarRace = familiarRace,
                isSwitchPass = isSwitchPass,
                buildCandidates = buildCandidates,
                scoreFamiliar = scoreFamiliar,
                priceFor = priceFor,
                preferLowerPrice = preferLowerPrice,
                usableEnthroneFamiliars = usableEnthroneFamiliars,
                usableBjornFamiliars = usableBjornFamiliars,
                scoring = scoring,
                preferences = preferences,
            )
            val score = scoreResult(
                charState, spec, passResult, thrallBonus, scoreFamiliar, scoring, preferences,
            )
            val failed = spec.evaluator.failed
            if (failed) continue
            val tie = tieResult(charState, passResult, spec, scoring, preferences)
            val price = priceFor?.let { MaximizerSpeculation.assignmentPrice(passResult.bestPerSlot, it) } ?: Int.MAX_VALUE
            if (MaximizerSpeculation.isBetterLoadout(
                    score, tie, price, failed,
                    bestScore, bestTie, bestPrice, bestFailed, preferLowerPrice,
                )
            ) {
                bestResult = passResult
                bestScore = score
                bestFailed = false
                bestTie = tie
                bestPrice = price
            }
        }

        return bestResult
    }

    private fun scoreResult(
        charState: CharacterState,
        spec: MaximizeSpec,
        result: TryAllResult,
        thrallBonus: Double,
        scoreFamiliar: (String?) -> Double,
        scoring: MaximizerScoringOptions,
        preferences: Preferences? = null,
    ): Double {
        val bonus = totalFamiliarBonus(result, scoreFamiliar)
        val card = MaximizerCardSelection.cardForOffhand(
            result.bestPerSlot[EquipmentSlot.OFFHAND]?.first, scoring.cardInSleeve, charState,
        )
        return MaximizerSpeculation.scoreLoadout(
            charState, result.bestPerSlot, spec.evaluator, bonus, thrallBonus,
            bestModes = scoring.bestModes,
            carryFamiliars = scoring.carryFamiliars,
            gameDatabase = scoring.gameDatabase,
            cardInSleeve = card,
            preferences = preferences,
        )
    }

    private fun tieResult(
        charState: CharacterState,
        result: TryAllResult,
        spec: MaximizeSpec,
        scoring: MaximizerScoringOptions,
        preferences: Preferences?,
    ): Double {
        val card = MaximizerCardSelection.cardForOffhand(
            result.bestPerSlot[EquipmentSlot.OFFHAND]?.first, scoring.cardInSleeve, charState,
        )
        return MaximizerSpeculation.tiebreakerScore(
            charState, result.bestPerSlot, spec.evaluator,
            bestModes = scoring.bestModes,
            carryFamiliars = scoring.carryFamiliars,
            gameDatabase = scoring.gameDatabase,
            cardInSleeve = card,
            preferences = preferences,
        )
    }

    internal fun totalFamiliarBonus(
        result: TryAllResult,
        scoreFamiliar: (String?) -> Double,
    ): Double {
        var bonus = result.familiarBonus
        result.enthronedRace?.let { bonus += scoreFamiliar(it) }
        result.bjornifiedRace?.let { bonus += scoreFamiliar(it) }
        return bonus
    }

    private fun runPass(
        spec: MaximizeSpec,
        charState: CharacterState,
        survivingOutfits: List<net.sourceforge.kolmafia.data.OutfitData>,
        rankedBuckets: SlotList<MaximizerRankedItem>,
        refineBestPerSlot: Map<EquipmentSlot, Pair<String, Double>>,
        comboBudget: ComboBudget,
        thrallBonus: Double,
        gameDatabase: GameDatabase,
        familiarRace: String?,
        isSwitchPass: Boolean,
        buildCandidates: (familiarRace: String?, isSwitchPass: Boolean) -> Map<EquipmentSlot, List<Pair<String, Double>>>,
        scoreFamiliar: (String?) -> Double,
        priceFor: ((String) -> Int)?,
        preferLowerPrice: Boolean,
        usableEnthroneFamiliars: List<String>,
        usableBjornFamiliars: List<String>,
        scoring: MaximizerScoringOptions,
        preferences: Preferences? = null,
    ): TryAllResult {
        val familiarBonus = scoreFamiliar(familiarRace)
        val candidatesBySlot = buildCandidates(familiarRace, isSwitchPass)
        var bestPerSlot = MaximizerOutfitSpeculation.tryOutfits(
            spec = spec,
            baseState = charState,
            survivingOutfits = survivingOutfits,
            rankedBuckets = rankedBuckets,
            candidatesBySlot = candidatesBySlot,
            budget = comboBudget,
            currentBest = refineBestPerSlot,
            gameDatabase = gameDatabase,
            familiarBonus = familiarBonus,
            thrallBonus = thrallBonus,
            priceFor = priceFor,
            scoring = scoring,
            preferences = preferences,
        )
        bestPerSlot = MaximizerFamiliarItemSpeculation.tryFamiliarItems(
            spec = spec,
            baseState = charState,
            candidatesBySlot = candidatesBySlot,
            rankedBuckets = rankedBuckets,
            budget = comboBudget,
            familiarBonus = familiarBonus,
            thrallBonus = thrallBonus,
            currentBest = bestPerSlot,
            gameDatabase = gameDatabase,
            priceFor = priceFor,
            scoring = scoring,
            preferences = preferences,
        )
        val containerResult = MaximizerContainerSpeculation.tryContainers(
            spec = spec,
            baseState = charState,
            candidatesBySlot = candidatesBySlot,
            rankedBuckets = rankedBuckets,
            budget = comboBudget,
            familiarBonus = familiarBonus,
            thrallBonus = thrallBonus,
            currentBest = bestPerSlot,
            gameDatabase = gameDatabase,
            priceFor = priceFor,
            usableBjornFamiliars = usableBjornFamiliars,
            scoreFamiliar = scoreFamiliar,
            scoring = scoring,
            preferences = preferences,
        )
        bestPerSlot = containerResult.bestPerSlot
        val hatResult = MaximizerHatSpeculation.tryHats(
            spec = spec,
            baseState = charState,
            candidatesBySlot = candidatesBySlot,
            rankedBuckets = rankedBuckets,
            budget = comboBudget,
            familiarBonus = familiarBonus,
            thrallBonus = thrallBonus,
            currentBest = bestPerSlot,
            gameDatabase = gameDatabase,
            priceFor = priceFor,
            usableEnthroneFamiliars = usableEnthroneFamiliars,
            activeBjornRace = containerResult.bjornifiedRace,
            scoreFamiliar = scoreFamiliar,
            scoring = scoring,
            preferences = preferences,
        )
        bestPerSlot = hatResult.bestPerSlot
        val speculated = MaximizerSpeculation.speculate(
            spec,
            charState,
            candidatesBySlot,
            comboBudget,
            familiarBonus,
            thrallBonus,
            bestPerSlot,
            priceFor,
            bestModes = scoring.bestModes,
            carryFamiliars = scoring.carryFamiliars,
            gameDatabase = gameDatabase,
            cardInSleeve = scoring.cardInSleeve,
            foldablesEnabled = scoring.foldablesEnabled,
            countFor = scoring.countFor,
            preferences = preferences,
        )
        if (speculated.isNotEmpty()) {
            bestPerSlot = speculated
        }
        val finalEnthronedRace = hatResult.enthronedRace?.takeIf {
            bestPerSlot[EquipmentSlot.HAT]?.first.equals(MaximizerManager.CROWN_OF_THRONES, ignoreCase = true)
        }
        val finalBjornifiedRace = containerResult.bjornifiedRace?.takeIf {
            bestPerSlot[EquipmentSlot.CONTAINER]?.first.equals(MaximizerManager.BUDDY_BJORN, ignoreCase = true)
        }
        return TryAllResult(
            bestPerSlot = bestPerSlot,
            familiarSwitch = familiarRace?.takeIf { isSwitchPass },
            familiarBonus = familiarBonus,
            enthronedRace = finalEnthronedRace,
            bjornifiedRace = finalBjornifiedRace,
        )
    }
}
