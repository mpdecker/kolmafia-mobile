package net.sourceforge.kolmafia.clan

/** Desktop ClanManager minimal subset — clan id/name + per-clan hotdog menu cache. */
object ClanManager {

    private var clanId: Int = 0
    private var clanName: String? = null
    private val clanHotdogs = mutableMapOf<Int, MutableList<String>>()

    fun getClanId(): Int = clanId

    fun getClanName(): String = clanName.orEmpty()

    fun setClan(id: Int, name: String?) {
        clanId = id
        clanName = name?.takeIf { it.isNotEmpty() }
    }

    fun clearCache(newCharacter: Boolean) {
        clanId = 0
        clanName = null
        if (newCharacter) {
            clanHotdogs.clear()
        }
    }

    fun getHotdogs(): List<String> {
        if (clanId == 0) return emptyList()
        return clanHotdogs[clanId]?.toList() ?: emptyList()
    }

    fun addHotdog(name: String) {
        if (clanId == 0) return
        val list = clanHotdogs.getOrPut(clanId) { mutableListOf() }
        if (!list.contains(name)) {
            list.add(name)
        }
    }

    internal fun resetForTest() {
        clanId = 0
        clanName = null
        clanHotdogs.clear()
    }
}
