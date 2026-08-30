package net.sourceforge.kolmafia.session

/** Shared Hunt the Wumpus graph and deduction engine (choice 360). */
object WumpusManager {
    const val CHOICE_ID = 360
    const val WARN_SAFE = 0
    const val WARN_BATS = 1
    const val WARN_PIT = 2
    const val WARN_WUMPUS = 4
    const val WARN_INDEFINITE = 8
    const val WARN_ALL = 15

    val warningStrings = arrayOf(
        "safe", "definite bats", "definite pit", "ERROR: BATS AND PIT",
        "definite Wumpus", "ERROR: BATS AND WUMPUS", "ERROR: PIT AND WUMPUS",
        "ERROR: BATS, PIT, AND WUMPUS", "safe and unvisited", "possible bats",
        "possible pit", "possible bats or pit", "possible Wumpus",
        "possible bats or Wumpus", "possible pit or Wumpus", "possible bats, pit, or Wumpus",
    )
    private val chamberNames = listOf(
        "acrid", "breezy", "creepy", "dripping", "echoing", "fetid", "gloomy",
        "howling", "immense", "long", "moaning", "narrow", "ordinary", "pillared",
        "quiet", "round", "sparkling", "underground", "vaulted", "windy",
    )

    class Room(val name: String) {
        val code = name.first().uppercaseChar().toString()
        var visited = false
        val exits = arrayOfNulls<Room>(3)
        var listen = WARN_INDEFINITE
        var hazards = WARN_ALL
        var pit = 0
        var bat = 0
        var wumpus = 0

        fun reset() {
            visited = false
            exits.fill(null)
            listen = WARN_INDEFINITE
            hazards = WARN_ALL
            pit = 0
            bat = 0
            wumpus = 0
        }

        fun setExit(index: Int, room: Room) { exits[index] = room }
        fun addExit(room: Room) {
            if (exits.any { it === room }) return
            exits.indexOfFirst { it == null }.takeIf { it >= 0 }?.let { exits[it] = room }
        }
        fun setHazards(value: Int): Int {
            val old = hazards
            if (old and WARN_INDEFINITE != 0) hazards = old and value
            return old
        }
        fun listenString(): String = buildList {
            if (listen and WARN_BATS != 0) add("bats")
            if (listen and WARN_PIT != 0) add("pit")
            if (listen and WARN_WUMPUS != 0) add("Wumpus")
        }.joinToString().ifBlank { "none" }
        override fun toString() = "the $name chamber"
    }

    val rooms: Map<String, Room> = chamberNames.associateWith { Room(it) }.toSortedMap()
    var current: Room? = null
        private set
    var last: Room? = null
        private set
    var bats1: Room? = null
        private set
    var bats2: Room? = null
        private set
    var pit1: Room? = null
        private set
    var pit2: Room? = null
        private set
    var wumpus: Room? = null
        private set
    private var monsterIsWumpus = false
    private val deductionLines = mutableListOf<String>()

    private val roomPattern = Regex(""">The (\w+) Chamber""", RegexOption.IGNORE_CASE)
    private val linkPattern = Regex("""Enter the (\w+) chamber""", RegexOption.IGNORE_CASE)

    fun reset() {
        rooms.values.forEach { it.reset() }
        current = null
        last = null
        bats1 = null
        bats2 = null
        pit1 = null
        pit2 = null
        wumpus = null
        monsterIsWumpus = false
        deductionLines.clear()
    }

