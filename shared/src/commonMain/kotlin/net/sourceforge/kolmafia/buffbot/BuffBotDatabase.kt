package net.sourceforge.kolmafia.buffbot

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
class BuffBotDatabase private constructor(
    costs: List<BuffCost>,
) {

    private val costMap: Map<Int, BuffCost> = costs.associateBy { it.buffId }
    private val botRegistry = mutableMapOf<String, BuffBotEntry>()
    private var registryLoaded = false

    fun find(buffId: Int): BuffCost? = costMap[buffId]

    fun isKnownBot(name: String): Boolean = botRegistry.containsKey(name.lowercase())

    fun findBot(name: String): BuffBotEntry? = botRegistry[name.lowercase()]

    fun isOptedOut(name: String): Boolean = findBot(name)?.xmlUrl == OPTOUT_URL

    fun allBots(): Collection<BuffBotEntry> = botRegistry.values

    suspend fun loadRegistry() {
        if (registryLoaded) return
        val text = Res.readBytes("files/data/buffbots.txt").decodeToString()
        applyRegistryParse(text)
        registryLoaded = true
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

    internal fun clearRegistryForTest() {
        botRegistry.clear()
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
