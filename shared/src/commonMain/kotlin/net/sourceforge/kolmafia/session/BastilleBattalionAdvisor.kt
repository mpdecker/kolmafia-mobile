package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.data.BastilleDatabase.Castle
import net.sourceforge.kolmafia.data.BastilleDatabase.Stat
import net.sourceforge.kolmafia.data.BastilleDatabase.Stats
import net.sourceforge.kolmafia.preferences.Preferences

/** Deterministic, headless Bastille battle simulator and choice advisor. */
object BastilleBattleSimulation {
    private val attack = listOf(Stat.MA, Stat.CA, Stat.PA)
    private val defense = listOf(Stat.MD, Stat.CD, Stat.PD)

    fun winProbability(
        stats: Stats,
        boosts: BastilleBoosts,
        enemy: Castle,
        battleNumber: Int,
        stance: BastilleStance,
    ): Double {
        val attackChance = seriesChance(attack.map { axisChance(stats.get(it) + boosts.boostedBy(it), enemyValue(enemy, it, battleNumber)) })
        val defenseChance = seriesChance(defense.map { axisChance(stats.get(it) + boosts.boostedBy(it), enemyValue(enemy, it, battleNumber)) })
        val aggressorChance = when (stance) {
            BastilleStance.OFFENSE -> 0.8
            BastilleStance.BIDE -> 0.5
            BastilleStance.DEFENSE -> 0.2
        }
        return attackChance * aggressorChance + defenseChance * (1.0 - aggressorChance)
    }

    fun bestStance(stats: Stats, boosts: BastilleBoosts, enemy: Castle, battleNumber: Int): BastilleStance =
        BastilleStance.entries.maxBy { winProbability(stats, boosts, enemy, battleNumber, it) }

    fun probabilities(stats: Stats, boosts: BastilleBoosts, enemy: Castle, battleNumber: Int): Map<BastilleStance, Double> =
        BastilleStance.entries.associateWith { winProbability(stats, boosts, enemy, battleNumber, it) }

    private fun enemyValue(enemy: Castle, stat: Stat, battleNumber: Int): Int =
        (battleNumber.coerceIn(1, 5) * 2) + if (enemy.superiorStat == stat) 3 else 0

    // Equal stats are a 50/50 comparison; each visible bonus moves the estimate by 10%.
    private fun axisChance(player: Int, enemy: Int): Double =
        (0.5 + (player - enemy) * 0.1).coerceIn(0.05, 0.95)

    private fun seriesChance(p: List<Double>): Double {
        val triple = p[0] * p[1] * p[2]
        return p[0] * p[1] + p[0] * p[2] + p[1] * p[2] - 2.0 * triple
    }
}

object BastilleBattalionAdvisor {
    data class Advice(val option: Int, val reason: String, val score: Double)

    fun advise(choiceId: Int, prefs: Preferences): Advice? = when (choiceId) {
        BastilleBattalionSync.CHOICE_CASTLE_VS_CASTLE -> battleAdvice(prefs)
        BastilleBattalionSync.CHOICE_CHEESE_SEEKING -> cheeseAdvice(prefs)
        BastilleBattalionSync.CHOICE_HELLO_TO_ARMS -> trainingAdvice(prefs)
        else -> null
    }

    fun recommend(choiceId: Int, prefs: Preferences): Int = advise(choiceId, prefs)?.option ?: 0

    private fun battleAdvice(prefs: Preferences): Advice? {
        val enemy = Castle.entries.firstOrNull { it.prefix == prefs.getString("_bastilleEnemyCastle") } ?: return null
        val turn = prefs.getInt("_bastilleGameTurn")
        val battle = ((turn + 2) / 3).coerceAtLeast(1)
        val stats = parseStats(prefs.getString("_bastilleStats"))
        val boosts = BastilleBoosts(prefs.getString("_bastilleBoosts"))
        val probabilities = BastilleBattleSimulation.probabilities(stats, boosts, enemy, battle)
        val stance = probabilities.maxBy { it.value }.key
        val probability = probabilities.getValue(stance)
        return Advice(stance.option, "${stance.label} stance, ${(probability * 100).toInt()}% estimated win", probability)
    }

    private fun cheeseAdvice(prefs: Preferences): Advice? {
        val stats = parseStats(prefs.getString("_bastilleStats"))
        return (1..3).mapNotNull { option ->
            val name = prefs.getString("_bastilleChoice$option")
            if (name.isBlank()) return@mapNotNull null
            val encounter = BastilleCheeseEncounter.forName(name)
            val expected = encounter.expectedCheese(stats.get(encounter.stat))
            Advice(option, "$name: about $expected cheese", expected.toDouble())
        }.maxByOrNull { it.score }
    }

    private fun trainingAdvice(prefs: Preferences): Advice? {
        val enemy = prefs.getString("_bastilleEnemyCastle")
        val superior = Castle.entries.firstOrNull { it.prefix == enemy }?.superiorStat
        return (1..3).mapNotNull { option ->
            val name = prefs.getString("_bastilleChoice$option")
            if (name.isBlank()) return@mapNotNull null
            val score = trainingScore(name, superior)
            Advice(option, "$name: training value $score", score)
        }.maxByOrNull { it.score }
    }

    private fun trainingScore(name: String, enemySuperior: Stat?): Double {
        val lower = name.lowercase()
        var score = when {
            lower.contains("cheese") || lower.contains("tax") -> 1.0
            lower.contains("soldier") || lower.contains("military") || lower.contains("boulder") -> 2.0
            lower.contains("wall") || lower.contains("keep") || lower.contains("retrofit") -> 2.0
            lower.contains("art") || lower.contains("memorial") || lower.contains("mural") -> 2.0
            else -> 1.5
        }
        if (enemySuperior != null && lower.contains(enemySuperior.code.first().lowercaseChar())) score += 0.1
        return score
    }

    internal fun parseStats(setting: String): Stats {
        val values = Regex("""(MA|MD|CA|CD|PA|PD)=(\d+)""").findAll(setting)
            .associate { it.groupValues[1] to it.groupValues[2].toInt() }
        return Stats(
            ma = values["MA"] ?: 0, md = values["MD"] ?: 0,
            ca = values["CA"] ?: 0, cd = values["CD"] ?: 0,
            pa = values["PA"] ?: 0, pd = values["PD"] ?: 0,
        )
    }
}
