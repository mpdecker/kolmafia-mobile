package net.sourceforge.kolmafia.faxbot

import net.sourceforge.kolmafia.data.GameDatabase

/** Regex XML parse for faxbot configs (desktop [FaxBotDatabase.DynamicBotFetcher]). */
object FaxBotXmlParser {

    private val botDataPattern = Regex("<botdata>(.*?)</botdata>", RegexOption.DOT_MATCHES_ALL)
    private val monsterDataPattern = Regex("<monsterdata>(.*?)</monsterdata>", RegexOption.DOT_MATCHES_ALL)
    private val namePattern = Regex("<name>(.*?)</name>", RegexOption.DOT_MATCHES_ALL)
    private val playerIdPattern = Regex("<playerid>(.*?)</playerid>", RegexOption.DOT_MATCHES_ALL)
    private val actualNamePattern = Regex("<actual_name>(.*?)</actual_name>", RegexOption.DOT_MATCHES_ALL)
    private val commandPattern = Regex("<command>(.*?)</command>", RegexOption.DOT_MATCHES_ALL)
    private val categoryPattern = Regex("<category>(.*?)</category>", RegexOption.DOT_MATCHES_ALL)

    data class ParsedConfig(
        val bots: List<FaxBot>,
        val monsters: List<FaxBotMonster>,
    )

    fun parse(xml: String, gameDatabase: GameDatabase?): ParsedConfig {
        val monsters = mutableListOf<FaxBotMonster>()
        for (nodeMatch in monsterDataPattern.findAll(xml)) {
            val block = nodeMatch.groupValues[1]
            val displayName = namePattern.find(block)?.groupValues?.get(1)?.trim().orEmpty()
            val actualName = actualNamePattern.find(block)?.groupValues?.get(1)?.trim().orEmpty()
            val command = commandPattern.find(block)?.groupValues?.get(1)?.trim().orEmpty()
            val category = categoryPattern.find(block)?.groupValues?.get(1)?.trim().orEmpty()
            FaxBotMonster.fromXmlFields(displayName, actualName, command, category, gameDatabase)
                ?.let { monsters.add(it) }
        }

        val bots = mutableListOf<FaxBot>()
        for (nodeMatch in botDataPattern.findAll(xml)) {
            val block = nodeMatch.groupValues[1]
            val name = namePattern.find(block)?.groupValues?.get(1)?.trim().orEmpty()
            val playerId = playerIdPattern.find(block)?.groupValues?.get(1)?.trim()?.toIntOrNull() ?: 0
            if (name.isBlank()) continue
            val bot = FaxBot(name, playerId)
            bot.addMonsters(monsters)
            bots.add(bot)
        }

        return ParsedConfig(bots = bots, monsters = monsters)
    }
}
