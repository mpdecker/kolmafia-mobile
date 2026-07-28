package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Desktop [StandardRewardDatabase] — Standard Path armory reward + pulverized currency data. */
@OptIn(ExperimentalResourceApi::class)
object StandardRewardDatabase {

    data class StandardReward(
        val itemId: Int,
        val year: Int,
        val hardcore: Boolean,
        val characterClass: CharacterClass,
        val row: String,
        val itemName: String,
    )

    data class StandardPulverized(
        val itemId: Int,
        val year: Int,
        val hardcore: Boolean,
        val itemName: String,
    )

    private val rewardByItemId = sortedMapOf<Int, StandardReward>()
    private val pulverizedByItemId = mutableMapOf<Int, StandardPulverized>()
    private val pulverizedByYearAndType = mutableMapOf<Int, MutableMap<Boolean, StandardPulverized>>()

    private var loaded = false

    fun allStandardRewards(): Map<Int, StandardReward> = rewardByItemId

    fun findStandardReward(itemId: Int): StandardReward? = rewardByItemId[itemId]

    fun findStandardPulverized(itemId: Int): StandardPulverized? = pulverizedByItemId[itemId]

    fun registerStandardReward(itemId: Int, reward: StandardReward) {
        rewardByItemId[itemId] = reward
    }

    fun registerStandardPulverized(itemId: Int, pulverized: StandardPulverized) {
        pulverizedByItemId[itemId] = pulverized
        pulverizedByYearAndType
            .getOrPut(pulverized.year) { mutableMapOf() }[pulverized.hardcore] = pulverized
    }

    fun findPulverization(reward: StandardReward): Int =
        findPulverization(reward.year + 1, reward.hardcore)

    fun findPulverization(year: Int, hardcore: Boolean): Int =
        pulverizedByYearAndType[year]?.get(hardcore)?.itemId ?: -1

    fun isPulverizedStandardReward(itemId: Int): Boolean = pulverizedByItemId.containsKey(itemId)

    suspend fun load() {
        if (loaded) return
        val rewardsText = Res.readBytes("files/data/standard-rewards.txt").decodeToString()
        val pulverizedText = Res.readBytes("files/data/standard-pulverized.txt").decodeToString()
        loadFromText(rewardsText, pulverizedText)
        loaded = true
    }

    internal fun loadFromText(rewardsText: String, pulverizedText: String) {
        rewardByItemId.clear()
        pulverizedByItemId.clear()
        pulverizedByYearAndType.clear()
        readRewards(rewardsText)
        readPulverized(pulverizedText)
    }

    internal fun resetForTest() {
        rewardByItemId.clear()
        pulverizedByItemId.clear()
        pulverizedByYearAndType.clear()
        loaded = false
    }

    private fun readRewards(text: String) {
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val parts = line.split('\t')
            if (parts.size != 6) continue

            val itemId = parts[0].toIntOrNull() ?: continue
            val year = parts[1].toIntOrNull() ?: continue
            val hardcore = when (parts[2].lowercase()) {
                "norm" -> false
                "hard" -> true
                else -> continue
            }
            val characterClass = parseClassCode(parts[3]) ?: continue
            val row = parts[4].trim()
            val itemName = parts[5].trim()

            registerStandardReward(
                itemId,
                StandardReward(itemId, year, hardcore, characterClass, row, itemName),
            )
        }
    }

    private fun readPulverized(text: String) {
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val parts = line.split('\t')
            if (parts.size != 4) continue

            val itemId = parts[0].toIntOrNull() ?: continue
            val year = parts[1].toIntOrNull() ?: continue
            val hardcore = when (parts[2].lowercase()) {
                "norm" -> false
                "hard" -> true
                else -> continue
            }
            val itemName = parts[3].trim()

            registerStandardPulverized(
                itemId,
                StandardPulverized(itemId, year, hardcore, itemName),
            )
        }
    }

    private fun parseClassCode(code: String): CharacterClass? = when (code.uppercase()) {
        "SC" -> CharacterClass.SEAL_CLUBBER
        "TT" -> CharacterClass.TURTLE_TAMER
        "PA" -> CharacterClass.PASTAMANCER
        "SA" -> CharacterClass.SAUCEROR
        "DB" -> CharacterClass.DISCO_BANDIT
        "AT" -> CharacterClass.ACCORDION_THIEF
        else -> null
    }

    fun parseRowNumber(row: String): Int? {
        val trimmed = row.trim()
        if (trimmed.equals("UNKNOWN", ignoreCase = true)) return null
        return trimmed.removePrefix("ROW").toIntOrNull()
    }
}
