package net.sourceforge.kolmafia.faxbot

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
class FaxBotDatabase private constructor() {

    private val registry = mutableListOf<FaxBotEntry>()
    private val faxbots = mutableListOf<FaxBot>()
    private var registryLoaded = false

    fun allBots(): List<FaxBot> = faxbots.toList()

    fun getFaxbot(name: String): FaxBot? =
        faxbots.firstOrNull { it.name.equals(name, ignoreCase = true) }

    fun getSortedFaxbots(preferences: Preferences?): List<FaxBot> {
        val preferredName = preferences?.getString(PREF_LAST_SUCCESSFUL, "").orEmpty()
        val preferred = getFaxbot(preferredName)
        if (preferred == null || faxbots.isEmpty()) return faxbots.toList()
        val index = faxbots.indexOfFirst { it.name.equals(preferred.name, ignoreCase = true) }
        if (index <= 0) return faxbots.toList()
        val list = faxbots.toMutableList()
        list.removeAt(index)
        list.add(0, preferred)
        return list
    }

    fun canFaxbot(monsterId: Int, botName: String? = null): Boolean {
        val bots = if (botName.isNullOrBlank()) faxbots else listOfNotNull(getFaxbot(botName))
        return bots.any { it.getMonsterByMonsterId(monsterId) != null }
    }

    suspend fun canFaxbotOnline(
        monsterId: Int,
        botName: String? = null,
        isOnline: suspend (String) -> Boolean,
    ): Boolean {
        val normalizedBot = botName?.trim()?.takeIf { it.isNotEmpty() }?.lowercase()
        for (bot in faxbots) {
            if (normalizedBot != null && !bot.name.equals(normalizedBot, ignoreCase = true)) continue
            if (bot.getMonsterByMonsterId(monsterId) == null) continue
            if (isOnline(bot.name)) return true
        }
        return false
    }

    suspend fun loadRegistry() {
        if (registryLoaded) return
        val text = Res.readBytes("files/data/faxbots.txt").decodeToString()
        applyRegistryParse(text)
        registryLoaded = true
    }

    suspend fun configure(httpClient: HttpClient, gameDatabase: GameDatabase?) {
        if (!registryLoaded) {
            loadRegistry()
        }
        if (faxbots.isNotEmpty()) return
        for (entry in registry) {
            fetchConfigForEntry(httpClient, entry, gameDatabase)
        }
    }

    internal suspend fun fetchConfigForEntry(
        httpClient: HttpClient,
        entry: FaxBotEntry,
        gameDatabase: GameDatabase?,
    ) {
        try {
            val xml = httpClient.get(entry.xmlUrl).bodyAsText()
            val parsed = FaxBotXmlParser.parse(xml, gameDatabase)
            faxbots.addAll(parsed.bots)
        } catch (_: Exception) {
        }
    }

    internal fun applyRegistryParse(text: String) {
        registry.clear()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (!line.contains('\t')) continue
            val parts = line.split('\t', limit = 2)
            if (parts.size < 2) continue
            registry.add(
                FaxBotEntry(
                    name = parts[0].trim(),
                    xmlUrl = parts[1].trim(),
                ),
            )
        }
    }

    internal fun registerBotForTest(bot: FaxBot) {
        faxbots.add(bot)
    }

    internal fun clearForTest() {
        registry.clear()
        faxbots.clear()
        registryLoaded = false
    }

    companion object {
        const val PREF_LAST_SUCCESSFUL = "lastSuccessfulFaxbot"

        private val defaultInstance = FaxBotDatabase()

        val instance: FaxBotDatabase get() = defaultInstance

        suspend fun load() {
            defaultInstance.loadRegistry()
        }
    }
}