    fun visitChoice(text: String?): List<String> {
        last = current
        current = null
        deductionLines.clear()
        if (text == null) return emptyList()
        val name = roomPattern.find(text)?.groupValues?.get(1)?.lowercase() ?: return emptyList()
        val room = rooms[name] ?: return emptyList()
        if (room.visited) {
            current = room
            return emptyList()
        }
        if (text.contains("the bats", true)) {
            knownBats(room, true)
            return deductionLines.toList()
        }
        if (text.contains("Thump", true)) {
            knownPit(room, true)
            return deductionLines.toList()
        }
        current = room
        val exits = linkPattern.findAll(text).take(3).mapNotNull {
            rooms[it.groupValues[1].lowercase()]
        }.toList()
        if (exits.size != 3) return deductionLines.toList()
        exits.forEachIndexed { index, exit ->
            room.setExit(index, exit)
            exit.addExit(room)
        }
        deductionLines += "Exits: ${exits.joinToString()}"
        var warning = WARN_INDEFINITE
        if (text.contains("a high-pitched squeaking")) warning = warning or WARN_BATS
        if (text.contains("a low roaring sound")) warning = warning or WARN_PIT
        if (text.contains("the Wumpus must be nearby")) warning = warning or WARN_WUMPUS
        deductionLines += "Sounds: ${listOfNotNull(
            "bats".takeIf { warning and WARN_BATS != 0 },
            "pit".takeIf { warning and WARN_PIT != 0 },
            "Wumpus".takeIf { warning and WARN_WUMPUS != 0 },
        ).joinToString().ifBlank { "none" }}"
        knownSafe(room, true)
        room.listen = warning
        exits.forEach { possibleHazard(it, warning) }
        repeat(3) { deduce(room) }
        return deductionLines.toList()
    }

    fun takeChoice(decision: Int, text: String): List<String> {
        val from = current ?: return emptyList()
        var exitNumber = decision
        if (exitNumber > 3) {
            monsterIsWumpus = false
            exitNumber -= 3
        }
        val room = from.exits.getOrNull(exitNumber - 1) ?: return emptyList()
        if (text.contains("wumpus was nowhere to be seen", true)) {
            last = from
            eliminateHazard(room, WARN_WUMPUS)
        } else if (
            text.contains("unexpectedly, a wumpus", true) ||
            text.contains("surprised the wumpus", true) ||
            text.contains("darkness.gif", true)
        ) {
            last = from
            knownWumpus(room, true)
        }
        return deductionLines.toList()
    }

    fun preWumpus(decision: Int) { monsterIsWumpus = decision > 3 }
    fun isWumpus() = monsterIsWumpus
    fun onWumpusFight(html: String): Boolean {
        if (html.contains("darkness.gif", true)) monsterIsWumpus = true
        val killed = monsterIsWumpus && (
            html.contains("You win the fight", true) ||
                html.contains("WINWINWIN", true) ||
                html.contains("wumpus is slain", true)
            )
        if (killed) reset()
        return monsterIsWumpus || killed
    }

    fun dynamicChoiceOptions(): List<String> =
        current?.exits?.map { warningStrings[it?.hazards ?: WARN_ALL] }.orEmpty().let { it + it }

    private fun knownSafe(room: Room, visited: Boolean) {
        room.bat = 9; room.pit = 9; room.wumpus = 9
        knownHazard(room, WARN_SAFE, visited, "Visit")
    }

    private fun knownBats(room: Room, visited: Boolean) {
        if (room.bat == 8) return
        room.bat = 8; room.pit = 9; room.wumpus = 9
        knownHazard(room, WARN_BATS, visited, if (visited) "Visit" else "Deduction")
        if (bats1 == null) bats1 = room else if (bats1 !== room && bats2 == null) {
            bats2 = room
            eliminateHazard(WARN_BATS)
        }
    }

    private fun knownPit(room: Room, visited: Boolean) {
        if (room.pit == 8) return
        room.bat = 9; room.pit = 8; room.wumpus = 9
        knownHazard(room, WARN_PIT, visited, if (visited) "Visit" else "Deduction")
        if (pit1 == null) pit1 = room else if (pit1 !== room && pit2 == null) {
            pit2 = room
            eliminateHazard(WARN_PIT)
        }
    }

    private fun knownWumpus(room: Room, visited: Boolean) {
        if (room.wumpus == 8) return
        room.bat = 9; room.pit = 9; room.wumpus = 8
        knownHazard(room, WARN_WUMPUS, visited, if (visited) "Visit" else "Deduction")
        wumpus = room
        eliminateHazard(WARN_WUMPUS)
    }

    private fun knownHazard(room: Room, warning: Int, visited: Boolean, reason: String) {
        if (visited) room.visited = true
        val old = room.setHazards(warning)
        if (old != room.hazards) deductionLines += "$reason: ${warningStrings[room.hazards]} in $room"
        deduceNeighbors(room)
    }

    private fun possibleHazard(room: Room, initialWarning: Int) {
        if (room.visited || room.hazards and WARN_INDEFINITE == 0) return
        var warning = initialWarning
        if (room.hazards and WARN_BATS != 0) {
            if (warning and WARN_BATS == 0 || bats2 != null) eliminateHazard(room, WARN_BATS)
            else if (++room.bat == 3) { knownBats(room, false); return }
        }
        if (room.hazards and WARN_PIT != 0) {
            if (warning and WARN_PIT == 0 || pit2 != null) eliminateHazard(room, WARN_PIT)
            else if (++room.pit == 3) { knownPit(room, false); return }
        }
        if (room.hazards and WARN_WUMPUS != 0) {
            if (warning and WARN_WUMPUS == 0 || wumpus != null) eliminateHazard(room, WARN_WUMPUS)
            else if (++room.wumpus == 2) { knownWumpus(room, false); return }
        }
        warning = room.hazards and warning
        if (warning == WARN_INDEFINITE) {
            room.bat = 9; room.pit = 9; room.wumpus = 9
        }
        val old = room.setHazards(warning)
        if (old != room.hazards) deductionLines += "Listen: ${warningStrings[room.hazards]} in $room"
    }

    private fun eliminateHazard(hazard: Int) = rooms.values.forEach { eliminateHazard(it, hazard) }
    private fun eliminateHazard(room: Room, hazard: Int) {
        if (room.hazards and WARN_INDEFINITE == 0) return
        if (hazard and WARN_PIT != 0) room.pit = 9
        if (hazard and WARN_BATS != 0) room.bat = 9
        if (hazard and WARN_WUMPUS != 0) room.wumpus = 9
        val old = room.setHazards(room.hazards and hazard.inv())
        if (old != room.hazards) deductionLines += "Deduction: no ${hazardName(hazard)} in $room"
    }

    private fun hazardName(hazard: Int) = when (hazard) {
        WARN_BATS -> "bats"; WARN_PIT -> "pit"; else -> "Wumpus"
    }

    private fun deduceNeighbors(room: Room) = room.exits.filterNotNull()
        .filter { it.visited && it.listen != WARN_INDEFINITE }.forEach(::deduce)

    private fun deduce(room: Room) {
        listOf(WARN_BATS, WARN_PIT, WARN_WUMPUS).forEach { mask ->
            val candidates = room.exits.filterNotNull().filter { it.hazards and mask != 0 }
            if (candidates.size == 1) when (mask) {
                WARN_BATS -> knownBats(candidates[0], false)
                WARN_PIT -> knownPit(candidates[0], false)
                WARN_WUMPUS -> knownWumpus(candidates[0], false)
            }
        }
    }

    fun getWumpinatorCode(): String = buildString {
        rooms.values.forEach { room ->
            room.exits.forEach { append(it?.code ?: "0") }
            append(room.pit % 10).append(room.bat % 10).append(room.wumpus % 10)
        }
        append("::P")
        rooms.values.filter { it.listen and WARN_PIT != 0 }.forEach { room ->
            append(":").append(room.exits.joinToString("") { it?.code ?: "0" })
        }
        append("::B")
        rooms.values.filter { it.listen and WARN_BATS != 0 }.forEach { room ->
            append(":").append(room.exits.joinToString("") { it?.code ?: "0" })
        }
    }

    fun printStatus(): List<String> = rooms.values.map { room ->
        fun flag(value: Int, hazard: String) = when (value) {
            9 -> "no $hazard"; 8 -> hazard.uppercase(); else -> value.toString()
        }
        "${room.name}: exits = ${room.exits.joinToString { it?.name ?: "unknown" }}: " +
            "${flag(room.pit, "pit")}, ${flag(room.bat, "bats")}, ${flag(room.wumpus, "wumpus")}"
    }

    fun applyChoice(decision: Int, responseText: String): Boolean {
        if (decision <= 0) visitChoice(responseText)
        else {
            preWumpus(decision)
            takeChoice(decision, responseText)
            if (roomPattern.containsMatchIn(responseText)) visitChoice(responseText)
        }
        return true
    }
}
