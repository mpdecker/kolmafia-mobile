package net.sourceforge.kolmafia.buffbot

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
class BuffBotDatabase private constructor(
    costs: List<BuffCost>,
) {

    private val costMap: Map<Int, BuffCost> = costs.associateBy { it.buffId }
    private val botRegistry = mutableMapOf<String, BuffBotEntry>()
    private var registryLoaded = false

    private val philanthropicOfferings = mutableMapOf<String, List<BuffBotOffering>>()
    private val standardOfferings = mutableMapOf<String, List<BuffBotOffering>>()
    private val offeringsFetched = mutableSetOf<String>()

    fun find(buffId: Int): BuffCost? = costMap[buffId]

    fun isKnownBot(name: String): Boolean = botRegistry.containsKey(name.lowercase())

    fun findBot(name: String): BuffBotEntry? = botRegistry[name.lowercase()]

    fun isOptedOut(name: String): Boolean = findBot(name)?.xmlUrl == OPTOUT_URL

    fun allBots(): Collection<BuffBotEntry> = botRegistry.values

    fun philanthropicOfferings(botName: String): List<BuffBotOffering> =
        philanthropicOfferings[botName.lowercase()].orEmpty()

    fun standardOfferings(botName: String): List<BuffBotOffering> =
        standardOfferings[botName.lowercase()].orEmpty()

    suspend fun loadRegistry() {
        if (registryLoaded) return
        val text = Res.readBytes("files/data/buffbots.txt").decodeToString()
        applyRegistryParse(text)
        registryLoaded = true
    }

    suspend fun configureOfferings(httpClient: HttpClient) {
        if (!registryLoaded) {
            loadRegistry()
        }
        for (entry in botRegistry.values) {
            fetchOfferingsForBot(httpClient, entry)
        }
    }

    internal suspend fun fetchOfferingsForBot(httpClient: HttpClient, entry: BuffBotEntry) {
        val key = entry.name.lowercase()
        if (key in offeringsFetched) return
        offeringsFetched.add(key)

        if (entry.xmlUrl == OPTOUT_URL) {
            philanthropicOfferings[key] = emptyList()
            standardOfferings[key] = emptyList()
            return
        }

        try {
            val xml = httpClient.get(entry.xmlUrl).bodyAsText()
            val (free, normal) = BuffBotXmlParser.parse(xml, entry.name)
            philanthropicOfferings[key] = free
            standardOfferings[key] = normal
        } catch (_: Exception) {
            philanthropicOfferings[key] = emptyList()
            standardOfferings[key] = emptyList()
        }
    }

    fun getOffering(
        recipient: String,
        amount: Long,
        activeEffectNames: Set<String>,
        gameDatabase: GameDatabase?,
    ): BuffOfferingResult {
        val botKey = recipient.lowercase()
        if (!botRegistry.containsKey(botKey)) {
            return BuffOfferingResult(meatAmount = amount)
        }

        val entry = botRegistry.getValue(botKey)
        if (entry.xmlUrl == OPTOUT_URL) {
            return BuffOfferingResult(
                meatAmount = 0,
                abortMessage = "${entry.name} has requested to be excluded from scripted requests.",
            )
        }

        val possibles = philanthropicOfferings(botKey)
        if (possibles.isEmpty()) {
            return BuffOfferingResult(meatAmount = amount)
        }

        val current = possibles.firstOrNull { it.price.toLong() == amount }
            ?: return BuffOfferingResult(meatAmount = amount)

        if (current.buffs.size != 1) {
            return BuffOfferingResult(meatAmount = amount)
        }

        val alternatives = standardOfferings(botKey)
        if (alternatives.isEmpty()) {
            return BuffOfferingResult(meatAmount = amount)
        }

        val matchBuff = current.buffs[0]
        val matchTurns = current.turns[0]

        var bestMatch: BuffBotOffering? = null
        var bestTurns = Int.MAX_VALUE

        for (alternative in alternatives) {
            if (alternative.buffs.size != 1) continue
            val testBuff = alternative.buffs[0]
            val testTurns = alternative.turns[0]
            if (!testBuff.equals(matchBuff, ignoreCase = true)) continue
            if (bestMatch == null || (testTurns >= matchTurns && testTurns < bestTurns)) {
                bestMatch = alternative
                bestTurns = testTurns
            }
        }

        if (bestMatch == null) {
            return BuffOfferingResult(meatAmount = amount)
        }

        if (isEffectActive(bestMatch.buffs[0], activeEffectNames, gameDatabase)) {
            return BuffOfferingResult(meatAmount = 0)
        }

        return BuffOfferingResult(
            meatAmount = bestMatch.price.toLong(),
            conversionMessage =
                "Converted to non-philanthropic request: ${bestMatch.turns[0]} turns of " +
                    "${bestMatch.buffs[0]} for ${bestMatch.price} Meat.",
        )
    }

    internal fun applyRegistryParse(text: String) {
        botRegistry.clear()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (!line.contains('\t') && line.toIntOrNull() != null) continue

            val parts = line.split('\t')
            if (parts.size < 3) continue

            val entry = BuffBotEntry(
                name = parts[0].trim(),
                playerId = parts[1].trim(),
                xmlUrl = parts[2].trim(),
            )
            botRegistry[entry.name.lowercase()] = entry
        }
    }

    internal fun registerBotForTest(entry: BuffBotEntry) {
        botRegistry[entry.name.lowercase()] = entry
    }

    internal fun setOfferingsForTest(
        botName: String,
        philanthropic: List<BuffBotOffering>,
        standard: List<BuffBotOffering>,
    ) {
        val key = botName.lowercase()
        philanthropicOfferings[key] = philanthropic
        standardOfferings[key] = standard
        offeringsFetched.add(key)
    }

    internal fun clearRegistryForTest() {
        botRegistry.clear()
        philanthropicOfferings.clear()
        standardOfferings.clear()
        offeringsFetched.clear()
        registryLoaded = false
    }

    companion object {
        const val OPTOUT_URL = "http://forums.kingdomofloathing.com/"

        private val DEFAULT_COSTS = listOf(
            BuffCost(buffId = 3004, buffName = "Empathy of the Newt", meatCost = 100L, turns = 10),
            BuffCost(buffId = 3009, buffName = "Elemental Saucesphere", meatCost = 100L, turns = 10),
            BuffCost(buffId = 1003, buffName = "Seal Clubbing Frenzy", meatCost = 50L, turns = 5),
            BuffCost(buffId = 1005, buffName = "Patience of the Tortoise", meatCost = 50L, turns = 5),
            BuffCost(buffId = 6014, buffName = "Fat Leon's Phat Loot Lyric", meatCost = 100L, turns = 10),
            BuffCost(buffId = 6003, buffName = "Ode to Booze", meatCost = 100L, turns = 10),
        )

        private val defaultInstance = BuffBotDatabase(DEFAULT_COSTS)

        val instance: BuffBotDatabase get() = defaultInstance

        /** @deprecated Use [instance] after [load]. */
        val default: BuffBotDatabase get() = defaultInstance

        suspend fun load() {
            defaultInstance.loadRegistry()
        }

        fun forTest(
            costs: List<BuffCost> = DEFAULT_COSTS,
            bots: List<BuffBotEntry> = emptyList(),
        ): BuffBotDatabase = BuffBotDatabase(costs).also { db ->
            bots.forEach { db.registerBotForTest(it) }
        }
    }
}

private fun isEffectActive(
    buffName: String,
    activeEffectNames: Set<String>,
    gameDatabase: GameDatabase?,
): Boolean {
    val canonical = buffName.lowercase()
    if (activeEffectNames.any { it.equals(canonical, ignoreCase = true) }) {
        return true
    }
    val effect = gameDatabase?.effect(buffName) ?: EffectDatabase.getByName(buffName)
    return effect != null && activeEffectNames.any { it.equals(effect.name, ignoreCase = true) }
}
