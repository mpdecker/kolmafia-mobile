package net.sourceforge.kolmafia.faxbot

/** Desktop [FaxBotDatabase.FaxBot] — one configured faxbot with monster lookups. */
class FaxBot(
    val name: String,
    val playerId: Int,
) {
    private val monsters = mutableListOf<FaxBotMonster>()
    private val monsterByMonsterId = mutableMapOf<Int, FaxBotMonster>()
    private val monsterByCommand = mutableMapOf<String, FaxBotMonster>()
    private var canonicalCommands: Array<String> = emptyArray()

    val categories: List<String>
        get() {
            val temp = linkedSetOf("All Monsters")
            monsters.mapNotNull { it.category.takeIf { c -> c.isNotBlank() && !c.equals("none", ignoreCase = true) } }
                .sorted()
                .forEach { temp.add(it) }
            return temp.toList()
        }

    fun addMonsters(entries: List<FaxBotMonster>) {
        monsters.clear()
        monsterByMonsterId.clear()
        monsterByCommand.clear()

        for (monster in entries) {
            if (monsterByMonsterId.containsKey(monster.monsterId)) continue
            monsters.add(monster)
            monsterByMonsterId[monster.monsterId] = monster
            monsterByCommand[canonicalKey(monster.command)] = monster
        }

        canonicalCommands = monsterByCommand.keys.sorted().toTypedArray()
    }

    fun getMonsterByMonsterId(monsterId: Int): FaxBotMonster? = monsterByMonsterId[monsterId]

    fun getMonsterByCommand(command: String): FaxBotMonster? =
        monsterByCommand[canonicalKey(command)]

    fun findMatchingCommands(command: String): List<String> {
        val search = canonicalKey(command)
        if (search.isEmpty()) return emptyList()

        val exact = canonicalCommands.filter { it == search }
        if (exact.isNotEmpty()) return exact.map { monsterByCommand.getValue(it).command }

        val wordStart = canonicalCommands.filter { it.startsWith(search) }
        if (wordStart.size == 1) {
            return listOf(monsterByCommand.getValue(wordStart[0]).command)
        }
        if (wordStart.size > 1) {
            return wordStart.map { monsterByCommand.getValue(it).command }
        }

        val contains = canonicalCommands.filter { it.contains(search) }
        return contains.map { monsterByCommand.getValue(it).command }
    }

    private fun canonicalKey(value: String): String =
        value.trim().lowercase().replace(Regex("\\s+"), " ")
}
