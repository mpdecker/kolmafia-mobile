package net.sourceforge.kolmafia.clan

/**
 * Desktop [ClanManager] cache hub — lounge/rumpus/hotdogs/stash/members/ranks
 * (Phases 2751–2765).
 */
object ClanManager {

    data class StashItem(val itemId: Int, val name: String, val quantity: Int)
    data class ClanMember(val name: String, val playerId: String, val title: String = "")

    private var clanId: Int = 0
    private var clanName: String? = null
    var stashRetrieved: Boolean = false
        private set
    private var ranksRetrieved: Boolean = false

    private val clanHotdogs = mutableMapOf<Int, MutableList<String>>()
    private val clanLounge = mutableMapOf<Int, MutableList<Pair<String, Int>>>()
    private val clanRumpus = mutableMapOf<Int, MutableList<String>>()
    private val stashContents = mutableListOf<StashItem>()
    private val rankList = mutableListOf<String>()
    private val currentMembers = mutableListOf<ClanMember>()
    private val whiteListMembers = mutableListOf<ClanMember>()
    private val titleMap = mutableMapOf<String, String>()
    private val stashLog = mutableListOf<String>()

    fun getClanId(): Int = clanId

    fun getClanName(): String = clanName.orEmpty()

    fun setClan(id: Int, name: String?) {
        if (id != clanId) {
            clearCache(newCharacter = false)
        }
        clanId = id
        clanName = name?.takeIf { it.isNotEmpty() }
    }

    fun setClanId(id: Int) {
        clanId = id
    }

    fun setClanName(name: String?) {
        clanName = name?.takeIf { it.isNotEmpty() }
    }

    fun changeClan(id: Int, name: String?) {
        clearCache(newCharacter = false)
        setClan(id, name)
    }

    fun resetClanId() {
        clanId = 0
        clanName = null
    }

    fun clearCache(newCharacter: Boolean) {
        clanId = 0
        clanName = null
        stashRetrieved = false
        ranksRetrieved = false
        currentMembers.clear()
        whiteListMembers.clear()
        titleMap.clear()
        rankList.clear()
        stashContents.clear()
        stashLog.clear()
        if (newCharacter) {
            clanLounge.clear()
            clanRumpus.clear()
            clanHotdogs.clear()
        }
    }

    fun isStashRetrieved(): Boolean = stashRetrieved

    fun setStashRetrieved() {
        stashRetrieved = true
    }

    fun getStash(): List<StashItem> = stashContents.toList()

    fun setStash(items: List<StashItem>) {
        stashContents.clear()
        stashContents.addAll(items)
        stashRetrieved = true
    }

    fun getRankList(): List<String> = rankList.toList()

    fun setRanks(ranks: List<String>) {
        rankList.clear()
        rankList.addAll(ranks)
        ranksRetrieved = true
    }

    fun isRanksRetrieved(): Boolean = ranksRetrieved

    fun getClanLounge(): List<Pair<String, Int>> {
        if (clanId == 0) return emptyList()
        return clanLounge[clanId]?.toList() ?: emptyList()
    }

    fun clearLounge() {
        if (clanId == 0) return
        clanLounge[clanId] = mutableListOf()
    }

    fun addToLounge(name: String, quantity: Int = 1) {
        if (clanId == 0 || name.isBlank()) return
        val list = clanLounge.getOrPut(clanId) { mutableListOf() }
        val idx = list.indexOfFirst { it.first.equals(name, ignoreCase = true) }
        if (idx >= 0) {
            list[idx] = name to (list[idx].second + quantity)
        } else {
            list.add(name to quantity)
        }
    }

    fun setLounge(items: List<Pair<String, Int>>) {
        if (clanId == 0) return
        clanLounge[clanId] = items.toMutableList()
    }

    fun getClanRumpus(): List<String> {
        if (clanId == 0) return emptyList()
        return clanRumpus[clanId]?.toList() ?: emptyList()
    }

    fun setClanRumpus(rumpus: List<String>) {
        if (clanId == 0) return
        clanRumpus[clanId] = rumpus.toMutableList()
    }

    fun addToRumpus(item: String) {
        if (clanId == 0 || item.isBlank()) return
        val list = clanRumpus.getOrPut(clanId) { mutableListOf() }
        if (list.none { it.equals(item, ignoreCase = true) }) list.add(item)
    }

    fun removeFromRumpus(item: String) {
        if (clanId == 0) return
        clanRumpus[clanId]?.removeAll { it.equals(item, ignoreCase = true) }
    }

    fun getHotdogs(): List<String> {
        if (clanId == 0) return emptyList()
        return clanHotdogs[clanId]?.toList() ?: emptyList()
    }

    fun addHotdog(name: String) {
        if (clanId == 0) return
        val list = clanHotdogs.getOrPut(clanId) { mutableListOf() }
        if (!list.contains(name)) list.add(name)
    }

    fun getMembers(): List<ClanMember> = currentMembers.toList()

    fun getWhiteList(): List<ClanMember> = whiteListMembers.toList()

    fun setMembers(members: List<ClanMember>) {
        currentMembers.clear()
        currentMembers.addAll(members)
        for (m in members) {
            if (m.title.isNotBlank()) titleMap[m.name.lowercase()] = m.title
        }
    }

    fun setWhiteList(members: List<ClanMember>) {
        whiteListMembers.clear()
        whiteListMembers.addAll(members)
    }

    fun registerMember(name: String, playerId: String, title: String = "") {
        if (currentMembers.none { it.playerId == playerId || it.name.equals(name, ignoreCase = true) }) {
            currentMembers.add(ClanMember(name, playerId, title))
        }
        if (title.isNotBlank()) titleMap[name.lowercase()] = title
    }

    fun unregisterMember(playerId: String) {
        currentMembers.removeAll { it.playerId == playerId }
    }

    fun isCurrentMember(memberName: String): Boolean =
        currentMembers.any { it.name.equals(memberName, ignoreCase = true) }

    fun isMember(memberName: String): Boolean =
        isCurrentMember(memberName) ||
            whiteListMembers.any { it.name.equals(memberName, ignoreCase = true) }

    fun getTitle(name: String): String = titleMap[name.lowercase()].orEmpty()

    fun getStashLog(): List<String> = stashLog.toList()

    fun addStashLogLine(line: String) {
        if (line.isNotBlank()) stashLog.add(line)
    }

    fun setStashLog(lines: List<String>) {
        stashLog.clear()
        stashLog.addAll(lines)
    }

    fun saveStashLog(): String = stashLog.joinToString("\n")

    internal fun resetForTest() {
        clearCache(newCharacter = true)
    }
}
