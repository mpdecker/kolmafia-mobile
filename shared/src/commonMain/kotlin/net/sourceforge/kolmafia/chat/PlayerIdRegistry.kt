package net.sourceforge.kolmafia.chat

/** Lightweight player name/id cache mirroring desktop `ContactManager` seen maps. */
object PlayerIdRegistry {

    private val seenPlayerIds = mutableMapOf<String, String>()
    private val seenPlayerNames = mutableMapOf<String, String>()

    fun register(name: String, id: String) {
        if (id.startsWith("-")) return
        val trimmedName = name.trim()
        if (trimmedName.isEmpty() || id.isBlank()) return
        val key = trimmedName.lowercase()
        if (seenPlayerIds.containsKey(key)) return
        seenPlayerIds[key] = id
        seenPlayerNames[id] = trimmedName
    }

    fun getPlayerId(name: String, retrieveId: Boolean = false, lookup: (() -> Unit)? = null): String {
        val key = name.trim().lowercase()
        if (key.isEmpty()) return name
        seenPlayerIds[key]?.let { return it }
        if (retrieveId) {
            lookup?.invoke()
            seenPlayerIds[key]?.let { return it }
        }
        return name
    }

    fun getPlayerName(id: String, retrieveName: Boolean = false, lookup: (() -> Unit)? = null): String {
        val playerId = id.trim()
        if (playerId.isEmpty()) return id
        seenPlayerNames[playerId]?.let { return it }
        if (retrieveName) {
            lookup?.invoke()
            seenPlayerNames[playerId]?.let { return it }
        }
        return id
    }

    fun clearForTest() {
        seenPlayerIds.clear()
        seenPlayerNames.clear()
    }
}
